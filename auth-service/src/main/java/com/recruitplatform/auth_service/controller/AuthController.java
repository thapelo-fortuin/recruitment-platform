package com.recruitplatform.auth_service.controller;
import com.recruitplatform.auth_service.soc.SocEvent;
import com.recruitplatform.auth_service.soc.SocEventClient;
import jakarta.servlet.http.HttpServletRequest;
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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
@RestController @RequestMapping("/api/v1/auth")
public class AuthController {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final SocEventClient socClient;
    private final String adminSetupKey;
    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil, SocEventClient socClient,
                          @Value("${app.admin-setup-key:}") String adminSetupKey) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.socClient = socClient;
        this.adminSetupKey = adminSetupKey;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        String role = request.getRole() == null ? "CANDIDATE" : request.getRole().toUpperCase();
        if (!role.equals("CANDIDATE") && !role.equals("RECRUITER")) {
            return ResponseEntity.badRequest().body("Only candidate or recruiter accounts can be registered here");
        }
        if (userRepository.existsByEmail(request.getEmail()))
        { return ResponseEntity.badRequest().body("Email already registered"); }
        User user = new User(); user.setEmail(request.getEmail()); user.setPasswordHash(passwordEncoder.encode(request.getPassword())); user.setName(request.getName()); user.setRole(role); userRepository.save(user);
        return ResponseEntity.ok("User registered successfully"); }

    public record AdminSetupRequest(@NotBlank String setupKey, @NotBlank String name,
                                    @Email @NotBlank String email, @NotBlank @Size(min = 8) String password) {}

    @PostMapping("/admin-setup")
    public synchronized ResponseEntity<?> setupAdmin(@Valid @RequestBody AdminSetupRequest request) {
        if (adminSetupKey.isBlank()) {
            return ResponseEntity.status(503).body("Administrator setup is not enabled on this server");
        }
        if (userRepository.existsByRole("ADMIN")) {
            return ResponseEntity.status(409).body("The initial administrator has already been created");
        }
        boolean keyMatches = MessageDigest.isEqual(
                adminSetupKey.trim().getBytes(StandardCharsets.UTF_8),
                request.setupKey().trim().getBytes(StandardCharsets.UTF_8));
        if (!keyMatches) return ResponseEntity.status(403).body("Invalid administrator setup key");
        if (userRepository.existsByEmail(request.email())) {
            return ResponseEntity.badRequest().body("Email already registered");
        }
        User user = new User();
        user.setEmail(request.email());
        user.setName(request.name());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole("ADMIN");
        userRepository.save(user);
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole());
        return ResponseEntity.ok(Map.of("token", token, "email", user.getEmail(),
                "name", user.getName(), "role", user.getRole()));
    }
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request,
                                   HttpServletRequest httpRequest) {
        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());

        if (userOpt.isEmpty()
                || !passwordEncoder.matches(request.getPassword(), userOpt.get().getPasswordHash())) {
            reportLogin(httpRequest, request.getEmail(), "LOGIN_FAILED", "MEDIUM", 401,
                    null, "Invalid email or password");
            return ResponseEntity.status(401).body("Invalid email or password");
        }

        User user = userOpt.get();
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole());
        reportLogin(httpRequest, user.getEmail(), "LOGIN_SUCCESS", "INFO", 200, user.getRole(), "Login successful");

        return ResponseEntity.ok(Map.of(
                "token", token,
                "email", user.getEmail(),
                "role", user.getRole(),
                "name", user.getName()));
    }

    private void reportLogin(HttpServletRequest http, String email, String eventType,
                             String severity, int status, String userRole, String message) {
        String ip = http.getHeader("X-Client-Ip");
        if (ip == null || ip.isBlank()) {
            ip = http.getRemoteAddr();
        }
        socClient.send(new SocEvent(
                null, "auth-service", eventType, severity, email, ip,
                "/api/v1/auth/login", "POST", status, message,
                http.getHeader("X-Correlation-Id"), "login-endpoint", userRole));
    }
    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestHeader("Authorization") String authHeader) { String token = authHeader.replace("Bearer ", ""); if (!jwtUtil.isTokenValid(token)) { return ResponseEntity.status(401).body("Invalid or expired token"); } return ResponseEntity.ok(Map.of( "email", jwtUtil.extractEmail(token), "role", jwtUtil.extractRole(token) )); } }
