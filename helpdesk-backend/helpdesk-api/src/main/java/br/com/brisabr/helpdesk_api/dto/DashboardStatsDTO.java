package br.com.brisabr.helpdesk_api.dto;

import br.com.brisabr.helpdesk_api.ticket.TicketResponseDTO; // Importante
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class DashboardStatsDTO {
    // Totais Gerais
    private long abertos;
    private long emAndamento;
    private long fechados;
    private long total;
    
    // KPIs
    private long slaViolado;
    
    // Mapas e Listas Detalhadas
    private Map<String, Long> chamadosPorAnalista;
    
    // NOVO: Lista detalhada para exibição na tela
    private List<TicketResponseDTO> chamadosSlaViolado;

    public DashboardStatsDTO() {
    }
}