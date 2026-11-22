package com.tollplaza.controller;

import com.tollplaza.dto.TollPlazaRequest;
import com.tollplaza.dto.TollPlazaResponse;
import com.tollplaza.service.TollPlazaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/toll-plazas")
public class TollPlazaController {

    private final TollPlazaService service;

    public TollPlazaController(TollPlazaService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<TollPlazaResponse> getTollPlazas(@Valid @RequestBody TollPlazaRequest request) {
        return ResponseEntity.ok(service.findTollPlazas(request.sourcePincode().trim(), request.destinationPincode().trim()));
    }
}
