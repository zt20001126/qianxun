const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')

test('商城付款结果查询失败后可重试，确认后打开对应订单详情', async () => {
  const source = fs.readFileSync(path.join(__dirname, '../pages/payment-success/index.ts'), 'utf8')
  const { outputText } = ts.transpileModule(source, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 },
  })
  let definition
  let queries = 0
  const redirects = []
  vm.runInNewContext(outputText, {
    exports: {},
    Page: value => { definition = value },
    require: () => ({
      async getMallPaymentStatus() {
        queries += 1
        if (queries === 1) throw new Error('network unavailable')
        return { status: 'SUCCESS', order_id: 12, order_no: 'MALL12' }
      },
    }),
    wx: {
      showToast() {},
      setNavigationBarTitle() {},
      switchTab({ url }) { redirects.push(url) },
      redirectTo({ url }) { redirects.push(url) },
    },
  })
  const page = {
    ...definition,
    data: structuredClone(definition.data),
    setData(patch) { Object.assign(this.data, patch) },
  }
  await page.onLoad({ source: 'mall', payment_no: 'PAY12', order_id: '12', order_no: 'MALL12' })
  assert.deepEqual(redirects, [])
  assert.equal(page.data.confirmed, false)
  assert.match(page.data.errorMessage, /无法确认/)

  await page.onRetryTap()
  assert.equal(page.data.confirmed, true)
  page.onDetailTap()
  assert.deepEqual(redirects, ['/pages/mall-order-detail/index?id=12'])
})
