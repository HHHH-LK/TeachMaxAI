<template>
  <nav id="navbar">
    <ul class="navbar-items flexbox-col">
      <li class="navbar-logo flexbox-left">
        <a class="navbar-item-inner flexbox">
          <!--自定义左侧导航栏logo-->
          <div class="navbar-item-inner-icon-wrapper flexbox">
            <svg t="1751809114317" class="icon" viewBox="0 0 1024 1024" version="1.1" xmlns="http://www.w3.org/2000/svg"
                 p-id="19585" width="200" height="200">
              <path
                  d="M512.008096 981.174059a469.175163 469.175163 0 1 1 469.175163-469.175163 469.708735 469.708735 0 0 1-469.175163 469.175163z m0-919.9513a450.776137 450.776137 0 1 0 450.776137 450.776137 451.29131 451.29131 0 0 0-450.776137-450.776137z"
                  p-id="19586"></path>
              <path
                  d="M209.813293 597.922347a9.199513 9.199513 0 0 1-9.199513-9.199513v-153.447876a9.199513 9.199513 0 0 1 18.399026 0v153.447876a9.199513 9.199513 0 0 1-9.199513 9.199513zM285.359694 580.323679a9.199513 9.199513 0 0 1-9.199513-9.199513V452.892025a9.199513 9.199513 0 0 1 18.399026 0v118.241341a9.199513 9.199513 0 0 1-9.199513 9.199513zM360.896895 677.847716a9.199513 9.199513 0 0 1-9.199513-9.199513V355.358788a9.199513 9.199513 0 0 1 18.399026 0v313.289415a9.199513 9.199513 0 0 1-9.199513 9.199513zM436.452495 778.959564a9.199513 9.199513 0 0 1-9.199513-9.199513V254.237741a9.199513 9.199513 0 0 1 18.399026 0v515.52231a9.199513 9.199513 0 0 1-9.199513 9.199513zM512.008096 745.004161a9.199513 9.199513 0 0 1-9.199513-9.199513V288.202343a9.199513 9.199513 0 0 1 18.399026 0v447.602305a9.199513 9.199513 0 0 1-9.199513 9.199513zM587.545297 657.921571a9.199513 9.199513 0 0 1-9.199513-9.199513V375.284933a9.199513 9.199513 0 0 1 18.399026 0v273.437125a9.199513 9.199513 0 0 1-9.199513 9.199513zM663.100897 616.477765a9.199513 9.199513 0 0 1-9.199513-9.199513V416.71954a9.199513 9.199513 0 0 1 18.399026 0v190.558712a9.199513 9.199513 0 0 1-9.199513 9.199513zM738.638098 680.156794a9.199513 9.199513 0 0 1-9.199513-9.199513V353.040511a9.199513 9.199513 0 1 1 18.399026 0v317.91677a9.199513 9.199513 0 0 1-9.199513 9.199513zM814.184499 581.179234a9.199513 9.199513 0 0 1-9.199513-9.199513v-119.96165a9.199513 9.199513 0 0 1 18.399026 0v119.96165a9.199513 9.199513 0 0 1-9.199513 9.199513z"
                  p-id="19587"></path>
            </svg>
          </div>
          <span class="link-text">Teach Max AI</span>
        </a>
      </li>
      <li
          v-for="item in menuItems"
          :key="item.id"
          class="navbar-item flexbox-left"
          @click="handleItemClick(item)"
      >
        <a class="navbar-item-inner flexbox-left">
          <div class="navbar-item-inner-icon-wrapper flexbox">
            <ion-icon :name="item.icon"></ion-icon>
          </div>
          <span class="link-text">{{ item.label }}</span>
        </a>
      </li>
    </ul>
  </nav>
</template>

<script setup>
import {ref, onMounted} from 'vue'

// Props
const props = defineProps({
  items: {
    type: Array,
    default: () => [
      {id: 'dashboard', label: '大屏概览', icon: 'tv-outline'},
      {id: 'user-management', label: '用户管理', icon: 'people-outline'},
      {id: 'resource-management', label: '课件资源管理', icon: 'library-outline'}
    ]
  } 
})

// Emits
const emit = defineEmits(['item-click'])

// Data
const menuItems = ref(props.items)

// Methods
const handleItemClick = (item) => {
  emit('item-click', item)
}

// Lifecycle
onMounted(() => {
  // 动态加载 Ionicons
  if (!document.querySelector('script[src*="ionicons"]')) {
    const script = document.createElement('script')
    script.src = 'https://unpkg.com/ionicons@7.1.0/dist/ionicons/ionicons.esm.js'
    script.type = 'module'
    document.head.appendChild(script)

    const script2 = document.createElement('script')
    script2.src = 'https://unpkg.com/ionicons@7.1.0/dist/ionicons/ionicons.js'
    script2.noModule = true
    document.head.appendChild(script2)
  }
})
</script>

<style scoped lang="less">
#navbar {
  top: 0;
  padding: 0;
  width: 5em;
  height: 100vh;
  position: fixed;
  background-color: hsl(256, 12%, 12%);
  transition: width .35s cubic-bezier(.175, .685, .32, 1);
  overflow-y: auto;
  overflow-x: hidden;
  z-index: 1000;

  &:hover {
    width: 16em;
  }

  &::-webkit-scrollbar-track {
    background-color: hsl(256, 12%, 12%);
  }

  &::-webkit-scrollbar {
    width: 8px;
    background-color: hsl(256, 12%, 12%);
  }

  &::-webkit-scrollbar-thumb {
    background-color: hsl(237, 94%, 81%);
  }
}

.navbar-items {
  margin: 0;
  padding: 0;
  list-style-type: none;
}

.navbar-logo {
  margin: 0 0 2em 0;
  width: 100%;
  height: 5em;
  background: hsl(256, 10%, 10%);

  .navbar-item-inner {
    padding: 1em 0;
    width: 100%;
    position: relative;
    color: hsl(0, 0%, 50%);
    border-radius: .25em;
    text-decoration: none;
    transition: all .2s cubic-bezier(.175, .685, .32, 1);

    &:hover {
      color: hsl(0, 0%, 100%);
      background: hsl(257, 11%, 16%);
      box-shadow: 0 17px 30px -10px hsla(0, 0%, 0%, .25);
    }

    .navbar-item-inner-icon-wrapper {
      width: calc(5rem - 1em - 8px);
      position: relative;

      svg {
        height: 2em;
        fill: hsl(0, 0%, 100%);
        position: absolute;
      }
    }
  }
}

.navbar-item {
  padding: 0 .5em;
  width: 100%;
  cursor: pointer;

  .navbar-item-inner {
    padding: 1em 0;
    width: 100%;
    position: relative;
    color: hsl(0, 0%, 50%);
    border-radius: .25em;
    text-decoration: none;
    transition: all .2s cubic-bezier(.175, .685, .32, 1);

    &:hover {
      color: hsl(0, 0%, 100%);
      background: hsl(257, 11%, 16%);
      box-shadow: 0 17px 30px -10px hsla(0, 0%, 0%, .25);
    }

    .navbar-item-inner-icon-wrapper {
      width: calc(5rem - 1em - 8px);
      position: relative;

      ion-icon {
        position: absolute;
        font-size: calc(2.4rem - 1rem);
      }
    }
  }
}

.link-text {
  margin: 0;
  width: 0;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: all .35s cubic-bezier(.175, .685, .32, 1);
  overflow: hidden;
  opacity: 0;
}

#navbar:hover .link-text {
  width: calc(100% - calc(5rem - 8px));
  opacity: 1;
}

.flexbox {
  display: flex;
  justify-content: center;
  align-items: center;
}

.flexbox-left {
  display: flex;
  justify-content: flex-start;
  align-items: center;
}

.flexbox-col {
  display: flex;
  justify-content: center;
  flex-direction: column;
  align-items: center;
}

@media only screen and (max-width: 1660px) {
  .navbar-logo .navbar-item-inner .navbar-item-inner-icon-wrapper {
    width: calc(4rem - 1em - 8px);
  }
}

@media only screen and (max-width: 1456px) {
  .navbar-logo .navbar-item-inner .navbar-item-inner-icon-wrapper {
    width: calc(3.8rem - 1em - 8px);
  }
}

@media only screen and (max-width: 1024px) {
  .navbar-logo .navbar-item-inner .navbar-item-inner-icon-wrapper {
    width: calc(3.5rem - 1em - 8px);
  }
}

@media only screen and (max-width: 756px) {
  .navbar-logo .navbar-item-inner .navbar-item-inner-icon-wrapper {
    width: calc(3rem - 1em - 8px);
  }
}
</style>