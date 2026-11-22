package com.tollplaza.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class ApiInfoController {

    @GetMapping("/")
    public Map<String, String> home() {
        return Map.of(
                "message", "Toll Plaza API is running",
                "endpoint", "/api/v1/toll-plazas",
                "method", "POST");
    }
}
