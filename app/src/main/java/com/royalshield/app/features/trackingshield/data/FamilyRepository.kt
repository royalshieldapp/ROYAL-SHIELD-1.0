package com.royalshield.app.features.trackingshield.data

import android.content.Context
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FamilyRepository(private val context: Context) {

    private val auth = Firebase.auth
    private val db = Firebase.database.reference

    val currentUserId: String? get() = auth.currentUser?.uid

    /**
     * Child app: links this authenticated account to a parent with a short-lived,
     * one-time pairing code. The child record and code claim are written atomically.
     */
    suspend fun linkToParent(pairingCode: String, childName: String): String? {
        val childUid = auth.currentUser?.uid
            ?: throw IllegalStateException("Authentication is required to pair a device")
        val normalizedCode = pairingCode.trim().uppercase()
        if (normalizedCode.length != PAIRING_CODE_LENGTH) return null

        val snapshot = db.child("pairing_codes").child(normalizedCode).get().await()
        val parentUid = snapshot.child("parentUid").getValue(String::class.java) ?: return null
        val expiresAt = snapshot.child("expiresAt").getValue(Long::class.java) ?: return null
        val claimedBy = snapshot.child("claimedBy").getValue(String::class.java)
        if (expiresAt <= System.currentTimeMillis() || claimedBy != null) return null

        val childId = getOrCreateChildId()
        val childData: Map<String, Any> = mapOf(
            "name" to childName.trim().take(60).ifBlank { "Child Device" },
            "id" to childId,
            "ownerUid" to childUid,
            "pairingCode" to normalizedCode,
            "status" to "SAFE",
            "last_update" to ServerValue.TIMESTAMP
        )
        val updates: Map<String, Any?> = mapOf(
            "users/$parentUid/children/$childId" to childData,
            "pairing_codes/$normalizedCode/claimedBy" to childUid,
            "pairing_codes/$normalizedCode/claimedAt" to ServerValue.TIMESTAMP
        )

        db.updateChildren(updates).await()
        com.royalshield.app.managers.PreferencesManager.setParentUid(parentUid)
        return parentUid
    }

    /** Parent app: observes its children in real time from Firebase RTDB. */
    fun observeChildren(): Flow<List<ChildDevice>> = callbackFlow {
        val uid = auth.currentUser?.uid ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val ref = db.child("users").child(uid).child("children")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val children = mutableListOf<ChildDevice>()
                snapshot.children.forEach { child ->
                    val id = child.key ?: return@forEach
                    val name = child.child("name").getValue(String::class.java) ?: "Child"
                    val lat = child.child("lat").getValue(Double::class.java) ?: 0.0
                    val lng = child.child("lng").getValue(Double::class.java) ?: 0.0
                    val statusStr = child.child("status").getValue(String::class.java) ?: "SAFE"
                    val battery = child.child("battery").getValue(Int::class.java) ?: 100
                    val speed = child.child("speed_kmh").getValue(Float::class.java) ?: 0f
                    val lastUpdate = child.child("last_update").getValue(Long::class.java)
                        ?: System.currentTimeMillis()
                    val signal = child.child("signal_strength").getValue(Int::class.java) ?: 4

                    children.add(
                        ChildDevice(
                            id = id,
                            name = name,
                            latLng = LatLng(lat, lng),
                            batteryPercent = battery,
                            signalStrength = signal,
                            gpsActive = true,
                            lastUpdateMillis = lastUpdate,
                            status = when (statusStr) {
                                "WARNING" -> ChildStatus.WARNING
                                "DANGER" -> ChildStatus.DANGER
                                else -> ChildStatus.SAFE
                            },
                            accuracyMeters = 12f,
                            speedKmh = speed
                        )
                    )
                }
                trySend(children)
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
            }
        }

        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    /** Child app: updates only its own record under the paired parent's account. */
    suspend fun updateChildLocation(
        parentUid: String,
        lat: Double,
        lng: Double,
        speedKmh: Float,
        batteryPercent: Int
    ) {
        val childId = getOrCreateChildId()
        val data: Map<String, Any> = mapOf(
            "lat" to lat.coerceIn(-90.0, 90.0),
            "lng" to lng.coerceIn(-180.0, 180.0),
            "speed_kmh" to speedKmh.coerceIn(0f, 500f),
            "battery" to batteryPercent.coerceIn(0, 100),
            "last_update" to ServerValue.TIMESTAMP,
            "status" to "SAFE",
            "signal_strength" to 4
        )
        db.child("users").child(parentUid).child("children").child(childId)
            .updateChildren(data).await()
    }

    /** Parent app: generates an authenticated one-time pairing code valid for ten minutes. */
    suspend fun generatePairingCode(): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("Authentication is required to create a pairing code")
        val code = UUID.randomUUID().toString()
            .replace("-", "")
            .take(PAIRING_CODE_LENGTH)
            .uppercase()
        val now = System.currentTimeMillis()

        db.child("pairing_codes").child(code).setValue(
            mapOf(
                "parentUid" to uid,
                "createdAt" to ServerValue.TIMESTAMP,
                "expiresAt" to now + PAIRING_CODE_TTL_MS
            )
        ).await()
        return code
    }

    /** Parent app: updates GPS polling mode for a child. */
    suspend fun updateGpsMode(childId: String, mode: GpsMode) {
        val uid = auth.currentUser?.uid ?: return
        db.child("users").child(uid).child("children").child(childId).child("gps_mode")
            .setValue(mode.name).await()
    }

    suspend fun triggerAlertSound(childId: String) {
        val uid = auth.currentUser?.uid ?: return
        db.child("users").child(uid).child("children").child(childId).child("play_sound_trigger")
            .setValue(ServerValue.TIMESTAMP).await()
    }

    fun observeAlertTrigger(parentUid: String): Flow<Long?> = callbackFlow {
        val childId = getOrCreateChildId()
        val ref = db.child("users").child(parentUid).child("children").child(childId)
            .child("play_sound_trigger")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.getValue(Long::class.java))
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun observeSafeZones(): Flow<List<SafeZone>> = callbackFlow {
        val uid = auth.currentUser?.uid ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val ref = db.child("users").child(uid).child("safe_zones")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val zones = mutableListOf<SafeZone>()
                snapshot.children.forEach { child ->
                    val id = child.key ?: return@forEach
                    val name = child.child("name").getValue(String::class.java) ?: "Zone"
                    val lat = child.child("lat").getValue(Double::class.java) ?: 0.0
                    val lng = child.child("lng").getValue(Double::class.java) ?: 0.0
                    val radius = child.child("radius").getValue(Double::class.java) ?: 200.0
                    val typeStr = child.child("type").getValue(String::class.java) ?: "HOME"
                    val enabled = child.child("enabled").getValue(Boolean::class.java) ?: true

                    zones.add(
                        SafeZone(
                            id = id,
                            name = name,
                            center = LatLng(lat, lng),
                            radiusMeters = radius,
                            type = try {
                                ZoneType.valueOf(typeStr)
                            } catch (_: IllegalArgumentException) {
                                ZoneType.HOME
                            },
                            isEnabled = enabled
                        )
                    )
                }
                trySend(zones)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun observeRiskZones(): Flow<List<RiskZone>> = callbackFlow {
        val uid = auth.currentUser?.uid ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val ref = db.child("users").child(uid).child("risk_zones")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val risks = mutableListOf<RiskZone>()
                snapshot.children.forEach { child ->
                    val id = child.key ?: return@forEach
                    val name = child.child("name").getValue(String::class.java) ?: "Risk Area"
                    val lat = child.child("lat").getValue(Double::class.java) ?: 0.0
                    val lng = child.child("lng").getValue(Double::class.java) ?: 0.0
                    val radius = child.child("radius").getValue(Double::class.java) ?: 300.0
                    val level = child.child("level").getValue(Int::class.java) ?: 1

                    risks.add(
                        RiskZone(
                            id = id,
                            name = name,
                            center = LatLng(lat, lng),
                            radiusMeters = radius,
                            threatLevel = level
                        )
                    )
                }
                trySend(risks)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun saveSafeZone(zone: SafeZone) {
        val uid = auth.currentUser?.uid ?: return
        val id = if (zone.id.isEmpty()) {
            db.child("users").child(uid).child("safe_zones").push().key
                ?: UUID.randomUUID().toString()
        } else {
            zone.id
        }
        val data = mapOf(
            "name" to zone.name.trim().take(80),
            "lat" to zone.center.latitude.coerceIn(-90.0, 90.0),
            "lng" to zone.center.longitude.coerceIn(-180.0, 180.0),
            "radius" to zone.radiusMeters.coerceIn(25.0, 50_000.0),
            "type" to zone.type.name,
            "enabled" to zone.isEnabled
        )
        db.child("users").child(uid).child("safe_zones").child(id)
            .updateChildren(data).await()
    }

    suspend fun saveRiskZone(risk: RiskZone) {
        val uid = auth.currentUser?.uid ?: return
        val id = if (risk.id.isEmpty()) {
            db.child("users").child(uid).child("risk_zones").push().key
                ?: UUID.randomUUID().toString()
        } else {
            risk.id
        }
        val data = mapOf(
            "name" to risk.name.trim().take(80),
            "lat" to risk.center.latitude.coerceIn(-90.0, 90.0),
            "lng" to risk.center.longitude.coerceIn(-180.0, 180.0),
            "radius" to risk.radiusMeters.coerceIn(25.0, 50_000.0),
            "level" to risk.threatLevel.coerceIn(1, 5)
        )
        db.child("users").child(uid).child("risk_zones").child(id)
            .updateChildren(data).await()
    }

    suspend fun deleteSafeZone(zoneId: String) {
        val uid = auth.currentUser?.uid ?: return
        db.child("users").child(uid).child("safe_zones").child(zoneId)
            .removeValue().await()
    }

    suspend fun deleteRiskZone(riskId: String) {
        val uid = auth.currentUser?.uid ?: return
        db.child("users").child(uid).child("risk_zones").child(riskId)
            .removeValue().await()
    }

    private fun getOrCreateChildId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Authentication is required for family tracking")
    }

    private companion object {
        const val PAIRING_CODE_LENGTH = 12
        const val PAIRING_CODE_TTL_MS = 10 * 60 * 1000L
    }
}
