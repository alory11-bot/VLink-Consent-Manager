# VLink
اپ مستر/کلاینت اندروید. یک APK، انتخاب نقش در اجرای اول.

## ساخت
1. Android Studio، Import پروژه.
2. JDK 17 و SDK 35.
3. Build > Generate Signed Bundle/APK > APK.
4. خروجی: app/build/outputs/apk/release/app-release.apk

## رله (اختیاری)
cd server; npm i; cp .env.example .env
MASTER_TOKEN=$(openssl rand -hex 32)
همان توکن در تنظیمات مستر وارد شود.

## ترتیب
مستر: نقش Master، آدرس رله و توکن، ساخت QR.
کلاینت: نقش Client، اسکن QR، دادن مجوزها، شروع.

بدون رله: دو گوشی روی یک وای‌فای.