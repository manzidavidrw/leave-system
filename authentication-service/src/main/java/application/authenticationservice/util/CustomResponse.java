package application.authenticationservice.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomResponse<T> {
    private String message;
    private int statusCode;
    private T data;

    // Private constructors to enforce controlled object creation
    private CustomResponse(String message, int statusCode, T data) {
        this.message = message;
        this.statusCode = statusCode;
        this.data = data;
    }

    private CustomResponse(String message, int statusCode) {
        this.message = message;
        this.statusCode = statusCode;
    }

    public static <T> CustomResponse<T> successResponse(String message, int statusCode, T data) {
        return new CustomResponse<>(message, statusCode, data);
    }

    public static <T> CustomResponse<T> successResponse(String message, int statusCode) {
        return new CustomResponse<>(message, statusCode);
    }

    public static <T> CustomResponse<T> errorResponse(String message, int statusCode) {
        return new CustomResponse<>(message, statusCode);
    }
}
