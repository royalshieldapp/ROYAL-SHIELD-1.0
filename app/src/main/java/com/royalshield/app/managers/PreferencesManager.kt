package com.royalshield.app.managers

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.royalshield.app.BuildConfig
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Singleton to handle all app preferences.
 * - securePrefs (EncryptedSharedPreferences): datos sensibles (phone, UID, API keys)
 * - prefs (SharedPreferences): configuraciones de UI no sensibles
 */
object PreferencesManager {
    private const val PREFS_NAME = "royal_shield_prefs"
    private const val SECURE_PREFS_NAME = "royal_shield_secure_prefs"

    // Keys for SharedPreferences
    private const val KEY_USER_NAME = "key_user_name"
    private const val KEY_EMERGENCY_PHONE = "key_emergency_phone"
    private const val KEY_VIBRATION_ENABLED = "key_vibration_enabled"
    private const val KEY_HISTORY_ENABLED = "key_history_enabled"
    private const val KEY_SOS_HISTORY = "key_sos_history"
    private const val KEY_LOCATION_TIMELINE = "key_location_timeline"
    private const val KEY_VIRUSTOTAL_API_KEY = "key_virustotal_api_key"
    private const val KEY_ALIENVAULT_API_KEY = "key_alienvault_api_key"
    private const val KEY_TWILIO_ACCOUNT_SID = "key_twilio_account_sid"
    private const val KEY_TWILIO_AUTH_TOKEN = "key_twilio_auth_token"
    private const val KEY_TWILIO_FROM_NUMBER = "key_twilio_from_number"
    private const val KEY_DID_API_KEY = "key_did_api_key"
    private const val KEY_DID_AGENT_ID = "key_did_agent_id"

    // System Configurations
    private const val KEY_DARK_MODE_ENABLED = "key_dark_mode_enabled"
    const val KEY_THEME_STYLE = "key_theme_style" // Made Public
    private const val KEY_NOTIFICATIONS_ENABLED = "key_notifications_enabled"
    private const val KEY_SOUND_ENABLED = "key_sound_enabled"
    private const val KEY_LOCATION_ENABLED = "key_location_enabled"
    private const val KEY_BACKGROUND_CAMERA_ENABLED = "key_background_camera_enabled"
    private const val KEY_PURCHASED_PACK = "key_purchased_pack"
    const val KEY_SYSTEM_ARMED = "key_system_armed"

    private lateinit var appContext: Context
    private lateinit var prefs: SharedPreferences
    private var _securePrefs: SharedPreferences? = null

    private val securePrefs: SharedPreferences
        get() {
            var sp = _securePrefs
            if (sp == null) {
                synchronized(this) {
                    sp = _securePrefs
                    if (sp == null) {
                        sp = createSecurePrefs(appContext)
                        _securePrefs = sp
                    }
                }
            }
            return sp!!
        }

    /**
     * Initializes the PreferencesManager. Must be called in Application or before use.
     */
    fun init(context: Context) {
        appContext = context.applicationContext
        prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        // Initialize securePrefs on a background thread to warm up the KeyStore / MasterKey and avoid startup ANR
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val warm = securePrefs
                Log.d("PreferencesManager", "securePrefs initialized in background successfully")
            } catch (e: Exception) {
                Log.e("PreferencesManager", "Background pre-initialization of securePrefs failed", e)
            }
        }
    }

    private fun createSecurePrefs(context: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                SECURE_PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fallback: si el dispositivo no soporta encryption (raro), usa prefs normales
            Log.w("PreferencesManager", "EncryptedSharedPreferences not available, falling back to standard prefs", e)
            context.getSharedPreferences("${SECURE_PREFS_NAME}_fallback", Context.MODE_PRIVATE)
        }
    }

    private fun isInitialized(): Boolean {
        return ::prefs.isInitialized
    }

    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        if (isInitialized()) prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        if (isInitialized()) prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    // ===== User Name Management (no sensible — nombre de perfil) =====

    fun saveUserName(name: String) {
        if (isInitialized()) prefs.edit().putString(KEY_USER_NAME, name).apply()
    }

    fun getUserName(): String? {
        return if (isInitialized()) prefs.getString(KEY_USER_NAME, null) else null
    }

    // ===== Emergency Phone Management (SENSIBLE → securePrefs) =====

    fun saveEmergencyPhone(phone: String) {
        if (isInitialized()) securePrefs.edit().putString(KEY_EMERGENCY_PHONE, phone).apply()
    }

    fun getEmergencyPhone(): String? {
        return if (isInitialized()) securePrefs.getString(KEY_EMERGENCY_PHONE, null) else null
    }

    // ===== Emergency Contacts Management (SENSIBLE → securePrefs) =====
    private const val KEY_EMERGENCY_CONTACTS = "key_emergency_contacts"
    
    data class EmergencyContact(
        val id: String,
        val name: String,
        val phone: String
    )
    
    fun addEmergencyContact(contact: EmergencyContact) {
        if (!isInitialized()) return

        val currentContacts = getEmergencyContacts().toMutableList()
        currentContacts.add(contact)
        val jsonArray = JSONArray()
        currentContacts.forEach { c ->
            val jsonContact = JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                put("phone", c.phone)
            }
            jsonArray.put(jsonContact)
        }
        // Contactos de emergencia son sensibles → securePrefs
        securePrefs.edit().putString(KEY_EMERGENCY_CONTACTS, jsonArray.toString()).apply()
    }

    fun getEmergencyContacts(): List<EmergencyContact> {
        if (!isInitialized()) return emptyList()
        val contactsJson = securePrefs.getString(KEY_EMERGENCY_CONTACTS, null) ?: return emptyList()
        
        return try {
            val jsonArray = JSONArray(contactsJson)
            val contacts = mutableListOf<EmergencyContact>()
            
            for (i in 0 until jsonArray.length()) {
                val jsonContact = jsonArray.getJSONObject(i)
                contacts.add(
                    EmergencyContact(
                        id = jsonContact.getString("id"),
                        name = jsonContact.getString("name"),
                        phone = jsonContact.getString("phone")
                    )
                )
            }
            
            contacts
        } catch (e: Exception) {
            Log.e("PreferencesManager", "Error parsing emergency contacts")
            emptyList()
        }
    }
    
    fun deleteEmergencyContact(contactId: String) {
        if (!isInitialized()) return
        val currentContacts = getEmergencyContacts().toMutableList()
        currentContacts.removeAll { it.id == contactId }
        val jsonArray = JSONArray()
        currentContacts.forEach { c ->
            val jsonContact = JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                put("phone", c.phone)
            }
            jsonArray.put(jsonContact)
        }
        securePrefs.edit().putString(KEY_EMERGENCY_CONTACTS, jsonArray.toString()).apply()
    }
    
    // ===== App Configurations =====
    
    fun setVibrationEnabled(enabled: Boolean) {
        if (isInitialized()) prefs.edit().putBoolean(KEY_VIBRATION_ENABLED, enabled).apply()
    }
    
    fun isVibrationEnabled(): Boolean {
        return if (isInitialized()) prefs.getBoolean(KEY_VIBRATION_ENABLED, true) else true // Default: enabled
    }
    
    fun setHistoryEnabled(enabled: Boolean) {
        if (isInitialized()) prefs.edit().putBoolean(KEY_HISTORY_ENABLED, enabled).apply()
    }
    
    fun isHistoryEnabled(): Boolean {
        return if (isInitialized()) prefs.getBoolean(KEY_HISTORY_ENABLED, true) else true // Default: enabled
    }

    // ===== System Preferences =====
    
    fun setDarkModeEnabled(enabled: Boolean) {
        if (isInitialized()) prefs.edit().putBoolean(KEY_DARK_MODE_ENABLED, enabled).apply()
    }

    fun isDarkModeEnabled(): Boolean {
        // Default: enabled (Neon theme is dark)
        return if (isInitialized()) prefs.getBoolean(KEY_DARK_MODE_ENABLED, true) else true
    }

    fun setThemeStyle(style: String) {
        if (isInitialized()) prefs.edit().putString(KEY_THEME_STYLE, style).apply()
    }

    fun getThemeStyle(): String {
        return if (isInitialized()) prefs.getString(KEY_THEME_STYLE, "Neon") ?: "Neon" else "Neon"
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        if (isInitialized()) prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
    }

    fun areNotificationsEnabled(): Boolean {
        return if (isInitialized()) prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true) else true
    }

    fun setSoundEnabled(enabled: Boolean) {
        if (isInitialized()) prefs.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()
    }

    fun isSoundEnabled(): Boolean {
        return if (isInitialized()) prefs.getBoolean(KEY_SOUND_ENABLED, true) else true
    }

    fun setLocationEnabled(enabled: Boolean) {
        if (isInitialized()) prefs.edit().putBoolean(KEY_LOCATION_ENABLED, enabled).apply()
    }

    fun isLocationEnabled(): Boolean {
        return if (isInitialized()) prefs.getBoolean(KEY_LOCATION_ENABLED, true) else true
    }

    fun setBackgroundCameraEnabled(enabled: Boolean) {
        if (isInitialized()) prefs.edit().putBoolean(KEY_BACKGROUND_CAMERA_ENABLED, enabled).apply()
    }

    fun isBackgroundCameraEnabled(): Boolean {
        return if (isInitialized()) prefs.getBoolean(KEY_BACKGROUND_CAMERA_ENABLED, true) else true
    }

    // ===== Purchase / Pack Management (SENSIBLE → securePrefs) =====

    fun savePurchasedPack(packId: String) {
        if (isInitialized()) securePrefs.edit().putString(KEY_PURCHASED_PACK, packId).apply()
    }

    fun getPurchasedPack(): String? {
        return if (isInitialized()) securePrefs.getString(KEY_PURCHASED_PACK, null) else null
    }
    
    // ===== VirusTotal API Key (SENSIBLE → securePrefs) =====

    fun saveVirusTotalApiKey(key: String) {
        if (isInitialized()) securePrefs.edit().putString(KEY_VIRUSTOTAL_API_KEY, key).apply()
    }

    fun getVirusTotalApiKey(): String? {
        if (!isInitialized()) return BuildConfig.VIRUSTOTAL_API_KEY.ifBlank { null }
        val key = securePrefs.getString(KEY_VIRUSTOTAL_API_KEY, null)
        return if (key.isNullOrBlank()) BuildConfig.VIRUSTOTAL_API_KEY.ifBlank { null } else key
    }

    // ===== OpenAI API Key (SENSIBLE → securePrefs) =====
    private const val KEY_OPENAI_API_KEY = "key_openai_api_key"

    fun saveOpenAiApiKey(key: String) {
        if (isInitialized()) securePrefs.edit().putString(KEY_OPENAI_API_KEY, key).apply()
    }

    fun getOpenAiApiKey(): String? {
        return if (isInitialized()) securePrefs.getString(KEY_OPENAI_API_KEY, null) else null
    }

    // ===== Google Gemini API Key (SENSIBLE → securePrefs) =====
    private const val KEY_GEMINI_API_KEY = "key_gemini_api_key"

    fun saveGeminiApiKey(key: String) {
        if (isInitialized()) securePrefs.edit().putString(KEY_GEMINI_API_KEY, key).apply()
    }

    fun getGeminiApiKey(): String? {
        if (!isInitialized()) return BuildConfig.GEMINI_API_KEY.ifBlank { null }
        val key = securePrefs.getString(KEY_GEMINI_API_KEY, null)
        return if (key.isNullOrBlank()) BuildConfig.GEMINI_API_KEY.ifBlank { null } else key
    }

    // ===== AlienVault API Key (SENSIBLE → securePrefs) =====

    fun saveAlienVaultApiKey(key: String) {
        if (isInitialized()) securePrefs.edit().putString(KEY_ALIENVAULT_API_KEY, key).apply()
    }

    fun getAlienVaultApiKey(): String? {
        if (!isInitialized()) return BuildConfig.ALIENVAULT_API_KEY.ifBlank { null }
        val key = securePrefs.getString(KEY_ALIENVAULT_API_KEY, null)
        return if (key.isNullOrBlank()) BuildConfig.ALIENVAULT_API_KEY.ifBlank { null } else key
    }

    // ===== Twilio Credentials (SENSIBLE → securePrefs) =====

    fun saveTwilioAccountSid(sid: String) {
        if (isInitialized()) securePrefs.edit().putString(KEY_TWILIO_ACCOUNT_SID, sid).apply()
    }

    fun getTwilioAccountSid(): String? {
        if (!isInitialized()) return BuildConfig.TWILIO_ACCOUNT_SID.ifBlank { null }
        val sid = securePrefs.getString(KEY_TWILIO_ACCOUNT_SID, null)
        return if (sid.isNullOrBlank()) BuildConfig.TWILIO_ACCOUNT_SID.ifBlank { null } else sid
    }

    fun saveTwilioAuthToken(token: String) {
        if (isInitialized()) securePrefs.edit().putString(KEY_TWILIO_AUTH_TOKEN, token).apply()
    }

    fun getTwilioAuthToken(): String? {
        if (!isInitialized()) return BuildConfig.TWILIO_AUTH_TOKEN.ifBlank { null }
        val token = securePrefs.getString(KEY_TWILIO_AUTH_TOKEN, null)
        return if (token.isNullOrBlank()) BuildConfig.TWILIO_AUTH_TOKEN.ifBlank { null } else token
    }

    fun saveTwilioFromNumber(number: String) {
        if (isInitialized()) securePrefs.edit().putString(KEY_TWILIO_FROM_NUMBER, number).apply()
    }

    fun getTwilioFromNumber(): String? {
        if (!isInitialized()) return null
        val number = securePrefs.getString(KEY_TWILIO_FROM_NUMBER, null)
        return if (number.isNullOrBlank()) null else number
    }

    // ===== D-ID API Key (SENSIBLE → securePrefs) =====

    fun saveDidApiKey(key: String) {
        if (isInitialized()) securePrefs.edit().putString(KEY_DID_API_KEY, key).apply()
    }

    fun getDidApiKey(): String? {
        if (!isInitialized()) return BuildConfig.DID_API_KEY.ifBlank { null }
        val key = securePrefs.getString(KEY_DID_API_KEY, null)
        return if (key.isNullOrBlank()) BuildConfig.DID_API_KEY.ifBlank { null } else key
    }

    // ===== D-ID Agent ID (non-sensible, public ID) =====

    fun saveDidAgentId(id: String) {
        if (isInitialized()) prefs.edit().putString(KEY_DID_AGENT_ID, id).apply()
    }

    fun getDidAgentId(): String? {
        return if (isInitialized()) prefs.getString(KEY_DID_AGENT_ID, "v2_agt_2oj6mK00") else "v2_agt_2oj6mK00"
    }

    // ===== Plan Tier Cache (for quick paywall checks) =====
    private const val KEY_USER_PLAN_TIER = "key_user_plan_tier"

    fun getUserPlanTier(): String {
        return if (isInitialized()) prefs.getString(KEY_USER_PLAN_TIER, "FREE") ?: "FREE" else "FREE"
    }

    fun setUserPlanTier(tier: String) {
        if (isInitialized()) prefs.edit().putString(KEY_USER_PLAN_TIER, tier).apply()
    }

    fun clearUserPlanTier() {
        if (isInitialized()) prefs.edit().remove(KEY_USER_PLAN_TIER).apply()
    }

    fun isPremiumUser(): Boolean {
        val tier = getUserPlanTier()
        return tier == "SOLO" || tier == "DUO"
    }

    // ===== Loyalty Cache =====
    private const val KEY_LOYALTY_POINTS = "key_loyalty_points"
    private const val KEY_LOYALTY_TIER = "key_loyalty_tier"

    fun getLoyaltyPoints(): Int {
        return if (isInitialized()) prefs.getInt(KEY_LOYALTY_POINTS, 0) else 0
    }

    fun saveLoyaltyPoints(points: Int) {
        if (isInitialized()) prefs.edit().putInt(KEY_LOYALTY_POINTS, points).apply()
    }

    fun getLoyaltyTier(): String {
        return if (isInitialized()) prefs.getString(KEY_LOYALTY_TIER, "Starter") ?: "Starter" else "Starter"
    }

    fun saveLoyaltyTier(tier: String) {
        if (isInitialized()) prefs.edit().putString(KEY_LOYALTY_TIER, tier).apply()
    }


    // ===== Family Tracking Role =====
    private const val KEY_DEVICE_ROLE = "key_device_role" // "PARENT", "CHILD", "UNSET"

    fun setDeviceRole(role: String) {
        if (isInitialized()) prefs.edit().putString(KEY_DEVICE_ROLE, role).apply()
    }

    fun getDeviceRole(): String {
        return if (isInitialized()) prefs.getString(KEY_DEVICE_ROLE, "UNSET") ?: "UNSET" else "UNSET"
    }

    private const val KEY_PARENT_UID = "key_parent_uid"

    // UID del padre es dato sensible → securePrefs
    fun setParentUid(uid: String) {
        if (isInitialized()) securePrefs.edit().putString(KEY_PARENT_UID, uid).apply()
    }

    fun getParentUid(): String? {
        return if (isInitialized()) securePrefs.getString(KEY_PARENT_UID, null) else null
    }

    // ===== SOS History Management =====
    
    /**
     * Represents a recorded SOS event
     */
    data class SOSEvent(
        val timestamp: Long,
        val dateTime: String,
        val hasLocation: Boolean,
        val latitude: Double? = null,
        val longitude: Double? = null
    )
    
    /**
     * Saves a new SOS event to the history
     */
    fun addSOSEvent(hasLocation: Boolean, latitude: Double? = null, longitude: Double? = null) {
        if (!isHistoryEnabled()) return
        if (!isInitialized()) return
        
        val currentHistory = getSOSHistory().toMutableList()
        val timestamp = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val dateTime = dateFormat.format(Date(timestamp))
        
        val newEvent = SOSEvent(timestamp, dateTime, hasLocation, latitude, longitude)
        currentHistory.add(0, newEvent) // Add to the beginning (most recent first)
        
        // Keep only the last 50 events to not saturate the memory
        val trimmedHistory = currentHistory.take(50)
        
        // Convert to JSON and save
        val jsonArray = JSONArray()
        trimmedHistory.forEach { event ->
            val jsonEvent = JSONObject().apply {
                put("timestamp", event.timestamp)
                put("dateTime", event.dateTime)
                put("hasLocation", event.hasLocation)
                if (event.latitude != null) put("latitude", event.latitude)
                if (event.longitude != null) put("longitude", event.longitude)
            }
            jsonArray.put(jsonEvent)
        }
        
        securePrefs.edit().putString(KEY_SOS_HISTORY, jsonArray.toString()).apply()
        // Clean up legacy insecure storage if present
        if (prefs.contains(KEY_SOS_HISTORY)) {
            prefs.edit().remove(KEY_SOS_HISTORY).apply()
        }
    }
    
    /**
     * Gets the full SOS event history
     */
    fun getSOSHistory(): List<SOSEvent> {
        if (!isInitialized()) return emptyList()
        
        // Migration logic: check legacy, move to secure, then read from secure
        var historyJson = securePrefs.getString(KEY_SOS_HISTORY, null)
        
        if (historyJson == null && prefs.contains(KEY_SOS_HISTORY)) {
            historyJson = prefs.getString(KEY_SOS_HISTORY, null)
            if (historyJson != null) {
                securePrefs.edit().putString(KEY_SOS_HISTORY, historyJson).apply()
                prefs.edit().remove(KEY_SOS_HISTORY).apply()
            }
        }
        
        if (historyJson == null) return emptyList()
        
        return try {
            val jsonArray = JSONArray(historyJson)
            val events = mutableListOf<SOSEvent>()
            
            for (i in 0 until jsonArray.length()) {
                val jsonEvent = jsonArray.getJSONObject(i)
                events.add(
                    SOSEvent(
                        timestamp = jsonEvent.getLong("timestamp"),
                        dateTime = jsonEvent.getString("dateTime"),
                        hasLocation = jsonEvent.getBoolean("hasLocation"),
                        latitude = if (jsonEvent.has("latitude")) jsonEvent.getDouble("latitude") else null,
                        longitude = if (jsonEvent.has("longitude")) jsonEvent.getDouble("longitude") else null
                    )
                )
            }
            
            events
        } catch (e: Exception) {
            Log.e("PreferencesManager", "Error parsing SOS history")
            emptyList()
        }
    }
    
    /**
     * Clears all SOS event history
     */
    fun clearSOSHistory() {
        if (isInitialized()) {
            securePrefs.edit().remove(KEY_SOS_HISTORY).apply()
            if (prefs.contains(KEY_SOS_HISTORY)) prefs.edit().remove(KEY_SOS_HISTORY).apply()
        }
    }

    // ===== Location Timeline Management =====

    data class VisitedPlace(
        val timestamp: Long,
        val dateTime: String,
        val latitude: Double,
        val longitude: Double,
        val address: String = "Unknown Location"
    )

    fun addVisitedPlace(latitude: Double, longitude: Double, address: String = "Secured Sector") {
        if (!isInitialized()) return
        
        val currentTimeline = getLocationTimeline().toMutableList()
        val timestamp = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("MMM dd, yyyy - HH:mm", Locale.getDefault())
        val dateTime = dateFormat.format(Date(timestamp))
        
        val newPlace = VisitedPlace(timestamp, dateTime, latitude, longitude, address)
        currentTimeline.add(0, newPlace) // Most recent first
        
        val trimmedTimeline = currentTimeline.take(100)
        
        val jsonArray = JSONArray()
        trimmedTimeline.forEach { place ->
            val jsonPlace = JSONObject().apply {
                put("timestamp", place.timestamp)
                put("dateTime", place.dateTime)
                put("latitude", place.latitude)
                put("longitude", place.longitude)
                put("address", place.address)
            }
            jsonArray.put(jsonPlace)
        }
        
        securePrefs.edit().putString(KEY_LOCATION_TIMELINE, jsonArray.toString()).apply()
        // Clean up legacy insecure storage
        if (prefs.contains(KEY_LOCATION_TIMELINE)) {
            prefs.edit().remove(KEY_LOCATION_TIMELINE).apply()
        }
    }

    fun getLocationTimeline(): List<VisitedPlace> {
        if (!isInitialized()) return emptyList()
        
        // Migration logic
        var timelineJson = securePrefs.getString(KEY_LOCATION_TIMELINE, null)
        
        if (timelineJson == null && prefs.contains(KEY_LOCATION_TIMELINE)) {
            timelineJson = prefs.getString(KEY_LOCATION_TIMELINE, null)
            if (timelineJson != null) {
                securePrefs.edit().putString(KEY_LOCATION_TIMELINE, timelineJson).apply()
                prefs.edit().remove(KEY_LOCATION_TIMELINE).apply()
            }
        }
        
        if (timelineJson == null) return emptyList()
        
        return try {
            val jsonArray = JSONArray(timelineJson)
            val timeline = mutableListOf<VisitedPlace>()
            
            for (i in 0 until jsonArray.length()) {
                val jsonPlace = jsonArray.getJSONObject(i)
                timeline.add(
                    VisitedPlace(
                        timestamp = jsonPlace.getLong("timestamp"),
                        dateTime = jsonPlace.getString("dateTime"),
                        latitude = jsonPlace.getDouble("latitude"),
                        longitude = jsonPlace.getDouble("longitude"),
                        address = if (jsonPlace.has("address")) jsonPlace.getString("address") else "Secured Area"
                    )
                )
            }
            timeline
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun clearLocationTimeline() {
        if (isInitialized()) {
            securePrefs.edit().remove(KEY_LOCATION_TIMELINE).apply()
            if (prefs.contains(KEY_LOCATION_TIMELINE)) prefs.edit().remove(KEY_LOCATION_TIMELINE).apply()
        }
    }

    private const val KEY_VOICE_UNLOCKED = "key_voice_unlocked"

    fun setVoiceUnlocked(unlocked: Boolean) {
        if (isInitialized()) prefs.edit().putBoolean(KEY_VOICE_UNLOCKED, unlocked).apply()
    }

    fun isVoiceUnlocked(): Boolean {
        return if (isInitialized()) prefs.getBoolean(KEY_VOICE_UNLOCKED, false) else false
    }

    private const val KEY_DISCOUNT_APPLIED = "key_discount_applied"

    fun setDiscountApplied(applied: Boolean) {
        if (isInitialized()) prefs.edit().putBoolean(KEY_DISCOUNT_APPLIED, applied).apply()
    }

    fun isDiscountApplied(): Boolean {
        return if (isInitialized()) prefs.getBoolean(KEY_DISCOUNT_APPLIED, false) else false
    }

    fun setSystemArmed(enabled: Boolean) {
        if (isInitialized()) prefs.edit().putBoolean(KEY_SYSTEM_ARMED, enabled).apply()
    }

    fun isSystemArmed(): Boolean {
        return if (isInitialized()) prefs.getBoolean(KEY_SYSTEM_ARMED, true) else true
    }
}
