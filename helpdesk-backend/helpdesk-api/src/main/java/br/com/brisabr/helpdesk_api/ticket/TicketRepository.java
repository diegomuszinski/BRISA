package br.com.brisabr.helpdesk_api.ticket;

import br.com.brisabr.helpdesk_api.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

    // --- Buscas Padrão ---
    List<Ticket> findBySolicitanteLoginIgnoreCase(String login);
    List<Ticket> findBySolicitanteLogin(String login);
    List<Ticket> findBySolicitanteId(Long solicitanteId);
    List<Ticket> findBySolicitante(User solicitante);
    Optional<Ticket> findTopByNumeroChamadoStartingWithOrderByIdDesc(String prefixo);

    @Query("SELECT t FROM Ticket t WHERE t.solicitante.equipe.id = :equipeId")
    List<Ticket> findByEquipeDoSolicitante(@Param("equipeId") Long equipeId);

    // --- CARDS DO DASHBOARD (Mantidos) ---
    long countByStatusNot(String status);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.solicitante.equipe.id = :equipeId AND t.status <> :status")
    long countAtivosByEquipe(@Param("equipeId") Long equipeId, @Param("status") String status);

    long countByStatus(String status);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.status = :status AND t.tecnicoAtribuido.equipe.id = :equipeId")
    long countByStatusAndEquipetecnico(@Param("status") String status, @Param("equipeId") Long equipeId);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.status = 'Aberto' AND t.solicitante.equipe.id = :equipeId")
    long countAbertosByEquipeSolicitante(@Param("equipeId") Long equipeId);

    // --- GRÁFICOS (CORRIGIDO PARA POSTGRESQL - EXTRACT INSTEAD OF YEAR/MONTH) ---
    
    // 1. Por Analista
    @Query(value = "SELECT u.nome AS nome, COUNT(t.id) AS quantidade " +
                   "FROM chamados t " +
                   "JOIN usuarios u ON t.id_tecnico_atribuido = u.id " +
                   "WHERE t.data_fechamento IS NOT NULL " +
                   "AND CAST(EXTRACT(YEAR FROM t.data_fechamento) AS INTEGER) = :ano " +
                   "AND CAST(EXTRACT(MONTH FROM t.data_fechamento) AS INTEGER) = :mes " +
                   "AND (:equipeId IS NULL OR :equipeId = -1 OR u.id_equipe = CAST(:equipeId AS BIGINT)) " +
                   "GROUP BY u.nome", nativeQuery = true)
    List<Object[]> getChamadosPorAnalista(@Param("ano") Integer ano, @Param("mes") Integer mes, @Param("equipeId") Long equipeId);

    // 2. Tempo Médio
    @Query(value = "SELECT c.nome AS categoria, AVG(EXTRACT(EPOCH FROM (t.data_fechamento - t.data_abertura)) / 3600.0) " +
                   "FROM chamados t " +
                   "JOIN categorias c ON t.id_categoria = c.id " +
                   "LEFT JOIN usuarios u ON t.id_tecnico_atribuido = u.id " +
                   "WHERE t.data_fechamento IS NOT NULL " +
                   "AND CAST(EXTRACT(YEAR FROM t.data_fechamento) AS INTEGER) = :ano " +
                   "AND CAST(EXTRACT(MONTH FROM t.data_fechamento) AS INTEGER) = :mes " +
                   "AND (:equipeId IS NULL OR :equipeId = -1 OR u.id_equipe = CAST(:equipeId AS BIGINT)) " +
                   "GROUP BY c.nome", nativeQuery = true)
    List<Object[]> getTempoMedioPorCategoria(@Param("ano") Integer ano, @Param("mes") Integer mes, @Param("equipeId") Long equipeId);

    // 3. Por Mês
    @Query(value = "SELECT CAST(EXTRACT(MONTH FROM t.data_abertura) AS INTEGER) AS mes, COUNT(t.id) AS quantidade " +
                   "FROM chamados t " +
                   "LEFT JOIN usuarios u ON t.id_tecnico_atribuido = u.id " +
                   "WHERE CAST(EXTRACT(YEAR FROM t.data_abertura) AS INTEGER) = :ano " +
                   "AND (:equipeId IS NULL OR :equipeId = -1 OR u.id_equipe = CAST(:equipeId AS BIGINT)) " +
                   "GROUP BY CAST(EXTRACT(MONTH FROM t.data_abertura) AS INTEGER)", nativeQuery = true)
    List<Object[]> getChamadosPorMes(@Param("ano") Integer ano, @Param("equipeId") Long equipeId);
}