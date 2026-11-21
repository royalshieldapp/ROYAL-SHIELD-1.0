package com.royalshield.app.receivers

object SmsPrivacy {
    fun receivedMessage(urlCount: Int): String = "SMS received; urlCount=$urlCount"
    fun scanCompleted(verdict: String): String = "SMS URL scan completed; verdict=$verdict"
}
