# أهلًا يا عبد الرحمن — تشغيل VELOX على جهازك

## محتاج تسطب
- **Java 17** (Microsoft OpenJDK أو Temurin)
- **Docker Desktop** (لقاعدة البيانات)
- **Flutter** (للتطبيق) — `flutter doctor` لازم يطلع أخضر للأندرويد والويب
- Git و VS Code (اختياري)

## التشغيل (3 خطوات)
```bash
# 1) قاعدة البيانات (PostgreSQL على بورت 5432)
docker compose up -d db

# 2) السيرفر — من فولدر backend
cd backend
# لازم تحدد أدمن ومفتاح توكن بتوعك (مفيش كلمات سر جوه الكود)
set VELOX_ADMIN_EMAIL=you@example.com
set VELOX_ADMIN_PASSWORD=اكتب-باسورد-طويل-12-حرف-على-الأقل
set VELOX_TOKEN_SECRET=اكتب-أي-نص-عشوائي-طويل
mvnw.cmd spring-boot:run
# بعدها افتح: http://localhost:8080  (الموقع) و http://localhost:8080/admin/ (لوحة التحكم)

# 3) التطبيق — من فولدر mobile
cd mobile
flutter pub get
flutter run -d chrome        # أو على موبايل أندرويد متوصل
```
في التطبيق: Settings → Server link → اكتب `http://<IP جهازك>:8080` لو بتجرب من موبايل.

## خريطة المشروع
| الفولدر | فيه إيه |
|---|---|
| `backend/` | السيرفر Java (Spring Boot) — 5 موديولات: core/auth، catalog، **orders (بتاعك)**، payment، operation |
| `web/` | **الموقع ولوحة التحكم (بتاعك)** — HTML/CSS/JS |
| `mobile/` | تطبيق Flutter (عميل، مطعم، مندوب، أدمن) |
| `database/migrations/` | جداول قاعدة البيانات (Flyway) |
| `tests/` | اختبارات API و Selenium والضغط |
| `security/` | خطة اختبار الاختراق لكل موديول |
| `design/` | نظام التصميم وتحليل المنافسين |
| `docs/` | التقرير النهائي و SRS |

## الاختبارات
```bash
cd backend
mvnw.cmd verify     # 94 اختبار + SpotBugs (محتاج Docker شغال)
```

## مهم
- **متحطش أي باسورد أو مفتاح في الكود** ولا ترفعه على GitHub. الحاجات السرية بتتحط كـ environment variables بس.
- البيانات الحقيقية (المطاعم والمنيو) على سيرفر نبيه، مش في الملف ده. عندك قاعدة بيانات فاضية للتجربة.
- الـ Swagger (كل الـ API): `http://localhost:8080/swagger-ui.html`
