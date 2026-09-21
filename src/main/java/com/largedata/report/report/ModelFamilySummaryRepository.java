package com.largedata.report.report;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.sql.Date;
import java.time.YearMonth;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ModelFamilySummaryRepository {

    private static final String SUMMARY_SQL = """
            SELECT product_group,
                   component_code,
                   COUNT(*) AS record_count,
                   SUM(qty) AS total_qty,
                   SUM(amount) AS total_amount
            FROM public.detail_test
            GROUP BY product_group, component_code
            ORDER BY product_group, component_code
            """;

    private static final String MONTHLY_SUMMARY_SQL = """
            SELECT product_group,
                   component_code,
                   COUNT(*) AS record_count,
                   SUM(qty) AS total_qty,
                   SUM(amount) AS total_amount
            FROM public.detail_test
            WHERE report_date >= ? AND report_date < ?
            GROUP BY product_group, component_code
            ORDER BY product_group, component_code
            """;

    private static final String AVAILABLE_MONTHS_SQL = """
            SELECT date_trunc('month', report_date)::date AS report_month,
                   COUNT(DISTINCT product_group) AS product_group_count,
                   COUNT(*) AS record_count
            FROM public.detail_test
            GROUP BY date_trunc('month', report_date)::date
            ORDER BY report_month
            """;

    private static final String MONTHLY_FAMILIES_SQL = """
            SELECT DISTINCT product_group
            FROM public.detail_test
            WHERE report_date >= ? AND report_date < ?
            ORDER BY product_group
            """;

    private static final String MONTHLY_DATA_SQL = """
            SELECT id, product_group, report_date, created_at, updated_at, component_code, qty, amount,
                   region_code, country_code, facility_code, customer_code, order_no, lot_no,
                   serial_no, status, priority, sales_channel, currency_code, unit_price,
                   discount_amount, tax_amount, net_amount, shipped_qty, pending_qty, is_active
            FROM public.detail_test
            WHERE report_date >= ? AND report_date < ? AND product_group = ?
            ORDER BY report_date DESC, created_at DESC, id
            """;

    private static final List<String> CSV_COLUMNS = List.of(
            "id", "product_group", "report_date", "created_at", "updated_at", "component_code", "qty", "amount",
            "region_code", "country_code", "facility_code", "customer_code", "order_no", "lot_no",
            "serial_no", "status", "priority", "sales_channel", "currency_code", "unit_price",
            "discount_amount", "tax_amount", "net_amount", "shipped_qty", "pending_qty", "is_active");

    private final JdbcTemplate jdbcTemplate;

    public ModelFamilySummaryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<FamilyPartSummary> findAll() {
        return jdbcTemplate.query(SUMMARY_SQL, (resultSet, rowNumber) -> new FamilyPartSummary(
                resultSet.getString("product_group"),
                resultSet.getString("component_code"),
                resultSet.getLong("record_count"),
                resultSet.getLong("total_qty"),
                resultSet.getBigDecimal("total_amount")));
    }

    public List<FamilyPartSummary> findByMonth(YearMonth month) {
        return jdbcTemplate.query(MONTHLY_SUMMARY_SQL,
                statement -> {
                    statement.setDate(1, Date.valueOf(month.atDay(1)));
                    statement.setDate(2, Date.valueOf(month.plusMonths(1).atDay(1)));
                },
                (resultSet, rowNumber) -> new FamilyPartSummary(
                        resultSet.getString("product_group"),
                        resultSet.getString("component_code"),
                        resultSet.getLong("record_count"),
                        resultSet.getLong("total_qty"),
                        resultSet.getBigDecimal("total_amount")));
    }

    public List<AvailableReportMonth> findAvailableMonths() {
        return jdbcTemplate.query(AVAILABLE_MONTHS_SQL, (resultSet, rowNumber) -> {
            YearMonth month = YearMonth.from(resultSet.getDate("report_month").toLocalDate());
            return new AvailableReportMonth(month, resultSet.getInt("product_group_count"), resultSet.getLong("record_count"));
        });
    }

    public List<String> findFamiliesByMonth(YearMonth month) {
        return jdbcTemplate.query(MONTHLY_FAMILIES_SQL,
                statement -> {
                    statement.setDate(1, Date.valueOf(month.atDay(1)));
                    statement.setDate(2, Date.valueOf(month.plusMonths(1).atDay(1)));
                },
                (resultSet, rowNumber) -> resultSet.getString("product_group"));
    }

    public void writeMonthlyFamilyCsv(YearMonth month, String family, Writer writer) {
        writeCsvHeader(writer);
        jdbcTemplate.query(MONTHLY_DATA_SQL,
                statement -> {
                    statement.setDate(1, Date.valueOf(month.atDay(1)));
                    statement.setDate(2, Date.valueOf(month.plusMonths(1).atDay(1)));
                    statement.setString(3, family);
                    statement.setFetchSize(1_000);
                },
                resultSet -> {
                    StringBuilder row = new StringBuilder();
                    for (int columnIndex = 0; columnIndex < CSV_COLUMNS.size(); columnIndex++) {
                        if (columnIndex > 0) {
                            row.append(',');
                        }
                        appendCsvValue(row, resultSet.getObject(CSV_COLUMNS.get(columnIndex)));
                    }
                    write(writer, row.append('\n').toString());
                });
    }

    private void writeCsvHeader(Writer writer) {
        write(writer, String.join(",", CSV_COLUMNS) + "\n");
    }

    private void appendCsvValue(StringBuilder row, Object value) {
        if (value == null) {
            return;
        }
        String text = value.toString();
        boolean needsQuotes = text.indexOf(',') >= 0 || text.indexOf('"') >= 0 || text.indexOf('\n') >= 0 || text.indexOf('\r') >= 0;
        if (needsQuotes) {
            row.append('"').append(text.replace("\"", "\"\"")).append('"');
        } else {
            row.append(text);
        }
    }

    private void write(Writer writer, String text) {
        try {
            writer.write(text);
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to write CSV data", exception);
        }
    }
}
