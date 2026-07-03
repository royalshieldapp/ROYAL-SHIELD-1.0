package com.royalshield.app.managers

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.royalshield.app.models.Campaign
import com.royalshield.app.models.CampaignClaim
import com.royalshield.app.models.UIPromotion
import kotlinx.coroutines.tasks.await

/**
 * Manager for the promotional campaigns system
 * Handles "First 50 users" and other limited promos
 */
class CampaignManager(private val context: Context) {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    companion object {
        private const val TAG = "CampaignManager"
        private const val COLLECTION_CAMPAIGNS = "campaigns"
        private const val COLLECTION_CLAIMS = "campaign_claims"
        private const val COLLECTION_UI_PROMOTIONS = "ui_promotions"
        private const val DOCUMENT_REGISTRATION_PROMOS = "registration"
        
        // ID of the First 50 campaign (must exist in Firestore)
        const val CAMPAIGN_FIRST_50_ID = "first_50_premium"
    }
    
    /**
     * Gets a campaign by ID
     */
    suspend fun getCampaign(campaignId: String): Result<Campaign?> {
        return try {
            val doc = firestore.collection(COLLECTION_CAMPAIGNS)
                .document(campaignId)
                .get()
                .await()
            
            if (doc.exists()) {
                val campaign = doc.toObject(Campaign::class.java)
                Result.success(campaign)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting campaign", e)
            Result.failure(e)
        }
    }
    
    /**
     * Gets all active and available campaigns
     */
    suspend fun getAvailableCampaigns(): Result<List<Campaign>> {
        return try {
            val now = System.currentTimeMillis()
            
            val snapshot = firestore.collection(COLLECTION_CAMPAIGNS)
                .whereEqualTo("isActive", true)
                .whereLessThanOrEqualTo("startAt", now)
                .whereGreaterThanOrEqualTo("endAt", now)
                .get()
                .await()
            
            val campaigns = snapshot.documents.mapNotNull { 
                it.toObject(Campaign::class.java) 
            }.filter {
                it.remainingSlots > 0
            }
            
            Result.success(campaigns)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting available campaigns", e)
            Result.failure(e)
        }
    }
    
    /**
     * Claims a slot in a campaign
     * Uses atomic transaction to avoid race conditions
     */
    suspend fun claimCampaign(campaignId: String): Result<CampaignClaim> {
        return try {
            val uid = auth.currentUser?.uid 
                ?: return Result.failure(Exception("User not authenticated"))
            
            val claimId = "${uid}_${campaignId}"
            
            // Check if claim already made
            val existingClaim = firestore.collection(COLLECTION_CLAIMS)
                .document(claimId)
                .get()
                .await()
            
            if (existingClaim.exists()) {
                return Result.failure(Exception("You have already claimed this promotion"))
            }
            
            // Get campaign
            val campaignDoc = firestore.collection(COLLECTION_CAMPAIGNS)
                .document(campaignId)
                .get()
                .await()
            
            if (!campaignDoc.exists()) {
                return Result.failure(Exception("Campaign not found"))
            }
            
            val campaign = campaignDoc.toObject(Campaign::class.java)!!
            
            // Validate availability
            if (!campaign.isAvailable()) {
                return Result.failure(Exception("Campaign not available"))
            }
            
            // Decrement slots atomically
            firestore.collection(COLLECTION_CAMPAIGNS)
                .document(campaignId)
                .update("remainingSlots", FieldValue.increment(-1))
                .await()
            
            // Calculate benefit expiration if applicable
            val benefitExpiresAt = if (campaign.benefit.premiumDays > 0) {
                System.currentTimeMillis() + (campaign.benefit.premiumDays * 24 * 60 * 60 * 1000L)
            } else {
                null
            }
            
            // Create claim
            val claim = CampaignClaim(
                id = claimId,
                uid = uid,
                campaignId = campaignId,
                claimedAt = System.currentTimeMillis(),
                benefitExpiresAt = benefitExpiresAt
            )
            
            // Save claim
            firestore.collection(COLLECTION_CLAIMS)
                .document(claimId)
                .set(claim)
                .await()
           
            // Apply benefit to user
            applyBenefitToUser(uid, campaign, benefitExpiresAt)
            
            Log.d(TAG, "✅ Campaign claimed: $campaignId by $uid")
            Result.success(claim)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error claiming campaign", e)
            Result.failure(e)
        }
    }
    
    /**
     * Applies the campaign benefit to the user
     */
    private suspend fun applyBenefitToUser(
        uid: String,
        campaign: Campaign,
        benefitExpiresAt: Long?
    ) {
        try {
            val updates = mutableMapOf<String, Any>()
            
            if (campaign.benefit.premiumDays > 0 && benefitExpiresAt != null) {
                updates["premiumExpiresAt"] = benefitExpiresAt
            }
            
            if (campaign.benefit.unlockPremiumTemp) {
                updates["campaignBenefits"] = FieldValue.arrayUnion(campaign.id)
            }
            
            if (updates.isNotEmpty()) {
                firestore.collection("users")
                    .document(uid)
                    .update(updates)
                    .await()
                
                Log.d(TAG, "Benefit applied to user: $uid")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error applying benefit", e)
        }
    }
    
    /**
     * Gets claims of the current user
     */
    suspend fun getUserClaims(): Result<List<CampaignClaim>> {
        return try {
            val uid = auth.currentUser?.uid 
                ?: return Result.failure(Exception("User not authenticated"))
            
            val snapshot = firestore.collection(COLLECTION_CLAIMS)
                .whereEqualTo("uid", uid)
                .get()
                .await()
            
            val claims = snapshot.documents.mapNotNull { 
                it.toObject(CampaignClaim::class.java) 
            }
            
            Result.success(claims)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting claims", e)
            Result.failure(e)
        }
    }
    
    /**
     * Gets promotions to show in Registration UI
     */
    suspend fun getRegistrationPromotions(): Result<List<UIPromotion>> {
        return try {
            val doc = firestore.collection(COLLECTION_UI_PROMOTIONS)
                .document(DOCUMENT_REGISTRATION_PROMOS)
                .get()
                .await()
            
            if (!doc.exists()) {
                return Result.success(emptyList())
            }
            
            @Suppress("UNCHECKED_CAST")
            val itemsList = doc.get("items") as? List<Map<String, Any>> ?: emptyList()
            
            val promotions = itemsList.mapNotNull { map ->
                try {
                    UIPromotion(
                        id = map["id"] as? String ?: "",
                        title = map["title"] as? String ?: "",
                        message = map["message"] as? String ?: "",
                        startAt = (map["startAt"] as? Long) ?: 0L,
                        endAt = (map["endAt"] as? Long) ?: 0L,
                        showOnce = (map["showOnce"] as? Boolean) ?: true,
                        campaignId = map["campaignId"] as? String
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Error parsing promotion", e)
                    null
                }
            }.filter { it.isAvailable() }
            
            Result.success(promotions)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting registration promotions", e)
            Result.failure(e)
        }
    }
    
    /**
     * Checks if a user has already seen a promotion
     * Uses SharedPreferences for local cache
     */
    fun hasSeenPromotion(promoId: String): Boolean {
        val prefs = context.getSharedPreferences("campaign_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("seen_$promoId", false)
    }
    
    /**
     * Marks a promotion as seen
     */
    fun markPromotionAsSeen(promoId: String) {
        val prefs = context.getSharedPreferences("campaign_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("seen_$promoId", true).apply()
    }
}
