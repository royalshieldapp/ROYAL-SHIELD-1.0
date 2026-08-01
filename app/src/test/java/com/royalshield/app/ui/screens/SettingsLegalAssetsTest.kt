package com.royalshield.app.ui.screens

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.charset.StandardCharsets

class SettingsLegalAssetsTest {

    @Test
    fun privacyAndTermsDocumentsArePackagedWithRequiredContent() {
        val assetsDirectory = sequenceOf(
            Path.of("src", "main", "assets"),
            Path.of("app", "src", "main", "assets")
        ).first { Files.isDirectory(it) }

        val privacy = assetsDirectory.resolve("privacy_policy_embed.html")
        val terms = assetsDirectory.resolve("terms_embed.html")

        assertTrue("Privacy Policy asset must be packaged", Files.isRegularFile(privacy))
        assertTrue("Terms asset must be packaged", Files.isRegularFile(terms))

        val privacyHtml = String(Files.readAllBytes(privacy), StandardCharsets.UTF_8)
        val termsHtml = String(Files.readAllBytes(terms), StandardCharsets.UTF_8)

        assertTrue(privacyHtml.contains("Privacy Policy — Royal Shield"))
        assertTrue(privacyHtml.contains("11. Contact"))
        assertTrue(termsHtml.contains("Terms and Conditions of Use"))
        assertTrue(termsHtml.contains("12. Contact"))
    }
}
