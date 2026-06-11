FROM amazoncorretto:17-alpine

WORKDIR /app

# 타임존 설정 및 curl 설치 (헬스체크용)
RUN apk add --no-cache tzdata curl && \
    ln -sf /usr/share/zoneinfo/Asia/Seoul /etc/localtime && \
    echo "Asia/Seoul" > /etc/timezone

# 애플리케이션 JAR 파일 복사
COPY build/libs/insight-reputation-*.jar app.jar

# 컨테이너 포트 노출
EXPOSE 8083

# 헬스체크 설정
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8083/actuator/health || exit 1

# JVM 설정 및 애플리케이션 실행
ENTRYPOINT ["java", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-Dspring.profiles.active=docker", \
  "-Xms512m", \
  "-Xmx1024m", \
  "-jar", "app.jar"]