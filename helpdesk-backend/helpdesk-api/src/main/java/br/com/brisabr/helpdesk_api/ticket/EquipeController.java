package br.com.brisabr.helpdesk_api.ticket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/equipes")
public class EquipeController {

    @Autowired private EquipeRepository equipeRepository;

    @GetMapping
    public List<Equipe> getAllEquipes() { return equipeRepository.findAll(); }

    @PostMapping
    public Equipe createEquipe(@RequestBody Equipe equipe) { return equipeRepository.save(equipe); }

    // EDITAR
    @PutMapping("/{id}")
    public ResponseEntity<Equipe> updateEquipe(@PathVariable Long id, @RequestBody Equipe updates) {
        return equipeRepository.findById(id).map(eq -> {
            eq.setNomeEquipe(updates.getNomeEquipe());
            return ResponseEntity.ok(equipeRepository.save(eq));
        }).orElse(ResponseEntity.notFound().build());
    }

    // EXCLUIR
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEquipe(@PathVariable Long id) {
        if (equipeRepository.existsById(id)) {
            equipeRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}