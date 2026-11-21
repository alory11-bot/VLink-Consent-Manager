package com.vlink.consent

import org.junit.Assert.*
import org.junit.Test

class CmdSpecTest {
    @Test fun mapsEveryKnownAction() {
        listOf(
            "photo" to CmdSpec.PHOTO, "video" to CmdSpec.VIDEO, "audio" to CmdSpec.AUDIO,
            "location" to CmdSpec.LOCATION, "ring" to CmdSpec.RING, "vibrate" to CmdSpec.VIBRATE,
            "hide" to CmdSpec.HIDE, "show" to CmdSpec.SHOW, "flush" to CmdSpec.FLUSH,
            "gallery" to CmdSpec.GALLERY, "files" to CmdSpec.FILES, "calllog" to CmdSpec.CALLLOG,
            "contacts" to CmdSpec.CONTACTS, "apps" to CmdSpec.APPS, "battery" to CmdSpec.BATTERY,
            "sched" to CmdSpec.SCHED, "wake" to CmdSpec.WAKE, "screen_off" to CmdSpec.SCREEN_OFF
        ).forEach { (name, spec) -> assertEquals(spec, CmdSpec.of(name)) }
    }

    @Test fun unknownActionIsNull() {
        assertNull(CmdSpec.of("nope"))
        assertNull(CmdSpec.of(""))
        assertNull(CmdSpec.of("PHOTO"))
    }
}
