package com.royalshield.app.features.ir.data

import android.content.Context
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonSerializer
import com.google.gson.reflect.TypeToken
import com.royalshield.app.RoyalShieldApp
import com.royalshield.app.features.ir.models.ChecklistItem
import com.royalshield.app.features.ir.models.Incident
import com.royalshield.app.features.ir.models.IncidentSeverity
import com.royalshield.app.features.ir.models.IncidentStatus
import com.royalshield.app.features.ir.models.IncidentType
import com.royalshield.app.features.ir.models.TimelineEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.tasks.await
import java.lang.reflect.Type
import java.util.Date
import java.util.UUID

class TimestampAdapter : JsonSerializer<Timestamp>, JsonDeserializer<Timestamp> {
    override fun serialize(src: Timestamp, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val obj = JsonObject()
        obj.addProperty("seconds", src.seconds)
        obj.addProperty("nanoseconds", src.nanoseconds)
        return obj
    }

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Timestamp {
        val obj = json.asJsonObject
        val seconds = obj.get("seconds").asLong
        val nanoseconds = obj.get("nanoseconds").asInt
        return Timestamp(seconds, nanoseconds)
    }
}

class IrRepository {
    private val firestore = try { FirebaseFirestore.getInstance() } catch (e: Exception) { null }
    private val auth = try { FirebaseAuth.getInstance() } catch (e: Exception) { null }
    private val userId: String? get() = try { auth?.currentUser?.uid } catch (e: Exception) { null }

    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(Timestamp::class.java, TimestampAdapter())
        .create()

    private val sharedPrefs by lazy {
        RoyalShieldApp.instance.getSharedPreferences("royal_shield_ir_prefs", Context.MODE_PRIVATE)
    }

    private val incidentsFlow = MutableStateFlow<List<Incident>>(emptyList())
    private val timelineFlows = mutableMapOf<String, MutableStateFlow<List<TimelineEvent>>>()
    private val checklistFlows = mutableMapOf<String, MutableStateFlow<List<ChecklistItem>>>()

    init {
        initializeMockData()
        refreshLocalFlows()
    }

    private fun getIrCollection() = userId?.let { uid ->
        firestore?.collection("users")?.document(uid)?.collection("ir")
    }

    private fun initializeMockData() {
        val currentIncidents = getLocalIncidents()
        if (currentIncidents.isEmpty()) {
            val mockIncidents = listOf(
                Incident(
                    id = "mock_incident_1",
                    title = "Phishing Campaign Detected",
                    type = IncidentType.PHISHING_BEC,
                    severity = IncidentSeverity.HIGH,
                    status = IncidentStatus.ACTIVE,
                    affectedAssets = "Corporate Email Server",
                    notes = "CEO spoofing email targeting finance department.",
                    createdAt = Timestamp.now()
                ),
                Incident(
                    id = "mock_incident_2",
                    title = "Database Breach Attempt",
                    type = IncidentType.DATA_BREACH,
                    severity = IncidentSeverity.HIGH,
                    status = IncidentStatus.MITIGATED,
                    affectedAssets = "User Database",
                    notes = "Suspicious queries detected from external IP address.",
                    createdAt = Timestamp(System.currentTimeMillis() / 1000 - 86400, 0)
                )
            )
            saveLocalIncidents(mockIncidents)

            // Timeline for mock 1
            val timeline1 = listOf(
                TimelineEvent(id = "e1", timestamp = Timestamp.now(), actionTaken = "Reported by employee", who = "Security Operations Center"),
                TimelineEvent(id = "e2", timestamp = Timestamp.now(), actionTaken = "Email accounts quarantined", who = "IT Admin")
            )
            saveLocalTimeline("mock_incident_1", timeline1)

            // Checklist for mock 1
            val checklist1 = listOf(
                ChecklistItem(id = "c1", title = "Identify compromised email account", isCompleted = true, type = IncidentType.PHISHING_BEC),
                ChecklistItem(id = "c2", title = "Reset passwords and force MFA logout", isCompleted = true, type = IncidentType.PHISHING_BEC),
                ChecklistItem(id = "c3", title = "Check email forwarding rules", isCompleted = false, type = IncidentType.PHISHING_BEC),
                ChecklistItem(id = "c4", title = "Notify financial department (if wire transfer risk)", isCompleted = false, type = IncidentType.PHISHING_BEC),
                ChecklistItem(id = "c5", title = "Scan device for malware", isCompleted = false, type = IncidentType.PHISHING_BEC)
            )
            saveLocalChecklist("mock_incident_1", checklist1)

            // Timeline for mock 2
            val timeline2 = listOf(
                TimelineEvent(id = "e3", timestamp = Timestamp(System.currentTimeMillis() / 1000 - 86400, 0), actionTaken = "Anomalous login alert triggered", who = "SIEM System"),
                TimelineEvent(id = "e4", timestamp = Timestamp(System.currentTimeMillis() / 1000 - 86000, 0), actionTaken = "IP blocked at firewall", who = "Network Engineer")
            )
            saveLocalTimeline("mock_incident_2", timeline2)

            // Checklist for mock 2
            val checklist2 = listOf(
                ChecklistItem(id = "c6", title = "Identify scope of leaked data", isCompleted = true, type = IncidentType.DATA_BREACH),
                ChecklistItem(id = "c7", title = "Secure remaining data repositories", isCompleted = true, type = IncidentType.DATA_BREACH),
                ChecklistItem(id = "c8", title = "Consult legal for notification requirements", isCompleted = false, type = IncidentType.DATA_BREACH),
                ChecklistItem(id = "c9", title = "Audit access logs", isCompleted = true, type = IncidentType.DATA_BREACH),
                ChecklistItem(id = "c10", title = "Change all administrative credentials", isCompleted = false, type = IncidentType.DATA_BREACH)
            )
            saveLocalChecklist("mock_incident_2", checklist2)
        }
    }

    private fun refreshLocalFlows() {
        val incidents = getLocalIncidents().sortedByDescending { it.createdAt }
        incidentsFlow.value = incidents
    }

    private fun getLocalIncidents(): List<Incident> {
        val json = sharedPrefs.getString("incidents", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<Incident>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveLocalIncidents(list: List<Incident>) {
        sharedPrefs.edit().putString("incidents", gson.toJson(list)).apply()
        incidentsFlow.value = list.sortedByDescending { it.createdAt }
    }

    private fun getLocalTimeline(incidentId: String): List<TimelineEvent> {
        val json = sharedPrefs.getString("timeline_$incidentId", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<TimelineEvent>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveLocalTimeline(incidentId: String, list: List<TimelineEvent>) {
        sharedPrefs.edit().putString("timeline_$incidentId", gson.toJson(list)).apply()
        getTimelineFlow(incidentId).value = list.sortedBy { it.timestamp }
    }

    private fun getLocalChecklist(incidentId: String): List<ChecklistItem> {
        val json = sharedPrefs.getString("checklist_$incidentId", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<ChecklistItem>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveLocalChecklist(incidentId: String, list: List<ChecklistItem>) {
        sharedPrefs.edit().putString("checklist_$incidentId", gson.toJson(list)).apply()
        getChecklistFlow(incidentId).value = list
    }

    @Synchronized
    private fun getTimelineFlow(incidentId: String): MutableStateFlow<List<TimelineEvent>> {
        return timelineFlows.getOrPut(incidentId) {
            MutableStateFlow(getLocalTimeline(incidentId))
        }
    }

    @Synchronized
    private fun getChecklistFlow(incidentId: String): MutableStateFlow<List<ChecklistItem>> {
        return checklistFlows.getOrPut(incidentId) {
            MutableStateFlow(getLocalChecklist(incidentId))
        }
    }

    fun resetData() {
        sharedPrefs.edit().clear().apply()
        timelineFlows.clear()
        checklistFlows.clear()
        initializeMockData()
        refreshLocalFlows()
    }

    // Incidents
    fun getIncidents(): Flow<List<Incident>> {
        val uid = userId
        if (uid != null && firestore != null) {
            val collection = getIrCollection()?.document("data")?.collection("incidents")
                ?.orderBy("createdAt", Query.Direction.DESCENDING)
            collection?.addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    val incidents = snapshot.toObjects(Incident::class.java)
                    if (incidents.isNotEmpty()) {
                        val localList = getLocalIncidents().toMutableList()
                        incidents.forEach { remote ->
                            val index = localList.indexOfFirst { it.id == remote.id }
                            if (index >= 0) {
                                localList[index] = remote
                            } else {
                                localList.add(remote)
                            }
                        }
                        saveLocalIncidents(localList)
                    }
                }
            }
        }
        return incidentsFlow
    }

    suspend fun createIncident(incident: Incident): String {
        val localId = "local_" + UUID.randomUUID().toString()
        val finalIncident = incident.copy(id = localId, createdAt = Timestamp.now())

        // Save locally first
        val localList = getLocalIncidents().toMutableList()
        localList.add(finalIncident)
        saveLocalIncidents(localList)

        // Populate checklist locally
        populateChecklist(localId, finalIncident.type)

        // Try Firestore in background/suspend if authenticated
        val uid = userId
        if (uid != null && firestore != null) {
            try {
                val docRef = getIrCollection()?.document("data")?.collection("incidents")?.document()
                if (docRef != null) {
                    val remoteIncident = finalIncident.copy(id = docRef.id)
                    docRef.set(remoteIncident).await()
                    val updatedList = getLocalIncidents().toMutableList()
                    val index = updatedList.indexOfFirst { it.id == localId }
                    if (index >= 0) {
                        updatedList[index] = remoteIncident
                        saveLocalIncidents(updatedList)
                        val checklist = getLocalChecklist(localId)
                        saveLocalChecklist(remoteIncident.id, checklist)
                        val timeline = getLocalTimeline(localId)
                        saveLocalTimeline(remoteIncident.id, timeline)
                    }
                    populateChecklistRemote(remoteIncident.id, remoteIncident.type)
                    return remoteIncident.id
                }
            } catch (e: Exception) {
                // Ignore remote error, we already saved locally
            }
        }

        return localId
    }

    suspend fun getIncident(incidentId: String): Incident? {
        val local = getLocalIncidents().find { it.id == incidentId }
        if (local != null) return local

        val uid = userId
        if (uid != null && firestore != null) {
            try {
                val remote = getIrCollection()?.document("data")?.collection("incidents")?.document(incidentId)
                    ?.get()?.await()?.toObject(Incident::class.java)
                if (remote != null) {
                    val localList = getLocalIncidents().toMutableList()
                    if (localList.none { it.id == remote.id }) {
                        localList.add(remote)
                        saveLocalIncidents(localList)
                    }
                    return remote
                }
            } catch (e: Exception) {
                // ignore
            }
        }
        return null
    }

    // Timeline
    fun getTimeline(incidentId: String): Flow<List<TimelineEvent>> {
        val flow = getTimelineFlow(incidentId)
        val uid = userId
        if (uid != null && firestore != null) {
            val collection = getIrCollection()?.document("data")?.collection("incidents")
                ?.document(incidentId)?.collection("timeline")
                ?.orderBy("timestamp", Query.Direction.ASCENDING)
            collection?.addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    val events = snapshot.toObjects(TimelineEvent::class.java)
                    if (events.isNotEmpty()) {
                        saveLocalTimeline(incidentId, events)
                    }
                }
            }
        }
        return flow
    }

    suspend fun addTimelineEvent(incidentId: String, event: TimelineEvent) {
        val localId = "local_event_" + UUID.randomUUID().toString()
        val finalEvent = event.copy(id = localId, timestamp = Timestamp.now())

        val list = getLocalTimeline(incidentId).toMutableList()
        list.add(finalEvent)
        saveLocalTimeline(incidentId, list)

        val uid = userId
        if (uid != null && firestore != null) {
            try {
                val docRef = getIrCollection()?.document("data")?.collection("incidents")
                    ?.document(incidentId)?.collection("timeline")?.document()
                if (docRef != null) {
                    val remoteEvent = finalEvent.copy(id = docRef.id)
                    docRef.set(remoteEvent).await()
                }
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    // Checklist
    fun getChecklist(incidentId: String): Flow<List<ChecklistItem>> {
        val flow = getChecklistFlow(incidentId)
        val uid = userId
        if (uid != null && firestore != null) {
            val collection = getIrCollection()?.document("data")?.collection("incidents")
                ?.document(incidentId)?.collection("checklist")
            collection?.addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    val items = snapshot.toObjects(ChecklistItem::class.java)
                    if (items.isNotEmpty()) {
                        saveLocalChecklist(incidentId, items)
                    }
                }
            }
        }
        return flow
    }

    suspend fun toggleChecklistItem(incidentId: String, itemId: String, completed: Boolean) {
        val list = getLocalChecklist(incidentId).toMutableList()
        val index = list.indexOfFirst { it.id == itemId }
        if (index >= 0) {
            list[index] = list[index].copy(isCompleted = completed)
            saveLocalChecklist(incidentId, list)
        }

        val uid = userId
        if (uid != null && firestore != null) {
            try {
                getIrCollection()?.document("data")?.collection("incidents")
                    ?.document(incidentId)?.collection("checklist")?.document(itemId)
                    ?.update("isCompleted", completed)?.await()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    private fun populateChecklist(incidentId: String, type: IncidentType) {
        val items = when (type) {
            IncidentType.RANSOMWARE -> listOf(
                "Isolate infected devices from network",
                "Disconnect backup drives (prevent encryption spread)",
                "Identify ransomware strain",
                "Check for decryption tools",
                "Contact legal and law enforcement",
                "Restore from offline backups"
            )
            IncidentType.PHISHING_BEC -> listOf(
                "Identify compromised email account",
                "Reset passwords and force MFA logout",
                "Check email forwarding rules",
                "Notify financial department (if wire transfer risk)",
                "Scan device for malware",
                "Alert recipients of phishing emails"
            )
            IncidentType.DATA_BREACH -> listOf(
                "Identify scope of leaked data",
                "Secure remaining data repositories",
                "Consult legal for notification requirements",
                "Audit access logs",
                "Change all administrative credentials"
            )
            else -> listOf(
                "Verify incident occurrence",
                "Document initial findings",
                "Containment: isolate affected systems",
                "Eradication: remove cause of incident",
                "Recovery: restore systems",
                "Post-incident review"
            )
        }

        val checklistItems = items.mapIndexed { idx, title ->
            ChecklistItem(id = "local_chk_${idx}_${UUID.randomUUID()}", title = title, isCompleted = false, type = type)
        }
        saveLocalChecklist(incidentId, checklistItems)
    }

    private suspend fun populateChecklistRemote(incidentId: String, type: IncidentType) {
        val items = when (type) {
            IncidentType.RANSOMWARE -> listOf(
                "Isolate infected devices from network",
                "Disconnect backup drives (prevent encryption spread)",
                "Identify ransomware strain",
                "Check for decryption tools",
                "Contact legal and law enforcement",
                "Restore from offline backups"
            )
            IncidentType.PHISHING_BEC -> listOf(
                "Identify compromised email account",
                "Reset passwords and force MFA logout",
                "Check email forwarding rules",
                "Notify financial department (if wire transfer risk)",
                "Scan device for malware",
                "Alert recipients of phishing emails"
            )
            IncidentType.DATA_BREACH -> listOf(
                "Identify scope of leaked data",
                "Secure remaining data repositories",
                "Consult legal for notification requirements",
                "Audit access logs",
                "Change all administrative credentials"
            )
            else -> listOf(
                "Verify incident occurrence",
                "Document initial findings",
                "Containment: isolate affected systems",
                "Eradication: remove cause of incident",
                "Recovery: restore systems",
                "Post-incident review"
            )
        }

        val checklistColl = getIrCollection()?.document("data")?.collection("incidents")
            ?.document(incidentId)?.collection("checklist") ?: return

        items.forEach { title ->
            val docRef = checklistColl.document()
            docRef.set(ChecklistItem(id = docRef.id, title = title, type = type)).await()
        }
    }
}

