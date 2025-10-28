#!/usr/bin/env bash
set -euo pipefail

#
# 운영 시나리오: 외부 설정 + prod 프로필로 지정한 잡 1회 실행
# - 인자 1: 잡 이름 (예: userJob, organizationJob, attendJob, applyJob, accountJob, importUsersJob)
# - 추가 인자: 잡 파라미터 (예: --job.debug=true --requestTime=20251029090000)
# - CONF_DIR, JVM_OPTS, HKHR_JAR 환경변수로 경로/옵션을 제어할 수 있습니다.
#
# 사용 예시:
#   ./bin/job_prod.sh userJob
#   ./bin/job_prod.sh importUsersJob --import.input-file=/data/users.json
#   CONF_DIR=/opt/app/conf ./bin/job_prod.sh attendJob --job.debug=true --job.debug.max-dumps=20
#

if [[ $# -lt 1 ]]; then
  echo "사용법: $0 <jobName> [추가 잡 파라미터...]" >&2
  exit 1
fi

JOB_NAME="$1"; shift || true

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

echo "[정보] JAR: ${JAR}"
echo "[정보] CONF_DIR: ${CONF_DIR} (external config)"
echo "[정보] 실행 잡: ${JOB_NAME}"

exec java ${JVM_OPTS} -jar "${JAR}" \
  --spring.profiles.active=prod \
  --spring.config.additional-location="file:${CONF_DIR}/" \
  --spring.batch.job.names="${JOB_NAME}" \
  "$@"

