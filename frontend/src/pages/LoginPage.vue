<template>
  <section>
    <h2>Login</h2>
    <form @submit.prevent="handleSubmit">
      <label>
        Email
        <input v-model="email" type="email" required />
      </label>
      <label>
        Password
        <input v-model="password" type="password" required />
      </label>
      <button type="submit" :disabled="loading">
        {{ loading ? 'Signing in...' : 'Login' }}
      </button>
    </form>
    <p v-if="message" :class="{ error: error }">{{ message }}</p>
    <p class="hint">Demo credentials: admin@example.com / password123</p>
  </section>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import api from '../api';

const email = ref('admin@example.com');
const password = ref('password123');
const message = ref('');
const error = ref(false);
const loading = ref(false);
const router = useRouter();

const handleSubmit = async () => {
  loading.value = true;
  message.value = '';
  error.value = false;
  try {
    const { data } = await api.post('/auth/login', {
      email: email.value,
      password: password.value,
    });
    localStorage.setItem('token', data.accessToken);
    message.value = 'Login successful. Redirecting...';
    await router.push('/player');
  } catch (err) {
    error.value = true;
    message.value = err.response?.data?.message || 'Unable to login';
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
form {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}
label {
  display: flex;
  flex-direction: column;
  font-size: 0.9rem;
  color: #374151;
}
input {
  padding: 0.5rem;
  border: 1px solid #d1d5db;
  border-radius: 0.375rem;
}
button {
  border: none;
  background: #2563eb;
  color: white;
  padding: 0.75rem;
  border-radius: 0.375rem;
  cursor: pointer;
}
button:disabled {
  opacity: 0.6;
  cursor: progress;
}
.error {
  color: #b91c1c;
}
.hint {
  color: #6b7280;
  margin-top: 1rem;
  font-size: 0.85rem;
}
</style>
