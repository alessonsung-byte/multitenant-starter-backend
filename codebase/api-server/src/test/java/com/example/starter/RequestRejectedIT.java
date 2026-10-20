package com.example.starter;

import com.example.starter.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <code>RequestRejectedIT</code> 被 Spring Security 防火牆拒絕的網址回 400，不是 401「尚未登入」
 * （401 會讓前端把使用者登出）
 */
class RequestRejectedIT extends IntegrationTestBase {

    @Test
    void encodedPercentInPathIsRejectedWith400() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(MockMvcRequestBuilders.get(URI.create("/ping%25")))
                .andReturn().getResponse();

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getContentAsString(StandardCharsets.UTF_8)).contains("ERROR_INVALID_ARGUMENT");
    }

    @Test
    void rejectedUrlWithValidTokenIsStill400NotUnauthorized() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(MockMvcRequestBuilders
                        .get(URI.create("/api/customers/abc%25def"))
                        .header("Authorization", "Bearer " + adminToken()))
                .andReturn().getResponse();

        assertThat(response.getStatus()).isEqualTo(400);
    }
}
