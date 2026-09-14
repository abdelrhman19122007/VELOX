/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.app.enums;

/**
 *
 * @author 3bdelr7man
 */

public enum ComplaintStatus {
    // الحالات المتاحه وكل حاله معاها الوصف بتاعها
    PENDING("Pending"),
    IN_REVIEW("In Review"),
    RESOLVED("Resolved");

    // متغير بيحفظ نص الوصف الخاص بكل حالة
    private final String description;

    // كونسركتر بيحط الوصف للحالة أول ما تتعرف
    ComplaintStatus(String description) {
        this.description = description;
    }
    // دالة بترجعلك نص الوصف عشان تعوض بيه أو تعرضه للمستخدم

    public String getDescription() {
        return description;
    }
}