# PostgreSQL sample data setup

Run these scripts in order against database `largedata`:

1. `001_largedata_schema.sql` creates the `public.detail_test` table and indexes. It does not create a View.
2. `002_regenerate_three_million_monthly_detail.sql` **deletes existing detail data** and creates 3,000,000 deterministic rows for 2026.

The generation script creates a different number of actual Model Families per month, from 20 to 36. It is intentionally destructive so the generated dataset is reproducible.

`region_code` uses administrative-area codes: Taiwan cities/counties (`TW-TPE`, `TW-NWT`, etc.), Japanese prefectures (`JP-13`, `JP-27`, etc.), US states, German states, and Singapore planning regions.

Example with the `test` Podman container:

```powershell
Get-Content -Raw .\sql\001_largedata_schema.sql | podman exec -i test psql -v ON_ERROR_STOP=1 -U postgres -d largedata
Get-Content -Raw .\sql\002_regenerate_three_million_monthly_detail.sql | podman exec -i test psql -v ON_ERROR_STOP=1 -U postgres -d largedata
```
