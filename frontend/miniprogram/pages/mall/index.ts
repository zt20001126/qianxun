/** 商城入口，展示全部已上架的芊浔好物。 */
import { listMallProducts, type MallProduct } from '../../services/mall'

/** 单次请求商品数量，与服务端分页上限保持一致。 */
const MALL_PRODUCT_PAGE_SIZE = 100

Page({
  data: { products: [] as MallProduct[], loading: false, error: '' },
  onShow() { void this.loadProducts() },
  async onPullDownRefresh() { await this.loadProducts(); wx.stopPullDownRefresh() },
  onRetry() { void this.loadProducts() },
  async loadProducts() {
    if (this.data.loading) return
    this.setData({ loading: true, error: '' })
    try {
      const products: MallProduct[] = []
      let page = 1
      let total = 0
      let received = 0
      // 接口最多返回 100 条；继续翻页以展示所有已上架商品。
      do {
        const result = await listMallProducts(page, MALL_PRODUCT_PAGE_SIZE)
        products.push(...result.list)
        total = result.total
        received = result.list.length
        page += 1
      } while (products.length < total && received > 0)
      this.setData({ products })
    }
    catch (error) { this.setData({ error: error instanceof Error ? error.message : '商品加载失败' }) }
    finally { this.setData({ loading: false }) }
  },
  onDetailTap(event: WechatMiniprogram.TouchEvent) {
    const id = Number(event.currentTarget.dataset.id)
    if (Number.isInteger(id) && id > 0) wx.navigateTo({ url: `/pages/mall-detail/index?id=${id}` })
  },
})
