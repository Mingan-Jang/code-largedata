package com.largedata.report.report;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.stereotype.Service;

@Service
public class ModelFamilyReportService {

    private final ModelFamilySummaryRepository repository;
    private final ReportExcelWriter excelWriter;

    public ModelFamilyReportService(ModelFamilySummaryRepository repository, ReportExcelWriter excelWriter) {
        this.repository = repository;
        this.excelWriter = excelWriter;
    }

    public byte[] createWorkbook() {
        List<FamilyPartSummary> summaries = repository.findAll();
        return excelWriter.write(summaries);
    }

    public List<AvailableReportMonth> availableMonths() {
        return repository.findAvailableMonths();
    }

    public byte[] createWorkbook(YearMonth month) {
        return excelWriter.write(repository.findByMonth(month), "Model Family Summary - " + month);
    }

    public void writeMonthlyDataZip(YearMonth month, OutputStream outputStream) throws IOException {
        List<String> families = repository.findFamiliesByMonth(month);
        try (ZipOutputStream zip = new ZipOutputStream(outputStream, StandardCharsets.UTF_8)) {
            String folderName = month + "/";
            zip.putNextEntry(new ZipEntry(folderName));
            zip.closeEntry();

            for (String family : families) {
                zip.putNextEntry(new ZipEntry(folderName + family + ".csv"));
                Writer writer = new OutputStreamWriter(zip, StandardCharsets.UTF_8);
                repository.writeMonthlyFamilyCsv(month, family, writer);
                writer.flush();
                zip.closeEntry();
            }
        }
    }
}
