/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author 3bdelr7man
 */
package com.app.service;

import com.app.exception.ReturnPolicyException;
import com.app.model.order.Order;
import com.app.util.Response;
import org.junit.Test;
import static org.junit.Assert.*;

public class ReturnServiceTest {

    @Test
    public void testProcessReturn_Success() throws ReturnPolicyException {
        // Arrange
        ReturnService returnService = new ReturnService();
        Order order = new Order("ORD-101", "Damietta", "01000000000");
        
        // Act
        Response<Double> response = returnService.processReturn(order);

        // Assert
        assertTrue(order.isReturned());
        assertNotNull(response);
        assertEquals("Return processed successfully. Amount refunded: 0.0 EGP", response.getMessage());
    }
}
