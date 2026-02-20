package br.com.brisabr.helpdesk_api.ticket;

import br.com.brisabr.helpdesk_api.user.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "equipes")
@Getter
@Setter
@NoArgsConstructor
public class Equipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nomeEquipe;

    // Proteção contra Loop Infinito
    @OneToMany(mappedBy = "equipe")
    @JsonIgnore 
    private List<User> membros = new ArrayList<>();
}