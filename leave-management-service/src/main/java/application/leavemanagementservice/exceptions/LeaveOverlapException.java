package application.leavemanagementservice.exceptions;

public class LeaveOverlapException extends RuntimeException {
    public LeaveOverlapException(String message) {
        super(message);
    }
}
