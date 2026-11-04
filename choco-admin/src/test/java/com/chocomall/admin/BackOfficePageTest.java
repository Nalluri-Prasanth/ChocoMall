package com.chocomall.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

class BackOfficePageTest extends AdminTestSupport {

    @Test
    void servesTheDashboardShellButProtectsTheApi() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk());
        mvc.perform(get("/index.html")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Back Office")));
        mvc.perform(get("/app/main.js")).andExpect(status().isOk());
        mvc.perform(get("/shared/refinement.css")).andExpect(status().isOk());
        mvc.perform(get("/app/admin-refinement.css")).andExpect(status().isOk());
        mvc.perform(get("/product/list")).andExpect(status().isUnauthorized());
    }
}
