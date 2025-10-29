package com.hkhr.link.util;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class HttpJsonTest {

    @Test
    void headers_buildCorrectly() {
        HttpHeaders h1 = HttpJson.authJsonHeaders("TKN");
        assertThat(h1.getFirst("Authorization")).isEqualTo("Bearer TKN");
        assertThat(h1.getContentType()).isEqualTo(MediaType.APPLICATION_JSON);

        HttpHeaders h2 = HttpJson.serviceKeyHeaders("KEY");
        assertThat(h2.getFirst("servicekey")).isEqualTo("KEY");
        assertThat(h2.getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
    }

    @Test
    void post_sendsBodyAndHeaders() {
        RestTemplate rt = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(rt);
        server.expect(requestTo("http://mock/api"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string("{\"x\":1}"))
                .andExpect(header("Authorization", "Bearer T"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        HttpHeaders h = HttpJson.authJsonHeaders("T");
        HttpJson.post(rt, "http://mock/api", h, "{\"x\":1}");
        server.verify();
    }
}

