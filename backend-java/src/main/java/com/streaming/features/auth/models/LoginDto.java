package com.streaming.features.auth.models;

import io.swagger.v3.oas.annotations.media.Schema;

public class LoginDto {

    @Schema(example = "admin@example.com")
    private String email;

    @Schema(example = "password123")
    private String password;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
