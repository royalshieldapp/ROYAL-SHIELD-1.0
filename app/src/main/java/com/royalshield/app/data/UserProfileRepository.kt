package com.royalshield.app.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.royalshield.app.managers.PreferencesManager
import kotlinx.coroutines.tasks.await

data class UserProfile(
    val displayName: String = "",
    val email: String = "",
    val phone: String = "",
    val city: String = "",
    val country: String = "",
    val emergencyPhone: String = "",
    val photoUrl: String? = null
)

class UserProfileRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
) {
    suspend fun loadProfile(): UserProfile {
        val user = auth.currentUser ?: throw IllegalStateException("No authenticated user")
        val snapshot = database.reference
            .child("users")
            .child(user.uid)
            .child("profile")
            .get()
            .await()

        return UserProfile(
            displayName = snapshot.child("name").getValue(String::class.java)
                ?: user.displayName
                ?: PreferencesManager.getUserName().orEmpty(),
            email = user.email.orEmpty(),
            phone = snapshot.child("phone").getValue(String::class.java)
                ?: user.phoneNumber.orEmpty(),
            city = snapshot.child("city").getValue(String::class.java).orEmpty(),
            country = snapshot.child("country").getValue(String::class.java).orEmpty(),
            emergencyPhone = PreferencesManager.getEmergencyPhone().orEmpty(),
            photoUrl = user.photoUrl?.toString()
        )
    }

    suspend fun saveProfile(profile: UserProfile) {
        val user = auth.currentUser ?: throw IllegalStateException("No authenticated user")
        val cleanName = profile.displayName.trim()

        user.updateProfile(
            UserProfileChangeRequest.Builder()
                .setDisplayName(cleanName)
                .build()
        ).await()

        database.reference
            .child("users")
            .child(user.uid)
            .child("profile")
            .updateChildren(
                mapOf(
                    "name" to cleanName,
                    "email" to user.email.orEmpty(),
                    "phone" to profile.phone.trim(),
                    "city" to profile.city.trim(),
                    "country" to profile.country.trim(),
                    "updatedAt" to ServerValue.TIMESTAMP
                )
            )
            .await()

        PreferencesManager.saveUserName(cleanName)
        PreferencesManager.saveEmergencyPhone(profile.emergencyPhone.trim())
    }

    fun isAuthenticated(): Boolean = auth.currentUser != null

    fun signOut() = auth.signOut()
}
