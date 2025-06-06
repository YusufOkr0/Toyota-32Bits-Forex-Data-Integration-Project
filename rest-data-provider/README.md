<div align="center">
  <h1>REST FOREX DATA PROVIDER</h1>

<img alt="Java" src="https://img.shields.io/badge/Java-007396.svg?style=flat&logo=java&logoColor=white" class="inline-block mx-1" style="margin: 0px 2px;">
<img alt="Redis" src="https://img.shields.io/badge/Redis-FF4438.svg?style=flat&logo=Redis&logoColor=white" class="inline-block mx-1" style="margin: 0px 2px;">
<img alt="PostgreSQL" src="https://img.shields.io/badge/PostgreSQL-4169E1.svg?style=flat&logo=postgresql&logoColor=white" class="inline-block mx-1" style="margin: 0px 2px;">
<img alt="Spring Boot" src="https://img.shields.io/badge/Spring%20Boot-6DB33F.svg?style=flat&logo=springboot&logoColor=white" class="inline-block mx-1" style="margin: 0px 2px;">

<br>
<br>

> RESTful API for authenticated access to real-time Forex rate data, supporting rate limiting and dynamic pricing plans.

This is a Spring Boot application that exposes secure, authenticated REST endpoints for querying real-time FX rate data. <br>
It supports rate limiting per user based on their pricing plan, JWT-based authentication, and Redis-backed rate storage. <br>
The system allows users to register, log in, and subscribe to different rate plans that define how many requests they can make per minute.

</div>

---

### Rate Limiting with Bucket4J + Redis

* Each user is associated with a **bucket** that defines how many requests they can make per minute.
* Buckets are created dynamically when the user first makes a request and are stored using **Bucket4J Redis proxy**.
* The limits are determined by the user's pricing plan (e.g., `STANDARD`, `PREMIUM`).
* If the limit is exceeded, the system responds with a **429 Too Many Requests** error.
* Buckets are removed when users are deleted or pricing plans change.

---

## ️ Rate Limiting Strategy

* Identify the user by JWT token.
* Check if their request bucket has available tokens.
* Reject with 429 if the user exceeds their plan limit.

Rate limits per plan:

| Plan       | Requests/Minute |
| ---------- | --------------- |
| STANDARD   | 20              |
| PREMIUM    | 60              |

---



## API Usage

> Explore and test all endpoints via Swagger UI.

### Swagger UI

```
http://localhost:8092/swagger-ui/index.html
```

#### Available Symbols:
| Symbol | Description               |
| ------ | ------------------------- |
| USDTRY | US Dollar / Turkish Lira  |
| EURUSD | Euro / US Dollar          |
| GBPUSD | British Pound / US Dollar |


## Contact

Yusuf Okur – **[yusufokr0@gmail.com](mailto:yusufokr0@gmail.com)**

---
