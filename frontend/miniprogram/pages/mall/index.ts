/** 商城入口，展示已上架的民宿商品。 */
import { listMallProducts, type MallProduct } from '../../services/mall'

Page({
  data: { products: [] as MallProduct[], loading: false, error: '', page: 1, hasMore: false },
  onShow() { void this.loadProducts(true) },
  async onPullDownRefresh() { await this.loadProducts(true); wx.stopPullDownRefresh() },
  onReachBottom() { if (this.data.hasMore) void this.loadProducts(false) },
  onRetry() { void this.loadProducts(true) },
  async loadProducts(reset: boolean) {
    if (this.data.loading) return
    this.setData({ loading: true, error: '' })
    try {
      const result = await listMallProducts(reset ? 1 : this.data.page + 1)
      this.setData({ products: reset ? result.list : [...this.data.products, ...result.list],
        page: result.page, hasMore: result.page * result.size < result.total })
    }
    catch (error) { this.setData({ error: error instanceof Error ? error.message : '商品加载失败' }) }
    finally { this.setData({ loading: false }) }
  },
  onProductTap(event: WechatMiniprogram.TouchEvent) {
    const id = Number(event.currentTarget.dataset.id)
    if (Number.isInteger(id) && id > 0) wx.navigateTo({ url: `/pages/mall-checkout/index?id=${id}` })
  },
})
