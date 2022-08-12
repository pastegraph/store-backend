FROM maven:3.8.3-openjdk-16-slim
WORKDIR /app
COPY . .
RUN mvn package -Dmaven.test.skip
ENV LOG_LEVEL="INFO"
CMD ["bash", "-c", "java -jar /app/target/*.jar"]
