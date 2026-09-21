# Large Data Report API

Spring Boot + Thymeleaf application for generating a monthly Power Query download package from PostgreSQL.

## Download package

The UI creates one ZIP per selected month. Each ZIP contains:

```text
largedata-YYYY-MM/
├── LargeData_Template.xlsx
└── model-family/
    └── Family_*.csv
```

Do not move `LargeData_Template.xlsx` away from its sibling `model-family` folder after extraction. The workbook's Power Query uses their relative path.

CSV files are generated one Product Group at a time under the ignored `download/.staging-*` directory. The final ZIP remains in `download/`; staging files are removed after compression. This prevents a complete month's data from being retained in JVM memory.

The CSV column names use `product_group`, `component_code`, `facility_code`, and `pending_qty`. The CSV filenames remain `Family_*.csv` so the existing Power Query selector can continue to match filenames.

## Start

```powershell
mvn spring-boot:run
```

Open `http://localhost:8080`.

## Database scripts

See [sql/README.md](sql/README.md). Run the schema and regeneration scripts in order against the new `public.detail_test` table.
