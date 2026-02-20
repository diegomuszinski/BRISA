package br.com.brisabr.helpdesk_api.ticket;

import br.com.brisabr.helpdesk_api.user.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "historico_chamados")
@Getter
@Setter
@NoArgsConstructor
public class Historico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "id_chamado") // CORREÇÃO VITAL: O banco usa id_chamado
    @JsonIgnore
    private Ticket ticket;

    @ManyToOne
    @JoinColumn(name = "id_autor") // CORREÇÃO: O banco usa id_autor
    private User usuario;

    @Column(name = "data_ocorrencia")
    private LocalDateTime dataHora = LocalDateTime.now(); // Inicializa data

    private String acao; 
    
    @Column(columnDefinition = "TEXT")
    private String comentario; 
}