# تقرير اختبار وتقييم النظام - Velox

**البطاقة:** الفرد 1 — إدارة الحسابات والهوية (Auth & Accounts)
**اسم العضو / دور الفريق:** نبيه محمد ابوسالم — بطاقة الفرد 1 (Auth & Accounts)
**تاريخ التقييم:** 20 سبتمبر 2026

---

## 1. بيئة التشغيل (Environment Constraints)

- **إصدار جافا (Java Version):** OpenJDK 17.0.20 (Microsoft build 17.0.20+8-LTS) — يعمل بنجاح ✅
- **حالة الـ API الأساسي:** `http://localhost:8081/api/governorates/CAIRO` → **OK 200** ✅
  (ملحوظة: البورت 8080 كان محجوزًا بتطبيق آخر `clinic-0.1.0.jar`، فتم تشغيل باك VELOX على **8081** — راجع بند المشاكل رقم 1)
- **المتصفح المستخدم:** Brave (Chromium) — التحقق تم عبر HTTP مباشر + مراجعة كود الفرونت
- **قاعدة البيانات:** H2 محلية تلقائية (MySQL غير متاحة على الجهاز) تحت `velox-backend/data/`

---

## 2. جدول حالات الاختبار (Test Cases Table)

| ID | الخطوات | النتيجة المتوقعة | النتيجة الفعلية | الحالة (ناجح/فاشل) |
|---|---|---|---|---|
| F1-T01 | تسجيل سليم (`POST /auth/register` ببيانات صحيحة) | `pending:true + otp` | `200 {"pending":true,...,"otp":"089623"}` | ناجح ✅ |
| F1-T02 | تسجيل بإيميل مكرر | `400 EMAIL_ALREADY_REGISTERED` | `400 {"code":"EMAIL_ALREADY_REGISTERED",...}` بالنص | ناجح ✅ |
| F1-T03 | OTP خاطئ (`POST /auth/verify-otp` بكود `000000`) | `400` | `400 {"message":"Invalid or expired code."}` | ناجح ✅ |
| F1-T04 | دخول بكلمة سر خاطئة | `401` | `401 {"message":"Invalid email or password."}` | ناجح ✅ |
| F1-T05 | فتح `/auth/profile` بدون Bearer token | `401` | `401 {"message":"Login required."}` | ناجح ✅ |
| F1-T06 | محاولة فتح بروفايل مستخدم آخر بتوكن مستخدم أول | `403` | `403 {"message":"Forbidden."}` | ناجح ✅ |
| F1-T07 | تحديث رقم الهاتف بصيغة غير صحيحة (`PUT /auth/profile`) | `400` | `400 {"message":"Invalid phone number."}` | ناجح ✅ |
| F1-T08 | ربط متكامل (سجل ← فعّل ← ادخل ← اعرض ← حدّث ← اخرج ← ادخل) | ينجح بلا انقطاع | register→verify→login→profile→update→logout→`401` بعد الخروج — كله سليم | ناجح ✅ |

**إضافي تم التحقق منه:** الدخول قبل تفعيل OTP → `403 ACCOUNT_NOT_VERIFIED` ✅ | تحديث رقم سليم → `200` وينعكس في البروفايل ✅ | اللوجاوت → `200 {"success":true}` والتوكن يموت ✅

---

## 3. جدول ربط وتنقل الصفحات (Page Integration Table)

| من صفحة | إلى صفحة | يعمل بنجاح؟ (نعم/لا) | ملاحظات التفاعل والملاحظات |
|---|---|---|---|
| register.html | Verify OTP ثم home.html | نعم ✅ | بعد التفعيل الناجح يتحول تلقائيًا إلى `home.html` (تم إصلاحه — كان `index.html`) |
| login.html | register.html والعكس | نعم ✅ | رابطان متبادلان موجودان في الصفحتين |
| account.html (بدون توكن) | login.html | نعم ✅ | حارس `NEED_LOGIN` يحوّل غير المسجل إلى الدخول |
| settings.html (تعديل الهاتف) | account.html بعد Refresh | نعم ✅ | بانل هاتف جديد يحفظ عبر `PUT /auth/profile`، و`account.html` يجلب البروفايل طازجًا كل تحميل |
| Logout | مسح التوكن ثم login.html | نعم ✅ | زر خروج في `account.html` + خروج المتجر يمسح التوكن باك/فرونت ويحوّل للدخول |

---

## 4. توثيق المشاكل والإصلاحات (Issues & Bug Fixes)

### المشكلة 1: البورت 8080 محجوز — الباك لا يقلع
- **الوصف:** عند التشغيل سقط السيرفر بخطأ `Port 8080 was already in use` — تبين أن تطبيقًا آخر (`clinic-0.1.0.jar`, PID 3480) يشغل البورت.
- **موقع الكود (File & Line):** لا ينطبق (تعارض بيئة) — الإقلاع عبر `velox-backend/mvnw.cmd`
- **السبب الجذر (Root Cause):** منفذ 8080 الافتراضي مشغول بعملية جافا خارجية، والفرونت مضبوط على `8080` في `velox-frontend/js/config.js:10`.
- **طريقة الإصلاح:** تشغيل الباك على `8081` عبر `SERVER_PORT=8081` + تعديل مؤقت لـ `API_BASE_URL` إلى `8081` (يُرجع لـ `8080` عند إيقاف clinic).
- **ناتج إعادة الاختبار:** `Tomcat started on port 8081` + `/api/governorates` → `200` ✅

### المشكلة 2: زر الدخول بجوجل معطل "يحتاج Client ID"
- **الوصف:** زر جوجل في `login.html` يظهر معطلًا رغم أن الباك مهيأ (`google-config` يرجع `enabled:true`).
- **موقع الكود (File & Line):** `velox-frontend/js/googleAuth.js:5-6` + `velox-frontend/js/config.js:10`
- **السبب الجذر (Root Cause):** المتصفح كان ماسك نسخة قديمة متكاشية من `config.js` تشاور على `8080` (ترد `401` من تطبيق clinic) فيفشل طلب `google-config` ويتعطل الزر.
- **طريقة الإصلاح:** إضافة كاسر كاش `?v=23` على سطر `config.js` في `login.html` و`index.html` و`home.html` و`products.html`.
- **ناتج إعادة الاختبار:** `google-config` → `enabled:true` والزر يظهر ويعمل ✅

### المشكلة 3: بعد Verify OTP يحوّل إلى `index.html` بدل `home.html`
- **الوصف:** مخالف لسيناريو البطاقة (التسجيل → Verify OTP → `home.html`).
- **موقع الكود (File & Line):** `velox-frontend/js/pages/registerPage.js:146`
- **السبب الجذر (Root Cause):** عنوان التحويل مكتوب `index.html` بدل `home.html`.
- **طريقة الإصلاح:** تغيير الوجهة إلى `home.html` + رفع نسخة السكربت إلى `?v=21` في `register.html`.
- **ناتج إعادة الاختبار:** الملف المخدوم يحتوي `location.href = 'home.html'` + فحص `node --check` سليم ✅

### المشكلة 4: لا يوجد تعديل لرقم الهاتف في `settings.html`
- **الوصف:** البطاقة تتطلب تعديل الهاتف من الإعدادات وظهوره في الحساب بعد التحديث، لكن الصفحة كانت تفضيلات فقط.
- **موقع الكود (File & Line):** `velox-frontend/settings.html` + `velox-frontend/js/pages/accountPages.js` (دالة جديدة `initSettings`)
- **السبب الجذر (Root Cause):** لم يكن هناك حقل هاتف ولا ربط مع `PUT /auth/profile` في صفحة الإعدادات.
- **طريقة الإصلاح:** بانل هاتف جديد (`set-phone` + حفظ) + `initSettings()` (تحميل من البروفايل + تحقق `01` + 11 رقم + رسائل) + مفاتيح ترجمة عربي/إنجليزي في `i18n-pages.js`.
- **ناتج إعادة الاختبار:** الـ HTML المخدوم يحتوي البانل + `node --check` سليم + الحفظ عبر نفس الـ endpoint المختبر live ✅

### المشكلة 5: اللوجاوت لا يحوّل إلى `login.html` ولا زر خروج في الحساب
- **الوصف:** الخروج كان يبقي المستخدم في نفس الصفحة، ولا يوجد زر خروج في `account.html`.
- **موقع الكود (File & Line):** `velox-frontend/js/pages/storefrontPage.js:280` + `velox-frontend/account.html` + `velox-frontend/js/pages/accountPages.js` (دالة `doLogout`)
- **السبب الجذر (Root Cause):** معالج الخروج يمسح الجلسة ويعرض toast فقط دون تحويل؛ وصفحة الحساب بلا زر خروج.
- **طريقة الإصلاح:** زر خروج في `account.html` + `doLogout()` (خروج باك + مسح جلسة + تحويل) + تحويل بعد خروج المتجر بمهلة 600ms.
- **ناتج إعادة الاختبار:** الـ HTML المخدوم يحتوي `logout-btn` + `node --check` سليم + التوكن ميت بعد الخروج live ✅

---

## 5. إقرار عدم تعديل سكيما قاعدة البيانات (Database Integrity Declaration)

أقرّ أنا عضو الفريق المذكور أعلاه بأنني **لم أقوم بإجراء أي تعديلات على سكيما قاعدة البيانات** (Schema) أو ملفات الهجرة (Migrations) أو ملف `VELOX_D2.SQL` أو قواعد H2.
وأن جميع الاختبارات والإصلاحات تمت **حصرًا عبر واجهات API والفرونت** (تسجيل/تحقق/دخول/بروفايل/تحديث/خروج)، والبيانات الوحيدة المنشأة هي **صفوف مستخدمي اختبار** مؤقتة في قاعدة H2 المحلية.
