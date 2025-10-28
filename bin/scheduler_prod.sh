#!/usr/bin/env bash
set -euo pipefail

#
# 운영 시나리오: 스케줄러만으로 주기 실행 (3분마다 masterJob)
# - 기동 시 자동 실행(step)은 비활성화하고(`--spring.batch.job.enabled=false`),
#   애플리케이션 내 스케줄러(@Scheduled)가 3분 간격으로 masterJob을 실행합니다.
# - 외부 설정 디렉터리(CONF_DIR)에서 운영 값을 로드합니다.
#
# 사용 예시:
#   ./bin/scheduler_prod.sh
#   CONF_DIR=/opt/app/conf JVM_OPTS="-Xms512m -Xmx1024m" ./bin/scheduler_prod.sh
#

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
echo "[정보] 모드: 스케줄러만으로 주기 실행 (기동 시 자동 실행 비활성화)"

exec java ${JVM_OPTS} -jar "${JAR}" \
  --spring.profiles.active=prod \
  --spring.config.additional-location="file:${CONF_DIR}/" \
  --spring.batch.job.enabled=false

