# ازاي ترفع جزءك — فادي (@fadyza15-collab)

**جزءك:** موديول الدفع والإشعارات (Java) + شاشات تطبيق Flutter (mobile/lib/screens، widgets، test)

الفولدر فيه الملفات بنفس أماكنها في الريبو بالظبط. **متغيرش الأماكن.**
ترتيبك في الرفع: **3** من ٤ (بعد ما الـ PR اللي قبلك يتعمله Merge).

## الرفع من المتصفح
1. افتح الريبو ← اختار الفرع **`feature/payments`** ← لو مكتوب "behind main" دوس **Sync / Update branch**.
2. **Add file ← Upload files** ← اسحب كل الفولدرات اللي هنا مع بعض.
3. Commit: `feat(payments): Vodafone Cash, Fawry, wallet, promos, notifications + app screens`
4. افتح **Pull Request** على `main` وReviewer **nabosallem-svg**، واستنى العلامة الخضرا ✅.

## بعد الرفع
- اقرا backend/src/main/java/com/velox/payment/MODULE_GUIDE.md
- شغّل flutter test و flutter analyze بعد ما نبيه يرفع أساس التطبيق
- جرّب البينتست اليدوي في security/payments.md
- جهّز رفع التطبيق على Google Play (حساب مطوّر + ١٢ مختبر)
