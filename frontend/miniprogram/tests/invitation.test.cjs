const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')

function setup() {
  const storage = new Map()
  const calls = []
  let token = ''
  const wx = {
    getStorageSync: key => storage.get(key),
    setStorageSync: (key, value) => storage.set(key, value),
    removeStorageSync: key => storage.delete(key),
    request: options => calls.push(options),
  }
  const source = fs.readFileSync(path.join(__dirname, '../utils/invitation.ts'), 'utf8')
  const js = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText
  const exports = {}
  vm.runInNewContext(js, { exports, wx, require: name => name === '../config'
    ? { API_BASE_URL: 'https://example.invalid' } : { getValidToken: () => token } })
  return { storage, calls, api: exports, setToken: value => { token = value } }
}

const first = 'AAAAAAAAAAAAAAAAAAAAAA'
const second = 'BBBBBBBBBBBBBBBBBBBBBB'

test('未登录时保留最近邀请，登录后只提交最新标识', async () => {
  const env = setup()
  env.api.savePendingInvitation(first)
  env.api.savePendingInvitation(second)
  await env.api.consumePendingInvitation()
  assert.equal(env.calls.length, 0)
  env.setToken('member-token')
  const pending = env.api.consumePendingInvitation()
  assert.equal(env.calls[0].data.invite_code, second)
  env.calls[0].success({ statusCode: 200 })
  await pending
  assert.equal(env.storage.size, 0)
})

test('网络失败保留标识，退出清除，旧请求不能删除新标识', async () => {
  const env = setup()
  env.setToken('member-token')
  env.api.savePendingInvitation(first)
  const pending = env.api.consumePendingInvitation()
  env.api.savePendingInvitation(second)
  env.calls[0].success({ statusCode: 500 })
  await pending
  assert.equal(env.storage.get('qx_pending_invitation'), second)
  assert.equal(env.calls.length, 2)
  env.api.clearPendingInvitation()
  env.calls[1].success({ statusCode: 200 })
  await new Promise(resolve => setImmediate(resolve))
  assert.equal(env.storage.size, 0)
})
