# 🛠️ MySQL Schema Crawler

A Spring Boot-based tool to extract MySQL schema metadata, generate Java model classes dynamically, and expose schema operations via RESTful APIs.

---

## 🧱 Architecture

```
┌──────────────┐       ┌───────────────┐
│ Configuration│─────▶│ DB Connection │
└──────────────┘       └──────┬────────┘
                              │
                    ┌────────▼────────┐
                    │ MetadataService │
                    └────────┬────────┘
                             │
                ┌────────────▼────────────┐
                │   Metadata Extraction   │
                └────────────┬────────────┘
                             │
                    ┌───────▼────────┐
                    │ Model Generator │
                    └───────┬─────────┘
                             │
            ┌────────────────▼─────────────┐
            │      Java Model Classes      │
            └────────────┬─────────────────┘
                         │
                ┌────────▼────────┐
                │    REST API     │
                └─────────────────┘
```

---

## ✅ Features

- 🔌 Connects to MySQL using JDBC
- 📊 Extracts schema metadata:
  - Tables
  - Columns (type, length, nullable)
  - Primary Keys
  - Foreign Keys
  - Indexes
- 🔁 Handles complex relationships (e.g., many-to-many)
- 🧬 Generates Java model classes dynamically
- 🌐 Exposes REST APIs for metadata access and model generation
- ⚙️ Configurable via `schema-config.json`

---

## ⚙️ Configuration

Create a file named `application.properties` (or `schema-config.json` if preferred):

```json
{
  "host": "localhost",
  "port": 3306,
  "database": "your_database",
  "username": "root",
  "password": "your_password"
}
```

---

## 🔄 Workflow

1. **Connect to Database**
   - Uses JDBC to establish the connection.

2. **Extract Schema Metadata**
   - Collects:
     - Tables
     - Columns
     - Primary Keys
     - Foreign Keys
     - Indexes

3. **Generate Java Models**
   - Dynamically generates Java class files (e.g., `Student.java`, `Enrollment.java`)
   - Supports relationship mapping

4. **Expose via REST API**
   - View schema metadata
   - Trigger model class generation

---

## 🌐 REST API Reference

| Method | Endpoint                          | Description                         |
|--------|-----------------------------------|-------------------------------------|
| GET    | `/api/schema/tables`              | List all tables                     |
| GET    | `/api/schema/columns/{table}`     | Get columns for a specific table    |
| GET    | `/api/schema/relationships`       | View foreign key relationships      |
| POST   | `/api/schema/generate-models`     | Trigger Java model generation       |

---

## 🧪 Example Schema: `university_db`

**Tables**

| Table     | Columns                      |
|-----------|------------------------------|
| student   | `id`, `name`, `email`        |
| course    | `id`, `title`, `credits`     |
| enrollment| `student_id`, `course_id`, `grade` |

**Relationships**

- `enrollment.student_id` → `student.id`
- `enrollment.course_id` → `course.id`

---

## 📦 Build & Run

```bash
# Build
./mvnw clean install

# Run
./mvnw spring-boot:run
```

> Ensure MySQL is running and accessible with the credentials provided in your config.

---

## 🧩 License

MIT License. Use freely, contribute kindly. ❤️

---
