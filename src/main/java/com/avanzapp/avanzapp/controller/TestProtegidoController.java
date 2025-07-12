package com.avanzapp.avanzapp.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestProtegidoController {

    @GetMapping("/protegido")
    public String recursoProtegido() {
        return "Acceso autorizado al recurso protegido 🔐";
    }
}
