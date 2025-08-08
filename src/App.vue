<script setup>
import { RouterView, useRoute } from 'vue-router'
import { watch } from 'vue'

const route = useRoute()

const applyLoginBodyStyles = () => {
  // document.body.style.background = 'linear-gradient(to bottom right, #4a69bd, #884ea0)'
  document.body.style.display = 'flex'
  document.body.style.justifyContent = 'center'
  document.body.style.alignItems = 'center'
  document.body.style.flexDirection = 'column'
  document.body.style.fontFamily = '"Montserrat", sans-serif'
  document.body.style.height = '100vh'
  document.body.style.margin = '-20px 0 50px'
}

const removeLoginBodyStyles = () => {
  document.body.style.background = ''
  document.body.style.display = ''
  document.body.style.justifyContent = ''
  document.body.style.alignItems = ''
  document.body.style.flexDirection = ''
  document.body.style.fontFamily = ''
  document.body.style.height = ''
  document.body.style.margin = ''
}

watch(route, (to) => {
  if (to.path === '/login') {
    applyLoginBodyStyles()
  } else {
    removeLoginBodyStyles()
  }
}, { immediate: true })
</script>

<template>
  <RouterView />
</template>

