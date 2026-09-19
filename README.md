# Large Data Report API

Spring Boot + Thymeleaf application for generating monthly Power Query download packages from PostgreSQL.

## Download package

The UI creates one ZIP per selected month. Each ZIP contains:

```text
largedata-YYYY-MM/
├── LargeData_Template.xlsx
└── model-family/
    └── Family_*.csv
```

Keep `LargeData_Template.xlsx` beside the `model-family` folder after extraction because the workbook uses a relative Power Query path. CSV files are generated one Product Group at a time on disk to keep JVM memory bounded.

## Start

```powershell
mvn spring-boot:run
```

Open `http://localhost:8080`.

## Database scripts

See [sql/README.md](sql/README.md). Run the scripts in order against PostgreSQL database `largedata`. The regeneration script recreates 3,000,000 deterministic rows for 2026 and intentionally replaces existing detail data.
