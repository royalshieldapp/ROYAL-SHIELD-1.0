package com.royalshield.app.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Plan tier del usuario
 */
enum class PlanTier {
    FREE,      // Usuario sin plan
    SOLO,      // Plan individual pagado
    DUO;       // Plan 2x1 por referido
    
    fun getDisplayName(): String = when(this) {
        FREE -> "Free"
        SOLO -> "Solo Protection"
        DUO -> "Duo Shield"
    }
}

/**
 * Modo de activación del referido
 * Configurable por Remote Config o Firestore
 */
enum class ActivationMode {
    REGISTER,  // Solo con registro
    VERIFY,    // Registro + verificación email/phone
    PAY;       // Primer pago
    
    fun getDescription(): String = when(this) {
        REGISTER -> "Al completar registro"
        VERIFY -> "After verifying email or phone"
        PAY -> "Al realizar primer pago"
    }
}

/**
 * Estado de una invitación
 */
enum class InviteStatus {
    PENDING,      // Código generado, esperando claim
    CLAIMED,      // Amigo abrió link y creó cuenta
    ACTIVATED,    // Condición de activación cumplida
    EXPIRED;      // Expiró sin ser usado
    
    fun getDisplayName(): String = when(this) {
        PENDING -> "Pendiente"
        CLAIMED -> "Reclamada"
        ACTIVATED -> "Activada"
        EXPIRED -> "Expirada"
    }
}

/**
 * Datos de referido del usuario
 */
@Parcelize
data class ReferralData(
    val activeInviteCount: Int = 0,        // Invitaciones activas actuales
    val monthKey: String = "",             // "YYYY-MM"
    val monthlyInviteCount: Int = 0,       // Invitaciones este mes
    val totalReferrals: Int = 0            // Total de referidos exitosos
) : Parcelable

/**
 * Datos de usuario en Firestore
 */
@Parcelize
data class UserPlanData(
    val uid: String = "",
    val email: String = "",
    val planTier: PlanTier = PlanTier.FREE,
    val duoPartnerUid: String? = null,          // UID del partner en Duo Shield
    val duoPartnerEmail: String? = null,        // Email del partner
    val referral: ReferralData = ReferralData(),
    val premiumExpiresAt: Long? = null,         // Timestamp de expiración premium
    val campaignBenefits: List<String> = emptyList() // IDs de campañas activas
) : Parcelable {
    
    fun isPremiumActive(): Boolean {
        if (planTier == PlanTier.DUO || planTier == PlanTier.SOLO) return true
        premiumExpiresAt?.let {
            return System.currentTimeMillis() < it
        }
        return false
    }
    
    fun hasDuoPartner(): Boolean = duoPartnerUid != null
}

/**
 * Invitación en Firestore
 */
@Parcelize
data class Invite(
    val code: String = "",                       // Código único (ej. "ABC123")
    val inviterUid: String = "",                 // Quien invita
    val inviterEmail: String = "",               // Email del invitador
    val status: InviteStatus = InviteStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = 0,                     // Timestamp de expiración
    val monthKey: String = "",                   // "YYYY-MM" para límites
    val claimedByUid: String? = null,            // UID de quien claimed
    val claimedByEmail: String? = null,          // Email de quien claimed
    val activationModeSnapshot: ActivationMode = ActivationMode.REGISTER,
    val activatedAt: Long? = null                // Timestamp cuando se activó
) : Parcelable {
    
    fun isExpired(): Boolean = System.currentTimeMillis() > expiresAt
    
    fun isActive(): Boolean = status == InviteStatus.ACTIVATED
    
    fun canBeActivated(): Boolean = 
        status == InviteStatus.CLAIMED && !isExpired()
}

/**
 * Solicitud de Duo Shield
 */
@Parcelize
data class DuoRequest(
    val id: String = "",                         // inviterUid_friendUid
    val inviterUid: String = "",
    val friendUid: String = "",
    val inviteCode: String = "",
    val status: DuoRequestStatus = DuoRequestStatus.WAITING_ACTIVATION,
    val createdAt: Long = System.currentTimeMillis(),
    val activatedAt: Long? = null
) : Parcelable

enum class DuoRequestStatus {
    WAITING_ACTIVATION,    // Waiting for activation condition
    ACTIVATED;             // Duo active
    
    fun getDisplayName(): String = when(this) {
        WAITING_ACTIVATION -> "Waiting for activation"
        ACTIVATED -> "Duo active"
    }
}

/**
 * Límites del sistema de referidos
 */
object ReferralLimits {
    const val MAX_INVITES_PER_MONTH = 1          // 1 amigo por mes
    const val MAX_ACTIVE_INVITES = 3             // Máximo 3 invitaciones activas
    const val INVITE_EXPIRATION_DAYS = 14        // 14 días para usar la invitación
}
