<template>
  <div class="tower-selector">
    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <div class="loading-spinner"></div>
      <p class="loading-text">正在加载塔数据...</p>
    </div>

    <!-- 错误提示 -->
    <div v-else-if="error" class="error-container">
      <p class="error-text">加载失败: {{ error }}</p>
      <button @click="fetchTowerData" class="retry-button">重试</button>
    </div>

    <!-- 塔选择器 -->
    <div v-else class="tower-picker">
      <div class="picker-container">
        <div class="picker-track" ref="pickerTrack">
          <div
            v-for="(tower, index) in towers"
            :key="index"
            class="tower-option"
            :class="{ active: selectedTowerIndex === index }"
            @click="selectTower(index)"
          >
            <div class="tower-number">{{ tower.name }}</div>
            <div class="tower-preview" v-if="index > 0">
              <canvas
                :ref="`towerCanvas${index}`"
                :width="60"
                :height="80"
                class="tower-canvas"
              ></canvas>
            </div>

          </div>
        </div>
      </div>
    </div>

    <!-- 塔展示区域 -->
    <div class="tower-display" v-if="selectedTowerIndex > 0">
      <!-- 调试信息 -->
      <div class="debug-info" style="color: white; margin-bottom: 10px; font-size: 12px;">
        当前塔层数量: {{ towerLayers.length }} | 已解锁: {{ towerLayers.filter(l => l.unlocked).length }} | 未解锁: {{ towerLayers.filter(l => !l.unlocked).length }}
      </div>
      <div class="display-container">


        <div class="navigation-buttons">
          <button
              class="nav-btn nav-left"
              @click="navigateTower(-1)"
              :disabled="selectedTowerIndex <= 1"
          >
            <svg viewBox="0 0 24 24" class="nav-icon">
              <path d="M15.41 7.41L14 6l-6 6 6 6 1.41-1.41L10.83 12z"/>
            </svg>
          </button>

          <button
              class="nav-btn nav-right"
              @click="navigateTower(1)"
              :disabled="selectedTowerIndex >= towers.length - 1"
          >
            <svg viewBox="0 0 24 24" class="nav-icon">
              <path d="M10 6L8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z"/>
            </svg>
          </button>
        </div>

        <div class="tower-parts">
          <!-- 塔顶 -->
          <div class="tower-part tower-top">
            <img :src="towerTopImg" alt="塔顶" />
          </div>

          <!-- 塔层 - 可滚动 -->
           <div class="tower-layers" ref="towerLayers">
             <div
               v-for="(layer, index) in towerLayers"
               :key="layer.id"
               class="tower-layer"
               :class="{ 'unlocked': layer.unlocked }"
             >
               <img :src="towerLayerImg" alt="塔层" />
               <div class="tower-lock" v-if="!layer.unlocked">
                 <svg viewBox="0 0 24 24" class="lock-icon">
                   <path d="M12 1C8.676 1 6 3.676 6 7v2H5c-1.103 0-2 .897-2 2v9c0 1.103.897 2 2 2h14c1.103 0 2-.897 2-2v-9c0-1.103-.897-2-2-2h-1V7c0-3.324-2.676-6-6-6zm6 10.723V20H5v-8.277h1V7c0-2.761 2.239-5 5-5s5 2.239 5 5v3.723h2z"/>
                   <path d="M12 12c-1.103 0-2 .897-2 2v3c0 1.103.897 2 2 2s2-.897 2-2v-3c0-1.103-.897-2-2-2z"/>
                 </svg>
               </div>
               <div class="tower-info" v-if="layer.description">
                 <div class="floor-number">第{{ layer.level }}层</div>
               </div>
             </div>
           </div>

          <!-- 塔底 -->
          <div class="tower-part tower-bottom">
            <img :src="towerBottomImg" alt="塔底" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import towerTopImg from '../assets/塔顶.png'
import towerLayerImg from '../assets/塔层.png'
import towerBottomImg from '../assets/塔底.png'
import { gameService } from '@/services/game.js'
import userConfig from "@/config/userConfig.js";

export default {
  name: 'TowerSelector',
  data() {
    return {
      selectedTowerIndex: 0,
      towers: [
        { name: '', level: 0 } // 第一个空选项保持不变
      ],
      towerLayers: [],
      towerTopImg,
      towerLayerImg,
      towerBottomImg,
      loading: false,
      error: null,
      currentTowerFloors: [] // 当前选择塔的塔层信息
    }
  },

  mounted() {
    this.fetchTowerData()
    this.initTowerLayers()
    this.setupScroll()
    // 不自动恢复塔选择状态，保持默认未选择状态
  },

  beforeUnmount() {
    // 组件卸载前可以选择是否清除保存的状态
    // 这里我们保留状态，让用户下次访问时能看到之前的选择
    // 如果需要清除，可以调用 this.clearTowerSelection()
  },

  methods: {
    // 初始化塔层
    initTowerLayers() {
      // 默认创建20层，后续会根据实际塔层信息更新
      this.towerLayers = Array.from({ length: 20 }, (_, i) => ({
        id: i,
        level: i + 1,
        unlocked: false,
        description: ''
      }))
    },

    // 渲染塔预览图
    renderTowerPreviews() {
      this.$nextTick(() => {
        this.towers.forEach((tower, index) => {
          if (index > 0) {
            this.renderTowerPreview(index, tower.level)
          }
        })
      })
    },

    // 渲染单个塔预览
    renderTowerPreview(towerIndex, level) {
      const canvas = this.$refs[`towerCanvas${towerIndex}`]
      if (!canvas || !canvas[0]) return

      const ctx = canvas[0].getContext('2d')
      const width = canvas[0].width
      const height = canvas[0].height

      // 清空画布
      ctx.clearRect(0, 0, width, height)

      // 创建更暗的渐变背景
      const gradient = ctx.createLinearGradient(0, 0, 0, height)
      gradient.addColorStop(0, '#1a0f08')
      gradient.addColorStop(0.3, '#2c1810')
      gradient.addColorStop(0.7, '#1a0f08')
      gradient.addColorStop(1, '#0a0504')

      ctx.fillStyle = gradient
      ctx.fillRect(0, 0, width, height)

      // 添加诡异的纹理
      for (let i = 0; i < 20; i++) {
        ctx.strokeStyle = `rgba(139, 0, 0, ${0.1 + Math.random() * 0.2})`
        ctx.lineWidth = 0.5
        ctx.beginPath()
        ctx.moveTo(Math.random() * width, Math.random() * height)
        ctx.lineTo(Math.random() * width, Math.random() * height)
        ctx.stroke()
      }

      // 绘制塔的轮廓 - 更加扭曲
      ctx.strokeStyle = '#8b0000'
      ctx.lineWidth = 2
      ctx.beginPath()
      ctx.moveTo(width * 0.2, height * 0.1)
      ctx.lineTo(width * 0.8, height * 0.1)
      ctx.lineTo(width * 0.9, height * 0.3)
      ctx.lineTo(width * 0.85, height * 0.5)
      ctx.lineTo(width * 0.9, height * 0.7)
      ctx.lineTo(width * 0.9, height * 0.9)
      ctx.lineTo(width * 0.1, height * 0.9)
      ctx.lineTo(width * 0.1, height * 0.7)
      ctx.lineTo(width * 0.15, height * 0.5)
      ctx.lineTo(width * 0.1, height * 0.3)
      ctx.closePath()
      ctx.stroke()

      // 添加内部阴影
      ctx.fillStyle = 'rgba(0, 0, 0, 0.6)'
      ctx.fill()

      // 添加血红色符文
      ctx.fillStyle = '#ff0000'
      ctx.font = 'bold 8px Courier'
      ctx.textAlign = 'center'
      ctx.shadowColor = '#ff0000'
      ctx.shadowBlur = 8
      ctx.fillText(`${level}`, width / 2, height * 0.6)

      // 添加诡异的眼睛效果
      ctx.fillStyle = '#ff0000'
      ctx.shadowBlur = 5
      ctx.beginPath()
      ctx.arc(width * 0.3, height * 0.4, 2, 0, Math.PI * 2)
      ctx.arc(width * 0.7, height * 0.4, 2, 0, Math.PI * 2)
      ctx.fill()

      // 添加血滴效果
      ctx.fillStyle = 'rgba(255, 0, 0, 0.7)'
      ctx.shadowBlur = 0
      ctx.beginPath()
      ctx.ellipse(width * 0.5, height * 0.8, 3, 1, 0, 0, Math.PI * 2)
      ctx.fill()

      // 添加闪电效果
      ctx.strokeStyle = '#ff0000'
      ctx.lineWidth = 1
      ctx.shadowColor = '#ff0000'
      ctx.shadowBlur = 3
      ctx.beginPath()
      ctx.moveTo(width * 0.2, height * 0.2)
      ctx.lineTo(width * 0.4, height * 0.3)
      ctx.lineTo(width * 0.3, height * 0.5)
      ctx.lineTo(width * 0.6, height * 0.6)
      ctx.stroke()

      ctx.shadowBlur = 0
    },

    // 选择塔
    async selectTower(index) {
      // 更新选择状态
      this.selectedTowerIndex = index
      
      if (index === 0) {
        // 第一个选项（空选项）：只更新选择状态，不触发动画
        this.$emit('tower-selected', this.towers[index])
        
        // 清除保存的塔选择状态
        this.clearTowerSelection()
        
        // 滚动到对应位置
        this.scrollTowerPickerToIndex(index)
        
        console.log('选择了空选项，清除塔选择状态')
        return
      }
      
      // 其他塔选项：触发完整的选择流程
      this.$emit('tower-selected', this.towers[index])
      
      // 获取塔层信息
      await this.fetchTowerFloors(this.towers[index].towerId)
      
      // 保存塔选择状态到 localStorage
      this.saveTowerSelection(index)
      
      // 滚动到对应位置
      this.scrollTowerPickerToIndex(index)
      
      // 使用路由跳转到转场动画页面
      const params = {
        towerIndex: index,
        towerName: this.towers[index].name,
        towerLevel: this.towers[index].level,
        towerId: this.towers[index].towerId,
        courseId: this.towers[index].courseId,
        fullStory: this.towers[index].story || `在遥远的未来，人类文明发展到了一个全新的高度，科技与智慧的结晶催生了一座神秘的高塔"永恒之塔"。这座塔并非由实体砖石建成，而是由无数虚拟系统和数据结构交织而成，象征着人类对完美设计与无穷知识的追求。塔的每一层都蕴含着特定的知识领域，唯有通过理解与掌握该层的核心理念，才能继续向上攀登。传说中，塔的顶层隐藏着一种能够解决所有人类问题的终极模型--"万物蓝图"。然而，进入塔中的人很快发现，这里的挑战并不在于体力或武器，而在于逻辑思维与系统设计能力。每一位挑战者都需要运用UML(统一建模语言)来解析塔中复杂的规则与结构:从用例图描绘塔的功能需求，到类图揭示其内部逻辑。从时序图模拟动态交互，到状态图追踪变化规律。随着挑战者不断深入，他们逐渐意识到，"永恒之塔"不仅是对个人能力的试炼场，更是一面镜子,映射出他们在现实世界中对系统分析与设计的理解深度。只有真正领悟UML建模精髓的人，才能解开层层谜题，最终触及那传说中的"万物蓝图"，为人类开启新的纪元篇章。`
      }
      
      console.log('准备跳转，传递的参数:', params)
      
      // 尝试使用 path + query 的方式传递参数
      this.$router.push({
        path: '/tower-transition',
        query: params
      })
    },

    // 导航到其他塔
    navigateTower(direction) {
      const newIndex = this.selectedTowerIndex + direction
      if (newIndex >= 0 && newIndex < this.towers.length) {
        // 调用 selectTower 方法，包括第一个空选项
        this.selectTower(newIndex)
      }
    },

    // 滚动塔选择器到指定索引位置
    scrollTowerPickerToIndex(index) {
      const pickerTrack = this.$refs.pickerTrack
      if (pickerTrack) {
        const towerOption = pickerTrack.children[index]
        if (towerOption) {
          const containerWidth = pickerTrack.offsetWidth
          const optionWidth = towerOption.offsetWidth
          const optionLeft = towerOption.offsetLeft
          const scrollPosition = optionLeft - (containerWidth / 2) + (optionWidth / 2)
          
          pickerTrack.scrollTo({
            left: scrollPosition,
            behavior: 'smooth'
          })
        }
      }
    },

    // 获取塔数据
    async fetchTowerData() {
      this.loading = true
      this.error = null
      
      try {
        const studentId = userConfig.studentId
        const response = await gameService.tower.getTowerByStudentId(studentId)
        console.log('获取塔数据响应:', response)
        
        if (response.data && response.data.code === 0 && response.data.data) {
          // 转换API数据为组件需要的格式
          const apiTowers = response.data.data
          this.towers = [
            { name: '', level: 0 }, // 第一个空选项保持不变
            ...apiTowers.map((tower, index) => ({
              name: tower.name || `塔${index + 1}`,
              level: tower.totalFloors || index + 1,
              towerId: tower.towerId,
              courseId: tower.courseId,
              totalFloors: tower.totalFloors,
              story: '' // 初始化故事字段
            }))
          ]
          
          console.log('转换后的塔数据:', this.towers)
          
          // 为每个塔获取故事信息
          await this.fetchTowerStories()
          
          // 数据加载完成后，渲染预览图，并检查是否需要恢复选择状态
          this.$nextTick(() => {
            this.renderTowerPreviews()
            // 检查是否有之前保存的选择状态，如果有则恢复
            this.checkAndRestoreSelection()
          })
        } else {
          throw new Error('API返回数据格式错误')
        }
      } catch (error) {
        console.error('获取塔数据失败:', error)
        this.error = error.message
        
        // 如果API调用失败，使用默认数据
        this.towers = [
          { name: '', level: 0 },
          { name: 'A塔', level: 1 },
          { name: 'B塔', level: 2 },
          { name: 'C塔', level: 3 },
          { name: 'D塔', level: 4 },
          { name: 'E塔', level: 5 },
          { name: 'F塔', level: 6 },
          { name: 'G塔', level: 7 },
          { name: 'H塔', level: 8 }
        ]
        
        this.$nextTick(() => {
          this.renderTowerPreviews()
          // 检查是否有之前保存的选择状态，如果有则恢复
          this.checkAndRestoreSelection()
        })
      } finally {
        this.loading = false
      }
    },

    // 获取塔层信息
    async fetchTowerFloors(towerId) {
      try {
        if (!towerId) return
        
        const floorResponse = await gameService.tower.getTowerFloorByTowerId(towerId)
        console.log(`塔${towerId}塔层响应:`, floorResponse)
        
        if (floorResponse.data && floorResponse.data.code === 0 && floorResponse.data.data) {
          const floors = floorResponse.data.data
          
          // 更新塔层信息
          this.currentTowerFloors = floors
          
          // 更新塔层显示
          this.updateTowerLayers(floors)
          
          console.log(`塔${towerId}塔层信息:`, this.currentTowerFloors)
        } else {
          console.warn(`塔${towerId}没有塔层信息`)
          this.currentTowerFloors = []
        }
      } catch (error) {
        console.error(`获取塔${towerId}塔层信息失败:`, error)
        this.currentTowerFloors = []
      }
    },

    // 更新塔层显示
    updateTowerLayers(floors) {
      if (!floors || floors.length === 0) return
      
      console.log('更新塔层显示，原始数据:', floors)
      
      // 根据实际塔层数量更新，并按照层数从高到低排序
      this.towerLayers = floors
        .map(floor => ({
          id: floor.floorId,
          level: floor.floorNo,
          unlocked: floor.unlocked,
          description: floor.description,
          floorId: floor.floorId,
          towerId: floor.towerId
        }))
        .sort((a, b) => b.level - a.level) // 从高到低排序（降序）
      
      console.log('更新后的塔层数据（已排序）:', this.towerLayers)
      
      // 强制重新渲染
      this.$nextTick(() => {
        this.$forceUpdate()
      })
    },

    // 获取塔故事信息
    async fetchTowerStories() {
      try {
        // 为每个有towerId的塔获取故事信息
        const storyPromises = this.towers
          .filter(tower => tower.towerId) // 过滤掉第一个空选项
          .map(async (tower) => {
            try {
              const storyResponse = await gameService.tower.getTowerStoryByTowerId(tower.towerId)
              console.log(`塔${tower.towerId}故事响应:`, storyResponse)
              
              if (storyResponse.data && storyResponse.data.code === 0 && storyResponse.data.data) {
                // 更新塔的故事信息
                tower.story = storyResponse.data.data
                console.log(`塔${tower.towerId}故事内容:`, tower.story)
              } else {
                tower.story = '暂无故事信息'
              }
            } catch (error) {
              console.error(`获取塔${tower.towerId}故事失败:`, error)
              tower.story = '故事加载失败'
            }
          })
        
        // 等待所有故事请求完成
        await Promise.all(storyPromises)
        console.log('所有塔故事获取完成:', this.towers)
        
      } catch (error) {
        console.error('获取塔故事信息失败:', error)
        // 如果获取故事失败，为所有塔设置默认故事
        this.towers.forEach(tower => {
          if (tower.towerId) {
            tower.story = '故事加载失败，使用默认故事'
          }
        })
      }
    },

    // 保存塔选择状态
    saveTowerSelection(index) {
      try {
        localStorage.setItem('selectedTowerIndex', index.toString())
        console.log('塔选择状态已保存:', index)
      } catch (error) {
        console.error('保存塔选择状态失败:', error)
      }
    },

    // 检查并恢复塔选择状态
    checkAndRestoreSelection() {
      try {
        const savedIndex = localStorage.getItem('selectedTowerIndex')
        if (savedIndex !== null) {
          const index = parseInt(savedIndex)
          if (index > 0 && index < this.towers.length) {
            // 恢复选择状态
            this.selectedTowerIndex = index
            console.log('塔选择状态已恢复:', index)
            
            // 恢复后获取塔层信息
            const tower = this.towers[index]
            if (tower && tower.towerId) {
              console.log('恢复选择状态后获取塔层信息:', tower.towerId)
              this.fetchTowerFloors(tower.towerId)
            }
            
            // 恢复后滚动到对应位置
            this.$nextTick(() => {
              this.scrollTowerPickerToIndex(index)
            })
          } else {
            // 保存的索引无效，清除保存的状态
            console.log('保存的塔索引无效，清除状态')
            this.clearTowerSelection()
          }
        } else {
          // 没有保存的状态，保持默认未选择状态
          console.log('没有保存的塔选择状态，保持默认状态')
        }
      } catch (error) {
        console.error('检查塔选择状态失败:', error)
        // 出错时清除保存的状态
        this.clearTowerSelection()
      }
    },

    // 恢复塔选择状态（保留原方法以兼容）
    restoreTowerSelection() {
      this.checkAndRestoreSelection()
    },

    // 清除保存的塔选择状态
    clearTowerSelection() {
      try {
        localStorage.removeItem('selectedTowerIndex')
        console.log('塔选择状态已清除')
      } catch (error) {
        console.error('清除塔选择状态失败:', error)
      }
    },

    // 设置滚动
    setupScroll() {
      // 塔选择器的左右滚动
      const pickerTrack = this.$refs.pickerTrack
      if (pickerTrack) {
        let isDown = false
        let startX
        let scrollLeft

        pickerTrack.addEventListener('mousedown', (e) => {
          isDown = true
          startX = e.pageX - pickerTrack.offsetLeft
          scrollLeft = pickerTrack.scrollLeft
        })

        pickerTrack.addEventListener('mouseleave', () => {
          isDown = false
        })

        pickerTrack.addEventListener('mouseup', () => {
          isDown = false
        })

        pickerTrack.addEventListener('mousemove', (e) => {
          if (!isDown) return
          e.preventDefault()
          const x = e.pageX - pickerTrack.offsetLeft
          const walk = (x - startX) * 2
          pickerTrack.scrollLeft = scrollLeft - walk
        })
      }

      // 塔层的上下滚动
      const towerLayers = this.$refs.towerLayers
      if (towerLayers) {
        let isDown = false
        let startY
        let scrollTop

        towerLayers.addEventListener('mousedown', (e) => {
          isDown = true
          startY = e.pageY - towerLayers.offsetTop
          scrollTop = towerLayers.scrollTop
        })

        towerLayers.addEventListener('mouseleave', () => {
          isDown = false
        })

        towerLayers.addEventListener('mouseup', () => {
          isDown = false
        })

        towerLayers.addEventListener('mousemove', (e) => {
          if (!isDown) return
          e.preventDefault()
          const y = e.pageY - towerLayers.offsetTop
          const walk = (y - startY) * 2
          towerLayers.scrollTop = scrollTop - walk
        })
      }
    }
  }
}
</script>

<style lang="less" scoped>
.tower-selector {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 65%;
  max-width: 600px;
  z-index: 10;
}

.tower-picker {
  margin-bottom: 30px;

  .picker-container {
    background: rgba(0, 0, 0, 0.9);
    border: 2px solid #8b0000;
    border-radius: 15px;
    padding: 20px;
    box-shadow: 0 0 30px rgba(139, 0, 0, 0.8), inset 0 0 20px rgba(0, 0, 0, 0.5);
    position: relative;
    animation: shadowFlicker 4s ease-in-out infinite;

    &::before {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background:
        radial-gradient(circle at 20% 20%, rgba(139, 0, 0, 0.3) 0%, transparent 50%),
        radial-gradient(circle at 80% 80%, rgba(0, 0, 0, 0.4) 0%, transparent 50%);
      border-radius: 15px;
      pointer-events: none;
    }
  }

  .picker-track {
    display: flex;
    gap: 20px;
    overflow-x: auto;
    scrollbar-width: none;
    -ms-overflow-style: none;

    &::-webkit-scrollbar {
      display: none;
    }

    scroll-behavior: smooth;
  }

  .tower-option {
    display: flex;
    flex-direction: column;
    align-items: center;
    cursor: pointer;
    transition: all 0.3s ease;
    min-width: 70px;
    position: relative;



    &::before {
      content: '';
      position: absolute;
      top: -5px;
      left: -5px;
      right: -5px;
      bottom: -5px;
      background: radial-gradient(circle, rgba(139, 0, 0, 0.2) 0%, transparent 70%);
      border-radius: 10px;
      opacity: 0;
      transition: opacity 0.3s ease;
      pointer-events: none;
    }

    &:hover {
      transform: translateY(-5px);

      &::before {
        opacity: 1;
      }
    }

    &.active {
      transform: scale(1.1);

      &::before {
        opacity: 1;
        background: radial-gradient(circle, rgba(139, 0, 0, 0.4) 0%, transparent 70%);
      }

      .tower-number {
        color: #ff0000;
        text-shadow: 0 0 15px #ff0000, 0 0 25px #8b0000;
        animation: textPulse 2s ease-in-out infinite;
      }
    }
  }

  .tower-number {
    color: #fff;
    font-size: 16px;
    font-weight: bold;
    margin-bottom: 10px;
    text-shadow: 0 0 5px rgba(255, 255, 255, 0.5);
    transition: all 0.3s ease;
    font-family: 'Courier New', monospace;
    letter-spacing: 1px;
  }

  .tower-preview {
    width: 80px;
    height: 100px;
    border: 2px solid #8b0000;
    border-radius: 8px;
    overflow: hidden;
    background: linear-gradient(135deg, #1a0f08, #2c1810);
    position: relative;

    &::after {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background:
        radial-gradient(circle at 30% 30%, rgba(139, 0, 0, 0.3) 0%, transparent 50%),
        linear-gradient(45deg, transparent 40%, rgba(0, 0, 0, 0.4) 50%, transparent 60%);
      pointer-events: none;
    }

    .tower-canvas {
      display: block;
      width: 100%;
      height: 100%;
      filter: contrast(1.2) brightness(0.8);
    }
  }
}

.tower-display {
  .display-container {
    background: rgba(0, 0, 0, 0.95);
    border: 3px solid #8b0000;
    border-radius: 20px;
    padding: 25px;
    box-shadow: 0 0 40px rgba(139, 0, 0, 0.9), inset 0 0 30px rgba(0, 0, 0, 0.7);
    position: relative;

    &::before {
      content: '';
      position: absolute;
      top: -2px;
      left: -2px;
      right: -2px;
      bottom: -2px;
      background: linear-gradient(45deg, #8b0000, #ff0000, #8b0000);
      border-radius: 20px;
      z-index: -1;
      animation: borderGlow 2s ease-in-out infinite;
    }

    &::after {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background:
        radial-gradient(circle at 20% 20%, rgba(139, 0, 0, 0.2) 0%, transparent 50%),
        radial-gradient(circle at 80% 80%, rgba(0, 0, 0, 0.3) 0%, transparent 50%);
      border-radius: 20px;
      pointer-events: none;
    }
  }

  // 导航按钮样式
  .navigation-buttons {
    position: absolute;
    top: 50%;
    left: 0;
    right: 0;
    transform: translateY(-50%);
    display: flex;
    justify-content: space-between;
    pointer-events: none;
    z-index: 20;
  }

  .nav-btn {
    width: 50px;
    height: 50px;
    border: 2px solid #8b0000;
    border-radius: 50%;
    background: 
      radial-gradient(circle at 30% 30%, rgba(139, 0, 0, 0.4) 0%, transparent 50%),
      radial-gradient(circle at 70% 70%, rgba(0, 0, 0, 0.6) 0%, transparent 50%),
      linear-gradient(135deg, #1a0f08 0%, #2c1810 50%, #1a0f08 100%);
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
    pointer-events: auto;
    position: relative;
    box-shadow: 
      0 0 20px rgba(139, 0, 0, 0.6),
      inset 0 0 15px rgba(0, 0, 0, 0.8),
      0 4px 8px rgba(0, 0, 0, 0.6);
    
    // 添加神秘符文背景
    &::before {
      content: '';
      position: absolute;
      top: -2px;
      left: -2px;
      right: -2px;
      bottom: -2px;
      background: 
        conic-gradient(from 0deg, #8b0000, #ff0000, #8b0000, #660000, #8b0000);
      border-radius: 50%;
      z-index: -1;
      opacity: 0;
      transition: opacity 0.4s ease;
      animation: borderRotate 8s linear infinite;
    }

    // 添加内部纹理
    &::after {
      content: '';
      position: absolute;
      top: 2px;
      left: 2px;
      right: 2px;
      bottom: 2px;
      background: 
        radial-gradient(circle at 20% 20%, rgba(255, 0, 0, 0.1) 0%, transparent 40%),
        radial-gradient(circle at 80% 80%, rgba(0, 0, 0, 0.3) 0%, transparent 40%),
        repeating-conic-gradient(from 0deg, transparent 0deg, rgba(139, 0, 0, 0.1) 10deg, transparent 20deg);
      border-radius: 50%;
      pointer-events: none;
    }

    &:hover {
      transform: scale(1.15) rotate(5deg);
      border-color: #ff0000;
      box-shadow: 
        0 0 40px rgba(255, 0, 0, 0.9),
        inset 0 0 25px rgba(255, 0, 0, 0.3),
        0 8px 16px rgba(0, 0, 0, 0.8);

      &::before {
        opacity: 1;
        animation: borderRotate 4s linear infinite;
      }

      .nav-icon {
        fill: #ff0000;
        filter: drop-shadow(0 0 15px rgba(255, 0, 0, 0.9));
        animation: iconPulse 1.5s ease-in-out infinite;
      }
    }

    &:active {
      transform: scale(0.95);
      box-shadow: 
        0 0 20px rgba(255, 0, 0, 0.7),
        inset 0 0 20px rgba(0, 0, 0, 0.9);
    }

    &:disabled {
      opacity: 0.3;
      cursor: not-allowed;
      border-color: #444;
      box-shadow: none;
      background: 
        radial-gradient(circle at 30% 30%, rgba(68, 68, 68, 0.2) 0%, transparent 50%),
        linear-gradient(135deg, #1a1a1a 0%, #2a2a2a 50%, #1a1a1a 100%);

      &:hover {
        transform: none;
        border-color: #444;
        box-shadow: none;

        &::before {
          opacity: 0;
        }

        .nav-icon {
          fill: #666;
          filter: none;
          animation: none;
        }
      }
    }

    .nav-icon {
      width: 24px;
      height: 24px;
      fill: #8b0000;
      transition: all 0.4s ease;
      filter: drop-shadow(0 0 8px rgba(139, 0, 0, 0.8));
      position: relative;
      z-index: 2;
    }
  }

  .tower-parts {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 0;
    position: relative;
  }

  .tower-part {
    img {
      width: 180px;
      height: auto;
      filter: drop-shadow(0 0 15px rgba(139, 0, 0, 0.9)) contrast(1.1) brightness(0.9);
      transition: all 0.3s ease;

      &:hover {
        filter: drop-shadow(0 0 25px rgba(255, 0, 0, 0.8)) contrast(1.2) brightness(1.1);
        transform: scale(1.05);
      }
    }
  }

  .tower-layers {
    max-height: 280px;
    overflow-y: auto;
    scrollbar-width: thin;
    scrollbar-color: #8b0000 #1a0f08;
    display: flex;
    flex-direction: column;
    align-items: center;
    margin-left: 15px; /* 向右移动塔层更多 */

    &::-webkit-scrollbar {
      width: 6px;
    }

    &::-webkit-scrollbar-track {
      background: #1a0f08;
      border-radius: 3px;
    }

    &::-webkit-scrollbar-thumb {
      background: #8b0000;
      border-radius: 3px;

      &:hover {
        background: #ff0000;
      }
    }

    .tower-layer {
     margin: 0;
     position: relative;
     display: flex;
     justify-content: center;

     &::before {
       content: '';
       position: absolute;
       top: 0;
       left: 0;
       right: 0;
       bottom: 0;
       background: linear-gradient(90deg, transparent 0%, rgba(139, 0, 0, 0.1) 50%, transparent 100%);
       opacity: 0;
       transition: opacity 0.3s ease;
       pointer-events: none;
     }

     &:hover::before {
       opacity: 1;
     }

     img {
       width: 140px;
       height: auto;
       filter: drop-shadow(0 0 8px rgba(139, 0, 0, 0.7)) contrast(1.1) brightness(0.9);
       transition: all 0.3s ease;

       &:hover {
         filter: drop-shadow(0 0 20px rgba(255, 0, 0, 0.8)) contrast(1.2) brightness(1.1);
         transform: scale(1.03);
       }
     }

     .tower-lock {
       position: absolute;
       top: 50%;
       left: 50%;
       transform: translate(-50%, -50%);
       z-index: 10;
       pointer-events: none;

       .lock-icon {
         width: 24px;
         height: 24px;
         fill: #8b0000;
         filter: drop-shadow(0 0 8px rgba(139, 0, 0, 0.8));
         animation: lockGlow 3s ease-in-out infinite;

         path {
           stroke: #ff0000;
           stroke-width: 0.5;
           stroke-linejoin: round;
         }
       }
     }

     &:hover .tower-lock .lock-icon {
       fill: #ff0000;
       filter: drop-shadow(0 0 15px rgba(255, 0, 0, 0.9));
       animation: lockPulse 1s ease-in-out infinite;
     }

     // 塔层信息样式
     .tower-info {
       position: absolute;
       top: 50%;
       left: 50%;
       transform: translate(-50%, -50%);
       z-index: 15;
       background: rgba(0, 0, 0, 0.8);
       border: 1px solid #8b0000;
       border-radius: 8px;
       padding: 8px;
       max-width: 200px;
       text-align: center;
       opacity: 0;
       transition: opacity 0.3s ease;
       pointer-events: none;

       .floor-number {
         color: #ff0000;
         font-size: 12px;
         font-weight: bold;
         margin-bottom: 4px;
         text-shadow: 0 0 5px rgba(255, 0, 0, 0.8);
       }

       .floor-description {
         color: #fff;
         font-size: 10px;
         line-height: 1.3;
         text-shadow: 0 0 3px rgba(0, 0, 0, 0.8);
       }
     }

     &:hover .tower-info {
       opacity: 1;
     }

     // 解锁状态的塔层样式
     &.unlocked {
       .tower-lock {
         display: none;
       }

       img {
         filter: drop-shadow(0 0 15px rgba(0, 255, 0, 0.6)) contrast(1.2) brightness(1.1);
       }
     }
   }
  }
 }

 @keyframes borderGlow {
  0%, 100% {
    opacity: 0.3;
    box-shadow: 0 0 20px rgba(139, 0, 0, 0.8);
  }
  50% {
    opacity: 1;
    box-shadow: 0 0 40px rgba(255, 0, 0, 1);
  }
}

@keyframes textPulse {
  0%, 100% {
    text-shadow: 0 0 15px #ff0000, 0 0 25px #8b0000;
  }
  50% {
    text-shadow: 0 0 25px #ff0000, 0 0 35px #8b0000, 0 0 45px #ff0000;
  }
}

@keyframes shadowFlicker {
  0%, 100% {
    box-shadow: 0 0 30px rgba(139, 0, 0, 0.6);
  }
  25% {
    box-shadow: 0 0 40px rgba(255, 0, 0, 0.8);
  }
  50% {
    box-shadow: 0 0 35px rgba(139, 0, 0, 0.7);
  }
  75% {
    box-shadow: 0 0 45px rgba(255, 0, 0, 0.9);
  }
}

@keyframes lockGlow {
  0%, 100% {
    filter: drop-shadow(0 0 8px rgba(139, 0, 0, 0.8));
    transform: scale(1);
  }
  50% {
    filter: drop-shadow(0 0 15px rgba(139, 0, 0, 0.9));
    transform: scale(1.05);
  }
}

@keyframes lockPulse {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.1);
  }
}

@keyframes borderRotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

@keyframes iconPulse {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.1);
  }
}



// 加载状态样式
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: #fff;
}

.loading-spinner {
  width: 50px;
  height: 50px;
  border: 3px solid rgba(139, 0, 0, 0.3);
  border-top: 3px solid #8b0000;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 20px;
}

.loading-text {
  font-size: 18px;
  color: #fff;
  text-shadow: 0 0 10px rgba(139, 0, 0, 0.8);
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

// 错误提示样式
.error-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: #fff;
}

.error-text {
  font-size: 16px;
  color: #ff6b6b;
  text-shadow: 0 0 10px rgba(255, 107, 107, 0.5);
  margin-bottom: 20px;
  text-align: center;
}

.retry-button {
  background: linear-gradient(135deg, #8b0000, #ff0000);
  border: none;
  color: #fff;
  padding: 12px 24px;
  border-radius: 25px;
  cursor: pointer;
  font-size: 16px;
  font-weight: bold;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(139, 0, 0, 0.4);

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 20px rgba(139, 0, 0, 0.6);
  }

  &:active {
    transform: translateY(0);
  }
}

// 响应式设计
@media (max-width: 768px) {
  .tower-selector {
    width: 90%;
  }

  .tower-picker .picker-container {
    padding: 15px;
  }

  .tower-display .display-container {
    padding: 20px;
  }

  .tower-display .nav-btn {
    width: 40px;
    height: 40px;

    .nav-icon {
      width: 20px;
      height: 20px;
    }
  }

  .tower-part img,
  .tower-layer img {
    width: 100px;
  }

  .tower-layer .tower-lock .lock-icon {
    width: 18px;
    height: 18px;
  }
}
</style>


