package com.avanzapp.avanzapp.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/login")
public class LoginController {

    @PostMapping
    public ResponseEntity<String> login(@RequestBody Map<String, String> credenciales) {
        String email = credenciales.get("email");
        String password = credenciales.get("password");

        // Aquí deberías verificar con tu base de datos
        if ("admin@avanza.com".equals(email) && "admin123".equals(password)) {
            return ResponseEntity.ok("Login exitoso");
        } else {
            return ResponseEntity.status(401).body("Credenciales inválidas");
        }
    }
}
