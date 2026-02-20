package br.com.brisabr.helpdesk_api.ticket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

    @Autowired private CategoriaRepository categoriaRepository;

    @GetMapping
    public List<Categoria> getAll() { return categoriaRepository.findAll(); }

    @PostMapping
    public Categoria create(@RequestBody Categoria c) { return categoriaRepository.save(c); }

    // EDITAR
    @PutMapping("/{id}")
    public ResponseEntity<Categoria> update(@PathVariable Long id, @RequestBody Categoria updates) {
        return categoriaRepository.findById(id).map(c -> {
            c.setNome(updates.getNome());
            return ResponseEntity.ok(categoriaRepository.save(c));
        }).orElse(ResponseEntity.notFound().build());
    }

    // EXCLUIR
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (categoriaRepository.existsById(id)) {
            categoriaRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}