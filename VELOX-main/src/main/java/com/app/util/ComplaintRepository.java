/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.app.util;

/**
 *
 * @author 3bdelr7man
 */


import com.app.model.order.Complaint;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;

// كلاس مسئول عن حفظ الشكاوى في ملف نصي خارجي
public class ComplaintRepository {
    private static final String FILE_NAME = "complaints.txt";

    // دالة الكتابة وحفظ الشكوى في الملف
    public static void saveComplaintToFile(Complaint complaint) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(FILE_NAME, true))) {
            writer.println(complaint.toFileString());
            System.out.println("--> [System] Complaint saved permanently to " + FILE_NAME);
        } catch (IOException e) {
            System.out.println("--> Error saving complaint to file: " + e.getMessage());
        }
    }
}