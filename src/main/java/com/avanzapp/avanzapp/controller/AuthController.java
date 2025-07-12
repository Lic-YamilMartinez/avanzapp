package com.avanzapp.avanzapp.controller;

import com.avanzapp.avanzapp.dto.LoginRequest;
import com.avanzapp.avanzapp.dto.LoginResponse;
import com.avanzapp.avanzapp.model.Usuario;
import com.avanzapp.avanzapp.service.UsuarioService;
import com.avanzapp.avanzapp.security.JwtUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        Usuario usuario = usuarioService.validarCredenciales(request.getEmail(), request.getPassword());

        if (usuario == null) {
            return ResponseEntity.status(401).body("Credenciales inválidas");
        }

        String token = jwtUtil.generateToken(usuario.getEmail());
        String rol = usuario.getRol(); // Asegurate de tener getRol() en Usuario
        System.out.println("Usuario: "+usuario.toString());
        return ResponseEntity.ok(Map.of(
                "token", token,
                "role", usuario.getRol(),
                "email", usuario.getEmail(),
                "nombre", usuario.getNombre()
        ));
    }
}
