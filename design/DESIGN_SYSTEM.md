# VELOX design system — "Nile Terracotta"

**Owner:** Hoda (@hodayasser511-stack) — UI/UX
**Source:** the team's Stitch project *Velox Delivery Platform UI*. These tokens are implemented in
`mobile/lib/core/theme.dart` (Flutter) and `web/css/tokens.css` (website and dashboard). Change them in both places.

## Principles
1. **Arabic first.** Right-to-left by default, Egyptian Arabic copy, English as a switch.
2. **Made for Samannoud.** Village picker, landmark addresses, tuk-tuk items, cash with change, "bring me anything".
3. **Easy for everyone.** Big touch targets (≥ 48 px), body text ≥ 14 px, strong contrast, an icon *and* a label on actions.
4. **Honest.** No invented numbers, no fake reviews, no stock photos pretending to be a real shop. A store without a
   photo shows its type icon on a warm gradient.
5. **Same look everywhere.** Phone app, website and dashboards share colours, type and status colours.

## Colour tokens
| Token | Light | Dark | Use |
|---|---|---|---|
| primary (terracotta) | `#9C3D10` | `#FFB693` | main buttons, prices, active tab |
| on-primary | `#FFFFFF` | `#5A1B00` | text on primary |
| primary-container | `#FFDBCC` | `#7A2E06` | selected chips, highlights |
| secondary (Nile green) | `#1F6F4A` | `#7FD9A8` | "open", success, discount, delivered |
| secondary-container | `#B3F1CC` | `#005234` | errand card, success badges |
| tertiary (amber) | `#B26A00` | — | waiting for payment, cash to collect, schedule |
| danger | `#BA1A1A` | `#FFB4AB` | errors, reject, cancel |
| background (sand) | `#FFF8F5` | `#1A110D` | page background |
| surface | `#FFFFFF` | `#231915` | cards |
| text / text-2 | `#231914` / `#53433C` | `#F1DFD8` / `#D8C2BA` | body / secondary text |
| outline / line | `#85736B` / `#E6D3CB` | `#A08D85` / `#53433C` | borders |

Contrast (computed): primary on white 6.8:1, text on sand 16.4:1, secondary text on sand 8.9:1, Nile green on white 6.1:1 — all pass WCAG AA for normal text.

## Order status colours (all apps)
| Status | Arabic | Colour |
|---|---|---|
| PENDING_PAYMENT | مستني الدفع | amber |
| PLACED | اتطلب / جديد | terracotta |
| ACCEPTED, READY | بيتجهز / جاهز | blue `#2F6FB0` |
| PICKED_UP | المندوب في الطريق | purple `#7A4CC2` |
| DELIVERED | وصل | Nile green |
| REJECTED, CANCELLED, RETURNED | اترفض / اتلغى / مرتجع | danger |

## Typography
- **Cairo** (Google Fonts) for Arabic and English.
- Sizes: page title 22–26 / 900 · section 18–20 / 800 · body 14.5–15 / 400 · small 12.5–13 · button 15–16 / 700.
- Line height 1.45–1.55 (Arabic needs room).

## Shape and spacing
- Radius: 14 px (inputs, buttons) · 18 px (cards, sheets) · pill for chips and badges.
- Spacing scale: 4 · 8 · 12 · 16 · 24. Page side margin 16 px on phones.
- Elevation: flat cards with a 1 px border; one soft shadow on the web.

## Components
| Component | Notes |
|---|---|
| Bottom navigation (customer) | Home · Orders · Cart · Account, with badges for active orders, cart count and unread notifications |
| Store card | photo or type icon, open/closed badge, name, rating (or "جديد"), type, ETA, delivery fee, hours in 12-hour ص/م |
| Product tile | name, description (2 lines), price in terracotta, 🛺 for large items, add button turns into the quantity |
| Add sheet | note field ("من غير بصل"), quantity stepper, button with the line price |
| Price breakdown | items · delivery (or "توصيل مجاني") · discount in green · total in bold |
| Status stepper | five steps with times; failed orders show the reason instead |
| Store board | columns New / Preparing / Ready / Done, sound and a toast for new orders, reject needs a reason |
| Empty state | round icon, one-line title, optional action |
| Error message | translated server code, never a stack trace |

## Screens
Customer: welcome/sign-in · home · search · store · cart · checkout · errand · payment (Vodafone Cash code / Fawry
reference) · tracking with map · orders · wallet · addresses · complaints · notifications · settings.
Store: orders board · menu (availability, price, accepting switch) · today and reviews.
Driver: online and vehicle · offers · current delivery (pick up / deliver, navigation, cash and change) · earnings.
Admin (web): numbers · orders · stores and menus (photo upload, CSV import) · areas · promo codes · users · complaints.

## To do (design)
- Real photos of the three partner restaurants and their dishes (taken on site, with permission).
- Final app icon and Play Store screenshots / feature graphic.
- Usability test with 5 people from Samannoud (see `design/USABILITY_TEST.md`).
