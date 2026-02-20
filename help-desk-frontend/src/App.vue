<script setup lang="ts">
import { computed } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import AppSidebar from './components/AppSidebar.vue'

const route = useRoute()

// Lógica para esconder o menu:
// Se a rota for '/' (raiz) ou '/login', a variável showSidebar será falsa.
const showSidebar = computed(() => {
  return route.path !== '/' && route.path !== '/login' && route.name !== 'login'
})
</script>

<template>
  <div class="app-layout">
    <AppSidebar v-if="showSidebar" />

    <main :class="{ 'full-width': !showSidebar }">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
/* Layout Flex para colocar Sidebar e Conteúdo lado a lado */
.app-layout {
  display: flex;
  width: 100%;
  min-height: 100vh;
}

/* Comportamento padrão: Conteúdo se ajusta ao lado do menu */
main {
  flex: 1;
  background-color: #f4f7f6;
  min-height: 100vh;
  transition: all 0.3s ease;
}

/* Quando não tem sidebar (Login), o conteúdo ocupa 100% da largura */
.full-width {
  width: 100%;
  margin: 0;
  padding: 0;
}
</style>
