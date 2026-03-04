package com.streaming.features.auth.models;

public class LoginResponseDto {

    private String accessToken;
    private UserInfoDto user;

    public LoginResponseDto(String accessToken, UserInfoDto user) {
        this.accessToken = accessToken;
        this.user = user;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public UserInfoDto getUser() {
        return user;
    }

    public void setUser(UserInfoDto user) {
        this.user = user;
    }
}
