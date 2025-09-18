# Build stage
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
# Copy toàn bộ project để Maven có đủ context dependencyManagement
COPY . .
# Build trực tiếp; bỏ go-offline để tránh lỗi UnresolvableModel khi cần repo bổ sung
RUN mvn -B -e -DskipTests package

# Runtime stage (Tomcat 10 Jakarta)
FROM tomcat:10.1-jdk17
ENV TZ=Asia/Ho_Chi_Minh
ENV SPRING_PROFILES_ACTIVE=prod

# Clean default webapps
RUN rm -rf /usr/local/tomcat/webapps/*

# Copy built WAR
COPY --from=builder /app/target/be-0.0.1-SNAPSHOT.war /usr/local/tomcat/webapps/ROOT.war

# Expose port
EXPOSE 8080

# Healthcheck (optional)
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=5 \
  CMD curl -fsS http://localhost:8080/actuator/health | grep '"status":"UP"' || exit 1

# Start Tomcat
CMD ["catalina.sh", "run"]


