package com.royalshield.app.managers

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.royalshield.app.models.*
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

/**
 * Manager for the Duo Shield referral system
 * Handles invite creation, limits, and Duo activation
 */
class ReferralManager(private val context: Context) {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    companion object {
        private const val TAG = "ReferralManager"
        private const val COLLECTION_USERS = "users"
        private const val COLLECTION_INVITES = "invites"
        private const val COLLECTION_DUO_REQUESTS = "duo_requests"
        
        // Configuration (ideally from Remote Config)
        var activationMode = ActivationMode.REGISTER
    }
    
    /**
     * Gets plan data for the current user
     */
    suspend fun getCurrentUserPlanData(): Result<UserPlanData> {
        return try {
            val uid = auth.currentUser?.uid 
                ?: return Result.failure(Exception("User not authenticated"))
            
            val doc = firestore.collection(COLLECTION_USERS)
                .document(uid)
                .get()
                .await()
            
            if (doc.exists()) {
                val data = doc.toObject(UserPlanData::class.java)
                    ?: UserPlanData(uid = uid, email = auth.currentUser?.email ?: "")
                Result.success(data)
            } else {
                // Create data for the first time
                val newData = UserPlanData(
                    uid = uid,
                    email = auth.currentUser?.email ?: ""
                )
                firestore.collection(COLLECTION_USERS)
                    .document(uid)
                    .set(newData)
                    .await()
                Result.success(newData)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user data", e)
            Result.failure(e)
        }
    }
    
    /**
     * Creates a new invite
     * Validates limits before creating
     */
    suspend fun createInvite(): Result<Invite> {
        return try {
            val uid = auth.currentUser?.uid 
                ?: return Result.failure(Exception("User not authenticated"))
            val email = auth.currentUser?.email ?: ""
            
            // Get current user data
            val userDataResult = getCurrentUserPlanData()
            if (userDataResult.isFailure) {
                return Result.failure(userDataResult.exceptionOrNull() 
                    ?: Exception("Error getting data"))
            }
            
            val userData = userDataResult.getOrNull()!!
            val currentMonthKey = getCurrentMonthKey()
            
            // Check limits
            val referral = userData.referral
            
            // Reset monthly count if month changed
            val monthlyCount = if (referral.monthKey == currentMonthKey) {
                referral.monthlyInviteCount
            } else {
                0
            }
            
            // Validate monthly limit
            if (monthlyCount >= ReferralLimits.MAX_INVITES_PER_MONTH) {
                return Result.failure(Exception(
                    "Monthly limit reached (${ReferralLimits.MAX_INVITES_PER_MONTH} invite per month)"
                ))
            }
            
            // Validate active invites limit
           if (referral.activeInviteCount >= ReferralLimits.MAX_ACTIVE_INVITES) {
                return Result.failure(Exception(
                    "Active invites limit reached (${ReferralLimits.MAX_ACTIVE_INVITES} maximum)"
                ))
            }
            
            // Generate unique code
            val code = generateInviteCode()
            
            // Calculate expiration
            val expiresAt = System.currentTimeMillis() + 
                (ReferralLimits.INVITE_EXPIRATION_DAYS * 24 * 60 * 60 * 1000L)
            
            // Create invite
            val invite = Invite(
                code = code,
                inviterUid = uid,
                inviterEmail = email,
                status = InviteStatus.PENDING,
                createdAt = System.currentTimeMillis(),
                expiresAt = expiresAt,
                monthKey = currentMonthKey,
                activationModeSnapshot = activationMode
            )
            
            // Save to Firestore
            firestore.collection(COLLECTION_INVITES)
                .document(code)
                .set(invite)
                .await()
            
            // Update user counters
            val updates = mapOf(
                "referral.activeInviteCount" to FieldValue.increment(1),
                "referral.monthKey" to currentMonthKey,
                "referral.monthlyInviteCount" to if (referral.monthKey == currentMonthKey) {
                    FieldValue.increment(1)
                } else {
                    1  // Reset counter
                }
            )
            
            firestore.collection(COLLECTION_USERS)
                .document(uid)
                .set(updates, SetOptions.merge())
                .await()
            
            Log.d(TAG, "Invite created successfully: $code")
            Result.success(invite)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error creating invite", e)
            Result.failure(e)
        }
    }
    
    /**
     * Gets all invites of the current user
     */
    suspend fun getUserInvites(): Result<List<Invite>> {
        return try {
            val uid = auth.currentUser?.uid 
                ?: return Result.failure(Exception("User not authenticated"))
            
            val snapshot = firestore.collection(COLLECTION_INVITES)
                .whereEqualTo("inviterUid", uid)
                .get()
                .await()
            
            val invites = snapshot.documents.mapNotNull { 
                it.toObject(Invite::class.java) 
            }
            
            Result.success(invites)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting invites", e)
            Result.failure(e)
        }
    }
    
    /**
     * Processes the claim of an invite by a friend
     * Called when the friend creates its account using the code
     */
    suspend fun claimInvite(code: String, friendUid: String, friendEmail: String): Result<Boolean> {
        return try {
            val inviteDoc = firestore.collection(COLLECTION_INVITES)
                .document(code)
                .get()
                .await()
            
            if (!inviteDoc.exists()) {
                return Result.failure(Exception("Invalid invite code"))
            }
            
            val invite = inviteDoc.toObject(Invite::class.java)!!
            
            // Validate it's not expired
            if (invite.isExpired()) {
                // Mark as expired
                firestore.collection(COLLECTION_INVITES)
                    .document(code)
                    .update("status", InviteStatus.EXPIRED.name)
                    .await()
                    
                return Result.failure(Exception("Code expired"))
            }
            
            // Validate it's not already claimed
            if (invite.status != InviteStatus.PENDING) {
                return Result.failure(Exception("Code already used"))
            }
            
            // Update invite to CLAIMED
            val updates = mapOf(
                "status" to InviteStatus.CLAIMED.name,
                "claimedByUid" to friendUid,
                "claimedByEmail" to friendEmail
            )
            
            firestore.collection(COLLECTION_INVITES)
                .document(code)
                .update(updates)
                .await()
            
            // Create DuoRequest
            val duoRequestId = "${invite.inviterUid}_${friendUid}"
            val duoRequest = DuoRequest(
                id = duoRequestId,
                inviterUid = invite.inviterUid,
                friendUid = friendUid,
                inviteCode = code,
                status = DuoRequestStatus.WAITING_ACTIVATION,
                createdAt = System.currentTimeMillis()
            )
            
            firestore.collection(COLLECTION_DUO_REQUESTS)
                .document(duoRequestId)
                .set(duoRequest)
                .await()
            
            // Evaluate activation according to mode
            if (activationMode == ActivationMode.REGISTER) {
                // Activate immediately
                activateDuoShield(code)
            }
            
            Log.d(TAG, "Invite claimed: $code by $friendEmail")
            Result.success(true)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing claim", e)
            Result.failure(e)
        }
    }
    
    /**
     * Activates Duo Shield for both users
     * Shows message "2-for-1 Protection Activated"
     */
    suspend fun activateDuoShield(inviteCode: String): Result<Boolean> {
        return try {
            val inviteDoc = firestore.collection(COLLECTION_INVITES)
                .document(inviteCode)
                .get()
                .await()
            
            if (!inviteDoc.exists()) {
                return Result.failure(Exception("Invite not found"))
           }
            
            val invite = inviteDoc.toObject(Invite::class.java)!!
            
            if (invite.claimedByUid == null) {
                return Result.failure(Exception("Invite not claimed"))
            }
            
            val inviterUid = invite.inviterUid
            val friendUid = invite.claimedByUid!!
            val duoRequestId = "${inviterUid}_${friendUid}"
            
            // Update both users to DUO plan
            val inviterUpdates = mapOf(
                "planTier" to PlanTier.DUO.name,
                "duoPartnerUid" to friendUid,
                "duoPartnerEmail" to invite.claimedByEmail
            )
            
            val friendUpdates = mapOf(
                "planTier" to PlanTier.DUO.name,
                "duoPartnerUid" to inviterUid,
                "duoPartnerEmail" to invite.inviterEmail
            )
            
            // Execute updates
            firestore.collection(COLLECTION_USERS)
                .document(inviterUid)
                .set(inviterUpdates, SetOptions.merge())
                .await()
            
            firestore.collection(COLLECTION_USERS)
                .document(friendUid)
                .set(friendUpdates, SetOptions.merge())
                .await()
            
            // Update invite to ACTIVATED
            firestore.collection(COLLECTION_INVITES)
                .document(inviteCode)
                .update(mapOf(
                    "status" to InviteStatus.ACTIVATED.name,
                    "activatedAt" to System.currentTimeMillis()
                ))
                .await()
            
            // Update DuoRequest
            firestore.collection(COLLECTION_DUO_REQUESTS)
                .document(duoRequestId)
                .update(mapOf(
                    "status" to DuoRequestStatus.ACTIVATED.name,
                    "activatedAt" to System.currentTimeMillis()
                ))
                .await()
            
            // Decrement active invites counter for inviter
            firestore.collection(COLLECTION_USERS)
                .document(inviterUid)
                .update("referral.activeInviteCount", FieldValue.increment(-1))
                .await()
            
            Log.d(TAG, "✅ Duo Shield activated: $inviterUid <-> $friendUid")
            Result.success(true)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error activating Duo Shield", e)
            Result.failure(e)
        }
    }
    
    /**
     * Generates a sharable invite link
     */
    fun generateInviteLink(code: String): String {
        // TODO: Use Firebase Dynamic Links in production
        return "https://royalshield.app/invite?code=$code"
    }
    
    /**
     * Private helpers
     */
    private fun generateInviteCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6)
            .map { chars.random() }
            .joinToString("")
    }
    
    private fun getCurrentMonthKey(): String {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        return sdf.format(Date())
    }
}
