# mediaasistant
Repo test AI with spring


## Orders codespace
Shift + Ctrl + P
Codespaces: Stop Current Codespace

gh codespace stop
gh codespace stop -r santsa/mediaasistant
lsof -nP -iTCP:8080 -sTCP:LISTEN
SPRING_PROFILES_ACTIVE=gemini ./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8080

./mvnw spring-boot:run \
  -Dspring-boot.run.arguments=--server.port=8080 \
  '-Dspring-boot.run.jvmArguments=-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005'

docker compose -f /workspaces/mediaasistant/docker-compose.yml up -d --force-recreate postgres
docker compose -f /workspaces/mediaasistant/docker-compose.yml ps -a && docker logs --tail 20 mediaassistant-db