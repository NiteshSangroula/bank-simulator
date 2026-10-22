# 💰 Finance Tracker API

A **Spring Boot REST API** that simulates core banking operations such as  
account management, deposits, withdrawals, and secure money transfers.

Built as a **real-world backend project** focusing on transactional safety,
clean architecture, pagination, filtering, API documentation, and automated testing.

---

## 🚀 Project Overview

Finance Tracker API provides a backend system capable of handling financial
operations while maintaining **data consistency** and **transaction integrity**.

The system ensures:

- Safe balance updates
- Atomic money transfers
- Transaction history tracking
- Reliable rollback on failures
- Fully tested backend workflows

---

## ✨ Features

### 🏦 Account Management
- Create accounts
- Retrieve account details
- View all accounts
- Delete accounts

---

### 💸 Transaction System
- Deposit money
- Withdraw money with balance validation
- Transfer funds between accounts
- Automatic rollback on failed transfers
- Transaction history tracking

---

### 📄 Pagination & Filtering
- Paginated transaction history
- Filter transactions by account
- Offset-based pagination using custom SQL queries

---

### 🔒 Data Integrity & Validation
- `@Transactional` money transfers
- Insufficient balance protection
- Account existence validation
- Request validation using Jakarta Validation
- Custom exception handling

---

### 📘 API Documentation
- Integrated **Swagger / OpenAPI UI**
- Interactive endpoint testing directly from browser

Swagger UI:
```
http://localhost:8080/swagger-ui.html
```

---

### 🧪 Testing
- Service layer unit tests (Mockito)
- Controller & database integration tests
- Transaction rollback verification
- Real database interaction testing

---

## 🧱 Architecture

Layered architecture following backend best practices:
```
Controller → Service → Repository → Database
```

### Layers
```
controller → REST endpoints
service → Business logic & transactions
repository → Spring Data JDBC + SQL queries
entity → Database models
dto → Request / response models
exception → Custom exceptions
config → Application configuration
```

---

## 🛠 Tech Stack

| Technology | Purpose |
|------------|---------|
| Java 21 | Core language |
| Spring Boot | Backend framework |
| Spring Data JDBC | Persistence layer |
| H2 Database | Development database |
| Swagger / OpenAPI | API documentation |
| Maven | Build tool |
| JUnit 5 | Testing |
| Mockito | Unit testing |
| MockMvc | Integration testing |

---

## 📂 Project Structure
```
src/main/java
├── controller
├── service
├── repository
├── entity
├── dto
├── exception
└── config
```

---

## ▶️ Running Locally

### 1️⃣ Clone Repository

```bash
git clone https://github.com/NiteshSangroula/FinanceTrackerAPI.git
cd FinanceTrackerAPI
```

---

### 2️⃣ Run Application
```
./mvnw spring-boot:run
```
Application starts at:
```
http://localhost:8080
```

---

## 🗄 Database

Default configuration uses an H2 in-memory database.

H2 Console
```
http://localhost:8080/h2-console
```
Configuration:
```
JDBC URL: jdbc:h2:mem:testdb
User: sa
Password:
```

---

## 📡 REST API Endpoints
### Account APIs
```
POST   /api/accounts
GET    /api/accounts
GET    /api/accounts/{id}
DELETE /api/accounts/{id}
```

---

### Transaction APIs
```
POST /api/transactions/deposit
POST /api/transactions/withdraw
POST /api/transactions/transfer
GET  /api/transactions
GET  /api/transactions/{id}
GET  /api/transactions/account/{accountId}
```

---

## 📄 Pagination Example
GET /api/transactions?size=10&offset=0

Filter by account:

GET /api/transactions/account/{accountId}?size=10&offset=0

---

## 🧪 Running Tests

### Execute all tests:
```
./mvnw test
```

### Test Coverage

- ✅ Deposit operations
- ✅ Withdraw operations
- ✅ Transfer success
- ✅ Transfer rollback on failure
- ✅ Account validation
- ✅ Integration testing with real database

---

## ⚙️ Key Backend Concepts Implemented

- Transaction Management (@Transactional)
- ACID-like financial consistency
- Repository Pattern
- DTO-based API design
- Custom SQL with Spring Data JDBC
- Pagination & filtering
- Integration Testing
- Exception-driven validation
- REST API documentation

---

## 📈 Project Status

- ✅ Backend fully implemented
- ✅ Pagination & filtering added
- ✅ Swagger documentation integrated
- ✅ Unit & integration tests completed

---

## 🔮 Future Improvements

- PostgreSQL migration
- Docker containerization
- Authentication & Authorization (JWT)
- Cloud deployment
- Frontend client (React / Angular)

---

## 🎯 Learning Outcomes

### Through this project:
- Designed a transaction-safe financial backend
- Implemented real service-layer business logic
- Practiced database consistency handling
- Built and tested REST APIs end-to-end
- Learned integration testing strategies

---

## 👨‍💻 Author

Nitesh Sangroula

GitHub:
https://github.com/NiteshSangroula

---

⭐ Project Note

This is the first complete end-to-end backend project
built from design → implementation → testing → documentation
as part of a structured Spring Boot learning journey.

