package com.arms.ke3

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class ArmsNotificationListener : NotificationListenerService() {
    private val supported = setOf(
        "com.whatsapp",
        "com.whatsapp.w4b",
        "org.telegram.messenger",
        "com.google.android.apps.messaging",
        "com.facebook.orca",
        "com.google.android.gm",
        "com.instagram.android"
    )

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName !in supported) return
        val extras = sbn.notification.extras ?: return
        val title = extras.getCharSequence("android.title")?.toString().orEmpty()
        val text = extras.getCharSequence("android.text")?.toString().orEmpty()
        val bigText = extras.getCharSequence("android.bigText")?.toString().orEmpty()
        val body = bigText.ifBlank { text }.trim()
        if (body.isBlank()) return
        val sender = title.ifBlank { appName(sbn.packageName) }
        val message = "$sender: $body"
        ArmsBleManager.sendNotification(message)
        NotificationStore.add(message)
    }

    private fun appName(pkg: String): String = when (pkg) {
        "com.whatsapp", "com.whatsapp.w4b" -> "WhatsApp"
        "org.telegram.messenger" -> "Telegram"
        "com.google.android.apps.messaging" -> "SMS"
        "com.facebook.orca" -> "Messenger"
        "com.google.android.gm" -> "Gmail"
        "com.instagram.android" -> "Instagram"
        else -> "Notifica"
    }
}
