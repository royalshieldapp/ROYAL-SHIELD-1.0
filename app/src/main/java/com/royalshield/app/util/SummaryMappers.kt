package com.royalshield.app.util

import com.royalshield.app.models.AppScanSummary

data class ScanProStrings(
    val header: String,
    val kpiLine: String,
    val distributionLine: String,
    val topPermissionsLines: List<String>
)

fun AppScanSummary.toTelemetryJson(): String {
    // Manually building JSON to avoid dependencies like Gson if not already present
    // metrics object
    val m = metrics
    
    val safeTop = m.topDangerousPermissions.take(5).joinToString(",") { 
        "{\"permission\":\"${it.permission}\",\"count\":${it.count}}" 
    }
    
    // Simplification for byPermission to avoid huge string if not needed, or just include top permissions
    val safeByPerm = m.byPermission.entries.take(10).joinToString(",") { 
        "\"${it.key}\":{\"frequency\":${it.value.frequency},\"averageRiskContribution\":${it.value.averageRiskContribution}}" 
    }

    return """
    {
      "feature": "appscan",
      "generatedAt": "$scanTimestamp",
      "metrics": {
        "totalApps": ${m.totalApps},
        "avgRiskScore": ${m.avgRiskScore},
        "minRiskScore": ${m.minRiskScore},
        "maxRiskScore": ${m.maxRiskScore},
        "low": ${m.low},
        "medium": ${m.medium},
        "high": ${m.high},
        "topDangerousPermissions": [$safeTop],
        "byPermission": {$safeByPerm}
      }
    }
    """.trimIndent()
}

fun AppScanSummary.toProStrings(): ScanProStrings {
    val m = metrics
    // Use Locale.US to ensure dot decimal separator and prevent formatting crashes
    val kpi = java.util.Locale.US.let { locale ->
        "Average risk %.1f (min %d / max %d)".format(locale, m.avgRiskScore, m.minRiskScore, m.maxRiskScore)
    }
    
    return ScanProStrings(
        header = "Resumen de escaneo — ${m.totalApps} apps",
        kpiLine = kpi,
        distributionLine = "Bajo ${m.low} · Medio ${m.medium} · Alto ${m.high}",
        topPermissionsLines = m.topDangerousPermissions.mapIndexed { index, perm ->
            val simpleName = perm.permission.substringAfterLast(".", perm.permission)
            "${index + 1}) $simpleName — ${perm.count} apps"
        }
    )
}
