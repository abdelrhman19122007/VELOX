# VELOX — إزاي نعمل كل الاختبارات بإيدينا

الملف ده بيقول **كل واحد يعمل إيه بالظبط**، وكل نوع اختبار بيتعمل إزاي، ونسجّل النتيجة فين.
قبل أي حاجة: شغّل VELOX على جهازك بـ `START-VELOX.bat` ([docs/TEAM_SETUP.md](../docs/TEAM_SETUP.md)).

| النوع | محتاج إيه | مين |
|---|---|---|
| ١. Manual (يدوي) | المتصفح + ملف Excel | **الكل**، كل واحد الجزء بتاعه |
| ٢. API (Postman) | برنامج Postman | **الكل** يشغّلوه مرة، و**رحيق** تسجّل النتيجة |
| ٣. Security (Burp Suite) | Burp Suite Community | **الكل**، كل واحد هجمتين على الجزء بتاعه |
| ٤. Automated (JUnit) | Java 17 + Docker | **نبيه** أو أي حد عنده Java |
| ٥. UI Automated (Selenium) | Java 17 + Chrome | **رحيق** (أو نبيه) |
| ٦. Performance (JMeter) | Apache JMeter | **نبيه** |
| ٧. Usability | شخص من برّه التيم | **الكل**، كل واحد يجرّب على شخص |

---

## ١) Manual Testing (اليدوي) — الكل

**الملف:** `tests/manual/VELOX-Manual-Tests.xlsx` (47 حالة)

| مين | الحالات | الجزء |
|---|---|---|
| هدى | M-CAT-01 → 09 + M-ACC-01 → 10 | المطاعم والمنيو + الحسابات |
| عبد الرحمن | M-ORD-01 → 10 | الطلبات والدفع ولوحة الأدمن |
| فادي | M-PAY-01 → 08 | المحفظة والإشعارات والتقييم |
| رحيق | M-OPS-01 → 10 | المطعم والمندوب |

**الخطوات:**
1. افتح الملف ← تاب **Test Cases** ← اعمل Filter على عمود **Owner** باسمك.
2. نفّذ كل حالة بالخطوات المكتوبة بالظبط.
3. اكتب اللي حصل في **Actual result**، واختار **Status**: `Pass` لو زي المتوقع، و`Fail` لو مختلف، و`Blocked` لو مقدرتش تنفّذ.
4. **سكرينشوت لكل حالة:** اضغط `Win + Shift + S` واحفظه في `tests/manual/screenshots/` باسم الحالة، زي `M-OPS-01.png`.
5. لو حاجة **Fail**: سجّلها في تاب **Bug Reports** (BUG-01، BUG-02...) بالخطوات والمتوقع والفعلي والخطورة، واكتب رقمها في عمود Bug ID.
6. تاب **Summary** بيحسب النسب لوحده.
7. ارفع الملف والسكرينشوتات على GitHub من حسابك (شوف الجزء ٨ تحت).

> **نصيحة:** افتح 3 متصفحات (Chrome عميل، Chrome Incognito مطعم، Edge مندوب) عشان تجرّب الدورة كاملة.

---

## ٢) API Testing (Postman) — الكل

**الملف:** `tests/api/velox.postman_collection.json` (30 طلب، 42 تأكيد). فيه الطلبات العادية وكمان **هجمات**: IDOR، والتلاعب بالسعر، وXSS، وتوكن مزوّر، وغيرهم.

1. نزّل **Postman** من `postman.com/downloads` وسطّبه (مش لازم تعمل حساب، اختار "Lightweight API Client").
2. **Import** ← اختار `tests/api/velox.postman_collection.json`.
3. دوس على الـ Collection ← تاب **Variables** ← اتأكد إن `baseUrl` = `http://localhost:8080` (أو البورت اللي START-VELOX كتبه).
4. دوس **Run** (أو `...` ← Run collection) ← **Run VELOX**.
5. لازم تطلع **30 طلب أخضر**. خد **سكرينشوت** للنتيجة.
6. افتح أي طلب وبص على تاب **Tests**، هتلاقي التأكيدات مكتوبة. **افهم 3 منهم في الجزء بتاعك** عشان الدكتور ممكن يسأل.

**رحيق:** صدّر النتيجة: في نافذة النتايج ← **Export Results** ← احفظها في `tests/results/postman-<التاريخ>.json`.

**وكمان Swagger:** افتح `http://localhost:8080/swagger-ui.html` ← اختار أي عنوان ← **Try it out** ← **Execute**. جرّب عنوان من الجزء بتاعك وخد سكرينشوت.

---

## ٣) Security Testing (Burp Suite) — الكل

**التقارير:** `security/catalog.md` (هدى)، و`security/orders.md` (عبد الرحمن)، و`security/payments.md` (فادي)، و`security/operations.md` (رحيق)، والأمان العام (نبيه).
كل تقرير فيه **جدول Test cases** بالهجمات المطلوبة.

1. افتح **Burp Suite** (متسطّب عند نبيه، والباقي ينزّلوا Community من `portswigger.net`).
2. **Proxy ← Open browser** (متصفح Burp)، وافتح فيه `http://localhost:8080/app/` وادخل.
3. اعمل الحاجة اللي عايز تهاجمها (مثلاً تطلب) ← **Proxy ← HTTP history** ← كليك يمين على الطلب ← **Send to Repeater**.
4. في **Repeater** غيّر حاجة وابعت، مثلاً:
   - **IDOR:** غيّر رقم الطلب في الرابط لطلب حد تاني ← المتوقع **403/404**.
   - **Price tampering:** زوّد `"price": 1` في الطلب ← المتوقع السيرفر يتجاهله.
   - **Missing auth:** امسح سطر `Authorization` ← المتوقع **401**.
   - **Wrong role:** ابعت طلب مطعم (`/api/restaurant/...`) بتوكن عميل ← المتوقع **403**.
   - **XSS:** اكتب `<script>alert(1)</script>` في العنوان أو التعليق ← المتوقع **مرفوض أو بيظهر كنص**.
5. **سكرينشوت** للطلب والرد.
6. اكتب في التقرير بتاعك جوه جدول **Findings**: لو الهجمة **منفعتش** اكتب "Not vulnerable" والدليل، ولو **نفعت** اكتبها ثغرة بالخطورة وقولّنا فوراً.
7. اكتب **Conclusion** في 3 سطور: جرّبت إيه، ولقيت إيه، وإيه المخاطر الباقية.

---

## ٤) Automated — JUnit (97 اختبار) — نبيه

محتاج **Java 17** و**Docker شغال** (الاختبارات بتشغّل PostgreSQL حقيقي جوه Docker).
```powershell
cd backend
.\mvnw.cmd test
```
- لازم في الآخر: `Tests run: 97, Failures: 0, Errors: 0` و`BUILD SUCCESS`.
- النتايج التفصيلية بتتحفظ في `backend\target\surefire-reports\`.
- **نسبة التغطية:** `.\mvnw.cmd verify` ← افتح `backend\target\site\jacoco\index.html`.
- **في NetBeans:** File ← Open Project ← `backend` ← كليك يمين ← **Test**.
- **آخر نتيجة:** 19 نوفمبر **97/97** (`tests/results/2026-11-19-junit-all.txt`).

**الفحص الأمني للكود (SpotBugs + FindSecBugs):**
```powershell
.\mvnw.cmd -DskipTests compile spotbugs:check
```

---

## ٥) UI Automated — Selenium — رحيق

محتاج **Java 17** و**Google Chrome**، وVELOX شغال.
```powershell
cd backend
.\mvnw.cmd -f ..\tests\e2e\pom.xml test "-Dvelox.url=http://localhost:8080"
```
بيفتح Chrome لوحده ويختبر الموقع ولوحة الأدمن (4 اختبارات). خد سكرينشوت للنتيجة.

---

## ٦) Performance — Apache JMeter — نبيه

1. نزّل **Apache JMeter** من `jmeter.apache.org` (Binaries ← zip) وفكّه.
2. افتح `bin\jmeter.bat` ← **File ← Open** ← `tests\performance\velox-browse.jmx`.
3. دوس ▶ Start. الخطة: **100 مستخدم × 10 لفّات** على المناطق والمطاعم والمنيو والبحث.
4. شوف **Summary Report**: المطلوب **95% من الطلبات أسرع من 500ms ومن غير أخطاء**.
5. أو من غير شاشة:
```powershell
jmeter -n -t tests\performance\velox-browse.jmx -Jhost=localhost -Jport=8080 -l results.jtl -e -o report
```
وافتح `report\index.html`.
> جرّبوها على النسخة المحلية (الداتابيز على نفس الجهاز). كده هنعرف إذا كان بطء D-10 جاي من الداتابيز البعيدة.

---

## ٧) Usability — الكل

- التاب **Usability** في نفس ملف الـ Excel.
- كل واحد يجيب **شخص واحد مش بيعرف التطبيق** (من أهله أو صحابه) ويديله الـ 5 مهام **من غير ما يساعده**.
- يسجّل: خلّص ولا لأ، وفي كام ثانية، وكام غلطة، وسهولة من 1 لـ 5، وتعليقه.

---

## ٨) ترفع النتايج على GitHub (من حسابك)

1. **GitHub Desktop** ← File ← Clone ← `abdelrhman19122007/VELOX`.
2. **Current branch ← New branch** ← سمّيه باسمك، زي `raheek/tests`.
3. حط ملفاتك (Excel، والسكرينشوتات، والتقرير الأمني بتاعك) في أماكنها.
4. اكتب رسالة واضحة، زي `tests: manual results for operations (10 cases)` ← **Commit** ← **Push origin**.
5. **Create Pull Request** ← نبيه يراجع ويدمج.

---

## ٩) الجدول (اقتراح)

| اليوم | المطلوب |
|---|---|
| **اليوم ١** | كل واحد يشغّل VELOX ويعمل الـ Manual بتاعه + Postman |
| **اليوم ٢** | Burp (هجمتين لكل واحد) + يكتب التقرير الأمني |
| **اليوم ٣** | Usability + نبيه يشغّل JUnit وJMeter + رحيق Selenium |
| **اليوم ٤** | رحيق تجمع كل النتايج في `tests/TEST_REPORT.md` ← بروفة العرض |
