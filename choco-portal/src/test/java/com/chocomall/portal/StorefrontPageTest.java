package com.chocomall.portal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

class StorefrontPageTest extends PortalTestSupport {

    @Test
    void servesTheStorefrontWithoutSigningIn() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk());
        mvc.perform(get("/index.html")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("ChocoMall")));
        mvc.perform(get("/app/main.js")).andExpect(status().isOk());
        mvc.perform(get("/shared/ui.css")).andExpect(status().isOk());
        mvc.perform(get("/shared/refinement.css")).andExpect(status().isOk());
        mvc.perform(get("/app/atelier.js")).andExpect(status().isOk());
        mvc.perform(get("/app/atelier.css")).andExpect(status().isOk());
        mvc.perform(get("/app/pages/discover.js")).andExpect(status().isOk());
    }
}
