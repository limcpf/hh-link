#!/usr/bin/env bash
set -euo pipefail

#
# 운영 시나리오: 외부 설정 + prod 프로필로 importUsersJob 실행 (JSON → Oracle USERS 적재)
# - 입력 파일: 기본은 외부에서 지정(인자 1), 미지정 시 application.yml의 기본(target/out/users.json)을 사용
# - USERS 테이블(EMP_ID, EMP_NM)에 INSERT ALL로 적재 (Mapper XML 참고)
#
# 사용 예시:
#   ./bin/import_users_prod.sh /data/users.json
#   ./bin/import_users_prod.sh   # 미지정 시 기본값 사용
#

INPUT_FILE="${1:-}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

JAR="${HKHR_JAR:-}"
if [[ -z "${JAR}" ]]; then
  DEFAULT_JAR="$(ls -1t "${ROOT_DIR}"/target/hkhr-link-batch-*.jar 2>/dev/null | grep -v '\.original$' | head -n1 || true)"
  JAR="${DEFAULT_JAR:-}"
fi

if [[ -z "${JAR}" || ! -f "${JAR}" ]]; then
  echo "[오류] Fat JAR을 찾지 못했습니다. 먼저 빌드하거나 HKHR_JAR로 경로를 지정하세요." >&2
  exit 1
fi

CONF_DIR="${CONF_DIR:-/opt/app/conf}"
JVM_OPTS="${JVM_OPTS:-}"

CMD=(java ${JVM_OPTS} -jar "${JAR}" \
  --spring.profiles.active=prod \
  --spring.config.additional-location="file:${CONF_DIR}/" \
  --spring.batch.job.names=importUsersJob)

if [[ -n "${INPUT_FILE}" ]]; then
  CMD+=(--import.input-file="${INPUT_FILE}")
fi

echo "[정보] JAR: ${JAR}"
echo "[정보] CONF_DIR: ${CONF_DIR} (external config)"
echo "[정보] 실행: ${CMD[*]}"

exec "${CMD[@]}"

