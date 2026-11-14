package com.enis.sectio.web;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SpaRoutingControllerTest {
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new SpaRoutingController()).build();

    @Test
    void clientRoutesStillForwardToIndex() throws Exception {
        mvc.perform(get("/login")).andExpect(forwardedUrl("/index.html"));
        mvc.perform(get("/projects/10")).andExpect(forwardedUrl("/index.html"));
    }

    @Test
    void logoRequestsAreNotCapturedBySpaController() throws Exception {
        // Without a resource handler, static paths must remain unmapped rather than return HTML.
        for (String file : new String[]{"01_Ana_Renkli_Yatay.png", "03_Beyaz_Negatif_Yatay.png", "04_Sadece_S_Amblemi.png"}) {
            mvc.perform(get("/brand/" + file)).andExpect(status().isNotFound());
        }
    }
}
