package com.mycompany.motorph;

import java.util.*;
import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;

public class MotorPH {

    static Scanner sc = new Scanner(System.in);

    // key = employee number, value = [last, first, birthday, grossSemi, hourlyRate, basicSalary]
    static HashMap<String, String[]> employees = new HashMap<>();

    static final int BASIC_SALARY_INDEX       = 13;
    static final int GROSS_SEMI_MONTHLY_INDEX  = 17;
    static final int HOURLY_RATE_INDEX         = 18;

    static final String[] MONTH_NAMES = {"", "January", "February", "March", "April",
        "May", "June", "July", "August", "September", "October", "November", "December"};

    public static void main(String[] args) {
        System.out.println("=== MotorPH Payroll System ===");

        String user = login();
        loadEmployees();

        if (user.equals("employee")) employeeMenu();
        else payrollMenu();
    }

    // =====================
    // LOGIN
    // =====================

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

    // =====================
    // MENUS
    // =====================

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
                        System.out.println("Name              : " + e[1] + " " + e[0]);
                        System.out.println("Birthday          : " + e[2]);
                        System.out.println("Basic Salary      : " + e[5]);
                        System.out.println("Gross Semi-monthly: " + e[3]);
                        System.out.println("Hourly Rate       : " + e[4]);
                    } else {
                        System.out.println("Employee number does not exist.");
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
                    else System.out.println("Employee number does not exist.");
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

    // =====================
    // CSV HELPERS
    // =====================

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

    // =====================
    // LOAD EMPLOYEES
    // =====================

    static void loadEmployees() {
        try {
            BufferedReader br = new BufferedReader(new FileReader("MotorPH_Employee Data.csv"));
            br.readLine(); // skip header

            String line;
            while ((line = br.readLine()) != null) {
                String[] p = parseCSVLine(line);
                if (p.length <= HOURLY_RATE_INDEX) continue;

                String id          = p[0].trim();
                String last        = p[1].trim();
                String first       = p[2].trim();
                String bday        = p[3].trim();
                String basicSalary = cleanMoney(p[BASIC_SALARY_INDEX]);
                String grossSemi   = cleanMoney(p[GROSS_SEMI_MONTHLY_INDEX]);
                String hourlyRate  = cleanMoney(p[HOURLY_RATE_INDEX]);

                // [0]=last, [1]=first, [2]=bday, [3]=grossSemi, [4]=hourlyRate, [5]=basicSalary
                employees.put(id, new String[]{last, first, bday, grossSemi, hourlyRate, basicSalary});
            }

            br.close();
            System.out.println("Employees loaded: " + employees.size());

        } catch (Exception e) {
            System.out.println("Error loading employees: " + e.getMessage());
        }
    }

    // =====================
    // GET MONTHS FROM ATTENDANCE
    // =====================

    static List<Integer> getAvailableMonths(String id) {
        Set<Integer> months = new TreeSet<>();

        try {
            BufferedReader br = new BufferedReader(new FileReader("employee_attendance.csv"));
            br.readLine(); // skip header

            String line;
            while ((line = br.readLine()) != null) {
                String[] p = parseCSVLine(line);
                if (p.length < 6) continue;
                if (!p[0].trim().equals(id)) continue;

                String[] d = p[3].trim().split("/");
                int month = Integer.parseInt(d[0]);
                months.add(month);
            }

            br.close();

        } catch (Exception e) {
            System.out.println("Attendance read error: " + e.getMessage());
        }

        return new ArrayList<>(months);
    }

    // =====================
    // CALCULATE HOURS
    // =====================

    static double calcHours(String id, int cutoff, int targetMonth) {
        double total = 0.0;
        DateTimeFormatter f = DateTimeFormatter.ofPattern("H:mm");

        LocalTime workStart  = LocalTime.of(8, 0);
        LocalTime graceEnd   = LocalTime.of(8, 10);
        LocalTime workEnd    = LocalTime.of(17, 0);
        int lunchMinutes     = 60;

        try {
            BufferedReader br = new BufferedReader(new FileReader("employee_attendance.csv"));
            br.readLine(); // skip header

            String line;
            while ((line = br.readLine()) != null) {
                String[] p = parseCSVLine(line);
                if (p.length < 6) continue;
                if (!p[0].trim().equals(id)) continue;

                String[] d = p[3].trim().split("/");
                int month = Integer.parseInt(d[0]);
                int day   = Integer.parseInt(d[1]);

                if (month != targetMonth) continue;

                boolean inCutoff1 = (cutoff == 1 && day <= 15);
                boolean inCutoff2 = (cutoff == 2 && day >= 16);
                if (!(inCutoff1 || inCutoff2)) continue;

                LocalTime timeIn  = LocalTime.parse(p[4].trim(), f);
                LocalTime timeOut = LocalTime.parse(p[5].trim(), f);

                LocalTime effectiveIn  = (!timeIn.isAfter(graceEnd)) ? workStart : timeIn;
                LocalTime effectiveOut = timeOut.isAfter(workEnd) ? workEnd : timeOut;

                long minutesWorked = Duration.between(effectiveIn, effectiveOut).toMinutes() - lunchMinutes;
                if (minutesWorked < 0) minutesWorked = 0;

                total += (minutesWorked / 60.0);
            }

            br.close();

        } catch (Exception e) {
            System.out.println("Attendance read error: " + e.getMessage());
        }

        return total;
    }

    // =====================
    // DISPLAY PAYROLL
    // =====================

    static void displayPayroll(String id) {
        String[] e = employees.get(id);

        double basicSalary     = 0.0;
        double effectiveHourly = 0.0;

        try {
            basicSalary = Double.parseDouble(e[5]);
        } catch (Exception ex) {
            System.out.println("Invalid basic salary for employee " + id + ". Using 0.");
        }

        try {
            effectiveHourly = Double.parseDouble(e[4]);
        } catch (Exception ex) {
            System.out.println("Invalid hourly rate for employee " + id + ". Fallback to basicSalary/160.");
            effectiveHourly = basicSalary / 160.0;
        }

        List<Integer> availableMonths = getAvailableMonths(id);

        if (availableMonths.isEmpty()) {
            System.out.println("No attendance records found for employee " + id + ".");
            return;
        }

        System.out.println("\n=======================");
        System.out.println("Employee Number : " + id);
        System.out.println("Employee Name   : " + e[1] + " " + e[0]);
        System.out.println("Birthday        : " + e[2]);
        System.out.printf("Basic Salary    : %.2f\n", basicSalary);

        for (int month : availableMonths) {

            double h1 = calcHours(id, 1, month);
            double g1 = h1 * effectiveHourly;

            double h2 = calcHours(id, 2, month);
            double g2 = h2 * effectiveHourly;

            double monthlyGross = g1 + g2;

            // Deductions based on combined monthly gross
            double sss        = calcSSS(monthlyGross);
            double philhealth = calcPhilHealth(monthlyGross);
            double pagibig    = calcPagIbig(monthlyGross);
            double totalNonTax = sss + philhealth + pagibig;

            // Tax based on (g1+g2) - non-tax deductions
            double taxableIncome   = monthlyGross - totalNonTax;
            double tax             = calcTax(taxableIncome);
            double totalDeductions = totalNonTax + tax;

            double net1 = g1;                        // no deductions on first cutoff
            double net2 = g2 - totalDeductions;      // all deductions on second cutoff
            if (net2 < 0) net2 = 0;

            System.out.println("\n-----------------------");
            System.out.println("Month: " + MONTH_NAMES[month]);

            // ---- CUTOFF 1 ----
            System.out.println("\nCutoff Date        : " + MONTH_NAMES[month] + " 1 to 15");
            System.out.printf("Total Hours Worked : %.2f hrs\n", h1);
            System.out.printf("Gross Salary       : %.2f\n", g1);
            System.out.printf("Net Salary         : %.2f\n", net1);

            // ---- CUTOFF 2 ----
            System.out.println("\nCutoff Date        : " + MONTH_NAMES[month] + " 16 to 30");
            System.out.printf("Total Hours Worked : %.2f hrs\n", h2);
            System.out.printf("Gross Salary       : %.2f\n", g2);
            System.out.printf("Total Deductions   : %.2f\n", totalDeductions);
            System.out.printf("Net Salary         : %.2f\n", net2);
            
            System.out.println("\n-- Deductions (based on Total Monthly Gross --");         
            System.out.printf("Total Monthly Gross: %.2f\n", monthlyGross);
            System.out.printf("SSS                : %.2f\n", sss);
            System.out.printf("PhilHealth         : %.2f\n", philhealth);
            System.out.printf("Pag-IBIG           : %.2f\n", pagibig);            
            System.out.printf("Taxable Income     : %.2f\n", taxableIncome);
            System.out.printf("Tax (Withholding)  : %.2f\n", tax);            
            System.out.printf("Total Deductions   : %.2f\n", totalDeductions);
            
            
        }

        System.out.println("\n=======================");
    }

    // =====================
    // DEDUCTION CALCULATIONS
    // =====================

    static double calcPagIbig(double basicSalary) {
        double contribution;

        if (basicSalary >= 1000 && basicSalary <= 1500) {
            contribution = basicSalary * 0.01;
        } else {
            contribution = basicSalary * 0.02;
        }

        return Math.min(contribution, 100.00); // max 100 per month
    }

    static double calcPhilHealth(double basicSalary) {
        double premium;

        if (basicSalary <= 10000) {
            premium = 300.00;
        } else if (basicSalary >= 60000) {
            premium = 1800.00;
        } else {
            premium = basicSalary * 0.03;
        }

        return premium / 2; // employee pays half
    }

    static double calcSSS(double basicSalary) {
        if (basicSalary < 3250)        return 135.00;
        else if (basicSalary <= 3750)  return 157.50;
        else if (basicSalary <= 4250)  return 180.00;
        else if (basicSalary <= 4750)  return 202.50;
        else if (basicSalary <= 5250)  return 225.00;
        else if (basicSalary <= 5750)  return 247.50;
        else if (basicSalary <= 6250)  return 270.00;
        else if (basicSalary <= 6750)  return 292.50;
        else if (basicSalary <= 7250)  return 315.00;
        else if (basicSalary <= 7750)  return 337.50;
        else if (basicSalary <= 8250)  return 360.00;
        else if (basicSalary <= 8750)  return 382.50;
        else if (basicSalary <= 9250)  return 405.00;
        else if (basicSalary <= 9750)  return 427.50;
        else if (basicSalary <= 10250) return 450.00;
        else if (basicSalary <= 10750) return 472.50;
        else if (basicSalary <= 11250) return 495.00;
        else if (basicSalary <= 11750) return 517.50;
        else if (basicSalary <= 12250) return 540.00;
        else if (basicSalary <= 12750) return 562.50;
        else if (basicSalary <= 13250) return 585.00;
        else if (basicSalary <= 13750) return 607.50;
        else if (basicSalary <= 14250) return 630.00;
        else if (basicSalary <= 14750) return 652.50;
        else if (basicSalary <= 15250) return 675.00;
        else if (basicSalary <= 15750) return 697.50;
        else if (basicSalary <= 16250) return 720.00;
        else if (basicSalary <= 16750) return 742.50;
        else if (basicSalary <= 17250) return 765.00;
        else if (basicSalary <= 17750) return 787.50;
        else if (basicSalary <= 18250) return 810.00;
        else if (basicSalary <= 18750) return 832.50;
        else if (basicSalary <= 19250) return 855.00;
        else if (basicSalary <= 19750) return 877.50;
        else if (basicSalary <= 20250) return 900.00;
        else if (basicSalary <= 20750) return 922.50;
        else if (basicSalary <= 21250) return 945.00;
        else if (basicSalary <= 21750) return 967.50;
        else if (basicSalary <= 22250) return 990.00;
        else if (basicSalary <= 22750) return 1012.50;
        else if (basicSalary <= 23250) return 1035.00;
        else if (basicSalary <= 23750) return 1057.50;
        else if (basicSalary <= 24250) return 1080.00;
        else if (basicSalary <= 24750) return 1102.50;
        else                           return 1125.00;
    }

    static double calcTax(double taxableIncome) {
        if (taxableIncome <= 20832)       return 0.00;
        else if (taxableIncome < 33333)   return (taxableIncome - 20832) * 0.20;
        else if (taxableIncome < 66667)   return 2500 + (taxableIncome - 33333) * 0.25;
        else if (taxableIncome < 166667)  return 10833 + (taxableIncome - 66667) * 0.30;
        else if (taxableIncome < 666667)  return 40833.33 + (taxableIncome - 166667) * 0.32;
        else                              return 200833.33 + (taxableIncome - 666667) * 0.35;
    }
}