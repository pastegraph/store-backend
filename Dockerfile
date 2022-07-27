FROM maven:3.8.3-openjdk-16-slim
WORKDIR /app
COPY . .
RUN mvn package
CMD ["java", "-jar", "com/github/pastegraph/store/*.jar"]