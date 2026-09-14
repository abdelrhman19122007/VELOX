# VELOX — All-in-One Express Delivery

نظام توصيل متكامل: تطبيق كونسول + REST API (Spring Boot) + واجهة ويب (HTML/CSS/JS) + قاعدة بيانات MySQL — كلهم شغالين على **نفس الداتابيز** كمصدر واحد للحقيقة.

```
velox-project/
├── velox-backend/                 # Spring Boot API + console app (Maven, Java 17+)
│   ├── src/                       # الكود (controllers, services, dao, model, util)
│   ├── velox-db/                  # سكربتات الداتابيز
│   │   ├── VELOX_D2.SQL           # الهيكل الأساسي + كتالوج المنتجات
│   │   └── migrations/            # ترحيلات V2..V6 (تطبق بالترتيب بعد الأساسي)
│   ├── pom.xml
│   ├── mvnw / mvnw.cmd            # Maven Wrapper (مش محتاج تثبت Maven)
│   └── README.md                  # خريطة الـ endpoints والمعمارية بالتفصيل
├── velox-frontend/                # المتجر (HTML/CSS/JS بدون build)
│   ├── index.html                 # الواجهة الرئيسية (ابدأ من هنا)
│   ├── account/orders/...html     # الحساب والطلبات وباقي الصفحات
│   └── js/ + css/ + assets/       # المنطق والتنسيقات والصور
├── .gitignore
└── README.md                      # (الملف ده)
```

## المزايا الشغالة

- تسجيل/دخول بالإيميل والباسورد + كود تحقق OTP (6 أرقام) + توكن جلسات في الداتابيز.
- كتالوج منتجات من الداتابيز (صور + عربي/إنجليزي)، أقسام، محافظات الـ 27 بأسعار الشحن.
- طلبات من الموقع بسعر محسوب في السيرفر + خصم مخزون + فاتورة PDF بالعربي.
- محفظة (شحن كارت/محفظة إلكترونية + حفظ طرق الدفع) والدفع منها.
- ولاء (كل 5 طلبات ناجحة = توصيل مجاني)، عروض مخصصة، إشعارات دائمة، تتبع حي.
- إرجاع بدورة كاملة (طلب → مراجعة → موافقة → استرداد للمحفظة = الإجمالي − 2× الشحن).
- شكاوى وتقييمات مربوطة بالطلبات. تطبيق الكونسول مربوط بنفس الداتابيز.

## التشغيل من الصفر (أي جهاز)

### المتطلبات
- **JDK 17** أو أحدث (متجرب على Temurin 25).
- **MySQL 8** شغالة.
- متصفح حديث. (المافن بينزل لوحده أول مرة — محتاج إنترنت).

### 1. الداتابيز (مرة واحدة)
1. اعمل داتابيز فاضية اسمها `velox_db`.
2. استورد `velox-backend/velox-db/VELOX_D2.SQL` الأول.
3. طبّق ملفات `velox-backend/velox-db/migrations/` **بالترتيب الرقمي** (V2 ثم V3...).
4. تأكد: جدول `products` ≈ 25 صف، وجداول `sessions` و`return_requests` و`user_notifications` و`otp_codes` موجودة.

### 2. إعدادات الباك (مرة واحدة لكل جهاز)
انسخ `velox-backend/src/main/resources/application.properties.example`
إلى `application.properties` (نفس المجلد) واكتب باسورد MySQL بتاع جهازك،
**أو** (الأفضل) عرّف متغيرات البيئة — هي اللي بتتكسب دايماً:
```powershell
setx VELOX_DB_URL "jdbc:mysql://localhost:3306/velox_db"
setx VELOX_DB_USER "root"
setx VELOX_DB_PASSWORD "باسورد جهازك"
```
> ملف `application.properties` خارج تتبع Git عمداً — كل جهاز يحتفظ بنسخته.

### 3. تشغيل الباك اند
```powershell
cd velox-backend
.\mvnw.cmd spring-boot:run
```
استنى سطر `Started VeloxApplication`، وجرّب في المتصفح:
`http://localhost:8080/api/governorates/CAIRO` (لازم يرجع JSON).

### 4. تشغيل الفرونت
دبل كليك على `velox-frontend/index.html` (الباك لازم يكون شغال الأول).
سجّل حساب جديد (أي محافظة حتى أسيوط/المنوفية) → أكّد كود الـ OTP →
تصفح → اشحن المحفظة → اطلب → حمّل الفاتورة → تابع التتبع.

### 5. الكونسول (اختياري)
```powershell
cd velox-backend
.\mvnw.cmd exec:java
```
ملاحظة: `java -jar` بيشغل الـ API مش الكونسول.

## للمطورين
- خريطة الـ endpoints الكاملة: `velox-backend/README.md`.
- البنية JDBC خام (مش JPA) — أي استعلام في `velox-backend/src/main/java/com/app/dao/`.
- Single source of truth هي MySQL؛ ملفات `data/` كاش محلي (متتجاهَلة).
- الفروع: الشغل الأساسي على `main`. ممنوع push مباشر لأسرار أو ملفات `.dat`.
- مشاكل شائعة: `Access denied` = MySQL واقفة أو الباسورد غلط؛ `Port 8080 in use` = شباك سيرفر قديم مفتوح؛ أحمر في IntelliJ = Maven Reload + JDK 17+ + تفعيل annotation processing (Lombok).
