package br.com.brisabr.helpdesk_api.ticket;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EquipeRepository extends JpaRepository<Equipe, Long> {
    boolean existsByNomeEquipe(String nomeEquipe);
}