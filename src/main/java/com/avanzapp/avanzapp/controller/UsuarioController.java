package com.avanzapp.avanzapp.controller;

import com.avanzapp.avanzapp.dto.UserDTO;
import com.avanzapp.avanzapp.model.Usuario;
import com.avanzapp.avanzapp.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/usuarios")
@CrossOrigin(origins = "*")
public class UsuarioController {

    private final UsuarioService usuarioService;

    @Autowired
    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // 🔹 Obtener todos los usuarios
    @GetMapping
    public List<Usuario> obtenerTodos() {
        return usuarioService.obtenerTodos();
    }

    // 🔹 Obtener un usuario por ID
    @GetMapping("/{id}")
    public Usuario obtenerPorId(@PathVariable Long id) {
        return usuarioService.obtenerPorId(id);
    }

    // 🔹 Crear nuevo usuario
    @PostMapping
    public Usuario crearUsuario(@RequestBody Usuario usuario) {
        return usuarioService.guardar(usuario);
    }

    // 🔹 Actualizar usuario existente
    @PutMapping("/{id}")
    public Usuario actualizarUsuario(@PathVariable Long id, @RequestBody Usuario usuario) {
        return usuarioService.actualizar(id, usuario);
    }

    // 🔹 Eliminar usuario por ID
    @DeleteMapping("/{id}")
    public void eliminarUsuario(@PathVariable Long id) {
        usuarioService.eliminar(id);
    }

    // 🔹 Obtener los datos del usuario logueado (desde el JWT)
    @GetMapping("/me")
    public ResponseEntity<UserDTO> obtenerMisDatos(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String email = userDetails.getUsername();
        UserDTO dto = usuarioService.obtenerDTOporEmail(email);
        return ResponseEntity.ok(dto);
    }

    // 🔹 Nuevo endpoint: Obtener solo los clientes (usuarios con rol CLIENT)
    @GetMapping("/clientes")
    public ResponseEntity<List<Usuario>> obtenerClientes() {
        List<Usuario> clientes = usuarioService.obtenerTodos()
                .stream()
                .filter(u -> "CLIENT".equalsIgnoreCase(u.getRol()))
                .toList();

        return ResponseEntity.ok(clientes);
    }
}
