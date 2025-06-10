package com.avanzapp.avanzapp.repository;

import com.avanzapp.avanzapp.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    // Ya podés usar métodos como findAll(), findById(), save(), delete(), etc.
}
