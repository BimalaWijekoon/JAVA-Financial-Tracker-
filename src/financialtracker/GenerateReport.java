/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package financialtracker;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.ss.usermodel.Row; 
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;

/**
 *
 * @author bmwmi
 */
public class GenerateReport extends javax.swing.JFrame {
    private static final Logger logger = Logger.getLogger(GenerateReport.class.getName());

    /**
     * Creates new form GenerateReport
     */
    private final String accountNumber; 
    
    public GenerateReport(String accountNumber) {
        initComponents();
        this.accountNumber = accountNumber;
        try {
            UserUtility.setUserNameFromDatabase(accountNumber, username, Account);
        } catch (SQLException ex) {
            Logger.getLogger(EditBudgetPlan.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    private Workbook generateReport(String year, String month, String fromDate, String toDate, String type) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Budget Report");

        try (Connection conn = new DataBase().getConnection()) {

            // Retrieve the monthly budget plan data
            PreparedStatement planStmt = conn.prepareStatement("SELECT Income, Budget_Limit FROM monthly_plan WHERE Acc_Number = ? AND Year = ? AND Month = ?");
            planStmt.setString(1, accountNumber);
            planStmt.setString(2, year);
            planStmt.setString(3, month);
            ResultSet planResultSet = planStmt.executeQuery();
            // Retrieve the income value
            double salary = retrieveIncome(planResultSet);
            planResultSet.beforeFirst();
            double budget = retrieveBudget(planResultSet);

            // Reset the cursor position of the ResultSet to the beginning
            planResultSet.beforeFirst();

            // Generate the monthly budget plan section and get the number of rows used
            int budgetPlanRows = generateBudgetPlan(sheet, planResultSet, month);

            // Adjust the starting row for printing transaction tables
            int startRow = budgetPlanRows + 2; // Add extra rows for spacing

            // Prepare the SQL query to retrieve transaction data
            PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM transactions WHERE Acc_Number = ? AND Year = ? AND Month = ? AND Date BETWEEN ? AND ? AND Type = ?");
            pstmt.setString(1, accountNumber);
            pstmt.setString(2, year);
            pstmt.setString(3, month);

            // Ensure that fromDate is before or equal to toDate
            if (fromDate.compareTo(toDate) > 0) {
                // Swap fromDate and toDate
                String temp = fromDate;
                fromDate = toDate;
                toDate = temp;
            }

            // Now fromDate is always before or equal to toDate
            pstmt.setString(4, fromDate);
            pstmt.setString(5, toDate);

            // If type is "BOTH", retrieve income and expenses separately
            double totalExpenses = 0;
            double totalIncomes = 0;
            if (type.equals("BOTH")) {
                // Income table
                pstmt.setString(6, "INCOMES"); // Set the type to "INCOMES"
                ResultSet incomeResultSet = pstmt.executeQuery();
                startRow = generateTable(sheet, incomeResultSet, "INCOMES", month, workbook, startRow);
                incomeResultSet.beforeFirst();
                totalIncomes = calculateTotal(incomeResultSet);

                // Expenses table
                pstmt.setString(6, "EXPENSES"); // Set the type to "EXPENSES"
                ResultSet expensesResultSet = pstmt.executeQuery();
                startRow= generateTable(sheet, expensesResultSet, "EXPENSES", month, workbook, startRow + 2); // Start expense table after income table
                expensesResultSet.beforeFirst();
                totalExpenses = calculateTotal(expensesResultSet);
            
            
                printAccountSummary(sheet, startRow, totalIncomes, totalExpenses, salary, budget);
            } else {
                // For single type (income or expenses)
                pstmt.setString(6, type); // Set the type parameter
                ResultSet rs = pstmt.executeQuery();
                startRow = generateTable(sheet, rs, type, month, workbook, startRow);
                if (type.equals("INCOMES")) {
                    rs.beforeFirst();
                    totalIncomes = calculateTotal(rs);
                    pstmt.setString(6, "EXPENSES"); // Set the type to "EXPENSES"
                    ResultSet expensesResultSet = pstmt.executeQuery();
                    totalExpenses = calculateTotal(expensesResultSet);
                    printAccountSummary(sheet, startRow, totalIncomes, totalExpenses, salary, budget);
                } else if (type.equals("EXPENSES")) {
                    rs.beforeFirst();
                    totalExpenses = calculateTotal(rs);
                    pstmt.setString(6, "INCOMES"); // Set the type to "INCOMES"
                    ResultSet incomeResultSet = pstmt.executeQuery();
                    totalIncomes = calculateTotal(incomeResultSet);
                    printAccountSummary(sheet, startRow, totalIncomes, totalExpenses, salary, budget);
                }
            }  
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "An error occurred while generating the report: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
        return workbook;
    }

    private void printAccountSummary(Sheet sheet, int startRow, double totalIncomes, double totalExpenses, double salary, double budgetLimit) {
        Row summaryRow = sheet.createRow(startRow + 3); // Add 3 rows after the transaction tables
        summaryRow.createCell(0).setCellValue("Account Summary:");

        // Retrieve budget limit from the last row of the budget plan section
        Row budgetPlanRow = sheet.getRow(startRow - 3); // Adjust to the appropriate row
        // Calculate remaining budget
        double remainingBudget = budgetLimit - totalExpenses;
        // Calculate account balance
        double accountBalance = totalIncomes - totalExpenses;

        // Print account summary details
        summaryRow = sheet.createRow(startRow + 4);
        summaryRow.createCell(0).setCellValue("Total Incomes up to now with salary = " + (totalIncomes + salary));
        summaryRow = sheet.createRow(startRow + 5);
        summaryRow.createCell(0).setCellValue("Total Expenses up to now = " + totalExpenses);
        summaryRow = sheet.createRow(startRow + 6);
        summaryRow.createCell(0).setCellValue("Budget Limit set for the month = " + budgetLimit);
        summaryRow = sheet.createRow(startRow + 7);
        summaryRow.createCell(0).setCellValue("Remaining Budget for the month = " + remainingBudget);
        summaryRow = sheet.createRow(startRow + 8);
        summaryRow.createCell(0).setCellValue("Balance in the Account = " + accountBalance);
    }

    private double calculateTotal(ResultSet resultSet) throws SQLException {
        double total = 0;
        while (resultSet.next()) {
            total += resultSet.getDouble("Amount");
        }
        return total;
    }
    
    private double retrieveIncome(ResultSet resultSet) throws SQLException {
        double income = 0;
        if (resultSet.next()) {
            income = resultSet.getDouble("Income");
        }
        return income;
    }
    
    private double retrieveBudget(ResultSet resultSet) throws SQLException {
        double budget = 0;
        if (resultSet.next()) {
            budget = resultSet.getDouble("Budget_Limit");
        }
        return budget;
    }

    private int generateBudgetPlan(Sheet sheet, ResultSet planResultSet, String month) throws SQLException {
        int rowNum = 0; // Start from the first row

        if (planResultSet.next()) {
            // Print the title
            Row titleRow = sheet.createRow(rowNum++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Monthly budget plan for " + month + ":");
            // Print Salary
            Row salaryRow = sheet.createRow(rowNum++);
            Cell salaryLabelCell = salaryRow.createCell(0);
            salaryLabelCell.setCellValue("Salary:");

            Cell salaryValueCell = salaryRow.createCell(1);
            salaryValueCell.setCellValue(planResultSet.getDouble("Income"));

            // Print Budget Limit
            Row budgetRow = sheet.createRow(rowNum++);
            Cell budgetLabelCell = budgetRow.createCell(0);
            budgetLabelCell.setCellValue("Budget Limit:");

            Cell budgetValueCell = budgetRow.createCell(1);
            budgetValueCell.setCellValue(planResultSet.getDouble("Budget_Limit"));
        } else {
            // Handle case where no data is found
            System.out.println("No monthly budget plan data found for " + month);
        }

        return rowNum;
    }

    private int generateTable(Sheet sheet, ResultSet rs, String type, String month, Workbook workbook, int startRow) throws SQLException {
        int rowNum = startRow; // Initialize rowNum to startRow

        // Create header based on selected type
        String headerText;
        if (type.equals("INCOMES")) {
            headerText = "Income Table for " + month;
        } else if (type.equals("EXPENSES")) {
            headerText = "Expenses Table for " + month;
        } else {
            headerText = "Income and Expenses Tables for " + month;
        }

        Row headerRow = sheet.createRow(rowNum++);
        Cell headerCell = headerRow.createCell(0);
        headerCell.setCellValue(headerText);

        // Create table headers
        Row tableHeaderRow = sheet.createRow(rowNum++);
        String[] headers = {"Transc_ID", "Year", "Month", "Date", "Type", "Reason", "Amount"};
        for (int i = 0; i < headers.length; i++) {
            Cell header = tableHeaderRow.createCell(i);
            header.setCellValue(headers[i]);
        }

        double totalAmount = 0; // Variable to store total amount

        while (rs.next()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(rs.getString("Transaction_Id"));
            row.createCell(1).setCellValue(rs.getString("Year"));
            row.createCell(2).setCellValue(rs.getString("Month"));
            row.createCell(3).setCellValue(rs.getString("Date"));
            row.createCell(4).setCellValue(rs.getString("Type"));
            row.createCell(5).setCellValue(rs.getString("Reason"));

            // Get amount and add it to the total
            double amount = rs.getDouble("Amount");
            totalAmount += amount;

            row.createCell(6).setCellValue(amount);
        }

        // Add total amount row
        Row totalRow = sheet.createRow(rowNum);
        Cell totalLabelCell = totalRow.createCell(4);
        totalLabelCell.setCellValue("Total Amount:");

        Cell totalAmountCell = totalRow.createCell(6);
        totalAmountCell.setCellValue(totalAmount);

        return rowNum;
    }

    private String generateFilename(String type, String month, String year, String saveLocation, String format) {
        // Generate a timestamp to use as an auto-increment number
        String timestamp = String.valueOf(System.currentTimeMillis());

        String filename = "BudgetReport_" + timestamp;

        // Add type to filename
        filename += " - " + type;

        // Add month and year to filename
        filename += " - for " + month + " " + year;

        // Add account number to filename
        filename += " - " + accountNumber;

        // Append file extension based on the selected format
        if (format.equalsIgnoreCase("xlsx")) {
            filename += ".xlsx";
        } else if (format.equalsIgnoreCase("pdf")) {
            filename += ".pdf";
        } else {
            // Handle unsupported format (optional)
        }

        // Ensure filename does not contain any invalid characters
        filename = filename.replaceAll("[\\\\/:*?\"<>|]", "_");

        // Combine with save location
        filename = saveLocation + File.separator + filename;

        return filename;
    }

    private void saveWorkbook(Workbook workbook, String filename, String format) throws IOException {
        try (FileOutputStream fileOut = new FileOutputStream(filename)) {
            if (format.equalsIgnoreCase("xlsx")) {
                workbook.write(fileOut);
            } else if (format.equalsIgnoreCase("pdf")) {
                convertExcelToPDF(workbook, fileOut);
            } else {
                throw new IllegalArgumentException("Unsupported format: " + format);
            }
        }
    }

    private void convertExcelToPDF(Workbook workbook, OutputStream fileOut) throws IOException {
        try {
            // Create a new PDF document
            PDDocument pdfDocument = new PDDocument();

            // Define page margins
            float margin = 50;
            float pageHeight = 841.89f;
            float pageWidth = 595.28f;
            float tableWidth = pageWidth - 2 * margin;

            // Loop through each sheet in the Excel workbook
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                XSSFSheet sheet = (XSSFSheet) workbook.getSheetAt(i);
                int lastRowNum = sheet.getLastRowNum();

                // Create a new page for each sheet
                PDPage page = new PDPage(new PDRectangle(pageWidth, pageHeight));
                pdfDocument.addPage(page);

                // Create content stream to write to the PDF page
                PDPageContentStream contentStream = new PDPageContentStream(pdfDocument, page, AppendMode.APPEND, true);

                // Set font and text properties
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
                float leading = 1.5f * 12; // Line spacing

                // Center and bold the sheet title ("Budget Report")
                float titleWidth = PDType1Font.HELVETICA_BOLD.getStringWidth("Budget Report") / 1000f * 12;
                float titleX = (pageWidth - titleWidth) / 2;
                float titleY = pageHeight - margin;
                contentStream.beginText();
                contentStream.newLineAtOffset(titleX, titleY);
                contentStream.showText("Budget Report");
                contentStream.endText();

                float y = pageHeight - margin - 30; // Adjust starting y-position

                // Loop through each row in the sheet
                for (int j = 0; j <= lastRowNum; j++) {
                    XSSFRow row = sheet.getRow(j);

                    if (row != null) {
                        int lastCellNum = row.getLastCellNum();
                        float x = margin; // Adjust starting x-position

                        // Loop through each cell in the row
                        for (int k = 0; k < lastCellNum; k++) {
                            XSSFCell cell = row.getCell(k);
                            String cellValue = "";

                            if (cell != null) {
                                cellValue = cell.toString();
                            }

                            // Write cell value to PDF
                            contentStream.beginText();
                            contentStream.newLineAtOffset(x, y);
                            contentStream.showText(cellValue);
                            contentStream.endText();

                            // Adjust x-position for the next cell
                            x += tableWidth / lastCellNum;
                        }
                    }

                    // Adjust y-position for the next row
                    y -= leading; // Adjust line spacing
                }

                // Close content stream for the page
                contentStream.close();
            }

            // Save the PDF document
            pdfDocument.save(fileOut);
            pdfDocument.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel2 = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        Account = new javax.swing.JLabel();
        username = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        MONTH = new javax.swing.JComboBox<>();
        SUBMIT = new javax.swing.JButton();
        jLabel10 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        TYPE = new javax.swing.JComboBox<>();
        jLabel4 = new javax.swing.JLabel();
        FORMAT = new javax.swing.JComboBox<>();
        YEAR = new javax.swing.JComboBox<>();
        jLabel8 = new javax.swing.JLabel();
        DATETO = new javax.swing.JComboBox<>();
        jLabel11 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        DATEFROM = new javax.swing.JComboBox<>();
        jLabel13 = new javax.swing.JLabel();
        SAVELOCATION = new javax.swing.JButton();
        LOCATION = new javax.swing.JLabel();
        dashboardreturn = new javax.swing.JButton();
        jLabel9 = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel2.setBackground(new java.awt.Color(7, 7, 77));
        jPanel2.setPreferredSize(new java.awt.Dimension(1130, 700));

        jLabel5.setBackground(new java.awt.Color(7, 7, 77));
        jLabel5.setFont(new java.awt.Font("Serif", 3, 36)); // NOI18N
        jLabel5.setForeground(new java.awt.Color(191, 191, 191));
        jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel5.setText("GENERATE YOUR FINANCIAL REPORTS FROM HERE");
        jLabel5.setOpaque(true);

        jLabel6.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel6.setForeground(new java.awt.Color(191, 191, 191));
        jLabel6.setText("ACCOUNT NUMBER :");

        jLabel7.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel7.setForeground(new java.awt.Color(191, 191, 191));
        jLabel7.setText("NAME :");

        Account.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        Account.setForeground(new java.awt.Color(191, 191, 191));
        Account.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        Account.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(255, 255, 255)));

        username.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        username.setForeground(new java.awt.Color(191, 191, 191));
        username.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        username.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(255, 255, 255)));

        jPanel3.setBackground(new java.awt.Color(7, 7, 77));
        jPanel3.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(255, 255, 255), 1, true));

        jLabel2.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel2.setForeground(new java.awt.Color(191, 191, 191));
        jLabel2.setText("MONTH");

        MONTH.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        MONTH.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "JANUARY", "FEBRUARY", "MARCH", "APRIL", "MAY", "JUNE", "JULY", "AUGUST", "SEPETEMBER", "OCTOMBER", "NOVEMBER", "DECEMBER" }));

        SUBMIT.setBackground(new java.awt.Color(0, 102, 255));
        SUBMIT.setForeground(new java.awt.Color(255, 255, 255));
        SUBMIT.setText("GENERATE REPORT");
        SUBMIT.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SUBMITActionPerformed(evt);
            }
        });

        jLabel10.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel10.setForeground(new java.awt.Color(191, 191, 191));
        jLabel10.setText("YEAR");

        jLabel3.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(191, 191, 191));
        jLabel3.setText("TYPE");

        TYPE.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        TYPE.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "INCOMES", "EXPENSES", "BOTH" }));

        jLabel4.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel4.setForeground(new java.awt.Color(191, 191, 191));
        jLabel4.setText("FORMAT");

        FORMAT.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        FORMAT.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "pdf", "xlsx" }));

        YEAR.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        YEAR.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "1990", "1991", "1992", "1993", "1994", "1995", "1996", "1997", "1998", "1999", "2000", "2001", "2002", "2003", "2004", "2005", "2006", "2007", "2008", "2009", "2010", "2011", "2012", "2013", "2014", "2015", "2016", "2017", "2018", "2019", "2020", "2021", "2022", "2023", "2024", "2025", "2026", "2027", "2028", "2029", "2030", " " }));

        jLabel8.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel8.setForeground(new java.awt.Color(191, 191, 191));
        jLabel8.setText("DATE");

        DATETO.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        DATETO.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31" }));

        jLabel11.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel11.setForeground(new java.awt.Color(191, 191, 191));
        jLabel11.setText("TO");

        jLabel12.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel12.setForeground(new java.awt.Color(191, 191, 191));
        jLabel12.setText("FROM");

        DATEFROM.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        DATEFROM.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31" }));

        jLabel13.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel13.setForeground(new java.awt.Color(191, 191, 191));
        jLabel13.setText("SAVE LOCATION :");

        SAVELOCATION.setText("CHOOSE");
        SAVELOCATION.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SAVELOCATIONActionPerformed(evt);
            }
        });

        LOCATION.setBackground(new java.awt.Color(255, 255, 255));
        LOCATION.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        LOCATION.setOpaque(true);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel10)
                                    .addComponent(jLabel2)
                                    .addComponent(jLabel3)
                                    .addComponent(jLabel8)
                                    .addComponent(jLabel4))
                                .addGap(91, 91, 91)
                                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(YEAR, javax.swing.GroupLayout.PREFERRED_SIZE, 270, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(MONTH, javax.swing.GroupLayout.PREFERRED_SIZE, 270, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(FORMAT, javax.swing.GroupLayout.PREFERRED_SIZE, 270, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addGroup(jPanel3Layout.createSequentialGroup()
                                                .addComponent(jLabel12)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(DATEFROM, javax.swing.GroupLayout.PREFERRED_SIZE, 58, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addGap(36, 36, 36)
                                                .addComponent(jLabel11)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(DATETO, javax.swing.GroupLayout.PREFERRED_SIZE, 58, javax.swing.GroupLayout.PREFERRED_SIZE))
                                            .addComponent(TYPE, javax.swing.GroupLayout.PREFERRED_SIZE, 270, javax.swing.GroupLayout.PREFERRED_SIZE))))
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addComponent(SUBMIT)))
                        .addContainerGap())
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jLabel13)
                        .addGap(18, 18, 18)
                        .addComponent(LOCATION, javax.swing.GroupLayout.PREFERRED_SIZE, 267, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(SAVELOCATION, javax.swing.GroupLayout.PREFERRED_SIZE, 97, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 44, Short.MAX_VALUE))))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel10)
                    .addComponent(YEAR, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(19, 19, 19)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(MONTH, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2))
                .addGap(18, 18, 18)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(DATETO, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel12)
                    .addComponent(jLabel11)
                    .addComponent(DATEFROM, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel8))
                .addGap(18, 18, 18)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(TYPE, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(FORMAT, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4))
                .addGap(18, 18, 18)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(LOCATION, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel13))
                    .addComponent(SAVELOCATION))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 61, Short.MAX_VALUE)
                .addComponent(SUBMIT)
                .addContainerGap())
        );

        dashboardreturn.setBackground(new java.awt.Color(255, 0, 0));
        dashboardreturn.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        dashboardreturn.setForeground(new java.awt.Color(255, 255, 255));
        dashboardreturn.setText("BACK TO DASHBOARD");
        dashboardreturn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dashboardreturnActionPerformed(evt);
            }
        });

        jLabel9.setIcon(new javax.swing.ImageIcon(getClass().getResource("/financialtracker/Generate Financial Reports.jpg"))); // NOI18N

        jSeparator1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabel5, javax.swing.GroupLayout.DEFAULT_SIZE, 1130, Short.MAX_VALUE)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(28, 28, 28)
                        .addComponent(jLabel6)
                        .addGap(18, 18, 18)
                        .addComponent(Account, javax.swing.GroupLayout.PREFERRED_SIZE, 244, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel7)
                        .addGap(18, 18, 18)
                        .addComponent(username, javax.swing.GroupLayout.PREFERRED_SIZE, 244, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(19, 19, 19)
                        .addComponent(jLabel9, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(dashboardreturn))
                        .addGap(13, 13, 13)))
                .addGap(12, 12, 12))
            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel2Layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jSeparator1)
                    .addContainerGap()))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(40, 40, 40)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel6)
                    .addComponent(Account, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel7)
                    .addComponent(username, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(71, 71, 71)
                        .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(41, 41, 41)
                        .addComponent(dashboardreturn)
                        .addContainerGap(46, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel9, javax.swing.GroupLayout.PREFERRED_SIZE, 511, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(24, 24, 24))))
            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel2Layout.createSequentialGroup()
                    .addGap(124, 124, 124)
                    .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(574, Short.MAX_VALUE)))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, 682, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void SUBMITActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SUBMITActionPerformed
         // Get user input values
    String selectedYear = (String) YEAR.getSelectedItem();
    String selectedMonth = (String) MONTH.getSelectedItem();
    String selectedType = (String) TYPE.getSelectedItem();
    String selectedFormat = (String) FORMAT.getSelectedItem();
    String fromDate = (String) DATEFROM.getSelectedItem();
    String toDate = (String) DATETO.getSelectedItem();

    // Your existing code here
    try {
        // Generate the report
        Workbook workbook = generateReport(selectedYear, selectedMonth, fromDate, toDate, selectedType);

        // Get the chosen save location
        String saveLocation = LOCATION.getText();

        // Generate the filename
       String filename = generateFilename(selectedType, selectedMonth, selectedYear, saveLocation, selectedFormat);


        // Save the workbook to the specified location with the generated filename
        saveWorkbook(workbook, filename, selectedFormat);

    } catch (IOException ex) {
        logger.log(Level.SEVERE, "Error generating or saving report: {0}", ex.getMessage());
        JOptionPane.showMessageDialog(this, "An error occurred while generating or saving the report.", "Error", JOptionPane.ERROR_MESSAGE);
    }
    }//GEN-LAST:event_SUBMITActionPerformed




    private void dashboardreturnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dashboardreturnActionPerformed

    // Retrieve the username associated with the account number
     String user = UserUtility.getUsernameByAccountNumber(accountNumber);

    try {
        DashBoard dashboard = new DashBoard(user);
        dashboard.setVisible(true);
        this.dispose();
    } catch (SQLException ex) {
        JOptionPane.showMessageDialog(this, "An error occurred while creating the dashboard.", "Error", JOptionPane.ERROR_MESSAGE);
    }
    }//GEN-LAST:event_dashboardreturnActionPerformed

    private void SAVELOCATIONActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SAVELOCATIONActionPerformed
        // TODO add your handling code here:
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Save Location");
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        // Show the file chooser dialog
        int result = fileChooser.showSaveDialog(this);
    
        // Check if a file was selected
        if (result == JFileChooser.APPROVE_OPTION) {
            // Get the selected file
            File selectedFile = fileChooser.getSelectedFile();
        
            // Set the absolute path of the selected file as the text of the location label
            LOCATION.setText(selectedFile.getAbsolutePath());
        }
    }//GEN-LAST:event_SAVELOCATIONActionPerformed

    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(GenerateReport.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(GenerateReport.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(GenerateReport.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(GenerateReport.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new GenerateReport("accountNumber").setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel Account;
    private javax.swing.JComboBox<String> DATEFROM;
    private javax.swing.JComboBox<String> DATETO;
    private javax.swing.JComboBox<String> FORMAT;
    private javax.swing.JLabel LOCATION;
    private javax.swing.JComboBox<String> MONTH;
    private javax.swing.JButton SAVELOCATION;
    private javax.swing.JButton SUBMIT;
    private javax.swing.JComboBox<String> TYPE;
    private javax.swing.JComboBox<String> YEAR;
    private javax.swing.JButton dashboardreturn;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JLabel username;
    // End of variables declaration//GEN-END:variables
}
