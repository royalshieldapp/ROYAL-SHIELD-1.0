package com.royalshield.app.auth

import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class AuthProfileRepository(
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
) {
    suspend fun ensureProfile(user: FirebaseUser, preferredName: String = "") {
        val profileRef = database.reference
            .child("users")
            .child(user.uid)
            .child("profile")

        val current = profileRef.get().await()
        val currentName = current.child("name").getValue(String::class.java).orEmpty()
        val currentEmail = current.child("email").getValue(String::class.java).orEmpty()
        val currentPhone = current.child("phone").getValue(String::class.java).orEmpty()

        val resolvedName = currentName.ifBlank {
            preferredName.trim()
                .ifBlank { user.displayName.orEmpty().trim() }
                .ifBlank { "Royal Shield User" }
                .take(60)
        }

        val updates = mapOf<String, Any>(
            "name" to resolvedName,
            "email" to currentEmail.ifBlank { user.email.orEmpty().trim().take(320) },
            "phone" to currentPhone.ifBlank { user.phoneNumber.orEmpty().take(25) },
            "updatedAt" to System.currentTimeMillis()
        )
        profileRef.updateChildren(updates).await()
    }
}
