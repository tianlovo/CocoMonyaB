<template>
  <router-view v-slot="{ Component }">
    <keep-alive :include="['AuthorView', 'WorkView', 'CharacterView']">
      <component :is="Component" />
    </keep-alive>
  </router-view>
</template>

<script setup lang="ts">
</script>

<style>
#app {
  width: 100%;
  height: 100vh;
  margin: 0;
  padding: 0;
}
</style>
