package com.hkhr.link.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@SpringBootTest(properties = {
        "auth.user.service-key=TEST_KEY",
        "auth.user.token-url=http://mock/token"
})
@ActiveProfiles("test")
class JwtServiceTest {

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    JwtService jwtService;

    MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        server = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    void fetchToken_supportsVariousResponseKeys() {
        String[] payloads = new String[]{
                "{\"token\":\"AAA\"}",
                "{\"jwt\":\"BBB\"}",
                "{\"access_token\":\"CCC\"}",
                "{\"data\":{\"token\":\"DDD\"}}"
        };
        String[] expected = new String[]{"AAA", "BBB", "CCC", "DDD"};

        for (int i = 0; i < payloads.length; i++) {
            server.reset();
            server.expect(requestTo("http://mock/token"))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(header("servicekey", "TEST_KEY"))
                    .andRespond(withSuccess(payloads[i], MediaType.APPLICATION_JSON));

            String token = jwtService.fetchToken("user");
            assertThat(token).isEqualTo(expected[i]);
            server.verify();
        }
    }

    @Test
    void fetchTokenWithRaw_returnsTokenAndRawBody() {
        String body = "{\"token\":\"ZZZ\"}";
        server.expect(requestTo("http://mock/token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("servicekey", "TEST_KEY"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        JwtService.JwtFetchResult r = jwtService.fetchTokenWithRaw("user");
        assertThat(r.token).isEqualTo("ZZZ");
        assertThat(r.rawBody).isEqualTo(body);
        server.verify();
    }

    @Test
    void fetchToken_throwsWhenTokenMissing() {
        String bad = "{\"no\":\"token\"}";
        server.expect(requestTo("http://mock/token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("servicekey", "TEST_KEY"))
                .andRespond(withSuccess(bad, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> jwtService.fetchToken("user"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Token not found");
        server.verify();
    }

    @Test
    void fetchToken_wrapsHttpErrors() {
        server.expect(requestTo("http://mock/token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("servicekey", "TEST_KEY"))
                .andRespond(withBadRequest());

        assertThatThrownBy(() -> jwtService.fetchToken("user"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Token request failed");
        server.verify();
    }
}
