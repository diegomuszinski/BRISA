package br.com.brisabr.helpdesk_api.user;

import br.com.brisabr.helpdesk_api.ticket.Equipe;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;
    
    @Column(unique = true)
    private String login;
    
    private String email;

    @Column(name = "senha") 
    private String password; 

    // Define se o usuário precisa trocar a senha (padrão: sim)
    @Column(name = "primeiro_acesso")
    private Boolean primeiroAcesso = true;

    private String perfil;   
    private LocalDateTime dataCriacao = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "id_equipe")
    private Equipe equipe;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (this.perfil == null) return List.of(new SimpleGrantedAuthority("ROLE_USER"));
        return List.of(new SimpleGrantedAuthority("ROLE_" + this.perfil.toUpperCase()));
    }

    @Override
    public String getUsername() { return login; }
    @Override
    public boolean isAccountNonExpired() { return true; }
    @Override
    public boolean isAccountNonLocked() { return true; }
    @Override
    public boolean isCredentialsNonExpired() { return true; }
    @Override
    public boolean isEnabled() { return true; }
}