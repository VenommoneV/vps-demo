# ==========================================
# Stage 1: 构建阶段 (起个别名叫 builder)
# ==========================================
FROM maven:3.8.6-openjdk-11-slim AS builder

# 设置工作目录
WORKDIR /build

# 1. 先拷贝 pom.xml 并下载依赖 (利用 Docker 缓存机制，代码变了依赖没变时，不会重新下载)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 2. 拷贝源代码并打包
COPY src ./src
RUN mvn package -DskipTests

# ==========================================
# Stage 2: 运行阶段 (最终镜像)
# ==========================================
FROM eclipse-temurin:11-jre


# 设置工作目录
WORKDIR /app

# 从 builder 阶段拷贝打包好的 jar 包，改名为 app.jar
COPY --from=builder /build/target/*.jar app.jar

# 设置时区 (面试坑点：不设时区，日志时间会差8小时)
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 暴露端口
EXPOSE 8080

# 启动命令 (加入 JVM 参数限制内存，防止 2核4G 被吃光)
# -Xms256m: 最小内存
# -Xmx512m: 最大内存 (给系统留足空间)
ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", "-jar", "app.jar"]
