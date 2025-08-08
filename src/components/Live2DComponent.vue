<template>
  <canvas ref="liveCanvas" :style="{ width: width + 'px', height: height + 'px' }"></canvas>
</template>

<script>
import * as PIXI from 'pixi.js';
import { Live2DModel } from 'pixi-live2d-display/cubism4';

window.PIXI = PIXI;

export default {
  props: {
    modelPath: {
      type: String,
      required: true
    },
    width: {
      type: Number,
      default: 600
    },
    height: {
      type: Number,
      default: 500
    },
    scale: {
      type: Number,
      default: 0.2
    }
  },
  data() {
    return {
      loading: false,
      app: null,
      model: null
    };
  },
  async mounted() {
    await this.initLive2D();
  },
  beforeUnmount() {
    this.destroyLive2D();
  },
  watch: {
    modelPath() {
      this.destroyLive2D();
      this.initLive2D();
    },
    width() {
      this.resizeCanvas();
    },
    height() {
      this.resizeCanvas();
    },
  },
  methods: {
    async restoreLive2D() {
      if (!this.app || !this.model) {
        await this.initLive2D();
      } else {
        // 重新计算位置和尺寸
        this.resizeCanvas();
        // 强制重绘
        this.app.render();
      }
    },
    async initLive2D() {
      this.loading = true;
      try {
        if (this.app) {
          this.app.destroy();
        }

        this.app = new PIXI.Application({
          view: this.$refs.liveCanvas,
          width: this.width,
          height: this.height,
          autoStart: true,
          backgroundAlpha: 0,
          preserveDrawingBuffer: true // 重要：保留绘图缓冲
        });

        this.model = await Live2DModel.from(this.modelPath);
        this.app.stage.addChild(this.model);
        this.model.scale.set(this.scale);

        // 添加模型位置居中
        this.model.x = 0;
        this.model.y = 0;

        this.app.renderer.render(this.app.stage);
      } catch (error) {
        console.error('Live2D初始化失败:', error);
      } finally {
        this.loading = false;
      }
    },
    destroyLive2D() {
      if (this.model) {
        this.model.destroy();
        this.model = null;
      }
      if (this.app) {
        this.app.destroy();
        this.app = null;
      }
    },
    resizeCanvas() {
      if (this.app) {
        this.app.renderer.resize(this.width, this.height);
      }
    }
  }
};
</script>
