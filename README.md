# VELOX — توصيل طلبات سمنود

منصة توصيل محلية لسمنود والقرى اللي حواليها: العميل يطلب من المطاعم والمحلات والصيدليات، ومندوب من البلد يوصّل، والطلب بيتتبع لايف على الخريطة.

**مشروع مبادرة رواد مصر الرقمية (DEPI) — تراك Penetration Testing.**

| | |
|---|---|
| Backend | Java 17 · Spring Boot 3 · PostgreSQL 16 · Flyway · OpenAPI (Swagger) |
| Mobile + app on the web | Flutter (Android APK / Google Play، ونفس التطبيق على `/app`) |
| Website + dashboard | HTML / CSS / JavaScript (`/` و`/admin`) |
| Security | OWASP Top 10 · CodeQL · SpotBugs + FindSecBugs · Trivy · OWASP ZAP · Burp Suite |
| Testing | JUnit 5 + Testcontainers · Newman · Selenium · Flutter test · JMeter |

## هيكل الريبو
```
backend/      الـ API (Java) — Clean Architecture: controller → service → repository → domain
mobile/       تطبيق Flutter: عميل، مطعم، مندوب، أدمن (نفس الكود للأندرويد والمتصفح)
web/          الموقع ولوحة تحكم الأدمن والمطعم
database/     migrations — ملف لكل موديول
design/       نظام التصميم (Nile Terracotta)، تحليل المنافسين، اختبار سهولة الاستخدام
docs/         SRS · ERD · التقرير النهائي
security/     خطط وتقارير اختبار الاختراق لكل موديول
tests/        Newman · Selenium · JMeter · خطة وتقرير الاختبار
deploy/       تشغيل المشروع على الإنترنت من جهاز (Cloudflare tunnel)
legacy/       النسخة الأولى (tag: v1.0-submitted)
```

## المستندات
- [SRS — المتطلبات](docs/SRS.md) · [التقرير النهائي](docs/report/FINAL_REPORT.md)
- [قاعدة البيانات](docs/database/ERD.md) · [نظام التصميم](design/DESIGN_SYSTEM.md) · [تحليل المنافسين](design/COMPETITOR_ANALYSIS.md)
- [خطة الاختبار](tests/TEST_PLAN.md) · [تقرير الاختبار](tests/TEST_REPORT.md) · [اختبار الاختراق](security/)
- [طريقة الشغل في التيم](CONTRIBUTING.md)

## التيم
كل واحد ماسك **موديول Java** في الباك إند + **تخصص**:

| العضو | الموديول (Java) | التخصص |
|---|---|---|
| نبيه — @nabosallem-svg (Team Lead) | الحسابات والأمان + الأساس | المعمارية، الربط، DevOps وفحوصات الأمان، أساس تطبيق Flutter، الأداء، التقرير |
| هدى — @hodayasser511-stack | المحلات والمنيو والبحث | UI/UX: التصميم، اختبار سهولة الاستخدام |
| عبد الرحمن — @abdelrhman19122007 | الطلبات والتوصيل | الويب: الموقع ولوحة التحكم |
| فادي — @fadyza15-collab | الدفع والمحفظة والإشعارات | الموبايل: شاشات Flutter، Google Play |
| رحيق — @Raheek25 | المطعم والمندوب والأدمن | الاختبار والأمان: خطة وتقرير الاختبار، Newman، Selenium |

## التشغيل على الجهاز
محتاج Java 17 وDocker (وFlutter للتطبيق).

```bash
docker compose up -d db                   # PostgreSQL  (لو عندك PostgreSQL على 5432: VELOX_DB_PORT=5434)
cd backend
./mvnw spring-boot:run                    # http://localhost:8080  (API + الموقع + /admin + /app + /swagger-ui.html)
./mvnw verify                             # كل الاختبارات + تقرير التغطية
```

أول أدمن بيتعمل من الـ environment (مفيش باسورد جوه الكود):
```bash
VELOX_ADMIN_EMAIL=admin@example.com VELOX_ADMIN_PASSWORD='at-least-12-chars' VELOX_TOKEN_SECRET='32+ random chars' ./mvnw spring-boot:run
```

التطبيق:
```bash
cd mobile
flutter run                               # على موبايل أو محاكي
flutter build web --base-href /app/       # نسخة المتصفح اللي السيرفر بيقدّمها على /app
flutter build apk --release
```

## على الإنترنت من جهاز واحد
`deploy/start-public.ps1` بيشغّل قاعدة البيانات والسيرفر ويفتح رابط Cloudflare مجاني، والتطبيق بيلاقي الرابط الجديد لوحده.
أول مرة بيسأل على إيميل وباسورد الأدمن وبيحفظهم في `deploy/.env` (مش بيترفع على GitHub).
