# موديول المحلات والمنتجات (catalog) — دليل الشرح

**صاحبته:** هدى (@hodayasser511-stack)

## الموديول بيعمل إيه
- **للعميل:** عرض المحلات (مفتوح/مقفول، التقييم، المواعيد)، منيو كل محل مقسّم أقسام، البحث بالعربي والإنجليزي، المفضلة.
- **للمطعم:** زرار "بستقبل طلبات"، إيقاف صنف خلص، تغيير السعر — **لمحله هو بس**.
- **للأدمن:** إضافة محل جديد ومعاه حساب الدخول بتاعه، تعديل المحل، إضافة وتعديل الأصناف، رفع الصور.

## الفايلات (Clean Architecture)
```
catalog/
├── controller/   CatalogController (عام)، RestaurantCatalogController (المطعم)، AdminCatalogController (الأدمن)
├── service/      CatalogService، RestaurantCatalogService، StoreAdminService، FavoriteService، ImageStorage
├── repository/   StoreRepository، ProductRepository، FavoriteRepository، IdSequence
├── domain/       Store، Product، OpeningHours، StoreType، ProductSize
├── dto/          CatalogDtos
└── CatalogConfig  حد حجم الرفع + فولدر /uploads
```
قاعدة البيانات: `database/migrations/V2__catalog.sql` (stores، products، favorites).

## الـ API
| الطلب | مين | بيعمل إيه |
|---|---|---|
| `GET /api/stores?openNow=true` | أي حد | المحلات |
| `GET /api/stores/{id}` | أي حد | المحل + المنيو بالأقسام |
| `GET /api/search?q=` | أي حد | بحث في المحلات والأصناف |
| `GET/POST /api/me/favorites/{key}` | عميل | المفضلة |
| `GET/PATCH /api/restaurant/store` | مطعم | محلي + زرار الاستقبال |
| `PATCH /api/restaurant/products/{id}` | مطعم | متاح / السعر |
| `POST /api/admin/stores` | أدمن | محل جديد + حسابه |
| `PATCH /api/admin/stores/{id}` | أدمن | تعديل محل |
| `POST /api/admin/stores/{id}/products` · `PUT /api/admin/products/{id}` | أدمن | الأصناف |
| `POST /api/uploads` | أدمن / مطعم | رفع صورة → بيرجع اللينك |

## Design Patterns
- **Factory-style ids:** نوع المحل (`StoreType`) بيحدد شكل الـ id: مطعم R1، صيدلية P1، سوبر ماركت G1، وأصنافهم F1، M1، Q1.
- **Value object:** `OpeningHours` بيعرف لوحده المحل مفتوح ولا لأ، حتى لو بيقفل بعد نص الليل (12 الضهر → 2 بالليل).

## أسئلة المناقشة المتوقعة
**إزاي البحث محمي من SQL Injection؟** الـ SQL كله بـ parameters (`?`)، وكمان `%` و`_` اللي المستخدم بيكتبهم بيتعاملوا كحروف عادية مش wildcards — عشان محدش يكتب `%` ويسحب كل الداتا. فيه اختبار بيبعت `' OR '1'='1` و`%%`.

**إزاي بتمنعي XSS في أسماء المنتجات؟** أي اسم أو وصف فيه `<` أو `>` بيترفض من الأول (400)، فمفيش حد يقدر يحط `<script>` في اسم منتج يظهر لكل الناس.

**رفع الصور آمن إزاي؟** (أخطر حتة في الموديول)
1. بنشوف **أول bytes في الملف** (magic bytes) — لازم يبقى JPEG أو PNG أو WebP بجد. اسم الملف ونوعه اللي جاي من المستخدم **مش بنصدقهم** (ملف HTML اسمه photo.png بيترفض).
2. اسم الملف بيتغير لاسم عشوائي (UUID) — فمحدش يقدر يكتب `../../` ويوصل لمكان تاني، ولا يكتب فوق ملف موجود.
3. الحد الأقصى 2 ميجا.
4. الصورة بتتعرض بـ header اسمه `nosniff` فالمتصفح عمره ما يشغّلها كصفحة.

**مطعم يقدر يغيّر سعر منتج محل تاني؟** لأ — السيرفر بيتأكد إن المنتج تبع محل الحساب اللي داخل. ولو مش تبعه بيرد "مش موجود" (404) مش "ممنوع"، عشان المهاجم ميعرفش إن المنتج ده موجود أصلاً (IDOR).

**المحل يبان مفتوح إزاي؟** لازم الحاجتين: الساعة دلوقتي (بتوقيت القاهرة) جوه مواعيده، **و** زرار "بستقبل طلبات" شغال.

## الاختبارات (19 — كلها شغالة)
| ملف الاختبار | بيختبر إيه |
|---|---|
| `CatalogUnitTest` | المواعيد (عادية وبعد نص الليل)، الـ wildcards، التعرف على الصور من محتواها |
| `StoreApiTest` | الأدمن يعمل محل وحسابه، عميل يحاول (403)، XSS، مواعيد ناقصة، زرار الاستقبال، IDOR بين محلين |
| `SearchAndFavoritesApiTest` | بحث عربي وإنجليزي، `%%` و`' OR '1'='1`، المفضلة لصاحبها بس |
| `UploadApiTest` | صورة حقيقية، HTML متنكر كصورة، ملف كبير، `../../etc/passwd.png`، عميل يحاول يرفع |

تشغيلهم: `cd backend && ./mvnw verify`
