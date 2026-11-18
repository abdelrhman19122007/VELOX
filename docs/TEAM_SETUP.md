# VELOX على جهازك — لكل واحد في التيم

الهدف: كل واحد يشغّل VELOX **كامل** على جهازه، بقاعدة بيانات خاصة بيه ومطاعم ومنيو حقيقية وحسابات تجربة،
**من غير سيرفر ومن غير أي باسورد من حد**. الطريقة دي هي نفسها اللي هنعرض بيها قدام الدكتور.

> عايز تفهم كل خطوة بتحصل جوه؟ اقرا [RUN_LOCAL.md](RUN_LOCAL.md). الملف ده هو الطريقة السهلة.

---

## ١. المطلوب (مرة واحدة)
| الحاجة | التفاصيل |
|---|---|
| Windows 10 أو 11 | الرامات 8 جيجا أحسن، ومساحة فاضية حوالي 5 جيجا |
| **Docker Desktop** | من https://www.docker.com/products/docker-desktop. سطّبه بالإعدادات العادية (WSL 2)، واعمل Restart للجهاز لو طلب |
| نت | أول مرة بس، عشان ينزّل حوالي 1 جيجا |

**مش محتاج** Java ولا Flutter ولا Maven ولا PostgreSQL ولا Python. كل ده بيشتغل جوه Docker.

## ٢. هات الكود
- **أسهل طريقة:** افتح https://github.com/abdelrhman19122007/VELOX/tree/velox-v2-integration ← زرار **Code** ← **Download ZIP** ← فك الضغط في مكان قصير، زي `C:\VELOX`.
- **أو بـ Git:**
  ```
  git clone -b velox-v2-integration https://github.com/abdelrhman19122007/VELOX.git C:\VELOX
  ```

## ٣. شغّل
1. افتح **Docker Desktop** واستنى لحد ما يكتب تحت على الشمال **Engine running**.
2. ادخل فولدر المشروع ودوس دبل كليك على **`START-VELOX.bat`**.
3. **أول مرة بتاخد من 5 لـ 10 دقايق**، لأنه بينزّل ويبني السيرفر. بعد كده بتاخد أقل من دقيقة.
4. لما يخلص هيكتب **VELOX is running** ويفتح المتصفح لوحده على التطبيق.

| اللينك | إيه ده |
|---|---|
| http://localhost:8080/app/ | **التطبيق** (العميل والمطعم والمندوب، كله من نفس المكان حسب الحساب) |
| http://localhost:8080/ | الموقع |
| http://localhost:8080/admin/ | لوحة التحكم (الأدمن والمطعم) |
| http://localhost:8080/swagger-ui.html | كل عناوين الـ API، تقدر تجرّبها |

## ٤. الحسابات
أول تشغيل بيعمل لوحده:
- **4 مطاعم وكافيهات بالمنيو الحقيقي** (مطعم ناصف فيه 111 صنف بالصور). الكافيهات لسه من غير منيو.
- **حساب لكل مطعم**، و**مندوب تجريبي**، و**عميل تجريبي**.

الباسوردات **اتعملت عشوائي على جهازك** واتحفظت في:
- `demo-data\local-accounts.txt`: المطاعم والمندوب والعميل.
- `.env`: باسورد الأدمن (`VELOX_ADMIN_PASSWORD`)، والإيميل `admin@velox.local`.

الملفين دول **مش بيترفعوا على GitHub**، وكل واحد في التيم عنده باسوردات مختلفة.

## ٥. جرّب كل الأدوار مع بعض
كل متصفح بيفتكر حساب واحد بس، فافتح **3 شبابيك منفصلة**:

| الشباك | ادخل بـ |
|---|---|
| Chrome عادي ← http://localhost:8080/app/ | **العميل** |
| Chrome **Incognito** (Ctrl+Shift+N) ← http://127.0.0.1:8080/app/ | **المطعم** |
| Edge (أو Firefox) ← http://localhost:8080/app/ | **المندوب**، وخليه **أونلاين** |

وبعدين: العميل يطلب من مطعم ناصف ← المطعم يسمع صوت ويدوس **قبول** وبعدين **جاهز** ← المندوب يدوس **خد الطلب** ثم **استلمت** ثم **سلّمت** ← العميل يقيّم.

**من الموبايل:** لو الموبايل على نفس الـ Wi-Fi، افتح من **متصفح الموبايل** اللينك اللي `START-VELOX.bat` كتبه جنب "Phones on the same Wi-Fi" (زي `http://192.168.1.5:8080/app/`). لو Windows سألك عن الـ Firewall، اختار **Private networks** ودوس Allow.
(تطبيق الـ APK نفسه مش هيشتغل على جهازك، لأنه بيرفض أي اتصال من غير HTTPS عشان الأمان. استخدم المتصفح.)

## ٦. الإيقاف والتشغيل تاني
- **وقّف:** دبل كليك على **`STOP-VELOX.bat`**. الداتا بتفضل موجودة.
- **شغّل تاني:** `START-VELOX.bat`.
- **امسح كل حاجة وابدأ من الصفر:** من فولدر المشروع افتح Terminal واكتب
  `docker compose --profile app down -v`، وبعدين امسح `.env` و`demo-data\local-accounts.txt`، وشغّل `START-VELOX.bat`.

## ٧. لو عدّلت في الكود
| عدّلت في | تعمل إيه |
|---|---|
| السيرفر (`backend/`) | `docker compose --profile app up -d --build api` |
| الموقع أو لوحة التحكم (`web/`) | ولا حاجة، اعمل Refresh بس (Ctrl+Shift+R) |
| التطبيق (`mobile/`) | محتاج Flutter: `flutter build web --release --base-href /app/` وبعدين انسخ `mobile\build\web` مكان `demo-data\app` |

## ٨. مشاكل وحلولها
| اللي ظاهر | الحل |
|---|---|
| `Docker is not running` | افتح Docker Desktop واستنى **Engine running** |
| Docker Desktop فاضي أو بيقفل لوحده | اقفله ← اعمل Rename لفولدر `%LOCALAPPDATA%\Docker\run` (مثلاً `run.old`) ← افتحه تاني. ولو لسه، اعمل Restart للجهاز |
| وهو بيبني: `certificate verify failed` أو `Failed to fetch https://repo.maven.apache.org` | برنامج الحماية بيفتح الاتصالات المشفّرة (زي **Avast ← Web Shield ← Enable HTTPS scanning**). اقفل الخاصية دي، أو وقّف الحماية من النت شوية، وشغّل تاني |
| `The container name "/velox-db" is already in use` | عندك نسخة قديمة من VELOX على الجهاز. ضيف في `.env` السطرين `VELOX_DB_CONTAINER=velox-db-2` و`VELOX_API_CONTAINER=velox-api-2` |
| `port is already allocated` / 8080 مستخدم | افتح `.env` وغيّر `VELOX_PORT=8080` لـ `VELOX_PORT=8090`، واستخدم 8090 في اللينكات |
| برنامج الحماية مسح `START-VELOX.bat` | ضيف فولدر المشروع في **Exceptions** بتاعة البرنامج، ورجّع الملف |
| `VELOX did not answer in 5 minutes` | اكتب `docker compose logs api` وابعت آخر 30 سطر للجروب |
| المنيو مش ظاهر | اكتب `docker compose logs demo-loader` |
| التطبيق شكله قديم | Ctrl+Shift+R |

## ٩. إيه اللي بيشتغل جوه Docker (للفهم)
```
START-VELOX.bat
  ├── .env                ← باسورد أدمن ومفتاح توكن عشوائيين (أول مرة بس)
  └── docker compose --profile app up -d --build
        ├── db           PostgreSQL 16         (الداتا في volume اسمه velox-db)
        ├── api          سيرفر Java (بيتبني من backend/Dockerfile)
        │                 + بيعرض web/ على /  و demo-data/app على /app/
        └── demo-loader  Python بيشتغل مرة واحدة: بيحمّل demo-data/catalog.json
                         والصور، ويعمل الحسابات ← demo-data/local-accounts.txt
```
