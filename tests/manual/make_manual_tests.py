"""Builds tests/manual/VELOX-Manual-Tests.xlsx: manual test cases per team member, a live summary, a bug log and a
usability form. Edit the CASES list here and run:  python tests/manual/make_manual_tests.py"""
import pathlib

from openpyxl import Workbook
from openpyxl.formatting.rule import CellIsRule
from openpyxl.styles import Alignment, Border, Font, PatternFill, Side
from openpyxl.utils import get_column_letter
from openpyxl.worksheet.datavalidation import DataValidation

OUT = pathlib.Path(__file__).with_name('VELOX-Manual-Tests.xlsx')
BRAND = '9C3D10'
HEAD = PatternFill('solid', fgColor=BRAND)
SOFT = PatternFill('solid', fgColor='FCEAE3')
WHITE = Font(color='FFFFFF', bold=True)
THIN = Side(style='thin', color='E6D3CB')
BOX = Border(left=THIN, right=THIN, top=THIN, bottom=THIN)
WRAP = Alignment(wrap_text=True, vertical='top')
RTL = Alignment(wrap_text=True, vertical='top', horizontal='right', readingOrder=2)

# (id, module, owner, title, preconditions, steps, expected)
CASES = [
    # ---- Accounts & security — Nabeeh
    ('M-ACC-01', 'Accounts', 'Nabeeh', 'تسجيل عميل جديد', 'VELOX شغال، مش داخل بحساب',
     '1) افتح /app/ ← "حساب جديد"  2) اكتب اسم وإيميل جديد وموبايل 01xxxxxxxxx وباسورد 8 حروف  3) "إنشاء الحساب"',
     'الحساب يتعمل ويدخل على الرئيسية، و"صباح الفل/مساء الخير يا <الاسم>" ظاهرة'),
    ('M-ACC-02', 'Accounts', 'Nabeeh', 'تسجيل بإيميل مستخدم قبل كده', 'فيه حساب بنفس الإيميل',
     '1) "حساب جديد" بنفس إيميل M-ACC-01  2) إنشاء', 'رسالة "الإيميل ده عليه حساب"، ومفيش حساب جديد'),
    ('M-ACC-03', 'Accounts', 'Nabeeh', 'باسورد أقل من 8 حروف', '—',
     '1) "حساب جديد" بباسورد 5 حروف', 'رسالة إن الباسورد قصير، والحساب مش بيتعمل'),
    ('M-ACC-04', 'Accounts', 'Nabeeh', 'دخول بباسورد غلط', 'حساب موجود',
     '1) "تسجيل الدخول" بإيميل صح وباسورد غلط', 'رسالة خطأ، ومفيش دخول'),
    ('M-ACC-05', 'Security', 'Nabeeh', 'قفل بعد 5 محاولات غلط (Brute force)', 'حساب موجود',
     '1) ادخل بباسورد غلط 5 مرات ورا بعض  2) جرّب المرة السادسة حتى بالباسورد الصح', 'المرة السادسة: رسالة "محاولات كتير" (429)'),
    ('M-ACC-06', 'Accounts', 'Nabeeh', 'تغيير الباسورد', 'داخل بحساب عميل في متصفحين',
     '1) حسابي ← "تغيير كلمة السر"  2) الحالية غلط ← حفظ  3) الحالية صح والجديدة مرتين ← حفظ  4) اعمل Refresh في المتصفح التاني',
     '2) خطأ  3) "اتغيرت"  4) المتصفح التاني يخرج من الحساب (التوكنات القديمة بطلت)'),
    ('M-ACC-07', 'Security', 'Nabeeh', 'عميل يحاول يدخل لوحة الأدمن', 'حساب عميل',
     '1) افتح /admin/  2) ادخل بحساب العميل', 'مرفوض برسالة واضحة'),
    ('M-ACC-08', 'Security', 'Nabeeh', 'SQL Injection في الدخول', '—',
     "1) في الإيميل اكتب  ' OR 1=1 --  وأي باسورد  2) دخول", 'مرفوض، ومفيش دخول ولا رسالة خطأ تقنية'),
    ('M-ACC-09', 'Accounts', 'Nabeeh', 'زرار واتساب الإدارة', 'شاشة الدخول',
     '1) دوس "كلّم الإدارة واتساب" تحت "نسيت كلمة السر"', 'واتساب بيفتح على 01287654219 والرسالة مكتوبة جاهزة'),
    ('M-ACC-10', 'Accounts', 'Nabeeh', 'تغيير اللغة', 'داخل بأي حساب',
     '1) دوس English فوق  2) ارجع العربي', 'كل الكلام يتغير، والاتجاه يبقى من الشمال لليمين في الإنجليزي'),
    # ---- Catalog — Hoda
    ('M-CAT-01', 'Catalog', 'Hoda', 'المطاعم في الرئيسية', 'داخل كعميل',
     '1) انزل في الرئيسية', '"مطاعم مميزة" و"المحلات" ظاهرين بالصور، ومطعم ناصف "مفتوح"'),
    ('M-CAT-02', 'Catalog', 'Hoda', 'منيو مطعم ناصف', 'داخل كعميل',
     '1) افتح مطعم ناصف', 'الأقسام والأصناف ظاهرة بالأسعار والصور (111 صنف)'),
    ('M-CAT-03', 'Catalog', 'Hoda', 'البحث', 'داخل كعميل',
     '1) دوس على البحث  2) اكتب "كفتة"', 'تظهر أصناف فيها كفتة من ناصف'),
    ('M-CAT-04', 'Catalog', 'Hoda', 'فلتر التصنيفات', 'داخل كعميل',
     '1) في "عايز تاكل إيه؟" اختار "كافيهات"', 'تظهر الكافيهات بس'),
    ('M-CAT-05', 'Catalog', 'Hoda', 'المفضلة', 'داخل كعميل',
     '1) افتح مطعم ← ❤  2) ارجع الرئيسية ← فلتر "المفضلة"', 'المطعم ظاهر في المفضلة'),
    ('M-CAT-06', 'Catalog', 'Hoda', 'المطعم يغيّر السعر', 'متصفح مطعم + متصفح عميل',
     '1) المطعم: تاب "المنيو" ← دوس على السعر ← غيّره ← حفظ  2) العميل: Refresh وافتح المطعم', 'العميل يشوف السعر الجديد'),
    ('M-CAT-07', 'Catalog', 'Hoda', 'صنف خلص', 'متصفح مطعم + متصفح عميل',
     '1) المطعم: اقفل الزرار جنب صنف  2) العميل: افتح المطعم', 'الصنف مش متاح للطلب'),
    ('M-CAT-08', 'Catalog', 'Hoda', 'المطعم يوقف استقبال الطلبات', 'متصفح مطعم + عميل',
     '1) المطعم: اقفل "بنستقبل طلبات" في الكارت اللي فوق  2) العميل: Refresh', 'المطعم يظهر مقفول ومينفعش الطلب منه. وبعد التجربة رجّعه'),
    ('M-CAT-09', 'Catalog', 'Hoda', 'الأدمن يضيف صنف', 'داخل /admin/ كأدمن',
     '1) المطاعم ← ناصف ← إضافة صنف (اسم عربي وإنجليزي وسعر وقسم) ← حفظ  2) العميل يفتح ناصف', 'الصنف الجديد ظاهر عند العميل'),
    # ---- Orders, website, dashboard — Abdelrhman
    ('M-ORD-01', 'Orders', 'Abdelrhman', 'السلة', 'داخل كعميل',
     '1) ضيف صنفين من ناصف  2) زوّد الكمية وقللها في السلة', 'الإجمالي بيتغير صح مع كل تغيير'),
    ('M-ORD-02', 'Orders', 'Abdelrhman', 'رسوم التوصيل قبل التأكيد', 'السلة فيها أصناف',
     '1) "إتمام الطلب"  2) غيّر المنطقة', 'رسوم التوصيل والإجمالي ظاهرين ويتغيروا حسب المنطقة قبل التأكيد'),
    ('M-ORD-03', 'Orders', 'Abdelrhman', 'طلب كاش بعلامة مميزة', 'السلة فيها أصناف',
     '1) اكتب العنوان والعلامة المميزة وملاحظة "من غير بصل"  2) كاش  3) أكّد', 'رقم الطلب يظهر (ORD-xxxx) والحالة "اتطلب"'),
    ('M-ORD-04', 'Orders', 'Abdelrhman', 'كوبون WELCOME', 'طلب أكبر من 60 ج.م',
     '1) في الدفع اكتب WELCOME  2) طبّق', 'خصم 20 ج.م'),
    ('M-ORD-05', 'Orders', 'Abdelrhman', 'كوبون غلط', 'في شاشة الدفع',
     '1) اكتب ABC123  2) طبّق', 'رسالة إن الكود مش صالح، ومفيش خصم'),
    ('M-ORD-06', 'Orders', 'Abdelrhman', 'العميل يلغي قبل القبول', 'طلب لسه "اتطلب"',
     '1) طلباتي ← الطلب ← إلغاء', 'الطلب يتلغي'),
    ('M-ORD-07', 'Orders', 'Abdelrhman', 'مينفعش يلغي بعد القبول', 'المطعم قبل الطلب',
     '1) العميل يفتح الطلب', 'زرار الإلغاء مش موجود، أو بيطلع رسالة إن الوقت فات'),
    ('M-ORD-08', 'Orders', 'Abdelrhman', 'مشوار "هاتلي أي حاجة"', 'داخل كعميل',
     '1) الرئيسية ← كارت المشوار  2) اكتب الطلبات والمحل  3) أكّد', 'الطلب يتعمل ويظهر للمندوب'),
    ('M-ORD-09', 'Orders', 'Abdelrhman', 'التتبع من غير Refresh', 'طلب شغال + مطعم ومندوب في متصفحات تانية',
     '1) العميل يفتح شاشة تتبع الطلب  2) المطعم يقبل ويقول جاهز، والمندوب ياخد', 'الخطوات تتحدث عند العميل لوحدها'),
    ('M-ORD-10', 'Dashboard', 'Abdelrhman', 'الطلب في لوحة الأدمن', 'داخل /admin/',
     '1) افتح الطلبات والإحصائيات', 'الطلب ظاهر بحالته، وأرقام النهارده اتحدثت'),
    # ---- Payments, notifications, customer screens — Fady
    ('M-PAY-01', 'Payments', 'Fady', 'المحفظة', 'داخل كعميل',
     '1) حسابي ← المحفظة', 'الرصيد ظاهر (0 ج.م لحساب جديد)'),
    ('M-PAY-02', 'Notifications', 'Fady', 'إشعار رفض الطلب', 'طلب جديد',
     '1) المطعم يرفض بسبب "خلص"  2) العميل يفتح الجرس 🔔', 'إشعار إن الطلب اترفض ومعاه السبب'),
    ('M-PAY-03', 'Notifications', 'Fady', 'قراءة الإشعارات', 'فيه إشعارات جديدة',
     '1) الرقم الأحمر على الجرس  2) افتح الإشعارات', 'الرقم يختفي بعد القراءة'),
    ('M-PAY-04', 'Payments', 'Fady', 'الفكّة', 'السلة فيها أصناف',
     '1) كاش ← "هدفع بـ 500"  2) أكّد  3) المندوب ياخد الطلب', 'كارت المندوب فيه "العميل هيدفع 500 — رجّعله ..." والرقم صح'),
    ('M-PAY-05', 'Feedback', 'Fady', 'التقييم بعد التسليم', 'طلب اتسلّم',
     '1) طلباتي ← الطلب ← قيّم (نجوم وتعليق)  2) المطعم: تاب "النهارده"', 'التقييم ظاهر عند المطعم'),
    ('M-PAY-06', 'Feedback', 'Fady', 'شكوى', 'طلب اتسلّم',
     '1) الطلب ← شكوى ← اكتب السبب  2) الأدمن: الشكاوى', 'الشكوى ظاهرة عند الأدمن'),
    ('M-PAY-07', 'Orders', 'Fady', 'اطلب تاني', 'طلب اتسلّم قبل كده',
     '1) الرئيسية ← "اطلب تاني"', 'نفس الأصناف تتضاف للسلة'),
    ('M-PAY-08', 'Accounts', 'Fady', 'العناوين المحفوظة', 'داخل كعميل',
     '1) حسابي ← العناوين ← ضيف عنوان  2) اطلب واختار العنوان', 'العنوان يتملي لوحده في الدفع'),
    # ---- Operations — Raheek
    ('M-OPS-01', 'Operations', 'Raheek', 'الطلب الجديد يرن عند المطعم', 'متصفح مطعم مفتوح',
     '1) العميل يطلب من ناصف', 'المطعم يسمع صوت، والطلب يظهر في "جديدة" من غير Refresh، وعدّاد "مستني" يزيد'),
    ('M-OPS-02', 'Operations', 'Raheek', 'القبول', 'طلب في "جديدة"',
     '1) المطعم ← قبول', 'الطلب ينتقل لـ "بتتجهز"'),
    ('M-OPS-03', 'Operations', 'Raheek', 'الرفض بسبب', 'طلب في "جديدة"',
     '1) المطعم ← رفض ← "الصنف خلص"', 'الطلب يترفض، والعميل يشوف السبب'),
    ('M-OPS-04', 'Operations', 'Raheek', 'جاهز', 'طلب في "بتتجهز"',
     '1) المطعم ← جاهز', 'الطلب ينتقل لـ "جاهزة"'),
    ('M-OPS-05', 'Operations', 'Raheek', 'أونلاين / أوفلاين', 'حساب مندوب',
     '1) اقفل زرار أونلاين  2) افتحه', 'وهو أوفلاين: مفيش طلبات متاحة. وهو أونلاين: الطلبات تظهر'),
    ('M-OPS-06', 'Operations', 'Raheek', 'كارت الطلب عند المندوب', 'طلب اتقبل + مندوب أونلاين',
     '1) المندوب يبص على "طلبات متاحة"', 'رقم الطلب والأصناف واسم العميل والعنوان والعلامة المميزة والكاش وأرباحه، كلهم صح'),
    ('M-OPS-07', 'Operations', 'Raheek', 'خد الطلب', 'طلب متاح',
     '1) المندوب ← خد الطلب  2) المطعم يبص على الطلب', 'المطعم يشوف "المندوب X جاي ياخده" وزرار اتصال'),
    ('M-OPS-08', 'Operations', 'Raheek', 'مينفعش يستلم قبل جاهز', 'المندوب خد طلب لسه "بتتجهز"',
     '1) المندوب ← استلمت', 'مرفوض لحد ما المطعم يقول جاهز'),
    ('M-OPS-09', 'Operations', 'Raheek', 'التسليم والأرباح', 'المندوب استلم الطلب',
     '1) المندوب ← سلّمت', 'الحالة "اتسلّم"، وأرباح النهارده والتوصيلات بيزيدوا'),
    ('M-OPS-10', 'Operations', 'Raheek', 'إلغاء أوتوماتيك لو المطعم مردّش', 'طلب جديد',
     '1) سيب الطلب 15 دقيقة من غير رد', 'الطلب يتلغي لوحده "The store didn\'t answer"، والعميل يتبلّغ'),
]

OWNERS = ['Nabeeh', 'Hoda', 'Abdelrhman', 'Fady', 'Raheek']
COLS = [('ID', 11), ('Module', 13), ('Owner', 12), ('Test case', 26), ('Preconditions', 26), ('Steps', 52),
        ('Expected result', 42), ('Actual result', 32), ('Status', 11), ('Bug ID', 10), ('Tester', 12), ('Date', 12),
        ('Screenshot / notes', 26)]


def header(ws, cols, row=1):
    for i, (name, width) in enumerate(cols, 1):
        c = ws.cell(row=row, column=i, value=name)
        c.fill, c.font, c.border = HEAD, WHITE, BOX
        c.alignment = Alignment(horizontal='center', vertical='center', wrap_text=True)
        ws.column_dimensions[get_column_letter(i)].width = width
    ws.row_dimensions[row].height = 30
    ws.freeze_panes = ws.cell(row=row + 1, column=1)


wb = Workbook()

# ---------- How to ----------
how = wb.active
how.title = 'How to'
how.sheet_view.rightToLeft = True
how.column_dimensions['A'].width = 110
lines = [
    ('VELOX — الاختبار اليدوي (Manual Testing)', Font(bold=True, size=16, color=BRAND)),
    ('', None),
    ('1) شغّل VELOX على جهازك بـ START-VELOX.bat (docs/TEAM_SETUP.md).', None),
    ('2) افتح تاب "Test Cases" واعمل Filter على عمود Owner باسمك.', None),
    ('3) نفّذ كل حالة بإيدك بالخطوات المكتوبة، وقارن باللي في "Expected result".', None),
    ('4) اكتب اللي حصل فعلاً في "Actual result"، واختار Status: Pass / Fail / Blocked.', None),
    ('5) خد سكرينشوت (Win+Shift+S) واحفظه في tests/manual/screenshots/<ID>.png، واكتب اسمه في آخر عمود.', None),
    ('6) لو Fail: سجّل الغلطة في تاب "Bug Reports" (BUG-01، BUG-02...) واكتب رقمها في عمود Bug ID.', None),
    ('7) تاب "Summary" بيحسب النتايج لوحده.', None),
    ('8) ارفع الملف والسكرينشوتات على GitHub من حسابك (GitHub Desktop) في فرع باسمك.', None),
    ('', None),
    ('حسابات التجربة: مكتوبة في آخر START-VELOX وفي demo-data\\local-accounts.txt. متكتبش باسوردات في الملف ده.', Font(bold=True)),
    ('محتاج متصفحين أو تلاتة: Chrome (عميل) و Chrome Incognito (مطعم) و Edge (مندوب).', None),
]
for i, (text, font) in enumerate(lines, 1):
    c = how.cell(row=i, column=1, value=text)
    c.alignment = RTL
    if font:
        c.font = font

# ---------- Test cases ----------
ws = wb.create_sheet('Test Cases')
ws.sheet_view.rightToLeft = True
header(ws, COLS)
for r, case in enumerate(CASES, 2):
    for col, val in enumerate(case, 1):
        c = ws.cell(row=r, column=col, value=val)
        c.alignment, c.border = RTL, BOX
    for col in range(len(case) + 1, len(COLS) + 1):
        ws.cell(row=r, column=col).border = BOX
        ws.cell(row=r, column=col).alignment = RTL
    if r % 2 == 0:
        for col in range(1, 4):
            ws.cell(row=r, column=col).fill = SOFT
    ws.row_dimensions[r].height = 75
last = len(CASES) + 1
status = DataValidation(type='list', formula1='"Pass,Fail,Blocked,Not run"', allow_blank=True)
ws.add_data_validation(status)
status.add(f'I2:I{last}')
tester = DataValidation(type='list', formula1='"' + ','.join(OWNERS) + '"', allow_blank=True)
ws.add_data_validation(tester)
tester.add(f'K2:K{last}')
for word, color in (('Pass', 'C8F0D4'), ('Fail', 'F9C9C4'), ('Blocked', 'FBE5B6')):
    ws.conditional_formatting.add(f'I2:I{last}', CellIsRule(operator='equal', formula=[f'"{word}"'], fill=PatternFill('solid', fgColor=color)))
ws.auto_filter.ref = f'A1:{get_column_letter(len(COLS))}{last}'

# ---------- Summary ----------
sm = wb.create_sheet('Summary')
sm.sheet_view.rightToLeft = True
header(sm, [('Owner', 16), ('Cases', 10), ('Pass', 10), ('Fail', 10), ('Blocked', 10), ('Not run', 10), ('Done %', 10)])
for r, owner in enumerate(OWNERS + ['TOTAL'], 2):
    sm.cell(row=r, column=1, value=owner).font = Font(bold=owner == 'TOTAL')
    rng_o, rng_s = f"'Test Cases'!$C$2:$C${last}", f"'Test Cases'!$I$2:$I${last}"
    if owner == 'TOTAL':
        for col in range(2, 7):
            L = get_column_letter(col)
            sm.cell(row=r, column=col, value=f'=SUM({L}2:{L}{r - 1})')
    else:
        sm.cell(row=r, column=2, value=f'=COUNTIF({rng_o},A{r})')
        for col, word in zip(range(3, 6), ('Pass', 'Fail', 'Blocked')):
            sm.cell(row=r, column=col, value=f'=COUNTIFS({rng_o},A{r},{rng_s},"{word}")')
        sm.cell(row=r, column=6, value=f'=B{r}-C{r}-D{r}-E{r}')
    sm.cell(row=r, column=7, value=f'=IF(B{r}=0,0,(C{r}+D{r}+E{r})/B{r})').number_format = '0%'
    for col in range(1, 8):
        sm.cell(row=r, column=col).border = BOX

# ---------- Bug reports ----------
bg = wb.create_sheet('Bug Reports')
bg.sheet_view.rightToLeft = True
header(bg, [('Bug ID', 10), ('Test case', 11), ('Title', 30), ('Steps to reproduce', 45), ('Expected', 30), ('Actual', 30),
            ('Severity', 12), ('Reporter', 12), ('Date', 12), ('Status', 12), ('Screenshot', 22)])
sev = DataValidation(type='list', formula1='"Critical,High,Medium,Low"', allow_blank=True)
bst = DataValidation(type='list', formula1='"Open,Fixed,Won\'t fix"', allow_blank=True)
bg.add_data_validation(sev)
bg.add_data_validation(bst)
sev.add('G2:G60')
bst.add('J2:J60')
example = ['BUG-00', 'M-XXX-00', 'مثال: الإجمالي مش بيتحدث لما أقلل الكمية', '1) ... 2) ... 3) ...', 'الإجمالي يقل',
           'الإجمالي فضل زي ما هو', 'Medium', 'Raheek', '2026-11-20', 'Open', 'BUG-00.png']
for col, val in enumerate(example, 1):
    c = bg.cell(row=2, column=col, value=val)
    c.alignment, c.border = RTL, BOX
    c.font = Font(italic=True, color='85736B')

# ---------- Usability ----------
us = wb.create_sheet('Usability')
us.sheet_view.rightToLeft = True
header(us, [('Participant', 14), ('Task', 46), ('Finished? (Yes/No)', 14), ('Time (sec)', 11), ('Mistakes', 10),
            ('Easy? (1-5)', 11), ('Comment', 40), ('Tester', 12)])
tasks = ['اعمل حساب جديد', 'لاقي مطعم ناصف وضيف صنفين للسلة', 'اطلب كاش واكتب علامة مميزة لبيتك',
         'تابع الطلب لحد ما يتسلّم', 'قيّم الطلب']
r = 2
for p in range(1, 6):
    for t in tasks:
        us.cell(row=r, column=1, value=f'P{p}')
        us.cell(row=r, column=2, value=t).alignment = RTL
        for col in range(1, 9):
            us.cell(row=r, column=col).border = BOX
        r += 1
us.cell(row=r + 1, column=2, value='كل واحد في التيم يجرّب على شخص واحد مش بيعرف التطبيق: اديله المهمة بس من غير ما تساعده، واكتب اللي حصل.').alignment = RTL

wb.save(OUT)
print('saved', OUT, '| cases:', len(CASES), {o: sum(1 for c in CASES if c[2] == o) for o in OWNERS})
