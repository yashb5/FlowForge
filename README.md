# FlowForge

Distributed Task Scheduling and Workflow Engine API built with Java 21 and Spring Boot 3.2.

## Features

- **Workflow Management**: DAG-based workflow definitions with task dependencies
- **Task Scheduling**: Cron-based and one-time scheduled tasks
- **Execution Engine**: Retry policies with exponential backoff
- **Authentication**: JWT-based security with role-based access
- **API Documentation**: OpenAPI/Swagger UI

## Tech Stack

- Java 21
- Spring Boot 3.2
- Spring Security + JWT
- Spring Data JPA
- PostgreSQL / H2
- Liquibase
- MapStruct

## Quick Start

```bash
# Run with Maven
./mvnw spring-boot:run

# Run with Docker
docker build -t flowforge .
docker run -p 8080:8080 flowforge
```

## API Endpoints

- `POST /api/v1/auth/login` - Authenticate
- `GET /api/v1/workflows` - List workflows
- `POST /api/v1/workflows/{id}/execute` - Execute workflow
- `GET /api/v1/executions` - List executions

Swagger UI: `http://localhost:8080/swagger-ui.html`
