/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package financialtracker;

import java.awt.Color;
import java.awt.Image;
import java.awt.Paint;
import java.awt.image.BufferedImage;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;


/**
 *
 * @author bmwmi
 */
public class DashBoard extends javax.swing.JFrame {
/**
     * Creates new form DashBoard
     */
    private final String username;

    public DashBoard(String username) throws SQLException {
        this.username = username;
        initComponents();
        updateDate();
        generateBarGraph();
        initData();
        setSalaryAndBudgetFromDatabase();
        setLabels();
        updateAccountSummaryTable();
        checkAndUpdateMonthChange();
        SwingUtilities.invokeLater(() -> showWarnings());
    }
    
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void checkAndUpdateMonthChange() {
        String accountNumber = account.getText();
        // Get the current date
        int currentDay = LocalDate.now().getDayOfMonth();

        // Check if the current date is the 1st of the month
        if (currentDay == 1) {
            // Get the current month and year
            String currentMonth = LocalDate.now().getMonth().name().toUpperCase();
            int currentYear = LocalDate.now().getYear();

            // Get the previous month
            String[] monthNames = {"DECEMBER", "JANUARY", "FEBRUARY", "MARCH", "APRIL", "MAY", "JUNE", "JULY", "AUGUST", "SEPTEMBER", "OCTOBER", "NOVEMBER"};
            int prevMonthIndex = (LocalDate.now().getMonthValue() + 11) % 12; // Get the index of the previous month
            String prevMonth = monthNames[prevMonthIndex];

            // Update the transaction table with the account balance of the previous month
            double prevMonthBalance = getAccountBalanceForMonth(accountNumber, currentYear, prevMonth);
            String type = (prevMonthBalance < 0) ? "EXPENSE" : "INCOME";
            String reason = (prevMonthBalance < 0) ? "Loan" : "Saving";
            double amount = Math.abs(prevMonthBalance);

            try (Connection conn = new DataBase().getConnection()) {
                addAccountBalanceTransaction(conn, accountNumber, currentYear, currentMonth, amount, type, reason);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    
    public double getAccountBalanceForMonth(String acc, int year, String month) {
        double balance = 0.0;
        String sql = "SELECT Acc_Balance FROM account_summary WHERE Acc_Number = ? AND Year = ? AND Month = ?";
    
        try (Connection conn = new DataBase().getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, acc);
            pstmt.setInt(2, year);
            pstmt.setString(3, month);
            ResultSet rs = pstmt.executeQuery();
        
        if (rs.next()) {
            balance = rs.getDouble("Account_Balance");
        }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    
        return balance;
    }

    public void addAccountBalanceTransaction(Connection connection, String acc, int year, String month, double amount, String type, String reason) {
        // Prepare SQL statement to insert transaction
        String sql = "INSERT INTO transactions (Username, Year, Month, Date, Type, Reason, Amount) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            // Set parameters for the SQL statement
            pstmt.setString(1, acc);
            pstmt.setInt(2, year);
            pstmt.setString(3, month);
            pstmt.setDate(4, Date.valueOf(LocalDate.now())); // Use current date
            pstmt.setString(5, type);
            pstmt.setString(6, reason);
            pstmt.setDouble(7, amount);

            // Execute the SQL statement to insert the transaction
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void initData() throws SQLException {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = new DataBase().getConnection();
            String query = "SELECT Name, Contact_Number, Acc_Number, propic FROM login WHERE Id_Number = ?";
            pstmt = conn.prepareStatement(query);
            pstmt.setString(1, username);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                String name = rs.getString("Name");
                String contactNumber = rs.getString("Contact_Number");
                int accountNum = rs.getInt("Acc_Number");
                byte[] profilePictureBytes = rs.getBytes("propic");

                // Set the retrieved data to labels
                Name.setText(name);
                Contact.setText(contactNumber);
                IdNum.setText(username);
                account.setText(String.valueOf(accountNum));
                // Set the profile picture if the byte array is not null
                if (profilePictureBytes != null) {
                    // Convert the byte array to an ImageIcon
                    ImageIcon originalIcon = new ImageIcon(profilePictureBytes);
                    // Get the original image from the ImageIcon
                    Image originalImage = originalIcon.getImage();
                    // Get the label dimensions
                    int labelWidth = Profile.getWidth();
                    int labelHeight = Profile.getHeight();
                    // Resize the image while maintaining aspect ratio
                    Image scaledImage = originalImage.getScaledInstance(labelWidth, labelHeight, Image.SCALE_SMOOTH);
                    // Create a new ImageIcon from the scaled image
                    ImageIcon scaledIcon = new ImageIcon(scaledImage);
                    // Set the ImageIcon to the Profile label
                    Profile.setIcon(scaledIcon);
                }
            }
            } catch (SQLException ex) {
                // Handle any SQL exceptions
                ex.printStackTrace();
            } finally {
                // Close the ResultSet, PreparedStatement, and Connection
                if (rs != null) {
                    rs.close();
                }
            if (pstmt != null) {
                pstmt.close();
            }
            if (conn != null) {
                conn.close();
            }
        }
    }
    private void updateDate() {
        
        // Get the current date
        LocalDate currentDate = LocalDate.now();
        // Update labels with the current date
        date.setText(String.valueOf(currentDate.getDayOfMonth()));
        month.setText(currentDate.getMonth().toString());
        year.setText(String.valueOf(currentDate.getYear()));
    }
    
    private void generateBarGraph() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        // Get the current year
        int currentYear = LocalDate.now().getYear();

        // Define colors for income and expenses
        Paint[] colors = {Color.GREEN, Color.RED};

        // Define an array of month names
        String[] monthNames = {"JANUARY", "FEBRUARY", "MARCH", "APRIL", "MAY", "JUNE", "JULY", "AUGUST", "SEPTEMBER", "OCTOBER", "NOVEMBER", "DECEMBER"};

        // Iterate over 12 months
        for (int i = 0; i < 12; i++) {
            // Query to retrieve the user's account number
            String accQuery = "SELECT Acc_Number FROM login WHERE ID_Number = ?";
            int userAccountNumber = 0;

            // Query to retrieve total expenses for the current month
            String expensesQuery = "SELECT SUM(amount) AS total_expenses FROM transactions WHERE Type = 'EXPENSES' AND Month = ? AND Year = ? AND Acc_Number = ?";
            // Query to retrieve total incomes for the current month
            String incomesQuery = "SELECT SUM(amount) AS total_incomes FROM transactions WHERE Type = 'INCOMES' AND Month = ? AND Year = ? AND Acc_Number = ?";
        
            double totalExpenses = 0.0;
            double totalIncomes = 0.0;
            double grandTotal = 0.0;
            double salaryAmount = 0.0;

        try (Connection conn = new DataBase().getConnection();
             PreparedStatement accStmt = conn.prepareStatement(accQuery);
             PreparedStatement expensesStmt = conn.prepareStatement(expensesQuery);
             PreparedStatement incomesStmt = conn.prepareStatement(incomesQuery)) {

            // Set parameter for account query
            accStmt.setString(1, username);
            ResultSet accResult = accStmt.executeQuery();
            if (accResult.next()) {
                userAccountNumber = accResult.getInt("Acc_Number");
            }

            // Set parameters for expenses query
            expensesStmt.setString(1, monthNames[i]); // Month
            expensesStmt.setInt(2, currentYear); // Year
            expensesStmt.setInt(3, userAccountNumber); // Account Number
            ResultSet expensesResult = expensesStmt.executeQuery();
            if (expensesResult.next()) {
                totalExpenses = expensesResult.getDouble("total_expenses");
            }

            // Set parameters for incomes query
            incomesStmt.setString(1, monthNames[i]); // Month
            incomesStmt.setInt(2, currentYear); // Year
            incomesStmt.setInt(3, userAccountNumber); // Account Number
            ResultSet incomesResult = incomesStmt.executeQuery();
            if (incomesResult.next()) {
                totalIncomes = incomesResult.getDouble("total_incomes");
            }

            // Fetch salary amount from monthly plan table
            String salaryQuery = "SELECT Income FROM monthly_plan WHERE Year = ? AND Month = ? AND Acc_Number = ?";
            PreparedStatement salaryStmt = conn.prepareStatement(salaryQuery);
            salaryStmt.setInt(1, currentYear);
            salaryStmt.setString(2, monthNames[i]);
            salaryStmt.setInt(3, userAccountNumber); // Account Number
            ResultSet salaryResult = salaryStmt.executeQuery();
            if (salaryResult.next()) {
                salaryAmount = salaryResult.getDouble("Income");
            }

            grandTotal = salaryAmount + totalIncomes;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Add data to dataset
        dataset.addValue(totalExpenses, "Total Expenses", monthNames[i].substring(0, 3)); // Display only first 3 letters
        dataset.addValue(grandTotal, "Total Incomes", monthNames[i].substring(0, 3)); // Display only first 3 letters
        }

         // Create the bar chart using JFreeChart
        JFreeChart barChart = ChartFactory.createBarChart(
            "Total Transactions Summary", // Title
            "Month", // X-axis label
            "Amount", // Y-axis label
            dataset, // Dataset
            PlotOrientation.VERTICAL, // Plot orientation
            true, // Show legend
            true, // Show tooltips
            false // Show URLs
        );

        // Set colors for income and expense bars
        CategoryPlot plot = barChart.getCategoryPlot();
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, colors[1]); // Expenses color
        renderer.setSeriesPaint(1, colors[0]); // Income color

        // Set range for the y-axis
        ValueAxis rangeAxis = (ValueAxis) plot.getRangeAxis();
        rangeAxis.setRange(1000, 250000);

        // Render the chart as an image
        int width = Graph1.getWidth();
        int height = Graph1.getHeight();
        BufferedImage chartImage = barChart.createBufferedImage(width, height);

        // Set the image of the JLabel to the rendered chart image
        Graph1.setIcon(new ImageIcon(chartImage));
    }



    private void refreshDataAndGraph() throws SQLException {
        clearData();
        updateDate();
        generateBarGraph();
        initData();
        setSalaryAndBudgetFromDatabase();
        setLabels();
        updateAccountSummaryTable();
        checkAndUpdateMonthChange();
        SwingUtilities.invokeLater(() -> showWarnings());
    }
    private void clearData() {
        // Clear name, contact, and id labels
        Name.setText("");
        Contact.setText("");
        account.setText("");
        IdNum.setText("");
        // Clear profile picture
        Profile.setIcon(null);
        // Clear graph
        Graph1.setIcon(null);
        salary.setText("");
        budget.setText("");
    }
    
    private void setSalaryAndBudgetFromDatabase() {
        String accountNumber = account.getText();
    
        // Get the current month in uppercase format
        String currentMonthStr = LocalDate.now().getMonth().name().toUpperCase();
        int currentyearStr = LocalDate.now().getYear();
    
        try (Connection conn = new DataBase().getConnection();
        PreparedStatement pstmt = conn.prepareStatement("SELECT Income, Budget_Limit FROM monthly_plan WHERE Acc_Number = ? AND Month = ? AND Year = ?")) {
            pstmt.setString(1, accountNumber);
            pstmt.setString(2, currentMonthStr); // Use the uppercase month name
            pstmt.setInt(3, currentyearStr); // Use the uppercase month name
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String salaryValue = rs.getString("Income");
                String budgetValue = rs.getString("Budget_Limit");
    
                // Parse the String values to doubles
                double salaryDouble = Double.parseDouble(salaryValue);
                double budgetDouble = Double.parseDouble(budgetValue);
    
                // Set the double values to the labels
                salary.setText(String.valueOf(salaryDouble));
                budget.setText(String.valueOf(budgetDouble));
            } else {
                // If there's no data for the current month, you can handle it accordingly
                // For example, clear the text fields or display a message to the user
                salary.setText("Rs. 0.00");
                budget.setText("Rs. 0.00");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    private void setLabels() {
        String accountNumber = account.getText();
        try (Connection conn = new DataBase().getConnection()) {
            // Calculate total income
            double totalIncome = getTransactionTotal(conn, "INCOMES");
            double salaryAmount = parseLabelValue(salary.getText()); // Assuming salary is a label
            double grandTotalIncome = totalIncome + salaryAmount;
            totincome.setText(String.valueOf(grandTotalIncome));

            // Calculate total expenses
            double totalExpenses = getTransactionTotal(conn, "EXPENSES");
            totexpenses.setText(String.valueOf(totalExpenses));

            // Calculate remaining budget
            double budgetAmount = parseLabelValue(budget.getText()); // Assuming budget is a label
            double remainingBudget = budgetAmount - totalExpenses;
            budremain.setText(String.valueOf(remainingBudget));

            // Calculate account balance
            double accountBalance = grandTotalIncome - totalExpenses;
            ACCBALANCE.setText(String.valueOf(accountBalance));
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void showWarnings() {
        String accountNumber = account.getText();
        try (Connection conn = new DataBase().getConnection()) {
            // Calculate total income
            double totalIncome = getTransactionTotal(conn, "INCOMES");
            double salaryAmount = parseLabelValue(salary.getText());
            double grandTotalIncome = totalIncome + salaryAmount;

            // Calculate total expenses
            double totalExpenses = getTransactionTotal(conn, "EXPENSES");

            // Calculate remaining budget
            double budgetAmount = parseLabelValue(budget.getText());
            double remainingBudget = budgetAmount - totalExpenses;

            // Calculate account balance
            double accountBalance = grandTotalIncome - totalExpenses;

            // Check if transactions and budget plan exist
            boolean transactionsExist = transactionsExist(accountNumber);
            boolean budgetPlanExists = budgetPlanExists(accountNumber);

            // Show warning messages based on existence of transactions and budget plan
            if (transactionsExist && budgetPlanExists) {
                if (totalExpenses > budgetAmount) {
                    JOptionPane.showMessageDialog(null, "Budget limit exceeded!", "Warning", JOptionPane.WARNING_MESSAGE);
                }
                if (remainingBudget <= 100) {
                    JOptionPane.showMessageDialog(null, "Budget limit will be exceeded soon!", "Warning", JOptionPane.WARNING_MESSAGE);
                }
                if (accountBalance <= 0) {
                    JOptionPane.showMessageDialog(null, "You don't have any savings for this month!", "Warning", JOptionPane.WARNING_MESSAGE);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
    
    private boolean transactionsExist(String accountNumber) throws SQLException {
        try (Connection conn = new DataBase().getConnection();
        PreparedStatement pstmt = conn.prepareStatement("SELECT COUNT(*) FROM transactions WHERE Acc_Number = ?")) {
            pstmt.setString(1, accountNumber);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt(1);
                    return count > 0;
                }
            }
        }
        return false;
    }

    private boolean budgetPlanExists(String accountNumber) throws SQLException {
        try (Connection conn = new DataBase().getConnection();
        PreparedStatement pstmt = conn.prepareStatement("SELECT COUNT(*) FROM monthly_plan WHERE Acc_Number = ?")) {
            pstmt.setString(1, accountNumber);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt(1);
                    return count > 0;
                }
            }
        }
        return false;
    }

    private double parseLabelValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            // Handle empty or null value
            return 0.0; // Default value or handle differently based on your requirements
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            // Handle invalid value
            return 0.0; // Default value or handle differently based on your requirements
        }
    }

    public static void updateAccountSummary(Connection connection, String accountNumber, int year, String month, double accountBalance) {
        boolean rowExists = checkRowExists(accountNumber, year, month);
        if (rowExists) {
            updateExistingRow(accountNumber, year, month, accountBalance);
        } else {
            insertNewRow(accountNumber, year, month, accountBalance);
        }
    }

    private void updateAccountSummaryTable() {
        // Handle any SQL exceptions
        String accountnum = account.getText();
        String monthnew = LocalDate.now().getMonth().name().toUpperCase();
        int yearnew = LocalDate.now().getYear();
        double accountBalance = Double.parseDouble(ACCBALANCE.getText());
        try (Connection conn = new DataBase().getConnection()) {
            updateAccountSummary(conn, accountnum, yearnew, monthnew, accountBalance);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


   // Method to check if a row already exists for the specified month and year
    private static boolean checkRowExists(String accountNumber, int year, String month) {
        String query = "SELECT COUNT(*) FROM account_summary WHERE Acc_Number = ? AND Year = ? AND Month = ?";
        try (Connection conn = new DataBase().getConnection();
        PreparedStatement preparedStatement = conn.prepareStatement(query)) {
            preparedStatement.setString(1, accountNumber);
            preparedStatement.setInt(2, year);
            preparedStatement.setString(3, month);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    int count = resultSet.getInt(1);
                    return count > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Method to update an existing row in the Account_Summary table
    private static void updateExistingRow(String accountNumber, int year, String month, double accountBalance) {
        String updateQuery = "UPDATE account_summary SET Acc_Balance = ? WHERE Acc_Number = ? AND Year = ? AND Month = ?";
        try (Connection conn = new DataBase().getConnection();
        PreparedStatement preparedStatement = conn.prepareStatement(updateQuery)) {
            preparedStatement.setDouble(1, accountBalance);
            preparedStatement.setString(2, accountNumber);
            preparedStatement.setInt(3, year);
            preparedStatement.setString(4, month);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Method to insert a new row into the Account_Summary table
    private static void insertNewRow(String accountNumber, int year, String month, double accountBalance) {
        String insertQuery = "INSERT INTO account_summary (Acc_Number, Year, Month, Acc_Balance) VALUES (?, ?, ?, ?)";
        try (Connection conn = new DataBase().getConnection();
        PreparedStatement preparedStatement = conn.prepareStatement(insertQuery)) {
            preparedStatement.setString(1, accountNumber);
            preparedStatement.setInt(2, year);
            preparedStatement.setString(3, month);
            preparedStatement.setDouble(4, accountBalance);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    
    private double getTransactionTotal(Connection conn, String type) throws SQLException {
        String month = LocalDate.now().getMonth().name().toUpperCase();
        int year = LocalDate.now().getYear();
        String query = "SELECT SUM(Amount) FROM transactions WHERE Acc_Number = ? AND Type = ? AND Year= ? AND Month = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, account.getText()); // Assuming account is a label
            pstmt.setString(2, type);
            pstmt.setInt(3, year); // Assuming year is an int variable
            pstmt.setString(4, month); // Assuming month is an string variable
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        }
        return 0;
    }

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        Profile = new javax.swing.JLabel();
        name1 = new javax.swing.JLabel();
        Account1 = new javax.swing.JLabel();
        Contact1 = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        jSeparator2 = new javax.swing.JSeparator();
        REPORTS = new javax.swing.JButton();
        BUDGET = new javax.swing.JButton();
        EXIT = new javax.swing.JButton();
        SETTINGS = new javax.swing.JButton();
        TRANSACTIONS = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        budget = new javax.swing.JLabel();
        salary = new javax.swing.JLabel();
        totexpenses = new javax.swing.JLabel();
        budremain = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        totincome = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        ACCBALANCE = new javax.swing.JLabel();
        moneytype = new javax.swing.JLabel();
        moneytype1 = new javax.swing.JLabel();
        moneytype2 = new javax.swing.JLabel();
        moneytype3 = new javax.swing.JLabel();
        moneytype4 = new javax.swing.JLabel();
        moneytype5 = new javax.swing.JLabel();
        Graph = new javax.swing.JPanel();
        Graph1 = new javax.swing.JLabel();
        account = new javax.swing.JLabel();
        Contact = new javax.swing.JLabel();
        Name = new javax.swing.JLabel();
        jPanel5 = new javax.swing.JPanel();
        date = new javax.swing.JLabel();
        month = new javax.swing.JLabel();
        year = new javax.swing.JLabel();
        REFRESH = new javax.swing.JButton();
        Id2 = new javax.swing.JLabel();
        IdNum = new javax.swing.JLabel();
        LOGOUT = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setBackground(new java.awt.Color(7, 7, 77));
        jPanel1.setPreferredSize(new java.awt.Dimension(1130, 700));

        jLabel5.setBackground(new java.awt.Color(255, 255, 255));
        jLabel5.setFont(new java.awt.Font("Rockwell Condensed", 1, 28)); // NOI18N
        jLabel5.setForeground(new java.awt.Color(191, 191, 191));
        jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel5.setText("WELCOME TO THE BUDGET BUDDY!! YOUR PERSONAL FINANCIAL PARTNER");

        Profile.setIcon(new javax.swing.ImageIcon(getClass().getResource("/financialtracker/user.jpg"))); // NOI18N

        name1.setBackground(new java.awt.Color(0, 0, 0));
        name1.setFont(new java.awt.Font("Rockwell", 3, 18)); // NOI18N
        name1.setForeground(new java.awt.Color(191, 191, 191));
        name1.setText("NAME :");

        Account1.setBackground(new java.awt.Color(0, 0, 0));
        Account1.setFont(new java.awt.Font("Rockwell", 3, 18)); // NOI18N
        Account1.setForeground(new java.awt.Color(191, 191, 191));
        Account1.setText("ACCOUNT NUMBER :");

        Contact1.setBackground(new java.awt.Color(0, 0, 0));
        Contact1.setFont(new java.awt.Font("Rockwell", 3, 18)); // NOI18N
        Contact1.setForeground(new java.awt.Color(191, 191, 191));
        Contact1.setText("CONTACT NUMBER :");

        jSeparator1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        jSeparator2.setForeground(new java.awt.Color(0, 0, 0));
        jSeparator2.setOrientation(javax.swing.SwingConstants.VERTICAL);
        jSeparator2.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        REPORTS.setText("GENERATE REPORTS");
        REPORTS.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        REPORTS.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                REPORTSActionPerformed(evt);
            }
        });

        BUDGET.setText("BUDGET PLANNER");
        BUDGET.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        BUDGET.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BUDGETActionPerformed(evt);
            }
        });

        EXIT.setBackground(new java.awt.Color(255, 51, 51));
        EXIT.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        EXIT.setForeground(new java.awt.Color(255, 255, 255));
        EXIT.setText("EXIT");
        EXIT.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                EXITActionPerformed(evt);
            }
        });

        SETTINGS.setText("PROFILE SETTINGS");
        SETTINGS.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        SETTINGS.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SETTINGSActionPerformed(evt);
            }
        });

        TRANSACTIONS.setText("TRANSACTIONS MANAGER");
        TRANSACTIONS.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        TRANSACTIONS.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                TRANSACTIONSActionPerformed(evt);
            }
        });

        jPanel2.setBackground(new java.awt.Color(7, 7, 77));
        jPanel2.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0), 1, true));

        jLabel2.setFont(new java.awt.Font("Myanmar Text", 1, 14)); // NOI18N
        jLabel2.setForeground(new java.awt.Color(191, 191, 191));
        jLabel2.setText("EXPENSES UPTO NOW :");

        jLabel6.setFont(new java.awt.Font("Myanmar Text", 1, 14)); // NOI18N
        jLabel6.setForeground(new java.awt.Color(191, 191, 191));
        jLabel6.setText("MONTHLY BUDGET LIMIT :");

        jLabel7.setFont(new java.awt.Font("Myanmar Text", 1, 14)); // NOI18N
        jLabel7.setForeground(new java.awt.Color(191, 191, 191));
        jLabel7.setText("MONTHLY SALARY :");

        jLabel8.setFont(new java.awt.Font("Myanmar Text", 1, 14)); // NOI18N
        jLabel8.setForeground(new java.awt.Color(191, 191, 191));
        jLabel8.setText("REMAINING BUDGET :");

        budget.setBackground(new java.awt.Color(7, 7, 77));
        budget.setFont(new java.awt.Font("Rockwell", 1, 14)); // NOI18N
        budget.setForeground(new java.awt.Color(191, 191, 191));
        budget.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        budget.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(255, 255, 255)));
        budget.setOpaque(true);

        salary.setBackground(new java.awt.Color(7, 7, 77));
        salary.setFont(new java.awt.Font("Rockwell", 1, 14)); // NOI18N
        salary.setForeground(new java.awt.Color(191, 191, 191));
        salary.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        salary.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(255, 255, 255)));
        salary.setOpaque(true);

        totexpenses.setBackground(new java.awt.Color(7, 7, 77));
        totexpenses.setFont(new java.awt.Font("Rockwell", 1, 14)); // NOI18N
        totexpenses.setForeground(new java.awt.Color(191, 191, 191));
        totexpenses.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        totexpenses.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(255, 255, 255)));
        totexpenses.setOpaque(true);

        budremain.setBackground(new java.awt.Color(7, 7, 77));
        budremain.setFont(new java.awt.Font("Rockwell", 1, 14)); // NOI18N
        budremain.setForeground(new java.awt.Color(191, 191, 191));
        budremain.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        budremain.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(255, 255, 255)));
        budremain.setOpaque(true);

        jLabel9.setFont(new java.awt.Font("Myanmar Text", 1, 14)); // NOI18N
        jLabel9.setForeground(new java.awt.Color(191, 191, 191));
        jLabel9.setText("TOTAL INCOME FOR THE MONTH :");

        totincome.setBackground(new java.awt.Color(7, 7, 77));
        totincome.setFont(new java.awt.Font("Rockwell", 1, 14)); // NOI18N
        totincome.setForeground(new java.awt.Color(191, 191, 191));
        totincome.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        totincome.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(255, 255, 255)));
        totincome.setOpaque(true);

        jLabel10.setFont(new java.awt.Font("Myanmar Text", 1, 14)); // NOI18N
        jLabel10.setForeground(new java.awt.Color(191, 191, 191));
        jLabel10.setText("BALANCE IN THE ACCOUNT :");

        ACCBALANCE.setBackground(new java.awt.Color(7, 7, 77));
        ACCBALANCE.setFont(new java.awt.Font("Rockwell", 1, 14)); // NOI18N
        ACCBALANCE.setForeground(new java.awt.Color(191, 191, 191));
        ACCBALANCE.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        ACCBALANCE.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(255, 255, 255)));
        ACCBALANCE.setOpaque(true);

        moneytype.setBackground(new java.awt.Color(7, 7, 77));
        moneytype.setFont(new java.awt.Font("Rockwell", 1, 14)); // NOI18N
        moneytype.setForeground(new java.awt.Color(191, 191, 191));
        moneytype.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        moneytype.setText("Rs.");
        moneytype.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(255, 255, 255)));
        moneytype.setOpaque(true);

        moneytype1.setBackground(new java.awt.Color(7, 7, 77));
        moneytype1.setFont(new java.awt.Font("Rockwell", 1, 14)); // NOI18N
        moneytype1.setForeground(new java.awt.Color(191, 191, 191));
        moneytype1.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        moneytype1.setText("Rs.");
        moneytype1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(255, 255, 255)));
        moneytype1.setOpaque(true);

        moneytype2.setBackground(new java.awt.Color(7, 7, 77));
        moneytype2.setFont(new java.awt.Font("Rockwell", 1, 14)); // NOI18N
        moneytype2.setForeground(new java.awt.Color(191, 191, 191));
        moneytype2.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        moneytype2.setText("Rs.");
        moneytype2.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(255, 255, 255)));
        moneytype2.setOpaque(true);

        moneytype3.setBackground(new java.awt.Color(7, 7, 77));
        moneytype3.setFont(new java.awt.Font("Rockwell", 1, 14)); // NOI18N
        moneytype3.setForeground(new java.awt.Color(191, 191, 191));
        moneytype3.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        moneytype3.setText("Rs.");
        moneytype3.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(255, 255, 255)));
        moneytype3.setOpaque(true);

        moneytype4.setBackground(new java.awt.Color(7, 7, 77));
        moneytype4.setFont(new java.awt.Font("Rockwell", 1, 14)); // NOI18N
        moneytype4.setForeground(new java.awt.Color(191, 191, 191));
        moneytype4.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        moneytype4.setText("Rs.");
        moneytype4.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(255, 255, 255)));
        moneytype4.setOpaque(true);

        moneytype5.setBackground(new java.awt.Color(7, 7, 77));
        moneytype5.setFont(new java.awt.Font("Rockwell", 1, 14)); // NOI18N
        moneytype5.setForeground(new java.awt.Color(191, 191, 191));
        moneytype5.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        moneytype5.setText("Rs.");
        moneytype5.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(255, 255, 255)));
        moneytype5.setOpaque(true);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel10)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(moneytype5, javax.swing.GroupLayout.PREFERRED_SIZE, 54, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel8)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(moneytype4, javax.swing.GroupLayout.PREFERRED_SIZE, 54, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(moneytype3, javax.swing.GroupLayout.PREFERRED_SIZE, 54, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel6)
                            .addComponent(jLabel7))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(moneytype, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 54, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(moneytype1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 54, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel9)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 13, Short.MAX_VALUE)
                        .addComponent(moneytype2, javax.swing.GroupLayout.PREFERRED_SIZE, 54, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(ACCBALANCE, javax.swing.GroupLayout.DEFAULT_SIZE, 171, Short.MAX_VALUE)
                    .addComponent(budremain, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(totexpenses, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(totincome, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(salary, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(budget, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel7)
                    .addComponent(salary, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(moneytype, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel6)
                        .addComponent(moneytype1, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(budget, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel9)
                        .addComponent(moneytype2, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(totincome, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel2)
                    .addComponent(totexpenses, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(moneytype3, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel8)
                        .addComponent(moneytype4, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(budremain, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel10)
                    .addComponent(ACCBALANCE, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(moneytype5, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(20, Short.MAX_VALUE))
        );

        Graph.setBackground(new java.awt.Color(7, 7, 77));

        Graph1.setOpaque(true);

        javax.swing.GroupLayout GraphLayout = new javax.swing.GroupLayout(Graph);
        Graph.setLayout(GraphLayout);
        GraphLayout.setHorizontalGroup(
            GraphLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(Graph1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        GraphLayout.setVerticalGroup(
            GraphLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(Graph1, javax.swing.GroupLayout.DEFAULT_SIZE, 372, Short.MAX_VALUE)
        );

        account.setBackground(new java.awt.Color(7, 7, 77));
        account.setFont(new java.awt.Font("Rockwell", 1, 18)); // NOI18N
        account.setForeground(new java.awt.Color(191, 191, 191));
        account.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        account.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(255, 255, 255)));
        account.setOpaque(true);

        Contact.setBackground(new java.awt.Color(7, 7, 77));
        Contact.setFont(new java.awt.Font("Rockwell", 1, 18)); // NOI18N
        Contact.setForeground(new java.awt.Color(191, 191, 191));
        Contact.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        Contact.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(255, 255, 255)));
        Contact.setOpaque(true);

        Name.setBackground(new java.awt.Color(7, 7, 77));
        Name.setFont(new java.awt.Font("Rockwell", 1, 18)); // NOI18N
        Name.setForeground(new java.awt.Color(191, 191, 191));
        Name.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        Name.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(255, 255, 255)));
        Name.setOpaque(true);

        jPanel5.setBackground(new java.awt.Color(7, 7, 77));

        date.setFont(new java.awt.Font("Tahoma", 1, 30)); // NOI18N
        date.setForeground(new java.awt.Color(191, 191, 191));
        date.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        date.setText("00");

        month.setFont(new java.awt.Font("Tahoma", 1, 30)); // NOI18N
        month.setForeground(new java.awt.Color(191, 191, 191));
        month.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        month.setText("AAAA");

        year.setFont(new java.awt.Font("Tahoma", 1, 30)); // NOI18N
        year.setForeground(new java.awt.Color(191, 191, 191));
        year.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        year.setText("0000");

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(date, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(month, javax.swing.GroupLayout.PREFERRED_SIZE, 112, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(year, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(33, 33, 33))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(date, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(month, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(year, javax.swing.GroupLayout.DEFAULT_SIZE, 49, Short.MAX_VALUE))
                .addContainerGap())
        );

        REFRESH.setBackground(new java.awt.Color(0, 255, 51));
        REFRESH.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        REFRESH.setText("REFRESH");
        REFRESH.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                REFRESHActionPerformed(evt);
            }
        });

        Id2.setBackground(new java.awt.Color(0, 0, 0));
        Id2.setFont(new java.awt.Font("Rockwell", 3, 18)); // NOI18N
        Id2.setForeground(new java.awt.Color(191, 191, 191));
        Id2.setText("ID NUMBER :");

        IdNum.setBackground(new java.awt.Color(7, 7, 77));
        IdNum.setFont(new java.awt.Font("Rockwell", 1, 18)); // NOI18N
        IdNum.setForeground(new java.awt.Color(191, 191, 191));
        IdNum.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        IdNum.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(255, 255, 255)));
        IdNum.setOpaque(true);

        LOGOUT.setBackground(new java.awt.Color(255, 51, 51));
        LOGOUT.setForeground(new java.awt.Color(255, 255, 255));
        LOGOUT.setText("LOGOUT");
        LOGOUT.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                LOGOUTActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSeparator1)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(Profile, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(name1, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(Name, javax.swing.GroupLayout.PREFERRED_SIZE, 299, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                .addComponent(Contact1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(Contact, javax.swing.GroupLayout.PREFERRED_SIZE, 299, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                .addComponent(Account1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 63, Short.MAX_VALUE)
                                .addComponent(account, javax.swing.GroupLayout.PREFERRED_SIZE, 299, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(Id2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(IdNum, javax.swing.GroupLayout.PREFERRED_SIZE, 299, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 47, Short.MAX_VALUE)
                        .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 19, Short.MAX_VALUE)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(EXIT)
                                .addGap(35, 35, 35)
                                .addComponent(LOGOUT)
                                .addGap(32, 32, 32)
                                .addComponent(REFRESH, javax.swing.GroupLayout.PREFERRED_SIZE, 91, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(SETTINGS, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(REPORTS, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(BUDGET, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(TRANSACTIONS, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE))))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(80, 80, 80)
                                .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(Graph, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 185, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGroup(jPanel1Layout.createSequentialGroup()
                                        .addComponent(TRANSACTIONS)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(BUDGET)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(REPORTS)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(SETTINGS)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(REFRESH)
                                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                .addComponent(LOGOUT)
                                                .addComponent(EXIT))))))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(72, 72, 72)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(name1)
                                    .addComponent(Name, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(Account1)
                                    .addComponent(account, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(Contact1)
                                    .addComponent(Contact, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(Id2)
                                    .addComponent(IdNum, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(0, 0, Short.MAX_VALUE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(33, 33, 33)
                        .addComponent(Profile)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 29, Short.MAX_VALUE)))
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(Graph, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(8, 8, 8)))
                .addGap(17, 17, 17))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 1133, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 703, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void BUDGETActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BUDGETActionPerformed
        // TODO add your handling code here:
        String accountnum = account.getText();
        this.hide();
        new Budget(accountnum).setVisible(true);
    }//GEN-LAST:event_BUDGETActionPerformed

    private void TRANSACTIONSActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_TRANSACTIONSActionPerformed
        // TODO add your handling code here:
        String accountnum = account.getText();
        this.hide();
        new Transactions(accountnum).setVisible(true);
    }//GEN-LAST:event_TRANSACTIONSActionPerformed

    private void REFRESHActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_REFRESHActionPerformed
        try {
        refreshDataAndGraph();
    } catch (SQLException ex) {
        Logger.getLogger(DashBoard.class.getName()).log(Level.SEVERE, null, ex);
    }
    }//GEN-LAST:event_REFRESHActionPerformed

    private void EXITActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_EXITActionPerformed
        // TODO add your handling code here:
        this.dispose();
    }//GEN-LAST:event_EXITActionPerformed

    private void REPORTSActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_REPORTSActionPerformed
        // TODO add your handling code here:
        String accountnum = account.getText();
        this.hide();
        new GenerateReport(accountnum).setVisible(true);
    }//GEN-LAST:event_REPORTSActionPerformed

    private void SETTINGSActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SETTINGSActionPerformed
        // TODO add your handling code here:
        String accountnum = account.getText();
        this.hide();
        new ProfileSettings(accountnum).setVisible(true);
    }//GEN-LAST:event_SETTINGSActionPerformed

    private void LOGOUTActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_LOGOUTActionPerformed
        // TODO add your handling code here:
        this.dispose();
        new LoginAndSignUp().setVisible(true);
    }//GEN-LAST:event_LOGOUTActionPerformed

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
            java.util.logging.Logger.getLogger(DashBoard.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(DashBoard.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(DashBoard.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(DashBoard.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    new DashBoard("username").setVisible(true);
                    new DashBoard("username").updateDate();
                } catch (SQLException ex) {
                    Logger.getLogger(DashBoard.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel ACCBALANCE;
    private javax.swing.JLabel Account1;
    private javax.swing.JButton BUDGET;
    private javax.swing.JLabel Contact;
    private javax.swing.JLabel Contact1;
    private javax.swing.JButton EXIT;
    private javax.swing.JPanel Graph;
    private javax.swing.JLabel Graph1;
    private javax.swing.JLabel Id2;
    private javax.swing.JLabel IdNum;
    private javax.swing.JButton LOGOUT;
    private javax.swing.JLabel Name;
    private javax.swing.JLabel Profile;
    private javax.swing.JButton REFRESH;
    private javax.swing.JButton REPORTS;
    private javax.swing.JButton SETTINGS;
    private javax.swing.JButton TRANSACTIONS;
    private javax.swing.JLabel account;
    private javax.swing.JLabel budget;
    private javax.swing.JLabel budremain;
    private javax.swing.JLabel date;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JLabel moneytype;
    private javax.swing.JLabel moneytype1;
    private javax.swing.JLabel moneytype2;
    private javax.swing.JLabel moneytype3;
    private javax.swing.JLabel moneytype4;
    private javax.swing.JLabel moneytype5;
    private javax.swing.JLabel month;
    private javax.swing.JLabel name1;
    private javax.swing.JLabel salary;
    private javax.swing.JLabel totexpenses;
    private javax.swing.JLabel totincome;
    private javax.swing.JLabel year;
    // End of variables declaration//GEN-END:variables
}
