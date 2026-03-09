package com.mycompany.motorph;

import java.util.*;
import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;

public class MotorPH {

    static Scanner sc = new Scanner(System.in);

    // key = employee number, value = [last, first, birthday, grossSemi, hourlyRate]
    static HashMap<String, String[]> employees = new HashMap<>();

    static final int GROSS_SEMI_MONTHLY_INDEX = 17;
    static final int HOURLY_RATE_INDEX = 18;

    public static void main(String[] args) {
        System.out.println("=== MotorPH Payroll System ===");

        String user = login();
        loadEmployees();

        if (user.equals("employee")) employeeMenu();
        else payrollMenu();
    }

    static String login() {
        System.out.print("Username: ");
        String u = sc.nextLine().trim();

        System.out.print("Password: ");
        String p = sc.nextLine().trim();

        if ((u.equals("employee") || u.equals("payroll_staff")) && p.equals("12345")) return u;

        System.out.println("Invalid login.");
        System.exit(0);
        return "";
    }

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
                        String[] e = employees.get(id);
                        System.out.println("Name: " + e[1] + " " + e[0]);
                        System.out.println("Birthday: " + e[2]);
                        System.out.println("Gross Semi-monthly: " + e[3]);
                        System.out.println("Hourly Rate: " + e[4]);
                    } else {
                        System.out.println("Employee not found.");
                    }
                    break;

                case "2":
                    System.exit(0);
                    break;

                default:
                    System.out.println("Invalid choice.");
            }
        }
    }

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
                    for (String idNum : employees.keySet()) displayPayroll(idNum);
                    break;

                case "3":
                    return;

                default:
                    System.out.println("Invalid choice.");
            }
        }
    }

    // split CSV line while respecting quoted commas
    static String[] parseCSVLine(String line) {
        List<String> cols = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);

            if (ch == '"') {
                inQuotes = !inQuotes;
            } else if (ch == ',' && !inQuotes) {
                cols.add(cur.toString().trim());
                cur.setLength(0);
            } else {
                cur.append(ch);
            }
        }
        cols.add(cur.toString().trim());

        return cols.toArray(new String[0]);
    }

    static String cleanMoney(String s) {
        return s.replace("\"", "").replace(",", "").trim();
    }

    static void loadEmployees() {
        try {
            BufferedReader br = new BufferedReader(new FileReader("MotorPH_Employee Data.csv"));
            br.readLine(); // header

            String line;
            while ((line = br.readLine()) != null) {
                String[] p = parseCSVLine(line);
                if (p.length <= HOURLY_RATE_INDEX) continue;

                String id = p[0].trim();
                String last = p[1].trim();
                String first = p[2].trim();
                String bday = p[3].trim();

                String grossSemi = cleanMoney(p[GROSS_SEMI_MONTHLY_INDEX]);
                String hourlyRate = cleanMoney(p[HOURLY_RATE_INDEX]);

                employees.put(id, new String[]{last, first, bday, grossSemi, hourlyRate});
            }

            br.close();
            System.out.println("Employees loaded: " + employees.size());

        } catch (Exception e) {
            System.out.println("Error loading employees.");
            System.out.println("Details: " + e.getMessage());
        }
    }

    static double calcHours(String id, int cutoff) {
        double total = 0.0;
        DateTimeFormatter f = DateTimeFormatter.ofPattern("H:mm");

        LocalTime workStart = LocalTime.of(8, 0);
        LocalTime graceEnd = LocalTime.of(8, 10);
        LocalTime workEnd = LocalTime.of(17, 0);
        int lunchMinutes = 60;

        try {
            BufferedReader br = new BufferedReader(new FileReader("employee_attendance.csv"));
            br.readLine(); // header
            String line;

            while ((line = br.readLine()) != null) {
                String[] p = parseCSVLine(line);
                if (p.length < 6) continue;
                if (!p[0].trim().equals(id)) continue;

                String[] d = p[3].trim().split("/");
                int month = Integer.parseInt(d[0]);
                int day = Integer.parseInt(d[1]);

                if (month != 6) continue;

                boolean inCutoff1 = (cutoff == 1 && day <= 15);
                boolean inCutoff2 = (cutoff == 2 && day >= 16);
                if (!(inCutoff1 || inCutoff2)) continue;

                LocalTime timeIn = LocalTime.parse(p[4].trim(), f);
                LocalTime timeOut = LocalTime.parse(p[5].trim(), f);

                LocalTime effectiveIn = (!timeIn.isAfter(graceEnd)) ? workStart : timeIn;
                LocalTime effectiveOut = timeOut.isAfter(workEnd) ? workEnd : timeOut;

                long minutesWorked = Duration.between(effectiveIn, effectiveOut).toMinutes() - lunchMinutes;
                if (minutesWorked < 0) minutesWorked = 0;

                total += (minutesWorked / 60.0);
            }

            br.close();

        } catch (Exception e) {
            System.out.println("Attendance read error.");
            System.out.println("Details: " + e.getMessage());
        }

        return total;
    }

    static void displayPayroll(String id) {
        String[] e = employees.get(id);

        double grossSemi = 0.0;
        double effectiveHourly = 0.0;

        try {
            grossSemi = Double.parseDouble(e[3]);
        } catch (Exception ex) {
            System.out.println("Invalid gross semi-monthly for employee " + id + ". Using 0.");
        }

        try {
            effectiveHourly = Double.parseDouble(e[4]);
        } catch (Exception ex) {
            System.out.println("Invalid hourly rate for employee " + id + ". Fallback to grossSemi/80.");
            effectiveHourly = grossSemi / 80.0;
        }

        System.out.println("\n=======================");
        System.out.println("Employee: " + e[1] + " " + e[0]);
        System.out.printf("Hourly Rate Used: %.2f\n", effectiveHourly); // debug proof

        double h1 = calcHours(id, 1);
        double g1 = h1 * effectiveHourly;

        System.out.println("\nJune 1-15");
        System.out.printf("Hours: %.4f\n", h1);
        System.out.printf("Gross: %.2f\n", g1);
        System.out.printf("Net: %.2f\n", g1);

        double h2 = calcHours(id, 2);
        double g2 = h2 * effectiveHourly;

        double deductions = 0.0;
        double net2 = g2 - deductions;
        if (net2 < 0) net2 = 0;

        System.out.println("\nJune 16-30");
        System.out.printf("Hours: %.4f\n", h2);
        System.out.printf("Gross: %.2f\n", g2);
        System.out.printf("Deductions: %.2f\n", deductions);
        System.out.printf("Net: %.2f\n", net2);
        System.out.println("=======================");
    }
}