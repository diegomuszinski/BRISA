package br.com.brisabr.helpdesk_api.auth;

import br.com.brisabr.helpdesk_api.config.TokenService;
import br.com.brisabr.helpdesk_api.user.User;
import br.com.brisabr.helpdesk_api.user.UserRepository; 
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired private AuthenticationManager authenticationManager;
    @Autowired private TokenService tokenService;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    // Regex de Senha Forte
    private static final String PASSWORD_PATTERN = "^(?=.*[0-9])(?=.*[a-zA-Z])(?=.*[^a-zA-Z0-9]).{8,}$";

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginDTO data) {
        try {
            var usernamePassword = new UsernamePasswordAuthenticationToken(data.getLogin(), data.getSenha());
            Authentication auth = authenticationManager.authenticate(usernamePassword);

            User user = (User) auth.getPrincipal();
            String token = tokenService.generateToken(user);

            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("id", user.getId());
            response.put("primeiroAcesso", user.getPrimeiroAcesso());
            response.put("role", user.getPerfil());
            response.put("nome", user.getNome());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace(); 
            return ResponseEntity.status(403).body("Login ou senha inválidos");
        }
    }

    // --- AÇÃO DO USUÁRIO (PRIMEIRO ACESSO) ---
    // Este endpoint libera o usuário após ele trocar a própria senha
    @PutMapping("/change-password/{id}")
    public ResponseEntity<?> changePassword(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String newPassword = payload.get("password");
        
        if (newPassword == null || !newPassword.matches(PASSWORD_PATTERN)) {
            return ResponseEntity.badRequest().body("A senha deve ter no mínimo 8 caracteres, contendo letras, números e caracteres especiais.");
        }

        return userRepository.findById(id).map(user -> {
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setPrimeiroAcesso(false); // AQUI SIM, LIBERA O ACESSO
            return ResponseEntity.ok(userRepository.save(user));
        }).orElse(ResponseEntity.notFound().build());
    }
}