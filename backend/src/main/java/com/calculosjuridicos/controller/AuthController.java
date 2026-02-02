package com.calculosjuridicos.controller;

import com.calculosjuridicos.dto.request.LoginRequest;
import com.calculosjuridicos.dto.request.RefreshTokenRequest;
import com.calculosjuridicos.dto.request.RegistroRequest;
import com.calculosjuridicos.dto.response.AuthResponse;
import com.calculosjuridicos.dto.response.MessageResponse;
import com.calculosjuridicos.entity.Usuario;
import com.calculosjuridicos.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Endpoints de autenticação e registro")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Registrar novo usuário")
    public ResponseEntity<MessageResponse> registrar(@Valid @RequestBody RegistroRequest request) {
        MessageResponse response = authService.registrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Realizar login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        AuthResponse response = authService.login(request, httpRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renovar token de acesso")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @Operation(summary = "Obter dados do usuário autenticado")
    public ResponseEntity<AuthResponse.UsuarioResponse> getUsuarioAtual(
            @AuthenticationPrincipal Usuario usuario) {
        AuthResponse.UsuarioResponse response = authService.getUsuarioAtual(usuario);
        return ResponseEntity.ok(response);
    }
}
