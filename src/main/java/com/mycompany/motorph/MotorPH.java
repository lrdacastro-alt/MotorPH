package com.mycompany.motorph;

import java.util.*;
import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class MotorPH {

    static Scanner sc = new Scanner(System.in);
 
    // =====================
    // EMPLOYEE CLASS
    // =====================

    static class Employee {
        String id;
        String lastName;
        String firstName;
        String birthday;
        String grossSemiMonthly;
        String hourlyRate;
        String basicSalary;

        Employee(String id, String lastName, String firstName, String birthday,
                 String basicSalary, String grossSemiMonthly, String hourlyRate) {
            this.id               = id;
            this.lastName         = lastName;
            this.firstName        = firstName;
            this.birthday         = birthday;
            this.basicSalary      = basicSalary;
            this.grossSemiMonthly = grossSemiMonthly;
            this.hourlyRate       = hourlyRate;
        }

        String fullName() { return firstName + " " + lastName; }
    }

    static HashMap<String, Employee> employees = new HashMap<>();
    static List<String[]> attendanceRecords = new ArrayList<>();

    // Column indices in the employee CSV file (0-based)
    static final int BASIC_SALARY_INDEX      = 13; // Column 14: Monthly basic salary
    static final int GROSS_SEMI_MONTHLY_INDEX = 17; // Column 18: Gross semi-monthly rate
    static final int HOURLY_RATE_INDEX        = 18; // Column 19: Hourly rate
    static final String EMPLOYEE_FILE         = "MotorPH_Employee Data.csv";
    static final String ATTENDANCE_FILE       = "employee_attendance.csv";

    // Month name lookup by month number (index 0 is unused; 1 = January, etc.)
    static final String[] MONTH_NAMES = {"", "January", "February", "March", "April",
        "May", "June", "July", "August", "September", "October", "November", "December"};

    public static void main(String[] args) {
        System.out.println("=== MotorPH Payroll System ===");

        String userRole = login();

        loadEmployees();
        loadAttendance();

        if (userRole.equals("employee")) employeeMenu();
        else payrollMenu();

        sc.close();
    }

    // =====================
    // LOGIN
    // =====================

    /**
     * Prompts the user for credentials and validates them.
     * Accepted usernames: "employee" and "payroll_staff", both with password "12345".
     *
     * @return the validated username (used to determine menu routing)
     */
    static String login() {
        System.out.print("Username: ");
        String username = sc.nextLine().trim();

        System.out.print("Password: ");
        String password = sc.nextLine().trim();

        if ((username.equals("employee") || username.equals("payroll_staff")) && password.equals("12345")) {
            return username;
        }

        System.out.println("Invalid login.");
        System.exit(0);
        return ""; // Unreachable, but required for compilation
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
                        Employee employee = employees.get(id);
                        printEmployeeInfo(id, employee);
                    } else {
                        System.out.println("Employee number does not exist.");
                    }
                    break;

                case "2":
                    return;

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
    // PRINT EMPLOYEE INFO
    // =====================

    static void printEmployeeInfo(String id, Employee e) {
        System.out.println("Employee Number   : " + id);
        System.out.println("Name              : " + e.fullName());
        System.out.println("Birthday          : " + e.birthday);
        System.out.println("Basic Salary      : " + e.basicSalary);
        System.out.println("Gross Semi-monthly: " + e.grossSemiMonthly);
        System.out.println("Hourly Rate       : " + e.hourlyRate);
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
            BufferedReader br = new BufferedReader(new FileReader(EMPLOYEE_FILE));
            br.readLine(); // skip header

            String line;
            while ((line = br.readLine()) != null) {
                String[] columns = parseCSVLine(line);
                if (columns.length <= HOURLY_RATE_INDEX) continue;

                String id          = columns[0].trim();
                String last        = columns[1].trim();
                String first       = columns[2].trim();
                String bday        = columns[3].trim();
                String basicSalary = cleanMoney(columns[BASIC_SALARY_INDEX]);
                String grossSemi   = cleanMoney(columns[GROSS_SEMI_MONTHLY_INDEX]);
                String hourlyRate  = cleanMoney(columns[HOURLY_RATE_INDEX]);

                employees.put(id, new Employee(id, last, first, bday, basicSalary, grossSemi, hourlyRate));
            }

            br.close();
            System.out.println("Employees loaded: " + employees.size());

        } catch (Exception e) {
            System.out.println("Error loading employees: " + e.getMessage());
        }
    }

    // =====================
    // LOAD ATTENDANCE
    // =====================

    static void loadAttendance() {
        try {
            BufferedReader br = new BufferedReader(new FileReader(ATTENDANCE_FILE));
            br.readLine(); // skip header

            String line;
            while ((line = br.readLine()) != null) {
                String[] columns = parseCSVLine(line);
                if (columns.length >= 6) {
                    attendanceRecords.add(columns);
                }
            }

            br.close();

        } catch (Exception e) {
            System.out.println("Error loading attendance: " + e.getMessage());
        }
    }

    // =====================
    // GET MONTHS FROM ATTENDANCE
    // =====================

    static List<Integer> getAvailableMonths(String id) {
        Set<Integer> months = new TreeSet<>();

        for (String[] columns : attendanceRecords) {
            if (!columns[0].trim().equals(id)) continue;

            String[] dateParts = columns[3].trim().split("/");
            int month = Integer.parseInt(dateParts[0]);
            months.add(month);
        }

        return new ArrayList<>(months);
    }

    // =====================
    // CALCULATE HOURS
    // =====================

    static double calcHours(String id, int cutoff, int targetMonth) {
        double total = 0.0;
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("H:mm");

        LocalTime workStart   = LocalTime.of(8, 0);
        LocalTime graceEnd    = LocalTime.of(8, 10);
        LocalTime workEnd     = LocalTime.of(17, 0);
        double maxHoursPerDay = 8.0;
        int lunchMinutes      = 60;

        for (String[] columns : attendanceRecords) {
            if (!columns[0].trim().equals(id)) continue;

            String[] dateParts = columns[3].trim().split("/");
            int month = Integer.parseInt(dateParts[0]);
            int day   = Integer.parseInt(dateParts[1]);

            if (month != targetMonth) continue;

            boolean inCutoff1 = (cutoff == 1 && day <= 15);
            boolean inCutoff2 = (cutoff == 2 && day >= 16);
            if (!(inCutoff1 || inCutoff2)) continue;

            LocalTime timeIn  = LocalTime.parse(columns[4].trim(), timeFormatter);
            LocalTime timeOut = LocalTime.parse(columns[5].trim(), timeFormatter);

            // Grace period: if clocked in at 8:10 or earlier, treat as 8:00
            // If clocked in after 8:10, start counting from actual time in (late)
            LocalTime effectiveIn  = timeIn.isAfter(graceEnd) ? timeIn : workStart;

            // Cap time out at 5:00 PM — no overtime counted
            LocalTime effectiveOut = timeOut.isAfter(workEnd) ? workEnd : timeOut;

            // Skip if employee clocked out before or at their start time
            if (!effectiveOut.isAfter(effectiveIn)) continue;

            // Calculate minutes worked, subtract 1-hour lunch break
            long minutesWorked = Duration.between(effectiveIn, effectiveOut).toMinutes() - lunchMinutes;
            if (minutesWorked < 0) minutesWorked = 0;

            // Convert to hours and enforce 8-hour daily max
            double hoursWorked = Math.min(minutesWorked / 60.0, maxHoursPerDay);
            total += hoursWorked;
        }

        return total;
    }

    // =====================
    // PAYROLL CALCULATION
    // =====================

        static double[] calculateMonthPayroll(String id, int month, double hourlyRate) {
        double h1 = calcHours(id, 1, month);
        double g1 = h1 * hourlyRate;

        double h2 = calcHours(id, 2, month);
        double g2 = h2 * hourlyRate;

        BigDecimal bdG1            = BigDecimal.valueOf(g1).setScale(2, RoundingMode.HALF_UP);
        BigDecimal bdG2            = BigDecimal.valueOf(g2).setScale(2, RoundingMode.HALF_UP);
        BigDecimal monthlyGross    = bdG1.add(bdG2);
        BigDecimal sss             = BigDecimal.valueOf(calcSSS(monthlyGross.doubleValue())).setScale(2, RoundingMode.HALF_UP);
        BigDecimal philhealth      = BigDecimal.valueOf(calcPhilHealth(monthlyGross.doubleValue())).setScale(2, RoundingMode.HALF_UP);
        BigDecimal pagibig         = BigDecimal.valueOf(calcPagIbig(monthlyGross.doubleValue())).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalNonTax     = sss.add(philhealth).add(pagibig);
        BigDecimal taxableIncome   = monthlyGross.subtract(totalNonTax);
        BigDecimal tax             = BigDecimal.valueOf(calcTax(taxableIncome.doubleValue())).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalDeductions = totalNonTax.add(tax);

        BigDecimal net1 = bdG1;
        BigDecimal net2 = bdG2.subtract(totalDeductions);
        if (net2.compareTo(BigDecimal.ZERO) < 0) net2 = BigDecimal.ZERO;

        return new double[]{h1, bdG1.doubleValue(), net1.doubleValue(),
                            h2, bdG2.doubleValue(), net2.doubleValue(),
                            monthlyGross.doubleValue(), sss.doubleValue(),
                            philhealth.doubleValue(), pagibig.doubleValue(),
                            taxableIncome.doubleValue(), tax.doubleValue(),
                            totalDeductions.doubleValue()};
    }

    // =====================
    // DISPLAY PAYROLL
    // =====================

    static void displayPayroll(String id) {
        Employee e = employees.get(id);

        double basicSalary     = 0.0;
        double effectiveHourly = 0.0;

        try {
            basicSalary = Double.parseDouble(e.basicSalary);
        } catch (Exception ex) {
            System.out.println("Invalid basic salary for employee " + id + ". Using 0.");
        }
        try {
            effectiveHourly = Double.parseDouble(e.hourlyRate);
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
        printEmployeeInfo(id, e);

        for (int month : availableMonths) {
            double[] p = calculateMonthPayroll(id, month, effectiveHourly);
            // p[0]=h1, p[1]=g1, p[2]=net1, p[3]=h2, p[4]=g2, p[5]=net2
            // p[6]=monthlyGross, p[7]=sss, p[8]=philhealth, p[9]=pagibig
            // p[10]=taxableIncome, p[11]=tax, p[12]=totalDeductions

            System.out.println("\n-----------------------");
            System.out.println("Month: " + MONTH_NAMES[month]);

            System.out.println("\nCutoff Date        : " + MONTH_NAMES[month] + " 1 to 15");
            System.out.println("Total Hours Worked : " + p[0] + " hrs");
            System.out.println("Gross Salary       : " + p[1]);
            System.out.println("Net Salary         : " + p[2]);

            System.out.println("\nCutoff Date        : " + MONTH_NAMES[month] + " 16 to 30");
            System.out.println("Total Hours Worked : " + p[3] + " hrs");
            System.out.println("Gross Salary       : " + p[4]);
            System.out.println("Total Deductions   : " + p[12]);
            System.out.println("Net Salary         : " + p[5]);

            System.out.println("\n-- Deductions (based on Total Monthly Gross) --");
            System.out.println("Total Monthly Gross: " + p[6]);
            System.out.println("SSS                : " + p[7]);
            System.out.println("PhilHealth         : " + p[8]);
            System.out.println("Pag-IBIG           : " + p[9]);
            System.out.println("Taxable Income     : " + p[10]);
            System.out.println("Tax (Withholding)  : " + p[11]);
            System.out.println("Total Deductions   : " + p[12]);
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
        if (taxableIncome <= 20832)      return 0.00;
        else if (taxableIncome < 33333)  return (taxableIncome - 20832) * 0.20;
        else if (taxableIncome < 66667)  return 2500 + (taxableIncome - 33333) * 0.25;
        else if (taxableIncome < 166667) return 10833 + (taxableIncome - 66667) * 0.30;
        else if (taxableIncome < 666667) return 40833.33 + (taxableIncome - 166667) * 0.32;
        else                             return 200833.33 + (taxableIncome - 666667) * 0.35;
    }
}