<script setup>
import { useRouter } from "vue-router";
import BaseCard from "../common/BaseCard.vue";

defineProps({
  posts: { type: Array, default: () => [] },
});

const router = useRouter();

function goToDetail(post) {
  if (post.id === undefined || post.id === null) return;
  router.push(`/board/${post.id}`);
}

function formatDate(iso) {
  if (!iso) return "";
  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(new Date(iso));
}
</script>

<template>
  <BaseCard class="overflow-hidden">
    <table class="w-full text-left text-sm">
      <thead class="bg-black/5 text-ink-700">
        <tr>
          <th class="w-20 px-4 py-3 font-medium">번호</th>
          <th class="px-4 py-3 font-medium">제목</th>
          <th class="w-32 px-4 py-3 font-medium">작성일</th>
        </tr>
      </thead>
      <tbody>
        <tr v-if="posts.length === 0">
          <td colspan="3" class="px-4 py-10 text-center text-ink-500">
            등록된 게시글이 없습니다.
          </td>
        </tr>
        <tr
          v-for="post in posts"
          :key="post.id"
          class="cursor-pointer border-t border-border hover:bg-black/5"
          @click="goToDetail(post)"
        >
          <td class="px-4 py-3 text-ink-500">{{ post.id }}</td>
          <td class="px-4 py-3 text-ink-900">{{ post.title }}</td>
          <td class="px-4 py-3 text-ink-500">{{ formatDate(post.createdAt) }}</td>
        </tr>
      </tbody>
    </table>
  </BaseCard>
</template>
