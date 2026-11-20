"""VELOX deck as a native PowerPoint (16:9). Same content and speaker notes as the Slides artifact."""
import pathlib
import re

from lxml import etree
from pptx import Presentation
from pptx.dml.color import RGBColor
from pptx.enum.shapes import MSO_CONNECTOR, MSO_SHAPE
from pptx.enum.text import MSO_ANCHOR, PP_ALIGN
from pptx.oxml.ns import qn
from pptx.util import Inches, Pt

HERE = pathlib.Path(__file__).parent
OUT = HERE / 'VELOX-Presentation.pptx'
FONT = 'Calibri'

# Colours taken from the VELOX logo: its black (#0E0F0F) and its amber/gold (#E8A21E). White pages, gold only as an
# accent, green only for "done/passed". Names kept from the first draft: SAND = page white, TERRA = dark gold for text.
INK, SAND, CLAY, PAPER = '111214', 'FFFFFF', 'F3F4F6', 'F7F8FA'
TERRA, TERRA_SOFT, TERRA_LIGHT = '8A5A00', 'FFF1CF', 'E8A21E'
GOLD = 'E8A21E'
NILE, NILE_SOFT, MUTED, LINE = '1E8E4E', 'DDF3E6', '565C64', 'E1E4E8'
NIGHT_CARD, NIGHT_LINE, NIGHT_TEXT = '1D1F23', '34373C', 'C9CDD2'

prs = Presentation()
prs.slide_width, prs.slide_height = Inches(13.333), Inches(7.5)
BLANK = prs.slide_layouts[6]
L, W = 0.75, 11.833  # left margin and content width (in)


def rgb(h):
    return RGBColor.from_string(h)


def notes_for(slide_id):
    html = (HERE / 'notes' / f'{slide_id}.html').read_text(encoding='utf-8')
    m = re.search(r'<aside>(.*?)</aside>', html, re.S)
    return m.group(1).strip() if m else ''


def new_slide(bg, slide_id):
    s = prs.slides.add_slide(BLANK)
    s.background.fill.solid()
    s.background.fill.fore_color.rgb = rgb(bg)
    s.notes_slide.notes_text_frame.text = notes_for(slide_id)
    return s


def text(s, x, y, w, h, runs, size=16, color=INK, bold=False, align=PP_ALIGN.LEFT, anchor=MSO_ANCHOR.TOP, spacing=1.1, name=None):
    """runs: a string, or a list of paragraphs, each a string or a list of (text, {bold, color}) pieces."""
    tb = s.shapes.add_textbox(Inches(x), Inches(y), Inches(w), Inches(h))
    if name:
        tb.name = name
    tf = tb.text_frame
    tf.word_wrap = True
    tf.margin_left = tf.margin_right = tf.margin_top = tf.margin_bottom = 0
    tf.vertical_anchor = anchor
    paras = [runs] if isinstance(runs, str) else runs
    for i, para in enumerate(paras):
        p = tf.paragraphs[0] if i == 0 else tf.add_paragraph()
        p.alignment = align
        p.line_spacing = spacing
        pieces = [(para, {})] if isinstance(para, str) else para
        for t, opt in pieces:
            r = p.add_run()
            r.text = t
            f = r.font
            f.name, f.size, f.bold = FONT, Pt(opt.get('size', size)), opt.get('bold', bold)
            f.color.rgb = rgb(opt.get('color', color))
    return tb


def box(s, x, y, w, h, fill, line=None, shape=MSO_SHAPE.ROUNDED_RECTANGLE, radius=0.12, line_w=1.25, name=None):
    sh = s.shapes.add_shape(shape, Inches(x), Inches(y), Inches(w), Inches(h))
    if name:
        sh.name = name
    if shape == MSO_SHAPE.ROUNDED_RECTANGLE:
        sh.adjustments[0] = radius
    if fill:
        sh.fill.solid()
        sh.fill.fore_color.rgb = rgb(fill)
    else:
        sh.fill.background()
    if line:
        sh.line.color.rgb = rgb(line)
        sh.line.width = Pt(line_w)
    else:
        sh.line.fill.background()
    sh.shadow.inherit = False
    sh.text_frame.text = ''
    return sh


def bullets(s, x, y, w, h, items, size=16, color=INK, gap=6):
    tb = s.shapes.add_textbox(Inches(x), Inches(y), Inches(w), Inches(h))
    tf = tb.text_frame
    tf.word_wrap = True
    tf.margin_left = tf.margin_right = tf.margin_top = tf.margin_bottom = 0
    for i, item in enumerate(items):
        p = tf.paragraphs[0] if i == 0 else tf.add_paragraph()
        p.space_after = Pt(gap)
        pPr = p._p.get_or_add_pPr()
        pPr.set('marL', str(Inches(0.28)))
        pPr.set('indent', str(-Inches(0.28)))
        bu_clr = etree.SubElement(pPr, qn('a:buClr'))
        etree.SubElement(bu_clr, qn('a:srgbClr')).set('val', GOLD)
        etree.SubElement(pPr, qn('a:buFont')).set('typeface', 'Arial')
        etree.SubElement(pPr, qn('a:buChar')).set('char', '•')
        pieces = [(item, {})] if isinstance(item, str) else item
        for t, opt in pieces:
            r = p.add_run()
            r.text = t
            r.font.name, r.font.size, r.font.bold = FONT, Pt(size), opt.get('bold', False)
            r.font.color.rgb = rgb(opt.get('color', color))
    return tb


def head(s, eyebrow, title, dark=False):
    text(s, L, 0.62, W, 0.32, eyebrow.upper(), size=13, bold=True, color=TERRA_LIGHT if dark else TERRA, name='Eyebrow')
    text(s, L, 0.95, W, 0.75, title, size=36, bold=True, color=SAND if dark else INK, name='Title')


def foot(s, n, dark=False):
    c = 'A7ABB1' if dark else MUTED
    text(s, L, 6.95, 3, 0.3, 'VELOX', size=11, bold=True, color=c)
    text(s, L + W - 1, 6.95, 1, 0.3, str(n), size=11, color=c, align=PP_ALIGN.RIGHT)


def banner(s, y, runs, h=0.62):
    box(s, L, y, W, h, INK, radius=0.18)
    text(s, L + 0.35, y, W - 0.7, h, [runs], size=17, color=SAND, anchor=MSO_ANCHOR.MIDDLE)


def card(s, x, y, w, h, fill=PAPER, line=LINE):
    return box(s, x, y, w, h, fill, line)


def arrow(s, x1, y1, x2, y2, color, both=False, width=2.5):
    c = s.shapes.add_connector(MSO_CONNECTOR.STRAIGHT, Inches(x1), Inches(y1), Inches(x2), Inches(y2))
    c.line.color.rgb = rgb(color)
    c.line.width = Pt(width)
    ln = c.line._get_or_add_ln()
    etree.SubElement(ln, qn('a:tailEnd')).set('type', 'triangle')
    if both:
        etree.SubElement(ln, qn('a:headEnd')).set('type', 'triangle')
    return c


def cols(n, gap):
    w = (W - gap * (n - 1)) / n
    return [(L + i * (w + gap), w) for i in range(n)]


# 1 ---------------------------------------------------------------- cover
s = new_slide(INK, 'cover')
box(s, 8.6, -1.5, 6.2, 6.2, None, NIGHT_LINE, MSO_SHAPE.OVAL, line_w=2)
box(s, 10.9, 2.2, 3.3, 3.3, None, GOLD, MSO_SHAPE.OVAL, line_w=2)
text(s, L, 1.1, 9, 0.35, 'GRADUATION PROJECT · DIGITAL EGYPT PIONEERS INITIATIVE (DEPI)', size=14, bold=True, color=TERRA_LIGHT)
text(s, L, 1.6, 9, 1.5, 'VELOX', size=96, bold=True, color=SAND)
text(s, L, 3.25, 9, 0.6, 'Your favorite food, at your doorstep.', size=32, color=TERRA_LIGHT)
text(s, L, 3.95, 9, 0.5, 'A smart food delivery platform for Samannoud City', size=22, color=NIGHT_TEXT)
text(s, L, 6.55, 5, 0.4, 'Penetration Testing Track', size=16, color=NIGHT_TEXT)
text(s, 5.2, 6.55, 7.38, 0.4, 'Team VELOX · Nabeeh · Hoda · Abdelrhman · Fady · Raheek', size=16, color=NIGHT_TEXT, align=PP_ALIGN.RIGHT)

# 2 ---------------------------------------------------------------- problem
s = new_slide(CLAY, 'problem')
head(s, 'The problem', 'Ordering food in Samannoud still runs on phone calls')
for (x, w), (k, items) in zip(cols(2, 0.35), [
        ('FOR CUSTOMERS', ['No organized menus with real prices', 'Delivery fee unknown until the door', 'No idea where the order is', 'Mistakes from orders taken by phone']),
        ('FOR RESTAURANTS', ['Orders scattered across calls and chats', 'Misheard items and addresses', 'No simple tool to update menus and prices', 'No courier network of their own'])]):
    card(s, x, 2.0, w, 3.3, SAND)
    text(s, x + 0.4, 2.3, w - 0.8, 0.35, k, size=15, bold=True, color=TERRA)
    bullets(s, x + 0.4, 2.8, w - 0.8, 2.4, items, size=18, gap=8)
banner(s, 5.65, [('Our opportunity: ', {'bold': True, 'color': TERRA_LIGHT}), ('one digital platform that makes local ordering clear, fast and organized.', {})])
foot(s, 2)

# 3 ---------------------------------------------------------------- solution
s = new_slide(SAND, 'solution')
head(s, 'Our solution', 'One platform for the whole delivery')
tiles = [('Real digital menus', 'Photos and prices from Samannoud’s restaurants'), ('Clear ordering', 'Full cost and delivery fee before you confirm'),
         ('Live tracking', 'Follow the courier on the map'), ('Pay your way', 'Cash on delivery and wallet balance'),
         ('A screen per role', 'Customer, restaurant and courier in one app'), ('Admin dashboard', 'Stores, zones, fees, users and complaints')]
for i, (t, d) in enumerate(tiles):
    (x, w), y = cols(3, 0.35)[i % 3], 2.0 + (i // 3) * 2.35
    card(s, x, y, w, 2.05)
    box(s, x + 0.35, y + 0.35, 0.5, 0.5, TERRA_SOFT, shape=MSO_SHAPE.OVAL)
    text(s, x + 0.35, y + 0.35, 0.5, 0.5, str(i + 1), size=16, bold=True, color=TERRA, align=PP_ALIGN.CENTER, anchor=MSO_ANCHOR.MIDDLE)
    text(s, x + 0.35, y + 1.0, w - 0.7, 0.4, t, size=20, bold=True)
    text(s, x + 0.35, y + 1.4, w - 0.7, 0.55, d, size=15, color=MUTED)
foot(s, 3)

# 4 ---------------------------------------------------------------- roles
s = new_slide(CLAY, 'roles')
head(s, 'The VELOX ecosystem', 'Four connected roles')
roles = [('Customer', 'Browses menus, orders, applies codes, pays and tracks, and can ask for “Get Me Anything”'),
         ('Restaurant', 'Hears new orders, accepts or rejects, marks ready, manages menu and prices'),
         ('Courier', 'Goes online, takes an order, picks it up and delivers, sharing live location'),
         ('Administrator', 'Runs stores, zones, fees, users, codes and complaints, and watches daily numbers')]
for i, ((x, w), (t, d)) in enumerate(zip(cols(4, 0.3), roles)):
    card(s, x, 2.0, w, 3.35, SAND)
    text(s, x + 0.35, 2.3, w - 0.7, 0.45, f'0{i + 1}', size=22, bold=True, color=TERRA)
    text(s, x + 0.35, 2.85, w - 0.7, 0.45, t, size=22, bold=True)
    text(s, x + 0.35, 3.4, w - 0.7, 1.8, d, size=16, color=MUTED, spacing=1.15)
banner(s, 5.7, [('One app for the first three roles. ', {}), ('The page you see depends on the account you sign in with.', {'bold': True, 'color': TERRA_LIGHT})])
foot(s, 4)

# 5 ---------------------------------------------------------------- customer
s = new_slide(SAND, 'customer')
head(s, 'Customer experience', 'Everything a customer needs, in one app')
steps = [('EXPLORE', 'Real places, menus with photos and prices'), ('ORDER', 'Full cost and delivery fee before confirming'),
         ('PAY', 'Cash on delivery, wallet balance, promo codes'), ('TRACK', 'The courier moves live on the map'), ('UPDATED', 'A notification at every step')]
for i, ((x, w), (k, d)) in enumerate(zip(cols(5, 0.25), steps)):
    card(s, x, 2.0, w, 3.1)
    box(s, x + 0.3, 2.3, 0.55, 0.55, GOLD, shape=MSO_SHAPE.OVAL)
    text(s, x + 0.3, 2.3, 0.55, 0.55, str(i + 1), size=18, bold=True, color=INK, align=PP_ALIGN.CENTER, anchor=MSO_ANCHOR.MIDDLE)
    text(s, x + 0.3, 3.1, w - 0.6, 0.4, k, size=17, bold=True, color=TERRA)
    text(s, x + 0.3, 3.55, w - 0.6, 1.4, d, size=15, color=MUTED, spacing=1.15)
banner(s, 5.5, [('Arabic and English · works in the browser and on Android · ', {}), ('add to the home screen like an app', {'bold': True, 'color': TERRA_LIGHT})])
foot(s, 5)

# 6 ---------------------------------------------------------------- restaurant
s = new_slide(CLAY, 'restaurant')
head(s, 'Restaurant management', 'Giving local restaurants real tools')
(x1, w1), (x2, w2) = cols(2, 0.35)
card(s, x1, 2.0, w1, 3.45, SAND)
text(s, x1 + 0.4, 2.3, w1 - 0.8, 0.35, 'ON THE RESTAURANT’S PHONE', size=15, bold=True, color=TERRA)
bullets(s, x1 + 0.4, 2.8, w1 - 0.8, 2.6, ['New orders ring, no refresh needed', 'Accept, reject with a reason, mark ready',
                                          'See how long each order has waited', 'Change prices, pause sold-out items', 'Pause all orders when too busy'], size=17, gap=6)
card(s, x2, 2.0, w2, 3.45, SAND, NILE)
text(s, x2 + 0.4, 2.3, w2 - 0.8, 0.35, 'WHAT THEY GAIN', size=15, bold=True, color=NILE)
bullets(s, x2 + 0.4, 2.8, w2 - 0.8, 2.6, ['New customers in their own town', 'Clear written orders, fewer mistakes',
                                          'A courier network they don’t have to hire', 'Works on any phone or tablet'], size=17, gap=6)
banner(s, 5.75, [('On VELOX now: ', {'bold': True, 'color': TERRA_LIGHT}), ('Nasif Restaurant (111 items with photos) · Orz Café · United Café · Point One Café (menus coming)', {})])
foot(s, 6)

# 7 ---------------------------------------------------------------- journey
s = new_slide(SAND, 'journey')
head(s, 'The order journey', 'From the kitchen to the doorstep')
jsteps = [('Order', 'Customer confirms after seeing the full cost', 'PLACED'), ('Accept', 'Restaurant has 15 minutes, or it auto-cancels', 'ACCEPTED'),
          ('Prepare', 'Kitchen cooks, then marks the order ready', 'READY'), ('Pick up', 'A courier takes it and collects it from the shop', 'PICKED_UP'),
          ('Deliver', 'Customer tracks it live until it arrives', 'DELIVERED')]
for i, ((x, w), (t, d, st)) in enumerate(zip(cols(5, 0.25), jsteps)):
    last = i == len(jsteps) - 1
    card(s, x, 2.0, w, 3.2, PAPER, NILE if last else LINE)
    text(s, x + 0.3, 2.25, w - 0.6, 0.4, f'0{i + 1}', size=20, bold=True, color=NILE if last else TERRA)
    text(s, x + 0.3, 2.7, w - 0.6, 0.45, t, size=22, bold=True)
    text(s, x + 0.3, 3.2, w - 0.6, 1.2, d, size=15, color=MUTED, spacing=1.15)
    box(s, x + 0.3, 4.55, w - 0.6, 0.42, NILE_SOFT if last else TERRA_SOFT, radius=0.5)
    text(s, x + 0.3, 4.55, w - 0.6, 0.42, st, size=13, bold=True, color=NILE if last else TERRA, align=PP_ALIGN.CENTER, anchor=MSO_ANCHOR.MIDDLE)
banner(s, 5.55, [('Every step reaches everyone ', {}), ('instantly', {'bold': True, 'color': TERRA_LIGHT}), ('. Each move is allowed only to the right role; an order no courier takes in 40 minutes is cancelled.', {})], h=0.8)
foot(s, 7)

# 8 ---------------------------------------------------------------- admin
s = new_slide(CLAY, 'admin')
head(s, 'Administration dashboard', 'One dashboard to run the whole platform')
caps = ['Stores & menus', 'Delivery zones', 'Delivery fees', 'Users & staff', 'Live orders', 'Discount codes', 'Complaints', 'Daily statistics']
for i, c in enumerate(caps):
    (x, w), y = cols(4, 0.3)[i % 4], 2.0 + (i // 4) * 1.6
    card(s, x, y, w, 1.35, SAND)
    box(s, x + 0.3, y + 0.42, 0.5, 0.5, TERRA_SOFT, shape=MSO_SHAPE.OVAL)
    text(s, x + 0.3, y + 0.42, 0.5, 0.5, str(i + 1), size=15, bold=True, color=TERRA, align=PP_ALIGN.CENTER, anchor=MSO_ANCHOR.MIDDLE)
    text(s, x + 0.95, y, w - 1.15, 1.35, c, size=19, bold=True, anchor=MSO_ANCHOR.MIDDLE)
banner(s, 5.55, [('Staff accounts are created ', {}), ('only by the admin', {'bold': True, 'color': TERRA_LIGHT}), (', and every admin action is logged.', {})])
foot(s, 8)

# 9 ---------------------------------------------------------------- features
s = new_slide(SAND, 'features')
head(s, 'What makes VELOX different', 'Built around how Samannoud lives')
bx, bw = L, 4.55
card(s, bx, 2.0, bw, 4.55, PAPER, NILE)
text(s, bx + 0.4, 2.35, bw - 0.8, 0.35, 'GET ME ANYTHING', size=15, bold=True, color=NILE)
text(s, bx + 0.4, 2.85, bw - 0.8, 1.0, 'Errands from any shop', size=28, bold=True)
text(s, bx + 0.4, 3.95, bw - 0.8, 2.2, 'Describe what you need. The courier buys it from any local shop and brings it.', size=18, color=MUTED, spacing=1.2)
small = [('LANDMARKS', '“Next to the pharmacy” is part of the address, the way people here give directions'),
         ('LOCAL COURIERS', 'Drivers from town who know every street'),
         ('LIVE', 'Map tracking and a notification at every step'),
         ('FOR EVERYONE', 'Arabic and English, phone or browser, cash welcome')]
sw = (W - bw - 0.35 - 0.35) / 2
for i, (k, d) in enumerate(small):
    x = bx + bw + 0.35 + (i // 2) * (sw + 0.35)
    y = 2.0 + (i % 2) * 2.35
    card(s, x, y, sw, 2.2)
    text(s, x + 0.35, y + 0.3, sw - 0.7, 0.35, k, size=15, bold=True, color=TERRA)
    text(s, x + 0.35, y + 0.75, sw - 0.7, 1.35, d, size=17, color=MUTED, spacing=1.15)
foot(s, 9)

# 10 --------------------------------------------------------------- stack
s = new_slide(CLAY, 'stack')
head(s, 'Technology stack', 'Built with modern, proven tools')
rows = [('Layer', 'Technology', 'Why'),
        ('Backend', 'Java 17 · Spring Boot 3 · Maven', 'Course language; layered REST API'),
        ('Database', 'PostgreSQL 16 · JDBC · Flyway', '22 tables, created in order by migrations'),
        ('Mobile & web app', 'Flutter (Dart)', 'One code base for Android and the browser'),
        ('Admin dashboard', 'HTML · CSS · JavaScript', 'Simple, served by the API itself'),
        ('Live updates', 'Server-Sent Events · Firebase (FCM)', 'Instant in the app, push when it’s closed'),
        ('Maps', 'OpenStreetMap', 'Free maps for tracking and addresses'),
        ('Running it', 'Docker · Docker Compose', 'The whole system in one double-click'),
        ('Testing', 'JUnit 5 · Postman · Selenium · JMeter', 'Unit, API, UI and load tests')]
tbl = s.shapes.add_table(len(rows), 3, Inches(L), Inches(1.95), Inches(W), Inches(0.5 * len(rows))).table
for j, wdt in enumerate((2.8, 4.4, W - 7.2)):
    tbl.columns[j].width = Inches(wdt)
for i, row in enumerate(rows):
    tbl.rows[i].height = Inches(0.5)
    for j, val in enumerate(row):
        cell = tbl.cell(i, j)
        cell.fill.solid()
        cell.fill.fore_color.rgb = rgb(INK if i == 0 else (SAND if i % 2 else PAPER))
        cell.margin_left = cell.margin_right = Inches(0.15)
        cell.vertical_anchor = MSO_ANCHOR.MIDDLE
        tf = cell.text_frame
        tf.text = ''
        r = tf.paragraphs[0].add_run()
        r.text = val
        r.font.name, r.font.size = FONT, Pt(15 if i else 14)
        r.font.bold = i == 0 or j == 0
        r.font.color.rgb = rgb(SAND if i == 0 else (TERRA if j == 0 else INK))
foot(s, 10)

# 11 --------------------------------------------------------------- architecture
s = new_slide(SAND, 'architecture')
head(s, 'Architecture', 'How the parts talk to each other')
ox, oy = L, 2.0
for i, (t, d) in enumerate([('Flutter app', 'Customer · Restaurant · Courier'), ('Website', 'Real menus, app download'), ('Admin dashboard', 'HTML · CSS · JavaScript')]):
    y = oy + i * 1.55
    card(s, ox, y, 3.1, 1.3)
    text(s, ox + 0.3, y + 0.2, 2.6, 0.45, t, size=20, bold=True)
    text(s, ox + 0.3, y + 0.68, 2.6, 0.5, d, size=14, color=MUTED)
cx, cw = ox + 4.3, 3.7
card(s, cx, oy, cw, 4.4, TERRA_SOFT, GOLD)
text(s, cx + 0.35, oy + 0.4, cw - 0.7, 0.35, 'SPRING BOOT REST API', size=15, bold=True, color=TERRA)
text(s, cx + 0.35, oy + 0.9, cw - 0.7, 1.0, 'Controller → Service → Repository', size=22, bold=True)
text(s, cx + 0.35, oy + 2.0, cw - 0.7, 1.2, 'auth · security · catalog · order · payment · notification · operation', size=15, color=MUTED, spacing=1.2)
box(s, cx + 0.35, oy + 3.45, cw - 0.7, 0.5, SAND, radius=0.5)
text(s, cx + 0.35, oy + 3.45, cw - 0.7, 0.5, 'Role checks on every request', size=14, bold=True, color=TERRA, align=PP_ALIGN.CENTER, anchor=MSO_ANCHOR.MIDDLE)
rx, rw = cx + cw + 1.25, W - (cx + cw + 1.25 - L)
right = [('PostgreSQL', '22 tables · Flyway V1–V7', 1.6, NILE_SOFT, NILE), ('Firebase', 'Push notifications', 1.25, PAPER, LINE), ('OpenStreetMap', 'Maps', 1.25, PAPER, LINE)]
y = oy
centers = []
for t, d, h, f, ln in right:
    card(s, rx, y, rw, h, f, ln)
    text(s, rx + 0.3, y + 0.2, rw - 0.6, 0.45, t, size=20, bold=True)
    text(s, rx + 0.3, y + 0.68, rw - 0.6, 0.5, d, size=14, color=MUTED)
    centers.append(y + h / 2)
    y += h + 0.15
for i in range(3):
    arrow(s, ox + 3.1, oy + i * 1.55 + 0.65, cx, oy + 2.2, INK)
arrow(s, cx + cw, centers[0], rx, centers[0], NILE, both=True)
arrow(s, cx + cw, centers[1], rx, centers[1], NILE)
text(s, ox + 3.15, oy + 3.6, 1.1, 0.7, 'HTTPS + JSON, live events', size=11, bold=True, color=TERRA, align=PP_ALIGN.CENTER)
text(s, cx + cw + 0.1, centers[0] - 0.45, 1.05, 0.3, 'JDBC', size=12, bold=True, color=NILE, align=PP_ALIGN.CENTER)
foot(s, 11)

# 12 --------------------------------------------------------------- security
s = new_slide(INK, 'security')
head(s, 'Security · Penetration Testing track', 'Secure by design, then attacked by us', dark=True)
(x1, w1), (x2, w2) = cols(2, 0.35)
card(s, x1, 2.0, w1, 4.55, NIGHT_CARD, NIGHT_LINE)
text(s, x1 + 0.4, 2.3, w1 - 0.8, 0.35, 'BUILT IN', size=15, bold=True, color=TERRA_LIGHT)
bullets(s, x1 + 0.4, 2.8, w1 - 0.8, 3.6, ['Passwords: PBKDF2-SHA256, 210,000 rounds + salt', 'Signed sign-in tokens, revoked on password change',
                                          'Role and ownership check on every action (no IDOR)', 'Locked after 5 wrong passwords (HTTP 429)',
                                          'Only parameterized SQL: no injection', 'SpotBugs + FindSecBugs, CodeQL, OWASP ZAP'], size=16, color=SAND, gap=7)
card(s, x2, 2.0, w2, 4.55, NIGHT_CARD, NIGHT_LINE)
text(s, x2 + 0.4, 2.3, w2 - 0.8, 0.35, 'WE ATTACKED OUR OWN SYSTEM', size=15, bold=True, color=TERRA_LIGHT)
bullets(s, x2 + 0.4, 2.8, w2 - 0.8, 3.6, [
    [('Critical: ', {'bold': True, 'color': TERRA_LIGHT}), ('server-link hijack in the app, fixed', {})],
    [('High: ', {'bold': True, 'color': TERRA_LIGHT}), ('cleartext HTTP and token in backups, fixed', {})],
    [('High: ', {'bold': True, 'color': TERRA_LIGHT}), ('unlimited payment-code guessing, fixed', {})],
    [('Java2Shell ', {'bold': True, 'color': TERRA_LIGHT}), ('(CVE-2026-114938): checked and mitigated the day it was published', {})]], size=16, color=SAND, gap=10)
foot(s, 12, dark=True)

# 13 --------------------------------------------------------------- testing
s = new_slide(CLAY, 'testing')
head(s, 'Testing & quality', 'Manual, automated, API and load testing')
stats = [('97/97', 'JUnit tests', True), ('45/45', 'Live end-to-end checks', True), ('30', 'API requests (Postman)', False),
         ('47', 'Manual test cases', False), ('100', 'Users at once (JMeter)', False), ('4/4', 'Selenium UI tests', False)]
for i, (n, lab, ok) in enumerate(stats):
    (x, w), y = cols(3, 0.35)[i % 3], 2.0 + (i // 3) * 1.75
    card(s, x, y, w, 1.5, SAND, NILE if ok else LINE)
    text(s, x + 0.35, y + 0.15, w - 0.7, 0.85, n, size=44, bold=True, color=NILE if ok else TERRA)
    text(s, x + 0.35, y + 0.98, w - 0.7, 0.4, lab, size=17, bold=True)
banner(s, 5.55, [('Found by our own tests: ', {'bold': True, 'color': TERRA_LIGHT}), ('slow responses with a remote database, and one test that wasn’t isolated. Both documented.', {})], h=0.8)
foot(s, 13)

# 14 --------------------------------------------------------------- demo
s = new_slide(GOLD, 'demo')
text(s, L, 1.7, W, 0.35, 'LIVE DEMO · 5 MINUTES', size=15, bold=True, color='3D2A00')
text(s, L, 2.15, W, 1.2, 'Let’s place an order', size=66, bold=True, color=INK)
flow = ['Customer orders', 'Restaurant accepts', 'Courier delivers', 'Admin sees it', 'Security: 401 · 403']
pw, gap = 1.95, 0.52
for i, f in enumerate(flow):
    x = L + i * (pw + gap)
    box(s, x, 3.95, pw, 0.75, INK, radius=0.5)
    text(s, x, 3.95, pw, 0.75, f, size=15, bold=True, color=GOLD, align=PP_ALIGN.CENTER, anchor=MSO_ANCHOR.MIDDLE)
    if i < len(flow) - 1:
        arrow(s, x + pw + 0.08, 4.325, x + pw + gap - 0.08, 4.325, INK, width=2.5)
foot(s, 14)

# 15 --------------------------------------------------------------- team
s = new_slide(SAND, 'team')
head(s, 'Meet the team', 'The people behind VELOX')
team = [('N', 'Nabeeh', 'Team leader', 'Accounts, security, database, deployment, app foundation'),
        ('H', 'Hoda', None, 'Restaurants and menus, UI design, documentation'),
        ('A', 'Abdelrhman', None, 'Orders, live tracking, website and admin dashboard'),
        ('F', 'Fady', None, 'Payments, notifications, customer screens'),
        ('R', 'Raheek', None, 'Restaurant and courier operations, testing lead')]
for (x, w), (a, n, tag, d) in zip(cols(5, 0.25), team):
    card(s, x, 2.0, w, 4.0)
    box(s, x + 0.3, 2.3, 0.8, 0.8, INK, shape=MSO_SHAPE.OVAL)
    text(s, x + 0.3, 2.3, 0.8, 0.8, a, size=26, bold=True, color=GOLD, align=PP_ALIGN.CENTER, anchor=MSO_ANCHOR.MIDDLE)
    text(s, x + 0.3, 3.3, w - 0.6, 0.45, n, size=21, bold=True)
    yy = 3.8
    if tag:
        box(s, x + 0.3, yy, 1.45, 0.36, TERRA_SOFT, radius=0.5)
        text(s, x + 0.3, yy, 1.45, 0.36, tag, size=12, bold=True, color=TERRA, align=PP_ALIGN.CENTER, anchor=MSO_ANCHOR.MIDDLE)
        yy += 0.5
    text(s, x + 0.3, yy, w - 0.6, 1.7, d, size=15, color=MUTED, spacing=1.15)
foot(s, 15)

# 16 --------------------------------------------------------------- thanks
s = new_slide(INK, 'thanks')
box(s, 9.0, -2.1, 5.7, 5.7, None, GOLD, MSO_SHAPE.OVAL, line_w=2)
text(s, L, 1.6, 9, 0.35, 'VELOX · MADE IN SAMANNOUD', size=14, bold=True, color=TERRA_LIGHT)
text(s, L, 2.05, 10, 1.4, 'Thank you', size=88, bold=True, color=SAND)
text(s, L, 3.6, 9, 0.6, 'Questions & discussion', size=32, color=TERRA_LIGHT)
text(s, L, 6.55, 6, 0.4, 'Connecting local restaurants. Simplifying delivery.', size=16, color=NIGHT_TEXT)
text(s, 6.6, 6.55, 5.98, 0.4, 'github.com/abdelrhman19122007/VELOX', size=16, color=NIGHT_TEXT, align=PP_ALIGN.RIGHT)

prs.core_properties.title = 'VELOX — Graduation Project'
prs.core_properties.author = 'Team VELOX'
OUT.parent.mkdir(parents=True, exist_ok=True)
prs.save(OUT)
print('saved', OUT, len(prs.slides), 'slides')
