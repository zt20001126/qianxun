/** 商城商品详情页，复用客房详情的图集和底部行动区交互。 */
import { getMallProduct, type MallProduct } from '../../services/mall'

interface MallDetailPageData {
  product: MallProduct | null
  loading: boolean
  errorMessage: string
  imageIndex: number
  imageCount: number
  openedSlides: boolean[]
  priceLabel: string
  buying: boolean
}

Page({
  data: {
    product: null,
    loading: true,
    errorMessage: '',
    imageIndex: 0,
    imageCount: 0,
    openedSlides: [true],
    priceLabel: '',
    buying: false,
  } as MallDetailPageData,

  productId: 0,

  async onLoad(options: Record<string, string | undefined>) {
    const productId = Number(options.id)
    if (!Number.isInteger(productId) || productId <= 0) {
      this.setData({ loading: false, errorMessage: '商品编号无效' })
      return
    }
    this.productId = productId
    await this.loadProduct()
  },

  async onPullDownRefresh() {
    await this.loadProduct()
    wx.stopPullDownRefresh()
  },

  /** 读取仍处于上架状态的商品详情。 */
  async loadProduct() {
    this.setData({ loading: true, errorMessage: '' })
    try {
      const product = await getMallProduct(this.productId)
      this.setData({
        product,
        imageIndex: 0,
        imageCount: product.images.length,
        openedSlides: [true],
        priceLabel: formatPrice(product.price),
      })
      wx.setNavigationBarTitle({ title: product.name })
    } catch (error) {
      this.setData({ errorMessage: error instanceof Error ? error.message : '商品详情读取失败' })
    } finally {
      this.setData({ loading: false })
    }
  },

  onGalleryChange(event: WechatMiniprogram.SwiperChange) {
    this.showGallerySlide(event.detail.current)
  },

  onGalleryStep(event: WechatMiniprogram.TouchEvent) {
    const product = this.data.product
    if (!product || product.images.length <= 1) return
    const delta = Number(event.currentTarget.dataset.delta)
    const imageIndex = (this.data.imageIndex + delta + product.images.length) % product.images.length
    this.showGallerySlide(imageIndex)
  },

  /** 访问某张轮播图时才挂载图片，已访问的图片继续保留。 */
  showGallerySlide(imageIndex: number) {
    this.setData({ imageIndex, [`openedSlides[${imageIndex}]`]: true })
  },

  /** 携带商品标识进入购买信息页。 */
  onBuyTap() {
    if (!this.data.product || this.data.buying) return
    this.setData({ buying: true })
    wx.navigateTo({
      url: '/pages/mall-checkout/index?id=' + this.productId,
      complete: () => this.setData({ buying: false }),
    })
  },

  onBackTap() {
    wx.navigateBack({ delta: 1 })
  },
})

function formatPrice(price: string): string {
  return price.replace(/\.00$/, '')
}
