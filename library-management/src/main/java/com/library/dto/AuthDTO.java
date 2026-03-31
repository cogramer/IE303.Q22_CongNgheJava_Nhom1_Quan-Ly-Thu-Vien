package com.library.dto;

public class AuthDTO {
    public static class LoginRequest {
        private String username;
        private String password;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class LoginResponse {
        private String message;
        private String token;

        public LoginResponse(String message, String token) {
            this.message = (message != null) ? message : "";
            this.token = (token != null) ? token : "";
        }

        public String getMessage() {
            return message;
        }

        public String getToken() {
            return token;
        }
    }

    public static class RegisterRequest {
        private String username;
        private String password;
        private String email;
        private String fullName;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }
    }

    public static class RegisterResponse {
        private String message;
        private String username;
        private String email;
        private String fullName;

        public RegisterResponse(String message, String username, String email, String fullName) {
            this.message = (message != null) ? message : "";
            this.username = (username != null) ? username : "";
            this.email = (email != null) ? email : "";
            this.fullName = (fullName != null) ? fullName : "";
        }

        public String getMessage() {
            return message;
        }

        public String getUsername() {
            return username;
        }
        
        public String getEmail() {
            return email;
        }

        public String getFullName() {
            return fullName;
        }
    }

    public static class ChangePasswordRequest {
        private String oldPassword;
        private String newPassword;

        public String getOldPassword() {
            return oldPassword;
        }

        public String getNewPassword() {
            return newPassword;
        }
    }
}
