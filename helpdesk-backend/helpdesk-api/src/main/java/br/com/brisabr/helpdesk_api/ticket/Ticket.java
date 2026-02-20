package br.com.brisabr.helpdesk_api.ticket;

import br.com.brisabr.helpdesk_api.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chamados") // CORREÇÃO: Nome da tabela alinhado com o banco
@Getter
@Setter
@NoArgsConstructor
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_chamado", unique = true, nullable = false)
    private String numeroChamado;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    private String status; 
    private String prioridade; 

    @ManyToOne
    @JoinColumn(name = "id_categoria")
    private Categoria categoria;

    @ManyToOne
    @JoinColumn(name = "id_problema")
    private Problema problema;

    @ManyToOne
    @JoinColumn(name = "id_solicitante")
    private User solicitante;

    @ManyToOne
    @JoinColumn(name = "id_tecnico_atribuido") // Nome da coluna alinhado
    private User tecnicoAtribuido;

    @Column(name = "data_abertura")
    private LocalDateTime dataAbertura;

    @Column(name = "data_fechamento")
    private LocalDateTime dataFechamento;

    @Column(columnDefinition = "TEXT")
    private String solucao;

    @Column(name = "foi_reaberto")
    private boolean foiReaberto = false;

    // Relacionamentos com listas
    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL)
    private List<Anexo> anexos = new ArrayList<>();

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL)
    private List<Historico> historico = new ArrayList<>();

    public static LocalDateTime calculateSlaDeadline(LocalDateTime dataAbertura, String prioridade) {
        if (dataAbertura == null) return null;
        int hoursToAdd = 48;
        if (prioridade != null) {
            switch (prioridade) {
                case "Crítica": hoursToAdd = 2; break;
                case "Elevada": hoursToAdd = 8; break;
                case "Média": hoursToAdd = 24; break;
                case "Baixa": hoursToAdd = 48; break;
            }
        }
        return dataAbertura.plusHours(hoursToAdd);
    }
}