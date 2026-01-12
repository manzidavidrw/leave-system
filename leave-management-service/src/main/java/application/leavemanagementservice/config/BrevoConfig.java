//package application.leavemanagementservice.config;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import sibApi.ApiClient;
//import sibApi.TransactionalEmailsApi;
//
//@Configuration
//public class BrevoConfig {
//
//    @Bean
//    public TransactionalEmailsApi brevoClient() {
//        ApiClient defaultClient = new ApiClient();
//        defaultClient.setApiKey(System.getenv("BREVO_API_KEY")); // or use @Value
//        return new TransactionalEmailsApi(defaultClient);
//    }
//}
