package br.com.brisabr.helpdesk_api.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByLogin(String login);
    
    Optional<User> findByLoginIgnoreCase(String login);

    List<User> findByPerfil(String perfil);
    List<User> findByPerfilIgnoreCase(String perfil);

    // --- CORREÇÃO FINAL: Removemos 'admin' e 'gestor' desta lista ---
    // Agora busca APENAS quem tem perfil de técnico (em inglês ou português)
    @Query("SELECT u FROM User u WHERE TRIM(LOWER(u.perfil)) IN ('technician', 'tecnico', 'técnico') OR u.perfil LIKE '%ecnic%'")
    List<User> findAllTechnicians();

    // --- Busca técnicos por equipe (Já estava correto) ---
    @Query("SELECT u FROM User u WHERE u.equipe.id = :equipeId AND (TRIM(LOWER(u.perfil)) IN ('technician', 'tecnico', 'técnico') OR u.perfil LIKE '%ecnic%')")
    List<User> findTechniciansByEquipeId(@Param("equipeId") Long equipeId);
}