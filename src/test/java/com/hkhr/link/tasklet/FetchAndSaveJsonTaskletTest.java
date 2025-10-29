package com.hkhr.link.tasklet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hkhr.link.config.AppSettings;
import com.hkhr.link.domain.Domain;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.test.MetaDataInstanceFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;

class FetchAndSaveJsonTaskletTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void independentDomain_writesArrayOrObject(@org.junit.jupiter.api.io.TempDir Path tmp) throws Exception {
        MockEnvironment env = new MockEnvironment()
                .withProperty("output.dir", tmp.toString())
                .withProperty("endpoints.user.list-url", "http://mock/users")
                .withProperty("endpoints.user.request-payload", "{\"date\":\"{date}\"}");
        AppSettings settings = new AppSettings(env);

        RestTemplate rt = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(rt);

        // First run: array response
        String date = new java.text.SimpleDateFormat("yyyyMMdd").format(new java.util.Date());
        server.expect(requestTo("http://mock/users"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string("{\"date\":\"" + date + "\"}"))
                .andRespond(withSuccess("[{\"a\":1},{\"b\":2}]", MediaType.APPLICATION_JSON));

        FetchAndSaveJsonTasklet t = new FetchAndSaveJsonTasklet(Domain.USER, settings, rt);
        StepExecution se = MetaDataInstanceFactory.createStepExecution();
        se.getJobExecution().getExecutionContext().putString("jwt.user", "DUMMY");
        StepContribution sc = new StepContribution(se);
        ChunkContext cc = new ChunkContext(new StepContext(se));
        t.execute(sc, cc);
        server.verify();

        // date 변수는 위에서 계산된 값을 재사용합니다.
        Path out = tmp.resolve("users-" + date + ".json");
        JsonNode arr = mapper.readTree(new String(Files.readAllBytes(out), java.nio.charset.StandardCharsets.UTF_8));
        assertThat(arr.isArray()).isTrue();
        assertThat(arr.size()).isEqualTo(2);

        // Second run: object response (overwrite=true to allow overwrite)
        env.setProperty("output.overwrite", "true");
        server.reset();
        server.expect(requestTo("http://mock/users"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string("{\"date\":\"" + date + "\"}"))
                .andRespond(withSuccess("{\"x\":1}", MediaType.APPLICATION_JSON));
        t.execute(new StepContribution(se), new ChunkContext(new StepContext(se)));
        server.verify();
        arr = mapper.readTree(new String(Files.readAllBytes(out), java.nio.charset.StandardCharsets.UTF_8));
        assertThat(arr.size()).isEqualTo(1);
        assertThat(arr.get(0).get("x").asInt()).isEqualTo(1);
    }

    @Test
    void dependentDomain_iteratesUsersAndFlattens(@org.junit.jupiter.api.io.TempDir Path tmp) throws Exception {
        // Prepare users.json for dependency
        String d = new java.text.SimpleDateFormat("yyyyMMdd").format(new java.util.Date());
        Path users = tmp.resolve("users-" + d + ".json");
        Files.write(users, "[{\"userId\":\"U1\"},{\"userId\":\"U2\"}]".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        MockEnvironment env = new MockEnvironment()
                .withProperty("output.dir", tmp.toString())
                .withProperty("fetch.max-threads", "1")
                .withProperty("endpoints.attend.by-user-url-template", "http://mock/attend")
                .withProperty("endpoints.attend.by-user-payload-template", "{\"userId\":\"{userId}\",\"date\":\"{date}\"}");
        AppSettings settings = new AppSettings(env);

        RestTemplate rt = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(rt);
        server.expect(requestTo("http://mock/attend"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string("{\"userId\":\"U1\",\"date\":\"" + d + "\"}"))
                .andRespond(withSuccess("{\"u\":\"U1\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://mock/attend"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string("{\"userId\":\"U2\",\"date\":\"" + d + "\"}"))
                .andRespond(withSuccess("[{\"u\":\"U2a\"},{\"u\":\"U2b\"}]", MediaType.APPLICATION_JSON));

        FetchAndSaveJsonTasklet t = new FetchAndSaveJsonTasklet(Domain.ATTEND, settings, rt);
        StepExecution se = MetaDataInstanceFactory.createStepExecution();
        se.getJobExecution().getExecutionContext().putString("jwt.attend", "DUMMY");
        StepContribution sc = new StepContribution(se);
        ChunkContext cc = new ChunkContext(new StepContext(se));
        t.execute(sc, cc);
        server.verify();

        Path out2 = tmp.resolve("attends-" + d + ".json");
        JsonNode arr2 = mapper.readTree(new String(Files.readAllBytes(out2), java.nio.charset.StandardCharsets.UTF_8));
        assertThat(arr2.isArray()).isTrue();
        assertThat(arr2.size()).isEqualTo(3);
    }
}
