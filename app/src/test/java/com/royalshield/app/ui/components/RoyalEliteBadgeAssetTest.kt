package com.royalshield.app.ui.components

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.InflaterInputStream

class RoyalEliteBadgeAssetTest {

    @Test
    fun royalEliteBadgeIsAHighResolutionTransparentCutout() {
        val drawableDirectory = sequenceOf(
            Path.of("src", "main", "res", "drawable-nodpi"),
            Path.of("app", "src", "main", "res", "drawable-nodpi")
        ).first { Files.isDirectory(it) }
        val badgePath = drawableDirectory.resolve("royal_elite_badge.png")

        assertTrue("Missing Royal Elite badge asset", Files.isRegularFile(badgePath))
        val badge = decodeRgbaPng(Files.readAllBytes(badgePath))
        assertTrue("Royal Elite badge must be at least 512 px wide", badge.width >= 512)
        assertTrue("Royal Elite badge must be at least 512 px high", badge.height >= 512)

        val cornerAlpha = listOf(
            badge.alphaAt(0, 0),
            badge.alphaAt(badge.width - 1, 0),
            badge.alphaAt(0, badge.height - 1),
            badge.alphaAt(badge.width - 1, badge.height - 1)
        )
        cornerAlpha.forEach { alpha ->
            assertTrue("Royal Elite badge corners must be transparent", alpha <= 16)
        }
    }

    private data class RgbaPng(val width: Int, val height: Int, val pixels: ByteArray) {
        fun alphaAt(x: Int, y: Int): Int = pixels[(y * width + x) * 4 + 3].toInt() and 0xFF
    }

    private fun decodeRgbaPng(bytes: ByteArray): RgbaPng {
        val signature = byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10)
        assertTrue("Royal Elite badge must be a PNG", bytes.take(8).toByteArray().contentEquals(signature))

        var offset = 8
        var width = 0
        var height = 0
        val compressed = ByteArrayOutputStream()
        while (offset + 12 <= bytes.size) {
            val length = ByteBuffer.wrap(bytes, offset, 4).order(ByteOrder.BIG_ENDIAN).int
            val type = String(bytes, offset + 4, 4, Charsets.US_ASCII)
            val dataOffset = offset + 8
            when (type) {
                "IHDR" -> {
                    width = ByteBuffer.wrap(bytes, dataOffset, 4).order(ByteOrder.BIG_ENDIAN).int
                    height = ByteBuffer.wrap(bytes, dataOffset + 4, 4).order(ByteOrder.BIG_ENDIAN).int
                    assertTrue("Royal Elite badge must use 8-bit channels", bytes[dataOffset + 8].toInt() == 8)
                    assertTrue("Royal Elite badge must use RGBA color", bytes[dataOffset + 9].toInt() == 6)
                    assertTrue("Royal Elite badge must not be interlaced", bytes[dataOffset + 12].toInt() == 0)
                }
                "IDAT" -> compressed.write(bytes, dataOffset, length)
                "IEND" -> break
            }
            offset = dataOffset + length + 4
        }

        val raw = InflaterInputStream(ByteArrayInputStream(compressed.toByteArray())).readBytes()
        val stride = width * 4
        assertTrue("Royal Elite badge PNG data is incomplete", raw.size == (stride + 1) * height)
        val pixels = ByteArray(stride * height)
        var rawOffset = 0
        for (y in 0 until height) {
            val filter = raw[rawOffset++].toInt() and 0xFF
            for (x in 0 until stride) {
                val value = raw[rawOffset++].toInt() and 0xFF
                val left = if (x >= 4) pixels[y * stride + x - 4].toInt() and 0xFF else 0
                val up = if (y > 0) pixels[(y - 1) * stride + x].toInt() and 0xFF else 0
                val upLeft = if (y > 0 && x >= 4) pixels[(y - 1) * stride + x - 4].toInt() and 0xFF else 0
                val decoded = when (filter) {
                    0 -> value
                    1 -> value + left
                    2 -> value + up
                    3 -> value + ((left + up) / 2)
                    4 -> value + paeth(left, up, upLeft)
                    else -> error("Unsupported PNG filter: $filter")
                }
                pixels[y * stride + x] = decoded.toByte()
            }
        }
        return RgbaPng(width, height, pixels)
    }

    private fun paeth(left: Int, up: Int, upLeft: Int): Int {
        val prediction = left + up - upLeft
        val leftDistance = kotlin.math.abs(prediction - left)
        val upDistance = kotlin.math.abs(prediction - up)
        val diagonalDistance = kotlin.math.abs(prediction - upLeft)
        return when {
            leftDistance <= upDistance && leftDistance <= diagonalDistance -> left
            upDistance <= diagonalDistance -> up
            else -> upLeft
        }
    }
}
