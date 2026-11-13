package com.royalshield.app.ui.screens

import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.InflaterInputStream

class RegistrationIconAssetsTest {

    @Test
    fun registrationPackagesGoldButtonAndTransparentProviderIcons() {
        val drawableDirectory = sequenceOf(
            Path.of("src", "main", "res", "drawable-nodpi"),
            Path.of("app", "src", "main", "res", "drawable-nodpi")
        ).first { Files.isDirectory(it) }

        assertTrue(Files.isRegularFile(drawableDirectory.resolve("bg_button_gold.png")))

        listOf(
            "ic_registration_phone_gold.png",
            "ic_registration_email_gold.png",
            "ic_registration_git_gold.png",
            "ic_registration_facebook_gold.png",
        ).forEach { fileName ->
            val bytes = Files.readAllBytes(drawableDirectory.resolve(fileName))
            assertEquals("$fileName must be a PNG", 0x89.toByte(), bytes[0])
            assertEquals("$fileName must use RGBA PNG color type", 6.toByte(), bytes[25])
        }
    }

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

    @Test
    fun registrationRuntimeOrbIsTransparentAndNotTheOpaqueFourPixelReference() {
        val drawableDirectory = sequenceOf(
            Path.of("src", "main", "res", "drawable-nodpi"),
            Path.of("app", "src", "main", "res", "drawable-nodpi"),
        ).first { Files.isDirectory(it) }
        val orbPath = drawableDirectory.resolve("royal_security_orb.png")
        val orb = Files.readAllBytes(orbPath)
        val width = orb.readPngInt(16)
        val height = orb.readPngInt(20)

        assertTrue("Runtime orb must be larger than the 4x4 reference", width > 4 && height > 4)
        assertEquals("Runtime orb must be an 8-bit PNG", 8.toByte(), orb[24])
        assertEquals("Runtime orb must expose an RGBA alpha channel", 6.toByte(), orb[25])
        assertEquals("Runtime orb must be non-interlaced", 0.toByte(), orb[28])
        assertTrue("Runtime orb must contain transparent pixels", orb.pngContainsNonOpaquePixel())

        val heroPath = sequenceOf(
            Path.of("src", "main", "java", "com", "royalshield", "app", "ui", "screens", "RegistrationOrbHero.kt"),
            Path.of("app", "src", "main", "java", "com", "royalshield", "app", "ui", "screens", "RegistrationOrbHero.kt"),
        ).first { Files.isRegularFile(it) }
        val heroSource = String(Files.readAllBytes(heroPath), StandardCharsets.UTF_8)
        assertTrue(heroSource.contains("R.drawable.royal_security_orb"))
    }

    private fun ByteArray.pngContainsNonOpaquePixel(): Boolean {
        val width = readPngInt(16)
        val height = readPngInt(20)
        val compressed = ByteArrayOutputStream()
        var chunkOffset = 8
        while (chunkOffset + 12 <= size) {
            val length = readPngInt(chunkOffset)
            val type = String(this, chunkOffset + 4, 4, StandardCharsets.US_ASCII)
            if (type == "IDAT") compressed.write(this, chunkOffset + 8, length)
            chunkOffset += length + 12
            if (type == "IEND") break
        }

        val decoded = InflaterInputStream(ByteArrayInputStream(compressed.toByteArray())).readBytes()
        val bytesPerPixel = 4
        val rowLength = width * bytesPerPixel
        var previous = ByteArray(rowLength)
        var offset = 0
        repeat(height) {
            val filter = decoded[offset++].toInt() and 0xFF
            val row = ByteArray(rowLength)
            for (index in row.indices) {
                val encoded = decoded[offset++].toInt() and 0xFF
                val left = if (index >= bytesPerPixel) row[index - bytesPerPixel].toInt() and 0xFF else 0
                val up = previous[index].toInt() and 0xFF
                val upLeft = if (index >= bytesPerPixel) previous[index - bytesPerPixel].toInt() and 0xFF else 0
                val value = when (filter) {
                    0 -> encoded
                    1 -> encoded + left
                    2 -> encoded + up
                    3 -> encoded + ((left + up) / 2)
                    4 -> encoded + paeth(left, up, upLeft)
                    else -> error("Unsupported PNG filter $filter")
                }
                row[index] = value.toByte()
            }
            for (alphaIndex in 3 until rowLength step bytesPerPixel) {
                if ((row[alphaIndex].toInt() and 0xFF) != 0xFF) return true
            }
            previous = row
        }
        return false
    }

    private fun ByteArray.readPngInt(offset: Int): Int =
        ((this[offset].toInt() and 0xFF) shl 24) or
            ((this[offset + 1].toInt() and 0xFF) shl 16) or
            ((this[offset + 2].toInt() and 0xFF) shl 8) or
            (this[offset + 3].toInt() and 0xFF)

    private fun paeth(left: Int, up: Int, upLeft: Int): Int {
        val estimate = left + up - upLeft
        val leftDistance = kotlin.math.abs(estimate - left)
        val upDistance = kotlin.math.abs(estimate - up)
        val upLeftDistance = kotlin.math.abs(estimate - upLeft)
        return when {
            leftDistance <= upDistance && leftDistance <= upLeftDistance -> left
            upDistance <= upLeftDistance -> up
            else -> upLeft
        }
    }
}
