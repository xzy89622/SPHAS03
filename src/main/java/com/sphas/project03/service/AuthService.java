package com.sphas.project03.service;

public interface AuthService {

    Long register(String username, String password, String nickname);

    String login(String username, String password);
}

