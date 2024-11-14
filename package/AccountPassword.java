// This code is using SQLite for database management and Java for GUI automation (similar to pyautogui).
import java.sql.*;
import java.util.Scanner;
import org.mindrot.jbcrypt.BCrypt;

public class AccountPasswordManager {
    private static Connection connection;
    private static PreparedStatement preparedStatement;

    public static void main(String[] args) throws SQLException {
        // Initialize database connection
        connection = DriverManager.getConnection("jdbc:sqlite:account_manager.db");

        // Create users table if not exists
        String createTableSQL = "CREATE TABLE IF NOT EXISTS users ("
                + "id INTEGER PRIMARY KEY,"
                + "username TEXT UNIQUE NOT NULL,"
                + "password TEXT NOT NULL,"
                + "category TEXT NOT NULL,"
                + "website TEXT NOT NULL"
                + ")";
        preparedStatement = connection.prepareStatement(createTableSQL);
        preparedStatement.executeUpdate();

        // Try auto login
        if (!autoLogin()) {
            accountPassword();
        }
    }

    // Auto login functionality
    private static boolean autoLogin() throws SQLException {
        String selectSQL = "SELECT id, username, category, website FROM users";
        preparedStatement = connection.prepareStatement(selectSQL);
        ResultSet resultSet = preparedStatement.executeQuery();

        if (resultSet.next()) {
            System.out.println("Available accounts:");
            do {
                System.out.println(resultSet.getRow() + ". " + resultSet.getString("username") + " (" + resultSet.getString("category") + ") - " + resultSet.getString("website"));
            } while (resultSet.next());

            Scanner scanner = new Scanner(System.in);
            System.out.print("Enter the account number to auto login: ");
            String choice = scanner.nextLine().trim();

            if (choice.isEmpty()) {
                return false;
            }

            int accountId;
            try {
                accountId = Integer.parseInt(choice) - 1; // Get the selected account ID
            } catch (NumberFormatException e) {
                System.out.println("Invalid selection, please try again.");
                return false; // Skip auto login on invalid selection
            }

            String userSQL = "SELECT username, password, website FROM users WHERE id = ?";
            preparedStatement = connection.prepareStatement(userSQL);
            preparedStatement.setInt(1, accountId);
            ResultSet userResult = preparedStatement.executeQuery();

            if (userResult.next()) {
                String username = userResult.getString("username");
                String hashedPassword = userResult.getString("password");
                String website = userResult.getString("website");

                System.out.print("Enter password for " + website + " to auto login: ");
                String password = new Scanner(System.in).nextLine();

                // Verify password
                if (BCrypt.checkpw(password, hashedPassword)) {
                    System.out.println("Auto logging in " + username + " to " + website + "...");
                    
                    // Log the action (logging code would go here)
                    
                    // Simulating the auto login process
                    // Here you might want to use a library like Robot for GUI automation
                    simulateLogin(username, password);
                    
                    System.out.println("Login operation completed, the program will exit.");
                    System.exit(0); // Exit program after login
                } else {
                    System.out.println("Incorrect password, unable to auto login.");
                    return false; // Skip auto login on incorrect password
                }
            } else {
                System.out.println("User not found.");
                return false; // Skip auto login if user not found
            }
        } else {
            System.out.println("No accounts found, unable to auto login.");
            return false; // Skip auto login if no accounts
        }
        return false;
    }

    // Simulate login process (to be implemented)
    private static void simulateLogin(String username, String password) {
        // Implement GUI automation logic here
    }

    // Create new account
    private static void createAccount() throws SQLException {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter username: ");
        String username = scanner.nextLine();

        if (username.length() < 3) {
            System.out.println("Username length must be greater than 3 characters.");
            return;
        }

        System.out.print("Enter password: ");
        String password = scanner.nextLine();

        // Hashing the password
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

        System.out.print("Enter account category (e.g., social media, work, entertainment): ");
        String category = scanner.nextLine();
        System.out.print("Enter website address (e.g., https://example.com): ");
        String website = scanner.nextLine();

        String insertSQL = "INSERT INTO users (username, password, category, website) VALUES (?, ?, ?, ?)";
        try {
            preparedStatement = connection.prepareStatement(insertSQL);
            preparedStatement.setString(1, username);
            preparedStatement.setString(2, hashedPassword);
            preparedStatement.setString(3, category);
            preparedStatement.setString(4, website);
            preparedStatement.executeUpdate();
            System.out.println("Account created successfully!");
        } catch (SQLException e) {
            System.out.println("Username already exists, please choose another username.");
        }
    }

    // Change password
    private static void changePassword() throws SQLException {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter the username whose password you want to change: ");
        String username = scanner.nextLine();

        String userSQL = "SELECT id FROM users WHERE username = ?";
        preparedStatement = connection.prepareStatement(userSQL);
        preparedStatement.setString(1, username);
        ResultSet userResult = preparedStatement.executeQuery();

        if (userResult.next()) {
            System.out.print("Enter new password: ");
            String newPassword = scanner.nextLine();
            String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
            String updateSQL = "UPDATE users SET password = ? WHERE id = ?";
            preparedStatement = connection.prepareStatement(updateSQL);
            preparedStatement.setString(1, hashedPassword);
            preparedStatement.setInt(2, userResult.getInt("id"));
            preparedStatement.executeUpdate();
            System.out.println(username + "'s password changed successfully!");
        } else {
            System.out.println("User not found, please check your input.");
        }
    }

    private static void changeCategory(int userId) throws SQLException {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter new account category: ");
        String newCategory = scanner.nextLine();
        String updateSQL = "UPDATE users SET category = ? WHERE id = ?";
        preparedStatement = connection.prepareStatement(updateSQL);
        preparedStatement.setString(1, newCategory);
        preparedStatement.setInt(2, userId);
        preparedStatement.executeUpdate();
        System.out.println("Account category changed successfully!");
    }

    private static void changeWebsite(int userId) throws SQLException {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter new website address: ");
        String newWebsite = scanner.nextLine();
        String updateSQL = "UPDATE users SET website = ? WHERE id = ?";
        preparedStatement = connection.prepareStatement(updateSQL);
        preparedStatement.setString(1, newWebsite);
        preparedStatement.setInt(2, userId);
        preparedStatement.executeUpdate();
        System.out.println("Website address changed successfully!");
    }

    private static void deleteAccount() throws SQLException {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter the username to delete: ");
        String username = scanner.nextLine();
        System.out.print("Enter the password to delete the account: ");
        String password = scanner.nextLine();

        String userSQL = "SELECT id FROM users WHERE username = ? AND password = ?";
        preparedStatement = connection.prepareStatement(userSQL);
        preparedStatement.setString(1, username);
        preparedStatement.setString(2, password);
        ResultSet userResult = preparedStatement.executeQuery();

        if (userResult.next()) {
            String deleteSQL = "DELETE FROM users WHERE id = ?";
            preparedStatement = connection.prepareStatement(deleteSQL);
            preparedStatement.setInt(1, userResult.getInt("id"));
            preparedStatement.executeUpdate();
            System.out.println("Account " + username + " has been successfully deleted.");
        } else {
            System.out.println("Username or password is incorrect, unable to delete account.");
        }
    }

    private static void viewAccountsByCategory() throws SQLException {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter the category to view: ");
        String category = scanner.nextLine();

        String selectSQL = "SELECT username, website FROM users WHERE category = ?";
        preparedStatement = connection.prepareStatement(selectSQL);
        preparedStatement.setString(1, category);
        ResultSet resultSet = preparedStatement.executeQuery();

        if (resultSet.next()) {
            System.out.println("\nAccounts under category '" + category + "':");
            do {
                System.out.println("Username: " + resultSet.getString("username") + " - Website: " + resultSet.getString("website"));
            } while (resultSet.next());
        } else {
            System.out.println("No accounts found under category '" + category + "'.");
        }
    }

    private static void processChoice(String choice) throws SQLException {
        if ("login".equalsIgnoreCase(choice)) {
            autoLogin();
        } else if ("create".equalsIgnoreCase(choice)) {
            createAccount();
        } else if ("change password".equalsIgnoreCase(choice)) {
            changePassword();
        } else if ("change category".equalsIgnoreCase(choice)) {
            System.out.print("Enter the account ID to change category: ");
            int userId = new Scanner(System.in).nextInt();
            changeCategory(userId);
        } else if ("change website".equalsIgnoreCase(choice)) {
            System.out.print("Enter the account ID to change website: ");
            int userId = new Scanner(System.in).nextInt();
            changeWebsite(userId);
        } else if ("delete".equalsIgnoreCase(choice)) {
            deleteAccount();
        } else if ("view".equalsIgnoreCase(choice)) {
            viewAccountsByCategory();
        } else if ("exit".equalsIgnoreCase(choice)) {
            System.out.println("Exiting program.");
            System.exit(0);
        } else {
            System.out.println("Invalid choice, please try again.");
        }
    }

    // Main menu
    private static void accountPassword() {  
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("\n----- Account Password Management Program -----");
            System.out.print("Choose operation mode ('voice control' or 'text input'): ");
            String choice = scanner.nextLine().trim().toLowerCase();

            if ("voice control".equalsIgnoreCase(choice)) {
                // Implement voice control logic here
                // String command = takeCommand();
                // processChoice(command);
            } else if ("text input".equalsIgnoreCase(choice)) {
                System.out.print("Enter your command: ");
                String command = scanner.nextLine().trim().toLowerCase();
                processChoice(command);
            } else {
                System.out.println("Invalid choice, please try again.");
            }
        }
    }
}
