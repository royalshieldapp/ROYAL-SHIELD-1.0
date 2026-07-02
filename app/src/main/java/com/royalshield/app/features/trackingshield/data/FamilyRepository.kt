package com.royalshield.app.features.trackingshield.data

import android.content.Context
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.firebase.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FamilyRepository(private val context: Context) {

    private val auth = Firebase.auth
    private val db = Firebase.database.reference

    val currentUserId: String? get() = auth.currentUser?.uid

    /**
     * Child app: links this device to a parent via pairing code.
     * Returns the parentUid if successful, null otherwise.
     */
    suspend fun linkToParent(pairingCode: String, childName: String): String? {
        val snapshot = db.child("pairing_codes").child(pairingCode).get().await()
        val parentUid = snapshot.getValue(String::class.java) ?: return null

        val childId = getOrCreateChildId()
        val childData = mapOf(
            "name" to childName,
            "id" to childId,
            "status" to "SAFE",
            "last_update" to System.currentTimeMillis()
        )

        com.royalshield.app.managers.PreferencesManager.setParentUid(parentUid)
        
        db.child("users").child(parentUid).child("children").child(childId)
            .updateChildren(childData).await()
        return parentUid
    }

    /**
     * Parent app: observes children in real-time from Firebase RTDB.
     * Returns a Flow of ChildDevice list.
     */
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
                                "DANGER"  -> ChildStatus.DANGER
                                else      -> ChildStatus.SAFE
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

    /**
     * Child app: updates its location in the parent's RTDB node.
     */
    suspend fun updateChildLocation(
        parentUid: String,
        lat: Double,
        lng: Double,
        speedKmh: Float,
        batteryPercent: Int
    ) {
        val childId = getOrCreateChildId()
        val data = mapOf(
            "lat"        to lat,
            "lng"        to lng,
            "speed_kmh"  to speedKmh,
            "battery"    to batteryPercent,
            "last_update" to System.currentTimeMillis(),
            "status"     to "SAFE",
            "signal_strength" to 4
        )
        db.child("users").child(parentUid).child("children").child(childId)
            .updateChildren(data).await()
    }

    /**
     * Parent app: generates a one-time pairing code and stores it in RTDB.
     */
    suspend fun generatePairingCode(): String {
        val uid = auth.currentUser?.uid ?: return UUID.randomUUID().toString().take(6)
        val code = UUID.randomUUID().toString().take(6).uppercase()

        // Store: pairing_codes/{code} = parentUid (expires after use in production)
        db.child("pairing_codes").child(code).setValue(uid).await()
        return code
    }
    
    /**
     * Parent app: updates GPS polling mode for a child.
     */
    suspend fun updateGpsMode(childId: String, mode: GpsMode) {
        val uid = auth.currentUser?.uid ?: return
        db.child("users").child(uid).child("children").child(childId).child("gps_mode")
            .setValue(mode.name).await()
    }

    suspend fun triggerAlertSound(childId: String) {
        val uid = auth.currentUser?.uid ?: return
        db.child("users").child(uid).child("children").child(childId).child("play_sound_trigger")
            .setValue(System.currentTimeMillis()).await()
    }

    fun observeAlertTrigger(parentUid: String): Flow<Long?> = callbackFlow {
        val childId = getOrCreateChildId()
        val ref = db.child("users").child(parentUid).child("children").child(childId).child("play_sound_trigger")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val timestamp = snapshot.getValue(Long::class.java)
                trySend(timestamp)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    // ── Safe & Risk Zones ──────────────────────────────────
    
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
                    
                    zones.add(SafeZone(
                        id = id,
                        name = name,
                        center = LatLng(lat, lng),
                        radiusMeters = radius,
                        type = try { ZoneType.valueOf(typeStr) } catch (e: Exception) { ZoneType.HOME },
                        isEnabled = enabled
                    ))
                }
                trySend(zones)
            }
            override fun onCancelled(error: DatabaseError) {}
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
                    
                    risks.add(RiskZone(
                        id = id,
                        name = name,
                        center = LatLng(lat, lng),
                        radiusMeters = radius,
                        threatLevel = level
                    ))
                }
                trySend(risks)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun saveSafeZone(zone: SafeZone) {
        val uid = auth.currentUser?.uid ?: return
        val id = if (zone.id.isEmpty()) db.child("users").child(uid).child("safe_zones").push().key ?: UUID.randomUUID().toString() else zone.id
        val data = mapOf(
            "name" to zone.name,
            "lat" to zone.center.latitude,
            "lng" to zone.center.longitude,
            "radius" to zone.radiusMeters,
            "type" to zone.type.name,
            "enabled" to zone.isEnabled
        )
        db.child("users").child(uid).child("safe_zones").child(id).updateChildren(data).await()
    }

    suspend fun saveRiskZone(risk: RiskZone) {
        val uid = auth.currentUser?.uid ?: return
        val id = if (risk.id.isEmpty()) db.child("users").child(uid).child("risk_zones").push().key ?: UUID.randomUUID().toString() else risk.id
        val data = mapOf(
            "name" to risk.name,
            "lat" to risk.center.latitude,
            "lng" to risk.center.longitude,
            "radius" to risk.radiusMeters,
            "level" to risk.threatLevel
        )
        db.child("users").child(uid).child("risk_zones").child(id).updateChildren(data).await()
    }

    suspend fun deleteSafeZone(zoneId: String) {
        val uid = auth.currentUser?.uid ?: return
        db.child("users").child(uid).child("safe_zones").child(zoneId).removeValue().await()
    }

    suspend fun deleteRiskZone(riskId: String) {
        val uid = auth.currentUser?.uid ?: return
        db.child("users").child(uid).child("risk_zones").child(riskId).removeValue().await()
    }

    // Stable per-installation child ID
    private fun getOrCreateChildId(): String {
        return android.os.Build.MODEL + "_" +
            android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            )
    }
}
