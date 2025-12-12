import { createRouter, createWebHistory } from 'vue-router';
import LoginPage from '../pages/LoginPage.vue';
import VideoPlayer from '../pages/VideoPlayer.vue';

const routes = [
  { path: '/', redirect: '/login' },
  { path: '/login', component: LoginPage },
  { path: '/player', component: VideoPlayer },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

export default router;
