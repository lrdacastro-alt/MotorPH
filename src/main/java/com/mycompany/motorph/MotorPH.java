package com.mycompany.motorph;

/*
MotorPH Payroll System

Explanation included for each code and what they do.

There are two types of users:
1. employee
2. payroll_staff

Employees can:
- Enter their employee number
- View their basic employee details

Payroll staff can:
- Process payroll
- Compute salaries based on attendance records

The system reads information from two CSV files:
1. MotorPH_Employee Data.csv
   → Contains employee personal information

2. employee_attendance.csv
   → Contains employee time-in and time-out records
*/

import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import java.time.LocalTime;
import java.time.Duration;
import java.time.format.DateTimeFormatter;

public class MotorPH {

    // =====================================================
    // EMPLOYEE CLASS
    // This is a simple structure used to store employee data
    // Each employee object will hold these 4 pieces of data
    // =====================================================
    static class Employee {

        String number;
        String firstName;
        String lastName;
        String birthday;
        double hourlyRate;
    }

    // Scanner is used to read user input from the keyboard
    static Scanner scanner = new Scanner(System.in);


    // =====================================================
    // MAIN METHOD
    // This is the starting point of the program
    // =====================================================
    public static void main(String[] args) {

        System.out.println("================================");
        System.out.println("       MotorPH Payroll System");
        System.out.println("================================");

        String username = login();

        if (username.equals("employee")) {
            employeeMenu();
        }

        if (username.equals("payroll_staff")) {
            payrollMenu();
        }
    }

    // =====================================================
    // LOGIN METHOD
    // =====================================================
    static String login() {

        System.out.print("Username: ");
        String username = scanner.nextLine();

        System.out.print("Password: ");
        String password = scanner.nextLine();

        boolean validUsername =
                username.equals("employee") ||
                username.equals("payroll_staff");

        boolean validPassword = password.equals("12345");

        if (!validUsername || !validPassword) {

            System.out.println("Incorrect username and/or password.");
            System.exit(0);
        }

        return username;
    }

    // =====================================================
    // EMPLOYEE MENU
    // =====================================================
    static void employeeMenu() {

        List<Employee> employees = loadEmployees();

        while (true) {

            System.out.println("\nEmployee Menu");
            System.out.println("1. Enter your employee number");
            System.out.println("2. Exit the program");

            String choice = scanner.nextLine();

            if (choice.equals("1")) {

                System.out.print("Enter employee number: ");
                String empNum = scanner.nextLine();

                boolean found = false;

                for (Employee emp : employees) {

                    if (emp.number.equals(empNum)) {

                        System.out.println("\nEmployee Number: " + emp.number);
                        System.out.println("Employee Name: " + emp.firstName + " " + emp.lastName);
                        System.out.println("Birthday: " + emp.birthday);

                        found = true;
                        break;
                    }
                }

                if (!found) {
                    System.out.println("Employee number does not exist.");
                }
            }

            if (choice.equals("2")) {

                System.out.println("Program terminated.");
                System.exit(0);
            }
        }
    }

    // =====================================================
    // PAYROLL STAFF MENU
    // =====================================================
    static void payrollMenu() {

        while (true) {

            System.out.println("\nPayroll Staff Menu");
            System.out.println("1. Process Payroll");
            System.out.println("2. Exit the program");

            String choice = scanner.nextLine();

            if (choice.equals("1")) {
                processPayrollMenu();
            }

            if (choice.equals("2")) {

                System.out.println("Program terminated.");
                System.exit(0);
            }
        }
    }

    // =====================================================
    // PROCESS PAYROLL MENU
    // =====================================================
    static void processPayrollMenu() {

        List<Employee> employees = loadEmployees();

        while (true) {

            System.out.println("\nProcess Payroll");
            System.out.println("1. One employee");
            System.out.println("2. All employees");
            System.out.println("3. Exit");

            String choice = scanner.nextLine();

            if (choice.equals("1")) {

                System.out.print("Enter employee number: ");
                String empNum = scanner.nextLine();

                boolean found = false;

                for (Employee emp : employees) {

                    if (emp.number.equals(empNum)) {

                        displayPayroll(emp);
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    System.out.println("Employee number does not exist.");
                }
            }

            if (choice.equals("2")) {

                for (Employee emp : employees) {
                    displayPayroll(emp);
                }
            }

            if (choice.equals("3")) {
                return;
            }
        }
    }

    // =====================================================
    // LOAD EMPLOYEES FROM CSV FILE
    // =====================================================
    static List<Employee> loadEmployees() {

        List<Employee> employees = new ArrayList<>();

        try {

            BufferedReader br = new BufferedReader(
                    new FileReader("MotorPH_Employee Data.csv"));

            br.readLine();

            String line;

            while ((line = br.readLine()) != null) {

                String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);

                Employee emp = new Employee();

                emp.number = parts[0];
                emp.lastName = parts[1];
                emp.firstName = parts[2];
                emp.birthday = parts[3];
                emp.hourlyRate = Double.parseDouble(parts[18].trim());

                employees.add(emp);
            }

            br.close();

        } catch (IOException e) {

            System.out.println("Error loading employee data.");
        }

        return employees;
    }

    // =====================================================
    // CALCULATE HOURS WORKED
    // =====================================================
    static double calculateHours(String employeeNumber, int cutoff) {

        double totalHours = 0;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("H:mm");

        try {

            BufferedReader br =
                    new BufferedReader(new FileReader("employee_attendance.csv"));

            br.readLine();

            String line;

            while ((line = br.readLine()) != null) {

                String[] parts = line.split(",");

                String empNum = parts[0];
                String date = parts[3];

                if (empNum.equals(employeeNumber)) {

                    String[] dateParts = date.split("/");

                    int month = Integer.parseInt(dateParts[0]);
                    int day = Integer.parseInt(dateParts[1]);

                    boolean correctCutoff = false;

                    if (month == 6) {

                        if (cutoff == 1 && day <= 15)
                            correctCutoff = true;

                        if (cutoff == 2 && day >= 16)
                            correctCutoff = true;
                    }

                    if (correctCutoff) {
                        LocalTime workStart    = LocalTime.of(8, 0);   // 8:00 AM
                        LocalTime graceEnd     = LocalTime.of(8, 10);  // grace period ends 8:10
                        LocalTime workEnd      = LocalTime.of(17, 0);  // 5:00 PM
                        int lunchMinutes       = 60;                   // 1 hour lunch

                        LocalTime timeIn  = LocalTime.parse(parts[4].trim(), formatter);
                        LocalTime timeOut = LocalTime.parse(parts[5].trim(), formatter);

                        // Rule 1: Grace period
                        LocalTime effectiveLogin;
                        if (!timeIn.isAfter(graceEnd)) {
                            effectiveLogin = workStart; // 8:00 - 8:10 treated as 8:00
                        } else {
                            effectiveLogin = timeIn;    // late, use actual login
                        }

                        // Rule 2: No overtime, cap at 5:00 PM
                        LocalTime effectiveLogout;

                        if (timeOut.isAfter(workEnd)) {
                            effectiveLogout = workEnd;
                        } else {
                            effectiveLogout = timeOut;
                        }

                        // Rule 3: Calculate minutes minus lunch
                        int minutesWorked = (effectiveLogout.getHour() * 60 + effectiveLogout.getMinute())
                                          - (effectiveLogin.getHour()  * 60 + effectiveLogin.getMinute())
                                          - lunchMinutes;

                        // No negative hours
                        if (minutesWorked < 0) minutesWorked = 0;

                        totalHours += minutesWorked / 60.0;
                    }
                }
            }

            br.close();

        } catch (IOException e) {

            System.out.println("Error reading attendance.");
        }

        return totalHours;
    }

    // =====================================================
    // DISPLAY PAYROLL INFORMATION
    // =====================================================
    static void displayPayroll(Employee emp) {

        System.out.println("\n=================================");
        System.out.println("Employee Number: " + emp.number);
        System.out.println("Employee Name: " + emp.firstName + " " + emp.lastName);
        System.out.println("Birthday: " + emp.birthday);

        // ── Cutoff 1: June 1 - 15
        double hours1 = calculateHours(emp.number, 1);
        double gross1 = hours1 * emp.hourlyRate; 
    
        System.out.println("\nCutoff Date: June 1 - June 15");
        System.out.println("Total Hours Worked: " + hours1);
        System.out.println("Gross Salary: " + gross1);
        System.out.println("Net Salary: " + gross1);

        // ── Cutoff 2: June 16 - 30
        double hours2 = calculateHours(emp.number, 2);
        double gross2 = hours2 * emp.hourlyRate; 

        double sss = 765;
        double philhealth = 257.25;
        double pagibig = 100;
        double tax = 0;

        double deductions = sss + philhealth + pagibig + tax;
        double net2 = gross2 - deductions;

        System.out.println("\nCutoff Date: June 16 - June 30");
        System.out.println("Total Hours Worked: " + hours2);
        System.out.println("Gross Salary: " + gross2);

        System.out.println("\nDeductions:");
        System.out.println("SSS: " + sss);
        System.out.println("PhilHealth: " + philhealth);
        System.out.println("Pag-IBIG: " + pagibig);
        System.out.println("Tax: " + tax);

        System.out.println("Total Deductions: +" + deductions);
        System.out.println("Net Salary: " + net2);

        System.out.println("=================================");
    }
}