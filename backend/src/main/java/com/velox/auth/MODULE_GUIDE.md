# موديول الحسابات والأمان (auth + security) — دليل الشرح

**صاحبه:** نبيه (@nabosallem-svg) — Team Lead

## الموديول بيعمل إيه
- تسجيل حساب جديد (عميل أو مندوب بس — المطعم والأدمن الأدمن هو اللي يعملهم).
- تسجيل الدخول، وتوكن صالح 30 يوم.
- تغيير كلمة السر، و"نسيت كلمة السر" عن طريق الأدمن.
- العناوين المحفوظة (بالعلامة المميزة "جنب الجامع").
- الأدمن: عرض الحسابات، حظر/إلغاء حظر، كلمة سر مؤقتة.
- الأمان لكل المشروع: التوكن، تشفير كلمات السر، حماية من تخمين كلمة السر، سجل العمليات الحساسة، الـ headers.

## الفايلات (Clean Architecture)
```
auth/
├── controller/   AuthController, AccountController, AdminUserController   ← يستقبل الطلب ويرد بس
├── service/      AuthService, AccountService, UserAdminService           ← القواعد كلها هنا
├── repository/   UserRepository, AddressRepository                      ← الـ SQL كله هنا
├── domain/       User, Address                                          ← شكل البيانات
└── dto/          AuthDtos                                               ← اللي بيدخل ويطلع من الـ API
security/         TokenService, PasswordHasher, LoginThrottle, AuditLog, CurrentUserResolver …
common/error/     ApiException, GlobalExceptionHandler                   ← شكل موحد للأخطاء
config/           WebConfig (CORS), SecurityHeadersFilter, AdminBootstrap
```

## أسئلة المناقشة المتوقعة
**كلمة السر بتتخزن إزاي؟** PBKDF2-SHA256 بـ 210,000 دورة وملح (salt) عشوائي لكل كلمة سر. حتى لو حد سرق قاعدة البيانات مش هيعرف كلمات السر، ونفس كلمة السر بتطلع hash مختلف كل مرة.

**التوكن شغال إزاي؟** `id.version.expiry.signature` متوقّع بـ HMAC-SHA256 بمفتاح سري من الـ environment. لو حد غيّر أي حرف التوقيع مش هيطابق. الـ version لو زاد (تغيير كلمة السر، حظر، خروج من كل الأجهزة) كل التوكنات القديمة بتبطل فوراً.

**إزاي بتمنع تخمين كلمة السر؟** كل محاولة بتتسجل في `login_attempts`. 5 محاولات غلط لنفس الإيميل في 10 دقايق = الإيميل يتقفل 10 دقايق (حتى بالكلمة الصح). و30 من نفس الشبكة = الشبكة تتقفل.

**ليه رسالة "إيميل غلط" زي "كلمة سر غلط"؟** عشان المهاجم ميعرفش الإيميل ده عليه حساب ولا لأ (account enumeration).

**إزاي بتمنع حد يشوف بيانات حد تاني (IDOR)؟** الـ id اللي بيطلع للعميل عشوائي (`Uxxxxx`) مش رقم يتخمن، وكل SQL بيفلتر بـ `user_id` بتاع صاحب التوكن.

**SQL Injection؟** كل الـ SQL بـ parameters (`?`) — مفيش ولا query بيتجمع من كلام المستخدم. فيه اختبار بيبعت `' OR '1'='1` وبيتأكد إنه بيرجع "دخول غلط" عادي.

## الاختبارات (23 — كلها شغالة، تغطية 89%)
| ملف الاختبار | بيختبر إيه |
|---|---|
| `TokenServiceTest` | توكن متزوّر / متعدّل / من مفتاح تاني / منتهي → مرفوض |
| `PasswordHasherTest` | الملح العشوائي، مفيش كلمة سر واضحة، الكلمة المؤقتة |
| `AuthApiTest` | التسجيل، الدخول، تكرار الإيميل، تسجيل كأدمن (مرفوض)، brute force، SQL injection، الـ headers |
| `AccountApiTest` | تغيير كلمة السر يقفل الجلسات القديمة، العناوين بالعربي، IDOR على العناوين |
| `AdminUserApiTest` | عميل يحاول يستخدم صلاحيات أدمن (403)، الحظر، كلمة السر المؤقتة، سجل العمليات |

الاختبارات بتشتغل على **PostgreSQL حقيقي** (Testcontainers) بنفس الـ migrations بتاعة الإنتاج: `cd backend && ./mvnw verify`
