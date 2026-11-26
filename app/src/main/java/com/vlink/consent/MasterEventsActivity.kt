package com.vlink.consent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.json.JSONObject

class MasterEventsActivity : ComponentActivity() {
    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    var events by remember { mutableStateOf(EventLog.all(this@MasterEventsActivity)) }
                    Column(Modifier.fillMaxSize().padding(12.dp)) {
                        Text("رویدادها", style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("photo" to "عکس", "video" to "ویدیو", "audio" to "صدا",
                                "location" to "موقعیت", "ring" to "زنگ", "hide" to "مخفی")
                                .forEach { (a, l) ->
                                    AssistChip(
                                        onClick = { Cmd.run(this@MasterEventsActivity, a, JSONObject()) },
                                        label = { Text(l) })
                                }
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(onClick = { events = EventLog.all(this@MasterEventsActivity) }) { Text("بازخوانی") }
                            OutlinedButton(onClick = {
                                EventLog.clear(this@MasterEventsActivity)
                                events = emptyList()
                            }) { Text("پاک کردن") }
                        }
                        LazyColumn(Modifier.fillMaxSize()) {
                            items(events) { e ->
                                Column(Modifier.padding(10.dp)) {
                                    Text(e.optString("kind"), style = MaterialTheme.typography.labelSmall)
                                    Text(e.optString("body"), style = MaterialTheme.typography.bodySmall)
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}
