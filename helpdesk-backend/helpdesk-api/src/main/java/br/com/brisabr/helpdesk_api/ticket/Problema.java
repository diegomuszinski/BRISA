package br.com.brisabr.helpdesk_api.ticket;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "problemas")
@Data
@NoArgsConstructor
public class Problema {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String nome;

    @Column(name = "prioridade_padrao", nullable = false)
    private String prioridadePadrao;
}