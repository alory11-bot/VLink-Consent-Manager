package com.vlink.consent

enum class DeviceRole { MASTER, SLAVE }

enum class Capability(val key: String, val title: String, val description: String, val sensitive: Boolean = false) {
    FILE_TRANSFER("file_transfer", "انتقال فایل", "درخواست دریافت یا ارسال فایل با تأیید هر انتقال"),
    MESSAGES("messages", "پیام بین دستگاه‌ها", "تبادل پیام متنی در نشست فعال"),
    LOCATION("location", "اشتراک‌گذاری موقعیت", "اشتراک‌گذاری محدود و زمان‌دار موقعیت", true),
    CAMERA("camera", "جلسه دوربین", "شروع دوربین فقط با نمایش اعلان و تأیید کاربر", true),
    MICROPHONE("microphone", "جلسه صوتی", "فعال‌سازی میکروفون فقط در نشست قابل مشاهده", true),
    NOTIFICATIONS("notifications", "اشتراک‌گذاری اعلان‌ها", "انتخاب آگاهانه برنامه‌ها و اعلان‌های قابل اشتراک", true)
}

enum class CapabilityStatus { NOT_REQUESTED, REQUESTED, GRANTED, REJECTED }

object RoleCapabilityPolicy {
    fun canRequest(role: DeviceRole, status: CapabilityStatus): Boolean =
        role == DeviceRole.MASTER && status != CapabilityStatus.REQUESTED && status != CapabilityStatus.GRANTED

    fun canDecide(role: DeviceRole, status: CapabilityStatus): Boolean =
        role == DeviceRole.SLAVE && status == CapabilityStatus.REQUESTED

    fun nextStatus(role: DeviceRole, current: CapabilityStatus, grant: Boolean): CapabilityStatus {
        require(canDecide(role, current)) { "Only the slave can decide a pending capability request" }
        return if (grant) CapabilityStatus.GRANTED else CapabilityStatus.REJECTED
    }

    fun revoke(role: DeviceRole, current: CapabilityStatus): CapabilityStatus {
        require(role == DeviceRole.SLAVE) { "Only the slave can revoke a granted capability" }
        require(current == CapabilityStatus.GRANTED) { "Only a granted capability can be revoked" }
        return CapabilityStatus.REJECTED
    }
}
