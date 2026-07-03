package com.royalshield.app.features.intel.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.royalshield.app.features.intel.models.AppSettings
import com.royalshield.app.features.intel.models.IOC
import com.royalshield.app.features.intel.models.IntelAlert
import com.royalshield.app.features.intel.models.Monitor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class IntelRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val userId: String? get() = auth.currentUser?.uid

    private fun getIntelCollection() = userId?.let { uid ->
        firestore.collection("users").document(uid).collection("intel")
    }

    // Monitors
    fun getMonitors(): Flow<List<Monitor>> = callbackFlow {
        val collection = getIntelCollection()?.document("data")?.collection("monitors")
            ?.orderBy("createdAt", Query.Direction.DESCENDING)
            
        val listener = collection?.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            val list = snapshot?.toObjects(Monitor::class.java) ?: emptyList()
            trySend(list)
        }
        awaitClose { listener?.remove() }
    }

    suspend fun addMonitor(monitor: Monitor) {
        val docRef = getIntelCollection()?.document("data")?.collection("monitors")?.document() ?: return
        docRef.set(monitor.copy(id = docRef.id)).await()
    }

    // IOCs
    fun getIocs(): Flow<List<IOC>> = callbackFlow {
        val collection = getIntelCollection()?.document("data")?.collection("iocs")
            ?.orderBy("createdAt", Query.Direction.DESCENDING)
            
        val listener = collection?.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            val list = snapshot?.toObjects(IOC::class.java) ?: emptyList()
            trySend(list)
        }
        awaitClose { listener?.remove() }
    }

    suspend fun addIoc(ioc: IOC) {
        val docRef = getIntelCollection()?.document("data")?.collection("iocs")?.document() ?: return
        docRef.set(ioc.copy(id = docRef.id)).await()
    }

    // Alerts
    fun getAlerts(): Flow<List<IntelAlert>> = callbackFlow {
        val collection = getIntelCollection()?.document("data")?.collection("alerts")
            ?.orderBy("createdAt", Query.Direction.DESCENDING)
            
        val listener = collection?.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            val list = snapshot?.toObjects(IntelAlert::class.java) ?: emptyList()
            trySend(list)
        }
        awaitClose { listener?.remove() }
    }

    suspend fun createAlert(alert: IntelAlert) {
        val docRef = getIntelCollection()?.document("data")?.collection("alerts")?.document() ?: return
        docRef.set(alert.copy(id = docRef.id)).await()
    }

    suspend fun getAlert(alertId: String): IntelAlert? {
        return getIntelCollection()?.document("data")?.collection("alerts")?.document(alertId)
            ?.get()?.await()?.toObject(IntelAlert::class.java)
    }
}

class SettingsRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val userId: String? get() = auth.currentUser?.uid

    private fun getSettingsDoc() = userId?.let { uid ->
        firestore.collection("users").document(uid).collection("settings").document("appSettings")
    }

    fun getAppSettings(): Flow<AppSettings> = callbackFlow {
        val doc = getSettingsDoc()
        val listener = doc?.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            val settings = snapshot?.toObject(AppSettings::class.java) ?: AppSettings()
            trySend(settings)
        }
        awaitClose { listener?.remove() }
    }

    suspend fun updateComplianceMode(enabled: Boolean) {
        getSettingsDoc()?.set(AppSettings(complianceMode = enabled))?.await()
    }
}
