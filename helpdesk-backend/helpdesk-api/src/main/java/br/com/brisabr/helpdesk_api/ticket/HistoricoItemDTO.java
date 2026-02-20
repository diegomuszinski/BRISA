package br.com.brisabr.helpdesk_api.ticket;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class HistoricoItemDTO {
    private String acao;
    private String nomeUsuario;
    private LocalDateTime dataHora;

    public HistoricoItemDTO(Historico historico) {
        this.acao = historico.getAcao();
        this.nomeUsuario = (historico.getUsuario() != null) ? historico.getUsuario().getNome() : "Sistema";
        this.dataHora = historico.getDataHora();
    }
}