package application.leavemanagementservice.config;

import application.leavemanagementservice.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "auth-service", url = "${auth.service.url}")
public interface AuthServiceClient {

    @GetMapping("/api/auth/user/{userId}")
    UserDTO getUserById(@PathVariable("userId") Long userId);

    @GetMapping("/api/users/me")
    UserDTO getCurrentUser(@RequestHeader("Authorization") String token);

    @GetMapping("/api/users/{userId}/manager")
    UserDTO getUserManager(@PathVariable("userId") Long userId,
                           @RequestHeader("Authorization") String token);
}