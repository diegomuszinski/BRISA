import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/services/api'
import type { Ticket, User, Anexo } from '@/types'

export function calculateSlaDeadline(
  openedAt: string | Date | undefined,
  priority: string | undefined,
): Date {
  const start = openedAt ? new Date(openedAt) : new Date()
  const p = priority || 'Média'
  let hoursToAdd = 48

  switch (p) {
    case 'Crítica':
      hoursToAdd = 2
      break
    case 'Elevada':
      hoursToAdd = 8
      break
    case 'Média':
      hoursToAdd = 24
      break
    case 'Baixa':
      hoursToAdd = 48
      break
    default:
      hoursToAdd = 48
  }

  return new Date(start.getTime() + hoursToAdd * 60 * 60 * 1000)
}

export const useTicketStore = defineStore('ticket', () => {
  const token = ref(localStorage.getItem('token') || '')
  const currentUser = ref({ name: '', email: '', role: '', login: '' })

  const openTickets = ref<Ticket[]>([])
  const inProgressTickets = ref<Ticket[]>([])
  const closedTickets = ref<Ticket[]>([])
  const myOpenTickets = ref<Ticket[]>([])
  const myClosedTickets = ref<Ticket[]>([])
  const activeTicket = ref<Ticket | null>(null)

  const categories = ref<{ id: number; nome: string }[]>([])
  const problems = ref<{ id: number; nome: string; prioridadePadrao: string }[]>([])
  const analysts = ref<User[]>([])

  const dashboardStats = ref({ abertos: 0, emAndamento: 0, fechados: 0, total: 0, slaViolado: 0 })

  if (token.value) {
    decodeToken(token.value)
  }

  function decodeToken(tk: string) {
    try {
      const base64Url = tk.split('.')[1]
      const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/')
      const jsonPayload = decodeURIComponent(
        window
          .atob(base64)
          .split('')
          .map(function (c) {
            return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)
          })
          .join(''),
      )

      const payload = JSON.parse(jsonPayload)
      const loginFromToken = payload.sub || ''

      currentUser.value = {
        name: payload.name || 'Usuário',
        email: payload.sub,
        login: loginFromToken,
        role: payload.role || 'user',
      }
    } catch (e) {
      console.error('Token inválido', e)
      logout()
    }
  }

  async function login(loginUser: string, senhaUser: string) {
    const response = await api.post('/api/auth/login', { login: loginUser, senha: senhaUser })
    token.value = response.data.token
    localStorage.setItem('token', token.value)
    decodeToken(token.value)
  }

  function logout() {
    token.value = ''
    currentUser.value = { name: '', email: '', role: '', login: '' }
    localStorage.removeItem('token')
    openTickets.value = []
    inProgressTickets.value = []
    closedTickets.value = []
    myOpenTickets.value = []
    myClosedTickets.value = []
    activeTicket.value = null
  }

  const normalize = (str: string) =>
    (str || '')
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')

  async function fetchTickets() {
    if (!token.value) return
    try {
      const response = await api.get('/api/tickets')
      const all: Ticket[] = response.data

      openTickets.value = all.filter((t) => t.status === 'Aberto')
      const emAndamento = all.filter((t) => t.status === 'Em Andamento')

      const userRole = normalize(currentUser.value.role)
      const userLogin = normalize(currentUser.value.login)
      const userName = normalize(currentUser.value.name)

      if (userRole.includes('technician') || userRole.includes('tecnico')) {
        inProgressTickets.value = emAndamento.filter((t) => {
          if (t.tecnicoAtribuido && typeof t.tecnicoAtribuido === 'object') {
            const techLogin = normalize(t.tecnicoAtribuido.login)
            const techName = normalize(t.tecnicoAtribuido.nome)
            return techLogin === userLogin || techName === userName
          }
          return false
        })
      } else {
        inProgressTickets.value = emAndamento
      }

      closedTickets.value = all.filter((t) =>
        ['Resolvido', 'Fechado', 'Encerrado', 'Cancelado'].includes(t.status),
      )
    } catch (error) {
      console.error(error)
    }
  }

  async function fetchMyTickets() {
    if (!token.value) return
    try {
      const response = await api.get('/api/tickets/me')
      const all: Ticket[] = response.data
      myOpenTickets.value = all.filter((t) => {
        const s = normalize(t.status)
        return s === 'aberto' || s === 'em andamento'
      })
      myClosedTickets.value = all.filter((t) => {
        const s = normalize(t.status)
        return s === 'resolvido' || s === 'fechado' || s === 'encerrado' || s === 'cancelado'
      })
    } catch (error) {
      console.error(error)
    }
  }

  async function fetchTicketById(id: number) {
    activeTicket.value = null
    try {
      const response = await api.get(`/api/tickets/${id}`)
      activeTicket.value = response.data
    } catch (error) {
      console.error(error)
    }
  }

  // CORREÇÃO: Agora utiliza o endpoint centralizado de estatísticas do backend
  async function fetchDashboardStats(equipeId: number | null = null) {
    try {
      const params = equipeId ? { equipeId } : {}
      const res = await api.get('/api/dashboard/stats', { params })
      dashboardStats.value = res.data
    } catch (e) {
      console.error('Erro ao buscar estatísticas:', e)
    }
  }

  async function createTicket(formData: any) {
    const data = new FormData()
    const ticketJson = JSON.stringify({
      descricao: formData.description,
      idCategoria: formData.category,
      idProblema: formData.problemaId,
      prioridade: formData.priority,
    })
    data.append('ticket', new Blob([ticketJson], { type: 'application/json' }))
    if (formData.files && formData.files.length > 0) {
      const fileList = formData.files
      for (let i = 0; i < fileList.length; i++) {
        data.append('anexos', fileList[i])
      }
    }
    await api.post('/api/tickets', data, { headers: { 'Content-Type': 'multipart/form-data' } })
    if (currentUser.value.role === 'user') await fetchMyTickets()
    else await fetchTickets()
  }

  async function createCategory(nome: string) {
    await api.post('/api/categorias', { nome })
    await fetchFormData()
  }
  async function updateCategory(id: number, nome: string) {
    await api.put(`/api/categorias/${id}`, { nome })
    await fetchFormData()
  }
  async function deleteCategory(id: number) {
    await api.delete(`/api/categorias/${id}`)
    await fetchFormData()
  }

  async function createProblem(nome: string, prioridadePadrao: string) {
    await api.post('/api/problemas', { nome, prioridadePadrao })
    await fetchFormData()
  }
  async function updateProblem(id: number, nome: string, prioridadePadrao: string) {
    await api.put(`/api/problemas/${id}`, { nome, prioridadePadrao })
    await fetchFormData()
  }
  async function deleteProblem(id: number) {
    await api.delete(`/api/problemas/${id}`)
    await fetchFormData()
  }

  async function updateTicketClassification(id: number, category: string, priority: string) {
    await api.put(`/api/tickets/${id}/classification`, { category, priority })
    if (activeTicket.value && activeTicket.value.id === id) await fetchTicketById(id)
    await fetchTickets()
  }

  async function uploadAttachment(ticketId: number, file: File) {
    const formData = new FormData()
    formData.append('file', file)
    const res = await api.post(`/api/tickets/${ticketId}/attachments`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    if (activeTicket.value && activeTicket.value.id === ticketId) activeTicket.value = res.data
  }

  async function downloadAnexo(anexo: Anexo) {
    try {
      const response = await api.get(`/api/anexos/${anexo.id}`, {
        responseType: 'blob',
      })

      const blob = new Blob([response.data], { type: response.headers['content-type'] })
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.setAttribute('download', anexo.nomeArquivo || 'arquivo')
      document.body.appendChild(link)
      link.click()
      link.remove()
      window.URL.revokeObjectURL(url)
    } catch (error) {
      console.error('Erro ao baixar anexo:', error)
      throw error
    }
  }

  async function addCommentToTicket(ticketId: number, comment: string) {
    await api.post(`/api/tickets/${ticketId}/comments`, { comentario: comment })
    await fetchTicketById(ticketId)
  }

  async function assignTicketToSelf(ticketId: number) {
    await api.post(`/api/tickets/${ticketId}/assign-self`)
    await fetchTickets()
    if (activeTicket.value?.id === ticketId) await fetchTicketById(ticketId)
  }

  async function assignTicket(ticketId: number, technicianId: number) {
    await api.post(`/api/tickets/${ticketId}/assign/${technicianId}`)
    await fetchTickets()
    if (activeTicket.value?.id === ticketId) await fetchTicketById(ticketId)
  }

  async function closeTicket(ticketId: number, solution: string) {
    await api.post(`/api/tickets/${ticketId}/close`, { solucao: solution })
    if (currentUser.value.role === 'user') await fetchMyTickets()
    else await fetchTickets()
    if (activeTicket.value?.id === ticketId) await fetchTicketById(ticketId)
  }

  async function reopenTicket(ticketId: number, reason: string) {
    await api.post(`/api/tickets/${ticketId}/reopen`, { motivo: reason })
    if (currentUser.value.role === 'user') await fetchMyTickets()
    else await fetchTickets()
    if (activeTicket.value?.id === ticketId) await fetchTicketById(ticketId)
  }

  async function fetchFormData() {
    try {
      const catRes = await api.get('/api/categorias')
      categories.value = catRes.data
      const probRes = await api.get('/api/problemas')
      problems.value = probRes.data
    } catch {
      categories.value = []
      problems.value = []
    }
  }

  async function fetchAnalysts() {
    try {
      const res = await api.get('/api/users/technicians')
      analysts.value = res.data
    } catch (e) {
      console.error(e)
    }
  }

  return {
    token,
    currentUser,
    openTickets,
    inProgressTickets,
    closedTickets,
    myOpenTickets,
    myClosedTickets,
    activeTicket,
    categories,
    problems,
    analysts,
    dashboardStats,
    login,
    logout,
    fetchTickets,
    fetchMyTickets,
    fetchTicketById,
    fetchDashboardStats,
    createTicket,
    createCategory,
    updateCategory,
    deleteCategory,
    createProblem,
    updateProblem,
    deleteProblem,
    updateTicketClassification,
    uploadAttachment,
    downloadAnexo,
    addCommentToTicket,
    assignTicketToSelf,
    assignTicket,
    closeTicket,
    reopenTicket,
    fetchFormData,
    fetchAnalysts,
  }
})
