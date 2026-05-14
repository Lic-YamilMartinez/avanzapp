package com.avanzapp.avanzapp.controller;

import com.avanzapp.avanzapp.dto.LoginRequest;
import com.avanzapp.avanzapp.model.Usuario;
import com.avanzapp.avanzapp.security.JwtUtil;
import com.avanzapp.avanzapp.service.UsuarioService;
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

        // 1) Normalizamos cedula para evitar "762.877" vs "762877" vs "762877 "
        String cedula = request.getCedula() == null ? "" : request.getCedula()
                .replace(".", "")
                .replace("-", "")
                .trim();

        String password = request.getPassword() == null ? "" : request.getPassword().trim();

        System.out.println("LOGIN -> cedula recibida: [" + request.getCedula() + "] normalizada: [" + cedula + "]");

        // 2) Validar credenciales
        Usuario usuario = usuarioService.validarCredenciales(cedula, password);

        // 3) Si no existe o pass incorrecto => 401 (y NO explota)
        if (usuario == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "error", "UNAUTHORIZED",
                    "message", "Cédula o contraseña incorrecta"
            ));
        }

        System.out.println("LOGIN OK -> usuario cedula: " + usuario.getCedula() + " rol: " + usuario.getRol());

        // 4) Token
        String token = jwtUtil.generateToken(usuario.getCedula());

        // 5) Respuesta para el front
        return ResponseEntity.ok(Map.of(
                "token", token,
                "role", usuario.getRol(),
                "email", usuario.getEmail() == null ? "" : usuario.getEmail(),
                "nombre", usuario.getNombre() == null ? "" : usuario.getNombre(),
                "cedula", usuario.getCedula()
        ));
    }
}
