package br.com.brisabr.helpdesk_api.user;

import br.com.brisabr.helpdesk_api.ticket.Equipe;
import br.com.brisabr.helpdesk_api.ticket.EquipeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EquipeRepository equipeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User createUser(UserCreateDTO data) {
        // Verifica duplicidade ignorando case
        if (userRepository.findByLoginIgnoreCase(data.getLogin()).isPresent()) {
            throw new RuntimeException("Login já existe.");
        }

        User user = new User();
        user.setNome(data.getNome());
        
        // Padroniza o login para minúsculo ao salvar (Boas práticas)
        user.setLogin(data.getLogin().toLowerCase());
        
        user.setEmail(data.getEmail());
        
        // Criptografa a senha (vai para a coluna 'senha' graças ao mapeamento no User.java)
        user.setPassword(passwordEncoder.encode(data.getPassword()));
        
        user.setPerfil(data.getPerfil());

        if (data.getEquipeId() != null) {
            equipeRepository.findById(data.getEquipeId()).ifPresent(user::setEquipe);
        }

        return userRepository.save(user);
    }

    public List<Equipe> getAllEquipes() {
        return equipeRepository.findAll();
    }

    public List<User> findTechnicians() {
        return userRepository.findByPerfil("technician");
    }
}