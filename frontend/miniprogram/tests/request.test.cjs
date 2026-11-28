const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')

function setupRequest() {
  const source = fs.readFileSync(path.join(__dirname, '../services/request.ts'), 'utf8')
  const { outputText } = ts.transpileModule(source, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 },
  })
  let pending
  let token = 'current-token'
  let redirects = 0
  const exports = {}
  vm.runInNewContext(outputText, {
    exports,
    require: name => name === '../config' ? { API_BASE_URL: 'http://localhost' } : {
      getValidToken: () => token,
      clearSession: () => { token = '' },
    },
    wx: { request: options => { pending = options }, reLaunch: () => { redirects += 1 } },
  })
  return {
    request: exports.request,
    respond: response => pending.success(response),
    replaceToken: value => { token = value },
    session: () => ({ token, redirects }),
  }
}

test('网关返回空响应时请求可结束并提示失败', async () => {
  const { request, respond } = setupRequest()
  const result = request({ url: '/api/app/rooms', withAuth: false })
  respond({ statusCode: 200, data: null })
  await assert.rejects(result, /请求失败/)
})

test('公开登录接口返回 401 时保留服务端提示，不重载登录页', async () => {
  const { request, respond, session } = setupRequest()
  const result = request({ url: '/api/app/auth/login', withAuth: false })
  respond({ statusCode: 401, data: { message: '微信登录凭证已失效' } })
  await assert.rejects(result, /微信登录凭证已失效/)
  assert.deepEqual(session(), { token: 'current-token', redirects: 0 })
})

test('旧会话请求的 401 不会清除刚登录的新会话', async () => {
  const { request, respond, replaceToken, session } = setupRequest()
  const result = request({ url: '/api/app/orders' })
  replaceToken('new-token')
  respond({ statusCode: 401, data: null })
  await assert.rejects(result, /登录状态已失效/)
  assert.deepEqual(session(), { token: 'new-token', redirects: 0 })
})

test('活跃会话收到 401 时清除令牌并返回登录页', async () => {
  const { request, respond, session } = setupRequest()
  const result = request({ url: '/api/app/orders' })
  respond({ statusCode: 401, data: null })
  await assert.rejects(result, /登录状态已失效/)
  assert.deepEqual(session(), { token: '', redirects: 1 })
})
