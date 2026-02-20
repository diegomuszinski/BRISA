package br.com.brisabr.helpdesk_api.ticket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Base64;

@RestController
@RequestMapping("/api/anexos")
public class AnexoChamadoController {

    @Autowired
    private TicketService ticketService;

    @GetMapping("/{id}")
    public ResponseEntity<ByteArrayResource> downloadAnexo(@PathVariable Long id) {
        Anexo anexo = ticketService.getAnexoById(id);
        
        // Decodifica o Base64 do banco para bytes reais
        byte[] data = Base64.getDecoder().decode(anexo.getDados());
        ByteArrayResource resource = new ByteArrayResource(data);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + anexo.getNomeArquivo() + "\"")
                .contentType(MediaType.parseMediaType(anexo.getTipoArquivo()))
                .contentLength(data.length)
                .body(resource);
    }
}