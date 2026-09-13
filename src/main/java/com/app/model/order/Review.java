/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.app.model.order;

/**
 *
 * @author 3bdelr7man
 */


public class Review {
    private final int rating;
    private final String comment;

    
    public Review(int rating, String comment) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }
        this.rating = rating;
        this.comment = comment == null ? "" : comment.trim();
    }

    public int getRating() { return rating; }
    public String getComment() { return comment; }

    public void displayReview() {
        System.out.println("\n--- Service Review ---");
        System.out.println("Rating: " + rating + " / 5 Stars ");
        if (comment != null && !comment.trim().isEmpty()) {
            System.out.println("Feedback: " + comment);
        }
    }
}