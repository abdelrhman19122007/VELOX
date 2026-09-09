/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.app.model.order;

/**
 *
 * @author 3bdelr7man
 */


import java.util.Scanner;


public class Review {
    private int rating;
    private String comment;

    
    public Review(int rating, String comment) {
        this.rating = rating;
        this.comment = comment;
    }

    public void displayReview() {
        System.out.println("\n--- Service Review ---");
        System.out.println("Rating: " + rating + " / 5 Stars ");
        if (comment != null && !comment.trim().isEmpty()) {
            System.out.println("Feedback: " + comment);
        }
    }
}