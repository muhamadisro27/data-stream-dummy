<template>
  <div class="app-shell">
    <header>
      <h1>Secure Video Demo</h1>
      <nav>
        <RouterLink to="/login">Login</RouterLink>
        <RouterLink to="/player">Player</RouterLink>
      </nav>
    </header>
    <main>
      <RouterView />
    </main>
  </div>
</template>

<script setup>
import { RouterLink, RouterView } from 'vue-router';
</script>

<style scoped>
.app-shell {
  max-width: 720px;
  margin: 0 auto;
  padding: 2rem;
  font-family: Arial, sans-serif;
}
header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1.5rem;
}
nav a {
  margin-left: 1rem;
  text-decoration: none;
  color: #2563eb;
}
nav a.router-link-active {
  font-weight: bold;
}
main {
  background: #f9fafb;
  padding: 1.5rem;
  border-radius: 0.5rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}
</style>
