# Z8 Client

Simple web-client for Z8 projects automation

Build:
./gradlew build

Usage:
java -jar z8-client-1.0.jar <url> <action> <request>");

Actions:
    - job: run job

Known requests:
    - gen: org.zenframework.z8.server.db.generator.SchemaGenerator

Example:
java -jar z8-client.jar http://Admin:pwd@localhost:9080/ job gen
