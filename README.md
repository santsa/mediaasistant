# mediaasistant
Repo test AI with spring


## Orders codespace
Shift + Ctrl + P
Codespaces: Stop Current Codespace

gh codespace stop
gh codespace stop -r santsa/mediaasistant
lsof -nP -iTCP:8080 -sTCP:LISTEN
SPRING_PROFILES_ACTIVE=gemini ./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8080