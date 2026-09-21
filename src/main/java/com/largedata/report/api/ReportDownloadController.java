package com.largedata.report.api;

import com.largedata.report.report.ModelFamilyReportService;
import com.largedata.report.report.MonthlyDownloadPackageService;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@Controller
public class ReportDownloadController {

    private static final DateTimeFormatter YEAR_MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final MediaType ZIP_MEDIA_TYPE = MediaType.parseMediaType("application/zip");

    private final ModelFamilyReportService reportService;
    private final MonthlyDownloadPackageService packageService;

    public ReportDownloadController(
            ModelFamilyReportService reportService,
            MonthlyDownloadPackageService packageService) {
        this.reportService = reportService;
        this.packageService = packageService;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("months", reportService.availableMonths());
        return "index";
    }

    @GetMapping(value = "/downloads/package.zip", produces = "application/zip")
    public ResponseEntity<StreamingResponseBody> downloadPackage(@RequestParam String month) throws java.io.IOException {
        YearMonth reportMonth = parseMonth(month);
        Path archive = packageService.createPackage(reportMonth);
        StreamingResponseBody body = outputStream -> {
            try (InputStream input = Files.newInputStream(archive)) {
                input.transferTo(outputStream);
            }
        };
        return ResponseEntity.ok()
                .contentType(ZIP_MEDIA_TYPE)
                .contentLength(Files.size(archive))
                .header(HttpHeaders.CONTENT_DISPOSITION, attachment("largedata-" + reportMonth + ".zip"))
                .body(body);
    }

    private YearMonth parseMonth(String value) {
        try {
            return YearMonth.parse(value, YEAR_MONTH_FORMAT);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("month must be in yyyy-MM format", exception);
        }
    }

    private String attachment(String filename) {
        return ContentDisposition.attachment().filename(filename).build().toString();
    }
}
