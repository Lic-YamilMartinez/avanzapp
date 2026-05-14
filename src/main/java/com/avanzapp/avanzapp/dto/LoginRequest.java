// LoginRequest.java
package com.avanzapp.avanzapp.dto;

public class LoginRequest {
    private String cedula;
    private String password;

    public LoginRequest() {
    }

    public LoginRequest(String cedula, String password) {
        this.cedula = cedula;
        this.password = password;
    }
    public LoginRequest(String password) {
        this.password = password;
    }

    // Getters y setters

    public String getCedula() {
        return cedula;
    }

    public void setCedula(String email) {
        this.cedula = email;
    }
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }



}

