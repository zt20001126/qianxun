/** 南浔周边游玩页：展示用户提供的古镇一日游路线图。 */
const GUIDE_IMAGE = 'https://qianxunminsu.oss-cn-shanghai.aliyuncs.com/miniprogram/guides/nanxun-one-day-guide-81227d1e37b84e88895d1b858e5a2b7a.jpg'

Page({
  data: {
    guideImage: GUIDE_IMAGE,
  },

  /** 打开原图预览，支持微信内置的缩放和拖动。 */
  onPreviewGuide() {
    wx.previewImage({
      current: GUIDE_IMAGE,
      urls: [GUIDE_IMAGE],
    })
  },
})
