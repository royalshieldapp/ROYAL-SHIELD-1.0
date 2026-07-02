package royalshield.v2

import com.royalshield.app.data.db.Routine
import com.royalshield.app.data.db.RoutineDao
import com.royalshield.app.data.db.TriggerType
import com.royalshield.app.managers.AutomationEvaluator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class RoutineEvaluatorTest {

    // A simple fake implementation of RoutineDao to exercise insert/query logic in unit tests without a database
    class FakeRoutineDao : RoutineDao {
        private val routines = mutableListOf<Routine>()

        override fun getAllRoutines(): Flow<List<Routine>> {
            return flowOf(routines)
        }

        override suspend fun getRoutineById(id: Int): Routine? {
            return routines.find { it.id == id }
        }

        override fun getRoutineByIdFlow(id: Int): Flow<Routine?> {
            return flowOf(routines.find { it.id == id })
        }

        override suspend fun insertRoutine(routine: Routine): Long {
            val newId = (routines.size + 1)
            val routineWithId = routine.copy(id = newId)
            routines.add(routineWithId)
            return newId.toLong()
        }

        override suspend fun updateRoutine(routine: Routine) {
            val index = routines.indexOfFirst { it.id == routine.id }
            if (index != -1) {
                routines[index] = routine
            }
        }

        override suspend fun deleteRoutine(routine: Routine) {
            routines.removeIf { it.id == routine.id }
        }
    }

    @Test
    fun testRoutineInsertionInFakeDao() = runBlocking {
        val dao = FakeRoutineDao()
        val routine = Routine(
            name = "Battery Saver",
            description = "Turns off features when battery is low",
            isEnabled = true,
            triggerType = TriggerType.BATTERY_LOW,
            triggerParams = "20",
            actionsJson = """[{"actionType":"MUTE_AUDIO"}]""",
            iconName = "ic_battery",
            colorHex = "#FF0000"
        )

        val generatedId = dao.insertRoutine(routine)
        assertEquals(1L, generatedId)

        val inserted = dao.getRoutineById(1)
        assertNotNull(inserted)
        assertEquals("Battery Saver", inserted?.name)
        assertEquals(TriggerType.BATTERY_LOW, inserted?.triggerType)
        assertEquals("20", inserted?.triggerParams)
    }

    @Test
    fun testAutomationEvaluator_BatteryLow_Triggered() {
        val routine = Routine(
            name = "Battery Saver",
            description = "Turns off features when battery is low",
            isEnabled = true,
            triggerType = TriggerType.BATTERY_LOW,
            triggerParams = "20",
            actionsJson = """[{"actionType":"MUTE_AUDIO"}]""",
            iconName = "ic_battery",
            colorHex = "#FF0000"
        )

        // 1. Should trigger if battery is below/equal to threshold (20)
        val dataTriggered = mapOf("battery" to 15)
        val resultTriggered = AutomationEvaluator.evaluateRoutine(routine, dataTriggered)
        assertTrue(resultTriggered)

        // 2. Should NOT trigger if battery is above threshold (20)
        val dataNotTriggered = mapOf("battery" to 30)
        val resultNotTriggered = AutomationEvaluator.evaluateRoutine(routine, dataNotTriggered)
        assertFalse(resultNotTriggered)
    }

    @Test
    fun testAutomationEvaluator_WifiConnect_Triggered() {
        val routine = Routine(
            name = "Secure Public Wifi",
            description = "Enable VPN on connection to public wifi",
            isEnabled = true,
            triggerType = TriggerType.WIFI_CONNECT,
            triggerParams = "Starbucks_WiFi",
            actionsJson = """[{"actionType":"TOGGLE_VPN"}]""",
            iconName = "ic_wifi",
            colorHex = "#0000FF"
        )

        // 1. Should trigger if SSID matches
        val dataTriggered = mapOf("wifi_ssid" to "Starbucks_WiFi")
        val resultTriggered = AutomationEvaluator.evaluateRoutine(routine, dataTriggered)
        assertTrue(resultTriggered)

        // 2. Should NOT trigger if SSID does not match
        val dataNotTriggered = mapOf("wifi_ssid" to "Home_Secure_WiFi")
        val resultNotTriggered = AutomationEvaluator.evaluateRoutine(routine, dataNotTriggered)
        assertFalse(resultNotTriggered)
    }

    @Test
    fun testAutomationEvaluator_DisabledRoutine_NeverTriggered() {
        val routine = Routine(
            name = "Disabled Saver",
            description = "Turns off features when battery is low",
            isEnabled = false, // Disabled
            triggerType = TriggerType.BATTERY_LOW,
            triggerParams = "20",
            actionsJson = """[{"actionType":"MUTE_AUDIO"}]""",
            iconName = "ic_battery",
            colorHex = "#FF0000"
        )

        val data = mapOf("battery" to 10)
        val result = AutomationEvaluator.evaluateRoutine(routine, data)
        assertFalse(result)
    }
}
