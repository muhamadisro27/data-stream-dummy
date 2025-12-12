<template>
  <section>
    <h2>Secure Video Player</h2>
    <div class="selector">
      <label>
        Choose video
        <select v-model="selectedFile" @change="handleSelection">
          <option disabled value="">Select file</option>
          <option v-for="file in files" :key="file.id" :value="file.id">
            {{ file.label }}
          </option>
        </select>
      </label>
      <button @click="refreshUrl" :disabled="!selectedFile || requesting">
        Refresh URL
      </button>
    </div>
    <p v-if="status">{{ status }}</p>
    <video v-if="videoUrl" :src="videoUrl" controls width="640" @error="onVideoError"></video>
    <p class="expires" v-if="expiresAt">
      URL expires at: {{ new Date(expiresAt * 1000).toLocaleTimeString() }}
    </p>
  </section>
</template>

<script setup>
import { nextTick, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import api from '../api';

const status = ref('Loading video catalog...');
const videoUrl = ref('');
const expiresAt = ref(null);
const files = ref([]);
const selectedFile = ref('');
const requesting = ref(false);
const router = useRouter();

const fetchPresignedUrl = async (fileId) => {
  if (!fileId) {
    videoUrl.value = '';
    expiresAt.value = null;
    return;
  }
  const token = localStorage.getItem('token');
  if (!token) {
    status.value = 'No token found. Please login first.';
    router.push('/login');
    return;
  }

  try {
    requesting.value = true;
    status.value = 'Requesting presigned URL...';
    const encodedId = encodeURIComponent(fileId);
    const { data } = await api.get(`/video/presign/${encodedId}`);
    videoUrl.value = data.url;
    expiresAt.value = data.expiresAt;
    status.value = 'Ready to play';
  } catch (err) {
    status.value = err.response?.data?.message || 'Failed to request presigned URL';
    videoUrl.value = '';
    expiresAt.value = null;
  } finally {
    requesting.value = false;
  }
};

const onVideoError = () => {
  status.value = 'The video element failed to load. Try refreshing to get a new URL.';
};

const loadFiles = async () => {
  try {
    const { data } = await api.get('/video/list');
    files.value = data.files;
    if (!files.value.length) {
      status.value = 'No supported video files found in backend/videos.';
      return;
    }
    selectedFile.value = files.value[0].id;
    await nextTick();
    await fetchPresignedUrl(selectedFile.value);
  } catch (err) {
    status.value = err.response?.data?.message || 'Failed to load video catalog';
  }
};

const handleSelection = async () => {
  await fetchPresignedUrl(selectedFile.value);
};

const refreshUrl = async () => {
  await fetchPresignedUrl(selectedFile.value);
};

onMounted(loadFiles);
</script>

<style scoped>
video {
  margin-top: 1rem;
  border-radius: 0.5rem;
  background: #000;
}
.expires {
  margin-top: 0.5rem;
  font-size: 0.9rem;
  color: #374151;
}
.selector {
  display: flex;
  align-items: flex-end;
  gap: 0.75rem;
  margin-bottom: 1rem;
}
select {
  display: block;
  margin-top: 0.35rem;
  padding: 0.4rem 0.5rem;
  border-radius: 0.375rem;
  border: 1px solid #d1d5db;
}
button {
  padding: 0.5rem 0.9rem;
  border-radius: 0.375rem;
  border: none;
  background: #2563eb;
  color: white;
  cursor: pointer;
}
button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
