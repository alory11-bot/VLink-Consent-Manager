package com.vlink.consent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.json.JSONObject

private val RoleInk = Color(0xFFF1F6FF)
private val RoleMuted = Color(0xFFB6C7DF)
private val RoleBlue = Color(0xFF83B7FF)
private val RoleCanvas = Color(0xFF091426)
private val RoleGreen = Color(0xFF174E4A)
private val RoleAmber = Color(0xFF62491F)

class RoleCapabilityActivity : ComponentActivity() {
    private lateinit var secureStore: SecureStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        secureStore = SecureStore(this)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = RoleBlue, onPrimary = Color(0xFF0B1B31), background = RoleCanvas,
                surface = Color(0xFF172943), onSurface = RoleInk, secondary = Color(0xFF75E0C2),
                outline = Color(0xFF526B8C)
            )) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    RoleCapabilityScreen()
                }
            }
        }
    }

    @Composable
    private fun RoleCapabilityScreen() {
        var roleName by remember { mutableStateOf(secureStore.get("vlink_role") ?: "") }
        var states by remember { mutableStateOf(loadStates()) }
        var events by remember { mutableStateOf(loadEvents()) }
        var showRoleReset by remember { mutableStateOf(false) }
        var info by remember { mutableStateOf("") }

        fun record(action: String, detail: String) {
            val next = (events + AuditEvent(System.currentTimeMillis(), action, detail)).takeLast(30)
            events = next
            val array = org.json.JSONArray()
            next.forEach { array.put(JSONObject().put("at", it.at).put("action", it.action).put("detail", it.detail)) }
            secureStore.put("audit", array.toString())
        }

        fun update(capability: Capability, status: CapabilityStatus, action: String) {
            states = states + (capability to status)
            val json = JSONObject()
            states.forEach { (key, value) -> json.put(key.key, value.name) }
            secureStore.put("capability_states", json.toString())
            record(action, "${capability.title}: ${statusLabel(status)}")
            info = "تغییر ثبت شد؛ این وضعیت فعلاً فقط روی همین گوشی ذخیره می‌شود."
        }

        Surface(Modifier.fillMaxSize(), color = RoleCanvas) {
            Column(
                Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF071120), Color(0xFF102A49), Color(0xFF171F3A)))).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("VLink", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Text("مدیریت نقش و دسترسی دستگاه‌ها", color = RoleMuted)
                    }
                    Surface(color = if (roleName == "MASTER") Color(0xFF244A70) else RoleGreen,
                        shape = RoundedCornerShape(50)) {
                        Text(if (roleName == "MASTER") "مستر" else if (roleName == "SLAVE") "اسلیو" else "راه‌اندازی",
                            Modifier.padding(horizontal = 12.dp, vertical = 8.dp), fontWeight = FontWeight.Bold, color = RoleInk)
                    }
                }

                if (roleName.isBlank()) {
                    HeaderCard("ابتدا نقش این گوشی را مشخص کنید",
                        "نقش در این نصب ذخیره می‌شود. مستر درخواست می‌فرستد و اسلیو درباره هر قابلیت مستقل تصمیم می‌گیرد.")
                    RoleChoiceCard("Master • مستر", "مدیریت نشست، انتخاب قابلیت‌های موردنیاز و ارسال درخواست دسترسی") {
                        roleName = DeviceRole.MASTER.name
                        secureStore.put("vlink_role", roleName)
                        record("ROLE_SELECTED", "نقش مستر انتخاب شد")
                    }
                    RoleChoiceCard("Slave • اسلیو", "دریافت درخواست‌ها، تأیید یا رد دسترسی و لغو مجوزها") {
                        roleName = DeviceRole.SLAVE.name
                        secureStore.put("vlink_role", roleName)
                        record("ROLE_SELECTED", "نقش اسلیو انتخاب شد")
                    }
                    InfoCard("نکته تست", "برای تست رابط هر نقش، نقش را انتخاب کنید. ارتباط بین دو گوشی هنوز پیاده‌سازی نشده است؛ بنابراین درخواست‌ها بین دستگاه‌ها منتقل نمی‌شوند.")
                } else {
                    HeaderCard(
                        if (roleName == "MASTER") "داشبورد مستر" else "داشبورد اسلیو",
                        if (roleName == "MASTER") "قابلیت‌های موردنیاز را درخواست کنید. هیچ دسترسی بدون تصمیم اسلیو مجاز نیست."
                        else "درخواست‌های دسترسی را بررسی کنید. اختیار تأیید، رد و لغو فقط در دست صاحب این گوشی است."
                    )
                    DirectConnectionCard(if (roleName == "MASTER") DeviceRole.MASTER else DeviceRole.SLAVE)

                    if (roleName == "MASTER") {
                        val pending = states.values.count { it == CapabilityStatus.REQUESTED }
                        val granted = states.values.count { it == CapabilityStatus.GRANTED }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatCard("در انتظار", pending.toString(), Modifier.weight(1f))
                            StatCard("تأییدشده", granted.toString(), Modifier.weight(1f))
                        }
                        Text("درخواست قابلیت", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Capability.entries.forEach { capability ->
                            val status = states[capability] ?: CapabilityStatus.NOT_REQUESTED
                            CapabilityCard(capability, status, onRequest = {
                                if (RoleCapabilityPolicy.canRequest(DeviceRole.MASTER, status)) {
                                    update(capability, CapabilityStatus.REQUESTED, "CAPABILITY_REQUESTED")
                                }
                            })
                        }
                    } else {
                        val pending = states.filterValues { it == CapabilityStatus.REQUESTED }
                        val granted = states.filterValues { it == CapabilityStatus.GRANTED }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatCard("در انتظار تصمیم", pending.size.toString(), Modifier.weight(1f))
                            StatCard("دسترسی فعال", granted.size.toString(), Modifier.weight(1f))
                        }
                        Text("درخواست‌های دسترسی", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (pending.isEmpty() && granted.isEmpty()) {
                            InfoCard("درخواستی وجود ندارد", "وقتی مستر درخواست قابلیت بفرستد، درخواست در این بخش نمایش داده می‌شود. در نسخه فعلی هنوز انتقال بین گوشی‌ها فعال نیست.")
                        }
                        Capability.entries.filter { states[it] == CapabilityStatus.REQUESTED || states[it] == CapabilityStatus.GRANTED }
                            .forEach { capability ->
                                val status = states[capability] ?: CapabilityStatus.NOT_REQUESTED
                                CapabilityCard(capability, status,
                                    onGrant = {
                                        if (RoleCapabilityPolicy.canDecide(DeviceRole.SLAVE, status))
                                            update(capability, RoleCapabilityPolicy.nextStatus(DeviceRole.SLAVE, status, true), "CAPABILITY_GRANTED")
                                    },
                                    onReject = {
                                        if (RoleCapabilityPolicy.canDecide(DeviceRole.SLAVE, status))
                                            update(capability, RoleCapabilityPolicy.nextStatus(DeviceRole.SLAVE, status, false), "CAPABILITY_REJECTED")
                                    },
                                    onRevoke = {
                                        if (status == CapabilityStatus.GRANTED)
                                            update(capability, RoleCapabilityPolicy.revoke(DeviceRole.SLAVE, status), "CAPABILITY_REVOKED")
                                    })
                            }
                    }

                    if (info.isNotBlank()) Text(info, color = RoleBlue, style = MaterialTheme.typography.bodySmall)
                    InfoCard("مرز امنیتی نسخه آزمایشی",
                        "اتصال از طریق اینترنت و WebRTC انجام می‌شود. کانال Signaling فقط برای مذاکره است و داده اصلی از مسیر P2P عبور می‌کند. دوربین، میکروفون، موقعیت و اعلان‌ها هنوز فعال نشده‌اند و هرکدام به مجوز و رضایت مستقل نیاز دارند.")
                    OutlinedButton(onClick = { showRoleReset = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("تغییر نقش این گوشی")
                    }
                }

                HorizontalDivider(color = Color(0x44FFFFFF))
                Text("گزارش محلی • ${events.size} رویداد اخیر", fontWeight = FontWeight.Bold, color = RoleInk)
                events.takeLast(6).reversed().forEach {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(it.action, fontWeight = FontWeight.SemiBold, color = RoleInk)
                        Text(it.detail, color = RoleMuted, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Text("VLink • نسخه آزمایشی ارتباط اینترنتی P2P",
                    Modifier.fillMaxWidth().padding(vertical = 8.dp), color = RoleMuted,
                    textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall)
            }
        }

        if (showRoleReset) {
            AlertDialog(
                onDismissRequest = { showRoleReset = false },
                title = { Text("تغییر نقش دستگاه؟") },
                text = { Text("نقش جدید برای این نصب ذخیره می‌شود. وضعیت درخواست‌ها و سوابق فعلی حفظ خواهند شد.") },
                confirmButton = {
                    TextButton(onClick = {
                        roleName = ""
                        secureStore.remove("vlink_role")
                        record("ROLE_RESET", "انتخاب نقش مجدداً فعال شد")
                        showRoleReset = false
                    }) { Text("تغییر نقش") }
                },
                dismissButton = { TextButton(onClick = { showRoleReset = false }) { Text("انصراف") } }
            )
        }
    }

    private fun loadStates(): Map<Capability, CapabilityStatus> = runCatching {
        val json = JSONObject(secureStore.get("capability_states") ?: "{}")
        Capability.entries.mapNotNull { capability ->
            val raw = json.optString(capability.key, CapabilityStatus.NOT_REQUESTED.name)
            capability to (runCatching { CapabilityStatus.valueOf(raw) }.getOrDefault(CapabilityStatus.NOT_REQUESTED))
        }.toMap()
    }.getOrDefault(emptyMap())

    private fun loadEvents(): List<AuditEvent> = runCatching {
        val array = org.json.JSONArray(secureStore.get("audit") ?: "[]")
        (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            AuditEvent(item.getLong("at"), item.getString("action"), item.getString("detail"))
        }.takeLast(30)
    }.getOrDefault(emptyList())

    @Composable
    private fun HeaderCard(title: String, body: String) {
        Card(shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x55FFFFFF)), modifier = Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF28577F), Color(0xFF172E4D), Color(0xFF1B2944))), RoundedCornerShape(26.dp))) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(body, color = Color(0xFFD8E2F0), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    @Composable
    private fun RoleChoiceCard(title: String, description: String, onClick: () -> Unit) {
        Card(onClick = onClick, shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1B304B)), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x55FFFFFF))) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = RoleInk)
                Text(description, color = RoleMuted)
                Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text("انتخاب این نقش") }
            }
        }
    }

    @Composable
    private fun CapabilityCard(
        capability: Capability,
        status: CapabilityStatus,
        onRequest: (() -> Unit)? = null,
        onGrant: (() -> Unit)? = null,
        onReject: (() -> Unit)? = null,
        onRevoke: (() -> Unit)? = null
    ) {
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color(0xCC1A2D47)), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x35FFFFFF))) {
            Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(capability.title, fontWeight = FontWeight.Bold, color = RoleInk)
                        Text(capability.description, color = RoleMuted, style = MaterialTheme.typography.bodySmall)
                    }
                    StatusTag(status)
                }
                if (capability.sensitive) Text("حساس • نیازمند رضایت مستقل", color = Color(0xFF8A5A00), style = MaterialTheme.typography.labelSmall)
                when {
                    onRequest != null -> Button(onClick = onRequest, enabled = status != CapabilityStatus.REQUESTED && status != CapabilityStatus.GRANTED,
                        modifier = Modifier.fillMaxWidth()) { Text(if (status == CapabilityStatus.REJECTED) "درخواست مجدد" else "درخواست دسترسی") }
                    onGrant != null && status == CapabilityStatus.REQUESTED -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onGrant, modifier = Modifier.weight(1f)) { Text("تأیید") }
                        OutlinedButton(onClick = { onReject?.invoke() }, modifier = Modifier.weight(1f)) { Text("رد") }
                    }
                    onRevoke != null && status == CapabilityStatus.GRANTED -> OutlinedButton(onClick = { onRevoke?.invoke() }, modifier = Modifier.fillMaxWidth()) { Text("لغو دسترسی") }
                }
            }
        }
    }

    @Composable
    private fun StatusTag(status: CapabilityStatus) {
        val (label, color) = when (status) {
            CapabilityStatus.NOT_REQUESTED -> "درخواست نشده" to RoleCanvas
            CapabilityStatus.REQUESTED -> "در انتظار" to RoleAmber
            CapabilityStatus.GRANTED -> "تأیید شده" to RoleGreen
            CapabilityStatus.REJECTED -> "رد / لغو شده" to RoleAmber
        }
        Surface(color = color, shape = RoundedCornerShape(50)) {
            Text(label, Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = RoleInk, style = MaterialTheme.typography.labelSmall)
        }
    }

    @Composable
    private fun StatCard(label: String, value: String, modifier: Modifier) {
        Card(modifier = modifier, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF203954)), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x44FFFFFF))) {
            Column(Modifier.fillMaxWidth().padding(14.dp)) {
                Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = RoleBlue)
                Text(label, color = RoleMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    @Composable
    private fun InfoCard(title: String, body: String) {
        Card(shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(title, fontWeight = FontWeight.Bold, color = RoleInk)
                Text(body, color = RoleMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }


    @Composable
    private fun DirectConnectionCard(role: DeviceRole) {
        var status by remember { mutableStateOf("اتصال اینترنتی هنوز شروع نشده است.") }
        var running by remember { mutableStateOf(false) }
        var endpoint by remember { mutableStateOf("wss://") }
        var inviteText by remember { mutableStateOf("") }
        var shareText by remember { mutableStateOf("") }
        var peers by remember { mutableStateOf(emptyList<String>()) }
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val transport = remember {
            WebRtcDirectTransport(this@RoleCapabilityActivity, object : WebRtcDirectTransport.Listener {
                override fun onStatus(message: String) { runOnUiThread { status = message } }
                override fun onPeerConnected(peerId: String) {
                    runOnUiThread { if (peerId !in peers) peers = peers + peerId; status = "ارتباط مستقیم اینترنتی و احراز هویت همتا برقرار شد." }
                }
                override fun onPeerDisconnected(peerId: String) {
                    runOnUiThread { peers = peers - peerId; status = "ارتباط یک دستگاه قطع شد." }
                }
                override fun onBytesReceived(peerId: String, bytes: ByteArray) {
                    runOnUiThread { status = "داده رمزنگاری‌شده از همتای احرازشده دریافت شد (${bytes.size} بایت)." }
                }
                override fun onFailure(operation: String, detail: String) {
                    runOnUiThread { status = "خطا در §{operation}: §{detail}" }
                }
            })
        }
        DisposableEffect(transport) { onDispose { transport.stop() } }

        fun start(master: Boolean) {
            val url = endpoint.trim()
            if (!url.startsWith("wss://")) {
                status = "آدرس سرور مذاکره باید با wss:// شروع شود."
                return
            }
            val invite = if (master) {
                InternetInvite.create().also { shareText = it.toShareText() }
            } else {
                InternetInvite.parse(inviteText).also { if (it == null) status = "دعوت‌نامه نامعتبر یا منقضی است." }
            } ?: return
            runCatching {
                transport.start(url, invite, master)
                running = true
                status = if (master) "در حال ساخت اتاق امن اینترنتی..." else "در حال اتصال به اتاق اینترنتی..."
            }.onFailure { status = "شروع اتصال ناموفق بود: §{it.localizedMessage}" }
        }

        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xCC192D47)), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x44FFFFFF))) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("اتصال مستقیم از طریق اینترنت", fontWeight = FontWeight.Bold, color = RoleInk)
                Text("شناسه پایدار این نصب: §{LocalDeviceIdentityStore(this@RoleCapabilityActivity).getOrCreate().shortId}",
                    color = RoleMuted, style = MaterialTheme.typography.labelSmall)
                OutlinedTextField(
                    value = endpoint, onValueChange = { endpoint = it },
                    modifier = Modifier.fillMaxWidth(), label = { Text("آدرس Signaling Server") },
                    placeholder = { Text("wss://your-domain.example") }, singleLine = true, enabled = !running
                )
                Text("این سرور فقط پیام‌های مذاکره اتصال را جابه‌جا می‌کند؛ داده اصلی از DataChannel مستقیم WebRTC عبور می‌کند.",
                    color = RoleMuted, style = MaterialTheme.typography.bodySmall)
                if (role == DeviceRole.SLAVE) {
                    OutlinedTextField(
                        value = inviteText, onValueChange = { inviteText = it },
                        modifier = Modifier.fillMaxWidth(), label = { Text("دعوت‌نامه مستر") },
                        placeholder = { Text("vlink://internet?...") }, minLines = 2, enabled = !running
                    )
                }
                Text(status, color = RoleBlue, style = MaterialTheme.typography.bodySmall)
                if (shareText.isNotBlank()) {
                    Text("دعوت‌نامه پنج‌دقیقه‌ای را فقط برای اسلیو موردنظر ارسال کنید:",
                        color = RoleMuted, style = MaterialTheme.typography.bodySmall)
                    SelectionContainer { Text(shareText, color = RoleInk, style = MaterialTheme.typography.bodySmall) }
                    OutlinedButton(onClick = {
                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("VLink invite", shareText))
                        status = "دعوت‌نامه در کلیپ‌بورد کپی شد."
                    }, modifier = Modifier.fillMaxWidth()) { Text("کپی دعوت‌نامه") }
                }
                if (peers.isNotEmpty()) {
                    Text("همتاهای احرازشده: §{peers.size}", fontWeight = FontWeight.SemiBold, color = RoleInk)
                    peers.forEach { peer ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(peer.take(8), Modifier.weight(1f), color = RoleMuted)
                            Text("احرازشده", color = Color(0xFF16866B))
                        }
                    }
                }
                if (running) {
                    OutlinedButton(onClick = {
                        transport.stop(); running = false; peers = emptyList(); shareText = ""
                        status = "اتصال اینترنتی متوقف شد."
                    }, modifier = Modifier.fillMaxWidth()) { Text("قطع ارتباط") }
                } else {
                    Button(onClick = { start(role == DeviceRole.MASTER) }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (role == DeviceRole.MASTER) "ساخت دعوت‌نامه و انتظار برای اسلیو" else "اتصال با دعوت‌نامه مستر")
                    }
                }
                Text("نیازی به Bluetooth یا کشف دستگاه نزدیک نیست. اینترنت موبایل یا Wi-Fi متصل به اینترنت قابل استفاده است. TURN هنوز فعال نشده است.",
                    color = RoleMuted, style = MaterialTheme.typography.labelSmall)
            }
        }
    }

    private fun statusLabel(status: CapabilityStatus): String = when (status) {
        CapabilityStatus.NOT_REQUESTED -> "درخواست نشده"
        CapabilityStatus.REQUESTED -> "در انتظار"
        CapabilityStatus.GRANTED -> "تأیید شده"
        CapabilityStatus.REJECTED -> "رد یا لغو شده"
    }
}
