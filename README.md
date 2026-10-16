# VELOX — توصيل طلبات سمنود

منصة توصيل محلية لسمنود والقرى اللي حواليها: العميل يطلب من المطاعم والمحلات والصيدليات، ومندوب من البلد يوصّل، والطلب بيتتبع لايف على الخريطة.

**مشروع مبادرة رواد مصر الرقمية (DEPI) — تراك Penetration Testing.**

| | |
|---|---|
| Backend | Java 17 · Spring Boot 3 · PostgreSQL 16 · Flyway |
| Web | HTML / CSS / JavaScript |
| Mobile | Flutter (Android — Google Play) |
| Security | OWASP Top 10 · ZAP · Burp Suite · Dependency-Check · SpotBugs |
| Testing | JUnit 5 · Mockito · Newman · Selenium · JMeter |

## هيكل الريبو
```
backend/      الـ API (Java) — Clean Architecture: controller → service → repository → domain
web/          الموقع
mobile/       تطبيق الموبايل (Flutter)
database/     migrations — ملف لكل موديول
docs/         SRS · ERD · التقرير · UML
security/     تقارير اختبار الاختراق لكل موديول
tests/        Postman · Selenium · JMeter · ZAP
legacy/       الكود اللي اتسلم قبل كده (tag: v1.0-submitted) وتوثيقه
```

## المستندات
- [SRS — المتطلبات](docs/SRS.md)
- [قاعدة البيانات (ERD + القواعد)](docs/database/ERD.md)
- [طريقة الشغل في التيم](CONTRIBUTING.md)
- [توثيق النسخة الأولى](legacy/README.md)

## الموديولات وأصحابها
| الموديول | صاحبه | الفرع |
|---|---|---|
| الحسابات والأمان + تجميع المشروع | نبيه — @nabosallem-svg (Team Lead) | `feature/auth` |
| المحلات والمنتجات والبحث | Hoda (@hodayasser511-stack) | `feature/catalog` |
| السلة والطلبات والتوصيل | Abdelrhman (@abdelrhman19122007) | `feature/orders` |
| الدفع والمحفظة والإشعارات | Fady (@fadyza15-collab) | `feature/payments` |
| لوحات المطعم والمندوب والأدمن | Raheek (@Raheek25) | `feature/operations` |

## التشغيل
محتاج Java 17 وDocker.

```bash
docker compose up -d                      # PostgreSQL
cd backend
./mvnw spring-boot:run                    # الـ API على http://localhost:8080
./mvnw verify                             # كل الاختبارات (بتشغّل PostgreSQL مؤقت لوحدها) + تقرير التغطية
```

أول أدمن بيتعمل من الـ environment (مفيش باسورد جوه الكود):
```bash
VELOX_ADMIN_EMAIL=admin@example.com VELOX_ADMIN_PASSWORD='at-least-12-chars' VELOX_TOKEN_SECRET='32+ random chars' ./mvnw spring-boot:run
```
