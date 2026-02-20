package br.com.brisabr.helpdesk_api.controller;

import br.com.brisabr.helpdesk_api.ticket.TicketRepository;
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

import java.util.*;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private UserRepository userRepository;

    private Long resolverEquipeId(Long equipeIdParam) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String login = auth.getName();
        Optional<User> userOpt = userRepository.findByLogin(login);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String perfil = user.getPerfil().toLowerCase();

            // Se for Gestor/Tecnico, força a equipe dele
            if (perfil.equals("gestor") || perfil.equals("manager") || perfil.equals("tecnico")) {
                if (user.getEquipe() != null) {
                    return user.getEquipe().getId();
                } else {
                    return -1L; // Sem equipe, não vê nada
                }
            }
        }
        // Se for Admin e não enviou equipe, retorna null (para ver tudo)
        return (equipeIdParam != null) ? equipeIdParam : null;
    }

    @GetMapping("/analistas")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'GESTOR', 'TECNICO')")
    public ResponseEntity<List<Map<String, Object>>> getChamadosPorAnalista(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Long equipeId) {

        Long idEquipeFinal = resolverEquipeId(equipeId);
        int ano = (year != null) ? year : Calendar.getInstance().get(Calendar.YEAR);
        int mes = (month != null) ? month : (Calendar.getInstance().get(Calendar.MONTH) + 1);

        // DEBUG VISUAL NO CONSOLE
        System.out.println("\n\n=== RELATORIO ANALISTAS ===");
        System.out.println("Filtros -> Ano: " + ano + " | Mes: " + mes + " | EquipeID: " + idEquipeFinal);

        List<Object[]> results = ticketRepository.getChamadosPorAnalista(ano, mes, idEquipeFinal);
        
        System.out.println("Registros Encontrados: " + results.size());
        System.out.println("===========================\n");

        List<Map<String, Object>> response = new ArrayList<>();
        for (Object[] row : results) {
            Map<String, Object> item = new HashMap<>();
            item.put("nomeAnalista", row[0]);
            item.put("totalChamados", row[1]);
            response.add(item);
        }
        return ResponseEntity.ok(response);
    }
    
    // Mantenha os outros métodos (categorias, mensal) com a lógica similar de resolverEquipeId
    @GetMapping("/categorias")
    public ResponseEntity<List<Map<String, Object>>> getTempoMedio(@RequestParam(required=false) Integer year, @RequestParam(required=false) Integer month, @RequestParam(required=false) Long equipeId) {
        Long id = resolverEquipeId(equipeId);
        int ano = (year != null) ? year : 2025;
        int mes = (month != null) ? month : 12;
        List<Object[]> res = ticketRepository.getTempoMedioPorCategoria(ano, mes, id);
        List<Map<String, Object>> list = new ArrayList<>();
        for(Object[] o : res) { Map<String,Object> m=new HashMap<>(); m.put("categoria", o[0]); m.put("tempoMedioHoras", o[1]); list.add(m); }
        return ResponseEntity.ok(list);
    }

    @GetMapping("/mensal")
    public ResponseEntity<List<Map<String, Object>>> getMensal(@RequestParam(required=false) Integer year, @RequestParam(required=false) Long equipeId) {
        Long id = resolverEquipeId(equipeId);
        int ano = (year != null) ? year : 2025;
        List<Object[]> res = ticketRepository.getChamadosPorMes(ano, id);
        List<Map<String, Object>> list = new ArrayList<>();
        for(Object[] o : res) { Map<String,Object> m=new HashMap<>(); m.put("mes", o[0]); m.put("totalChamados", o[1]); list.add(m); }
        return ResponseEntity.ok(list);
    }
}