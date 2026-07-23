<script setup>
import BaseCard from "../common/BaseCard.vue";
import BaseButton from "../common/BaseButton.vue";
import StatusBadge from "./StatusBadge.vue";

defineProps({
  deployments: { type: Array, required: true },
});
defineEmits(["view-detail"]);
</script>

<template>
  <BaseCard class="overflow-hidden">
    <table class="w-full text-left text-sm">
      <thead class="bg-black/5 text-ink-700">
        <tr>
          <th class="px-4 py-3 font-medium">프로젝트 이름</th>
          <th class="px-4 py-3 font-medium">서버 상태</th>
          <th class="px-4 py-3 font-medium">배포 상태</th>
          <th class="px-4 py-3 font-medium">URL</th>
          <th class="px-4 py-3 font-medium">상세보기</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in deployments" :key="item.id" class="border-t border-border">
          <td class="px-4 py-3 text-ink-900">{{ item.projectName }}</td>
          <td class="px-4 py-3">
            <StatusBadge kind="server" :status="item.serverStatus" />
          </td>
          <td class="px-4 py-3">
            <StatusBadge kind="deploy" :status="item.deployStatus" />
          </td>
          <td class="px-4 py-3">
            <a :href="item.url" target="_blank" rel="noreferrer" class="text-primary hover:underline">
              {{ item.url }}
            </a>
          </td>
          <td class="px-4 py-3">
            <BaseButton variant="secondary" @click="$emit('view-detail', item)">
              상세보기
            </BaseButton>
          </td>
        </tr>
      </tbody>
    </table>
  </BaseCard>
</template>
