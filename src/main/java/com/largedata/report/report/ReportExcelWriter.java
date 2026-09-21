package com.largedata.report.report;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

@Component
public class ReportExcelWriter {

    private static final String INDEX_SHEET_NAME = "Index";

    public byte[] write(List<FamilyPartSummary> summaries) {
        return write(summaries, "Product Group Report Index");
    }

    public byte[] write(List<FamilyPartSummary> summaries, String reportTitle) {
        Map<String, List<FamilyPartSummary>> summariesByFamily = summaries.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        FamilyPartSummary::productGroup,
                        LinkedHashMap::new,
                        java.util.stream.Collectors.toList()));

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle amountStyle = createAmountStyle(workbook);
            Map<String, String> sheetNames = createFamilySheets(workbook, summariesByFamily, headerStyle, amountStyle);
            createIndexSheet(workbook, summariesByFamily, sheetNames, headerStyle, amountStyle, reportTitle);
            workbook.setActiveSheet(workbook.getSheetIndex(INDEX_SHEET_NAME));
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to create XLSX report", exception);
        }
    }

    private Map<String, String> createFamilySheets(
            Workbook workbook,
            Map<String, List<FamilyPartSummary>> summariesByFamily,
            CellStyle headerStyle,
            CellStyle amountStyle) {
        Map<String, String> sheetNames = new LinkedHashMap<>();
        for (Map.Entry<String, List<FamilyPartSummary>> entry : summariesByFamily.entrySet()) {
            String sheetName = uniqueSheetName(workbook, entry.getKey() + " Summary");
            sheetNames.put(entry.getKey(), sheetName);
            Sheet sheet = workbook.createSheet(sheetName);

            Row title = sheet.createRow(0);
            title.createCell(0).setCellValue(entry.getKey() + " Summary");
            createDocumentLink(workbook, title.createCell(5), "Back to Index", INDEX_SHEET_NAME);

            Row header = sheet.createRow(1);
            writeHeader(header, headerStyle, "Component", "Record Count", "Total Qty", "Total Amount");

            int rowIndex = 2;
            for (FamilyPartSummary summary : entry.getValue()) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(summary.componentCode());
                row.createCell(1).setCellValue(summary.recordCount());
                row.createCell(2).setCellValue(summary.totalQty());
                Cell amount = row.createCell(3);
                amount.setCellValue(summary.totalAmount().doubleValue());
                amount.setCellStyle(amountStyle);
            }
            setSummaryWidths(sheet);
        }
        return sheetNames;
    }

    private void createIndexSheet(
            Workbook workbook,
            Map<String, List<FamilyPartSummary>> summariesByFamily,
            Map<String, String> sheetNames,
            CellStyle headerStyle,
            CellStyle amountStyle,
            String reportTitle) {
        Sheet indexSheet = workbook.createSheet(INDEX_SHEET_NAME);
        Row title = indexSheet.createRow(0);
        title.createCell(0).setCellValue(reportTitle);
        Row header = indexSheet.createRow(1);
        writeHeader(header, headerStyle, "Product Group", "Components", "Record Count", "Total Qty", "Total Amount", "Shortcut");

        int rowIndex = 2;
        for (Map.Entry<String, List<FamilyPartSummary>> entry : summariesByFamily.entrySet()) {
            List<FamilyPartSummary> familyRows = entry.getValue();
            long recordCount = familyRows.stream().mapToLong(FamilyPartSummary::recordCount).sum();
            long totalQty = familyRows.stream().mapToLong(FamilyPartSummary::totalQty).sum();
            BigDecimal totalAmount = familyRows.stream()
                    .map(FamilyPartSummary::totalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Row row = indexSheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(entry.getKey());
            row.createCell(1).setCellValue(familyRows.size());
            row.createCell(2).setCellValue(recordCount);
            row.createCell(3).setCellValue(totalQty);
            Cell amount = row.createCell(4);
            amount.setCellValue(totalAmount.doubleValue());
            amount.setCellStyle(amountStyle);
            createDocumentLink(workbook, row.createCell(5), "Open Summary", sheetNames.get(entry.getKey()));
        }
        setIndexWidths(indexSheet);
    }

    private void createDocumentLink(Workbook workbook, Cell cell, String text, String targetSheet) {
        CreationHelper helper = workbook.getCreationHelper();
        Hyperlink hyperlink = helper.createHyperlink(HyperlinkType.DOCUMENT);
        hyperlink.setAddress("#'" + targetSheet.replace("'", "''") + "'!A1");
        cell.setCellValue(text);
        cell.setHyperlink(hyperlink);
    }

    private String uniqueSheetName(Workbook workbook, String preferredName) {
        String baseName = WorkbookUtil.createSafeSheetName(preferredName);
        String candidate = baseName;
        int suffix = 2;
        while (workbook.getSheet(candidate) != null) {
            String suffixText = " (" + suffix++ + ")";
            candidate = baseName.substring(0, Math.min(baseName.length(), 31 - suffixText.length())) + suffixText;
        }
        return candidate;
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        return style;
    }

    private CellStyle createAmountStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));
        return style;
    }

    private void writeHeader(Row row, CellStyle headerStyle, String... titles) {
        for (int columnIndex = 0; columnIndex < titles.length; columnIndex++) {
            Cell cell = row.createCell(columnIndex);
            cell.setCellValue(titles[columnIndex]);
            cell.setCellStyle(headerStyle);
        }
    }

    private void setSummaryWidths(Sheet sheet) {
        sheet.setColumnWidth(0, 20 * 256);
        sheet.setColumnWidth(1, 14 * 256);
        sheet.setColumnWidth(2, 14 * 256);
        sheet.setColumnWidth(3, 16 * 256);
        sheet.setColumnWidth(5, 16 * 256);
    }

    private void setIndexWidths(Sheet sheet) {
        sheet.setColumnWidth(0, 24 * 256);
        sheet.setColumnWidth(1, 12 * 256);
        sheet.setColumnWidth(2, 14 * 256);
        sheet.setColumnWidth(3, 14 * 256);
        sheet.setColumnWidth(4, 16 * 256);
        sheet.setColumnWidth(5, 18 * 256);
    }
}
