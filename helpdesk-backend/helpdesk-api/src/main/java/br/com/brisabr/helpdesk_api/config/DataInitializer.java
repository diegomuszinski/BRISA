package br.com.brisabr.helpdesk_api.config;

import br.com.brisabr.helpdesk_api.user.User;
import br.com.brisabr.helpdesk_api.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@Configuration
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // O application.properties (ddl-auto=create) já limpou o banco.
        // Agora só precisamos garantir que o ADMIN exista.
        createOrUpdateAdmin("admin", "Administrador", "admin@brisa.com", "admin");
    }

    private void createOrUpdateAdmin(String login, String nome, String email, String senhaCrua) {
        if (userRepository.findByLogin(login).isEmpty()) {
            User user = new User();
            user.setNome(nome);
            user.setLogin(login);
            user.setEmail(email);
            user.setPerfil("admin");
            user.setPassword(passwordEncoder.encode(senhaCrua));
            user.setDataCriacao(LocalDateTime.now());
            userRepository.save(user);
            System.out.println(">>> USUÁRIO ADMIN RECRIADO COM SUCESSO.");
        }
    }
}