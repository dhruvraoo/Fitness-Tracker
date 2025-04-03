import java.io.File;
import java.io.FileWriter;
import java.sql.*;
import java.util.Scanner;
import java.time.LocalDate;

interface Track_health
{
    void track_progress(Connection con,int userId,Scanner sc);
}

class User {
    Time time_rec;
    int steps;
    int heartPoints;
    double waterIntake;
    String activity;

    User(Time time_rec, int steps, int heartPoints, double waterIntake,String activity) {
        this.time_rec=time_rec;
        this.steps = steps;
        this.heartPoints = heartPoints;
        this.waterIntake = waterIntake;
        this.activity=activity;
    }

    @Override
    public String toString() {
        return "Time: " + time_rec + ", Steps: " + steps + ", Heart Points: " + heartPoints + ", Water: " + waterIntake + "L"+" Acitivity : "+activity;
    }
}

class DailyReporting implements Track_health {
    public void track_progress(Connection con, int R_no, Scanner sc) {
        try {
            LocalDate currentDate = LocalDate.now(); // Get today's date
            // Step 1: Check if an entry already exists for today
            String checkQuery = "SELECT COUNT(*) FROM dailytrack WHERE R_no = ? AND DATE(time) = ?";
            PreparedStatement checkPst = con.prepareStatement(checkQuery);
            checkPst.setInt(1, R_no);
            checkPst.setDate(2, java.sql.Date.valueOf(currentDate));

            ResultSet rs = checkPst.executeQuery();
            rs.next();
            int count = rs.getInt(1);

            // Step 2: If an entry exists, update it. Otherwise, insert a new record.
            if (count > 0) {
                // Update existing record
                String updateQuery = "UPDATE dailytrack SET steps_count = steps_count + ?, heart_count = heart_count + ?, water_intake = water_intake + ?, EditTime = NOW() WHERE R_no = ? AND DATE(time) = ?";
                PreparedStatement updatePst = con.prepareStatement(updateQuery);

                System.out.println("Enter your step count:");
                int steps_cnt = sc.nextInt();

                System.out.println("Enter your heart points:");
                int heart_points = sc.nextInt();

                System.out.println("Enter water consumed (in liters):");
                double water = sc.nextDouble();

                updatePst.setInt(1, steps_cnt);
                updatePst.setInt(2, heart_points);
                updatePst.setDouble(3, water);
                updatePst.setInt(4, R_no);
                updatePst.setDate(5, java.sql.Date.valueOf(currentDate));

                updatePst.executeUpdate();
                System.out.println("Health data has been updated successfully for " + currentDate);
            } else {
                // Insert new record
                String insertQuery = "INSERT INTO dailytrack (R_no, activity, steps_count, heart_count, water_intake, time, EditTime) values (?, ?, ?, ?, ?, NOW(), NULL)";
                PreparedStatement insertPst = con.prepareStatement(insertQuery);

                System.out.println("Enter your step count:");
                int steps_cnt = sc.nextInt();

                System.out.println("Enter your heart points:");
                int heart_points = sc.nextInt();

                System.out.println("Enter water consumed (in liters):");
                double water = sc.nextDouble();

                insertPst.setInt(1, R_no); // Set user_id
                insertPst.setString(2, "Walking");
                insertPst.setInt(3, steps_cnt);
                insertPst.setInt(4, heart_points);
                insertPst.setDouble(5, water);

                insertPst.executeUpdate();
                System.out.println("Health data has been recorded successfully for " + currentDate);
            }

        } catch (Exception e) {
            System.out.println("An error occurred while recording health data: " + e.getMessage());
        }
    }

    public void add_activity(Connection con, Scanner sc, int R_no) {
        try {
            LocalDate currentDate = LocalDate.now(); // Get today's date

            // Step 1: Check if an entry already exists for today
            String checkQuery = "SELECT activity FROM dailytrack WHERE R_no = ? AND DATE(time) = ?";
            PreparedStatement checkPst = con.prepareStatement(checkQuery);
            checkPst.setInt(1, R_no);
            checkPst.setDate(2, java.sql.Date.valueOf(currentDate));

            ResultSet rs = checkPst.executeQuery();

            if (rs.next()) {
                String currentActivity = rs.getString("activity");

                System.out.println("Enter the new activity (other than walking):");
                String newActivity = sc.nextLine();

                System.out.println("Enter heart points for this activity:");
                int heartPoints = sc.nextInt();

                // Consume the leftover newline
                sc.nextLine();

                // Combine the current activity with the new activity
                String updatedActivity = currentActivity == null || currentActivity.isEmpty()
                    ? newActivity
                    : currentActivity + ", " + newActivity;

                // Update the record
                String updateQuery = "UPDATE dailytrack SET activity = ?, heart_count = heart_count + ?, EditTime = NOW() WHERE R_no = ? AND DATE(time) = ?";
                PreparedStatement updatePst = con.prepareStatement(updateQuery);
                updatePst.setString(1, updatedActivity);
                updatePst.setInt(2, heartPoints);
                updatePst.setInt(3, R_no);
                updatePst.setDate(4, java.sql.Date.valueOf(currentDate));

                updatePst.executeUpdate();
                System.out.println("Activity has been updated successfully for " + currentDate);
            } else {
                System.out.println("No record found for today. Please record daily data first.");
            }

        } catch (Exception e) {
            System.out.println("An error occurred while updating activity data: " + e.getMessage());
        }
    }

    public void generateReport(Connection con, int R_no) {
        try {
            LocalDate currentDate = LocalDate.now(); // Get today's date

            // Fetch weight goal data
            String weightGoalQuery = "SELECT weight_goal, Steps, heart_points, water_intake FROM weightgoal WHERE R_no = ?";
            PreparedStatement weightGoalPst = con.prepareStatement(weightGoalQuery);
            weightGoalPst.setInt(1, R_no);
            ResultSet weightGoalRs = weightGoalPst.executeQuery();

            // Fetch daily track data
            String dailyTrackQuery = "SELECT Activity, steps_count, heart_count, water_intake FROM dailytrack WHERE R_no = ? AND DATE(time) = ?";
            PreparedStatement dailyTrackPst = con.prepareStatement(dailyTrackQuery);
            dailyTrackPst.setInt(1, R_no);
            dailyTrackPst.setDate(2, java.sql.Date.valueOf(currentDate));
            ResultSet dailyTrackRs = dailyTrackPst.executeQuery();

            // Create a file to store the report
            File reportFile = new File("HealthReport_" + R_no + "_" + currentDate + ".txt");
            FileWriter writer = new FileWriter(reportFile);

            if (weightGoalRs.next() && dailyTrackRs.next()) {
                // Compare data
                int goalSteps = weightGoalRs.getInt("Steps");
                int goalHeartPoints = weightGoalRs.getInt("heart_points");
                double goalWaterIntake = weightGoalRs.getDouble("water_intake");

                int actualSteps = dailyTrackRs.getInt("steps_count");
                int actualHeartPoints = dailyTrackRs.getInt("heart_count");
                double actualWaterIntake = dailyTrackRs.getDouble("water_intake");

                String report = "Health Report for User: " + R_no + " on " + currentDate + "\n\n";
                report += "Activity: " + dailyTrackRs.getString("Activity") + "\n";
                report += "Steps Goal: " + goalSteps + ", Actual Steps: " + actualSteps + "\n";
                report += "Heart Points Goal: " + goalHeartPoints + ", Actual Heart Points: " + actualHeartPoints + "\n";
                report += "Water Intake Goal: " + goalWaterIntake + " liters, Actual Water Intake: " + actualWaterIntake + " liters\n\n";

                // Check if goals are achieved and provide feedback
                if (actualSteps >= goalSteps && actualHeartPoints >= goalHeartPoints && actualWaterIntake >= goalWaterIntake) {
                    report += "Congratulations! You have achieved your daily health goals!\n";
                } else {
                    report += "Keep going! You're doing great, but you need to improve to meet your goals:\n";
                    if (actualSteps < goalSteps) {
                        report += "- Increase your steps by " + (goalSteps - actualSteps) + " steps.\n";
                    }
                    if (actualHeartPoints < goalHeartPoints) {
                        report += "- Earn " + (goalHeartPoints - actualHeartPoints) + " more heart points.\n";
                    }
                    if (actualWaterIntake < goalWaterIntake) {
                        report += "- Drink " + (goalWaterIntake - actualWaterIntake) + " more liters of water.\n";
                    }
                }

                writer.write(report); // Write the report to the file
                System.out.println("Health report generated successfully!");

            } else {
                System.out.println("No data found for today or user has not set a weight goal.");
            }

            // Close the writer
            writer.close();

        } catch (Exception e) {
            System.out.println("An error occurred while generating the health report: " + e.getMessage());
        }
    }
}


class DailyReportingDisplay implements Track_health {
    @Override
    public void track_progress(Connection con, int R_no, Scanner sc) {
        try {
            String sql = "SELECT time, steps_count, heart_count, water_intake,activity FROM dailytrack WHERE R_no = ?";
            PreparedStatement pst = con.prepareStatement(sql);
            pst.setInt(1, R_no);
            ResultSet rs = pst.executeQuery();

            MyLinkedList_health reportList = new MyLinkedList_health();
            while (rs.next()) {
                Time date = rs.getTime("time");
                int steps = rs.getInt("steps_count");
                int heartPoints = rs.getInt("heart_count");
                double water = rs.getDouble("water_intake");
                String activity = rs.getString("activity");

                User dailyReport = new User(date, steps, heartPoints, water,activity);
                reportList.insert(dailyReport);
            }

            if (reportList.head == null) {
                System.out.println("No daily reporting data found.");
            } else {
                System.out.println("Daily Reporting Data:");
                reportList.display();
            }

        } catch (Exception e) {
            System.out.println("An error occurred while fetching daily reporting data: " + e.getMessage());
        }
    }
}


class EditDailyReport implements Track_health
{
    public void track_progress(Connection con,int R_no,Scanner sc)
    {
        int steps=0;
        int heartPoints=0;
        double water=0.0;
        try {
            System.out.println("What would you like to edit : ");
            System.out.println("1. Steps");
            System.out.println("2. Heart points");
            System.out.println("3. Water consumed");
            int ch = sc.nextInt();

            String sql = "UPDATE dailytrack SET ";

            if (ch == 1) {
                System.out.println("Enter Steps:");
                steps = sc.nextInt();
                sql += "steps_count = ?, EditTime = CURRENT_TIMESTAMP WHERE R_no = ?";
            } else if (ch == 2) {
                System.out.println("Enter Heart Points:");
                heartPoints = sc.nextInt();
                sql += "heart_count = ?, EditTime = CURRENT_TIMESTAMP WHERE R_no = ?";
            } else if (ch == 3) {
                System.out.println("Enter Water Consumed (in liters):");
                water = sc.nextDouble();
                sql += "water_intake = ?, EditTime = CURRENT_TIMESTAMP WHERE R_no = ?";
            } else {
                System.out.println("Invalid choice!");
                return;
            }

            PreparedStatement pst = con.prepareStatement(sql);

            // Set the first parameter based on the user's choice
            if (ch == 1) {
                pst.setInt(1, steps);
            } else if (ch == 2) {
                pst.setInt(1, heartPoints);
            } else if (ch == 3) {
                pst.setDouble(1, water);
            }

            pst.setInt(2, R_no); // Set the R_no

            int r = pst.executeUpdate();

            if (r > 0) {
                System.out.println("Edited successfully !!");
            } else {
                System.out.println("Oops something went wrong, could not edit !!");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

class MyLinkedList_health {
    Node head;

    class Node {
        User data;
        Node next;

        Node(User data) {
            this.data = data;
            this.next = null;
        }
    }

    public void insert(User data) {
        Node newNode = new Node(data);
        if (head == null) {
            head = newNode;
        } else {
            Node temp = head;
            while (temp.next != null) {
                temp = temp.next;
            }
            temp.next = newNode;
        }
    }

    public void display() {
        if (head == null) {
            System.out.println("No data to display.");
            return;
        }
        Node temp = head;
        while (temp != null) {
            System.out.println(temp.data.toString());
            temp = temp.next;
        }
    }
}


public class HealthCare {
    int R_no; 
    public static void main(String[] args) {
        HealthCare hc = new HealthCare();
        Scanner sc = new Scanner(System.in);
        String DriverName = "com.mysql.cj.jdbc.Driver";
        String dbUrl = "jdbc:mysql://localhost:3306/health_care";
        String dbUser = "root";
        String dbPass = "";
        
        try {
            Class.forName(DriverName);
            Connection con = DriverManager.getConnection(dbUrl, dbUser, dbPass);
            if (con != null) {
                System.out.println("Successfully connected to database !!");
            } else {
                System.out.println("Failed to connect to database !!");
            }
    
            int ch;
            boolean exit = false;
            boolean loggedIn = false;
    
            while (!exit) {
                    System.out.println("Press 1: Register ");
                    System.out.println("Press 2: Log in to use app");
                    System.out.println("Press 3: Exit the system");
                    ch = sc.nextInt();
                    sc.nextLine(); 
    
                    switch (ch) {
                        case 1:
                            System.out.println("~~~~~~~~~~~~~~~Registration Page~~~~~~~~~~~~~~~~~~");
                            hc.Registration(con, sc);
                            break;
    
                        case 2:
                            System.out.println("~~~~~~~~~~~~~~~Welcome to log in page~~~~~~~~~~~~~~~~~~");
                            loggedIn = hc.Login(con, sc); 
                            if (loggedIn) {
                                if (!hc.isGoalSet(con)) {
                                    hc.displayBMISuggestion(con);
                                    hc.setGoal(con, sc); 
                                }
                                hc.postLoginActions(con,sc); 
                            }
                            break;
    
                        case 3:
                            System.out.println("Are you sure you want to exit the system? (y/n)");
                            String ans = sc.nextLine();
                            if (ans.equalsIgnoreCase("y")) {
                                System.out.println("Exiting from the system ....");
                                exit = true;
                            }
                            break;
    
                        default:
                            System.out.println("Invalid choice. Please try again.");
                            break;
                    }
            }
    
            sc.close();
            con.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void Registration(Connection con, Scanner sc) throws Exception {
        String sql1 = "INSERT INTO registration(Username,password, age, gender,weight, height,BMI,Medical_Condition) VALUES (?,?,?,?,?,?,?,?)";
        PreparedStatement pst = con.prepareStatement(sql1);
        
        System.out.println("Enter Username:");
        String username = sc.nextLine();
        System.out.println("Enter Password:");
        String password = sc.next();
        System.out.println("Enter your Age: ");
        int age = sc.nextInt();
        if(age<=10)
        {
            System.out.println("You are too young for this , Enjoy your life Kido !!!");
            return;
        }
        sc.nextLine();  // Consume the newline character
        System.out.println("Enter your Gender: ");
        String gender = sc.nextLine();
        System.out.println("Enter your current weight(in kg) : ");
        double weight = sc.nextDouble();
        if(weight<=35)
        {
            System.out.println("Invalid weight buddy !!");
            System.out.println("Pls enter your correct weight");
            return;
        }
        System.out.println("Enter your current height(in cm) : ");
        double height = sc.nextDouble();
        System.out.println("Any Medical Condition :");
        sc.nextLine();
        String medicalCond = sc.nextLine();
        double new_height = height/100;
        Double BMI = weight/(new_height*new_height);

        pst.setString(1, username);
        pst.setString(2, password);
        pst.setInt(3, age);
        pst.setString(4, gender);
        pst.setDouble(5, weight);
        pst.setDouble(6, height);
        pst.setDouble(7, BMI);
        pst.setString(8,medicalCond);
        
        int r1 = pst.executeUpdate();
        if (r1 > 0) {
            System.out.println("Sign in successful !!");
        } else {
            System.out.println("Sign in failed !!");
        }
    }
    
    public boolean Login(Connection con, Scanner sc) throws Exception {
        int attempts = 3;
        boolean authenticated = false;

        while (attempts > 0 && !authenticated) {
            System.out.println("Enter your username: ");
            String username = sc.nextLine();
            System.out.println("Enter your password: ");
            String password = sc.next();
            sc.nextLine();

            String sql = "SELECT R_no, Username, password FROM registration WHERE Username = ? AND password = ?";
            PreparedStatement pst = con.prepareStatement(sql);
            pst.setString(1, username);
            pst.setString(2, password);

            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                R_no = rs.getInt("R_no"); 
                System.out.println("Login successful!");
                authenticated = true;
            } else {
                attempts--;
                if (attempts > 0) {
                    System.out.println("Oops Invalid Username or password...Try Again !!!");
                    System.out.println("You have " + attempts + " attempts left.");
                }
            }
        }

        if (!authenticated) {
            System.out.println("Too many failed attempts. Exiting the system.");
            System.exit(0);
        }

        return authenticated;
    }

    public boolean isGoalSet(Connection con) throws Exception {
        String sql = "SELECT COUNT(*) FROM weightgoal WHERE R_no = ?";
        PreparedStatement pst = con.prepareStatement(sql);
        pst.setInt(1, R_no);
        ResultSet rs = pst.executeQuery();
        if (rs.next()) {
            int count = rs.getInt(1);
            return count > 0;
        }
        return false;
    }
    public void displayBMISuggestion(Connection con) throws Exception {
        String sql = "SELECT weight, height, bmi FROM registration WHERE R_no=?";
        PreparedStatement pst = con.prepareStatement(sql);
        pst.setInt(1, R_no);
        ResultSet r = pst.executeQuery();
        if (r.next()) {
            double weight = r.getDouble("weight");
            double height = r.getDouble("height");
            double bmi = r.getDouble("bmi");
            System.out.println("Your weight is " + weight + " and height is " + height);
            if (bmi < 18.5) {
                double minNormalWeight = 18.5 * (height * height);
                System.out.print("Your BMI indicates that you are underweight. ");
                double roundedWeight = Math.round(minNormalWeight * 100.0) / 100.0;
                String weightString = roundedWeight + " kg.";
                System.out.println();
                System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
                System.out.print("It is suggested to gain weight to at least "+ weightString);
                System.out.println("You can set your step goal to 6000-8000 steps per day");
                System.out.println("You can add your heart point to 10-15 heart points per day");
                System.out.println("You should have 8-10 cups of water per day (2-2.5L)");
                System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
            } else if (bmi >= 18.5 && bmi <= 24.9) {
                System.out.println("Your BMI is within the normal range. ");
                System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
                System.out.print("It is suggested to maintain your current weight.\n");
                System.out.println("You can set your step goal to 8000-10000 steps per day");
                System.out.println("You can add your heart point to 15-20 heart points per day");
                System.out.println("You should have 8-10 cups of water per day (2-2.5L)");
                System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
            } else if (bmi >= 25.0 && bmi <= 29.9) {
                double maxNormalWeight = 24.9 * (height * height);
                System.out.print("Your BMI indicates that you are overweight. ");
                double roundedWeight = Math.round(maxNormalWeight * 100.0) / 100.0;
                String weightString = roundedWeight + " kg.";
                System.out.println();
                System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
                System.out.print("It is suggested to lose weight to at least "+ weightString);
                System.out.println("You can set your step goal to 10000-12000 steps per day");
                System.out.println("You can add your heart point to 20-25 heart points per day");
                System.out.println("You should have 8-12 cups of water per day (2-3L)");
                System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
            } else {
                double maxNormalWeight = 24.9 * (height * height);
                System.out.print("Your BMI indicates that you are obese. ");
                double roundedWeight = Math.round(maxNormalWeight * 100.0) / 100.0;
                String weightString = roundedWeight + " kg.";
                System.out.println();
                System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
                System.out.print("It is suggested to lose weight to at least "+ weightString);
                System.out.println("You can set your step goal to 10000-12000 steps per day");
                System.out.println("You can add your heart point to 20-25 heart points per day");
                System.out.println("You should have 8-14 cups of water per day (2-3L)");
                System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
            }
        }
    }
    
    
    public void setGoal(Connection con, Scanner sc) throws Exception {
        System.out.println("Time to set your Goals !!!!!");
        System.out.println("Please enter your weight goal(Lose/Gain/Maintain) : ");
        String weightgoal = sc.nextLine();
        System.out.println("Please enter your step goal per day : ");
        int stepGoal = sc.nextInt();
        System.out.println("Please enter your heart point goal per day : ");
        int heartGoal = sc.nextInt();
        System.out.println("Please enter your water intake goal per day(in litre) : ");
        double waterintake =sc.nextDouble();
        String sql4 = "INSERT INTO weightgoal(R_no, weight_goal,steps,heart_points,water_intake) VALUES (?, ?, ?, ?, ?)";
        PreparedStatement pst4 = con.prepareStatement(sql4);
        pst4.setInt(1, R_no);
        pst4.setString(2, weightgoal);
        pst4.setInt(3, stepGoal);
        pst4.setInt(4, heartGoal);
        pst4.setDouble(5, waterintake);
        pst4.executeUpdate();
    }

    public void postLoginActions(Connection con, Scanner sc) throws Exception {
        int choice = 0;  // Initialize choice to avoid compilation issues
        boolean running = true;

        do {
            System.out.println("=================================Main Menu======================================");
            System.out.println("1. Daily Entry");
            System.out.println("2. Edit Entry");
            System.out.println("3. Add Additional activity");
            System.out.println("4. Health Report");
            System.out.println("5. Display Daily Entry Record");
            System.out.println("6. Exit Main Menu ?");
            System.out.println("===============================================================================");
            
            System.out.print("Please enter your choice: ");
            
            try {
                choice = sc.nextInt(); 
                sc.nextLine();  
            } catch (Exception e) {
                System.out.println("Invalid input. Please enter a number.");
                sc.next();  
                continue;  
            }

            switch (choice) {
                case 1:
                    System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~Daily Reporting~~~~~~~~~~~~~~~~~~~~~~~~~~~");
                    DailyReporting dr = new DailyReporting();
                    dr.track_progress(con, R_no,sc);
                    System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
                    break;
                case 2:
                    System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~Edit Reporting~~~~~~~~~~~~~~~~~~~~~~~~~~~");
                    EditDailyReport edr = new EditDailyReport();
                    edr.track_progress(con, R_no, sc);
                    System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
                    break;
                case 3:
                    System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~Additional Activity~~~~~~~~~~~~~~~~~~~~~~~~~~~");
                    DailyReporting drp = new DailyReporting();
                    drp.add_activity(con, sc, R_no);
                    System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
                    break;
                case 4:
                    System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~Health Report~~~~~~~~~~~~~~~~~~~~~~~~~~~");
                    DailyReporting dd = new DailyReporting();
                    dd.generateReport(con, R_no);
                    System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
                    break;
                case 5:
                    System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~Daily Reporting Record~~~~~~~~~~~~~~~~~~~~~~~~~~~");
                    DailyReportingDisplay drd = new DailyReportingDisplay();
                    drd.track_progress(con, R_no,sc);
                    System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
                    break;
                case 6:
                    System.out.println("Exiting...");
                    running = false;  
                    break;
                default:
                    System.out.println("Invalid option. Please try again.");
                    break;
            }
        } while (running);
    }
}
