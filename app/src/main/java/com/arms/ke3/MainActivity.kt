package com.arms.ke3

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.*
import kotlinx.coroutines.*

class MainActivity : Activity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var statusText: TextView
    private lateinit var healthText: TextView
    private lateinit var logText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNeededPermissions()
        buildUi()
        observeState()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 32)
        }
        val title = TextView(this).apply {
            text = "ARMS"
            textSize = 30f
            typeface = Typeface.DEFAULT_BOLD
        }
        val subtitle = TextView(this).apply {
            text = "KE3 PRO companion - v0.1 alpha"
            textSize = 14f
        }
        statusText = TextView(this).apply { textSize = 16f; setPadding(0, 24, 0, 24) }
        healthText = TextView(this).apply { textSize = 16f; setPadding(0, 24, 0, 24) }
        logText = TextView(this).apply { textSize = 13f; setPadding(0, 12, 0, 12) }

        val connect = Button(this).apply { text = "Connetti KE3 PRO"; setOnClickListener { ArmsBleManager.scanAndConnect() } }
        val test = Button(this).apply { text = "Invia TEST HELLO"; setOnClickListener { ArmsBleManager.sendNotification("ARMS: HELLO") } }
        val notif = Button(this).apply { text = "Abilita accesso notifiche"; setOnClickListener { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) } }
        val disconnect = Button(this).apply { text = "Disconnetti"; setOnClickListener { ArmsBleManager.disconnect() } }

        val info = TextView(this).apply {
            text = "Moduli inclusi:\n• Notifiche WhatsApp/Telegram/SMS/Gmail\n• Protocollo KE3: 56FF / 34F1 / 34F2\n• Centro Benessere: batteria + parser salute iniziale\n• Passi, SpO₂, sonno: schermate predisposte, protocollo da completare"
            textSize = 14f
        }

        root.addView(title); root.addView(subtitle); root.addView(statusText)
        root.addView(connect); root.addView(test); root.addView(notif); root.addView(disconnect)
        root.addView(healthText); root.addView(info); root.addView(logText)
        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun observeState() {
        scope.launch {
            ArmsBleManager.status.collect { statusText.text = "Stato: $it" }
        }
        scope.launch {
            HealthRepository.state.collect {
                healthText.text = "Centro Benessere\nBatteria: ${it.battery?.toString() ?: "--"}%\nBattito: ${it.heartRate?.toString() ?: "--"} bpm\nPassi: ${it.steps?.toString() ?: "in sviluppo"}\nSpO₂: ${it.spo2?.toString() ?: "in sviluppo"}\nUltimo pacchetto: ${it.lastRaw}"
            }
        }
        scope.launch {
            NotificationStore.items.collect { list -> logText.text = "Ultime notifiche intercettate:\n" + list.joinToString("\n\n") }
        }
    }

    private fun requestNeededPermissions() {
        if (Build.VERSION.SDK_INT >= 31) {
            val perms = mutableListOf<String>()
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) perms += Manifest.permission.BLUETOOTH_SCAN
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) perms += Manifest.permission.BLUETOOTH_CONNECT
            if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) perms += Manifest.permission.POST_NOTIFICATIONS
            if (perms.isNotEmpty()) requestPermissions(perms.toTypedArray(), 10)
        } else if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 11)
        }
    }

    override fun onDestroy() { super.onDestroy(); scope.cancel() }
}
