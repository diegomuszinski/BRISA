package br.com.brisabr.helpdesk_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RelatorioCategoriaDTO {
    private String categoria;
    private Double tempoMedioHoras;
}