/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.mycompany.motorph;

/**
 *
 * @author daniel
 */

import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class MotorPH {

    

    // ── 1. MAIN ────────────────────────────────────────────────
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        String username = login(scanner);

        if (username.equals("employee")) {
            employeeMenu(scanner);
        } else if (username.equals("payroll_staff")) {
            payrollMenu(scanner);
        }

        scanner.close();
    }

    // ── 2. LOGIN ───────────────────────────────────────────────
    static String login(Scanner scanner) {
        System.out.print("Enter username: ");
        String username = scanner.nextLine();

        System.out.print("Enter password: ");
        String password = scanner.nextLine();

        boolean validUsername = username.equals("employee") || username.equals("payroll_staff");
        boolean validPassword = password.equals("12345");

        if (!validUsername || !validPassword) {
            System.out.println("Incorrect username and/or password.");
            scanner.close();
            System.exit(0);
        }

        return username;
    }

    // ── 3. LOAD EMPLOYEES FROM CSV ─────────────────────────────


    // ── 4. EMPLOYEE MENU ───────────────────────────────────────
    static void employeeMenu(Scanner scanner) {
    List<String[]> employees = new ArrayList<>();

    try (BufferedReader br = new BufferedReader(new FileReader("MotorPH_Employee Data.csv"))) {
        br.readLine(); // skip header row
        String line;

        while ((line = br.readLine()) != null) {
            String[] parts = line.split(",", -1);
            if (parts.length >= 4) {
                employees.add(new String[]{
                parts[0].trim(), // number
                parts[1].trim(), // lastName
                parts[2].trim(), // firstName
                parts[3].trim()  // birthday
            });
            }
        }
    } catch (IOException e) {
        System.out.println("Error: Could not load employee file.");
    }

    boolean checkEmployee = true;
    while (checkEmployee) {
        System.out.println("--- Employee Menu ---");
        System.out.println("1. Enter your employee number");
        System.out.println("2. Exit the program");
        System.out.print("Choose an option: ");
        String choice = scanner.nextLine();

        switch (choice) {
            case "1":
                searchEmployee(scanner, employees);
                break;
            case "2":
                checkEmployee = false;
                break;
        }
    }
}

    // ── 5. SEARCH EMPLOYEE ─────────────────────────────────────
    static void searchEmployee(Scanner scanner, List<String[]> employees) {
        System.out.print("Enter employee number: ");
        String empNum = scanner.nextLine();

        boolean found = false;
        for (String[] emp : employees) {
            if (emp[0].equalsIgnoreCase(empNum)) {
                displayEmployee(emp);
                found = true;
                break;
            }
        }

        if (!found) {
            System.out.println("Employee number does not exist.");
        }
    }

    // ── 6. DISPLAY EMPLOYEE DETAILS ────────────────────────────
    static void displayEmployee(String[] emp) {
        System.out.println("=============================");
        System.out.println("      EMPLOYEE DETAILS       ");
        System.out.println("=============================");
        System.out.println("Employee Number : " + emp[0]);
        System.out.println("Employee Name   : " + emp[2] + " " + emp[1]);
        System.out.println("Birthday        : " + emp[3]);
        System.out.println("=============================");
    }

    // ── 7. PAYROLL STAFF MENU ──────────────────────────────────
    static void payrollMenu(Scanner scanner) {
        System.out.println("Welcome, Payroll Staff!");
        // TODO: payroll_staff logic here
    }

}
