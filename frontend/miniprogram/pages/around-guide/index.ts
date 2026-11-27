/** 南浔周边游玩页：展示用户提供的古镇一日游路线图。 */
const GUIDE_IMAGE = 'https://qianxun-1536638067.cos.ap-shanghai.myqcloud.com/miniprogram/guides/nanxun-one-day-guide-70985dd84719a4ce.webp'

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
