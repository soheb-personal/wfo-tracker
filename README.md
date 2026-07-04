# WFO-Tracker

A web-based internal **Work From Office Compliance Tracker** application for organizations/teams to track employee office attendance compliance based on company policy (50% mandatory office attendance).

## Application Architecture

The application is built as a monolithic Spring Boot backend rendering Thymeleaf templates for the UI.

- **Backend:** Java 21, Spring Boot 3.3.x, Spring MVC, Spring Data JPA, Spring Security
- **Frontend:** Thymeleaf, Bootstrap 5
- **Database:** MySQL 8+
- **Migrations:** Flyway
- **Deployment:** Docker & Docker Compose

## Installation & Running Locally

1. **Clone the repository:**
```bash
git clone <repo-url>
cd wfotracker
```

2. **Start MySQL database:**
You can use docker-compose to start only the database:
```bash
docker-compose up -d mysql
```

3. **Run the Spring Boot application:**
```bash
./mvnw spring-boot:run
```

## Running via Docker Compose

To start both the application and the database using Docker Compose:

1. **Build the JAR:**
```bash
./mvnw clean package -DskipTests
```

2. **Build and start containers:**
```bash
docker-compose up --build -d
```

## Login Credentials

Upon the very first execution, Flyway creates an admin user with the following credentials:

- **Username:** `admin`
- **Password:** `A9#Lm2@Qx7`
- **Role:** `ADMIN`

Admin can then create teams and managers. Managers will be provisioned with default passwords which they are forced to change upon first login. Employees are provisioned by managers.
