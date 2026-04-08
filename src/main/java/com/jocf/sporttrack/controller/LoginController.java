package com.jocf.sporttrack.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String loginPage() {
        return "login"; 
    }

    @GetMapping("/register") 
    public String registerPage() {
        return "/create/register";
    }
}