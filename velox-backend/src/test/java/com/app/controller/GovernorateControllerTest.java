package com.app.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * KAN-126: controller tests for the Governorate shipping API (no DB needed).
 */
class GovernorateControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new GovernorateController()).build();
    }

    @Test
    void getAllReturnsTwentySevenGovernorates() throws Exception {
        mockMvc.perform(get("/api/governorates").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(27)))
                .andExpect(jsonPath("$[0].name", is("CAIRO")))
                .andExpect(jsonPath("$[0].shippingPrice", is(20.0)));
    }

    @Test
    void getByNameIsCaseInsensitive() throws Exception {
        mockMvc.perform(get("/api/governorates/cairo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("CAIRO")))
                .andExpect(jsonPath("$.code", is(1)))
                .andExpect(jsonPath("$.deliveryDays", is(1)));
    }

    @Test
    void getByNameUnknownReturns404() throws Exception {
        mockMvc.perform(get("/api/governorates/ATLANTIS"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getShippingPriceReturnsPricePayload() throws Exception {
        mockMvc.perform(get("/api/governorates/GIZA/shipping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.governorate", is("GIZA")))
                .andExpect(jsonPath("$.shippingPrice", is(25.0)))
                .andExpect(jsonPath("$.deliveryDays", is(1)));
    }

    @Test
    void getShippingPriceUnknownReturns404() throws Exception {
        mockMvc.perform(get("/api/governorates/NOWHERE/shipping"))
                .andExpect(status().isNotFound());
    }
}
