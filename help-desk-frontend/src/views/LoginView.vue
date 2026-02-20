<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useTicketStore } from '@/stores/ticketStore'
import { useToast } from 'vue-toastification'
import api from '@/services/api'
import logoBrisa from '@/assets/brisa.png'
import { EyeIcon, EyeSlashIcon, CheckCircleIcon, XCircleIcon } from '@heroicons/vue/24/outline'

const login = ref('')
const password = ref('')
const showPassword = ref(false)
const router = useRouter()
const ticketStore = useTicketStore()
const toast = useToast()
const isLoading = ref(false)

const showFirstAccessModal = ref(false)
const newPassword = ref('')
const confirmPassword = ref('')
const tempUserId = ref<number | null>(null)
const tempToken = ref('')

// Controle do Olho (Ver Senha)
const showNewPassword = ref(false)
const showConfirmPassword = ref(false)

const hasMinLength = computed(() => newPassword.value.length >= 8)
const hasNumber = computed(() => /\d/.test(newPassword.value))
const hasLetter = computed(() => /[a-zA-Z]/.test(newPassword.value))
const hasSpecial = computed(() => /[^a-zA-Z0-9]/.test(newPassword.value))
const passwordsMatch = computed(
  () => newPassword.value && newPassword.value === confirmPassword.value,
)

const isFormValid = computed(
  () =>
    hasMinLength.value &&
    hasNumber.value &&
    hasLetter.value &&
    hasSpecial.value &&
    passwordsMatch.value,
)

async function handleLogin() {
  if (!login.value || !password.value) {
    toast.warning('Preencha o login e a senha.')
    return
  }

  isLoading.value = true
  try {
    const response = await api.post('/api/auth/login', {
      login: login.value,
      senha: password.value,
    })

    const data = response.data

    ticketStore.token = data.token
    ticketStore.currentUser = {
      id: data.id,
      login: login.value,
      role: data.role.toLowerCase(),
      name: data.nome, // CORREÇÃO: Atribui 'nome' do backend para 'name' da Store
    }
    localStorage.setItem('token', data.token)
    localStorage.setItem('user', JSON.stringify(ticketStore.currentUser))

    api.defaults.headers.common['Authorization'] = `Bearer ${data.token}`

    if (data.primeiroAcesso === true) {
      showFirstAccessModal.value = true
      tempUserId.value = data.id
      tempToken.value = data.token
      toast.info('Primeiro acesso detectado. Crie uma nova senha.')
      isLoading.value = false
      return
    }

    const role = data.role.toLowerCase()
    if (role === 'user' || role === 'usuario') {
      router.push('/meus-chamados')
    } else {
      router.push('/fila/abertos')
    }
  } catch (error: any) {
    let msg = 'Login ou senha inválidos.'
    if (error.response && error.response.data) {
      if (typeof error.response.data === 'string') msg = error.response.data
      else if (error.response.data.message) msg = error.response.data.message
    }
    toast.error(msg)
  } finally {
    isLoading.value = false
  }
}

async function handleChangePassword() {
  if (!isFormValid.value || !tempUserId.value) return

  try {
    await api.put(
      `/api/auth/change-password/${tempUserId.value}`,
      { password: newPassword.value },
      { headers: { Authorization: `Bearer ${tempToken.value}` } },
    )

    toast.success('Senha alterada com sucesso! Entre novamente.')
    showFirstAccessModal.value = false

    login.value = ''
    password.value = ''
    newPassword.value = ''
    confirmPassword.value = ''
    localStorage.removeItem('token')
    delete api.defaults.headers.common['Authorization']
  } catch (error: any) {
    let msg = 'Erro ao alterar senha.'
    if (error.response && error.response.data) {
      if (typeof error.response.data === 'string') msg = error.response.data
      else if (error.response.data.message) msg = error.response.data.message
    }
    toast.error(msg)
  }
}
</script>

<template>
  <div class="login-container">
    <div class="login-card">
      <div class="logo-area">
        <img :src="logoBrisa" alt="Logo Brisa" class="logo" />
        <h1>Help Desk</h1>
      </div>

      <form @submit.prevent="handleLogin" class="login-form">
        <div class="form-group">
          <label for="login">Usuário (Login)</label>
          <input id="login" type="text" v-model="login" placeholder="******" required autofocus />
        </div>

        <div class="form-group">
          <label for="password">Senha</label>
          <div class="password-wrapper">
            <input
              id="password"
              :type="showPassword ? 'text' : 'password'"
              v-model="password"
              placeholder="******"
              required
            />
            <button type="button" class="toggle-password" @click="showPassword = !showPassword">
              <EyeIcon v-if="!showPassword" class="icon" />
              <EyeSlashIcon v-else class="icon" />
            </button>
          </div>
        </div>

        <button type="submit" class="btn-login" :disabled="isLoading">
          {{ isLoading ? 'Entrando...' : 'Entrar' }}
        </button>
      </form>
    </div>

    <div v-if="showFirstAccessModal" class="modal-overlay">
      <div class="modal-content">
        <h2>🔒 Segurança Obrigatória</h2>
        <p class="modal-desc">Este é seu primeiro acesso. Crie uma nova senha segura.</p>

        <div class="form-group">
          <label>Nova Senha</label>
          <div class="password-wrapper">
            <input
              :type="showNewPassword ? 'text' : 'password'"
              v-model="newPassword"
              placeholder="Nova senha forte"
            />
            <button
              type="button"
              class="toggle-password"
              @click="showNewPassword = !showNewPassword"
            >
              <EyeIcon v-if="!showNewPassword" class="icon" />
              <EyeSlashIcon v-else class="icon" />
            </button>
          </div>
        </div>

        <div class="security-checklist">
          <div :class="{ valid: hasMinLength }">
            <component :is="hasMinLength ? CheckCircleIcon : XCircleIcon" class="check-icon" />
            Mínimo 8 caracteres
          </div>
          <div :class="{ valid: hasLetter }">
            <component :is="hasLetter ? CheckCircleIcon : XCircleIcon" class="check-icon" /> Pelo
            menos uma letra
          </div>
          <div :class="{ valid: hasNumber }">
            <component :is="hasNumber ? CheckCircleIcon : XCircleIcon" class="check-icon" /> Pelo
            menos um número
          </div>
          <div :class="{ valid: hasSpecial }">
            <component :is="hasSpecial ? CheckCircleIcon : XCircleIcon" class="check-icon" />
            Caractere especial (@#$%)
          </div>
        </div>

        <div class="form-group">
          <label>Confirmar Senha</label>
          <div class="password-wrapper">
            <input
              :type="showConfirmPassword ? 'text' : 'password'"
              v-model="confirmPassword"
              placeholder="Repita a senha"
            />
            <button
              type="button"
              class="toggle-password"
              @click="showConfirmPassword = !showConfirmPassword"
            >
              <EyeIcon v-if="!showConfirmPassword" class="icon" />
              <EyeSlashIcon v-else class="icon" />
            </button>
          </div>
          <span v-if="newPassword && !passwordsMatch" class="error-msg"
            >As senhas não conferem.</span
          >
        </div>

        <button @click="handleChangePassword" class="btn-login btn-change" :disabled="!isFormValid">
          Salvar Nova Senha
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #f4f7f6;
}
.login-card {
  background: white;
  padding: 2.5rem;
  border-radius: 8px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
  width: 100%;
  max-width: 400px;
  text-align: center;
}
.logo-area {
  margin-bottom: 2rem;
}
.logo {
  width: 120px;
  margin-bottom: 1rem;
}
.logo-area h1 {
  font-size: 1.5rem;
  color: #2c3e50;
  font-weight: 400;
  margin: 0;
}
.login-form {
  text-align: left;
}
.form-group {
  margin-bottom: 1.2rem;
}
.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  color: #666;
  font-size: 0.9rem;
}
.form-group input {
  width: 100%;
  padding: 0.8rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 1rem;
}
.form-group input:focus {
  border-color: #007bff;
  outline: none;
}
.password-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}
.toggle-password {
  position: absolute;
  right: 10px;
  background: none;
  border: none;
  cursor: pointer;
  color: #666;
  padding: 0;
  display: flex;
  align-items: center;
}
.icon {
  width: 20px;
  height: 20px;
}
.btn-login {
  width: 100%;
  padding: 0.9rem;
  background-color: #003366;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 1rem;
  font-weight: bold;
  cursor: pointer;
  transition: background 0.2s;
  margin-top: 1rem;
}
.btn-login:hover:not(:disabled) {
  background-color: #002244;
}
.btn-login:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}

/* MODAL STYLES */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.85);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
}
.modal-content {
  background: white;
  padding: 2rem;
  border-radius: 8px;
  width: 90%;
  max-width: 450px;
  text-align: left;
}
.modal-content h2 {
  color: #003366;
  margin-top: 0;
}
.modal-desc {
  color: #666;
  font-size: 0.95rem;
  margin-bottom: 1.5rem;
}
.security-checklist {
  background: #f8f9fa;
  padding: 1rem;
  border-radius: 4px;
  margin-bottom: 1.5rem;
  font-size: 0.9rem;
}
.security-checklist div {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.3rem;
  color: #dc3545;
}
.security-checklist div.valid {
  color: #198754;
  font-weight: bold;
}
.check-icon {
  width: 18px;
  height: 18px;
}
.error-msg {
  color: red;
  font-size: 0.85rem;
  display: block;
  margin-top: 0.3rem;
}
.btn-change {
  background-color: #28a745;
}
.btn-change:hover:not(:disabled) {
  background-color: #218838;
}
</style>
