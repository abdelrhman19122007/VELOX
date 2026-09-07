/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.app.enums;

/**
 *
 * @author 3bdelr7man
 */

// enum يحدد الحالات المختلفة للشكوى
public enum ComplaintStatus {
    // الحلات المتاحه و كل حاله معها الوصف بتاعها
    PENDING("Pending"),
    IN_REVIEW("In Review"),
    RESOLVED("Resolved");

    // متغير بيحفظ نص الوصف الخاص بكل حالة
    private final String description;

    // كونستركتر بيحط الوصف للحله اول ما تتعرف
    ComplaintStatus(String description) {
        this.description = description;
    }

    // داله بترجعلك نص الوصف عشان تتعوض بيه او تعرض للمستخدم
    public String getDescription() {
        return description;
    }
}