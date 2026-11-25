const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')

test('邀请页读取完成状态，模板保留文字和红绿标识且不显示奖励金额', async () => {
  let page
  const source = fs.readFileSync(path.join(__dirname, '../pages/invitation/index.ts'), 'utf8')
  const js = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText
  const records = [
    { masked_phone: '138****0001', bound_at: '2026-11-25T12:00:00+08:00', has_completed_order: false },
    { masked_phone: '138****0002', bound_at: '2026-11-25T12:00:00+08:00', has_completed_order: true },
  ]
  vm.runInNewContext(js, {
    exports: {}, Page: value => { page = value },
    wx: { hideShareMenu() {}, showShareMenu() {} },
    require: () => ({ getInvitationCode: async () => ({ invite_code: 'code' }), getInvitations: async () => ({ list: records, total: 2 }) }),
  })
  page.setData = values => Object.assign(page.data, values)
  await page.load()
  assert.equal(page.data.records[0].has_completed_order, false)
  assert.equal(page.data.records[1].has_completed_order, true)
  const template = fs.readFileSync(path.join(__dirname, '../pages/invitation/index.wxml'), 'utf8')
  const css = fs.readFileSync(path.join(__dirname, '../pages/invitation/index.wxss'), 'utf8')
  assert.match(template, /has_completed_order/)
  assert.match(template, /已下单/)
  assert.match(template, /未下单/)
  assert.doesNotMatch(template, /reward_amount/)
  assert.match(css, /\.order-status\.pending\s*\{\s*color:#b83b2a/)
  assert.match(css, /\.order-status\.complete\s*\{\s*color:#4e7a65/)
})
