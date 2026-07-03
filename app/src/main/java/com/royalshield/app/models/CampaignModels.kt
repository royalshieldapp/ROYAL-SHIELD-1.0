package com.royalshield.app.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Tipo de campaña promocional
 */
enum class CampaignType {
    FIRST_50,          // Primeros 50 usuarios
    LIMITED_TIME,      // Tiempo limitado
    REFERRAL_BONUS;    // Bonus por referido
    
    fun getDisplayName(): String = when(this) {
        FIRST_50 -> "First 50 Users"
        LIMITED_TIME -> "Limited Time Offer"
        REFERRAL_BONUS -> "Referral Bonus"
    }
}

/**
 * Beneficio de campaña
 */
@Parcelize
data class CampaignBenefit(
    val premiumDays: Int = 0,              // Días de premium gratis
    val discountPercent: Int = 0,          // Descuento en %
    val unlockPremiumTemp: Boolean = false // Desbloquear features premium temporalmente
) : Parcelable

/**
 * Campaña promocional en Firestore
 */
@Parcelize
data class Campaign(
    val id: String = "",                   // ID único
    val type: CampaignType = CampaignType.FIRST_50,
    val title: String = "",                // "Limited Offer"
    val message: String = "",              // Mensaje completo
    val remainingSlots: Int = 50,          // Slots disponibles
    val totalSlots: Int = 50,              // Total de slots
    val startAt: Long = 0,                 // Timestamp inicio
    val endAt: Long = 0,                   // Timestamp fin
    val benefit: CampaignBenefit = CampaignBenefit(),
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable {
    
    fun isAvailable(): Boolean {
        val now = System.currentTimeMillis()
        return isActive && 
               remainingSlots > 0 && 
               now >= startAt && 
               now <= endAt
    }
    
    fun isFull(): Boolean = remainingSlots <= 0
    
    fun getProgressPercentage(): Int {
        if (totalSlots == 0) return 0
        val claimed = totalSlots - remainingSlots
        return ((claimed.toFloat() / totalSlots) * 100).toInt()
    }
}

/**
 * Claim de campaña por usuario
 */
@Parcelize
data class CampaignClaim(
    val id: String = "",                   // uid_campaignId
    val uid: String = "",
    val campaignId: String = "",
    val claimedAt: Long = System.currentTimeMillis(),
    val benefitExpiresAt: Long? = null     // Cuando expira el beneficio
) : Parcelable {
    
    fun isBenefitActive(): Boolean {
        benefitExpiresAt?.let {
            return System.currentTimeMillis() < it
        }
        return false
    }
}

/**
 * Promoción para UI (popup en registration)
 */
@Parcelize
data class UIPromotion(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val startAt: Long = 0,
    val endAt: Long = 0,
    val showOnce: Boolean = true,          // Mostrar solo una vez por sesión
    val campaignId: String? = null         // Link a campaña si aplica
) : Parcelable {
    
    fun isAvailable(): Boolean {
        val now = System.currentTimeMillis()
        return now >= startAt && now <= endAt
    }
}
