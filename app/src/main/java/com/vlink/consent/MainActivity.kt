package com.vlink.consent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private lateinit var store: SecureStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = SecureStore(this)
        setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    VLinkScreen()
                }
            }
        }
    }

    private fun readEvents(): List<AuditEvent> = runCatching {
        val array = JSONArray(store.get("audit") ?: "[]")
        (0 until array.length()).map { i ->
            val item = array.getJSONObject(i)
            AuditEvent(item.getLong("at"), item.getString("action"), item.getString("detail"))
        }.takeLast(30).reversed()
    }.getOrDefault(emptyList())

    private fun addEvent(action: String, detail: String) {
        val all = readEvents().reversed().toMutableList()
        all += AuditEvent(System.currentTimeMillis(), action, detail)
        val json = JSONArray()
        all.takeLast(30).forEach { event ->
            json.put(JSONObject().put("at", event.at).put("action", event.action).put("detail", event.detail))
        }
        store.put("audit", json.toString())
    }

    @Composable
    private fun VLinkScreen() {
        var role by remember { mutableStateOf(store.get("role") ?: "") }
        var code by remember { mutableStateOf("") }
        var invite by remember { mutableStateOf(store.get("invite") ?: "") }
        var companion by remember { mutableStateOf(store.get("companion") ?: "") }
        var consent by remember { mutableStateOf(store.get("consent") == "yes") }
        var events by remember { mutableStateOf(readEvents()) }
        var showConsent by remember { mutableStateOf(false) }
        var label by remember { mutableStateOf("") }
        var message by remember { mutableStateOf("") }

        fun log(action: String, detail: String) {
            addEvent(action, detail)
            events = readEvents()
        }

        Surface(Modifier.fillMaxSize()) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("VLink", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text("مدیریت اتصال دستگاه‌ها با رضایت روشن و قابل لغو")
                Card {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("۱. انتخاب نقش", fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                role = "controller"; store.put("role", role)
                                log("ROLE", "نقش کنترل‌کننده انتخاب شد")
                            }) { Text("کنترل‌کننده") }
                            OutlinedButton(onClick = {
                                role = "companion"; store.put("role", role)
                                log("ROLE", "نقش دستگاه همراه انتخاب شد")
                            }) { Text("دستگاه همراه") }
                        }
                        if (role.isNotBlank()) Text("نقش فعلی: " + if (role == "controller") "کنترل‌کننده" else "دستگاه همراه")
                    }
                }

                if (role == "controller") {
                    Card {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("۲. دعوت یک‌بارمصرف", fontWeight = FontWeight.Bold)
                            Text("کد در این نسخه فقط محلی تولید و ثبت می‌شود؛ هنوز جفت‌سازی شبکه‌ای فعال نیست.")
                            Button(onClick = {
                                invite = PairingCode.create()
                                store.put("invite", invite)
                                store.put("invite_at", System.currentTimeMillis().toString())
                                log("INVITE_CREATED", "کد دعوت محلی ساخته شد")
                            }) { Text("ساخت کد دعوت") }
                            if (invite.isNotBlank()) Text(invite, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                    Card {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("۳. ثبت رضایت دستگاه همراه", fontWeight = FontWeight.Bold)
                            OutlinedTextField(value = label, onValueChange = { label = it },
                                label = { Text("نام قابل شناسایی دستگاه") }, modifier = Modifier.fillMaxWidth())
                            Text("در این نسخه فقط رضایت محلی ثبت می‌شود و هیچ داده‌ای از دستگاه دیگری دریافت نمی‌شود.")
                            Button(onClick = { showConsent = true }, enabled = label.isNotBlank()) {
                                Text("نمایش و ثبت رضایت")
                            }
                        }
                    }
                } else if (role == "companion") {
                    Card {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("۲. اعتبارسنجی دعوت", fontWeight = FontWeight.Bold)
                            OutlinedTextField(value = code, onValueChange = { code = it.uppercase(); message = "" },
                                label = { Text("کد دعوت") }, modifier = Modifier.fillMaxWidth())
                            Button(onClick = {
                                val issued = store.get("invite") ?: ""
                                val issuedAt = store.get("invite_at")?.toLongOrNull() ?: 0L
                                val valid = PairingCode.isValid(code.trim()) && code.trim() == issued &&
                                    System.currentTimeMillis() - issuedAt in 0L..300_000L
                                message = if (valid) {
                                    log("CODE_VALIDATED", "کد دعوت در همین دستگاه اعتبارسنجی شد")
                                    "کد محلی معتبر است؛ انتقال به دستگاه دیگر هنوز پیاده‌سازی نشده است."
                                } else "کد نامعتبر یا منقضی شده است."
                            }, enabled = code.isNotBlank()) { Text("اعتبارسنجی کد") }
                            if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                if (consent) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("رضایت ثبت‌شده", fontWeight = FontWeight.Bold)
                            Text("دستگاه: " + companion)
                            Text("این وضعیت فقط محلی است و اتصال شبکه‌ای ایجاد نمی‌کند.")
                            OutlinedButton(onClick = {
                                consent = false
                                companion = ""
                                store.remove("consent")
                                store.remove("companion")
                                log("CONSENT_REVOKED", "رضایت محلی لغو شد")
                            }) { Text("لغو رضایت") }
                        }
                    }
                }

                Card {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("گزارش رویدادهای محلی", fontWeight = FontWeight.Bold)
                        if (events.isEmpty()) Text("هنوز رویدادی ثبت نشده است.")
                        events.forEach { event ->
                            Text("${time(event.at)} • ${event.action}: ${event.detail}",
                                style = MaterialTheme.typography.bodySmall)
                            HorizontalDivider()
                        }
                    }
                }
                Text(
                    "حریم خصوصی: این نسخه هیچ مجوز حساس اندروید، سرویس پس‌زمینه، دوربین، میکروفون، پیامک، تماس، اعلان یا مکان را درخواست نمی‌کند.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        if (showConsent) {
            AlertDialog(
                onDismissRequest = { showConsent = false },
                title = { Text("تأیید رضایت") },
                text = { Text("آیا ثبت می‌کنی که دستگاه «$label» با آگاهی صاحب دستگاه برای مدیریت اتصال محلی وارد این طرح شده است؟ امکان لغو در هر زمان وجود دارد.") },
                confirmButton = {
                    TextButton(onClick = {
                        companion = label.trim()
                        consent = true
                        store.put("companion", companion)
                        store.put("consent", "yes")
                        store.put("consent_at", System.currentTimeMillis().toString())
                        log("CONSENT_GRANTED", "رضایت محلی برای " + companion + " ثبت شد")
                        showConsent = false
                    }) { Text("تأیید و ثبت") }
                },
                dismissButton = { TextButton(onClick = { showConsent = false }) { Text("انصراف") } }
            )
        }
    }

    private fun time(millis: Long): String =
        SimpleDateFormat("yyyy/MM/dd HH:mm", Locale("fa")).format(Date(millis))
}
