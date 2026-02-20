<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import api from '@/services/api'
import { useToast } from 'vue-toastification'
import {
  PencilSquareIcon,
  TrashIcon,
  KeyIcon,
  EyeIcon,
  EyeSlashIcon,
  XMarkIcon,
  MagnifyingGlassIcon,
} from '@heroicons/vue/24/outline'
import type { User } from '@/types'

// Interfaces
interface Equipe {
  id: number
  nomeEquipe: string
}

const toast = useToast()
const users = ref<User[]>([])
const equipes = ref<Equipe[]>([])

// --- CONTROLES DE MODAL ---
const showUserModal = ref(false)
const showPasswordModal = ref(false)
const showDeleteModal = ref(false)

const isEditing = ref(false)
const userSelected = ref<User | null>(null)

// Visualização de Senha
const showPasswordInput = ref(false)

// --- BUSCA (ATUALIZADA) ---
const searchQuery = ref('')

// Computed Property para filtrar a lista em tempo real
const filteredUsers = computed(() => {
  if (!searchQuery.value) return users.value

  const lowerSearch = searchQuery.value.toLowerCase()

  return users.value.filter((user) => {
    // 1. Busca por Nome
    const matchNome = user.nome.toLowerCase().includes(lowerSearch)
    // 2. Busca por Login
    const matchLogin = user.login.toLowerCase().includes(lowerSearch)
    // 3. Busca por Email
    const matchEmail = user.email && user.email.toLowerCase().includes(lowerSearch)
    // 4. Busca por Perfil (Tradução ou original)
    const termoPerfil = translateRole(user.perfil).toLowerCase()
    const matchPerfil =
      termoPerfil.includes(lowerSearch) || user.perfil.toLowerCase().includes(lowerSearch)
    // 5. Busca por Equipe (Verifica se existe equipe antes de ler o nome)
    const matchEquipe = user.equipe && user.equipe.nomeEquipe.toLowerCase().includes(lowerSearch)

    // Se qualquer um for verdadeiro, retorna o usuário
    return matchNome || matchLogin || matchEmail || matchPerfil || matchEquipe
  })
})

// Formulários
const formUser = ref({
  id: null as number | null,
  nome: '',
  login: '',
  email: '',
  perfil: 'user',
  equipeId: '' as string | number,
})

const formPassword = ref({
  newPassword: '',
})

onMounted(() => {
  fetchUsers()
  fetchEquipes()
})

async function fetchUsers() {
  try {
    const res = await api.get('/api/users')
    users.value = res.data
  } catch (e) {
    console.error(e)
  }
}

async function fetchEquipes() {
  try {
    const res = await api.get('/api/equipes')
    equipes.value = res.data
  } catch (e) {
    console.error(e)
  }
}

function translateRole(role: string): string {
  const map: Record<string, string> = {
    admin: 'ADMINISTRADOR',
    manager: 'GESTOR',
    technician: 'TÉCNICO',
    user: 'USUÁRIO',
  }
  return map[(role || '').toLowerCase()] || (role || '').toUpperCase()
}

// --- AÇÕES ---
function openCreateModal() {
  isEditing.value = false
  userSelected.value = null
  formUser.value = { id: null, nome: '', login: '', email: '', perfil: 'user', equipeId: '' }
  formPassword.value.newPassword = ''
  showPasswordInput.value = false
  showUserModal.value = true
}

function openEditModal(user: User) {
  isEditing.value = true
  userSelected.value = user
  formUser.value = {
    id: user.id,
    nome: user.nome,
    login: user.login,
    email: user.email,
    perfil: user.perfil || 'user',
    equipeId: user.equipe ? user.equipe.id : '',
  }
  showUserModal.value = true
}

async function saveUser() {
  if (!formUser.value.nome || !formUser.value.login)
    return toast.warning('Preencha os campos obrigatórios.')

  if (!isEditing.value && !formPassword.value.newPassword) {
    return toast.warning('Crie uma senha inicial para o novo usuário.')
  }

  const payload: any = {
    nome: formUser.value.nome,
    login: formUser.value.login,
    email: formUser.value.email,
    perfil: formUser.value.perfil,
    equipe: formUser.value.equipeId ? { id: Number(formUser.value.equipeId) } : null,
  }

  if (!isEditing.value) {
    payload.password = formPassword.value.newPassword
  }

  try {
    if (isEditing.value && formUser.value.id) {
      await api.put(`/api/users/${formUser.value.id}`, payload)
      toast.success('Dados atualizados com sucesso!')
    } else {
      await api.post('/api/users', payload)
      toast.success('Usuário criado com sucesso!')
    }
    showUserModal.value = false
    fetchUsers()
  } catch (error: any) {
    const msg = error.response?.data?.message || error.response?.data || 'Erro ao salvar.'
    toast.error(msg)
  }
}

function openPasswordModal(user: User) {
  userSelected.value = user
  formPassword.value.newPassword = ''
  showPasswordInput.value = false
  showPasswordModal.value = true
}

async function saveNewPassword() {
  if (!userSelected.value || !formPassword.value.newPassword)
    return toast.warning('Digite a nova senha.')

  try {
    // Usa o novo endpoint seguro no AuthController ou UserController
    await api.put(`/api/users/${userSelected.value.id}/change-password`, {
      password: formPassword.value.newPassword,
    })

    toast.success(`Senha de ${userSelected.value.nome} alterada!`)
    toast.info('O usuário deverá trocá-la no próximo acesso.')
    showPasswordModal.value = false
  } catch (error: any) {
    let msg = 'Erro ao redefinir senha.'
    if (error.response && error.response.data) {
      if (typeof error.response.data === 'string') msg = error.response.data
      else if (error.response.data.message) msg = error.response.data.message
    }
    toast.error(msg)
  }
}

function confirmDelete(user: User) {
  userSelected.value = user
  showDeleteModal.value = true
}

async function executeDelete() {
  if (!userSelected.value) return
  try {
    await api.delete(`/api/users/${userSelected.value.id}`)
    toast.success('Usuário excluído.')
    fetchUsers()
  } catch (e) {
    toast.error('Erro ao excluir usuário.')
  } finally {
    showDeleteModal.value = false
  }
}
</script>

<template>
  <div class="admin-container">
    <div class="header-area">
      <div class="header-title">
        <h1>Gerenciamento de Usuários</h1>
        <p>Cadastre, edite dados ou redefina senhas.</p>
      </div>
      <button class="btn-primary" @click="openCreateModal">+ Novo Usuário</button>
    </div>

    <div class="content-grid">
      <div class="card list-card full-width">
        <div class="search-bar">
          <div class="search-input-wrapper">
            <MagnifyingGlassIcon class="search-icon" />
            <input
              v-model="searchQuery"
              type="text"
              placeholder="Buscar por nome, login, perfil ou equipe..."
            />
          </div>
        </div>

        <table>
          <thead>
            <tr>
              <th>Nome</th>
              <th>Login</th>
              <th>Perfil</th>
              <th>Equipe</th>
              <th class="text-right">Ações</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="u in filteredUsers" :key="u.id">
              <td>{{ u.nome }}</td>
              <td>{{ u.login }}</td>
              <td>
                <span :class="['badge', u.perfil]">{{ translateRole(u.perfil) }}</span>
              </td>
              <td>{{ u.equipe ? u.equipe.nomeEquipe : '-' }}</td>
              <td class="actions-cell text-right">
                <button @click="openEditModal(u)" class="icon-btn edit" title="Editar Dados">
                  <PencilSquareIcon class="icon" />
                </button>
                <button @click="openPasswordModal(u)" class="icon-btn key" title="Redefinir Senha">
                  <KeyIcon class="icon" />
                </button>
                <button @click="confirmDelete(u)" class="icon-btn delete" title="Excluir">
                  <TrashIcon class="icon" />
                </button>
              </td>
            </tr>
            <tr v-if="filteredUsers.length === 0">
              <td colspan="5" class="empty-msg">
                Nenhum usuário encontrado para "{{ searchQuery }}".
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-if="showUserModal" class="modal-overlay">
      <div class="modal-content">
        <div class="modal-header">
          <h2>{{ isEditing ? 'Editar Dados' : 'Novo Usuário' }}</h2>
          <button class="close-btn" @click="showUserModal = false">
            <XMarkIcon class="icon" />
          </button>
        </div>

        <form @submit.prevent="saveUser">
          <label>Nome Completo</label>
          <input v-model="formUser.nome" required placeholder="Ex: Maria Souza" />

          <div class="row">
            <div>
              <label>Login</label>
              <input v-model="formUser.login" required placeholder="maria" />
            </div>
            <div>
              <label>E-mail</label>
              <input v-model="formUser.email" type="email" placeholder="email@empresa.com" />
            </div>
          </div>

          <div v-if="!isEditing" class="password-section">
            <label>Senha Inicial</label>
            <div class="password-wrapper">
              <input
                :type="showPasswordInput ? 'text' : 'password'"
                v-model="formPassword.newPassword"
                placeholder="******"
                required
              />
              <button
                type="button"
                class="toggle-pass"
                @click="showPasswordInput = !showPasswordInput"
              >
                <EyeIcon v-if="!showPasswordInput" class="icon-sm" />
                <EyeSlashIcon v-else class="icon-sm" />
              </button>
            </div>
          </div>

          <div class="row">
            <div>
              <label>Nível</label>
              <select v-model="formUser.perfil">
                <option value="user">Usuário</option>
                <option value="technician">Técnico</option>
                <option value="manager">Gestor</option>
                <option value="admin">Administrador</option>
              </select>
            </div>
            <div>
              <label>Equipe</label>
              <select v-model="formUser.equipeId">
                <option value="">Nenhuma / Geral</option>
                <option v-for="eq in equipes" :key="eq.id" :value="eq.id">
                  {{ eq.nomeEquipe }}
                </option>
              </select>
            </div>
          </div>

          <div class="modal-actions">
            <button type="button" class="btn-cancel" @click="showUserModal = false">
              Cancelar
            </button>
            <button type="submit" class="btn-save">Salvar</button>
          </div>
        </form>
      </div>
    </div>

    <div v-if="showPasswordModal" class="modal-overlay">
      <div class="modal-content small-modal">
        <div class="modal-header">
          <h2 style="color: #d97706">Redefinir Senha</h2>
          <button class="close-btn" @click="showPasswordModal = false">
            <XMarkIcon class="icon" />
          </button>
        </div>

        <p class="desc-text">
          Digite a nova senha para <strong>{{ userSelected?.nome }}</strong
          >.
        </p>

        <div class="form-group">
          <label>Nova Senha</label>
          <div class="password-wrapper">
            <input
              :type="showPasswordInput ? 'text' : 'password'"
              v-model="formPassword.newPassword"
              placeholder="Nova senha forte"
              autofocus
            />
            <button
              type="button"
              class="toggle-pass"
              @click="showPasswordInput = !showPasswordInput"
            >
              <EyeIcon v-if="!showPasswordInput" class="icon-sm" />
              <EyeSlashIcon v-else class="icon-sm" />
            </button>
          </div>
        </div>

        <div class="alert-box">
          ℹ️ Ao salvar, o usuário será obrigado a trocar esta senha no próximo login.
        </div>

        <div class="modal-actions">
          <button class="btn-cancel" @click="showPasswordModal = false">Cancelar</button>
          <button class="btn-save" @click="saveNewPassword">Salvar Nova Senha</button>
        </div>
      </div>
    </div>

    <div v-if="showDeleteModal" class="modal-overlay">
      <div class="modal-content delete-modal">
        <h3 style="color: #dc3545">Excluir Usuário</h3>
        <p>
          Tem certeza que deseja excluir <strong>{{ userSelected?.nome }}</strong
          >?
        </p>
        <div class="modal-actions">
          <button class="btn-cancel" @click="showDeleteModal = false">Cancelar</button>
          <button class="btn-danger" @click="executeDelete">Sim, Excluir</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-container {
  padding: 2rem;
  max-width: 1200px;
  margin: 0 auto;
}
.header-area {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
}
.header-title h1 {
  margin: 0;
  color: #003366;
}
.header-title p {
  margin: 0;
  color: #666;
}

.content-grid {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}
.full-width {
  width: 100%;
}

.card {
  background: white;
  padding: 1.5rem;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

/* ESTILOS DA BUSCA */
.search-bar {
  margin-bottom: 1.5rem;
}
.search-input-wrapper {
  display: flex;
  align-items: center;
  background: #f8f9fa;
  border: 1px solid #ddd;
  border-radius: 6px;
  padding: 0.5rem 1rem;
  transition: border-color 0.2s;
}
.search-input-wrapper:focus-within {
  border-color: #003366;
}
.search-icon {
  width: 20px;
  height: 20px;
  color: #666;
  margin-right: 0.5rem;
}
.search-input-wrapper input {
  border: none;
  background: transparent;
  width: 100%;
  font-size: 1rem;
  outline: none;
  color: #333;
}

/* Tabela e Botões */
table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.9rem;
}
th,
td {
  padding: 1rem;
  text-align: left;
  border-bottom: 1px solid #eee;
}
th {
  color: #555;
  font-weight: 600;
  font-size: 0.9rem;
}
.text-right {
  text-align: right;
}
.actions-cell {
  display: flex;
  gap: 0.8rem;
  justify-content: flex-end;
}
.icon-btn {
  border: none;
  background: none;
  cursor: pointer;
  padding: 4px;
  transition: transform 0.1s;
}
.icon-btn:hover {
  transform: scale(1.1);
}
.icon-btn.edit {
  color: #0d6efd;
}
.icon-btn.key {
  color: #d97706;
}
.icon-btn.delete {
  color: #dc3545;
}
.icon {
  width: 20px;
  height: 20px;
}
.empty-msg {
  text-align: center;
  padding: 2rem;
  color: #999;
  font-style: italic;
}

/* MODALS e FORMS */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}
.modal-content {
  background: white;
  padding: 2rem;
  border-radius: 8px;
  width: 500px;
  max-width: 90%;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.2);
}
.modal-content.small-modal {
  width: 400px;
}
.modal-content.delete-modal {
  width: 350px;
  text-align: center;
}
.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}
.modal-header h2 {
  margin: 0;
  color: #003366;
  font-size: 1.4rem;
}
.close-btn {
  background: none;
  border: none;
  cursor: pointer;
  color: #999;
}
label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 500;
  color: #444;
}
input,
select {
  width: 100%;
  padding: 0.7rem;
  border: 1px solid #ccc;
  border-radius: 4px;
  margin-bottom: 1rem;
}
.row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}
.password-section {
  margin-bottom: 1rem;
}
.password-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}
.password-wrapper input {
  margin-bottom: 0;
  padding-right: 40px;
}
.toggle-pass {
  position: absolute;
  right: 10px;
  background: none;
  border: none;
  cursor: pointer;
  color: #666;
  display: flex;
  align-items: center;
  height: 100%;
}
.icon-sm {
  width: 18px;
  height: 18px;
}
.alert-box {
  background: #fff3cd;
  color: #856404;
  padding: 0.8rem;
  border-radius: 4px;
  font-size: 0.9rem;
  margin-top: 1rem;
  border: 1px solid #ffeeba;
}
.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
  margin-top: 2rem;
}
.delete-modal .modal-actions {
  justify-content: center;
}
.btn-save,
.btn-primary {
  background: #003366;
  color: white;
  padding: 0.7rem 1.5rem;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-weight: bold;
}
.btn-cancel {
  background: #eee;
  color: #333;
  padding: 0.7rem 1.5rem;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}
.btn-danger {
  background: #dc3545;
  color: white;
  padding: 0.7rem 1.5rem;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-weight: bold;
}

/* BADGES */
.badge {
  padding: 0.2rem 0.6rem;
  border-radius: 12px;
  color: white;
  font-size: 0.75rem;
  font-weight: bold;
  text-transform: uppercase;
}
.badge.admin {
  background: #dc3545;
}
.badge.manager {
  background: #f59e0b;
  color: #fff;
}
.badge.technician {
  background: #0ea5e9;
}
.badge.user {
  background: #6b7280;
}
</style>
