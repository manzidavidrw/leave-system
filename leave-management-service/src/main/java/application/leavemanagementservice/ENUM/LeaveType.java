package application.leavemanagementservice.ENUM;

public enum LeaveType {
    SICK("Sick Leave", 15),
    ANNUAL("Annual Leave", 21),
    CASUAL("Casual Leave", 7),
    MATERNITY("Maternity Leave", 90);

    private final String displayName;
    private final int defaultAllowance; // days per year

    LeaveType(String displayName, int defaultAllowance) {
        this.displayName = displayName;
        this.defaultAllowance = defaultAllowance;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getDefaultAllowance() {
        return defaultAllowance;
    }
}