package br.com.brisabr.helpdesk_api.ticket;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TicketCreateDTO {
    // Estes nomes devem bater com o JSON do Frontend (idCategoria, idProblema)
    private String descricao;  
    private Long idCategoria;
    private Long idProblema;
    private String prioridade; // Opcional
}