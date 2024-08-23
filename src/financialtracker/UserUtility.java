/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package financialtracker;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

public class UserUtility {

    public static void setUserNameFromDatabase(String accountNumber, JLabel usernameLabel, JLabel accountLabel) throws java.sql.SQLException {
        try (Connection conn = new DataBase().getConnection();
                PreparedStatement pstmt = conn.prepareStatement("SELECT Name FROM login WHERE Acc_Number = ?")) {
            pstmt.setString(1, accountNumber);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String name = rs.getString("Name");
                usernameLabel.setText(name);
                accountLabel.setText(accountNumber);
            }
        }    
    }
    
    public static String getUsernameByAccountNumber(String accountNumber) {
        String userName = null;
        try (Connection conn = new DataBase().getConnection();
                PreparedStatement pstmt = conn.prepareStatement("SELECT Id_Number FROM login WHERE Acc_Number = ?")) {
            pstmt.setString(1, accountNumber);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                userName = rs.getString("Id_Number");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "An error occurred while retrieving username.", "Error", JOptionPane.ERROR_MESSAGE);
        }
        return userName;
    }
}
