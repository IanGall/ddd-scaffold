# 基础镜像
FROM eclipse-temurin:21-jre

# 配置
ENV PARAMS=""

# 工作目录：日志按相对路径落盘（见 logback-spring.xml），固定到 /app 便于挂卷与采集
WORKDIR /app

# 生产部署固定 prod Profile：中间件地址与凭证一律由部署环境注入（见 application-prod.yml）
ENV SPRING_PROFILES_ACTIVE=prod

# 容器内存感知的 JVM 参数，部署时可用 JAVA_OPTS 覆盖
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"

# 时区（与网关保持一致，避免跨服务日志/时间口径不同）
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 添加应用
ADD target/${artifactId}.jar /${artifactId}.jar

# 骨架默认不引入 HTTP 触发能力（spring-boot-starter-web 仅在 -Phttp 下激活），运行期只有 Dubbo Provider，
# 因此暴露 Dubbo 端口；application.yml 里的 server.port 在未启用 -Phttp 时不会被监听。
EXPOSE 20880

# exec 使 java 取代 sh 成为 PID 1，正确接收 SIGTERM
ENTRYPOINT ["sh","-c","exec java $JAVA_OPTS -jar /${artifactId}.jar $PARAMS"]
