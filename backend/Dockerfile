FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace/backend
ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright

COPY backend/pom.xml backend/maven-central-settings.xml ./
RUN mvn -s maven-central-settings.xml -B dependency:go-offline

COPY backend/src ./src
RUN mvn -s maven-central-settings.xml -B -DskipTests package \
    && mvn -s maven-central-settings.xml -B dependency:build-classpath -Dmdep.outputFile=/tmp/playwright-classpath \
    && java -cp "$(cat /tmp/playwright-classpath)" com.microsoft.playwright.CLI install chromium

FROM eclipse-temurin:17-jdk-jammy

ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright \
    DISPLAY=:99 \
    TZ=Asia/Shanghai

RUN apt-get update \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
        ca-certificates \
        curl \
        fluxbox \
        fonts-noto-cjk \
        libasound2 \
        libatk-bridge2.0-0 \
        libatk1.0-0 \
        libcups2 \
        libdrm2 \
        libgbm1 \
        libgtk-3-0 \
        libnss3 \
        libx11-6 \
        libx11-xcb1 \
        libxcb1 \
        libxcomposite1 \
        libxdamage1 \
        libxext6 \
        libxfixes3 \
        libxkbcommon0 \
        libxrandr2 \
        novnc \
        tzdata \
        websockify \
        wget \
        x11vnc \
        xvfb \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=build /workspace/backend/target/*.jar /app/app.jar
COPY --from=build /ms-playwright /ms-playwright
COPY backend/docker-entrypoint.sh /app/docker-entrypoint.sh

RUN chmod +x /app/docker-entrypoint.sh \
    && mkdir -p /app/logs /app/browser-data/csdn /app/browser-data/zhihu \
    && chmod -R 755 /ms-playwright

EXPOSE 8080 5900 6080

ENTRYPOINT ["/app/docker-entrypoint.sh"]
