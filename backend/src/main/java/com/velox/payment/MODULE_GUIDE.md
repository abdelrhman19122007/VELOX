# موديول الدفع والمحفظة والإشعارات (payment + notification) — دليل الشرح

**صاحبه:** فادي (@fadyza15-collab)

## الموديول بيعمل إيه
- **الدفع الأونلاين:**
  - **فودافون كاش:** العميل بيكتب رقم محفظته (010…)، فبيوصله كود من 6 أرقام يكتبه في التطبيق. قدامه 10 دقايق.
  - **فوري:** بياخد رقم مرجعي من 9 أرقام يدفع بيه في أي منفذ فوري. قدامه ساعة.
  - **المحفظة:** الطلب بيتدفع منها على طول لو الرصيد يكفي.
- **الطلب مش بيوصل للمطعم غير بعد الدفع:** حالته بتفضل "مستني الدفع"، ولو الوقت خلص بيتلغي لوحده.
- **الاسترجاع:**
  - لو المطعم رفض الطلب أو اتلغى بعد الدفع، الفلوس كلها بترجع المحفظة.
  - لو العميل رجّع الطلب، بيرجعله تمن الأصناف بس (من غير التوصيل).
- **المحفظة:** رصيد وسجل حركات. في وضع التجربة فيه شحن تجريبي لحد 1000 جنيه.
- **نقاط الولاء:** نقطة لكل 10 جنيه من أي طلب اتوصّل، ومرة واحدة بس لكل طلب.
- **أكواد الخصم:**
  - 3 أنواع: نسبة (VELOX10)، مبلغ ثابت (WELCOME)، وتوصيل مجاني (FREESHIP).
  - ليها حد أدنى للطلب وتاريخ انتهاء وعدد مرات استخدام، ومرة واحدة بس لكل عميل.
  - الأدمن بيضيف أكواد جديدة.
- **الإشعارات:**
  - صندوق إشعارات جوه التطبيق.
  - Push للموبايل عن طريق Firebase. ده بيشتغل لوحده أول ما ملفات Firebase تتحط، ومن غيرها كل حاجة شغالة عادي بس من غير Push.

> **وضع التجربة (Sandbox):** فوري وفودافون كاش الحقيقيين محتاجين عقد تاجر. عشان كده عاملين نسخة تجريبية بنفس الخطوات:
> - الكود اللي المفروض يوصل SMS بيظهر في رد `/payment`.
> - فيه زرار "اتدفع في فوري".
>
> لما نتعاقد، هنكتب class جديدة بتعمل `PaymentGateway` من غير ما نغيّر أي حاجة تانية، وبعدين نقفل الوضع التجريبي بـ `velox.payments.sandbox=false`.

## الفايلات
```
payment/
├── gateway/      PaymentGateway، SandboxVodafoneCash، SandboxFawry      ← بوابات الدفع (Strategy)
├── domain/       Payment
├── repository/   PaymentRepository، WalletRepository، PromoRepository، LoyaltyRepository
├── service/      OnlinePaymentStarter، WalletPaymentStarter، PaymentService، RefundListener،
│                 PromoDiscountPolicy، WalletService، PromoAdminService، SandboxCodes
├── controller/   PaymentController
└── dto/          PaymentDtos
notification/     NotificationService، PushService، NotificationController
```
قاعدة البيانات: `database/migrations/V4__payments.sql`، والإشعارات في `V5__operations.sql`.

## إزاي بيتربط بموديول الطلبات (من غير ما نعدّل فيه)
موديول عبد الرحمن عامل 3 "مقابس" في `order/spi`، وموديول فادي بيركّب فيهم:

| المقبس (عند عبد الرحمن) | اللي بيركّب فيه (عند فادي) |
|---|---|
| `PaymentStarter` | `OnlinePaymentStarter` (فودافون كاش وفوري) و`WalletPaymentStarter` |
| `DiscountPolicy` | `PromoDiscountPolicy` |
| حدث `OrderChanged` | `RefundListener` (الاسترجاع)، و`WalletService` (النقاط)، و`NotificationService` (الإشعارات) |

## Design Patterns
| Pattern | فين | ليه |
|---|---|---|
| **Strategy** | `PaymentGateway`، `PaymentStarter`، `DiscountPolicy` | كل طريقة دفع وكل نوع خصم في class لوحدها، ونضيف بوابة حقيقية من غير ما نلمس الباقي |
| **Observer** | `RefundListener`، `NotificationService` | بيسمعوا تغيير حالة الطلب. الاسترجاع بيحصل في نفس الـ transaction (يا الاتنين ينجحوا يا الاتنين يفشلوا)، والإشعار بيتبعت بعد الحفظ بس |
| **Adapter** | `PushService` | بيحوّل "الطلب اتقبل" لطلب HTTP بالشكل اللي Firebase عايزه |

## أسئلة المناقشة المتوقعة
**حد يقدر يجرّب كل الأكواد لحد ما يلاقي كود فودافون كاش؟**
لأ. بعد 5 محاولات غلط الدفع بيتقفل، حتى لو كتب الكود الصح بعدها.
- كان فيه bug هنا: الـ transaction كانت بتعمل rollback مع الخطأ، فعداد المحاولات مكانش بيزيد خالص.
- اكتشفناه بالاختبار `fiveWrongCodesClosesThePayment` وصلّحناه بـ `noRollbackFor`.
- الكود نفسه متخزّن hashed (PBKDF2)، مش نص عادي.

**حد يدفع نفس الطلب مرتين (Replay)؟**
لأ. التحديث شرطي: `UPDATE payments SET status='PAID' WHERE id=? AND status='PENDING'`، فمرة واحدة بس هتنجح، وأي محاولة تانية بترجع `PAYMENT_CLOSED`.

**عميل يشوف أو يدفع طلب عميل تاني؟**
لأ. بياخد `404 ORDER_NOT_FOUND`، بالظبط زي كود طلب مش موجود أصلاً.

**حد يسحب من المحفظة أكتر من رصيده لو بعت طلبين في نفس اللحظة؟**
لأ. قبل ما نخصم بنقفل صف العميل (`SELECT … FOR UPDATE`)، فالطلب التاني بيستنى الأول يخلص وبعدين يشوف الرصيد الجديد.

**حد يخمّن أكواد الخصم؟**
الكود الغلط والمنتهي والمستخدم قبل كده وقليل عن الحد الأدنى… كلهم بيرجعوا نفس الرد `INVALID_PROMO`، فمفيش طريقة يعرف منها إن الكود موجود.

**مفتاح Firebase فين؟**
في ملف `.firebase/service-account.json` على السيرفر بس، والملف ده في `.gitignore` ومش بيترفع على GitHub خالص.

## الاختبارات (18 اختبار)
| الملف | بيختبر إيه |
|---|---|
| `OnlinePaymentApiTest` | فودافون كاش بالكود، الكود الغلط، الحد الأقصى 5 محاولات، فوري، الدفع مرتين، IDOR، انتهاء الوقت، الإلغاء قبل الدفع |
| `WalletAndPromoApiTest` | الاسترجاع للمحفظة، الدفع منها، حدود الشحن، النقاط مرة واحدة، أكواد الخصم بأنواعها وحدودها |
| `NotificationApiTest` | المطعم بيعرف بالطلب بعد الدفع بس، صندوق كل واحد لوحده، الأجهزة |

التشغيل: `cd backend && ./mvnw verify`
