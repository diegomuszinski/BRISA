export interface User {
  name: string
  email: string
  role: 'admin' | 'manager' | 'technician' | 'user' | null
}

export interface HistoryItem {
  author: string
  comment: string
  date: string
}

export interface TicketDTO {
  id: number
  numeroChamado: string
  descricao: string
  categoria: string
  prioridade: string
  status: string
  dataAbertura: string
  dataFechamento: string | null
  solucao: string | null
  local: string
  unidade: string
  foiReaberto: boolean
  nomeSolicitante: string
  nomeTecnicoAtribuido: string | null
  historico: HistoryItem[]
  slaDeadline: string
}

export interface Ticket {
  id: number
  numeroChamado: string
  user: string
  description: string
  category: string
  priority: string
  status: string
  openedAt: string
  closedAt: string | null
  assignedTo: string | null
  local: string
  unidade: string
  solution: string | null
  history: HistoryItem[]
  isReopened: boolean
  slaDeadline: Date
}

export interface NewTicketPayload {
  user: string
  description: string
  category: string
  priority: string
  unidade: string
  local: string
}

export interface Analyst {
  id: number
  name: string
  team: string
}

export interface DashboardStats {
  chamadosNaFila: number
  chamadosPorAnalista: { nomeAnalista: string; totalChamados: number }[]
  chamadosSlaViolado: TicketDTO[]
}

export interface RelatorioAnalista {
  nomeAnalista: string
  totalChamados: number
}

export interface RelatorioCategoria {
  categoria: string
  tempoMedioHoras: number
}

export interface RelatorioMensal {
  mes: number
  totalChamados: number
}
