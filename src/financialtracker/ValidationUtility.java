/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package financialtracker;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;

public class ValidationUtility {

     public static boolean isMatchingPassword(String enteredPassword, String storedPassword) {
        return enteredPassword.equals(storedPassword);
    }
     
    public static boolean isValidSecurityAnswer(String userId, String userAnswer) {
        try (Connection conn = new DataBase().getConnection();
                PreparedStatement pstmt = conn.prepareStatement("SELECT Security FROM login WHERE Id_Number = ?")) {
            pstmt.setString(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String storedAnswer = rs.getString("Security");
                return userAnswer.equals(storedAnswer);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean isValidEmail(String email) {
        // Regular expression pattern for email validation
        String emailPattern = "^[a-zA-Z0-9._%+-]+@gmail\\.com$";
        return Pattern.matches(emailPattern, email);
    }

    public static boolean isValidContactNumber(String contactNumber) {
        // Regular expression pattern for contact number validation (only digits allowed)
        return Pattern.matches("\\d+", contactNumber);
    }

    public static boolean isValidPassword(String password) {
        // Regular expression pattern for password validation
        String passwordPattern = "^(?=.*[0-9])(?=.*[a-zA-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$";
        return Pattern.matches(passwordPattern, password);
    }

    public static String encryptPassword(String password) {
        try {
            // Use SHA-256 for password encryption
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            // Handle hashing algorithm exception
            ex.printStackTrace();
            return null;
        }
    }
}
