package br.com.brisabr.helpdesk_api.ticket;

import br.com.brisabr.helpdesk_api.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    @Autowired
    private TicketService ticketService;

    // Rota Principal (Admin, Gestor, Técnico)
    @GetMapping
    public List<TicketResponseDTO> getAllTickets(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicial,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFinal,
            @RequestParam(required = false) String tipoData,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String unidade,
            @RequestParam(required = false) String local,
            @RequestParam(required = false) Long solicitanteId,
            @RequestParam(required = false) Long tecnicoId,
            // NOVOS FILTROS DE BUSCA
            @RequestParam(required = false) String search, 
            @RequestParam(required = false) String searchType,
            @AuthenticationPrincipal User user
    ) {
        return ticketService.getAllTickets(
                dataInicial, dataFinal, tipoData, status,
                categoria, unidade, local, solicitanteId, tecnicoId, 
                search, searchType, // Passando novos parametros
                user
        );
    }

    @GetMapping("/me")
    public List<TicketResponseDTO> getMyTickets(
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal User user
    ) {
        return ticketService.getAllTickets(
                null, null, null, status, 
                null, null, null, null, null, 
                null, null, // Sem busca na rota simplificada
                user
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketResponseDTO> getTicketById(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.findTicketById(id));
    }

    @PostMapping
    public ResponseEntity<Ticket> createTicket(
            @RequestPart("ticket") TicketCreateDTO dto,
            @RequestPart(value = "anexos", required = false) List<MultipartFile> files,
            @AuthenticationPrincipal User user
    ) throws IOException {
        Ticket created = ticketService.createTicket(dto, user, files);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}/classification")
    public ResponseEntity<TicketResponseDTO> updateTicketClassification(
            @PathVariable Long id,
            @RequestBody Map<String, String> updates,
            @AuthenticationPrincipal User user
    ) {
        String category = updates.get("category");
        String priority = updates.get("priority");
        return ResponseEntity.ok(ticketService.updateTicketClassification(id, category, priority, user));
    }

    @PostMapping("/{id}/attachments")
    public ResponseEntity<TicketResponseDTO> addAttachment(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User user
    ) throws IOException {
        return ResponseEntity.ok(ticketService.addAttachment(id, file, user));
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<HistoricoItemDTO> addComment(
            @PathVariable Long id,
            @RequestBody CommentCreateDTO data,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(ticketService.addComment(id, data, user));
    }

    @PostMapping("/{id}/assign-self")
    public ResponseEntity<TicketResponseDTO> assignToSelf(
            @PathVariable Long id,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(ticketService.assignTicketToSelf(id, user));
    }

    @PostMapping("/{id}/assign/{techId}")
    public ResponseEntity<TicketResponseDTO> assignToTechnician(
            @PathVariable Long id,
            @PathVariable Long techId,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(ticketService.assignTicketToTechnician(id, techId, user));
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<TicketResponseDTO> closeTicket(
            @PathVariable Long id,
            @RequestBody CloseTicketDTO data,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(ticketService.closeTicket(id, data, user));
    }

    @PostMapping("/{id}/reopen")
    public ResponseEntity<TicketResponseDTO> reopenTicket(
            @PathVariable Long id,
            @RequestBody TicketReopenDTO data,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(ticketService.reopenTicket(id, data, user));
    }
}