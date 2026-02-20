package br.com.brisabr.helpdesk_api.auth;

import lombok.Data;

@Data
public class LoginDTO {
    private String login;
    private String senha;
}