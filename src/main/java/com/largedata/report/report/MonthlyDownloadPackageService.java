package com.largedata.report.report;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class MonthlyDownloadPackageService {

    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final ModelFamilySummaryRepository repository;
    private final Path downloadDirectory;

    public MonthlyDownloadPackageService(
            ModelFamilySummaryRepository repository,
            @Value("${app.download-directory:download}") String downloadDirectory) {
        this.repository = repository;
        this.downloadDirectory = Path.of(downloadDirectory).toAbsolutePath().normalize();
    }

    public Path createPackage(YearMonth month) throws IOException {
        Files.createDirectories(downloadDirectory);
        Path stagingDirectory = Files.createTempDirectory(downloadDirectory, ".staging-" + month + "-");
        String packageFolderName = "largedata-" + month;
        Path packageFolder = stagingDirectory.resolve(packageFolderName);
        Path csvDirectory = packageFolder.resolve("model-family");

        try {
            Files.createDirectories(csvDirectory);
            copyPowerQueryTemplate(packageFolder.resolve("LargeData_Template.xlsx"));

            List<String> families = repository.findFamiliesByMonth(month);
            for (String family : families) {
                Path csvFile = csvDirectory.resolve(family + ".csv");
                try (BufferedWriter writer = Files.newBufferedWriter(csvFile, StandardCharsets.UTF_8)) {
                    repository.writeMonthlyFamilyCsv(month, family, writer);
                }
            }

            Path temporaryZip = Files.createTempFile(downloadDirectory, ".package-", ".zip");
            try {
                zipDirectory(stagingDirectory, temporaryZip);
                String filename = "largedata-" + month + "-" + FILE_TIMESTAMP.format(java.time.LocalDateTime.now()) + ".zip";
                Path finalZip = downloadDirectory.resolve(filename);
                return Files.move(temporaryZip, finalZip, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(temporaryZip);
            }
        } finally {
            deleteRecursively(stagingDirectory);
        }
    }

    private void copyPowerQueryTemplate(Path destination) throws IOException {
        ClassPathResource template = new ClassPathResource("LargeData_Template.xlsx");
        try (InputStream input = template.getInputStream()) {
            Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void zipDirectory(Path sourceDirectory, Path destination) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(destination), StandardCharsets.UTF_8)) {
            try (var paths = Files.walk(sourceDirectory)) {
                for (Path path : paths.filter(Files::isRegularFile).toList()) {
                    String entryName = sourceDirectory.relativize(path).toString().replace('\\', '/');
                    zip.putNextEntry(new ZipEntry(entryName));
                    Files.copy(path, zip);
                    zip.closeEntry();
                }
            }
        }
    }

    private void deleteRecursively(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        try (var paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }
}
