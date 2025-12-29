package com.lucero.backend.controllers;

import com.lucero.backend.models.Usuario;
import com.lucero.backend.repositories.UsuarioRepository;
import com.lucero.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime; // <--- IMPORTANTE: Necesario para la fecha
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody Map<String, String> request) {
        
        Usuario usuario = new Usuario();
        usuario.setNombre(request.get("nombre"));
        usuario.setEmail(request.get("email"));

        // Rol
        String rol = request.get("rol");
        if (rol == null || rol.isEmpty()) {
            usuario.setRol("usuario");
        } else {
            usuario.setRol(rol);
        }

        // Contraseña
        String rawPassword = request.get("password");
        if (rawPassword == null) rawPassword = request.get("passwordHash"); // Fallback
        if (rawPassword == null) throw new IllegalArgumentException("Password requerida");
        
        usuario.setPasswordHash(passwordEncoder.encode(rawPassword));

        // --- SOLUCIÓN DEL ERROR NUEVO ---
        // Asignamos la fecha actual y activamos el usuario
        usuario.setCreadoEn(LocalDateTime.now());
        usuario.setActivo(true); 
        // --------------------------------

        usuarioRepository.save(usuario);

        // Generar Token
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("rol", usuario.getRol());
        extraClaims.put("nombre", usuario.getNombre());

        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                usuario.getEmail(),
                usuario.getPasswordHash(),
                java.util.Collections.emptyList());

        String token = jwtService.generateToken(extraClaims, userDetails);

        Map<String, String> response = new HashMap<>();
        response.put("token", token);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.get("email"), request.get("password")));

        Usuario user = usuarioRepository.findByEmail(request.get("email")).orElseThrow();

        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("rol", user.getRol());
        extraClaims.put("nombre", user.getNombre());

        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPasswordHash(),
                java.util.Collections.emptyList());

        String token = jwtService.generateToken(extraClaims, userDetails);

        Map<String, String> response = new HashMap<>();
        response.put("token", token);
        return ResponseEntity.ok(response);
    }
}