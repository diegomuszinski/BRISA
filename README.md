# 🎫 BRISA HELPDESK

Sistema completo de Help Desk desenvolvido para gerenciar chamados, equipes e análise de desempenho de suporte técnico.

## 📋 Índice

- [Sobre o Projeto](#sobre-o-projeto)
- [Funcionalidades](#funcionalidades)
- [Tecnologias Utilizadas](#tecnologias-utilizadas)
- [Pré-requisitos](#pré-requisitos)
- [Instalação](#instalação)
- [Configuração](#configuração)
- [Como Executar](#como-executar)
- [Estrutura do Projeto](#estrutura-do-projeto)
- [Credenciais Padrão](#credenciais-padrão)
- [Contribuindo](#contribuindo)

## 🎯 Sobre o Projeto

O BRISA HELPDESK é uma solução completa para gerenciamento de tickets de suporte técnico, oferecendo recursos avançados de análise, relatórios e gestão de equipes.

### Principais Recursos

- **Gestão de Chamados**: Criação, atribuição e acompanhamento de tickets
- **Sistema de Usuários**: Controle de acesso com diferentes perfis (Admin, Técnico, Usuário)
- **Gestão de Equipes**: Organização de técnicos em equipes de trabalho
- **Analytics e Dashboards**: Visualização de métricas e KPIs em tempo real
- **Relatórios**: Geração de relatórios de SLA e satisfação
- **Histórico**: Rastreamento completo de todas as ações em cada chamado
- **Anexos**: Suporte para upload de arquivos nos chamados
- **Pesquisa de Satisfação**: Coleta de feedback dos usuários

## ✨ Funcionalidades

### Para Usuários
- Abertura de chamados
- Acompanhamento do status dos tickets
- Visualização do histórico de chamados
- Avaliação de satisfação após fechamento

### Para Técnicos
- Dashboard com chamados atribuídos
- Atualização de status e solução de tickets
- Comunicação através de comentários
- Gerenciamento de prioridades

### Para Administradores
- Painel de gestão completo
- Gerenciamento de usuários e equipes
- Configuração de categorias e tipos de problemas
- Relatórios analíticos detalhados
- Métricas de desempenho por analista
- Alertas de SLA
- Análise de tickets por período

## 🚀 Tecnologias Utilizadas

### Frontend
- **Vue 3** - Framework JavaScript progressivo
- **TypeScript** - Superset tipado de JavaScript
- **Vite** - Build tool e dev server
- **Pinia** - Gerenciamento de estado
- **Vue Router** - Roteamento SPA
- **Chart.js** - Gráficos e visualizações
- **Axios** - Cliente HTTP
- **Heroicons** - Ícones
- **TailwindCSS** (implícito nos componentes)
- **JWT Decode** - Autenticação JWT
- **jsPDF** - Exportação de relatórios em PDF
- **html2canvas** - Captura de tela para relatórios

### Backend
- **Spring Boot 3.2** - Framework Java
- **Java 17** - Linguagem de programação
- **Spring Data JPA** - Persistência de dados
- **Spring Security** - Autenticação e autorização
- **PostgreSQL** - Banco de dados relacional
- **JWT** - Autenticação baseada em tokens
- **Lombok** - Redução de boilerplate
- **Gradle** - Gerenciador de build

## 📦 Pré-requisitos

Antes de começar, certifique-se de ter instalado:

- **Node.js** 20.19.0 ou superior / 22.12.0 ou superior
- **Java 17** ou superior
- **PostgreSQL** 12 ou superior
- **Gradle** (incluído via wrapper)
- **Git**

## 🔧 Instalação

### 1. Clone o repositório

```bash
git clone <url-do-repositorio>
cd "BRISA HELPDESK"
```

### 2. Configurar o Banco de Dados

#### Criar o banco de dados PostgreSQL

```sql
CREATE DATABASE helpdesk;
```

#### Executar o script de criação das tabelas

Execute o conteúdo do arquivo `Tabelas Banco de Dados.txt` no PostgreSQL para criar todas as tabelas e dados iniciais.

```bash
psql -U postgres -d helpdesk -f "Tabelas Banco de Dados.txt"
```

### 3. Instalar dependências do Frontend

```bash
cd help-desk-frontend
npm install
```

### 4. Configurar o Backend

Edite o arquivo `helpdesk-backend/helpdesk-api/src/main/resources/application.properties`:

```properties
# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/helpdesk
spring.datasource.username=postgres
spring.datasource.password=admin

# JPA Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# Server Configuration
server.port=8080
```

## ⚙️ Configuração

### Variáveis de Ambiente (Frontend)

Crie um arquivo `.env` na pasta `help-desk-frontend`:

```env
VITE_API_BASE_URL=http://localhost:8080
```

### Configuração do Backend

As configurações principais estão em:
- `helpdesk-backend/helpdesk-api/src/main/resources/application.properties`

## ▶️ Como Executar

### Iniciar o Backend

Na pasta raiz do projeto:

```bash
cd helpdesk-backend/helpdesk-api
./gradlew bootRun
```

Ou no Windows:

```bash
cd helpdesk-backend/helpdesk-api
gradlew.bat bootRun
```

O backend estará disponível em: `http://localhost:8080`

### Iniciar o Frontend

Em outro terminal, na pasta raiz do projeto:

```bash
cd help-desk-frontend
npm run dev
```

O frontend estará disponível em: `http://localhost:5173`

### Atalho Rápido

Consulte o arquivo `Iniciar Sistema.txt` para comandos rápidos:

**Backend:**
```bash
./gradlew bootRun
```

**Frontend:**
```bash
npm run dev
```

## 📁 Estrutura do Projeto

```
BRISA HELPDESK/
├── help-desk-frontend/           # Aplicação Vue.js
│   ├── src/
│   │   ├── assets/               # Arquivos estáticos
│   │   ├── components/           # Componentes Vue reutilizáveis
│   │   │   ├── AnalystPerformanceChart.vue
│   │   │   ├── AppModal.vue
│   │   │   ├── AssignTicketModal.vue
│   │   │   ├── AvgTimeByCategoryChart.vue
│   │   │   ├── DetailedTicketTable.vue
│   │   │   ├── KPICard.vue
│   │   │   ├── SlaAlerts.vue
│   │   │   ├── TicketCreateForm.vue
│   │   │   ├── TicketList.vue
│   │   │   └── ...
│   │   ├── router/               # Configuração de rotas
│   │   ├── services/             # Serviços de API
│   │   ├── stores/               # Gerenciamento de estado (Pinia)
│   │   ├── types/                # Definições TypeScript
│   │   ├── utils/                # Utilitários e helpers
│   │   │   ├── exporters.ts      # Exportação de relatórios
│   │   │   └── formatters.ts     # Formatação de dados
│   │   ├── views/                # Páginas da aplicação
│   │   │   ├── LoginView.vue
│   │   │   ├── DashboardView.vue
│   │   │   ├── CreateTicketView.vue
│   │   │   ├── TicketDetailView.vue
│   │   │   ├── AnalyticsDashboardView.vue
│   │   │   ├── AdminUsersView.vue
│   │   │   ├── AdminEquipesView.vue
│   │   │   ├── ReportsView.vue
│   │   │   └── ...
│   │   ├── App.vue               # Componente raiz
│   │   └── main.ts               # Ponto de entrada
│   ├── package.json
│   └── vite.config.ts
│
├── helpdesk-backend/             # API Spring Boot
│   └── helpdesk-api/
│       ├── src/
│       │   ├── main/
│       │   │   ├── java/
│       │   │   │   └── br/com/brisabr/
│       │   │   │       ├── controller/
│       │   │   │       ├── model/
│       │   │   │       ├── repository/
│       │   │   │       ├── service/
│       │   │   │       ├── security/
│       │   │   │       └── config/
│       │   │   └── resources/
│       │   │       └── application.properties
│       │   └── test/
│       ├── build.gradle
│       └── gradlew
│
├── Iniciar Sistema.txt           # Comandos de inicialização
├── Tabelas Banco de Dados.txt    # Script SQL do banco
└── README.md                      # Este arquivo
```

## 🔑 Credenciais Padrão

Após executar o script do banco de dados, você pode fazer login com:

**Administrador:**
- **Login:** admin
- **Senha:** 123456

> ⚠️ **Importante:** Altere a senha padrão após o primeiro acesso!

## 👥 Perfis de Usuário

O sistema possui 3 tipos de perfis:

1. **ADMIN** - Acesso completo ao sistema
   - Gerenciamento de usuários e equipes
   - Acesso a todos os relatórios
   - Configurações do sistema

2. **TECNICO** - Atendimento de chamados
   - Visualização e atendimento de tickets atribuídos
   - Atualização de status
   - Registro de soluções

3. **USUARIO** - Solicitante
   - Abertura de chamados
   - Acompanhamento de tickets próprios
   - Avaliação de satisfação

## 📊 Módulos do Sistema

### Dashboard
- Visão geral de chamados
- KPIs em tempo real
- Gráficos de desempenho

### Gerenciamento de Chamados
- Criação e edição de tickets
- Sistema de prioridades
- Categorização de problemas
- Anexos e documentação

### Analytics
- Análise temporal de tickets
- Desempenho por analista
- Tempo médio por categoria
- Tickets por mês

### Relatórios
- Relatório de SLA
- Relatório de satisfação
- Exportação em PDF
- Filtros avançados

### Administração
- Gestão de usuários
- Gestão de equipes
- Configuração de categorias
- Configuração de tipos de problemas

## 📝 Scripts Disponíveis

### Frontend

```bash
npm run dev          # Inicia servidor de desenvolvimento
npm run build        # Build para produção
npm run preview      # Preview do build de produção
npm run type-check   # Verificação de tipos TypeScript
npm run lint         # Lint do código
npm run format       # Formata o código com Prettier
```

### Backend

```bash
./gradlew bootRun    # Inicia a aplicação
./gradlew build      # Build do projeto
./gradlew test       # Executa os testes
./gradlew clean      # Limpa builds anteriores
```

## 🔒 Segurança

- Autenticação baseada em JWT
- Senhas criptografadas com BCrypt
- Controle de acesso baseado em roles (RBAC)
- Proteção contra CSRF
- Validação de dados no backend

## 🐛 Solução de Problemas

### Backend não inicia

1. Verifique se o PostgreSQL está rodando
2. Confirme as credenciais no `application.properties`
3. Verifique se a porta 8080 está disponível

### Frontend não conecta ao backend

1. Verifique se o backend está rodando
2. Confirme a URL da API no arquivo `.env`
3. Verifique o CORS configurado no backend

### Erro de autenticação

1. Limpe o localStorage do navegador
2. Verifique se o token JWT está válido
3. Confirme que o usuário existe no banco de dados
