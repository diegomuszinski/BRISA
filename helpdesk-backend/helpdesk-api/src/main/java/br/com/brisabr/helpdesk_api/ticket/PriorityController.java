package br.com.brisabr.helpdesk_api.ticket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prioridades")
public class PriorityController {

    @Autowired
    private PriorityRepository priorityRepository;

    @GetMapping
    public ResponseEntity<List<Priority>> getAll() {
        return ResponseEntity.ok(priorityRepository.findAll());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('admin') or hasAuthority('manager')")
    public ResponseEntity<Priority> create(@RequestBody Priority priority) {
        return ResponseEntity.ok(priorityRepository.save(priority));
    }
}