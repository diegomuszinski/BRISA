package br.com.brisabr.helpdesk_api.user;

import lombok.Data;

@Data
public class UserCreateDTO {
    private String nome;
    private String login;
    private String email;
    private String password;
    private String perfil;
    private Long equipeId;
}