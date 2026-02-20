package br.com.brisabr.helpdesk_api.controller;

import br.com.brisabr.helpdesk_api.dto.DashboardStatsDTO;
import br.com.brisabr.helpdesk_api.ticket.TicketService;
import br.com.brisabr.helpdesk_api.user.User;
import br.com.brisabr.helpdesk_api.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private TicketService ticketService;
    @Autowired
    private UserRepository userRepository;

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'GESTOR')")
    public ResponseEntity<DashboardStatsDTO> getDashboardStats(@RequestParam(required = false) Long equipeId) {
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String login = auth.getName();
        Optional<User> userOpt = userRepository.findByLogin(login);

        // Inicializa com o parâmetro recebido (pode ser null se for Admin vendo tudo)
        Long idParaBusca = (equipeId != null && equipeId > 0) ? equipeId : null;

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String perfil = user.getPerfil() != null ? user.getPerfil().toLowerCase() : "";

            // REGRA: Gestor visualiza SOMENTE os dados da sua equipe.
            // Ignora o parâmetro 'equipeId' e força o ID da equipe do usuário logado.
            if (perfil.contains("manager") || perfil.contains("gestor")) {
                if (user.getEquipe() != null) {
                    idParaBusca = user.getEquipe().getId();
                } else {
                    // Gestor sem equipe vinculada retorna painel zerado por segurança
                    return ResponseEntity.ok(new DashboardStatsDTO());
                }
            }
            // Se for Administrador, mantém o idParaBusca vindo do parâmetro ou null (Todas as Equipes)
        }

        return ResponseEntity.ok(ticketService.getDashboardStats(idParaBusca));
    }
}