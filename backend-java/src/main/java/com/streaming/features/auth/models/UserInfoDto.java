package com.streaming.features.auth.models;

import io.swagger.v3.oas.annotations.media.Schema;

public class UserInfoDto {

    @Schema(example = "admin@example.com")
    private String email;

    @Schema(example = "Demo Admin")
    private String name;

    public UserInfoDto(String email, String name) {
        this.email = email;
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
