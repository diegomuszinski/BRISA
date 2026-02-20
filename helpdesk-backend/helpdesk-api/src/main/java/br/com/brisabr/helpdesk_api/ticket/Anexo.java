package br.com.brisabr.helpdesk_api.ticket;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "anexos_chamados") // CORREÇÃO: Nome correto da tabela
@Data
@NoArgsConstructor
public class Anexo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome_arquivo")
    private String nomeArquivo;

    @Column(name = "tipo_arquivo")
    private String tipoArquivo;

    @Lob
    @Column(columnDefinition = "TEXT") 
    private String dados; 

    @Column(name = "data_upload")
    private LocalDateTime dataUpload = LocalDateTime.now(); 

    @ManyToOne
    @JoinColumn(name = "id_chamado") 
    @JsonIgnore
    private Ticket ticket;
}