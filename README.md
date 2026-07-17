# WFO-Tracker

A web-based internal **Work From Office Compliance Tracker** application for organizations/teams to track employee office attendance compliance based on corporate policy (50% mandatory office attendance).

Designed as an enterprise-grade, production-quality solution built with Java 21, Spring Boot 4.x, and MySQL.

---

## 1. Application Architecture

The application is structured as a monolithic Spring Boot backend rendering responsive Thymeleaf templates for the UI.

- **Backend:** Java 21, Spring Boot 4.x, Spring MVC, Spring Data JPA, Spring Security (BCrypt)
- **Frontend:** Thymeleaf, Bootstrap 5, standard responsive CSS
- **Exports:** Apache POI (dynamic streaming exports in Excel XLSX and raw CSV formats)
- **Database:** MySQL 8 (database schema managed dynamically via **Flyway Schema Migrations**)
- **Deployment:** Containerized Docker & Docker Compose setup

---

## 2. Installation & Running Locally

1. **Clone the repository:**
```bash
git clone <repo-url>
cd wfotracker
```

2. **Start MySQL database:**
Start the database service independently in background mode using Docker:
```bash
docker compose up -d mysql
```

3. **Run the Spring Boot application:**
Execute the application wrapper locally:
```bash
./mvnw spring-boot:run
```

---

## 3. Running via Docker Compose

To run the complete production-ready stack (Application + MySQL Database) containerized:

1. **Build the production JAR:**
```bash
./mvnw clean package -DskipTests
```

2. **Build and start all containers:**
```bash
docker compose up --build -d
```

---

## 4. Port 80 Deployment & Bypassing Corporate Proxies / Zscaler

In corporate environments, traffic on non-standard ports (e.g., `8080`, `3000`) is routinely blocked by proxy services (such as Zscaler).

To ensure your application is fully accessible from corporate laptops, **the application has been pre-configured to run on standard HTTP Port 80**:

### **Step-by-Step Oracle Cloud VM / VPS Deployment:**

1.  **OCI Console Rule:** Add an **Ingress Rule** inside your Virtual Cloud Network (VCN) security list allowing incoming TCP traffic on **Port 80** from `0.0.0.0/0`.
2.  **OS-Level Firewall Rule:** Open port 80 inside your VM local OS firewall.
	-   *For Oracle Linux (RHEL):*
		```bash
		sudo firewall-cmd --permanent --zone=public --add-port=80/tcp
		sudo firewall-cmd --reload
		```
	-   *For Ubuntu / Debian:*
		```bash
		sudo ufw allow 80/tcp
		sudo ufw reload
		```
3.  **Launch Compose:** Pull the changes and execute `docker compose up --build -d`.
4.  **Access App:** You can now connect to your application from **any corporate laptop** using standard HTTP:
	```text
	http://<your-vps-public-ip>
	```

---

## 5. Initial Login Credentials

Upon startup, Flyway automatically provisions an Administrator account:

- **Username:** `admin`
- **Password:** `admin@123`
- **Role:** `ADMIN`

The Administrator must change their password upon their very first login. Admins manage Teams and Managers, Managers provision Employees, and Employees track their attendance.
