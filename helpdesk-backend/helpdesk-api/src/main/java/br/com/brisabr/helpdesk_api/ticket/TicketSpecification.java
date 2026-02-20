package br.com.brisabr.helpdesk_api.ticket;

import br.com.brisabr.helpdesk_api.user.User;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TicketSpecification {

    public static Specification<Ticket> withFilters(
            LocalDate dataInicial, LocalDate dataFinal, String tipoData,
            String status, String categoria, String unidade, String local,
            Long solicitanteId, Long tecnicoId,
            String termoBusca, String tipoBusca, // NOVOS CAMPOS DE BUSCA
            User userLogado) {

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Joins para acessar dados relacionados
            Join<Ticket, User> tecnicoJoin = root.join("tecnicoAtribuido", JoinType.LEFT);
            Join<Ticket, User> solicitanteJoin = root.join("solicitante", JoinType.LEFT);

            String loginLogado = userLogado.getLogin().toLowerCase();
            String perfil = userLogado.getPerfil() != null ? userLogado.getPerfil().toLowerCase() : "";

            // 1. REGRAS DE SEGURANÇA (PERFIS)
            if (perfil.equals("user")) {
                predicates.add(criteriaBuilder.equal(
                    criteriaBuilder.lower(solicitanteJoin.get("login")), 
                    loginLogado
                ));
            } else if (perfil.equals("manager")) {
                if (userLogado.getEquipe() != null) {
                     Join<User, Object> equipeSolicitanteJoin = solicitanteJoin.join("equipe", JoinType.LEFT);
                     predicates.add(criteriaBuilder.equal(equipeSolicitanteJoin.get("id"), userLogado.getEquipe().getId()));
                }
            } else if (perfil.equals("technician") || perfil.equals("tecnico")) {
                Predicate meusChamados = criteriaBuilder.equal(
                    criteriaBuilder.lower(tecnicoJoin.get("login")), 
                    loginLogado
                );
                
                Predicate statusAberto = criteriaBuilder.equal(root.get("status"), "Aberto");
                Predicate semTecnico = criteriaBuilder.isNull(root.get("tecnicoAtribuido"));
                Predicate naFilaGeral = criteriaBuilder.and(statusAberto, semTecnico);

                Predicate regraFilaFinal;
                if (userLogado.getEquipe() != null) {
                    Join<User, Object> equipeSolicitanteJoin = solicitanteJoin.join("equipe", JoinType.LEFT);
                    Predicate daMinhaEquipe = criteriaBuilder.equal(equipeSolicitanteJoin.get("id"), userLogado.getEquipe().getId());
                    regraFilaFinal = criteriaBuilder.and(naFilaGeral, daMinhaEquipe);
                } else {
                    regraFilaFinal = naFilaGeral;
                }
                predicates.add(criteriaBuilder.or(meusChamados, regraFilaFinal));
            }

            // 2. BUSCA POR TEXTO (IMPLEMENTAÇÃO NOVA)
            if (termoBusca != null && !termoBusca.isBlank()) {
                String termo = "%" + termoBusca.toLowerCase() + "%";
                String tipo = (tipoBusca != null) ? tipoBusca.toLowerCase() : "descricao";

                switch (tipo) {
                    case "chamado":
                    case "numero":
                        predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("numeroChamado")), termo));
                        break;
                    case "solicitante":
                        predicates.add(criteriaBuilder.like(criteriaBuilder.lower(solicitanteJoin.get("nome")), termo));
                        break;
                    case "prioridade":
                        predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("prioridade")), termo));
                        break;
                    case "descricao":
                    default:
                        predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("descricao")), termo));
                        break;
                }
            }

            // 3. FILTROS PADRÃO
            if (dataInicial != null && dataFinal != null && tipoData != null) {
                String dateField = tipoData.equalsIgnoreCase("fechamento") ? "dataFechamento" : "dataAbertura";
                predicates.add(criteriaBuilder.between(root.get(dateField), dataInicial.atStartOfDay(), dataFinal.plusDays(1).atStartOfDay()));
            }

            if (status != null && !status.isEmpty() && !status.equalsIgnoreCase("todos")) {
                if (status.equalsIgnoreCase("Fechado") || status.equalsIgnoreCase("Resolvido") || status.equalsIgnoreCase("Fechados")) {
                    predicates.add(criteriaBuilder.or(
                        criteriaBuilder.equal(root.get("status"), "Resolvido"),
                        criteriaBuilder.equal(root.get("status"), "Fechado"),
                        criteriaBuilder.equal(root.get("status"), "Encerrado")
                    ));
                } else {
                    predicates.add(criteriaBuilder.equal(root.get("status"), status));
                }
            }

            // Filtro de Categoria (Pode manter aqui, basta o frontend parar de enviar para desativar)
            if (categoria != null && !categoria.isEmpty() && !categoria.equalsIgnoreCase("todas")) {
                predicates.add(criteriaBuilder.equal(root.get("categoria"), categoria));
            }
            
            if (unidade != null && !unidade.isEmpty() && !unidade.equalsIgnoreCase("todas")) {
                predicates.add(criteriaBuilder.equal(root.get("unidade"), unidade));
            }
            if (local != null && !local.isEmpty() && !local.equalsIgnoreCase("todas")) {
                predicates.add(criteriaBuilder.equal(root.get("local"), local));
            }

            if (!perfil.equals("user")) { 
                if (solicitanteId != null) predicates.add(criteriaBuilder.equal(solicitanteJoin.get("id"), solicitanteId));
                if (tecnicoId != null) predicates.add(criteriaBuilder.equal(tecnicoJoin.get("id"), tecnicoId));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}