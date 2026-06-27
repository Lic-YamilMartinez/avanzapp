package com.avanzapp.avanzapp.service;

import com.avanzapp.avanzapp.model.Usuario;
import com.avanzapp.avanzapp.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import com.avanzapp.avanzapp.dto.UserDTO;


import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Autenticación para Spring Security
    @Override
    public UserDetails loadUserByUsername(String cedula) throws UsernameNotFoundException {
        System.out.println("En Usuario Service:"+cedula);
        Usuario usuario = usuarioRepository.findByCedula(cedula)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con cedula: " + cedula));

        return new org.springframework.security.core.userdetails.User(
                usuario.getCedula(),
                usuario.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().toUpperCase()))
        );
    }


    // Obtener todos los usuarios
    public List<Usuario> obtenerTodos() {
        return usuarioRepository.findAll();
    }

    // Buscar por ID
    public Usuario obtenerPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));
    }

    // Guardar nuevo usuario (con encriptación)
    public Usuario guardar(Usuario usuario) {
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        return usuarioRepository.save(usuario);
    }

    // Actualizar usuario existente
    public Usuario actualizar(Long id, Usuario usuarioActualizado) {
        return usuarioRepository.findById(id)
                .map(usuario -> {
                    usuario.setNombre(usuarioActualizado.getNombre());
                    usuario.setEmail(usuarioActualizado.getEmail());
                    usuario.setPassword(passwordEncoder.encode(usuarioActualizado.getPassword()));
                    usuario.setRol(usuarioActualizado.getRol());
                    return usuarioRepository.save(usuario);
                })
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));
    }

    // Eliminar por ID
    public void eliminar(Long id) {
        usuarioRepository.deleteById(id);
    }

    // Validar credenciales para login
    public Usuario validarCredenciales(String cedula, String password) {
        System.out.println("HASH 1234 = " + passwordEncoder.encode("1234"));
        System.out.println("Cedula: "+cedula);
        Optional<Usuario> usuarioOpt = usuarioRepository.findByCedula(cedula);

        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();

            System.out.println("Usuario encontrado: " + usuario.getCedula());
            System.out.println("Password DB: " + usuario.getPassword());
            System.out.println("Password match: " + passwordEncoder.matches(password, usuario.getPassword()));

            if (passwordEncoder.matches(password, usuario.getPassword())) {
                return usuario;
            }
        }
        return null;
    }


    public UserDTO obtenerDTOporCedula(String cedula) {
        Usuario usuario = usuarioRepository.findByCedula(cedula)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con cedula: " + cedula));

        return new UserDTO(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getEmail(),
                usuario.getTelefono(),
                usuario.getUbicacion(),
                usuario.getCedula()
        );
    }
    public Usuario obtenerPorCedula(String cedula) {
        return usuarioRepository.findByCedula(cedula)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Usuario no encontrado con cedula: " + cedula));
    }

}
