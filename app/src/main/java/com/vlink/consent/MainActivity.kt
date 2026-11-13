package com.vlink.consent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Ink = Color(0xFF17243A)
private val Muted = Color(0xFF66758B)
private val Brand = Color(0xFF2457A6)
private val Canvas = Color(0xFFF4F7FB)
private val Mint = Color(0xFFE6F5EF)
private val Amber = Color(0xFFFFF3DB)

class MainActivity : ComponentActivity() {
    private lateinit var store: SecureStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = SecureStore(this)
        setContent {
            val colors = lightColorScheme(
                primary = Brand,
                onPrimary = Color.White,
                background = Canvas,
                surface = Color.White,
                onSurface = Ink,
                secondary = Color(0xFF16866B),
                outline = Color(0xFFDCE4EF)
            )
            MaterialTheme(colorScheme = colors) {
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
        var inviteAt by remember { mutableLongStateOf(store.get("invite_at")?.toLongOrNull() ?: 0L) }
        var companion by remember { mutableStateOf(store.get("companion") ?: "") }
        var consent by remember { mutableStateOf(store.get("consent") == "yes") }
        var events by remember { mutableStateOf(readEvents()) }
        var showConsent by remember { mutableStateOf(false) }
        var label by remember { mutableStateOf("") }
        var message by remember { mutableStateOf("") }
        var selectedPage by remember { mutableStateOf("home") }
        val clipboard = LocalClipboardManager.current

        fun log(action: String, detail: String) {
            addEvent(action, detail)
            events = readEvents()
        }

        val isInviteValid = invite.isNotBlank() &&
            PairingCode.isValidForWindow(invite, inviteAt, System.currentTimeMillis())

        Surface(Modifier.fillMaxSize(), color = Canvas) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(Brand),
                        contentAlignment = Alignment.Center
                    ) { Text("V", color = Color.White, fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineMedium) }
                    Column(Modifier.weight(1f)) {
                        Text("VLink", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = Ink)
                        Text("مدیریت اتصال با رضایت شفاف", style = MaterialTheme.typography.bodySmall, color = Muted)
                    }
                    StatusPill(if (consent) "رضایت فعال" else "بدون اتصال", if (consent) Mint else Amber)
                }

                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Ink)
                ) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("حریم خصوصی، تحت کنترل شماست", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                        Text(
                            "دعوت‌نامه محلی بسازید، رضایت را آگاهانه ثبت کنید و هر زمان خواستید آن را لغو کنید.",
                            color = Color(0xFFD8E2F0), style = MaterialTheme.typography.bodyMedium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MetricChip("۳۰", "رویداد قابل نگهداری")
                            MetricChip("۵ دقیقه", "اعتبار کد دعوت")
                        }
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PageChip("خانه", selectedPage == "home", Modifier.weight(1f)) { selectedPage = "home" }
                    PageChip("رویدادها", selectedPage == "events", Modifier.weight(1f)) { selectedPage = "events" }
                    PageChip("درباره", selectedPage == "about", Modifier.weight(1f)) { selectedPage = "about" }
                }

                when (selectedPage) {
                    "home" -> {
                        SectionTitle("نقش دستگاه", "نقش این گوشی را برای گردش کار محلی انتخاب کنید")
                        Card(shape = RoundedCornerShape(20.dp)) {
                            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    RoleButton("کنترل‌کننده", role == "controller", Modifier.weight(1f)) {
                                        role = "controller"; store.put("role", role)
                                        log("ROLE", "نقش کنترل‌کننده انتخاب شد")
                                    }
                                    RoleButton("دستگاه همراه", role == "companion", Modifier.weight(1f)) {
                                        role = "companion"; store.put("role", role)
                                        log("ROLE", "نقش دستگاه همراه انتخاب شد")
                                    }
                                }
                                Text(
                                    if (role.isBlank()) "هنوز نقشی انتخاب نشده است."
                                    else "نقش انتخاب‌شده: " + if (role == "controller") "کنترل‌کننده" else "دستگاه همراه",
                                    color = Muted, style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        if (role == "controller") {
                            SectionTitle("دعوت‌نامه یک‌بارمصرف", "کد محلی با اعتبار پنج دقیقه‌ای")
                            Card(shape = RoundedCornerShape(20.dp)) {
                                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("برای شروع فرایند آزمایشی، یک کد دعوت تولید کنید.", color = Muted)
                                    if (invite.isNotBlank()) {
                                        Box(
                                            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                                                .background(if (isInviteValid) Mint else Amber).padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text(invite, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold, color = Ink)
                                                Text(if (isInviteValid) "کد معتبر است" else "کد منقضی شده است", color = Muted, style = MaterialTheme.typography.labelMedium)
                                            }
                                        }
                                        OutlinedButton(
                                            onClick = { clipboard.setText(AnnotatedString(invite)); message = "کد در کلیپ‌بورد کپی شد." },
                                            modifier = Modifier.fillMaxWidth()
                                        ) { Text("کپی کد دعوت") }
                                        if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                                    }
                                    Button(
                                        onClick = {
                                            invite = PairingCode.create()
                                            inviteAt = System.currentTimeMillis()
                                            store.put("invite", invite)
                                            store.put("invite_at", inviteAt.toString())
                                            message = ""
                                            log("INVITE_CREATED", "کد دعوت محلی ساخته شد")
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text(if (invite.isBlank()) "ساخت کد دعوت" else "ساخت کد جدید") }
                                    Text("این کد در نسخه فعلی فقط روی همین نصب ذخیره می‌شود و به گوشی دیگری ارسال نمی‌شود.", color = Muted, style = MaterialTheme.typography.bodySmall)
                                }
                            }

                            SectionTitle("ثبت رضایت", "ثبت نام دستگاه و تأیید صریح")
                            Card(shape = RoundedCornerShape(20.dp)) {
                                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(
                                        value = label, onValueChange = { label = it },
                                        label = { Text("نام قابل شناسایی دستگاه") },
                                        placeholder = { Text("مثلاً گوشی شخصی") },
                                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    Button(onClick = { showConsent = true }, enabled = label.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                                        Text("مرور و ثبت رضایت")
                                    }
                                }
                            }
                        } else if (role == "companion") {
                            SectionTitle("اعتبارسنجی کد دعوت", "کد را از کنترل‌کننده دریافت کنید")
                            Card(shape = RoundedCornerShape(20.dp)) {
                                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(
                                        value = code, onValueChange = { code = it.uppercase().filter { c -> c.isLetterOrDigit() }.take(12); message = "" },
                                        label = { Text("کد دعوت") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    Button(onClick = {
                                        val issued = store.get("invite") ?: ""
                                        val issuedAt = store.get("invite_at")?.toLongOrNull() ?: 0L
                                        val valid = PairingCode.isValidForWindow(code.trim(), issuedAt, System.currentTimeMillis()) &&
                                            code.trim() == issued
                                        message = if (valid) {
                                            store.remove("invite"); store.remove("invite_at")
                                            invite = ""; inviteAt = 0L
                                            log("CODE_VALIDATED", "کد دعوت محلی مصرف شد")
                                            "کد محلی معتبر بود؛ ارتباط بین دو دستگاه هنوز فعال نیست."
                                        } else "کد نامعتبر، منقضی یا در این گوشی صادر نشده است."
                                    }, enabled = code.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("بررسی کد") }
                                    if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                                    Text("اعتبارسنجی فعلی محلی است؛ برای جفت‌سازی واقعی باید تبادل امن بین دو دستگاه پیاده‌سازی شود.", color = Muted, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        } else {
                            InfoCard("برای شروع", "یکی از دو نقش را انتخاب کنید. امکانات بعدی بر اساس نقش انتخابی نمایش داده می‌شوند.")
                        }

                        if (consent) {
                            SectionTitle("وضعیت رضایت", "قابل مشاهده و قابل لغو")
                            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Mint)) {
                                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("رضایت ثبت شده است", fontWeight = FontWeight.Bold, color = Color(0xFF17664F))
                                    Text("دستگاه: $companion", color = Ink)
                                    Text("این ثبت، محلی است و به معنی اتصال شبکه‌ای نیست.", color = Muted, style = MaterialTheme.typography.bodySmall)
                                    OutlinedButton(onClick = {
                                        consent = false; companion = ""
                                        store.remove("consent"); store.remove("companion")
                                        log("CONSENT_REVOKED", "رضایت محلی لغو شد")
                                    }, modifier = Modifier.fillMaxWidth()) { Text("لغو رضایت") }
                                }
                            }
                        }
                    }
                    "events" -> {
                        SectionTitle("گزارش رویدادها", "آخرین ۳۰ رویداد محلی")
                        Card(shape = RoundedCornerShape(20.dp)) {
                            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                if (events.isEmpty()) Text("هنوز رویدادی ثبت نشده است.", color = Muted)
                                events.forEach { event ->
                                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text(event.action, color = Ink, fontWeight = FontWeight.SemiBold)
                                        Text(event.detail, color = Muted, style = MaterialTheme.typography.bodySmall)
                                        Text(time(event.at), color = Muted, style = MaterialTheme.typography.labelSmall)
                                    }
                                    HorizontalDivider(color = Color(0xFFE8EDF4))
                                }
                            }
                        }
                    }
                    else -> {
                        SectionTitle("درباره VLink", "نسخه آزمایشی مدیریت رضایت")
                        InfoCard("ذخیره‌سازی امن", "داده‌های محلی با AES-GCM رمزنگاری می‌شوند و کلید در Android Keystore نگهداری می‌شود.")
                        InfoCard("حدود نسخه فعلی", "این نسخه رابط و گردش‌کار رضایت محلی را ارائه می‌کند. سرور، جفت‌سازی واقعی، TLS بین دستگاه‌ها و تبادل داده در آن فعال نیست.")
                        InfoCard("حریم خصوصی", "مجوز دوربین، میکروفون، پیامک، تماس، اعلان، مکان یا Accessibility درخواست نمی‌شود. هیچ فرمان راه‌دور یا پایش پنهانی اجرا نمی‌شود.")
                    }
                }

                Text(
                    "VLink • نسخه آزمایشی محلی • اطلاعات این نسخه به دستگاه دیگری ارسال نمی‌شود.",
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    color = Muted, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center
                )
            }
        }

        if (showConsent) {
            AlertDialog(
                onDismissRequest = { showConsent = false },
                title = { Text("تأیید آگاهانه رضایت") },
                text = { Text("آیا ثبت می‌کنید که صاحب دستگاه «$label» با آگاهی و اختیار خود در این فرایند آزمایشی محلی مشارکت می‌کند؟ این رضایت در هر زمان قابل لغو است. هیچ داده‌ای از دستگاه دیگر جمع‌آوری نمی‌شود.") },
                confirmButton = {
                    TextButton(onClick = {
                        companion = label.trim()
                        consent = true
                        store.put("companion", companion)
                        store.put("consent", "yes")
                        store.put("consent_at", System.currentTimeMillis().toString())
                        log("CONSENT_GRANTED", "رضایت محلی برای $companion ثبت شد")
                        showConsent = false
                    }) { Text("تأیید و ثبت") }
                },
                dismissButton = { TextButton(onClick = { showConsent = false }) { Text("بازگشت") } }
            )
        }
    }

    @Composable
    private fun SectionTitle(title: String, subtitle: String) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Ink)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Muted)
        }
    }

    @Composable
    private fun InfoCard(title: String, body: String) {
        Card(shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, fontWeight = FontWeight.Bold, color = Ink)
                Text(body, color = Muted, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    @Composable
    private fun StatusPill(text: String, color: Color) {
        Text(text, Modifier.clip(CircleShape).background(color).padding(horizontal = 11.dp, vertical = 7.dp),
            color = Ink, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }

    @Composable
    private fun MetricChip(value: String, label: String) {
        Column(Modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xFF2A3A53)).padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(value, color = Color.White, fontWeight = FontWeight.Bold)
            Text(label, color = Color(0xFFD8E2F0), style = MaterialTheme.typography.labelSmall)
        }
    }

    @Composable
    private fun PageChip(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
        if (selected) Button(onClick = onClick, modifier = modifier, contentPadding = PaddingValues(vertical = 10.dp)) { Text(text) }
        else OutlinedButton(onClick = onClick, modifier = modifier, contentPadding = PaddingValues(vertical = 10.dp)) { Text(text) }
    }

    @Composable
    private fun RoleButton(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
        if (selected) Button(onClick = onClick, modifier = modifier) { Text(text) }
        else OutlinedButton(onClick = onClick, modifier = modifier) { Text(text) }
    }

    private fun time(millis: Long): String =
        SimpleDateFormat("yyyy/MM/dd HH:mm", Locale("fa")).format(Date(millis))
}
