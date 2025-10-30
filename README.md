# HKHR Link Batch

JDK 8, Spring Boot 2.7.x + Spring Batch 4.x 기반의 단순 배치. 도메인별 Service Key로 JWT 발급 후 API 호출 결과를 단일 JSON 배열 파일로 저장합니다.

## 목표/구성 요약
- Fat Jar 패키징: `spring-boot-maven-plugin` repackage
- 프로필: `dev`(기본), `prod`
- 도메인: user, organization, attend, apply, account
- 잡 구성:
  - 재사용 스텝 2개
    - FetchJWT(domain): `auth.<domain>.service-key`, `auth.<domain>.token-url`로 JWT 발급 → `jwt.<domain>` 저장
    - FetchAndSaveJSON(domain): API 호출 후 JSON 배열 파일 저장
  - 마스터 잡 `masterJob`: `userJob → organizationJob → attendJob → applyJob → accountJob`

## 설정 키
- 인증: `auth.token-url`(공통), `auth.<domain>.service-key`
- 엔드포인트(모두 POST + JSON Payload): `endpoints.<domain>.url` + `endpoints.<domain>.request-payload`(JSON 문자열, 기본 `{}`)
- 출력: `output.dir`(기본 `target/out`), `output.overwrite`(기본 true), `output.pretty`(기본 false)
- HTTP: `http.connect-timeout-ms`, `http.read-timeout-ms`
- 성능: `fetch.max-threads`(기본 6, 상한 6), `fetch.continue-on-error`(기본 false)

### 템플릿/치환 변수
- `{date}`: 실행 기준 날짜(형식 `yyyyMMdd`). `--requestTime=yyyyMMdd`가 있으면 해당 값, 없으면 오늘 날짜가 적용됩니다.
- `{year_start}`, `{start_of_year}`: 연초일(예: `20250101`)
- `{year_end}`, `{end_of_year}`: 연말일(예: `20251231`)
- `{date_prev_year}`: 1년 전 같은 월·일(예: `20240101`)
- `{year_prev}`: 1년 전 연도(예: `2024`) — 조합에 활용(예: `{year_prev}{MM}{dd}`)
- 하위 호환: `{request_date}`, `{boy}`, `{eoy}`, `{date_last_year}`, `{yyyy_last}`도 지원되지만, 위 권장 이름 사용을 권장합니다.

### 템플릿 예시
- 일자 고정 호출(요청일자):
  - `endpoints.user.request-payload: '{"date":"{date}"}'`
  - 실행: `--requestTime=20250115` → 바디 `{ "date": "20250115" }`
- 연간 범위 호출(연초~연말):
  - `'{"from":"{year_start}","to":"{year_end}"}'` → `{ "from": "20250101", "to": "20251231" }`
- 전년 동일일자 호출(두 방식 모두 동일 결과):
  - `'{"prev":"{date_prev_year}"}'` 또는 `'{"prev":"{year_prev}{MM}{dd}"}'`
  - 기준일 2025-03-10 → `{ "prev": "20240310" }`

## 빌드/실행
- 빌드: `mvn -DskipTests clean package`
- 실행(마스터 잡):
  ```bash
  java -jar target/hkhr-link-batch-0.1.0-SNAPSHOT.jar --spring.batch.job.names=masterJob --requestTime=20250101
  ```
- 개별 잡 실행 예:
  ```bash
  java -jar target/hkhr-link-batch-0.1.0-SNAPSHOT.jar --spring.batch.job.names=userJob --requestTime=20250101
  ```
- 프로필 전환:
  ```bash
  ... --spring.profiles.active=prod
  ```

### 재실행 편의 (RunIdIncrementer)
- 모든 Job(`userJob`, `organizationJob`, `attendJob`, `applyJob`, `accountJob`, `masterJob`)에 `RunIdIncrementer`가 적용되어 있습니다.
- 동일한 파라미터로도 재실행이 가능하며, 실행마다 `run.id`가 자동 증가합니다.

### 스케줄러 온/오프 (프로세스 종료 제어)
- 스케줄러 빈은 조건부 등록입니다: `@ConditionalOnProperty(name="scheduler.enabled", havingValue="true")`
- 단일 잡 1회 실행 후 종료하려면 스케줄러 비활성화:
  - `--scheduler.enabled=false` (bin/master_prod.sh, bin/job_prod.sh, bin/import_users_prod.sh에 기본 포함)
- 주기 실행(3분 간격 masterJob) 시 스케줄러 활성화:
  - `--scheduler.enabled=true` (bin/scheduler_prod.sh에 기본 포함)

### 잡 파라미터(디버그)
- `--job.debug=true|false` (기본 false): 디버그 덤프 활성화
- `--job.debug.dump-sensitive=true|false` (기본 false): 민감정보(JWT/Authorization/Service Key) 마스킹 해제
- `--job.debug.max-dumps=N` (기본 무제한): 종속 도메인 사용자별 요청/응답 덤프 개수 제한
  - 덤프 위치: `<output.dir>/debug/<requestTime(yyyyMMdd) 또는 실행시각>/`
  - JWT: `jwt/jwt-<domain>.txt`, `jwt/token-response-<domain>.json`
  - 독립 도메인: `api/<domain>/req-list.json`, `api/<domain>/resp-list.json`
  - 종속 도메인: `api/<domain>/req-by-user-<userId>.json`, `api/<domain>/resp-by-user-<userId>.json`
  - 기본은 마스킹된 값 저장. `dump-sensitive=true`일 때 원문 저장

## 외부 설정/비밀 값 주입
- 개념: JAR 내부 `application*.yml`은 기본값, 운영 값은 외부 파일/환경변수/명령줄로 덮어쓰기
- 권장: 외부 설정 디렉터리 사용
  - 서버에 `/opt/app/conf/application-prod.yml` 배치
  - 리포 템플릿: `conf/application-prod.yml.sample:1`
  - 실행 예:
    ```bash
    java -jar target/hkhr-link-batch-0.1.0-SNAPSHOT.jar \
      --spring.profiles.active=prod \
      --spring.config.additional-location=file:/opt/app/conf/ \
      --spring.batch.job.names=masterJob
    ```
- 환경변수 예(비밀 값 주입에 적합)
  - `SPRING_PROFILES_ACTIVE=prod`
  - `SPRING_CONFIG_ADDITIONAL_LOCATION=file:/opt/app/conf/`
  - `SPRING_DATASOURCE_URL=jdbc:oracle:thin:@//db-host:1521/PROD`
  - `SPRING_DATASOURCE_USERNAME=BATCH`, `SPRING_DATASOURCE_PASSWORD=******`
  - 커스텀: `AUTH_USER_SERVICE_KEY=...`, `ENDPOINTS_USER_URL=...`, `ENDPOINTS_USER_REQUEST_PAYLOAD='{"date":"{date}"}'`, `OUTPUT_DIR=/data/hkhr/out`
- 주의
  - 수동 DDL 사용 시: `spring.batch.jdbc.initialize-schema=never`
  - 사설 CA 사용 시 JVM 옵션으로 truststore 지정(예시):
    `-Djavax.net.ssl.trustStore=/path/truststore.jks -Djavax.net.ssl.trustStorePassword=*****`
  - requestTime 형식: `yyyyMMdd` (예: 20250101)

## 산출물
- 파일명 규칙: `<domain>s-YYYYMMDD.json` (예: `users-20250101.json`)
- 모두 "최상위 배열" 구조로 스트리밍 방식으로 저장됩니다.

## 구현 메모
- 인증 토큰(FetchJWT): `servicekey` 헤더에 평문 Service Key를 넣어 공통 Auth URL로 POST(바디 없음) → `{ "jwt": "..." }` 응답에서 토큰 추출
- 도메인 호출: `endpoints.<domain>.url`로 POST + `request-payload` 1회 호출 → 응답이 배열이면 병합, 객체면 단일 항목으로 저장
- 실패 정책: 기본 오류 시 Step 실패. `fetch.continue-on-error=true` 시 오류 항목만 스킵 후 계속
- 병렬: `fetch.max-threads>1` 설정 시 내부적으로 호출 처리 병렬화(파일 쓰기는 스트리밍으로 안전)

## 주의사항
- 토큰 요청 시 헤더 키는 정확히 `servicekey`를 사용합니다.
- 실제 토큰 응답(JSON)은 기본적으로 `jwt` 키를 기대하며, 호환을 위해 `token`, `access_token`도 지원합니다.

## 파일 구조
- `src/main/java/com/hkhr/link/BatchApplication.java`: 부트스트랩
- `src/main/java/com/hkhr/link/config/`: 설정 및 잡 정의
- `src/main/java/com/hkhr/link/tasklet/`: 재사용 Tasklet 2개
- `src/main/java/com/hkhr/link/service/JwtService.java`: JWT 발급
- `src/main/java/com/hkhr/link/util/JsonArrayFileWriter.java`: 스트리밍 JSON 파일 작성기
