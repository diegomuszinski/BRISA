package br.com.brisabr.helpdesk_api.ticket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/problemas")
public class ProblemaController {

    @Autowired private ProblemaRepository problemaRepository;

    @GetMapping
    public List<Problema> getAll() { return problemaRepository.findAll(); }

    @PostMapping
    public ResponseEntity<Problema> create(@RequestBody Map<String, String> payload) {
        String nome = payload.get("nome");
        String prioridade = payload.get("prioridadePadrao");
        if (prioridade == null || prioridade.trim().isEmpty()) prioridade = "Média"; // Regra padrão

        Problema p = new Problema();
        p.setNome(nome);
        p.setPrioridadePadrao(prioridade);
        return ResponseEntity.ok(problemaRepository.save(p));
    }

    // EDITAR
    @PutMapping("/{id}")
    public ResponseEntity<Problema> update(@PathVariable Long id, @RequestBody Problema updates) {
        return problemaRepository.findById(id).map(p -> {
            p.setNome(updates.getNome());
            if (updates.getPrioridadePadrao() != null) p.setPrioridadePadrao(updates.getPrioridadePadrao());
            return ResponseEntity.ok(problemaRepository.save(p));
        }).orElse(ResponseEntity.notFound().build());
    }

    // EXCLUIR
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (problemaRepository.existsById(id)) {
            problemaRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}