package com.royalshield.app.ui.screens

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

class RegistrationIconAssetsTest {

    @Test
    fun registrationPackagesEveryApprovedBlackGoldProviderIcon() {
        val drawableDirectory = sequenceOf(
            Path.of("src", "main", "res", "drawable"),
            Path.of("app", "src", "main", "res", "drawable")
        ).first { Files.isDirectory(it) }

        val approvedIcons = listOf(
            "ic_registration_phone.xml",
            "ic_registration_google_play.xml",
            "ic_registration_apple.xml",
            "ic_registration_git.xml",
            "ic_registration_facebook.xml",
            "ic_registration_email.xml"
        )

        approvedIcons.forEach { fileName ->
            val icon = drawableDirectory.resolve(fileName)
            assertTrue("Missing approved Registration icon: $fileName", Files.isRegularFile(icon))

            val xml = String(Files.readAllBytes(icon), StandardCharsets.UTF_8)
            assertTrue("$fileName must include the black circular base", xml.contains("#FF080808"))
            assertTrue("$fileName must include the Royal Gold treatment", xml.contains("#FFFFD700"))
        }
    }
}
