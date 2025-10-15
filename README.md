# 🚗 Gridee - Smart Parking Management System

A comprehensive Spring Boot-based parking management application that enables users to find, book, and manage parking spots with real-time availability tracking, payment processing, and QR code-based check-in/check-out functionality.

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![MongoDB](https://img.shields.io/badge/MongoDB-Latest-green.svg)](https://www.mongodb.com/)
[![Docker](https://img.shields.io/badge/Docker-Ready-blue.svg)](https://www.docker.com/)

---

## 📋 Table of Contents

- [Features](#-features)
- [Architecture](#-architecture)
- [Tech Stack](#-tech-stack)
- [Prerequisites](#-prerequisites)
- [Installation](#-installation)
- [Configuration](#-configuration)
- [Running the Application](#-running-the-application)
- [API Documentation](#-api-documentation)
- [Docker Deployment](#-docker-deployment)
- [Security](#-security)
- [Monitoring & Logging](#-monitoring--logging)
- [Project Structure](#-project-structure)
- [Contributing](#-contributing)

---

## 🌟 Features

### Core Functionality
- ✅ **User Management**
  - User registration and authentication
  - Social login integration
  - Role-based access control
  - Multi-vehicle support per user

- ✅ **Parking Lot Management**
  - Create and manage multiple parking lots
  - Define parking zones and spots
  - Real-time spot availability tracking
  - Dynamic pricing based on vehicle type and duration

- ✅ **Smart Booking System**
  - Real-time spot availability search
  - Instant booking with automatic slot reservation
  - Booking extensions and modifications
  - Booking history and management
  - Automatic cancellation policies

- ✅ **QR Code Integration**
  - QR code generation for bookings
  - QR-based check-in and check-out
  - Contactless parking experience

- ✅ **Payment Integration**
  - Payment gateway integration
  - Digital wallet system
  - Automated payment processing
  - Transaction history and receipts
  - Refund management

- ✅ **OTP Verification**
  - Secure OTP generation and validation
  - Phone number verification

### Advanced Features
- 📊 **Analytics & Reporting**
  - Booking statistics
  - Revenue tracking
  - Occupancy rates

- 🔔 **Notifications**
  - Booking confirmations
  - Payment notifications
  - Check-in/check-out alerts

- 🔐 **Security**
  - Token-based authentication
  - Social login support
  - CSRF protection
  - Secure password handling
  - API endpoint protection

- 📝 **Comprehensive Logging**
  - Structured JSON logging
  - Application logs
  - Business event tracking
  - Performance metrics
  - Error tracking

- 📈 **Monitoring**
  - Health check endpoints
  - Metrics collection
  - Distributed tracing
  - Custom logging

---

## 🏗️ Architecture

```
┌─────────────────┐
│   Web Client    │
│   (Browser/App) │
└────────┬────────┘
         │ HTTPS
         ▼
┌─────────────────────────────────────┐
│     Spring Boot Application         │
│  ┌──────────────────────────────┐  │
│  │   Security Layer              │  │
│  └──────────────────────────────┘  │
│  ┌──────────────────────────────┐  │
│  │   REST API Controllers        │  │
│  └──────────────────────────────┘  │
│  ┌──────────────────────────────┐  │
│  │   Business Logic (Services)   │  │
│  └──────────────────────────────┘  │
│  ┌──────────────────────────────┐  │
│  │   Data Access (Repositories)  │  │
│  └──────────────────────────────┘  │
└────────┬────────────────────────────┘
         │
         ▼
┌─────────────────┐      ┌──────────────┐
│    MongoDB      │      │   Payment    │
│   (Database)    │      │   Gateway    │
└─────────────────┘      └──────────────┘
```

---

## 🛠️ Tech Stack

### Backend
- **Framework:** Spring Boot 3.5.5
- **Language:** Java 17
- **Database:** MongoDB
- **Authentication:** JWT, OAuth2
- **Build Tool:** Gradle 8.5

### Libraries & Dependencies
- **Spring Security** - Authentication & Authorization
- **Spring Data MongoDB** - Database operations
- **Spring Boot Actuator** - Health checks & monitoring
- **Lombok** - Reduce boilerplate code
- **Springdoc OpenAPI** - API documentation
- **Logstash Logback Encoder** - Structured logging
- **Micrometer** - Metrics & distributed tracing

### Infrastructure
- **Containerization:** Docker
- **API Documentation:** Swagger/OpenAPI 3.0
- **Logging:** Logback with JSON formatting
- **Monitoring:** Prometheus, Zipkin

---

## 📦 Prerequisites

Before you begin, ensure you have the following installed:

- **Java 17** or higher
- **Gradle 8.5** or higher (or use included Gradle wrapper)
- **MongoDB 7.0** or higher
- **Docker** (optional, for containerized deployment)
- **Git** (for cloning the repository)

### Optional
- **MongoDB Compass** - GUI for MongoDB
- **Postman** or **Bruno** - API testing
- **IntelliJ IDEA** or **VS Code** - IDE

---

## 🚀 Installation

### 1. Clone the Repository

```bash
git clone <repository-url>
cd gridee
```

### 2. Configure MongoDB

**Option A: Local MongoDB**
```bash
mongod --dbpath /path/to/data/db
```

**Option B: MongoDB Atlas (Cloud)**
- Sign up at [MongoDB Atlas](https://www.mongodb.com/cloud/atlas)
- Create a cluster and get connection string

**Option C: Docker MongoDB**
```bash
docker run -d --name mongodb -p 27017:27017 mongo:7.0
```

### 3. Set Up Environment Variables

Create or update your configuration file with required environment variables.

**Required Configuration:**
- Database connection string
- JWT secret key (minimum 32 characters)
- Payment gateway credentials
- OAuth2 client credentials (if using social login)

> ⚠️ **Security Note:** Never commit sensitive credentials to version control. Use environment variables or secure vault services.

### 4. Build the Application

```bash
# Using Gradle wrapper (recommended)
./gradlew clean build
```

---

## ⚙️ Configuration

### Application Profiles

The application supports multiple profiles:

- **local** - Development environment
- **prod** - Production environment

Activate a profile via environment variable:
```bash
export SPRING_PROFILES_ACTIVE=prod
```

### Key Configuration Areas

- **Server Configuration** - Port, SSL settings
- **Database Configuration** - MongoDB connection
- **Security Configuration** - JWT, OAuth2
- **Payment Configuration** - Gateway credentials
- **Logging Configuration** - Log levels and outputs

> 📝 **Note:** Refer to internal documentation for detailed configuration parameters.

---

## 🎯 Running the Application

### Method 1: Using Gradle (Development)

```bash
./gradlew bootRun
```

### Method 2: Using JAR

```bash
# Build JAR
./gradlew bootJar

# Run JAR
java -jar build/libs/gridee-backend-0.0.1-SNAPSHOT.jar
```

### Method 3: Using Docker

See [Docker Deployment](#-docker-deployment) section below.

### Access the Application

Once started, access:
- **Swagger UI:** `/swagger-ui/index.html`
- **API Docs:** `/v3/api-docs`
- **Health Check:** `/actuator/health`

---

## 📚 API Documentation

### Swagger UI

The application includes interactive API documentation powered by Swagger/OpenAPI.

Access the Swagger UI after starting the application to explore all available endpoints.

### API Collections

API collections are available in the `gridee-backend API/` folder for testing with Bruno or similar tools.

---

## 🐳 Docker Deployment

### Build Docker Image

```bash
docker build -t gridee-backend:latest .
```

### Run with Docker

```bash
docker run -d \
  --name gridee-backend \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_DATA_MONGODB_URI=<your-mongodb-uri> \
  -e JWT_SECRET=<your-jwt-secret> \
  gridee-backend:latest
```

For detailed Docker instructions, see [DOCKER_GUIDE.md](DOCKER_GUIDE.md)

---

## 🔐 Security

### Best Practices

- ✅ All passwords are encrypted using industry-standard algorithms
- ✅ Token-based authentication for API access
- ✅ CSRF protection enabled
- ✅ Input validation on all endpoints
- ✅ Rate limiting recommended for production
- ✅ Secure session management

### Security Configuration

- Configure strong JWT secrets (minimum 32 characters)
- Use HTTPS in production
- Regularly update dependencies
- Implement proper CORS policies
- Use environment variables for sensitive data
- Enable security audit logging

> ⚠️ **Important:** Never expose sensitive configuration details in public repositories.

---

## 🌐 Deployment

### Cloud Platforms

The application can be deployed to various cloud platforms:

- AWS (ECS, Elastic Beanstalk, EC2)
- Google Cloud (Cloud Run, GKE)
- Azure (Container Instances, App Service)
- Other cloud providers

See [DOCKER_GUIDE.md](DOCKER_GUIDE.md) for detailed cloud deployment instructions.

---

## 🗺️ Roadmap

### Upcoming Features
- Advanced analytics dashboard
- Multi-language support
- AI-based parking recommendations
- IoT sensor integration

**Made with ❤️ for Smart Parking Solutions**

---

*Last Updated: October 15, 2025*
