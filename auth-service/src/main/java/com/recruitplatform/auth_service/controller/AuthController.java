package com.recruitplatform.auth_service.controller;
import com.recruitplatform.auth_service.dto.LoginRequest;
import com.recruitplatform.auth_service.dto.RegisterRequest;
import com.recruitplatform.auth_service.model.User;
import com.recruitplatform.auth_service.repository.UserRepository;
import com.recruitplatform.auth_service.util.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.Optional;
@RestController @RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) { this.userRepository = userRepository; this.passwordEncoder = passwordEncoder; this.jwtUtil = jwtUtil; }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail()))
        { return ResponseEntity.badRequest().body("Email already registered"); }
        User user = new User(); user.setEmail(request.getEmail()); user.setPasswordHash(passwordEncoder.encode(request.getPassword())); user.setName(request.getName()); user.setRole(request.getRole().toUpperCase()); userRepository.save(user);
        return ResponseEntity.ok("User registered successfully"); }
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) { Optional<User> userOpt = userRepository.findByEmail(request.getEmail());
        if (userOpt.isEmpty() || !passwordEncoder.matches(request.getPassword(), userOpt.get().getPasswordHash())) { return ResponseEntity.status(401).body("Invalid email or password"); } User user = userOpt.get(); String token = jwtUtil.generateToken(user.getEmail(), user.getRole());
        return ResponseEntity.ok(Map.of( "token", token, "email", user.getEmail(), "role", user.getRole(), "name", user.getName() )); }
    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestHeader("Authorization") String authHeader) { String token = authHeader.replace("Bearer ", ""); if (!jwtUtil.isTokenValid(token)) { return ResponseEntity.status(401).body("Invalid or expired token"); } return ResponseEntity.ok(Map.of( "email", jwtUtil.extractEmail(token), "role", jwtUtil.extractRole(token) )); } }