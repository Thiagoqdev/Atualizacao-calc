package com.calculosjuridicos.service;

import com.calculosjuridicos.dto.request.LoginRequest;
import com.calculosjuridicos.dto.request.RefreshTokenRequest;
import com.calculosjuridicos.dto.request.RegistroRequest;
import com.calculosjuridicos.dto.response.AuthResponse;
import com.calculosjuridicos.dto.response.MessageResponse;
import com.calculosjuridicos.entity.LogAuditoria;
import com.calculosjuridicos.entity.Perfil;
import com.calculosjuridicos.entity.Usuario;
import com.calculosjuridicos.exception.BusinessException;
import com.calculosjuridicos.repository.LogAuditoriaRepository;
import com.calculosjuridicos.repository.PerfilRepository;
import com.calculosjuridicos.repository.UsuarioRepository;
import com.calculosjuridicos.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final LogAuditoriaRepository logAuditoriaRepository;

    @Transactional
    public MessageResponse registrar(RegistroRequest request) {
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email já está em uso", HttpStatus.CONFLICT);
        }

        Perfil perfilUser = perfilRepository.findByNome(Perfil.ROLE_USER)
            .orElseThrow(() -> new BusinessException("Perfil de usuário não encontrado"));

        Usuario usuario = Usuario.builder()
            .nomeCompleto(request.getNomeCompleto())
            .email(request.getEmail())
            .senhaHash(passwordEncoder.encode(request.getSenha()))
            .ativo(true)
            .build();

        usuario.addPerfil(perfilUser);
        usuarioRepository.save(usuario);

        log.info("Novo usuário registrado: {}", request.getEmail());

        return MessageResponse.success("Usuário registrado com sucesso");
    }

    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getSenha())
        );

        Usuario usuario = (Usuario) authentication.getPrincipal();

        String accessToken = tokenProvider.generateAccessToken(usuario);
        String refreshToken = tokenProvider.generateRefreshToken(usuario);

        // Log de auditoria
        LogAuditoria logAuditoria = LogAuditoria.builder()
            .usuarioId(usuario.getId())
            .acao(LogAuditoria.ACAO_LOGIN)
            .descricao("Login realizado com sucesso")
            .ip(getClientIp(httpRequest))
            .build();
        logAuditoriaRepository.save(logAuditoria);

        log.info("Login realizado: {}", usuario.getEmail());

        return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(tokenProvider.getAccessTokenExpiration() / 1000) // em segundos
            .usuario(AuthResponse.UsuarioResponse.builder()
                .id(usuario.getId())
                .nomeCompleto(usuario.getNomeCompleto())
                .email(usuario.getEmail())
                .build())
            .build();
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!tokenProvider.validateToken(refreshToken)) {
            throw new BusinessException("Refresh token inválido ou expirado", HttpStatus.UNAUTHORIZED);
        }

        String email = tokenProvider.getEmailFromToken(refreshToken);
        Usuario usuario = usuarioRepository.findByEmail(email)
            .orElseThrow(() -> new BusinessException("Usuário não encontrado", HttpStatus.NOT_FOUND));

        if (!usuario.getAtivo()) {
            throw new BusinessException("Usuário desativado", HttpStatus.FORBIDDEN);
        }

        String newAccessToken = tokenProvider.generateAccessToken(usuario);
        String newRefreshToken = tokenProvider.generateRefreshToken(usuario);

        return AuthResponse.builder()
            .accessToken(newAccessToken)
            .refreshToken(newRefreshToken)
            .tokenType("Bearer")
            .expiresIn(tokenProvider.getAccessTokenExpiration() / 1000)
            .usuario(AuthResponse.UsuarioResponse.builder()
                .id(usuario.getId())
                .nomeCompleto(usuario.getNomeCompleto())
                .email(usuario.getEmail())
                .build())
            .build();
    }

    public AuthResponse.UsuarioResponse getUsuarioAtual(Usuario usuario) {
        return AuthResponse.UsuarioResponse.builder()
            .id(usuario.getId())
            .nomeCompleto(usuario.getNomeCompleto())
            .email(usuario.getEmail())
            .build();
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
