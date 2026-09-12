#!/usr/bin/env bash
# 带 JaCoCo Agent 启动本服务，用于分布式 E2E 覆盖率采集。
#
# Agent 以 tcpserver 模式在本机端口监听，由 coverage-controller 主动连接 dump，
# 因此服务无需感知控制器地址，也不要求控制器先启动。
#
# 端口约定（全局唯一，冲突时用环境变量覆盖）：
#   Gateway 6300 / 标准服务 6301 / 新服务从 6302 起顺延
#
#   COVERAGE_AGENT_NAME        Agent 名称，需与控制器 coverage.agents[].name 一致（默认工程名）
#   COVERAGE_AGENT_PORT        Agent 监听端口（默认 6301）
#   COVERAGE_INCLUDES          需要插桩的类，默认 cn.iantech.*
#   COVERAGE_PROFILE           Spring profile，默认 dev
#   JACOCO_AGENT_JAR           覆盖 jacocoagent 路径
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"
# 从目录名推导 ${rootArtifactId}，避免依赖模板变量（本脚本由非过滤文件集生成）
ROOT_NAME="$(basename "${PROJECT_DIR}")"

COVERAGE_AGENT_NAME="${COVERAGE_AGENT_NAME:-${ROOT_NAME}}"
COVERAGE_AGENT_PORT="${COVERAGE_AGENT_PORT:-6301}"
COVERAGE_INCLUDES="${COVERAGE_INCLUDES:-cn.iantech.*}"
COVERAGE_PROFILE="${COVERAGE_PROFILE:-dev}"

JACOCO_VERSION="${JACOCO_VERSION:-0.8.13}"
JACOCO_AGENT_JAR="${JACOCO_AGENT_JAR:-${HOME}/.m2/repository/org/jacoco/org.jacoco.agent/${JACOCO_VERSION}/org.jacoco.agent-${JACOCO_VERSION}-runtime.jar}"

if [[ ! -f "${JACOCO_AGENT_JAR}" ]]; then
  echo "未找到 jacocoagent: ${JACOCO_AGENT_JAR}" >&2
  echo "请先执行: mvn dependency:get -Dartifact=org.jacoco:org.jacoco.agent:${JACOCO_VERSION}:jar:runtime" >&2
  exit 1
fi

AGENT_OPTS="-javaagent:${JACOCO_AGENT_JAR}=output=tcpserver,address=127.0.0.1,port=${COVERAGE_AGENT_PORT},sessionid=${COVERAGE_AGENT_NAME},includes=${COVERAGE_INCLUDES},dumponexit=true"

echo "启动 ${COVERAGE_AGENT_NAME}，覆盖率 Agent 监听 127.0.0.1:${COVERAGE_AGENT_PORT}，profile=${COVERAGE_PROFILE}"

cd "${PROJECT_DIR}/${ROOT_NAME}-boot"
exec mvn -q spring-boot:run -P"${COVERAGE_PROFILE}" \
  -Dspring-boot.run.jvmArguments="${AGENT_OPTS}"
