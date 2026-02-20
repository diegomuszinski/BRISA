package br.com.brisabr.helpdesk_api.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication; 
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    // Regex de Senha Forte
    private static final String PASSWORD_PATTERN = "^(?=.*[0-9])(?=.*[a-zA-Z])(?=.*[^a-zA-Z0-9]).{8,}$";

    @GetMapping
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @GetMapping("/technicians")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_TECHNICIAN', 'admin', 'manager', 'technician', 'ADMIN', 'MANAGER', 'TECHNICIAN')")
    public List<User> getTechnicians() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String login = auth.getName();
        
        Optional<User> userOpt = userRepository.findByLogin(login);
        
        if (userOpt.isPresent()) {
            User currentUser = userOpt.get();
            String perfil = currentUser.getPerfil().toLowerCase();

            if (perfil.equals("manager") || perfil.equals("gestor")) {
                if (currentUser.getEquipe() != null) {
                    return userRepository.findTechniciansByEquipeId(currentUser.getEquipe().getId());
                } else {
                    return List.of();
                }
            }
        }
        return userRepository.findAllTechnicians();
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody User user) {
        if (userRepository.findByLogin(user.getLogin()).isPresent()) {
            return ResponseEntity.badRequest().body("Login já existe");
        }
        
        if (user.getPassword() == null || !user.getPassword().matches(PASSWORD_PATTERN)) {
            return ResponseEntity.badRequest().body("A senha deve ter no mínimo 8 caracteres, contendo letras, números e caracteres especiais.");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setPrimeiroAcesso(true); // Criação sempre força troca
        
        return ResponseEntity.ok(userRepository.save(user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody User updatedUser) {
        return userRepository.findById(id).map(user -> {
            user.setNome(updatedUser.getNome());
            user.setEmail(updatedUser.getEmail());
            user.setLogin(updatedUser.getLogin());
            user.setPerfil(updatedUser.getPerfil());
            user.setEquipe(updatedUser.getEquipe());

            // Se o admin mandou senha no update geral, força reset
            if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
                if (!updatedUser.getPassword().matches(PASSWORD_PATTERN)) {
                    throw new RuntimeException("A senha não atende aos requisitos de segurança.");
                }
                user.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
                user.setPrimeiroAcesso(true); 
            }
            
            return ResponseEntity.ok(userRepository.save(user));
        }).orElse(ResponseEntity.notFound().build());
    }

    // --- AÇÃO DO ADMINISTRADOR (RESETAR SENHA) ---
    @PutMapping("/{id}/change-password")
    public ResponseEntity<?> changePassword(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String newPassword = payload.get("password");
        
        if (newPassword == null || !newPassword.matches(PASSWORD_PATTERN)) {
            return ResponseEntity.badRequest().body("A senha deve ter no mínimo 8 caracteres, contendo letras, números e caracteres especiais.");
        }

        return userRepository.findById(id).map(user -> {
            user.setPassword(passwordEncoder.encode(newPassword));
            // AQUI ESTAVA O ERRO: Se o admin muda, o usuário OBRIGATORIAMENTE troca depois.
            user.setPrimeiroAcesso(true); 
            return ResponseEntity.ok(userRepository.save(user));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}