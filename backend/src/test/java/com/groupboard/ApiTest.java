package com.groupboard;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:t") @AutoConfigureMockMvc
class ApiTest {
    @Autowired MockMvc mvc;
    @Test void groupsNeedLogin() throws Exception { mvc.perform(get("/api/groups")).andExpect(status().isUnauthorized()); }
    @Test void badPasswordRejected() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"admin\",\"password\":\"x\"}")).andExpect(status().isUnauthorized());
    }
    @Test void loginWorks() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"admin\",\"password\":\"change-me-now\"}")).andExpect(status().isOk());
    }
}
