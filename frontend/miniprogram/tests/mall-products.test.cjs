const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')

function setup(products) {
  const requests = []
  let pageDefinition
  const source = fs.readFileSync(path.join(__dirname, '../pages/mall/index.ts'), 'utf8')
  const wxml = fs.readFileSync(path.join(__dirname, '../pages/mall/index.wxml'), 'utf8')
  const js = ts.transpileModule(source, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 },
  }).outputText
  const context = {
    Page: definition => { pageDefinition = definition },
    wx: { stopPullDownRefresh() {}, navigateTo() {} },
    require: moduleName => {
      if (moduleName !== '../../services/mall') throw new Error(`unexpected module: ${moduleName}`)
      return {
        listMallProducts: async (page, size) => {
          requests.push([page, size])
          const offset = (page - 1) * size
          return { list: products.slice(offset, offset + size), total: products.length, page, size }
        },
      }
    },
    exports: {},
    module: { exports: {} },
    Promise,
  }
  vm.runInNewContext(js, context, { filename: 'pages/mall/index.ts' })
  const page = {
    data: JSON.parse(JSON.stringify(pageDefinition.data)),
    setData(patch) { Object.assign(this.data, patch) },
  }
  for (const [key, value] of Object.entries(pageDefinition)) {
    if (typeof value === 'function') page[key] = value.bind(page)
  }
  return { page, requests, wxml }
}

test('商城入口展示所有已上架商品', async () => {
  const products = [
    { id: 2, name: '山水雅韵·中式陶瓷功夫茶具套装', description: '', images: [], price: '9999.00' },
    { id: 1, name: '云柔雅韵·轻奢蚕丝被', description: '', images: [], price: '999.00' },
  ]
  const { page, requests, wxml } = setup(products)

  await page.onPullDownRefresh()

  assert.ok(requests.length > 0)
  assert.deepEqual(JSON.parse(JSON.stringify(page.data.products)), products)
  assert.match(wxml, /wx:for="{{products}}"/)
  assert.match(wxml, /{{item\.name}}/)
})

test('商城入口继续读取下一页上架商品', async () => {
  const products = Array.from({ length: 101 }, (_, index) => ({
    id: 101 - index,
    name: `商品${index + 1}`,
    description: '',
    images: [],
    price: '1.00',
  }))
  const { page, requests } = setup(products)

  await page.onPullDownRefresh()

  assert.deepEqual(requests, [[1, 100], [2, 100]])
  assert.equal(page.data.products.length, 101)
  assert.equal(page.data.products[100].name, '商品101')
})
