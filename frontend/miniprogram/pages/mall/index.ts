/** 商城入口，仅展示当前第一件上架的芊浔好物。 */
import { listMallProducts, type MallProduct } from '../../services/mall'

Page({
  data: { product: null as MallProduct | null, loading: false, error: '' },
  onShow() { void this.loadProduct() },
  async onPullDownRefresh() { await this.loadProduct(); wx.stopPullDownRefresh() },
  onRetry() { void this.loadProduct() },
  async loadProduct() {
    if (this.data.loading) return
    this.setData({ loading: true, error: '' })
    try {
      const result = await listMallProducts(1, 1)
      // 商城前台仅露出一件已上架好物，后台仍可继续维护完整商品目录。
      this.setData({ product: result.list[0] || null })
    }
    catch (error) { this.setData({ error: error instanceof Error ? error.message : '商品加载失败' }) }
    finally { this.setData({ loading: false }) }
  },
  onDetailTap(event: WechatMiniprogram.TouchEvent) {
    const id = Number(event.currentTarget.dataset.id)
    if (Number.isInteger(id) && id > 0) wx.navigateTo({ url: `/pages/mall-detail/index?id=${id}` })
  },
})
