package com.vlink.consent

import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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

private val RoleInk = Color(0xFF17243A)
private val RoleMuted = Color(0xFF66758B)
private val RoleBlue = Color(0xFF2457A6)
private val RoleCanvas = Color(0xFFF4F7FB)
private val RoleGreen = Color(0xFFE6F5EF)
private val RoleAmber = Color(0xFFFFF3DB)

class RoleCapabilityActivity : ComponentActivity() {
    private lateinit var secureStore: SecureStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        secureStore = SecureStore(this)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(
                primary = RoleBlue, background = RoleCanvas, surface = Color.White,
                onSurface = RoleInk, secondary = Color(0xFF16866B), outline = Color(0xFFDCE4EF)
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
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("VLink", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = RoleInk)
                        Text("مدیریت نقش و دسترسی دستگاه‌ها", color = RoleMuted)
                    }
                    Surface(color = if (roleName == "MASTER") RoleBlue.copy(alpha = .10f) else RoleGreen,
                        shape = RoundedCornerShape(50)) {
                        Text(if (roleName == "MASTER") "MASTER" else if (roleName == "SLAVE") "SLAVE" else "SETUP",
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
                        "این صفحه فقط وضعیت رضایت را ثبت می‌کند؛ فعال‌سازی دوربین، میکروفون، موقعیت یا اعلان‌ها انجام نمی‌شود و مجوز سیستمی اندروید درخواست نمی‌شود. برای اجرای واقعی هر قابلیت باید نشست امن و رضایت همان لحظه پیاده‌سازی شود.")
                    OutlinedButton(onClick = { showRoleReset = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("تغییر نقش این گوشی")
                    }
                }

                HorizontalDivider()
                Text("گزارش محلی • ${events.size} رویداد اخیر", fontWeight = FontWeight.Bold, color = RoleInk)
                events.takeLast(6).reversed().forEach {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(it.action, fontWeight = FontWeight.SemiBold, color = RoleInk)
                        Text(it.detail, color = RoleMuted, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Text("VLink • نسخه آزمایشی نقش و رضایت • بدون اتصال شبکه‌ای",
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
        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = RoleInk)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(body, color = Color(0xFFD8E2F0), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    @Composable
    private fun RoleChoiceCard(title: String, description: String, onClick: () -> Unit) {
        Card(onClick = onClick, shape = RoundedCornerShape(20.dp)) {
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
        Card(shape = RoundedCornerShape(18.dp)) {
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
        Card(modifier = modifier, shape = RoundedCornerShape(16.dp)) {
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
        var status by remember { mutableStateOf("اتصال مستقیم هنوز شروع نشده است.") }
        var isRunning by remember { mutableStateOf(false) }
        var multiPoint by remember { mutableStateOf(false) }
        var peers by remember { mutableStateOf(emptyList<Pair<String, String>>()) }
        var approvalTitle by remember { mutableStateOf<String?>(null) }
        var approvalDigits by remember { mutableStateOf("") }
        var approvalAction by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }

        val transport = remember {
            NearbyP2pTransport(this@RoleCapabilityActivity, object : NearbyP2pTransport.Listener {
                override fun onStatus(message: String) { status = message }
                override fun onPeerFound(endpointId: String, endpointName: String) { status = "دستگاه نزدیک پیدا شد: $endpointName" }
                override fun onPeerLost(endpointId: String) { status = "دستگاه از محدوده کشف خارج شد." }
                override fun onVerificationRequired(endpointId: String, endpointName: String, authenticationDigits: String, decide: (Boolean) -> Unit) {
                    approvalTitle = endpointName
                    approvalDigits = authenticationDigits
                    approvalAction = decide
                }
                override fun onPeerConnected(endpointId: String, endpointName: String) {
                    peers = peers.filterNot { it.first == endpointId } + (endpointId to endpointName)
                    status = "اتصال مستقیم برقرار شد: $endpointName"
                }
                override fun onPeerDisconnected(endpointId: String) {
                    peers = peers.filterNot { it.first == endpointId }
                    status = "ارتباط یک دستگاه قطع شد."
                }
                override fun onBytesReceived(endpointId: String, bytes: ByteArray) { status = "داده مستقیم دریافت شد (${bytes.size} بایت)." }
                override fun onFailure(operation: String, statusCode: Int) {
                    isRunning = false
                    status = "اتصال برقرار نشد ($operation / $statusCode). واسطه‌ای استفاده نمی‌شود."
                }
            })
        }

        val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            if (result.values.all { it }) {
                runCatching {
                    transport.start(role, "VLink-${Build.MODEL}", if (multiPoint) Int.MAX_VALUE else 1)
                    isRunning = true
                }.onFailure { status = "شروع ارتباط ناموفق بود: ${it.localizedMessage}" }
            } else {
                status = "مجوزهای لازم برای کشف و اتصال مستقیم داده نشد."
            }
        }

        fun startDirect() {
            val required = buildList {
                if (Build.VERSION.SDK_INT >= 31) {
                    add(Manifest.permission.BLUETOOTH_ADVERTISE)
                    add(Manifest.permission.BLUETOOTH_CONNECT)
                    add(Manifest.permission.BLUETOOTH_SCAN)
                } else if (Build.VERSION.SDK_INT >= 29) {
                    add(Manifest.permission.ACCESS_FINE_LOCATION)
                } else {
                    add(Manifest.permission.ACCESS_COARSE_LOCATION)
                }
                if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.NEARBY_WIFI_DEVICES)
            }.distinct()
            val missing = required.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
            if (missing.isEmpty()) {
                runCatching {
                    transport.start(role, "VLink-${Build.MODEL}", if (multiPoint) Int.MAX_VALUE else 1)
                    isRunning = true
                }.onFailure { status = "شروع ارتباط ناموفق بود: ${it.localizedMessage}" }
            } else {
                permissionLauncher.launch(missing.toTypedArray())
            }
        }

        DisposableEffect(transport) { onDispose { transport.stop() } }

        Card(shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("اتصال مستقیم دستگاه‌ها", fontWeight = FontWeight.Bold, color = RoleInk)
                Text(
                    if (role == DeviceRole.MASTER) "این گوشی مستر است و اتصال مستقیم اسلیوها را می‌پذیرد."
                    else "این گوشی اسلیو است و مسترهای نزدیک را پیدا می‌کند.",
                    color = RoleMuted, style = MaterialTheme.typography.bodySmall
                )
                Text("نوع ارتباط", fontWeight = FontWeight.SemiBold, color = RoleInk)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (multiPoint) OutlinedButton(onClick = { multiPoint = false }, modifier = Modifier.weight(1f)) { Text("یک‌به‌یک") }
                    else Button(onClick = { multiPoint = false }, modifier = Modifier.weight(1f)) { Text("یک‌به‌یک") }
                    if (multiPoint) Button(onClick = { multiPoint = true }, modifier = Modifier.weight(1f)) { Text("یک‌به‌چند") }
                    else OutlinedButton(onClick = { multiPoint = true }, modifier = Modifier.weight(1f)) { Text("یک‌به‌چند") }
                }
                Text(status, color = RoleBlue, style = MaterialTheme.typography.bodySmall)
                if (peers.isNotEmpty()) {
                    Text("دستگاه‌های متصل: ${peers.size}", fontWeight = FontWeight.SemiBold, color = RoleInk)
                    peers.forEach { (id, name) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(name, Modifier.weight(1f), color = RoleMuted)
                            TextButton(onClick = { transport.disconnect(id) }) { Text("قطع") }
                        }
                    }
                }
                if (isRunning) {
                    OutlinedButton(onClick = {
                        transport.stop()
                        isRunning = false
                        peers = emptyList()
                        status = "ارتباط مستقیم متوقف شد."
                    }, modifier = Modifier.fillMaxWidth()) { Text("توقف کشف و اتصال") }
                } else {
                    Button(onClick = { startDirect() }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (role == DeviceRole.MASTER) "آماده‌سازی مستر برای اتصال" else "جست‌وجوی مسترهای نزدیک")
                    }
                }
                Text("بدون سرور و بدون رله؛ اتصال فقط با پشتیبانی رادیویی، مجوزهای لازم و حضور دستگاه‌ها در محدوده ارتباط برقرار می‌شود.",
                    color = RoleMuted, style = MaterialTheme.typography.labelSmall)
            }
        }

        if (approvalTitle != null) {
            AlertDialog(
                onDismissRequest = {
                    approvalAction?.invoke(false)
                    approvalAction = null
                    approvalTitle = null
                },
                title = { Text("تأیید اتصال مستقیم") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("دستگاه: $approvalTitle")
                        Text("کد احراز هویت را با گوشی مقابل مقایسه کنید:")
                        Text(approvalDigits, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text("فقط اگر کد در هر دو گوشی یکسان است و دستگاه را می‌شناسید، اتصال را تأیید کنید.")
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        approvalAction?.invoke(true)
                        approvalAction = null
                        approvalTitle = null
                    }) { Text("کد یکسان است؛ تأیید") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        approvalAction?.invoke(false)
                        approvalAction = null
                        approvalTitle = null
                    }) { Text("رد اتصال") }
                }
            )
        }
    }

    private fun statusLabel(status: CapabilityStatus): String = when (status) {
        CapabilityStatus.NOT_REQUESTED -> "درخواست نشده"
        CapabilityStatus.REQUESTED -> "در انتظار"
        CapabilityStatus.GRANTED -> "تأیید شده"
        CapabilityStatus.REJECTED -> "رد یا لغو شده"
    }
}
