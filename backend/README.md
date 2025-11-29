# XO Backend (Spring Boot)

This folder contains a minimal Spring Boot Maven project skeleton for the XO application backend.

What was added:
- `pom.xml` with Spring Boot Web and WebSocket dependencies, Lombok, and build plugins
- `XoApplication` Spring Boot main class
- Model skeletons in `com.example.xo.model`: `Game`, `Player`, `Move`, `GameState`, `BotStrategy`
- `application.properties` (default port 8080)

How to build

You need a JDK (17+) and Maven installed. From the workspace root run:

```bash
mvn -f backend/pom.xml -DskipTests package
```

How to run

```bash
java -jar backend/target/xo-backend-0.0.1-SNAPSHOT.jar
```

Next steps (suggested):
- Implement controllers and WebSocket endpoints for online play
- Implement game logic and bot strategies (minimax or simple heuristics)
- Add persistence (optional) and tests
