package br.com.brisabr.helpdesk_api.user;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRegistrationDTO {
    private String nome;
    private String email;
    private String senha;
    private String perfil; 
}