package com.app.util;

import java.util.HashSet;
import java.util.Set;

/**
 * فئة إدارة أكواد الخصم والـ Promo Codes الخاصة بالنظام.
 * مسؤولة عن تخزين الأكواد الصحيحة والتحقق من صلاحيتها.
 */
public class PromoCodeManager {
    
    // مجموعة (Set) لحفظ الأكواد الصحيحة المتاحة للاستخدام، ومميزة لعدم تكرار الأكواد.
    private static final Set<String> validCodes = new HashSet<>();

    // كتلة تهيئة ثابتة (Static Block) لتعبئة الأكواد المتاحة أول تشغيل الفئة.
    static {
        validCodes.add("VELOX10");     // كود خصم بنسبة 10%
        validCodes.add("FIRSTORDER");  // كود خصم لأول طلب للعميل
        validCodes.add("FREESHIP");    // كود توصيل مجاني
    }

    /**
     * دالة للتحقق مما إذا كان كود الخصم المدخل صحيحاً أم لا.
     * تقوم بإزالة الفراغات الزائدة وتحويل الأحرف إلى حروف كبيرة لضمان صحة المقارنة.
     * 
     * @param code كود الخصم المراد التحقق منه
     * @return true إذا كان الكود موجوداً وصحيحاً، و false خلاف ذلك
     */
    public static boolean isValidCode(String code) {
        return code != null && validCodes.contains(code.trim().toUpperCase());
    }
}
