package br.com.brisabr.helpdesk_api.ticket;

import lombok.Data;

@Data
public class AnexoChamadoDTO {
    private Long id;
    private String nomeArquivo;
    private String tipoArquivo;

    public AnexoChamadoDTO(Anexo anexo) {
        this.id = anexo.getId();
        this.nomeArquivo = anexo.getNomeArquivo();
        this.tipoArquivo = anexo.getTipoArquivo();
    }
}