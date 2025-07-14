package aptech.be.dto.staff;

public class AttendanceReport {
    private String staffCode;
    private String name;
    private int totalDays;
    private double totalHours;
    private long daysAbsent;
    private long timesLate;

    public AttendanceReport() {
    }

    public AttendanceReport(String staffCode, String name, int totalDays, long daysAbsent, double totalHours, long timesLate) {
        this.staffCode = staffCode;
        this.name = name;
        this.totalDays = totalDays;
        this.daysAbsent = daysAbsent;
        this.totalHours = totalHours;
        this.timesLate = timesLate;
    }

    public String getStaffCode() {
        return staffCode;
    }

    public void setStaffCode(String staffCode) {
        this.staffCode = staffCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getTotalDays() {
        return totalDays;
    }

    public void setTotalDays(int totalDays) {
        this.totalDays = totalDays;
    }

    public double getTotalHours() {
        return totalHours;
    }

    public void setTotalHours(double totalHours) {
        this.totalHours = totalHours;
    }

    public long getDaysAbsent() {
        return daysAbsent;
    }

    public void setDaysAbsent(long daysAbsent) {
        this.daysAbsent = daysAbsent;
    }

    public long getTimesLate() {
        return timesLate;
    }

    public void setTimesLate(long timesLate) {
        this.timesLate = timesLate;
    }
}
