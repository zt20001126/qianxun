const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')

function loadModule(file, globals) {
  const source = fs.readFileSync(path.join(__dirname, '..', file), 'utf8')
  const { outputText } = ts.transpileModule(source, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 },
  })
  const exports = {}
  vm.runInNewContext(outputText, { exports, ...globals }, { filename: file })
  return exports
}

test('客房订单详情对含节假日价的多晚房费显示四舍五入后的平均夜价', async () => {
  let definition
  const order = {
    id: 8, order_no: 'QX202611280001', room_id: 1, room_number: '101',
    check_in: '2026-11-30', check_out: '2026-12-03', nights: 3, status: 'PENDING_CONFIRM',
    room_amount: '350.03', discount_amount: '12.00', paid_amount: '338.03', points_used: 360,
  }
  const modules = {
    '../../services/rooms': { getRoomDetail: async () => ({ name: '测试房' }) },
    '../../services/orders': {
      getMyOrder: async () => order,
      getCancellationEligibility: async () => ({ eligible: true }),
      getLatestRefund: async () => null,
    },
    '../../types/booking': { getOrderStatusLabel: () => '待确认' },
  }
  loadModule('pages/order-detail/index.ts', {
    require: name => name === '../../utils/money' ? loadModule('utils/money.ts', {}) : (modules[name] || {}),
    wx: { setNavigationBarTitle() {} },
    Page: value => { definition = value },
  })
  const page = {
    ...definition,
    data: structuredClone(definition.data),
    setData(patch) { Object.assign(this.data, patch) },
  }
  await page.onLoad({ id: '8' })
  assert.equal(page.data.unitPrice, '116.68')
  assert.equal(page.data.roomAmountLabel, '350.03')
  assert.equal(page.data.paidAmountLabel, '338.03')
})
