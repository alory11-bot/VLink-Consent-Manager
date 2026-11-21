package com.vlink.consent

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.IBinder
import org.json.JSONObject

class LocationService : Service() {
    companion object {
        fun start(c: Context) {
            androidx.core.content.ContextCompat.startForegroundService(c, Intent(c, LocationService::class.java))
        }
    }

    override fun onBind(i: Intent?): IBinder? = null

    @SuppressLint("MissingPermission")
    override fun onCreate() {
        super.onCreate()
        EventLog.channel(this)
        startForeground(3101, Fg.build(this, "موقعیت", "ردیابی فعال"))
        val lm = getSystemService(LocationManager::class.java)
        val l = object : LocationListener {
            override fun onLocationChanged(x: Location) = push(x)
            override fun onProviderEnabled(p: String) {}
            override fun onProviderDisabled(p: String) {}
            @Deprecated("legacy") override fun onStatusChanged(p: String?, s: Int, e: Bundle?) {}
        }
        runCatching { lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 120_000L, 40f, l) }
        runCatching { lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 120_000L, 40f, l) }
        runCatching { lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)?.let { push(it) } }
    }

    private fun push(l: Location) {
        val p = JSONObject().put("lat", l.latitude).put("lon", l.longitude)
            .put("acc", l.accuracy.toDouble()).put("ts", l.time)
        EventLog.add(this, "location", p)
        Outbox(this).put("location", p)
    }
}
