package br.com.brisabr.helpdesk_api.ticket;

import br.com.brisabr.helpdesk_api.dto.DashboardStatsDTO;
import br.com.brisabr.helpdesk_api.user.User;
import br.com.brisabr.helpdesk_api.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TicketService {

    @Autowired private TicketRepository ticketRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private ProblemaRepository problemaRepository;
    @Autowired private AnexoRepository anexoRepository;
    @Autowired private HistoricoRepository historicoRepository;

    @Transactional(readOnly = true)
    public List<TicketResponseDTO> getAllTickets(java.time.LocalDate dataInicial, java.time.LocalDate dataFinal, String tipoData,
            String status, String categoria, String unidade, String local,
            Long solicitanteId, Long tecnicoId, 
            String termoBusca, String tipoBusca, 
            User user) {
       
        User userAtualizado = userRepository.findById(user.getId()).orElse(user);
        Specification<Ticket> spec = TicketSpecification.withFilters(
                dataInicial, dataFinal, tipoData, status, categoria, unidade, local, 
                solicitanteId, tecnicoId, termoBusca, tipoBusca, userAtualizado);
        return ticketRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "id")).stream()
                .map(TicketResponseDTO::new).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TicketResponseDTO> getTicketsByLoggedUser(User user) {
        User userAtualizado = userRepository.findById(user.getId()).orElse(user);
        return ticketRepository.findBySolicitanteLoginIgnoreCase(userAtualizado.getLogin()).stream()
                .sorted(Comparator.comparing(Ticket::getId).reversed())
                .map(TicketResponseDTO::new).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TicketResponseDTO findTicketById(Long id) {
        return new TicketResponseDTO(ticketRepository.findById(id).orElseThrow());
    }

    @Transactional(readOnly = true)
    public DashboardStatsDTO getDashboardStats(Long equipeId) {
        DashboardStatsDTO stats = new DashboardStatsDTO();
        List<Ticket> ticketsRelevantes;

        if (equipeId != null && equipeId > 0) {
             // CORREÇÃO: Filtragem por equipe deve considerar o TÉCNICO, não o Solicitante.
             List<Ticket> all = ticketRepository.findAll();
             ticketsRelevantes = all.stream().filter(t -> {
                 // Verifica se o chamado tem técnico e se o técnico pertence à equipe filtrada
                 return t.getTecnicoAtribuido() != null 
                        && t.getTecnicoAtribuido().getEquipe() != null 
                        && t.getTecnicoAtribuido().getEquipe().getId().equals(equipeId);
             }).collect(Collectors.toList());
        } else {
            ticketsRelevantes = ticketRepository.findAll();
        }

        stats.setAbertos(ticketsRelevantes.stream().filter(t -> "Aberto".equalsIgnoreCase(t.getStatus())).count());
        stats.setEmAndamento(ticketsRelevantes.stream().filter(t -> "Em Andamento".equalsIgnoreCase(t.getStatus())).count());
        stats.setFechados(ticketsRelevantes.stream().filter(t -> List.of("Resolvido", "Fechado", "Encerrado").contains(t.getStatus())).count());
        stats.setTotal(ticketsRelevantes.stream()
                .filter(t -> !List.of("Resolvido", "Fechado", "Encerrado").contains(t.getStatus()))
                .count());

        List<Ticket> ticketsSlaViolado = ticketsRelevantes.stream()
            .filter(t -> !"Fechado".equals(t.getStatus()) && !"Resolvido".equals(t.getStatus()) && !"Encerrado".equals(t.getStatus()))
            .filter(this::isSlaViolado)
            .collect(Collectors.toList());
        stats.setSlaViolado(ticketsSlaViolado.size());
        
        stats.setChamadosSlaViolado(ticketsSlaViolado.stream()
            .map(TicketResponseDTO::new)
            .collect(Collectors.toList()));

        Map<String, Long> porAnalista = new HashMap<>();
        ticketsRelevantes.stream()
            .filter(t -> "Em Andamento".equals(t.getStatus()) && t.getTecnicoAtribuido() != null)
            .forEach(t -> {
                String nome = t.getTecnicoAtribuido().getNome();
                porAnalista.put(nome, porAnalista.getOrDefault(nome, 0L) + 1);
            });
        stats.setChamadosPorAnalista(porAnalista);

        return stats;
    }

    @Transactional(readOnly = true)
    public DashboardStatsDTO getDashboardStats() {
        return getDashboardStats(null);
    }

    private boolean isSlaViolado(Ticket t) {
        if (t.getDataAbertura() == null) return false;
        int horasLimite = 48;
        String p = t.getPrioridade() != null ? t.getPrioridade() : "Média";
        switch (p) {
            case "Crítica": horasLimite = 2; break;
            case "Elevada": horasLimite = 8; break;
            case "Média": horasLimite = 24; break;
            default: horasLimite = 48;
        }
        LocalDateTime deadline = t.getDataAbertura().plusHours(horasLimite);
        return LocalDateTime.now().isAfter(deadline);
    }

    @Transactional
    public synchronized Ticket createTicket(TicketCreateDTO dto, User solicitante, List<MultipartFile> files) throws IOException {
        Ticket ticket = new Ticket();
        ticket.setSolicitante(solicitante);
        ticket.setDescricao(dto.getDescricao());
        ticket.setStatus("Aberto");
        ticket.setDataAbertura(LocalDateTime.now());
        ticket.setNumeroChamado(gerarProximoNumeroChamado());
        
        if (dto.getIdCategoria() != null) categoriaRepository.findById(dto.getIdCategoria()).ifPresent(ticket::setCategoria);
        if (dto.getIdProblema() != null) problemaRepository.findById(dto.getIdProblema()).ifPresent(ticket::setProblema);

        ticket.setPrioridade("Média");
        if(ticket.getProblema() != null && ticket.getProblema().getPrioridadePadrao() != null) ticket.setPrioridade(ticket.getProblema().getPrioridadePadrao());
        else if (dto.getPrioridade() != null) ticket.setPrioridade(dto.getPrioridade());

        Ticket saved = ticketRepository.save(ticket);
        registrarHistorico(saved, solicitante, "Chamado Aberto");
        if (files != null) for (MultipartFile f : files) if(!f.isEmpty()) saveAttachment(saved, f);
        return saved;
    }
    
    private String gerarProximoNumeroChamado() {
        int anoAtual = java.time.LocalDate.now().getYear();
        String prefixo = anoAtual + "-";
        Optional<Ticket> ultimoTicket = ticketRepository.findTopByNumeroChamadoStartingWithOrderByIdDesc(prefixo);
        int proximoSequencial = 1;
        if (ultimoTicket.isPresent()) {
            try {
                String[] partes = ultimoTicket.get().getNumeroChamado().split("-");
                if (partes.length == 2) proximoSequencial = Integer.parseInt(partes[1]) + 1;
            } catch (Exception e) { proximoSequencial = 1;
            }
        }
        return prefixo + String.format("%03d", proximoSequencial);
    }
    
    @Transactional
    public TicketResponseDTO assignTicketToSelf(Long id, User user) {
        Ticket ticket = ticketRepository.findById(id).orElseThrow();
        ticket.setTecnicoAtribuido(user);
        ticket.setStatus("Em Andamento");
        registrarHistorico(ticket, user, "Chamado capturado");
        return new TicketResponseDTO(ticketRepository.save(ticket));
    }

    @Transactional
    public TicketResponseDTO assignTicketToTechnician(Long tId, Long techId, User user) {
        Ticket ticket = ticketRepository.findById(tId).orElseThrow();
        User tecnico = userRepository.findById(techId).orElseThrow();
        ticket.setTecnicoAtribuido(tecnico);
        ticket.setStatus("Em Andamento");
        registrarHistorico(ticket, user, "Atribuído para: " + tecnico.getNome());
        return new TicketResponseDTO(ticketRepository.save(ticket));
    }
    
    @Transactional
    public TicketResponseDTO closeTicket(Long id, CloseTicketDTO data, User user) {
        Ticket ticket = ticketRepository.findById(id).orElseThrow();
        ticket.setStatus("Fechado");
        ticket.setSolucao(data.getSolucao());
        ticket.setDataFechamento(LocalDateTime.now());
        registrarHistoricoComentario(ticket, user, "Fechado", "Solução: " + data.getSolucao());
        return new TicketResponseDTO(ticketRepository.save(ticket));
    }

    @Transactional
    public TicketResponseDTO reopenTicket(Long id, TicketReopenDTO data, User user) {
        Ticket ticket = ticketRepository.findById(id).orElseThrow();
        ticket.setStatus("Aberto");
        ticket.setDataFechamento(null);
        ticket.setFoiReaberto(true);
        registrarHistoricoComentario(ticket, user, "Reaberto", "Motivo: " + data.getMotivo());
        return new TicketResponseDTO(ticketRepository.save(ticket));
    }
    
    private void saveAttachment(Ticket t, MultipartFile f) throws IOException {
        Anexo a = new Anexo();
        a.setTicket(t);
        a.setNomeArquivo(f.getOriginalFilename());
        a.setTipoArquivo(f.getContentType());
        a.setDados(Base64.getEncoder().encodeToString(f.getBytes()));
        anexoRepository.save(a);
    }

    private Historico registrarHistorico(Ticket t, User u, String acao) { 
        return registrarHistoricoComentario(t, u, acao, acao);
    }

    private Historico registrarHistoricoComentario(Ticket t, User u, String acao, String comentario) {
        Historico h = new Historico();
        h.setTicket(t); 
        h.setUsuario(u); 
        h.setAcao(acao); 
        h.setComentario(comentario);
        h.setDataHora(LocalDateTime.now()); 
        return historicoRepository.save(h);
    }

    @Transactional(readOnly=true) public Anexo getAnexoById(Long id) { return anexoRepository.findById(id).orElseThrow();
    }
    
    @Transactional public TicketResponseDTO updateTicketClassification(Long id, String c, String p, User u) {
        Ticket t = ticketRepository.findById(id).orElseThrow();
        if(c!=null && !c.isEmpty()) categoriaRepository.findByNome(c).ifPresent(t::setCategoria);
        if(p!=null) t.setPrioridade(p);
        registrarHistorico(ticketRepository.save(t), u, "Classificação: "+p);
        return new TicketResponseDTO(t);
    }
    
    @Transactional public TicketResponseDTO addAttachment(Long id, MultipartFile f, User u) throws IOException {
        Ticket t = ticketRepository.findById(id).orElseThrow();
        saveAttachment(t, f);
        registrarHistorico(t, u, "Anexo: "+f.getOriginalFilename());
        return new TicketResponseDTO(ticketRepository.save(t));
    }
    
    @Transactional public HistoricoItemDTO addComment(Long id, CommentCreateDTO d, User u) {
        return new HistoricoItemDTO(registrarHistoricoComentario(ticketRepository.findById(id).orElseThrow(), u, "Comentário", d.getComentario()));
    }
}