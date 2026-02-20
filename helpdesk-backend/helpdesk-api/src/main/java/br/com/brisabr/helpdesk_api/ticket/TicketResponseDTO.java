package br.com.brisabr.helpdesk_api.ticket;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
public class TicketResponseDTO {
    private Long id;
    private String numeroChamado;
    private String descricao;
    private String status;
    private String prioridade;
    
    private SimpleObjDTO categoria;
    private SimpleObjDTO problema;
    private SimpleObjDTO solicitante;
    
    private SimpleObjDTO tecnico;          
    private SimpleObjDTO tecnicoAtribuido;
    private LocalDateTime dataAbertura;
    private LocalDateTime dataFechamento;
    private String solucao;
    private boolean foiReaberto;
    
    private List<AnexoDTO> anexos = new ArrayList<>();
    private List<HistoricoDTO> historico = new ArrayList<>();

    public TicketResponseDTO(Ticket t) {
        this.id = t.getId();
        this.numeroChamado = t.getNumeroChamado();
        this.descricao = t.getDescricao();
        
        if (t.getStatus() != null && (t.getStatus().equalsIgnoreCase("Resolvido") || t.getStatus().equalsIgnoreCase("Encerrado"))) {
            this.status = "Fechado";
        } else {
            this.status = t.getStatus();
        }

        this.prioridade = t.getPrioridade();
        
        this.categoria = (t.getCategoria() != null) 
            ? new SimpleObjDTO(t.getCategoria().getId(), t.getCategoria().getNome(), null)
            : new SimpleObjDTO(0L, "Sem Categoria", null);

        this.problema = (t.getProblema() != null)
            ? new SimpleObjDTO(t.getProblema().getId(), t.getProblema().getNome(), null)
            : new SimpleObjDTO(0L, "Outros", null);

        Long equipeSolicitanteId = (t.getSolicitante() != null && t.getSolicitante().getEquipe() != null) 
                                    ? t.getSolicitante().getEquipe().getId() : null;

        this.solicitante = (t.getSolicitante() != null)
            ? new SimpleObjDTO(t.getSolicitante().getId(), t.getSolicitante().getNome(), equipeSolicitanteId)
            : new SimpleObjDTO(0L, "Desconhecido", null);

        if (t.getTecnicoAtribuido() != null) {
            Long equipeTecnicoId = (t.getTecnicoAtribuido().getEquipe() != null) ? t.getTecnicoAtribuido().getEquipe().getId() : null;
            SimpleObjDTO tech = new SimpleObjDTO(t.getTecnicoAtribuido().getId(), t.getTecnicoAtribuido().getNome(), equipeTecnicoId);
            this.tecnico = tech;
            this.tecnicoAtribuido = tech;
        } else {
            SimpleObjDTO pendente = new SimpleObjDTO(null, "Pendente", null);
            this.tecnico = pendente;
            this.tecnicoAtribuido = pendente;
        }
        
        this.dataAbertura = t.getDataAbertura();
        this.dataFechamento = t.getDataFechamento();
        this.solucao = t.getSolucao();
        this.foiReaberto = t.isFoiReaberto();

        if (t.getAnexos() != null) {
            this.anexos = t.getAnexos().stream().map(AnexoDTO::new).collect(Collectors.toList());
        }
        
        if (t.getHistorico() != null) {
            this.historico = t.getHistorico().stream().map(HistoricoDTO::new).collect(Collectors.toList());
        }
    }

    @Data
    public static class SimpleObjDTO {
        private Long id;
        private String nome;
        private Long equipeId;

        public SimpleObjDTO(Long id, String nome, Long equipeId) { 
            this.id = id;
            this.nome = nome; 
            this.equipeId = equipeId;
        }
    }

    @Data
    public static class AnexoDTO {
        private Long id;
        private String nomeArquivo;
        public AnexoDTO(Anexo a) { this.id = a.getId(); this.nomeArquivo = a.getNomeArquivo(); }
    }

    @Data
    public static class HistoricoDTO {
        private String acao;
        private String nomeUsuario;
        private LocalDateTime dataHora;
        private String comentario;
        public HistoricoDTO(Historico h) {
            this.acao = h.getAcao();
            this.nomeUsuario = h.getUsuario() != null ? h.getUsuario().getNome() : "Sistema";
            this.dataHora = h.getDataHora();
            this.comentario = h.getComentario();
        }
    }
}