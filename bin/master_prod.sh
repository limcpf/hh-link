#!/usr/bin/env bash
set -euo pipefail

#
# 운영 시나리오: 외부 설정(추가 경로) + prod 프로필로 masterJob 1회 실행
# - JAR는 Fat Jar를 사용합니다.
# - 외부 설정 디렉터리(CONF_DIR)에 운영 값(application-prod.yml 등)을 배치합니다.
# - 필요 시 JVM 옵션(JVM_OPTS)에 truststore, 메모리 등을 추가합니다.
#
# 사용 예시:
#   ./bin/master_prod.sh
#   CONF_DIR=/opt/app/conf JVM_OPTS="-Xms512m -Xmx1024m" ./bin/master_prod.sh
#   HKHR_JAR=/opt/app/hkhr-link-batch-0.1.0-SNAPSHOT.jar ./bin/master_prod.sh
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
  echo "예) mvn -DskipTests clean package" >&2
  exit 1
fi

CONF_DIR="${CONF_DIR:-/opt/app/conf}"
JVM_OPTS="${JVM_OPTS:-}"

echo "[정보] JAR: ${JAR}"
echo "[정보] CONF_DIR: ${CONF_DIR} (external config)"

exec java ${JVM_OPTS} -jar "${JAR}" \
  --spring.profiles.active=prod \
  --spring.config.additional-location="file:${CONF_DIR}/" \
  --scheduler.enabled=false \
  --spring.batch.job.names=masterJob
