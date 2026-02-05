# JAVA Financial Tracker

A comprehensive personal finance management application built with Java Swing that helps users track their income, expenses, budgets, and generate financial reports.

## 📋 Features

### User Management
- **User Registration & Authentication**: Secure sign-up and login system with password hashing
- **Password Recovery**: Forgot password functionality
- **Profile Management**: Users can view and update their profile settings

### Financial Tracking
- **Transaction Management**: 
  - Add, view, edit, and delete financial transactions
  - Track income and expenses with categories
  - Monthly transaction overview
  
- **Budget Planning**:
  - Create and manage monthly budget plans
  - Set budget limits for different expense categories
  - View and edit existing budget plans
  - Track budget vs. actual spending

- **Dashboard**:
  - Visual representation of financial data with bar graphs (using JFreeChart)
  - Real-time overview of income, expenses, and budget status
  - Current month financial summary

- **Financial Reports**:
  - Generate comprehensive financial reports
  - Export reports to PDF format (using Apache PDFBox)
  - Visual charts and data analysis

## 🛠️ Technologies Used

- **Programming Language**: Java
- **GUI Framework**: Java Swing
- **Database**: MySQL (budget_buddy database)
- **Build Tool**: Apache Ant (NetBeans project)
- **Libraries**:
  - MySQL Connector (8.0.17) - Database connectivity
  - JFreeChart (1.0.19) - Chart generation and data visualization
  - Apache POI (5.2.5) - Excel file handling
  - Apache PDFBox (2.0.31) - PDF report generation
  - Apache Commons Libraries (IO, Compress, Codec, Collections, Math, Logging)

## 📋 Prerequisites

Before running this application, ensure you have the following installed:

- Java Development Kit (JDK) 8 or higher
- MySQL Server (5.7 or higher)
- NetBeans IDE (recommended) or any Java IDE
- Apache Ant (usually bundled with NetBeans)

## ⚙️ Installation

### 1. Clone the Repository
```bash
git clone https://github.com/BimalaWijekoon/JAVA-Financial-Tracker-.git
cd JAVA-Financial-Tracker-
```

### 2. Database Setup

1. Start your MySQL server
2. Create a new database named `budget_buddy`:
```sql
CREATE DATABASE budget_buddy;
```

3. The application will create necessary tables on first run. The database schema includes:
   - Users table (for authentication)
   - Transactions table (for income/expense records)
   - Budget table (for budget plans)
   - Additional tables for user profiles and settings

4. **Configure Database Connection**:
   - Open `src/financialtracker/DataBase.java`
   - Update the database credentials if needed (default: username=root, password=empty):
   ```java
   private String url = "jdbc:mysql://localhost:3306/budget_buddy?zeroDateTimeBehavior=convertToNull";
   private String un = "root";  // Your MySQL username
   private String pw = "";      // Your MySQL password
   ```

### 3. Build and Run

#### Using NetBeans:
1. Open the project in NetBeans IDE
2. Right-click on the project and select "Clean and Build"
3. Run the project (F6 or right-click → Run)

#### Using Command Line:
```bash
# Build the project
ant clean
ant compile
ant jar

# Run the application
java -jar dist/FinancialTracker.jar
```

## 🚀 Usage

1. **First Time Setup**:
   - Launch the application
   - Click on "Sign Up" to create a new account
   - Fill in your details and create your profile

2. **Login**:
   - Enter your username and password
   - Click "Login" to access the dashboard

3. **Dashboard**:
   - View your financial overview
   - Access different modules through the navigation menu

4. **Managing Transactions**:
   - Navigate to the Transactions section
   - Add new income or expense entries
   - View, edit, or delete existing transactions

5. **Budget Planning**:
   - Go to the Budget section
   - Set up monthly budget plans
   - Track spending against your budget

6. **Generate Reports**:
   - Access the Reports section
   - Select date range and report type
   - Generate and export reports as PDF

## 📁 Project Structure

```
JAVA-Financial-Tracker-/
├── src/financialtracker/
│   ├── FinancialTracker.java      # Main application entry point
│   ├── LoginAndSignUp.java        # Authentication interface
│   ├── SignUp.java                # User registration
│   ├── ForgotPassword.java        # Password recovery
│   ├── DashBoard.java             # Main dashboard with charts
│   ├── HomePage.java              # Home page interface
│   ├── Transactions.java          # Transaction management hub
│   ├── AddTransactions.java       # Add new transactions
│   ├── ViewTransactions.java      # View transaction history
│   ├── EditTransactions.java      # Edit existing transactions
│   ├── Budget.java                # Budget management hub
│   ├── SetupBudget.java           # Create budget plans
│   ├── ViewBudgetPlans.java       # View budget plans
│   ├── EditBudgetPlan.java        # Edit budget plans
│   ├── GenerateReport.java        # Financial report generation
│   ├── ProfileSettings.java       # User profile management
│   ├── DataBase.java              # Database connection handler
│   ├── UserUtility.java           # User-related utilities
│   ├── ValidationUtility.java     # Input validation utilities
│   └── [Various .form and .png files] # GUI forms and images
├── dist/                          # Compiled distribution files
├── build/                         # Build output directory
├── nbproject/                     # NetBeans project configuration
├── build.xml                      # Ant build script
├── manifest.mf                    # JAR manifest file
└── README.md                      # This file
```

## 🎨 Screenshots

The application includes a rich graphical user interface with various screens for different functionalities. Image assets are located in the `src/financialtracker/` directory.

## 👨‍💻 Author

- **Bimala Wijekoon** ([@BimalaWijekoon](https://github.com/BimalaWijekoon))

## 📝 License

This project is available for educational and personal use.

## 🤝 Contributing

Contributions, issues, and feature requests are welcome! Feel free to check the issues page.

## ⚠️ Important Notes

- Make sure MySQL server is running before starting the application
- Default database credentials are set to root with no password - change these for production use
- The application requires all dependencies in the `dist/lib/` folder to run properly
- Keep your database backed up regularly to prevent data loss

## 🔒 Security Considerations

- Passwords are hashed using MessageDigest before storage
- Always use strong passwords for database connections
- Never commit database credentials to version control
- Consider implementing additional security measures for production deployment

---

For any questions or support, please open an issue on the GitHub repository.
