package br.com.brisabr.helpdesk_api.ticket;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    // Método necessário para buscar categoria pelo texto
    Optional<Categoria> findByNome(String nome);
}