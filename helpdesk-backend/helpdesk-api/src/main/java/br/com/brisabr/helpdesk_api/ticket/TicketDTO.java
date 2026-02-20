package br.com.brisabr.helpdesk_api.ticket;

import lombok.Data;

@Data
public class TicketDTO {
    private String descricao;
    private Long idCategoria;
    private Long idProblema;
}