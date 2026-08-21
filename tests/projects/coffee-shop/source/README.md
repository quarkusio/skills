# Coffeeshop Docker Setup

This bundle lets you build and run all services together.

## Steps

1. Build JARs:
   ```bash
   ./mvnw -DskipTests package
   ```

2. Start all services with Docker Compose:
   ```bash
   docker compose up --build
   ```

3. Access services:
   - Web UI: http://localhost:8080
   - Counter API: http://localhost:8081/api/order
