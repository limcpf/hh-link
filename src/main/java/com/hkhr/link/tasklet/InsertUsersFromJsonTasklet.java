package com.hkhr.link.tasklet;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.hkhr.link.config.AppSettings;
import com.hkhr.link.db.UserMapper;
import com.hkhr.link.db.UserRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.util.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

// JSON 파일(users.json 등)에서 EMP_ID/EMP_NM을 추출하여 USERS 테이블에 일괄 INSERT 하는 예시 Tasklet
public class InsertUsersFromJsonTasklet implements Tasklet {
    private static final Logger log = LoggerFactory.getLogger(InsertUsersFromJsonTasklet.class);

    private final AppSettings settings;
    private final UserMapper userMapper;

    public InsertUsersFromJsonTasklet(AppSettings settings, UserMapper userMapper) {
        this.settings = settings;
        this.userMapper = userMapper;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        // 잡 파라미터(import.input-file) 우선, 없으면 기본 users.json 경로 사용
        String inputPathParam = (String) chunkContext.getStepContext().getJobParameters().get("import.input-file");
        Path input = StringUtils.hasText(inputPathParam)
                ? Paths.get(inputPathParam)
                : settings.getUsersJsonPath();

        if (!Files.exists(input)) {
            throw new IllegalStateException("Input JSON not found: " + input);
        }

        // 배치 크기 단위로 insertAll 호출
        int batchSize = settings.getDbBatchSize();
        JsonFactory jf = new JsonFactory();
        long total = 0L;
        List<UserRow> batch = new ArrayList<>(batchSize);

        try (JsonParser p = jf.createParser(input.toFile())) {
            if (p.nextToken() != JsonToken.START_ARRAY) { throw new IllegalStateException("Top-level array expected: " + input); }
            while (p.nextToken() != JsonToken.END_ARRAY) {
                String empId = null;
                String empNm = null;
                if (p.currentToken() == JsonToken.START_OBJECT) {
                    while (p.nextToken() != JsonToken.END_OBJECT) {
                        String field = p.getCurrentName();
                        p.nextToken();
                        // 다양한 필드명을 허용(empId/EMP_ID/userId/id)
                        if ("empId".equals(field) || "EMP_ID".equalsIgnoreCase(field) || "userId".equals(field) || "id".equals(field)) {
                            empId = p.getValueAsString();
                        } else if ("empNm".equals(field) || "EMP_NM".equalsIgnoreCase(field) || "name".equals(field) || "userName".equals(field)) {
                            empNm = p.getValueAsString();
                        } else {
                            p.skipChildren();
                        }
                    }
                } else {
                    p.skipChildren();
                }
                if (StringUtils.hasText(empId)) {
                    batch.add(new UserRow(empId, empNm));
                }
                if (batch.size() >= batchSize) {
                    total += flush(batch);
                }
            }
        }
        total += flush(batch);
        log.info("Inserted USERS rows: {} from {}", total, input);
        return RepeatStatus.FINISHED;
    }

    // 남은 배치를 INSERT 하고 버퍼를 비웁니다.
    private long flush(List<UserRow> batch) {
        if (batch.isEmpty()) return 0L;
        int n = userMapper.insertAll(batch);
        batch.clear();
        return n;
    }
}
