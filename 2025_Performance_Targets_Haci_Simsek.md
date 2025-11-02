# Yearly Performance Targets
**Hacı Şimşek**  
**Manager:** Osman YALIN  
**Team:** Turkish Web Team  
**Year:** 2025

---

## Table of Contents
1. [Real-Time Order Processing and Messaging System](#real-time-order-processing-and-messaging-system)
    - [Analyze](#analyze)
    - [Timeline – ETA](#timeline--eta)
    - [Project Details](#project-details)
    - [Tech Stack](#tech-stack)
    - [Gains](#gains)

---

## Real-Time Order Processing and Messaging System

### Analyze
This project aims to provide a fully functional **microservice-based architecture** to process high-volume orders and notifications in real time in a single **20-week phase**.  
The solution will adopt an **event-driven design** that encompasses supporting infrastructure such as order acquisition, processing, notification delivery, reporting, caching, messaging, containerization, and monitoring.

- **Redis** → High-speed data caching
- **RabbitMQ** → Inter-service communication, job queue management, message routing
- **PostgreSQL** → Primary database
- The system will be designed with **scalability**, **security**, and **ease of maintenance** in mind.

---

### Timeline – ETA

| Phase | Duration | Period | Key Deliverables |
|-------|-----------|--------|------------------|
| **1. Planning & Analysis** | 3 Weeks | Aug 16 – Sep 5, 2025 | Project kickoff, requirements gathering, environment setup, architecture design |
| **2. Core Microservices Development** | 6 Weeks | Sep 6 – Oct 17, 2025 | Order, Notification, and Inventory services, PostgreSQL integration, RESTful APIs |
| **3. Messaging Infrastructure** | 3 Weeks | Oct 18 – Nov 7, 2025 | RabbitMQ setup, event handling, DLQ and idempotency logic |
| **4. Security Implementation** | 2 Weeks | Nov 8 – Nov 21, 2025 | JWT authentication, RBAC, API Gateway security |
| **5. Reporting Service** | 2 Weeks | Nov 22 – Dec 5, 2025 | Reporting module, export features, dashboard visualization |
| **6. Containerization & Orchestration** | 2 Weeks | Dec 6 – Dec 19, 2025 | Dockerization, Kubernetes orchestration, Prometheus & Grafana monitoring |
| **7. Testing, CI/CD & Production Deployment** | 1 Week | Dec 20 – Dec 27, 2025 | Integration & load testing, CI/CD with GitHub Actions, go-live |

---

### Project Details

#### 1. Planning & Analysis – Aug 16 – Sep 5, 2025 (3 Weeks)
- Project kickoff and requirements gathering
- Finalize full project scope and delivery milestones
- Set up development environment (Java/Spring Boot, PostgreSQL, Redis, RabbitMQ)
- Architecture design: **Microservices + CQRS + Event-driven**

#### 2. Core Microservices Development – Sep 6 – Oct 17, 2025 (6 Weeks)
- **Order Management Service:** CRUD, workflow management
- **Notification Service:** Email, SMS, push notifications
- **Inventory Service:** Real-time stock updates
- PostgreSQL schema design and integration
- RESTful API development
- Unit testing & error handling

#### 3. Messaging Infrastructure – Oct 18 – Nov 7, 2025 (3 Weeks)
- RabbitMQ setup and configuration
- Exchange, queue, and binding definitions
- Event publish/subscribe integration
- Retry logic, DLQ management, and idempotency

#### 4. Security Implementation – Nov 8 – Nov 21, 2025 (2 Weeks)
- JWT-based authentication
- RBAC for user roles and permissions
- API Gateway security enforcement (rate limiting, validation)
- **Execution Plan**
  - Requirements & Gap Analysis: catalogue protected endpoints, role matrix, and inter-service trust boundaries.
  - Shared Auth Package: implement reusable JWT verification module and rotation strategy.
  - Order-Service Hardening: add Spring Security guards and controller-level authorization policies.
  - Notification & Inventory Hardening: align authentication middleware and tighten management endpoints.
  - API Gateway Policies: enforce rate limiting, TLS termination, and secure defaults (CORS, headers).
  - Security Testing: automate JWT scope tests, dependency scans, and document incident response runbooks.

#### 5. Reporting Service – Nov 22 – Dec 5, 2025 (2 Weeks)
- Reporting (daily, weekly, monthly)
- JSON/CSV export capabilities
- Dashboard visualization

#### 6. Containerization & Orchestration – Dec 6 – Dec 19, 2025 (2 Weeks)
- Dockerizing all services
- Orchestration with Docker Compose & Kubernetes (Minikube)
- Monitoring & alerting setup (Prometheus, Grafana)

#### 7. Testing, CI/CD & Production Deployment – Dec 20 – Dec 27, 2025 (1 Week)
- Integration, load, and performance testing
- CI/CD pipeline (GitHub Actions)
- Operational documentation and production go-live

---

### Tech Stack

| Category | Technologies |
|-----------|--------------|
| **Backend** | Java (Spring Boot) |
| **Database** | PostgreSQL |
| **Messaging** | RabbitMQ |
| **Caching** | Redis |
| **Architecture** | Microservices, CQRS, Event-driven |
| **Containerization** | Docker, Kubernetes (Minikube) |
| **Security** | JWT, RBAC |
| **CI/CD** | GitHub Actions |
| **Monitoring** | Prometheus, Grafana |

---

### Gains
A software developer aiming to leverage and enhance technical expertise by utilizing technologies adopted within the team, integrating them with new tools to create robust, secure, and scalable infrastructures.

Experienced in the **end-to-end lifecycle** of microservice-based systems — from design and development to deployment and maintenance — capable of processing **high-volume data streams** with low latency using technologies such as **RabbitMQ**, **PostgreSQL**, **Redis**, **Docker**, and **Kubernetes (Minikube)**.

Skilled in:
- Designing relational schemas and analytics workflows
- Applying **JWT** and **RBAC** for secure access control
- Implementing **containerization**, **orchestration**, **CI/CD**, and **system observability**
- Ensuring scalability, maintainability, and production readiness

---

**Document Status:** Public  
