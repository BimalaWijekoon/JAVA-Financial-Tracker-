/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package financialtracker;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DataBase {
    // Private variables for database connection
    private String url = "jdbc:mysql://localhost:3306/budget_buddy?zeroDateTimeBehavior=convertToNull";
    private String un = "root";
    private String pw = "";
    private Connection con;

    // Method to establish database connection
    public Connection getConnection() {
        try {
            // Register the JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            // Establish the connection
            con = DriverManager.getConnection(url, un, pw);
            
            if(con != null) {
                System.out.println("Connected to the database successfully!");
            }
        } catch (ClassNotFoundException e) {
            System.out.println("Could not find the JDBC driver class: " + e.getMessage());
        } catch (SQLException e) {
            System.out.println("An error occurred while connecting to the database: " + e.getMessage());
        }
        return con;
    }

    // Method to close the database connection
    public void closeConnection(Connection con) {
        try {
            if (con != null && !con.isClosed()) {
                con.close();
                System.out.println("Database connection closed.");
            }
        } catch (SQLException e) {
            System.out.println("An error occurred while closing the database connection: " + e.getMessage());
        }
    }
    
    // Method to execute a query
    public boolean execute(String query, Connection con) {
        boolean status = false;
        Statement stmt = null;
        try {
            stmt = con.createStatement();
            stmt.executeUpdate(query);
            status = true;
        } catch (SQLException ex) {
            Logger.getLogger(DataBase.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException ex) {
                    Logger.getLogger(DataBase.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return status;
    }

    
    // Method to retrieve data from the database
    public ResultSet getData(String query) {
        ResultSet rs = null;
        try {
            // Establish connection
            Connection con = getConnection();
            
            // Create statement and execute query
            Statement stmt = con.createStatement();
            rs = stmt.executeQuery(query);
        } catch (SQLException ex) {
            Logger.getLogger(DataBase.class.getName()).log(Level.SEVERE, null, ex);
        }
        return rs;
    }
}