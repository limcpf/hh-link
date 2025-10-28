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

class FetchAndSaveJsonTaskletTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void independentDomain_writesArrayOrObject(@org.junit.jupiter.api.io.TempDir Path tmp) throws Exception {
        MockEnvironment env = new MockEnvironment()
                .withProperty("output.dir", tmp.toString())
                .withProperty("endpoints.user.list-url", "http://mock/users");
        AppSettings settings = new AppSettings(env);

        RestTemplate rt = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(rt);

        // First run: array response
        server.expect(requestTo("http://mock/users"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[{\"a\":1},{\"b\":2}]", MediaType.APPLICATION_JSON));

        FetchAndSaveJsonTasklet t = new FetchAndSaveJsonTasklet(Domain.USER, settings, rt);
        StepExecution se = MetaDataInstanceFactory.createStepExecution();
        se.getJobExecution().getExecutionContext().putString("jwt.user", "DUMMY");
        StepContribution sc = new StepContribution(se);
        ChunkContext cc = new ChunkContext(new StepContext(se));
        t.execute(sc, cc);
        server.verify();

        Path out = tmp.resolve("users.json");
        JsonNode arr = mapper.readTree(Files.readString(out));
        assertThat(arr.isArray()).isTrue();
        assertThat(arr.size()).isEqualTo(2);

        // Second run: object response (overwrite=true to allow overwrite)
        env.setProperty("output.overwrite", "true");
        server.reset();
        server.expect(requestTo("http://mock/users"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"x\":1}", MediaType.APPLICATION_JSON));
        t.execute(new StepContribution(se), new ChunkContext(new StepContext(se)));
        server.verify();
        arr = mapper.readTree(Files.readString(out));
        assertThat(arr.size()).isEqualTo(1);
        assertThat(arr.get(0).get("x").asInt()).isEqualTo(1);
    }

    @Test
    void dependentDomain_iteratesUsersAndFlattens(@org.junit.jupiter.api.io.TempDir Path tmp) throws Exception {
        // Prepare users.json for dependency
        Path users = tmp.resolve("users.json");
        Files.writeString(users, "[{\"userId\":\"U1\"},{\"userId\":\"U2\"}]");

        MockEnvironment env = new MockEnvironment()
                .withProperty("output.dir", tmp.toString())
                .withProperty("fetch.max-threads", "1")
                .withProperty("endpoints.attend.by-user-url-template", "http://mock/attend?userId={userId}");
        AppSettings settings = new AppSettings(env);

        RestTemplate rt = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(rt);
        server.expect(requestTo("http://mock/attend?userId=U1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"u\":\"U1\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://mock/attend?userId=U2"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[{\"u\":\"U2a\"},{\"u\":\"U2b\"}]", MediaType.APPLICATION_JSON));

        FetchAndSaveJsonTasklet t = new FetchAndSaveJsonTasklet(Domain.ATTEND, settings, rt);
        StepExecution se = MetaDataInstanceFactory.createStepExecution();
        se.getJobExecution().getExecutionContext().putString("jwt.attend", "DUMMY");
        StepContribution sc = new StepContribution(se);
        ChunkContext cc = new ChunkContext(new StepContext(se));
        t.execute(sc, cc);
        server.verify();

        Path out = tmp.resolve("attends.json");
        JsonNode arr = mapper.readTree(Files.readString(out));
        assertThat(arr.isArray()).isTrue();
        assertThat(arr.size()).isEqualTo(3);
    }
}

