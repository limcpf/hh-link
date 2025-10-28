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
- 인증: `auth.<domain>.service-key`, `auth.<domain>.token-url`
- 엔드포인트:
  - 독립 도메인: `endpoints.<domain>.list-url`
  - 종속 도메인: `endpoints.<domain>.by-user-url-template` (예: `.../attend?userId={userId}`)
- 출력: `output.dir`(기본 `target/out`), `output.overwrite`(기본 false), `output.pretty`(기본 false)
- HTTP: `http.connect-timeout-ms`, `http.read-timeout-ms`
- 성능: `fetch.max-threads`(기본 1), `fetch.continue-on-error`(기본 false)

## 빌드/실행
- 빌드: `mvn -DskipTests clean package`
- 실행(마스터 잡):
  ```bash
  java -jar target/hkhr-link-batch-0.1.0-SNAPSHOT.jar --spring.batch.job.names=masterJob
  ```
- 개별 잡 실행 예:
  ```bash
  java -jar target/hkhr-link-batch-0.1.0-SNAPSHOT.jar --spring.batch.job.names=userJob
  ```
- 프로필 전환:
  ```bash
  ... --spring.profiles.active=prod
  ```

### 재실행 편의 (RunIdIncrementer)
- 모든 Job(`userJob`, `organizationJob`, `attendJob`, `applyJob`, `accountJob`, `masterJob`)에 `RunIdIncrementer`가 적용되어 있습니다.
- 동일한 파라미터로도 재실행이 가능하며, 실행마다 `run.id`가 자동 증가합니다.

### 잡 파라미터(디버그)
- `--job.debug=true|false` (기본 false): 디버그 덤프 활성화
- `--job.debug.dump-sensitive=true|false` (기본 false): 민감정보(JWT/Authorization/Service Key) 마스킹 해제
- `--job.debug.max-dumps=N` (기본 무제한): 종속 도메인 사용자별 요청/응답 덤프 개수 제한
- 덤프 위치: `<output.dir>/debug/<requestTime 또는 실행시각>/`
  - JWT: `jwt/jwt-<domain>.txt`, `jwt/token-response-<domain>.json`
  - 독립 도메인: `api/<domain>/req-list.json`, `api/<domain>/resp-list.json`
  - 종속 도메인: `api/<domain>/req-by-user-<userId>.json`, `api/<domain>/resp-by-user-<userId>.json`
  - 기본은 마스킹된 값 저장. `dump-sensitive=true`일 때 원문 저장

## 산출물
- `target/out/users.json`, `organizations.json`, `attends.json`, `applies.json`, `accounts.json`
- 모두 "최상위 배열" 구조로 스트리밍 방식으로 저장됩니다.

## 구현 메모
- 독립 도메인(user, organization): `list-url` 1회 호출 → 배열이면 항목 병합, 객체면 1개 항목으로 저장
- 종속 도메인(attend, apply, account): `users.json`의 `userId`(없으면 `id`)를 순회하며 `{userId}`를 템플릿에 바인딩하여 호출. 결과가 배열이면 항목 병합, 객체면 1개 항목으로 저장
- 실패 정책: 기본 오류 시 Step 실패. `fetch.continue-on-error=true` 시 해당 사용자만 스킵하고 계속
- 병렬: `fetch.max-threads>1`이면 사용자 단위 병렬 호출. 파일 쓰기는 내부적으로 동기화되어 일관성 유지

## 주의사항
- 실제 토큰 응답(JSON)에서 키는 `token`, `jwt`, `access_token` 중 하나여야 합니다(필요 시 서비스 요구에 맞게 수정 가능)
- `users.json`은 최상위 배열이어야 하며 각 항목에 `userId` 또는 `id` 필드를 포함해야 합니다.

## 파일 구조
- `src/main/java/com/hkhr/link/BatchApplication.java`: 부트스트랩
- `src/main/java/com/hkhr/link/config/`: 설정 및 잡 정의
- `src/main/java/com/hkhr/link/tasklet/`: 재사용 Tasklet 2개
- `src/main/java/com/hkhr/link/service/JwtService.java`: JWT 발급
- `src/main/java/com/hkhr/link/util/JsonArrayFileWriter.java`: 스트리밍 JSON 파일 작성기
