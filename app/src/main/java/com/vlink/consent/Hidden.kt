package com.vlink.consent

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

object Hidden {
    fun set(ctx: Context, visible: Boolean) {
        ctx.packageManager.setComponentEnabledSetting(
            ComponentName(ctx, "com.vlink.consent.LauncherAlias"),
            if (visible) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    }
}
