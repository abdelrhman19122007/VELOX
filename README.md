# VELOX — توصيل طلبات سمنود

منصة توصيل محلية لسمنود والقرى اللي حواليها: العميل يطلب من المطاعم والمحلات والصيدليات، ومندوب من البلد يوصّل، والطلب بيتتبع لايف على الخريطة.

**مشروع مبادرة رواد مصر الرقمية (DEPI) — تراك Penetration Testing.**

> **ابدأ من هنا:**
> - ▶️ **شغّل VELOX على جهازك بدبل كليك (لكل التيم):** نزّل Docker Desktop ← دبل كليك على `START-VELOX.bat`. التفاصيل: [docs/TEAM_SETUP.md](docs/TEAM_SETUP.md)
> - 🎤 **يوم المناقشة (العرض قدام الدكتور):** [docs/DEMO_DAY.md](docs/DEMO_DAY.md)
> - 🛠️ **فهم كل خطوة وتشغيلها بإيدك:** [docs/RUN_LOCAL.md](docs/RUN_LOCAL.md)
> - ✅ **نتايج الاختبارات:** [tests/TEST_REPORT.md](tests/TEST_REPORT.md) — والملفات الخام في [tests/results/](tests/results/)
> - 📚 **ملف المذاكرة الكامل:** [docs/study/VELOX-Study-Guide.pdf](docs/study/VELOX-Study-Guide.pdf) — و**الخريطة التفاعلية:** [docs/study/velox-map.html](docs/study/velox-map.html) (نزّلها وافتحها في المتصفح)
> - 🖥️ **تشغيله على سيرفر (VPS / aaPanel):** [docs/deploy-vps/](docs/deploy-vps/)

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
- [البريزنتيشن](docs/presentation/VELOX-Presentation.pptx) · [سكريبت العرض](docs/presentation/PRESENTER_SCRIPT.pdf) · [الدليل الكامل](docs/guide/VELOX-Complete-Guide.pdf)
- [طريقة الشغل في التيم](CONTRIBUTING.md)

## التيم
كل واحد ماسك **موديول Java** في الباك إند، وجزء في العرض، والاختبارات بتاعة الجزء بتاعه:

| العضو | مسؤول عن | في العرض | الاختبارات |
|---|---|---|---|
| نبيه — @nabosallem-svg (Team Lead) | الحسابات والأمان، وقاعدة البيانات، والتشغيل (Docker وCI) | سلايد 10–12 و14–16 + الأمان في الديمو + الأسئلة | JUnit (97)، وJMeter، وفحوصات الأمان في CI |
| هدى — @hodayasser511-stack | المطاعم والمنيو والبحث + التصميم والتوثيق | سلايد 1–3 | يدوي: المطاعم والحسابات + Burp على الكتالوج |
| عبد الرحمن — @abdelrhman19122007 | الطلبات والتتبع + الموقع ولوحة الأدمن | سلايد 8–9 + الأدمن في الديمو | يدوي: الطلبات + Burp على الطلبات |
| فادي — @fadyza15-collab | الدفع والمحفظة والإشعارات + شاشات العميل | سلايد 4–5 + العميل والمندوب في الديمو | يدوي: الدفع + Burp على الدفع |
| رحيق — @Raheek25 | المطعم والمندوب + مسؤولة الاختبارات | سلايد 6–7 و13 + المطعم في الديمو | يدوي: المطعم والمندوب + Postman + تقرير الاختبار |

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
