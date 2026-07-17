package com.forecastpro.controller;

import com.forecastpro.dto.CategorySalesRow;
import com.forecastpro.dto.ForecastDashboardKpis;
import com.forecastpro.dto.MonthlyProductSalesRow;
import com.forecastpro.dto.MonthlySalesRow;
import com.forecastpro.dto.ProductSalesRow;
import com.forecastpro.dto.SeasonalityRow;
import com.forecastpro.service.ForecastService;
import com.forecastpro.service.InventoryService;
import com.forecastpro.service.SeasonalityService;
import com.forecastpro.util.DisplayFormats;
import com.forecastpro.entity.Category;
import com.forecastpro.entity.Forecast;
import com.forecastpro.entity.InventoryMovement;
import com.forecastpro.entity.Product;
import com.forecastpro.service.CategoryService;
import com.forecastpro.service.SecurityService;
import com.forecastpro.service.SalesReportService;
import com.forecastpro.service.ProductService;
import com.forecastpro.repository.ForecastRepository;
import com.forecastpro.repository.InventoryMovementRepository;
import com.forecastpro.repository.SaleRepository;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.primefaces.model.charts.ChartData;
import org.primefaces.model.charts.bar.BarChartDataSet;
import org.primefaces.model.charts.bar.BarChartModel;
import org.primefaces.model.charts.line.LineChartDataSet;
import org.primefaces.model.charts.line.LineChartModel;
import org.primefaces.model.charts.pie.PieChartDataSet;
import org.primefaces.model.charts.pie.PieChartModel;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Named("reportsBean")
@ViewScoped
public class ReportsBean implements Serializable {

    public enum ReportType {
        SALES,
        INVENTORY,
        FORECAST,
        PRODUCT_PERFORMANCE,
        CATEGORY,
        SEASONALITY,
        ALL
    }

    public enum TimeMode {
        MONTHLY,
        YEARLY,
        RANGE
    }

    @Inject
    private UserSessionBean userSession;

    @Inject
    private SecurityService securityService;

    @Inject
    private CategoryService categoryService;

    @Inject
    private ProductService productService;

    @Inject
    private SalesReportService salesReportService;

    @Inject
    private SaleRepository saleRepository;

    @Inject
    private ForecastRepository forecastRepository;

    @Inject
    private InventoryMovementRepository inventoryMovementRepository;

    @Inject
    private InventoryService inventoryService;

    @Inject
    private ForecastService forecastService;

    @Inject
    private SeasonalityService seasonalityService;

    private ReportType selectedReport = ReportType.SALES;
    private TimeMode timeMode = TimeMode.RANGE;

    private int selectedYear;
    private int selectedMonth; // 1-12
    private LocalDate fromDate;
    private LocalDate toDate;

    private List<Category> categories = new ArrayList<>();
    private Long selectedCategoryId;
    private List<Product> products = new ArrayList<>();
    private Long selectedProductId;

    // KPIs
    private BigDecimal kpiRevenue = BigDecimal.ZERO;
    private long kpiUnits = 0;
    private long kpiRows = 0;

    // Sales
    private List<MonthlyProductSalesRow> salesRows = new ArrayList<>();

    // Forecast
    private List<Forecast> forecastRows = new ArrayList<>();

    // Inventory
    private List<InventoryMovement> inventoryRows = new ArrayList<>();

    // Product/Category performance
    private List<ProductSalesRow> productPerfRows = new ArrayList<>();
    private List<CategorySalesRow> categoryRows = new ArrayList<>();

    // Seasonality
    private List<SeasonalityRow> seasonalityRows = new ArrayList<>();

    // Forecast dashboard KPIs
    private ForecastDashboardKpis forecastKpis = new ForecastDashboardKpis();

    // Charts (3-up)
    private LineChartModel lineModel;
    private BarChartModel barModel;
    private PieChartModel pieModel;

    @PostConstruct
    public void init() {
        // Page security (even if URL is hit directly)
        try {
            securityService.requireAdminOrSalesManager(userSession.getRole());
        } catch (Exception e) {
            redirectDenied();
            return;
        }

        categories = new ArrayList<>(categoryService.findAllForUi(userSession.getRole()));
        selectedYear = LocalDate.now().getYear();
        selectedMonth = LocalDate.now().getMonthValue();
        // Default: last 12 months
        toDate = LocalDate.now();
        fromDate = LocalDate.now().minusMonths(11).withDayOfMonth(1);
        refreshProducts();

        String reportCode = resolveReportCodeFromViewId();
        if (reportCode != null) {
            openReport(reportCode);
        } else if (isHubView()) {
            loadHubOverview();
        } else {
            ensureChartModels();
        }
    }

    /** Hub page: show combined overview below report tiles. */
    public void loadHubOverview() {
        selectedReport = ReportType.ALL;
        try {
            refresh();
        } catch (RuntimeException e) {
            addLoadError(e);
        }
        ensureChartModels();
    }

    private boolean isHubView() {
        String viewId = currentViewId();
        return viewId != null && viewId.endsWith("/reports/reports.xhtml");
    }

    private String resolveReportCodeFromViewId() {
        String viewId = currentViewId();
        if (viewId == null) {
            return null;
        }
        if (viewId.contains("sales-report")) {
            return "SALES";
        }
        if (viewId.contains("inventory-report")) {
            return "INVENTORY";
        }
        if (viewId.contains("forecast-report")) {
            return "FORECAST";
        }
        if (viewId.contains("product-performance-report")) {
            return "PRODUCT_PERFORMANCE";
        }
        if (viewId.contains("category-report")) {
            return "CATEGORY";
        }
        if (viewId.contains("seasonality-report")) {
            return "SEASONALITY";
        }
        return null;
    }

    private static String currentViewId() {
        FacesContext fc = FacesContext.getCurrentInstance();
        if (fc == null || fc.getViewRoot() == null) {
            return null;
        }
        return fc.getViewRoot().getViewId();
    }

    private void ensureChartModels() {
        if (lineModel == null) {
            lineModel = Charts.emptyLine();
        }
        if (barModel == null) {
            barModel = Charts.emptyBar();
        }
        if (pieModel == null) {
            pieModel = Charts.emptyPie();
        }
    }

    private void addLoadError(RuntimeException e) {
        FacesContext fc = FacesContext.getCurrentInstance();
        if (fc != null) {
            fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Could not load report data. See server log.", e.getMessage()));
        }
    }

    /** Called from each report detail page via f:viewAction (backup if PostConstruct view id unavailable). */
    public void openReport(String code) {
        securityService.requireAdminOrSalesManager(userSession.getRole());
        if (code == null || code.isBlank()) {
            selectedReport = ReportType.SALES;
        } else {
            selectedReport = ReportType.valueOf(code.trim());
        }
        try {
            refresh();
        } catch (RuntimeException e) {
            addLoadError(e);
        }
        ensureChartModels();
    }

    private void redirectDenied() {
        FacesContext fc = FacesContext.getCurrentInstance();
        if (fc == null) return;
        ExternalContext ec = fc.getExternalContext();
        try {
            ec.redirect(ec.getRequestContextPath() + "/access-denied.xhtml");
            fc.responseComplete();
        } catch (IOException ignored) {
        }
    }

    public boolean reportShows(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        return selectedReport == ReportType.valueOf(code.trim());
    }

    public boolean isMonthlyMode() {
        return timeMode == TimeMode.MONTHLY;
    }

    public boolean isYearlyMode() {
        return timeMode == TimeMode.YEARLY;
    }

    public boolean isRangeMode() {
        return timeMode == TimeMode.RANGE;
    }

    public void onSelectReport(String code) {
        if (code == null || code.isBlank()) {
            selectedReport = ReportType.ALL;
        } else {
            selectedReport = ReportType.valueOf(code.trim());
        }
        refresh();
    }

    public boolean reportSelected(String code) {
        if (code == null || code.isBlank()) {
            return selectedReport == ReportType.ALL;
        }
        return selectedReport == ReportType.valueOf(code.trim());
    }

    public void onTimeModeChange() {
        syncDateRangeFromTimeMode();
    }

    public void onMonthYearChange() {
        if (timeMode == TimeMode.MONTHLY) {
            syncDateRangeFromTimeMode();
        }
    }

    public void onYearChange() {
        if (timeMode == TimeMode.YEARLY) {
            syncDateRangeFromTimeMode();
        }
    }

    public void applyFilters() {
        if (selectedReport != ReportType.SEASONALITY) {
            if (timeMode != TimeMode.RANGE) {
                syncDateRangeFromTimeMode();
            } else {
                normalizeCustomRange();
            }
        }
        refresh();
    }

    private void normalizeCustomRange() {
        if (fromDate == null) {
            fromDate = LocalDate.now().minusMonths(11).withDayOfMonth(1);
        }
        if (toDate == null) {
            toDate = LocalDate.now();
        }
        if (fromDate.isAfter(toDate)) {
            LocalDate swap = fromDate;
            fromDate = toDate;
            toDate = swap;
        }
    }

    private void syncDateRangeFromTimeMode() {
        if (timeMode == TimeMode.MONTHLY) {
            YearMonth ym = YearMonth.of(selectedYear, Math.max(1, Math.min(12, selectedMonth)));
            fromDate = ym.atDay(1);
            toDate = ym.atEndOfMonth();
        } else if (timeMode == TimeMode.YEARLY) {
            fromDate = LocalDate.of(selectedYear, 1, 1);
            toDate = LocalDate.of(selectedYear, 12, 31);
        } else {
            normalizeCustomRange();
        }
    }

    public void onCategoryChange() {
        selectedProductId = null;
        refreshProducts();
    }

    private void refreshProducts() {
        products = new ArrayList<>();
        if (selectedCategoryId != null) {
            products = new ArrayList<>(productService.listByCategory(userSession.getRole(), selectedCategoryId));
        }
    }

    public void refresh() {
        securityService.requireAdminOrSalesManager(userSession.getRole());
        LocalDate from = fromDate != null ? fromDate : LocalDate.now().minusMonths(11).withDayOfMonth(1);
        LocalDate to = toDate != null ? toDate : LocalDate.now();

        // SALES rows (monthly buckets in selected date range)
        salesRows = salesReportService.monthlySales(userSession.getRole(), selectedCategoryId, selectedProductId, from, to);
        // PERFORMANCE rows (date range)
        productPerfRows = mapProductPerf(saleRepository.productRevenueRangeNative(selectedCategoryId, selectedProductId, from, to));
        categoryRows = mapCategory(saleRepository.categoryRevenueRangeNative(from, to));

        LocalDate forecastFrom = YearMonth.from(from).atDay(1);
        LocalDate forecastTo = YearMonth.from(to).atEndOfMonth();
        // FORECAST rows (date range on forecastMonth)
        forecastRows = forecastRepository.findByFilters(forecastFrom, forecastTo, selectedCategoryId, selectedProductId, 500);

        // INVENTORY rows (date range on dateAdded)
        inventoryRows = inventoryMovementRepository.findByFilters(from, to, selectedCategoryId, selectedProductId, 500);

        // Seasonality
        seasonalityRows = seasonalityService.seasonalityReport(userSession.getRole(),
                selectedCategoryId, selectedProductId);

        forecastKpis = forecastService.computeDashboardKpis(userSession.getRole());

        // KPIs from currently selected report (simple + consistent)
        computeKpisFor(selectedReport, from, to);
        buildChartsFor(selectedReport, from, to);
        ensureChartModels();
    }

    private void computeKpisFor(ReportType type, LocalDate from, LocalDate to) {
        kpiRevenue = BigDecimal.ZERO;
        kpiUnits = 0;
        kpiRows = 0;

        ReportType t = type != null ? type : ReportType.ALL;
        if (t == ReportType.SALES || t == ReportType.ALL || t == ReportType.PRODUCT_PERFORMANCE || t == ReportType.CATEGORY) {
            for (ProductSalesRow r : productPerfRows) {
                kpiRevenue = kpiRevenue.add(r.getRevenue() != null ? r.getRevenue() : BigDecimal.ZERO);
                kpiUnits += r.getQuantitySold();
            }
            kpiRows = salesRows.size();
            return;
        }
        if (t == ReportType.FORECAST) {
            kpiRevenue = forecastKpis.getForecastedRevenue();
            kpiUnits = forecastKpis.getTotalPredictedUnits();
            kpiRows = forecastRows.size();
            return;
        }
        if (t == ReportType.SEASONALITY) {
            for (ProductSalesRow r : productPerfRows) {
                kpiRevenue = kpiRevenue.add(r.getRevenue() != null ? r.getRevenue() : BigDecimal.ZERO);
                kpiUnits += r.getQuantitySold();
            }
            kpiRows = seasonalityRows.size();
            return;
        }
        if (t == ReportType.INVENTORY) {
            for (InventoryMovement i : inventoryRows) {
                if (i.getQuantityAdded() != null) {
                    kpiUnits += i.getQuantityAdded();
                }
            }
            kpiRows = inventoryRows.size();
        }
    }

    private void buildChartsFor(ReportType type, LocalDate from, LocalDate to) {
        ReportType t = type != null ? type : ReportType.ALL;
        if (t == ReportType.INVENTORY) {
            lineModel = Charts.inventoryTrend(inventoryRows);
            barModel = Charts.inventoryByProduct(inventoryRows);
            pieModel = Charts.inventoryBySource(inventoryRows, inventoryService::inventoryVendorLabel);
            return;
        }
        if (t == ReportType.FORECAST) {
            lineModel = Charts.forecastRevenueTrend(forecastRows);
            barModel = Charts.forecastRevenueByProduct(forecastRows);
            pieModel = Charts.forecastRevenueByCategory(forecastRows);
            return;
        }
        if (t == ReportType.SEASONALITY) {
            lineModel = Charts.seasonalityMonthlyLine(
                    seasonalityService.seasonalityChartData(userSession.getRole(),
                            selectedCategoryId, selectedProductId));
            barModel = Charts.seasonalityAvgSalesBar(seasonalityRows);
            pieModel = Charts.salesRevenueByCategory(categoryRows);
            return;
        }
        // default = sales-like
        List<MonthlySalesRow> monthly = mapMonthlyRevenue(saleRepository.monthlyRevenueRangeNative(from, to));
        lineModel = Charts.salesRevenueTrend(monthly);
        barModel = Charts.salesRevenueByProduct(productPerfRows);
        pieModel = Charts.salesRevenueByCategory(categoryRows);
    }

    private static List<MonthlySalesRow> mapMonthlyRevenue(List<Object[]> raw) {
        List<MonthlySalesRow> rows = new ArrayList<>();
        for (Object[] o : raw) {
            int y = ((Number) o[0]).intValue();
            int m = ((Number) o[1]).intValue();
            BigDecimal rev = o[2] instanceof BigDecimal bd ? bd : new BigDecimal(o[2].toString());
            rows.add(new MonthlySalesRow(y, m, rev));
        }
        return rows;
    }

    private static List<ProductSalesRow> mapProductPerf(List<Object[]> raw) {
        List<ProductSalesRow> rows = new ArrayList<>();
        for (Object[] o : raw) {
            Long id = ((Number) o[0]).longValue();
            String name = String.valueOf(o[1]);
            BigDecimal rev = o[2] instanceof BigDecimal bd ? bd : new BigDecimal(o[2].toString());
            long qty = ((Number) o[3]).longValue();
            rows.add(new ProductSalesRow(id, name, rev, qty));
        }
        return rows;
    }

    private static List<CategorySalesRow> mapCategory(List<Object[]> raw) {
        List<CategorySalesRow> rows = new ArrayList<>();
        for (Object[] o : raw) {
            rows.add(new CategorySalesRow(String.valueOf(o[0]), o[1] instanceof BigDecimal bd ? bd : new BigDecimal(o[1].toString())));
        }
        return rows;
    }

    public String getRangeLabel() {
        LocalDate from = fromDate;
        LocalDate to = toDate;
        if (from == null || to == null) {
            return "—";
        }
        return DisplayFormats.formatDate(from) + " — " + DisplayFormats.formatDate(to);
    }

    /** UI display with arrow (reports filter panel). */
    public String getRangeDisplay() {
        LocalDate from = fromDate;
        LocalDate to = toDate;
        if (from == null || to == null) {
            return "—";
        }
        return DisplayFormats.formatDate(from) + " → " + DisplayFormats.formatDate(to);
    }

    public String formatReportDate(LocalDate date) {
        return DisplayFormats.formatDate(date);
    }

    public String inventoryVendorLabel(InventoryMovement movement) {
        return inventoryService.inventoryVendorLabel(movement);
    }

    public List<Integer> getYearOptions() {
        int current = LocalDate.now().getYear();
        List<Integer> years = new ArrayList<>();
        for (int y = current - 10; y <= current + 1; y++) {
            years.add(y);
        }
        return years;
    }

    /** Server-side PDF (OpenPDF) — avoids empty/broken PrimeFaces DataExporter PDF with converters/Unicode. */
    public void exportSalesPdf() {
        securityService.requireAdminOrSalesManager(userSession.getRole());
        try {
            Document doc = new Document();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter.getInstance(doc, baos);
            doc.open();
            doc.add(new Paragraph("Sales summary", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14)));
            doc.add(new Paragraph("Period: " + getRangeLabel(), FontFactory.getFont(FontFactory.HELVETICA, 10)));
            doc.add(new Paragraph(" "));
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            pdfHeaderRow(table, "Product", "Month", "Qty", "Revenue (INR)");
            for (MonthlyProductSalesRow r : salesRows) {
                table.addCell(pdfCell(safe(r.getProductName())));
                table.addCell(pdfCell(safe(r.getMonthLabel())));
                table.addCell(pdfCell(String.valueOf(r.getQuantitySold())));
                table.addCell(pdfCell(r.getRevenue() != null ? r.getRevenue().toPlainString() : "0"));
            }
            doc.add(table);
            doc.close();
            sendPdf("sales-report.pdf", baos.toByteArray());
        } catch (DocumentException | IOException e) {
            addExportError(e);
        }
    }

    public void exportForecastPdf() {
        securityService.requireAdminOrSalesManager(userSession.getRole());
        try {
            Document doc = new Document();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter.getInstance(doc, baos);
            doc.open();
            doc.add(new Paragraph("Forecast summary", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14)));
            doc.add(new Paragraph("Period: " + getRangeLabel(), FontFactory.getFont(FontFactory.HELVETICA, 10)));
            doc.add(new Paragraph(" "));
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            pdfHeaderRow(table, "Product", "Month", "Predicted units", "Predicted revenue (INR)");
            for (Forecast f : forecastRows) {
                String name = f.getProduct() != null ? safe(f.getProduct().getName()) : "";
                String month = DisplayFormats.formatDate(f.getForecastMonth());
                table.addCell(pdfCell(name));
                table.addCell(pdfCell(month));
                table.addCell(pdfCell(f.getPredictedSales() != null ? f.getPredictedSales().toPlainString() : ""));
                table.addCell(pdfCell(f.getPredictedRevenue() != null ? f.getPredictedRevenue().toPlainString() : ""));
            }
            doc.add(table);
            doc.close();
            sendPdf("forecast-report.pdf", baos.toByteArray());
        } catch (DocumentException | IOException e) {
            addExportError(e);
        }
    }

    public void exportInventoryPdf() {
        securityService.requireAdminOrSalesManager(userSession.getRole());
        try {
            Document doc = new Document();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter.getInstance(doc, baos);
            doc.open();
            doc.add(new Paragraph("Inventory summary", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14)));
            doc.add(new Paragraph("Period: " + getRangeLabel(), FontFactory.getFont(FontFactory.HELVETICA, 10)));
            doc.add(new Paragraph(" "));
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            pdfHeaderRow(table, "Date", "Product", "Qty added", "Vendor");
            for (InventoryMovement i : inventoryRows) {
                String d = DisplayFormats.formatDate(i.getDateAdded());
                String pn = i.getProduct() != null ? safe(i.getProduct().getName()) : "";
                table.addCell(pdfCell(d));
                table.addCell(pdfCell(pn));
                table.addCell(pdfCell(i.getQuantityAdded() != null ? String.valueOf(i.getQuantityAdded()) : ""));
                table.addCell(pdfCell(safe(inventoryService.inventoryVendorLabel(i))));
            }
            doc.add(table);
            doc.close();
            sendPdf("inventory-report.pdf", baos.toByteArray());
        } catch (DocumentException | IOException e) {
            addExportError(e);
        }
    }

    public void exportSalesExcel() {
        securityService.requireAdminOrSalesManager(userSession.getRole());
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Sales");
            Row h = sheet.createRow(0);
            h.createCell(0).setCellValue("Product");
            h.createCell(1).setCellValue("Month");
            h.createCell(2).setCellValue("Qty");
            h.createCell(3).setCellValue("Revenue");
            int r = 1;
            for (MonthlyProductSalesRow row : salesRows) {
                Row x = sheet.createRow(r++);
                x.createCell(0).setCellValue(row.getProductName());
                x.createCell(1).setCellValue(row.getMonthLabel());
                x.createCell(2).setCellValue(row.getQuantitySold());
                if (row.getRevenue() != null) {
                    x.createCell(3).setCellValue(row.getRevenue().doubleValue());
                }
            }
            for (int i = 0; i < 4; i++) {
                sheet.autoSizeColumn(i);
            }
            sendExcel("sales-report.xlsx", wb);
        } catch (IOException e) {
            addExportError(e);
        }
    }

    public void exportForecastExcel() {
        securityService.requireAdminOrSalesManager(userSession.getRole());
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Forecast");
            Row h = sheet.createRow(0);
            h.createCell(0).setCellValue("Product");
            h.createCell(1).setCellValue("Month");
            h.createCell(2).setCellValue("Predicted units");
            h.createCell(3).setCellValue("Predicted revenue");
            int r = 1;
            for (Forecast f : forecastRows) {
                Row x = sheet.createRow(r++);
                if (f.getProduct() != null) {
                    x.createCell(0).setCellValue(f.getProduct().getName());
                }
                if (f.getForecastMonth() != null) {
                    x.createCell(1).setCellValue(DisplayFormats.formatDate(f.getForecastMonth()));
                }
                if (f.getPredictedSales() != null) {
                    x.createCell(2).setCellValue(f.getPredictedSales().doubleValue());
                }
                if (f.getPredictedRevenue() != null) {
                    x.createCell(3).setCellValue(f.getPredictedRevenue().doubleValue());
                }
            }
            for (int i = 0; i < 4; i++) {
                sheet.autoSizeColumn(i);
            }
            sendExcel("forecast-report.xlsx", wb);
        } catch (IOException e) {
            addExportError(e);
        }
    }

    public void exportInventoryExcel() {
        securityService.requireAdminOrSalesManager(userSession.getRole());
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Inventory");
            Row h = sheet.createRow(0);
            h.createCell(0).setCellValue("Date");
            h.createCell(1).setCellValue("Product");
            h.createCell(2).setCellValue("Qty added");
            h.createCell(3).setCellValue("Vendor");
            int r = 1;
            for (InventoryMovement i : inventoryRows) {
                Row x = sheet.createRow(r++);
                if (i.getDateAdded() != null) {
                    x.createCell(0).setCellValue(DisplayFormats.formatDate(i.getDateAdded()));
                }
                if (i.getProduct() != null) {
                    x.createCell(1).setCellValue(i.getProduct().getName());
                }
                if (i.getQuantityAdded() != null) {
                    x.createCell(2).setCellValue(i.getQuantityAdded());
                }
                x.createCell(3).setCellValue(inventoryService.inventoryVendorLabel(i));
            }
            for (int i = 0; i < 4; i++) {
                sheet.autoSizeColumn(i);
            }
            sendExcel("inventory-report.xlsx", wb);
        } catch (IOException e) {
            addExportError(e);
        }
    }

    private void sendPdf(String fileName, byte[] bytes) throws IOException {
        FacesContext fc = FacesContext.getCurrentInstance();
        ExternalContext ec = fc.getExternalContext();
        ec.responseReset();
        ec.setResponseContentType("application/pdf");
        ec.setResponseCharacterEncoding("UTF-8");
        ec.setResponseHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        ec.setResponseContentLength(bytes.length);
        OutputStream out = ec.getResponseOutputStream();
        out.write(bytes);
        out.flush();
        fc.responseComplete();
    }

    private void sendExcel(String fileName, XSSFWorkbook wb) throws IOException {
        FacesContext fc = FacesContext.getCurrentInstance();
        ExternalContext ec = fc.getExternalContext();
        ec.responseReset();
        ec.setResponseContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        ec.setResponseHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        OutputStream out = ec.getResponseOutputStream();
        wb.write(out);
        out.flush();
        fc.responseComplete();
    }

    private static void pdfHeaderRow(PdfPTable table, String... headers) {
        for (String h : headers) {
            PdfPCell c = new PdfPCell(new Phrase(h, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
            c.setBackgroundColor(new java.awt.Color(241, 245, 249));
            table.addCell(c);
        }
    }

    private static PdfPCell pdfCell(String text) {
        return new PdfPCell(new Phrase(text != null ? text : "", FontFactory.getFont(FontFactory.HELVETICA, 10)));
    }

    private static String safe(String s) {
        return s != null ? s : "";
    }

    private void addExportError(Exception e) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Export failed", e.getMessage()));
    }

    /** Export the currently selected report type as PDF. */
    public void exportCurrentPdf() {
        securityService.requireAdminOrSalesManager(userSession.getRole());
        switch (selectedReport) {
            case SALES -> exportSalesPdf();
            case PRODUCT_PERFORMANCE -> exportSalesPdf();
            case CATEGORY -> exportSalesPdf();
            case FORECAST -> exportForecastPdf();
            case INVENTORY -> exportInventoryPdf();
            case SEASONALITY -> exportSeasonalityPdf();
            default -> { }
        }
    }

    public void exportCurrentExcel() {
        securityService.requireAdminOrSalesManager(userSession.getRole());
        switch (selectedReport) {
            case SALES -> exportSalesExcel();
            case PRODUCT_PERFORMANCE -> exportSalesExcel();
            case CATEGORY -> exportSalesExcel();
            case FORECAST -> exportForecastExcel();
            case INVENTORY -> exportInventoryExcel();
            case SEASONALITY -> exportSeasonalityExcel();
            default -> { }
        }
    }

    public void exportSeasonalityPdf() {
        securityService.requireAdminOrSalesManager(userSession.getRole());
        try {
            Document doc = new Document();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter.getInstance(doc, baos);
            doc.open();
            doc.add(new Paragraph("Seasonality Report", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14)));
            doc.add(new Paragraph("Period: " + getRangeLabel(), FontFactory.getFont(FontFactory.HELVETICA, 10)));
            appendSeasonalityPdfSection(doc);
            doc.close();
            sendPdf("seasonality-report.pdf", baos.toByteArray());
        } catch (DocumentException | IOException e) {
            addExportError(e);
        }
    }

    public void exportSeasonalityExcel() {
        securityService.requireAdminOrSalesManager(userSession.getRole());
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            writeSeasonalitySheet(wb.createSheet("Seasonality"));
            sendExcel("seasonality-report.xlsx", wb);
        } catch (IOException e) {
            addExportError(e);
        }
    }

    private void appendForecastKpiParagraph(Document doc) throws DocumentException {
        doc.add(new Paragraph("Forecast KPIs", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
        doc.add(new Paragraph("Forecasted revenue: " + forecastKpis.getForecastedRevenue(), FontFactory.getFont(FontFactory.HELVETICA, 10)));
        doc.add(new Paragraph("Total predicted units: " + forecastKpis.getTotalPredictedUnits(), FontFactory.getFont(FontFactory.HELVETICA, 10)));
        doc.add(new Paragraph("Highest: " + forecastKpis.getHighestPredictedProduct() + " | Lowest: "
                + forecastKpis.getLowestPredictedProduct(), FontFactory.getFont(FontFactory.HELVETICA, 10)));
        doc.add(new Paragraph("Forecast growth %: " + forecastKpis.getForecastGrowthPercent(), FontFactory.getFont(FontFactory.HELVETICA, 10)));
        doc.add(new Paragraph(" "));
    }

    private void appendSalesPdfSection(Document doc) throws DocumentException {
        doc.add(new Paragraph("Sales Report", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        pdfHeaderRow(table, "Product", "Month", "Qty", "Revenue");
        for (MonthlyProductSalesRow r : salesRows) {
            table.addCell(pdfCell(safe(r.getProductName())));
            table.addCell(pdfCell(safe(r.getMonthLabel())));
            table.addCell(pdfCell(String.valueOf(r.getQuantitySold())));
            table.addCell(pdfCell(r.getRevenue() != null ? r.getRevenue().toPlainString() : "0"));
        }
        doc.add(table);
        doc.add(new Paragraph(" "));
    }

    private void appendInventoryPdfSection(Document doc) throws DocumentException {
        doc.add(new Paragraph("Inventory Report", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        pdfHeaderRow(table, "Date", "Product", "Qty added", "Vendor");
        for (InventoryMovement i : inventoryRows) {
            table.addCell(pdfCell(DisplayFormats.formatDate(i.getDateAdded())));
            table.addCell(pdfCell(i.getProduct() != null ? safe(i.getProduct().getName()) : ""));
            table.addCell(pdfCell(i.getQuantityAdded() != null ? String.valueOf(i.getQuantityAdded()) : ""));
            table.addCell(pdfCell(safe(inventoryService.inventoryVendorLabel(i))));
        }
        doc.add(table);
        doc.add(new Paragraph(" "));
    }

    private void appendForecastPdfSection(Document doc) throws DocumentException {
        doc.add(new Paragraph("Forecast Report", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        pdfHeaderRow(table, "Product", "Month", "Predicted", "Confidence", "Revenue");
        for (Forecast f : forecastRows) {
            table.addCell(pdfCell(f.getProduct() != null ? safe(f.getProduct().getName()) : ""));
            table.addCell(pdfCell(DisplayFormats.formatDate(f.getForecastMonth())));
            table.addCell(pdfCell(f.getPredictedSales() != null ? f.getPredictedSales().toPlainString() : ""));
            String conf = (f.getLowerBound() != null ? f.getLowerBound().toPlainString() : "0")
                    + " - " + (f.getUpperBound() != null ? f.getUpperBound().toPlainString() : "0");
            table.addCell(pdfCell(conf));
            table.addCell(pdfCell(f.getPredictedRevenue() != null ? f.getPredictedRevenue().toPlainString() : ""));
        }
        doc.add(table);
        doc.add(new Paragraph(" "));
    }

    private void appendProductPerfPdfSection(Document doc) throws DocumentException {
        doc.add(new Paragraph("Product Performance", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        pdfHeaderRow(table, "Product", "Revenue", "Qty");
        for (ProductSalesRow r : productPerfRows) {
            table.addCell(pdfCell(safe(r.getProductName())));
            table.addCell(pdfCell(r.getRevenue() != null ? r.getRevenue().toPlainString() : "0"));
            table.addCell(pdfCell(String.valueOf(r.getQuantitySold())));
        }
        doc.add(table);
        doc.add(new Paragraph(" "));
    }

    private void appendCategoryPdfSection(Document doc) throws DocumentException {
        doc.add(new Paragraph("Category Report", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        pdfHeaderRow(table, "Category", "Revenue");
        for (CategorySalesRow r : categoryRows) {
            table.addCell(pdfCell(safe(r.getCategoryName())));
            table.addCell(pdfCell(r.getRevenue() != null ? r.getRevenue().toPlainString() : "0"));
        }
        doc.add(table);
        doc.add(new Paragraph(" "));
    }

    private void appendSeasonalityPdfSection(Document doc) throws DocumentException {
        doc.add(new Paragraph("Seasonality Report", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        pdfHeaderRow(table, "Product", "Best Month", "Worst Month", "Avg Monthly");
        for (SeasonalityRow r : seasonalityRows) {
            table.addCell(pdfCell(safe(r.getProductName())));
            table.addCell(pdfCell(safe(r.getBestMonth())));
            table.addCell(pdfCell(safe(r.getWorstMonth())));
            table.addCell(pdfCell(r.getAverageMonthlySales() != null ? r.getAverageMonthlySales().toPlainString() : ""));
        }
        doc.add(table);
        doc.add(new Paragraph(" "));
    }

    private void writeSalesSheet(Sheet sheet) {
        Row h = sheet.createRow(0);
        h.createCell(0).setCellValue("Product");
        h.createCell(1).setCellValue("Month");
        h.createCell(2).setCellValue("Qty");
        h.createCell(3).setCellValue("Revenue");
        int r = 1;
        for (MonthlyProductSalesRow row : salesRows) {
            Row x = sheet.createRow(r++);
            x.createCell(0).setCellValue(row.getProductName());
            x.createCell(1).setCellValue(row.getMonthLabel());
            x.createCell(2).setCellValue(row.getQuantitySold());
            if (row.getRevenue() != null) {
                x.createCell(3).setCellValue(row.getRevenue().doubleValue());
            }
        }
    }

    private void writeForecastSheet(Sheet sheet) {
        Row h = sheet.createRow(0);
        h.createCell(0).setCellValue("Product");
        h.createCell(1).setCellValue("Month");
        h.createCell(2).setCellValue("Predicted units");
        h.createCell(3).setCellValue("Lower bound");
        h.createCell(4).setCellValue("Upper bound");
        h.createCell(5).setCellValue("Predicted revenue");
        int r = 1;
        for (Forecast f : forecastRows) {
            Row x = sheet.createRow(r++);
            if (f.getProduct() != null) {
                x.createCell(0).setCellValue(f.getProduct().getName());
            }
            if (f.getForecastMonth() != null) {
                x.createCell(1).setCellValue(DisplayFormats.formatDate(f.getForecastMonth()));
            }
            if (f.getPredictedSales() != null) {
                x.createCell(2).setCellValue(f.getPredictedSales().doubleValue());
            }
            if (f.getLowerBound() != null) {
                x.createCell(3).setCellValue(f.getLowerBound().doubleValue());
            }
            if (f.getUpperBound() != null) {
                x.createCell(4).setCellValue(f.getUpperBound().doubleValue());
            }
            if (f.getPredictedRevenue() != null) {
                x.createCell(5).setCellValue(f.getPredictedRevenue().doubleValue());
            }
        }
    }

    private void writeInventorySheet(Sheet sheet) {
        Row h = sheet.createRow(0);
        h.createCell(0).setCellValue("Date");
        h.createCell(1).setCellValue("Product");
        h.createCell(2).setCellValue("Qty added");
        h.createCell(3).setCellValue("Vendor");
        int r = 1;
        for (InventoryMovement i : inventoryRows) {
            Row x = sheet.createRow(r++);
            if (i.getDateAdded() != null) {
                x.createCell(0).setCellValue(DisplayFormats.formatDate(i.getDateAdded()));
            }
            if (i.getProduct() != null) {
                x.createCell(1).setCellValue(i.getProduct().getName());
            }
            if (i.getQuantityAdded() != null) {
                x.createCell(2).setCellValue(i.getQuantityAdded());
            }
            x.createCell(3).setCellValue(inventoryService.inventoryVendorLabel(i));
        }
    }

    private void writeProductPerfSheet(Sheet sheet) {
        Row h = sheet.createRow(0);
        h.createCell(0).setCellValue("Product");
        h.createCell(1).setCellValue("Revenue");
        h.createCell(2).setCellValue("Qty");
        int r = 1;
        for (ProductSalesRow row : productPerfRows) {
            Row x = sheet.createRow(r++);
            x.createCell(0).setCellValue(row.getProductName());
            if (row.getRevenue() != null) {
                x.createCell(1).setCellValue(row.getRevenue().doubleValue());
            }
            x.createCell(2).setCellValue(row.getQuantitySold());
        }
    }

    private void writeCategorySheet(Sheet sheet) {
        Row h = sheet.createRow(0);
        h.createCell(0).setCellValue("Category");
        h.createCell(1).setCellValue("Revenue");
        int r = 1;
        for (CategorySalesRow row : categoryRows) {
            Row x = sheet.createRow(r++);
            x.createCell(0).setCellValue(row.getCategoryName());
            if (row.getRevenue() != null) {
                x.createCell(1).setCellValue(row.getRevenue().doubleValue());
            }
        }
    }

    private void writeSeasonalitySheet(Sheet sheet) {
        Row h = sheet.createRow(0);
        h.createCell(0).setCellValue("Product");
        h.createCell(1).setCellValue("Best Month");
        h.createCell(2).setCellValue("Worst Month");
        h.createCell(3).setCellValue("Avg Monthly Sales");
        int r = 1;
        for (SeasonalityRow row : seasonalityRows) {
            Row x = sheet.createRow(r++);
            x.createCell(0).setCellValue(row.getProductName());
            x.createCell(1).setCellValue(row.getBestMonth());
            x.createCell(2).setCellValue(row.getWorstMonth());
            if (row.getAverageMonthlySales() != null) {
                x.createCell(3).setCellValue(row.getAverageMonthlySales().doubleValue());
            }
        }
    }

    // Getters
    public ReportType getSelectedReport() { return selectedReport; }
    public TimeMode getTimeMode() { return timeMode; }
    public void setTimeMode(TimeMode timeMode) { this.timeMode = timeMode; }
    public int getSelectedYear() { return selectedYear; }
    public void setSelectedYear(int selectedYear) { this.selectedYear = selectedYear; }
    public int getSelectedMonth() { return selectedMonth; }
    public void setSelectedMonth(int selectedMonth) { this.selectedMonth = selectedMonth; }
    public LocalDate getFromDate() { return fromDate; }
    public void setFromDate(LocalDate fromDate) { this.fromDate = fromDate; }
    public LocalDate getToDate() { return toDate; }
    public void setToDate(LocalDate toDate) { this.toDate = toDate; }
    public List<Category> getCategories() { return categories; }
    public Long getSelectedCategoryId() { return selectedCategoryId; }
    public void setSelectedCategoryId(Long selectedCategoryId) { this.selectedCategoryId = selectedCategoryId; }
    public List<Product> getProducts() { return products; }
    public Long getSelectedProductId() { return selectedProductId; }
    public void setSelectedProductId(Long selectedProductId) { this.selectedProductId = selectedProductId; }

    public BigDecimal getKpiRevenue() { return kpiRevenue; }
    public long getKpiUnits() { return kpiUnits; }
    public long getKpiRows() { return kpiRows; }

    public List<MonthlyProductSalesRow> getSalesRows() { return salesRows; }
    public List<Forecast> getForecastRows() { return forecastRows; }
    public List<InventoryMovement> getInventoryRows() { return inventoryRows; }
    public List<ProductSalesRow> getProductPerfRows() { return productPerfRows; }
    public List<CategorySalesRow> getCategoryRows() { return categoryRows; }
    public List<SeasonalityRow> getSeasonalityRows() { return seasonalityRows; }
    public ForecastDashboardKpis getForecastKpis() { return forecastKpis; }

    public boolean isShowForecastKpis() {
        ReportType t = selectedReport;
        return t == ReportType.FORECAST || t == ReportType.ALL;
    }

    public LineChartModel getLineModel() { return lineModel; }
    public BarChartModel getBarModel() { return barModel; }
    public PieChartModel getPieModel() { return pieModel; }

    /**
     * Small static helpers for PrimeFaces chart models (keep bean readable).
     */
    static class Charts {
        static LineChartModel salesRevenueTrend(List<MonthlySalesRow> monthly) {
            LineChartModel model = new LineChartModel();
            ChartData data = new ChartData();

            List<String> labels = new ArrayList<>();
            List<Object> values = new ArrayList<>();
            for (MonthlySalesRow r : monthly) {
                labels.add(r.getLabel());
                values.add(r.getRevenue() != null ? r.getRevenue().doubleValue() : 0d);
            }
            if (labels.isEmpty()) { labels.add("—"); values.add(0d); }

            LineChartDataSet ds = new LineChartDataSet();
            ds.setLabel("Revenue (₹)");
            ds.setData(values);
            ds.setFill(false);
            ds.setBorderColor("rgb(30, 64, 175)");
            ds.setBackgroundColor("rgba(30, 64, 175, 0.15)");
            ds.setTension(0.25);

            data.addChartDataSet(ds);
            data.setLabels(labels);
            model.setData(data);
            return model;
        }

        static BarChartModel salesRevenueByProduct(List<ProductSalesRow> rows) {
            BarChartModel model = new BarChartModel();
            ChartData data = new ChartData();

            List<String> labels = new ArrayList<>();
            List<Number> values = new ArrayList<>();
            for (ProductSalesRow r : rows) {
                labels.add(r.getProductName());
                values.add(r.getRevenue() != null ? r.getRevenue().doubleValue() : 0d);
            }
            if (labels.isEmpty()) { labels.add("—"); values.add(0d); }

            BarChartDataSet ds = new BarChartDataSet();
            ds.setLabel("Revenue (₹)");
            ds.setData(values);
            ds.setBackgroundColor("rgba(30, 64, 175, 0.45)");
            ds.setBorderColor("rgb(30, 64, 175)");
            ds.setBorderWidth(1);
            data.addChartDataSet(ds);
            data.setLabels(labels);
            model.setData(data);
            return model;
        }

        static PieChartModel salesRevenueByCategory(List<CategorySalesRow> rows) {
            PieChartModel model = new PieChartModel();
            ChartData data = new ChartData();

            List<String> labels = new ArrayList<>();
            List<Number> values = new ArrayList<>();
            for (CategorySalesRow r : rows) {
                labels.add(r.getCategoryName());
                values.add(r.getRevenue() != null ? r.getRevenue().doubleValue() : 0d);
            }
            if (labels.isEmpty()) { labels.add("—"); values.add(0d); }

            PieChartDataSet ds = new PieChartDataSet();
            ds.setData(values);
            applyPieSliceColors(ds, values.size());
            data.setLabels(labels);
            data.addChartDataSet(ds);
            model.setData(data);
            return model;
        }

        static LineChartModel forecastRevenueTrend(List<Forecast> rows) {
            // month -> total predicted revenue
            java.util.Map<LocalDate, Double> byMonth = new java.util.TreeMap<>();
            for (Forecast f : rows) {
                if (f.getForecastMonth() == null || f.getPredictedRevenue() == null) continue;
                byMonth.merge(f.getForecastMonth(), f.getPredictedRevenue().doubleValue(), Double::sum);
            }
            List<String> labels = new ArrayList<>();
            List<Object> values = new ArrayList<>();
            for (var e : byMonth.entrySet()) {
                labels.add(DisplayFormats.formatDate(e.getKey()));
                values.add(e.getValue());
            }
            if (labels.isEmpty()) { labels.add("—"); values.add(0d); }

            LineChartModel model = new LineChartModel();
            ChartData data = new ChartData();
            LineChartDataSet ds = new LineChartDataSet();
            ds.setLabel("Predicted revenue (₹)");
            ds.setData(values);
            ds.setFill(false);
            ds.setBorderColor("rgb(30, 64, 175)");
            ds.setBackgroundColor("rgba(30, 64, 175, 0.15)");
            ds.setTension(0.25);
            data.addChartDataSet(ds);
            data.setLabels(labels);
            model.setData(data);
            return model;
        }

        static BarChartModel forecastRevenueByProduct(List<Forecast> rows) {
            java.util.Map<String, Forecast> latest = new java.util.LinkedHashMap<>();
            for (Forecast f : rows) {
                if (f.getProduct() == null || f.getProduct().getName() == null || f.getForecastMonth() == null) continue;
                String name = f.getProduct().getName();
                Forecast prev = latest.get(name);
                if (prev == null || f.getForecastMonth().isAfter(prev.getForecastMonth())) {
                    latest.put(name, f);
                }
            }
            List<String> labels = new ArrayList<>();
            List<Number> values = new ArrayList<>();
            for (var e : latest.entrySet()) {
                labels.add(e.getKey());
                values.add(e.getValue().getPredictedRevenue() != null ? e.getValue().getPredictedRevenue().doubleValue() : 0d);
            }
            if (labels.isEmpty()) { labels.add("—"); values.add(0d); }

            BarChartModel model = new BarChartModel();
            ChartData data = new ChartData();
            BarChartDataSet ds = new BarChartDataSet();
            ds.setLabel("Predicted revenue (₹)");
            ds.setData(values);
            ds.setBackgroundColor("rgba(30, 64, 175, 0.45)");
            ds.setBorderColor("rgb(30, 64, 175)");
            ds.setBorderWidth(1);
            data.addChartDataSet(ds);
            data.setLabels(labels);
            model.setData(data);
            return model;
        }

        static PieChartModel forecastRevenueByCategory(List<Forecast> rows) {
            java.util.Map<String, Double> byCat = new java.util.LinkedHashMap<>();
            for (Forecast f : rows) {
                if (f.getProduct() == null || f.getProduct().getCategory() == null || f.getPredictedRevenue() == null) continue;
                String name = f.getProduct().getCategory().getName();
                byCat.merge(name, f.getPredictedRevenue().doubleValue(), Double::sum);
            }
            List<String> labels = new ArrayList<>();
            List<Number> values = new ArrayList<>();
            for (var e : byCat.entrySet()) {
                labels.add(e.getKey());
                values.add(e.getValue());
            }
            if (labels.isEmpty()) { labels.add("—"); values.add(0d); }

            PieChartModel model = new PieChartModel();
            ChartData data = new ChartData();
            PieChartDataSet ds = new PieChartDataSet();
            ds.setData(values);
            applyPieSliceColors(ds, values.size());
            data.setLabels(labels);
            data.addChartDataSet(ds);
            model.setData(data);
            return model;
        }

        static LineChartModel inventoryTrend(List<InventoryMovement> rows) {
            java.util.Map<LocalDate, Double> byDay = new java.util.TreeMap<>();
            for (InventoryMovement i : rows) {
                if (i.getDateAdded() == null || i.getQuantityAdded() == null) continue;
                byDay.merge(i.getDateAdded(), i.getQuantityAdded().doubleValue(), Double::sum);
            }
            List<String> labels = new ArrayList<>();
            List<Object> values = new ArrayList<>();
            for (var e : byDay.entrySet()) {
                labels.add(DisplayFormats.formatDate(e.getKey()));
                values.add(e.getValue());
            }
            if (labels.isEmpty()) { labels.add("—"); values.add(0d); }

            LineChartModel model = new LineChartModel();
            ChartData data = new ChartData();
            LineChartDataSet ds = new LineChartDataSet();
            ds.setLabel("Qty added");
            ds.setData(values);
            ds.setFill(false);
            ds.setBorderColor("rgb(30, 64, 175)");
            ds.setBackgroundColor("rgba(30, 64, 175, 0.15)");
            ds.setTension(0.25);
            data.addChartDataSet(ds);
            data.setLabels(labels);
            model.setData(data);
            return model;
        }

        static BarChartModel inventoryByProduct(List<InventoryMovement> rows) {
            java.util.Map<String, Double> byProduct = new java.util.LinkedHashMap<>();
            for (InventoryMovement i : rows) {
                if (i.getProduct() == null || i.getProduct().getName() == null || i.getQuantityAdded() == null) continue;
                byProduct.merge(i.getProduct().getName(), i.getQuantityAdded().doubleValue(), Double::sum);
            }
            List<String> labels = new ArrayList<>();
            List<Number> values = new ArrayList<>();
            for (var e : byProduct.entrySet()) {
                labels.add(e.getKey());
                values.add(e.getValue());
            }
            if (labels.isEmpty()) { labels.add("—"); values.add(0d); }

            BarChartModel model = new BarChartModel();
            ChartData data = new ChartData();
            BarChartDataSet ds = new BarChartDataSet();
            ds.setLabel("Qty added");
            ds.setData(values);
            ds.setBackgroundColor("rgba(30, 64, 175, 0.45)");
            ds.setBorderColor("rgb(30, 64, 175)");
            ds.setBorderWidth(1);
            data.addChartDataSet(ds);
            data.setLabels(labels);
            model.setData(data);
            return model;
        }

        static PieChartModel inventoryBySource(List<InventoryMovement> rows,
                                               java.util.function.Function<InventoryMovement, String> vendorLabel) {
            java.util.Map<String, Double> bySource = new java.util.LinkedHashMap<>();
            for (InventoryMovement i : rows) {
                if (i.getQuantityAdded() == null) {
                    continue;
                }
                String label = vendorLabel.apply(i);
                if (label == null || label.isBlank()) {
                    continue;
                }
                bySource.merge(label, i.getQuantityAdded().doubleValue(), Double::sum);
            }
            List<String> labels = new ArrayList<>();
            List<Number> values = new ArrayList<>();
            for (var e : bySource.entrySet()) {
                labels.add(e.getKey());
                values.add(e.getValue());
            }
            if (labels.isEmpty()) { labels.add("—"); values.add(0d); }

            PieChartModel model = new PieChartModel();
            ChartData data = new ChartData();
            PieChartDataSet ds = new PieChartDataSet();
            ds.setData(values);
            applyPieSliceColors(ds, values.size());
            data.setLabels(labels);
            data.addChartDataSet(ds);
            model.setData(data);
            return model;
        }

        static LineChartModel seasonalityMonthlyLine(List<Object[]> chartData) {
            List<String> labels = new ArrayList<>();
            List<Object> values = new ArrayList<>();
            for (Object[] o : chartData) {
                labels.add(String.valueOf(o[0]));
                values.add(o[1] instanceof Number n ? n.doubleValue() : 0d);
            }
            if (labels.isEmpty()) {
                labels.add("—");
                values.add(0d);
            }
            LineChartModel model = new LineChartModel();
            ChartData data = new ChartData();
            LineChartDataSet ds = new LineChartDataSet();
            ds.setLabel("Average units sold");
            ds.setData(values);
            ds.setFill(false);
            ds.setBorderColor("rgb(30, 64, 175)");
            ds.setTension(0.25);
            data.addChartDataSet(ds);
            data.setLabels(labels);
            model.setData(data);
            return model;
        }

        static BarChartModel seasonalityAvgSalesBar(List<SeasonalityRow> rows) {
            List<String> labels = new ArrayList<>();
            List<Number> values = new ArrayList<>();
            for (SeasonalityRow r : rows) {
                labels.add(r.getProductName());
                values.add(r.getAverageMonthlySales() != null ? r.getAverageMonthlySales().doubleValue() : 0d);
            }
            if (labels.isEmpty()) {
                labels.add("—");
                values.add(0d);
            }
            BarChartModel model = new BarChartModel();
            ChartData data = new ChartData();
            BarChartDataSet ds = new BarChartDataSet();
            ds.setLabel("Avg monthly sales (units)");
            ds.setData(values);
            ds.setBackgroundColor("rgba(245, 158, 11, 0.45)");
            ds.setBorderColor("rgb(245, 158, 11)");
            ds.setBorderWidth(1);
            data.addChartDataSet(ds);
            data.setLabels(labels);
            model.setData(data);
            return model;
        }

        private static final List<String> PIE_SLICE_COLORS = List.of(
                "rgba(37, 99, 235, 0.88)",
                "rgba(16, 185, 129, 0.88)",
                "rgba(245, 158, 11, 0.88)",
                "rgba(239, 68, 68, 0.88)",
                "rgba(139, 92, 246, 0.88)",
                "rgba(236, 72, 153, 0.88)",
                "rgba(14, 165, 233, 0.88)",
                "rgba(132, 204, 22, 0.88)"
        );

        private static final List<String> PIE_SLICE_BORDERS = List.of(
                "rgb(37, 99, 235)",
                "rgb(16, 185, 129)",
                "rgb(245, 158, 11)",
                "rgb(239, 68, 68)",
                "rgb(139, 92, 246)",
                "rgb(236, 72, 153)",
                "rgb(14, 165, 233)",
                "rgb(132, 204, 22)"
        );

        private static void applyPieSliceColors(PieChartDataSet ds, int sliceCount) {
            int n = Math.max(1, sliceCount);
            List<String> bg = new ArrayList<>(n);
            List<String> border = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                int idx = i % PIE_SLICE_COLORS.size();
                bg.add(PIE_SLICE_COLORS.get(idx));
                border.add(PIE_SLICE_BORDERS.get(idx));
            }
            ds.setBackgroundColor(bg);
            ds.setBorderColor(border);
            List<Number> widths = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                widths.add(1);
            }
            ds.setBorderWidth(widths);
        }

        static LineChartModel emptyLine() {
            LineChartModel model = new LineChartModel();
            ChartData data = new ChartData();
            LineChartDataSet ds = new LineChartDataSet();
            ds.setLabel("—");
            ds.setData(List.of(0));
            data.addChartDataSet(ds);
            data.setLabels(List.of("—"));
            model.setData(data);
            return model;
        }

        static BarChartModel emptyBar() {
            BarChartModel model = new BarChartModel();
            ChartData data = new ChartData();
            BarChartDataSet ds = new BarChartDataSet();
            ds.setLabel("—");
            ds.setData(List.of(0));
            data.addChartDataSet(ds);
            data.setLabels(List.of("—"));
            model.setData(data);
            return model;
        }

        static PieChartModel emptyPie() {
            PieChartModel model = new PieChartModel();
            ChartData data = new ChartData();
            PieChartDataSet ds = new PieChartDataSet();
            ds.setData(List.of(1));
            applyPieSliceColors(ds, 1);
            data.setLabels(List.of("—"));
            data.addChartDataSet(ds);
            model.setData(data);
            return model;
        }
    }
}

