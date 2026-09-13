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
    PENDING("Pending"),
    IN_REVIEW("In Review"),
    RESOLVED("Resolved");

    private final String description;

    ComplaintStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}