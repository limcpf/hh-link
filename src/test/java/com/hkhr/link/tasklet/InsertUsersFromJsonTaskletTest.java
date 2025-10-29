package com.hkhr.link.tasklet;

import com.hkhr.link.config.AppSettings;
import com.hkhr.link.db.UserMapper;
import com.hkhr.link.db.UserRow;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.test.MetaDataInstanceFactory;
import org.springframework.mock.env.MockEnvironment;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

class InsertUsersFromJsonTaskletTest {

    @Test
    void parsesVariousFieldNames_andFlushesByBatchSize() throws Exception {
        // Prepare temp dir and input JSON
        Path tmp = Files.createTempDirectory("users-json-test");
        String date = new java.text.SimpleDateFormat("yyyyMMdd").format(new java.util.Date());
        Path input = tmp.resolve("users-" + date + ".json");
        String json = "[\n" +
                " {\"empId\":\"E1\",\"empNm\":\"N1\"},\n" +
                " {\"EMP_ID\":\"E2\",\"EMP_NM\":\"N2\"},\n" +
                " {\"userId\":\"E3\",\"name\":\"N3\"},\n" +
                " {\"id\":\"E4\",\"userName\":\"N4\"},\n" +
                " {\"noId\":\"skip\"}\n" +
                "]";
        Files.write(input, json.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        // Settings: output.dir to temp, db.batch-size=2
        MockEnvironment env = new MockEnvironment()
                .withProperty("output.dir", tmp.toString())
                .withProperty("db.batch-size", "2");
        AppSettings settings = new AppSettings(env);

        // Mock mapper to capture batches
        UserMapper mapper = Mockito.mock(UserMapper.class);
        List<List<UserRow>> seen = new ArrayList<>();
        when(mapper.insertAll(anyList())).thenAnswer(inv -> {
            @SuppressWarnings("unchecked")
            List<UserRow> l = (List<UserRow>) inv.getArgument(0);
            seen.add(new ArrayList<>(l)); // copy to avoid later clear()
            return l.size();
        });

        InsertUsersFromJsonTasklet tasklet = new InsertUsersFromJsonTasklet(settings, mapper);

        // Build minimal Step/Chunk context
        StepExecution stepExecution = MetaDataInstanceFactory.createStepExecution();
        StepContribution contribution = new StepContribution(stepExecution);
        ChunkContext chunkContext = new ChunkContext(new StepContext(stepExecution));

        tasklet.execute(contribution, chunkContext);

        // Verify batching: with size=2 and 4 valid rows -> 3 batches (2,2,0?)
        // Actually: 2 + 2, final flush empty skipped; ensure 2 invocations
        assertThat(seen.size()).isEqualTo(2);
        assertThat(seen.get(0).size()).isEqualTo(2);
        assertThat(seen.get(1).size()).isEqualTo(2);

        // Flatten and verify mapping
        List<UserRow> all = new ArrayList<>();
        seen.forEach(all::addAll);
        assertThat(all).extracting(UserRow::getEmpId).containsExactly("E1","E2","E3","E4");
        assertThat(all).extracting(UserRow::getEmpNm).containsExactly("N1","N2","N3","N4");
    }
}
