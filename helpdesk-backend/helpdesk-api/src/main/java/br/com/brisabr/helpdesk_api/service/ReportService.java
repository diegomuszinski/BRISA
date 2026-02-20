package br.com.brisabr.helpdesk_api.service;

import br.com.brisabr.helpdesk_api.dto.RelatorioAnalistaDTO;
import br.com.brisabr.helpdesk_api.dto.RelatorioCategoriaDTO;
import br.com.brisabr.helpdesk_api.dto.RelatorioMensalDTO;
import br.com.brisabr.helpdesk_api.ticket.Ticket;
import br.com.brisabr.helpdesk_api.ticket.TicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReportService {

    @Autowired
    private TicketRepository ticketRepository;

    public List<RelatorioAnalistaDTO> getChamadosPorAnalista(Integer ano, Integer mes, Long equipeId) {
        if (ano == null) ano = LocalDate.now().getYear();
        if (mes == null) mes = LocalDate.now().getMonthValue();
        
        List<Object[]> results = ticketRepository.getChamadosPorAnalista(ano, mes, equipeId);
        List<RelatorioAnalistaDTO> dtos = new ArrayList<>();
        
        for (Object[] row : results) {
            String nome = (String) row[0];
            Number qtd = (Number) row[1]; // Cast seguro
            // Ajustado para o novo DTO (nome, totalChamados)
            dtos.add(new RelatorioAnalistaDTO(nome, qtd != null ? qtd.longValue() : 0L));
        }
        return dtos;
    }

    public List<RelatorioCategoriaDTO> getTempoMedioPorCategoria(Integer ano, Integer mes, Long equipeId) {
        if (ano == null) ano = LocalDate.now().getYear();
        if (mes == null) mes = LocalDate.now().getMonthValue();

        List<Object[]> results = ticketRepository.getTempoMedioPorCategoria(ano, mes, equipeId);
        List<RelatorioCategoriaDTO> dtos = new ArrayList<>();
        
        for (Object[] row : results) {
            String cat = (String) row[0];
            Number media = (Number) row[1];
            dtos.add(new RelatorioCategoriaDTO(cat, media != null ? media.doubleValue() : 0.0));
        }
        return dtos;
    }

    public List<RelatorioMensalDTO> getChamadosPorMes(Integer ano, Long equipeId) {
        if (ano == null) ano = LocalDate.now().getYear();
        List<Object[]> results = ticketRepository.getChamadosPorMes(ano, equipeId);
        List<RelatorioMensalDTO> dtos = new ArrayList<>();
        
        // Inicializa com zero para os 12 meses
        for(int i=1; i<=12; i++) {
            dtos.add(new RelatorioMensalDTO(i, 0L));
        }

        for (Object[] row : results) {
            Number mesIndexNum = (Number) row[0];
            Number qtd = (Number) row[1];
            
            if (mesIndexNum != null) {
                int mesIndex = mesIndexNum.intValue();
                // Ajusta índice (1=Janeiro -> array 0)
                if (mesIndex >= 1 && mesIndex <= 12) {
                    dtos.set(mesIndex - 1, new RelatorioMensalDTO(mesIndex, qtd != null ? qtd.longValue() : 0L));
                }
            }
        }
        return dtos;
    }

    public List<Ticket> getDetailedReport(LocalDate inicio, LocalDate fim, String status, String prioridade, 
                                          String categoria, String problema, String tecnico, 
                                          Long equipeId, Long solicitanteId, String solucao) {
        
        List<Ticket> todos = ticketRepository.findAll();
        
        return todos.stream()
            .filter(t -> inicio == null || !t.getDataAbertura().toLocalDate().isBefore(inicio))
            .filter(t -> fim == null || !t.getDataAbertura().toLocalDate().isAfter(fim))
            .filter(t -> status == null || status.isEmpty() || t.getStatus().equalsIgnoreCase(status))
            .filter(t -> prioridade == null || prioridade.isEmpty() || t.getPrioridade().equalsIgnoreCase(prioridade))
            .filter(t -> categoria == null || categoria.isEmpty() || (t.getCategoria() != null && t.getCategoria().getNome().equalsIgnoreCase(categoria)))
            // Filtro de equipe
            .filter(t -> equipeId == null || (t.getTecnicoAtribuido() != null && t.getTecnicoAtribuido().getEquipe() != null && t.getTecnicoAtribuido().getEquipe().getId().equals(equipeId)))
            .collect(Collectors.toList());
    }
}