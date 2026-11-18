/** 小程序全局会话状态，登录数据仍以本地安全存储为准。 */
App({
  globalData: {
    member: null,
    token: '',
    tokenExpiresAt: 0,
  },
})
