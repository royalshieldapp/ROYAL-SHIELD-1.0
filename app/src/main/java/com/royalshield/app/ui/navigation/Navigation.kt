package com.royalshield.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.royalshield.app.ui.components.BottomNavItem
import com.royalshield.app.ui.screens.*

@Composable
fun RoyalShieldNavGraph(
    navController: NavHostController,
    billingManager: com.royalshield.app.managers.BillingManager,
    onTriggerSOS: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = BottomNavItem.Home.route
    ) {
        composable(BottomNavItem.Home.route) {
            DashboardScreen(
                onNavigateToSettings = { navController.navigate(BottomNavItem.Settings.route) },
                onNavigateToSystemStatus = { navController.navigate("system_status") },
                onNavigateToSOS = onTriggerSOS,
                onNavigateToAutomation = { navController.navigate(BottomNavItem.Automation.route) },
                onNavigateToAiHub = { navController.navigate("ai_neural_hub") },
                onNavigateToFileScan = { navController.navigate("file_scan") },
                onNavigateToPhishing = { navController.navigate("phishing_detector") },
                onNavigateToMap = { navController.navigate(BottomNavItem.GlobalMap.route) },
                onNavigateToLoyalty = { navController.navigate("loyalty") },
                onNavigateToPremium = { navController.navigate("subscription") },
                onNavigateToCourses = { navController.navigate("courses") },
                onNavigateToBusinessDashboard = { navController.navigate("business_dashboard") },
                onNavigateToContacts = { navController.navigate("contacts") },
                onNavigateToVpn = { navController.navigate("vpn") },
                onNavigateToTrackingShield = { navController.navigate("tracking_shield") },
                onNavigateToGallery = { navController.navigate("gallery") },
                onNavigateToSoundDetection = { navController.navigate("sound_detection") },
                onNavigateToSecurityCamera = { navController.navigate("security_camera") },
                onNavigateToSolutionEngine = { navController.navigate("solution_engine") },
                billingManager = billingManager
            )
        }

        composable("marketplace") {
            MarketplaceScreen(onBack = { navController.popBackStack() })
        }

        composable(BottomNavItem.GlobalMap.route) {
            GlobalMapScreen(onBack = { navController.popBackStack() })
        }

        composable(BottomNavItem.CyberMap.route) {
            CyberHomeScreen(
                onBack = { navController.popBackStack() },
                onNavigateToMap = { navController.navigate(BottomNavItem.GlobalMap.route) },
                onNavigateToCourses = { navController.navigate("courses") },
                onNavigateToVoiceScam = { navController.navigate("voice_scam") },
                onNavigateToXdr = { navController.navigate("xdr_dashboard") },
                onNavigateToIntel = { navController.navigate("intel_hub") }
            )
        }



        composable(BottomNavItem.Automation.route) {
            AutomationScreen(
                onBack = { navController.popBackStack() },
                onBusinessClick = { navController.navigate("business") }
            )
        }

        composable(BottomNavItem.Settings.route) {
            SettingsScreen(
                onNavigateToSubscription = { navController.navigate("subscription") },
                onNavigateToLoyalty = { navController.navigate("loyalty") },
                onNavigateToAdvancedSettings = { navController.navigate("advanced_settings") },
                onNavigateToCourses = { navController.navigate("courses") },
                onNavigateToFundamentals = { navController.navigate("courses") },
                onNavigateToReferral = { navController.navigate("referral") },
                onBackPressed = { navController.popBackStack() }
            )
        }

        composable("subscription") {
            val isPremium by billingManager.hasPremiumAccess.collectAsState()
            SubscriptionScreen(
                isPremium = isPremium,
                billingManager = billingManager,
                onBack = { navController.popBackStack() }
            )
        }

        composable("gallery") { SosGalleryScreen(onBack = { navController.popBackStack() }) }

        composable("loyalty") { LoyaltyProgramScreen(onBack = { navController.popBackStack() }) }
        composable("advanced_settings") { AdvancedSettingsScreen(onBackPressed = { navController.popBackStack() }) }
        composable("file_scan") {
            FileScanScreen(
                onBack = { navController.popBackStack() },
                billingManager = billingManager,
                onNavigateToPremium = { navController.navigate("subscription") }
            )
        }
        composable("phishing_detector") { PhishingDetectorScreen(onBack = { navController.popBackStack() }) }
        composable("business") {
            BusinessScreen(
                onBack = { navController.popBackStack() },
                onAccessDashboard = { navController.navigate("business_dashboard") }
            )
        }

        composable("ai_hub") {
            AiHubScreen(
                onBack = { navController.popBackStack() },
                billingManager = billingManager,
                onNavigateToPremium = { navController.navigate("subscription") },
                onNavigateToScriptLab = { navController.navigate("ai_script_lab") }
            )
        }

        composable("ai_neural_hub") {
            val context = androidx.compose.ui.platform.LocalContext.current
            AiNeuralHubScreen(
                onBack = { navController.popBackStack() },
                onTriggerSos = onTriggerSOS,
                onCheckUrl = { url ->
                    android.widget.Toast.makeText(context, "Scanning URL: $url", android.widget.Toast.LENGTH_SHORT).show()
                }
            )
        }
        composable("ai_script_lab") { AiScriptLabScreen(onBack = { navController.popBackStack() }) }
        composable("courses") { CourseScreen(onBackPressed = { navController.popBackStack() }) }

        // Business Tools Navigation
        composable("business_dashboard") {
            BusinessDashboardScreen(
                onBack = { navController.popBackStack() },
                onNavigateToReports = { navController.navigate("reports_center") },
                onNavigateToCompliance = { navController.navigate("compliance_monitor") },
                onNavigateToAuditLogs = { navController.navigate("audit_logs") },
                onNavigateToPolicies = { navController.navigate("policy_manager") },
                onNavigateToIrHub = { navController.navigate("ir_hub") },
                onNavigateToIntelHub = { navController.navigate("intel_hub") }
            )
        }
        composable("reports_center") { ReportsCenterScreen(onBack = { navController.popBackStack() }) }
        composable("compliance_monitor") { ComplianceMonitorScreen(onBack = { navController.popBackStack() }) }
        composable("audit_logs") { AuditLogsScreen(onBack = { navController.popBackStack() }) }
        composable("policy_manager") { PolicyManagerScreen(onBack = { navController.popBackStack() }) }

        composable("xdr_dashboard") {
             XdrScreen(onBack = { navController.popBackStack() })
        }

        composable("contacts") {
            ContactsScreen(onBack = { navController.popBackStack() })
        }

        composable("vpn") {
            VpnScreen(
                onBack = { navController.popBackStack() },
                billingManager = billingManager,
                onNavigateToPremium = { navController.navigate("subscription") }
            )
        }

        composable("tracking_shield") {
            TrackingShieldScreen(onBack = { navController.popBackStack() })
        }

        composable("timeline") {
            LocationTimelineScreen(onBackPressed = { navController.popBackStack() })
        }

        composable("system_status") {
            SystemStatusScreen(onBack = { navController.popBackStack() })
        }

        composable("sound_detection") {
            SoundDetectionScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable("security_camera") {
            SecurityCameraScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable("solution_engine") {
            SolutionEngineScreen(onBack = { navController.popBackStack() })
        }

        // â”€â”€ Previously missing routes â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        composable("voice_scam") {
            VoiceScamScreen(onBack = { navController.popBackStack() })
        }

        // Removed duplicate xdr_dashboard

        composable("referral") {
            ReferralScreen(onNavigateBack = { navController.popBackStack() })
        }

        // â”€â”€ PHASE 4: IR & Intel Hub â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        composable("ir_hub") {
            IrHubScreen(
                onNavigateToDetail = { id -> navController.navigate("ir_incident_detail/$id") },
                onBack = { navController.popBackStack() }
            )
        }

        composable("ir_incident_detail/{incidentId}") { backStackEntry ->
            val incidentId = backStackEntry.arguments?.getString("incidentId") ?: ""
            IrIncidentDetailScreen(
                incidentId = incidentId,
                onBack = { navController.popBackStack() }
            )
        }

        composable("intel_hub") {
            IntelHubScreen(
                onNavigateToAlertDetail = { id -> navController.navigate("intel_alert_detail/$id") },
                onBack = { navController.popBackStack() }
            )
        }

        composable("intel_alert_detail/{alertId}") { backStackEntry ->
            val alertId = backStackEntry.arguments?.getString("alertId") ?: ""
            IntelAlertDetailScreen(
                alertId = alertId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
