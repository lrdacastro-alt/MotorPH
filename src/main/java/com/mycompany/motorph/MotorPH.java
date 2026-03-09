package com.mycompany.motorph; // groups this class under your project package

import java.util.*; // lets us use Scanner, HashMap, etc.
import java.io.*; // lets us read files (CSV)
import java.time.*; // lets us compute time differences
import java.time.format.DateTimeFormatter; // lets us read time format like 8:00

public class MotorPH {

    static Scanner sc = new Scanner(System.in); // reads keyboard input

    // key = employee number, value = [last name, first name, birthday, gross semi-monthly]
    static HashMap<String, String[]> employees = new HashMap<>();

    // based on your CSV header, Gross Semi-monthly Rate is column 17
    static final int GROSS_SEMI_MONTHLY_INDEX = 17;

    public static void main(String[] args) {
        System.out.println("=== MotorPH Payroll System ==="); // title

        String user = login(); // ask for username/password
        loadEmployees(); // load employee data from csv into hashmap

        if (user.equals("employee")) employeeMenu(); // employee menu
        else payrollMenu(); // payroll staff menu
    }

    // asks user credentials and returns user role if valid
    static String login() {
        System.out.print("Username: ");
        String u = sc.nextLine().trim();

        System.out.print("Password: ");
        String p = sc.nextLine().trim();

        // allowed users in this project
        if ((u.equals("employee") || u.equals("payroll_staff")) && p.equals("12345")) return u;

        System.out.println("Invalid login.");
        System.exit(0); // stop program if invalid
        return "";
    }

    // menu for employee account
    static void employeeMenu() {
        while (true) {
            System.out.println("\n1. View Employee");
            System.out.println("2. Exit");
            System.out.print("Choose: ");

            switch (sc.nextLine()) {
                case "1":
                    System.out.print("Employee Number: ");
                    String id = sc.nextLine().trim();

                    if (employees.containsKey(id)) {
                        String[] e = employees.get(id); // get employee info
                        System.out.println("Name: " + e[1] + " " + e[0]);
                        System.out.println("Birthday: " + e[2]);
                        System.out.println("Gross Semi-monthly: " + e[3]);
                    } else {
                        System.out.println("Employee not found.");
                    }
                    break;

                case "2":
                    System.exit(0); // end program
                    break;

                default:
                    System.out.println("Invalid choice.");
            }
        }
    }

    // menu for payroll staff
    static void payrollMenu() {
        while (true) {
            System.out.println("\n1. One Employee Payroll");
            System.out.println("2. All Employees Payroll");
            System.out.println("3. Exit");
            System.out.print("Choose: ");

            switch (sc.nextLine()) {
                case "1":
                    System.out.print("Employee Number: ");
                    String id = sc.nextLine().trim();

                    if (employees.containsKey(id)) displayPayroll(id);
                    else System.out.println("Employee not found.");
                    break;

                case "2":
                    for (String idNum : employees.keySet()) displayPayroll(idNum); // show all
                    break;

                case "3":
                    return; // exit payroll menu

                default:
                    System.out.println("Invalid choice.");
            }
        }
    }

    // reads employee csv and stores needed columns in hashmap
    static void loadEmployees() {
        try {
            BufferedReader br = new BufferedReader(new FileReader("MotorPH_Employee Data.csv"));
            br.readLine(); // skip header row

            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split(",", -1); // split row into columns
                if (p.length <= GROSS_SEMI_MONTHLY_INDEX) continue; // skip bad row

                String id = p[0].trim();
                String last = p[1].trim();
                String first = p[2].trim();
                String bday = p[3].trim();

                // remove quotes/commas from number text
                String grossSemi = p[GROSS_SEMI_MONTHLY_INDEX].replace("\"", "").replace(",", "").trim();

                employees.put(id, new String[]{last, first, bday, grossSemi});
            }

            br.close();
            System.out.println("Employees loaded: " + employees.size());

        } catch (Exception e) {
            System.out.println("Error loading employees.");
            System.out.println("Details: " + e.getMessage());
        }
    }

    // computes total worked hours for a given employee and cutoff
    // cutoff 1 = June 1-15, cutoff 2 = June 16-30
    static double calcHours(String id, int cutoff) {
        double total = 0;
        DateTimeFormatter f = DateTimeFormatter.ofPattern("H:mm");

        try {
            BufferedReader br = new BufferedReader(new FileReader("employee_attendance.csv"));
            br.readLine(); // skip header
            String line;

            while ((line = br.readLine()) != null) {
                String[] p = line.split(",", -1);
                if (p.length < 6) continue;
                if (!p[0].trim().equals(id)) continue; // only target employee

                String[] d = p[3].trim().split("/"); // date in M/D/YYYY
                int month = Integer.parseInt(d[0]);
                int day = Integer.parseInt(d[1]);

                if (month != 6) continue; // only June for this project

                boolean inCutoff1 = (cutoff == 1 && day <= 15);
                boolean inCutoff2 = (cutoff == 2 && day >= 16);

                if (inCutoff1 || inCutoff2) {
                    LocalTime in = LocalTime.parse(p[4].trim(), f);
                    LocalTime out = LocalTime.parse(p[5].trim(), f);
                    total += Duration.between(in, out).toMinutes() / 60.0; // minutes to hours
                }
            }

            br.close();

        } catch (Exception e) {
            System.out.println("Attendance read error.");
            System.out.println("Details: " + e.getMessage());
        }

        return total;
    }

    // prints payroll result for one employee
    static void displayPayroll(String id) {
        String[] e = employees.get(id);

        double grossSemi = 0;
        try {
            grossSemi = Double.parseDouble(e[3]); // convert text number to double
        } catch (Exception ex) {
            System.out.println("Invalid gross semi-monthly for employee " + id + ". Using 0.");
        }

        // assumption: 80 work hours per cutoff
        double effectiveHourly = grossSemi / 80.0;

        System.out.println("\n=======================");
        System.out.println("Employee: " + e[1] + " " + e[0]);

        // cutoff 1
        double h1 = calcHours(id, 1);
        double g1 = h1 * effectiveHourly;

        System.out.println("\nJune 1-15");
        System.out.printf("Hours: %.2f\n", h1);
        System.out.printf("Gross: %.2f\n", g1);
        System.out.printf("Net: %.2f\n", g1); // no deductions here

        // cutoff 2
        double h2 = calcHours(id, 2);
        double g2 = h2 * effectiveHourly;

        double deductions = 765.00 + 257.25 + 100.00;
        double net2 = g2 - deductions;
        if (net2 < 0) net2 = 0; // prevent negative net

        System.out.println("\nJune 16-30");
        System.out.printf("Hours: %.2f\n", h2);
        System.out.printf("Gross: %.2f\n", g2);
        System.out.printf("Net: %.2f\n", net2);

        System.out.println("=======================");
    }
}