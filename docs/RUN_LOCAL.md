# VELOX — تشغيل المشروع على جهازك (Local) خطوة بخطوة

الملف ده بيشرح **كل حاجة حرفياً**: إيه اللي بيشتغل، وبيشتغل إزاي، وكل أمر بالترتيب، وإزاي تختبر، وإيه تعمل لو حاجة باظت.

> **عايز تشغّله وبس؟** الطريقة الأسهل بدبل كليك (Docker بس، من غير Java ولا Flutter): [TEAM_SETUP.md](TEAM_SETUP.md).
> الملف ده للي عايز يفهم كل خطوة أو يشغّل السيرفر من غير Docker.
كل الأوامر مكتوبة لـ **Windows PowerShell**، وبتتنفّذ من فولدر المشروع (اللي فيه `backend` و`mobile` و`web`).

---

## ٠. الصورة الكبيرة: إيه اللي بيشتغل على جهازك

```
 الموبايل / المتصفح
        │  HTTPS
        ▼
 Cloudflare Tunnel (cloudflared.exe)  ←── اختياري: بيدّي لينك عام https://xxxx.trycloudflare.com
        │  HTTP
        ▼
 السيرفر Java (velox-backend-2.0.0.jar) على http://localhost:8080
   ├── /api/...        الـ API (Spring Boot)
   ├── /               الموقع ولوحة التحكم   (فولدر web/)
   └── /app/           التطبيق في المتصفح    (فولدر mobile/build/web/)
        │  JDBC (PostgreSQL)
        ▼
 قاعدة البيانات: Supabase (على النت)  أو  PostgreSQL في Docker (على جهازك)
```

| الجزء | بيشتغل فين | بيتشغّل إزاي |
|---|---|---|
| قاعدة البيانات | Supabase (افتراضي) أو Docker | Supabase شغالة لوحدها 24 ساعة. Docker: `docker compose up -d db` |
| السيرفر | جهازك، بورت 8080 | `java -jar backend\target\velox-backend-2.0.0.jar` |
| الموقع + لوحة التحكم | جوه السيرفر نفسه | مفيش حاجة، السيرفر بيعرضهم |
| التطبيق في المتصفح | جوه السيرفر (`/app/`) | يتبني مرة واحدة بـ `flutter build web` |
| تطبيق الأندرويد | الموبايل | APK من `/downloads/velox.apk` |
| اللينك العام | Cloudflare | `cloudflared tunnel --url http://localhost:8080` |

> **مهم:** لو الجهاز اتقفل أو عمل Restart، السيرفر والـ tunnel بيقفوا. لازم تشغّلهم تاني (الخطوات 4 و6). وقاعدة البيانات على Supabase مش بتتأثر.

---

## ١. البرامج المطلوبة (مرة واحدة)

| البرنامج | النسخة | ليه | إزاي تتأكد |
|---|---|---|---|
| **Java JDK** | 17 | تشغيل وبناء السيرفر | `java -version` |
| **Git** | أي نسخة | الكود | `git --version` |
| **Flutter** | 3.x (Dart 3) | بناء التطبيق | `flutter --version` |
| **Python** | 3.10+ | سكريبتات الاختبار | `python --version` |
| **cloudflared** | أي نسخة | اللينك العام (اختياري) | موجود في `%USERPROFILE%\cloudflared\cloudflared.exe` |
| **Docker Desktop** | اختياري | قاعدة بيانات محلية + اختبارات JUnit الكاملة | `docker info` |
| **Android SDK** | عن طريق Flutter | بناء الـ APK بس | `flutter doctor` |

**Maven مش محتاج يتسطّب**: المشروع فيه `mvnw.cmd` بينزّله لوحده.

على جهاز نبيه بالذات، الكاش متحوّل لـ `E:` عشان المساحة. قبل أي أمر Flutter:
```powershell
$env:PUB_CACHE = 'E:\DevTools\pub-cache'
$env:GRADLE_USER_HOME = 'E:\DevTools\gradle-home'
```

---

## ٢. الإعدادات والأسرار (فين وإيه)

**مفيش أي سر في الكود ولا على GitHub.** كل الأسرار على جهازك في ملفين:

| الملف | فيه إيه | مترفوع على GitHub؟ |
|---|---|---|
| `deploy\.env` | إعدادات السيرفر (أسماء تحت) | ❌ (في `.gitignore`) |
| `..\..\_secrets\supabase.txt` | باسورد Supabase (أول سطر) | ❌ برّه الريبو خالص |
| `..\..\_secrets\VELOX-accounts.txt` | حسابات المطاعم والمناديب التجريبية | ❌ برّه الريبو |

**المفاتيح اللي في `deploy\.env`** (الأسماء بس):

| المفتاح | معناه |
|---|---|
| `DB_URL` | عنوان قاعدة البيانات: `jdbc:postgresql://aws-0-eu-north-1.pooler.supabase.com:5432/postgres?sslmode=require` |
| `DB_USER` | يوزر قاعدة البيانات |
| `DB_PASSWORD_FILE` | **مسار** ملف الباسورد (مش الباسورد نفسه) |
| `VELOX_TOKEN_SECRET` | مفتاح توقيع تسجيل الدخول (32 حرف عشوائي على الأقل) |
| `VELOX_ADMIN_EMAIL` / `VELOX_ADMIN_PASSWORD` | أول أدمن، بيتعمل أول مرة بس لو مش موجود |
| `VELOX_API_DOCS` | `false` = صفحة Swagger مقفولة |
| `VELOX_PAYMENTS_SANDBOX` | `false` = كاش بس (الدفع التجريبي مقفول) |
| `VELOX_DB_PORT` / `VELOX_DB_NAME` | لقاعدة البيانات المحلية في Docker بس |

**إزاي السيرفر بيقرا الإعدادات:** `backend\src\main\resources\application.properties` فيه سطور زي
`spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/velox}`، يعني خد `DB_URL` من البيئة، ولو مش موجود استخدم localhost.

---

## ٣. قاعدة البيانات: اختار طريقة

### (أ) Supabase — الافتراضي، مش محتاج حاجة
قاعدة البيانات على النت وشغالة لوحدها. كل اللي محتاجه إن `DB_URL` و`DB_USER` و`DB_PASSWORD_FILE` يكونوا في `deploy\.env`.

### (ب) PostgreSQL على جهازك (Docker)
```powershell
docker compose up -d db                  # PostgreSQL 16 على بورت 5432، يوزر وباسورد velox
docker exec velox-db pg_isready -U velox # لازم تطلع: accepting connections
```
وفي الخطوة 4 خلّي `DB_URL=jdbc:postgresql://localhost:5432/velox` و`DB_USER=velox` و`DB_PASSWORD=velox`.

**الجداول بتتعمل لوحدها:** أول ما السيرفر يقوم، **Flyway** بيشغّل الملفات `database/migrations/V1…V7` اللي لسه ماتشغلتش، بالترتيب، وبيسجّلها في جدول `flyway_schema_history`.

---

## ٤. تشغيل السيرفر

### ٤.١ بناء ملف الـ JAR (أول مرة، أو بعد أي تعديل في `backend`)
```powershell
cd backend
.\mvnw.cmd -q -DskipTests package        # بيطلّع backend\target\velox-backend-2.0.0.jar
cd ..
```

### ٤.٢ التشغيل
```powershell
# 1) حمّل الإعدادات من deploy\.env
Get-Content deploy\.env | Where-Object { $_ -match '^[A-Za-z_.]+=' } |
  ForEach-Object { $k, $v = $_ -split '=', 2; Set-Item "env:$k" $v }

# 2) الباسورد من الملف (مش بيتكتب في أي حتة)
$env:DB_PASSWORD = (Get-Content $env:DB_PASSWORD_FILE -TotalCount 1).Trim()

# 3) شغّل من جوه فولدر backend (عشان يلاقي ..\web و ..\mobile\build\web)
cd backend
java -XX:-UseBASE64Intrinsics -jar target\velox-backend-2.0.0.jar
```
`-XX:-UseBASE64Intrinsics` ده حماية من ثغرة **CVE-2026-114938** (اسمها "Java4Shell" أو "Java2Shell") اللي في Java 17 على المعالجات اللي فيها AVX-512 VBMI. بيقفل الجزء المصاب، ومش بيأثر على حاجة غير سرعة صغيرة جداً.
هتشوف `Started VeloxApplication in … seconds`. **سيب الشباك ده مفتوح**، ولو قفلته السيرفر يقف.

### ٤.٣ اتأكد إنه شغال (من شباك تاني)
| افتح | لازم يطلع |
|---|---|
| http://localhost:8080/api/zones | `[{"code":"SAMANNOUD",…}]` |
| http://localhost:8080/ | الموقع |
| http://localhost:8080/admin/ | لوحة التحكم |
| http://localhost:8080/app/ | التطبيق (بعد الخطوة 5) |
| http://localhost:8080/swagger-ui.html | كل عناوين الـ API، لو `VELOX_API_DOCS=true` |

### ٤.٤ إيه اللي بيحصل جوه وهو بيقوم
1. Spring Boot بيقرا الإعدادات ويشغّل **Tomcat** على 8080.
2. **HikariCP** بيفتح مجموعة اتصالات بقاعدة البيانات (Connection Pool).
3. **Flyway** بيتأكد إن الجداول كاملة.
4. **AdminBootstrap** بيعمل أول أدمن لو مش موجود.
5. بيشتغل **StaleOrderCloser** كل دقيقة: يلغي أي طلب المطعم مردّش عليه في 15 دقيقة، أو أي طلب مفيش مندوب خده في 40 دقيقة.
6. **EventHub** جاهز لأي تطبيق يفتح `/api/events` عشان يوصله التحديث المباشر (SSE).

---

## ٥. الواجهات

### ٥.١ الموقع ولوحة التحكم
مش محتاجين بناء، دول ملفات HTML/JS في `web/` والسيرفر بيعرضهم على طول.

### ٥.٢ التطبيق في المتصفح (`/app/`)
```powershell
cd mobile
flutter pub get
flutter build web --release --base-href /app/     # بيطلّع mobile\build\web
cd ..
```
لو بتبني من **Git Bash** مش PowerShell، اكتب قبلها `export MSYS_NO_PATHCONV=1`، وإلا `/app/` هيتحوّل لمسار Windows غلط.
**بعد البناء:** المتصفح ممكن يفضل فاكر النسخة القديمة، فاعمل **Ctrl+Shift+R**.

### ٥.٣ تطبيق الأندرويد (APK)
```powershell
cd mobile
flutter build apk --release --split-per-abi   # نسخة لكل نوع معالج
flutter build apk --release                   # نسخة universal
cd ..
copy mobile\build\app\outputs\flutter-apk\app-arm64-v8a-release.apk web\downloads\velox.apk
copy mobile\build\app\outputs\flutter-apk\app-release.apk           web\downloads\velox-universal.apk
```
وبعدها زوّد رقم `build` في `web\downloads\version.json`، لازم يساوي `appBuild` في `mobile\lib\core\app_state.dart` و`version` في `mobile\pubspec.yaml`. التطبيقات القديمة بتقارن الرقم ده وتقول "فيه نسخة جديدة".

**Release vs Debug:** نسخة الـ Release بترفض أي اتصال `http://` من غير تشفير (`usesCleartextTraffic=false`)، فلازم تكلم السيرفر بـ HTTPS (الخطوة 6). نسخة الـ Debug مسموحلها بـ http عشان التطوير.

---

## ٦. لينك عام (عشان الموبايل أو أي حد برّه)

```powershell
& "$env:USERPROFILE\cloudflared\cloudflared.exe" tunnel --url http://localhost:8080
```
هيطبع لينك زي `https://xxxx-yyyy.trycloudflare.com`. **سيب الشباك مفتوح.**

**التطبيق بيلاقي اللينك الجديد لوحده:** ابعت اللينك على قناة ntfy:
```powershell
Invoke-RestMethod -Method Post -Uri https://ntfy.sh/velox-nabeeh-k7q2m9 -Body 'https://xxxx-yyyy.trycloudflare.com'
```
التطبيق بيقرا القناة دي (`discoverServer()` في `mobile/lib/core/api.dart`)، و**مبيقبلش** غير لينكات `https://….trycloudflare.com`. ده عشان لو حد غريب بعت لينك تاني على القناة، التطبيق ميتصلش بيه. وتقدر كمان تكتب اللينك بإيدك من **الإعدادات ← رابط السيرفر**.

> اللينك **بيتغير كل مرة** تشغّل cloudflared. وده سبب إن المشروع محتاج سيرفر ثابت لو هيشتغل بجد.

---

## ٧. أمر واحد بيعمل كل ده: `deploy\start-public.ps1`
السكريبت ده بيعمل الخطوات 3 لـ 6 لوحده، وبيعيد فتح الـ tunnel لو النت قطع:
```powershell
powershell -ExecutionPolicy Bypass -File deploy\start-public.ps1
```
⚠️ **Avast بيمسح الملف ده** لأنه بيشغّل tunnel ويبعت على النت، وده شكل مريب لبرامج الحماية. عشان تستخدمه:
1. Avast ← **Menu ← Settings ← General ← Exceptions ← Add exception** ← ضيف فولدر المشروع.
2. Avast ← **Menu ← Quarantine** ← رجّع `start-public.ps1` (Restore).
ولو مش عايز تعمل استثناء، استخدم الخطوات اليدوية 4 و6. هي نفس الحاجة بالظبط.

---

## ٨. الاختبارات: كل نوع وإزاي يتشغّل

| النوع | الأمر | محتاج إيه | آخر نتيجة (18 نوفمبر 2026) |
|---|---|---|---|
| **JUnit (Unit)**: كلاس لوحده | `cd backend; .\mvnw.cmd test "-Dtest=CatalogUnitTest,OrderStatusTest,PasswordHasherTest,TokenServiceTest"` | Java بس | **15 / 15** |
| **JUnit (كله)**: Unit + Integration | `cd backend; .\mvnw.cmd test` | **Docker شغال** (Testcontainers بيشغّل PostgreSQL حقيقي) | 94 / 94 (26 أكتوبر). لسه متعادش لأن Docker مش شغال |
| **تغطية الكود** | `.\mvnw.cmd verify` ← `backend\target\site\jacoco\index.html` | Docker | 86 % (26 أكتوبر) |
| **فحص أمني للكود** | `.\mvnw.cmd -DskipTests compile spotbugs:check` (SpotBugs + FindSecBugs) | Java | 0 مشاكل (26 أكتوبر) |
| **فحص السيرفر الحقيقي (E2E)** | `python tests\e2e_live_check.py <الرابط> tests\results\<التاريخ>-e2e-live.json` | السيرفر شغال | **45 / 45** عن طريق HTTPS |
| **Flutter** | `cd mobile; flutter test; flutter analyze lib` | Flutter | **6 / 6**، والفحص من غير مشاكل |
| **API (Postman)** | `newman run tests\api\velox.postman_collection.json` | Node + newman | 30 / 30 طلب (26 أكتوبر) |
| **الموقع (Selenium)** | `cd tests\e2e; mvn test` | Chrome | 4 / 4 (26 أكتوبر) |
| **الحِمل (100 مستخدم)** | `python tests\performance\quick_load.py --base http://localhost:8080 --users 100 --rounds 10` أو JMeter: `jmeter -n -t tests\performance\velox-browse.jmx` | السيرفر شغال | شوف `tests/TEST_REPORT.md` |

**فحص السيرفر الحقيقي (`e2e_live_check.py`) بيعمل إيه بالظبط:** بيفتح الصفحات، ويتأكد من رؤوس الأمان وإن CORS بيمنع المواقع الغريبة وإن Swagger و`.env` مقفولين، ويعمل عميل جديد، ويطلب، ويخلّي المطعم يقبل ويقول جاهز، ويخلّي مندوب ياخد ويستلم ويسلّم، ويقيّم. وفي الآخر **بيمسح كل الحسابات اللي عملها**، عشان الداتا الحقيقية متتوسخش.

النتايج كلها محفوظة في `tests/results/`، والتقرير في `tests/TEST_REPORT.md`.

---

## ٩. الإيقاف
- السيرفر: اقفل الشباك بتاعه، أو **Ctrl+C**.
- الـ tunnel: اقفل الشباك بتاعه.
- Docker (لو استخدمته): `docker compose stop db`. **الداتا بتفضل موجودة.**

---

## ١٠. مشاكل وحلولها

| اللي ظاهر | السبب | الحل |
|---|---|---|
| التطبيق: "مش قادر يوصل للسيرفر" | السيرفر أو الـ tunnel واقف، أو اللينك اتغير | شغّل الخطوة 4 و6، وابعت اللينك الجديد على ntfy |
| `PKIX path building failed` وإنت بتبني الـ APK | **Avast** بيفتح الاتصالات المشفّرة ويقفلها بشهادته (HTTPS scanning) | Avast ← Web Shield ← اقفل **Enable HTTPS scanning** |
| `start-public.ps1` اختفى | Avast حطه في الـ Quarantine | الخطوة 7 |
| `Port 8080 was already in use` | فيه نسخة تانية من السيرفر شغالة | اقفلها، أو `Get-NetTCPConnection -LocalPort 8080` تعرف مين |
| `password authentication failed` | `DB_PASSWORD` مش متحمّل | راجع `DB_PASSWORD_FILE` في `deploy\.env` |
| `HikariPool … Failed to validate connection` بعد ما الجهاز ينام | الاتصالات القديمة بقاعدة البيانات اتقفلت | عادي، Hikari بيفتح اتصالات جديدة لوحده |
| JUnit: `Could not find a valid Docker environment` | Docker مش شغال | شغّل Docker Desktop، أو شغّل اختبارات الـ Unit بس |
| المتصفح بيعرض نسخة قديمة من التطبيق | الكاش | **Ctrl+Shift+R** |
| `--base-href should start and end with /` | Git Bash بيغيّر `/app/` | `export MSYS_NO_PATHCONV=1` أو استخدم PowerShell |

---

## ١١. الحسابات للتجربة
- **العميل:** اعمل حساب جديد من التطبيق (العملاء بس هما اللي بيسجّلوا لنفسهم).
- **المطعم / المندوب / الأدمن:** الأدمن هو اللي بيعملهم. بياناتهم في `_secrets\VELOX-accounts.txt` عند نبيه، **ومش بتتبعت في الجروب**.
- **قبل أي ديمو:** خلّي حد فاتح حساب **المندوب** وعامل **أونلاين**، وإلا الطلب هيتلغي لوحده بعد شوية (StaleOrderCloser).
