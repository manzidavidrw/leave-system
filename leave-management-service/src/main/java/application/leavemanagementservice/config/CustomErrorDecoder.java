//package application.leavemanagementservice.config;
//
//import feign.Response;
//import feign.codec.ErrorDecoder;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.HttpStatus;
//import org.springframework.web.server.ResponseStatusException;
//
//@Slf4j
//public class CustomErrorDecoder implements ErrorDecoder {
//
//    private final ErrorDecoder defaultErrorDecoder = new Default();
//
//    @Override
//    public Exception decode(String methodKey, Response response) {
//        log.error("Feign client error: {} - Status: {}", methodKey, response.status());
//
//        switch (response.status()) {
//            case 401:
//                return new ResponseStatusException(HttpStatus.UNAUTHORIZED,
//                        "Authentication failed when calling auth service");
//            case 403:
//                return new ResponseStatusException(HttpStatus.FORBIDDEN,
//                        "Authorization failed when calling auth service. Check if the token is valid.");
//            case 404:
//                return new ResponseStatusException(HttpStatus.NOT_FOUND,
//                        "Resource not found in auth service");
//            default:
//                return defaultErrorDecoder.decode(methodKey, response);
//        }
//    }
//}