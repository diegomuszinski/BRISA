export interface User {
  id: number
  nome: string
  login: string
  email: string
  perfil: string
  equipe?: {
    id: number
    nomeEquipe: string
  }
  token?: string
}

export interface Anexo {
  id: number
  nomeArquivo: string
  tipoArquivo: string
  dados: string // Base64
}

export interface Historico {
  id: number
  acao: string
  comentario: string
  dataHora?: string
  date?: string
  usuario?: User | string
  author?: string
}

export interface Ticket {
  id: number
  numeroChamado: string
  descricao: string
  description?: string
  status: string

  // Prioridade e Categoria (podem vir como objeto ou string dependendo da API)
  prioridade: string
  priority?: string

  categoria?: { id: number; nome: string } | null
  category?: string // Compatibilidade com DTO simples

  problema?: { id: number; nome: string; prioridadePadrao: string } | null

  solicitante?: User | string
  user?: string // Compatibilidade

  tecnicoAtribuido?: User | null
  assignedTo?: string // Compatibilidade

  dataAbertura: string
  openedAt?: string

  dataFechamento?: string
  closedAt?: string

  solucao?: string
  solution?: string

  anexos: Anexo[]
  historico: Historico[]
  history?: Historico[]
}
