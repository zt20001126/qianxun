/** 详情图进入实际可视区域后才挂载，使用接口原 URL 复用微信图片缓存。 */
const observers = new WeakMap<object, WechatMiniprogram.IntersectionObserver>()
const FOOTER_HEIGHT_RPX = 140
const SCREEN_WIDTH_RPX = 750
type ImageState = 'loading' | 'loaded' | 'failed'

Component({
  properties: {
    images: { type: Array, value: [] },
  },

  data: {
    states: [] as ImageState[],
  },

  lifetimes: {
    ready() {
      const { windowWidth, screenHeight, safeArea } = wx.getWindowInfo
        ? wx.getWindowInfo()
        : wx.getSystemInfoSync()
      const safeBottom = safeArea ? screenHeight - safeArea.bottom : 0
      // 两个详情页的固定操作栏均为 140rpx 加底部安全区。
      const footerHeight = FOOTER_HEIGHT_RPX * windowWidth / SCREEN_WIDTH_RPX + safeBottom
      const observer = this.createIntersectionObserver({ observeAll: true })
      observers.set(this, observer)
      observer.relativeToViewport({ bottom: -footerHeight }).observe('.detail-image-slot', (entry) => {
        if (observers.get(this) !== observer || entry.intersectionRatio <= 0) return
        const index = Number(entry.dataset.index)
        if (!this.data.states[index]) this.setImageState(index, 'loading')
      })
    },

    detached() {
      observers.get(this)?.disconnect()
      observers.delete(this)
    },
  },

  methods: {
    setImageState(index: number, state: ImageState) {
      this.setData({ [`states[${index}]`]: state })
    },

    onImageLoad(event: WechatMiniprogram.CustomEvent) {
      this.setImageState(Number(event.currentTarget.dataset.index), 'loaded')
    },

    onImageError(event: WechatMiniprogram.CustomEvent) {
      this.setImageState(Number(event.currentTarget.dataset.index), 'failed')
    },

    onImageRetry(event: WechatMiniprogram.TouchEvent) {
      const index = Number(event.currentTarget.dataset.index)
      if (this.data.states[index] === 'failed') this.setImageState(index, 'loading')
    },
  },
})
