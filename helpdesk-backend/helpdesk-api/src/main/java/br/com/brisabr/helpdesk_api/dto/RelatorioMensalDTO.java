package br.com.brisabr.helpdesk_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RelatorioMensalDTO {
    private Integer mes; // Frontend precisa do número (1 a 12)
    private Long totalChamados; // Frontend espera "totalChamados"
}