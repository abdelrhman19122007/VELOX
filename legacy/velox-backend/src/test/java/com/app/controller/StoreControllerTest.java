package com.app.controller;

import com.app.util.DatabaseConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Store directory endpoint with a mocked JDBC layer.
 */
class StoreControllerTest {

    private MockedStatic<DatabaseConnection> db;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() throws Exception {
        db = Mockito.mockStatic(DatabaseConnection.class);
        Connection conn = mock(Connection.class);
        db.when(DatabaseConnection::getConnection).thenReturn(conn);
        PreparedStatement ps = mock(PreparedStatement.class);
        when(conn.prepareStatement(anyString())).thenReturn(ps);
        ResultSet rs = mock(ResultSet.class);
        when(rs.next()).thenReturn(true, false);
        when(rs.getInt("id")).thenReturn(1);
        when(rs.getString("name")).thenReturn("Abou Tarek & Shabrawy");
        when(rs.getString("name_ar")).thenReturn("أبو طارق");
        when(rs.getString("store_type")).thenReturn("RESTAURANT");
        when(rs.getInt("products")).thenReturn(6);
        when(rs.getInt("reviews")).thenReturn(2);
        when(rs.getObject("rating")).thenReturn(4.5);
        when(ps.executeQuery()).thenReturn(rs);
        mockMvc = MockMvcBuilders.standaloneSetup(new StoreController()).build();
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    @Test
    void listReturnsStoresWithRatings() throws Exception {
        mockMvc.perform(get("/api/stores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Abou Tarek & Shabrawy")))
                .andExpect(jsonPath("$[0].rating", is(4.5)))
                .andExpect(jsonPath("$[0].productsCount", is(6)));
    }
}
