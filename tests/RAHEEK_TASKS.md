# مهام رحيق قبل المناقشة (مسؤولة الاختبارات)

الشغل ده **بتاعك وباسمك**: إنتي اللي هتنفّذيه وترفعيه من حسابك، عشان يبان على GitHub، ولأن الدكتور هيسألك فيه.
كل خطوة مكتوب جنبها الوقت المتوقع. **المجموع حوالي 5 ساعات على 3 أيام.**

---

## ٠) شغّلي المشروع (أول مرة 15 دقيقة)
1. نزّلي **Docker Desktop** وسطّبيه، وافتحيه واستني لحد ما يكتب **Engine running**.
2. من `github.com/abdelrhman19122007/VELOX` ← **Code** ← **Download ZIP** ← فكّي الضغط في `C:\VELOX`.
3. دبل كليك على **`START-VELOX.bat`**. في الآخر هيكتبلك حسابات المطعم والمندوب والعميل.
4. افتحي 3 متصفحات: **Chrome** (عميل)، و**Chrome Incognito** (مطعم)، و**Edge** (مندوب)، وجرّبي طلب كامل.

---

## ١) Manual Testing — اليوم الأول (حوالي ساعة ونص)
**الملف:** `tests/manual/VELOX-Manual-Tests.xlsx` ← تاب **Test Cases** ← Filter على **Owner = Raheek**.

| ID | الحالة |
|---|---|
| M-OPS-01 | الطلب الجديد بيرن عند المطعم من غير Refresh |
| M-OPS-02 | القبول ← "بتتجهز" |
| M-OPS-03 | الرفض بسبب |
| M-OPS-04 | جاهز |
| M-OPS-05 | المندوب أونلاين/أوفلاين |
| M-OPS-06 | كارت الطلب عند المندوب (الأصناف والعنوان والكاش) |
| M-OPS-07 | المندوب ياخد الطلب ← المطعم يشوف اسمه |
| M-OPS-08 | مينفعش يستلم قبل "جاهز" |
| M-OPS-09 | التسليم والأرباح |
| M-OPS-10 | الإلغاء الأوتوماتيك بعد 15 دقيقة (سيبي طلب واشتغلي في حاجة تانية) |

لكل حالة:
- اكتبي اللي حصل في **Actual result**، واختاري **Pass / Fail / Blocked**.
- اكتبي اسمك والتاريخ.
- خدي **سكرينشوت** (`Win + Shift + S`) واحفظيه في `tests/manual/screenshots/M-OPS-01.png` وهكذا.
- لو حاجة **Fail**: سجّليها في تاب **Bug Reports** (BUG-01...).

---

## ٢) API Testing بـ Postman — اليوم الأول (نص ساعة)
1. نزّلي **Postman** (اختاري "Lightweight API Client"، من غير حساب).
2. **Import** ← `tests/api/velox.postman_collection.json`.
3. تاب **Variables** ← اتأكدي إن `baseUrl` = `http://localhost:8080`.
4. **Run collection** ← لازم يطلع **30 أخضر**.
5. **Export Results** ← احفظيه في `tests/results/2026-11-XX-postman.json` (حطي التاريخ)، وخدي سكرينشوت في `tests/results/postman.png`.

---

## ٣) Security Testing بـ Burp Suite — اليوم التاني (حوالي ساعتين)
**التقرير بتاعك:** `security/operations.md`. فيه 18 هجمة، **اعملي الـ 5 دول على الأقل**:

| # | الهجمة | تعملي إيه في Burp | المتوقع |
|---|---|---|---|
| **OP-01** | عميل بيستخدم صلاحيات المطعم | ادخلي كعميل ← أي طلب في HTTP history ← **Send to Repeater** ← غيّري الرابط لـ `GET /api/restaurant/orders` ← Send | **403** |
| **OP-03** | مطعم بيقبل طلب مطعم تاني | طلب "قبول" ← Repeater ← غيّري رقم الطلب (ORD-xxxx) لرقم طلب من مطعم تاني (لو مفيش، أي رقم مش بتاعه) ← Send | **404** |
| **OP-06** | مندوبين ياخدوا نفس الطلب في نفس اللحظة | محتاجة حسابين مندوب (اطلبي من نبيه يعمل تاني من الأدمن). طلبين `take` بتوكنين مختلفين ← في Repeater حطيهم في **Group** ← **Send group in parallel** | **واحد بس ينجح** |
| **OP-08** | تخطي خطوات | المندوب ياخد طلب لسه "بتتجهز" ← ابعتي `POST /api/driver/orders/{code}/pickup` | **مرفوض (BAD_STATUS)** |
| **OP-13** | XSS في سبب الرفض أو التقييم | رفض طلب ← Repeater ← خلّي `"reason": "<script>alert(1)</script>"` ← Send | **400** |

لكل هجمة:
- **سكرينشوت** للطلب والرد في `security/evidence/operations/OP-01.png` وهكذا.
- في جدول التقرير، اكتبي في عمود **Result**: `pass` (الهجمة منفعتش، يعني السيستم سليم) أو `FAIL` (لقيتي ثغرة: **قولي لنبيه فوراً**).
- غيّري **Date** في أول التقرير، و**Target** لـ `main`.
- في **Findings**: لو مفيش ثغرات اكتبي "No vulnerabilities found in the tested cases (OP-01, 03, 06, 08, 13)"، ولو فيه اكتبيها بالتفصيل.
- اكتبي **Conclusion** في 3 سطور: جرّبتي إيه، ولقيتي إيه، وإيه اللي متجرّبش.

> **Burp أول مرة:** Proxy ← **Open browser** (متصفح Burp جاهز)، وافتحي فيه `http://localhost:8080/app/`. كل طلب هتلاقيه في **Proxy ← HTTP history**.

---

## ٤) تجميع النتايج — اليوم التالت (ساعة)
إنتي **مسؤولة تقرير الاختبارات**: `tests/TEST_REPORT.md`.
1. اسألي كل واحد في التيم عن نتيجة الـ Manual بتاعه (تاب **Summary** في ملف الإكسيل بيحسبها).
2. ضيفي جزء جديد فوق في التقرير `## Re-run — <التاريخ> (team)` فيه جدول:

| Suite | Result | Evidence |
|---|---|---|
| Manual — 47 cases | X pass / Y fail / Z blocked | `tests/manual/VELOX-Manual-Tests.xlsx` |
| API — Postman | 30 / 30 | `tests/results/...-postman.json` |
| Security — operations | 5 attacks, 0 vulnerabilities | `security/operations.md` |
| Usability — 5 people | ... | تاب Usability |

3. لو فيه Bugs، ضيفيها في جدول **Defects** تحت (D-13، D-14...).

---

## ٥) الرفع على GitHub من حسابك (15 دقيقة)
1. نزّلي **GitHub Desktop** وسجّلي دخول بحساب **Raheek25**.
2. **File ← Clone repository** ← `abdelrhman19122007/VELOX` ← اختاري فولدر.
3. فوق: **Current branch ← New branch** ← اسمه `raheek/testing`.
4. انسخي ملفاتك جوه الفولدر ده في أماكنها:
   - `tests/manual/VELOX-Manual-Tests.xlsx` + `tests/manual/screenshots/*.png`
   - `tests/results/...-postman.json` + `postman.png`
   - `security/operations.md` + `security/evidence/operations/*.png`
   - `tests/TEST_REPORT.md`
5. هتلاقي التعديلات على الشمال ← تحت اكتبي رسالة، مثلاً: `tests: manual cases M-OPS, Postman run, operations pentest` ← **Commit to raheek/testing**.
6. **Publish branch** (أو **Push origin**).
7. **Create Pull Request** ← هيفتح GitHub ← **Create pull request**. نبيه هيدمجه.

> **ارفعي كل يوم اللي خلص فيه** (commit صغير لكل جزء) بدل رفعة واحدة كبيرة في الآخر. التاريخ هيبان أحسن.
> **متكتبيش أي باسورد** في أي ملف.

---

## ٦) في المناقشة
- إنتي بتقدّمي سلايد **6 و7 و13**، وبتعملي **المطعم في الديمو** (`docs/presentation/PRESENTER_SCRIPT.pdf`).
- أي سؤال عن **الاختبارات** أو **المطعم والمندوب** إنتي اللي بتردي عليه.
- أهم حاجة تكوني فاهماها: الفرق بين **Manual** و**Automated** و**API** testing، و**State Machine** بتاعة الطلب (سلايد 7)، والـ 5 هجمات اللي عملتيها.
