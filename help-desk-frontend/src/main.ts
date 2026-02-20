import './assets/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'

// Importações do Toast e CSS
import Toast, { type PluginOptions, POSITION } from 'vue-toastification'
import 'vue-toastification/dist/index.css'

import App from './App.vue'
import router from './router' // Este import exige que o router tenha 'export default'

const app = createApp(App)

// Configurações para as notificações (Toasts)
const toastOptions: PluginOptions = {
  position: POSITION.TOP_RIGHT,
  timeout: 3000,
  closeOnClick: true,
  pauseOnFocusLoss: true,
  pauseOnHover: true,
  draggable: true,
  draggablePercent: 0.6,
  showCloseButtonOnHover: false,
  hideProgressBar: false,
  closeButton: 'button',
  icon: true,
  rtl: false,
}

app.use(createPinia())
app.use(router)
app.use(Toast, toastOptions) // Usa as opções definidas acima

app.mount('#app')
