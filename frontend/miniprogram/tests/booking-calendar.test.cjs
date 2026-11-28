const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')

function getCalendar(range, getRoomAvailability) {
  let definition
  function load(file, globals) {
    const source = fs.readFileSync(path.join(__dirname, '..', file), 'utf8')
    const { outputText } = ts.transpileModule(source, {
      compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 },
    })
    const context = { exports: {}, ...globals }
    vm.runInNewContext(outputText, context, { filename: file })
    return context.exports
  }
  const dates = load('utils/stay-date.ts', {})
  const events = []
  const toasts = []
  load('components/booking-calendar/index.ts', {
    Component: value => { definition = value },
    require: name => name.endsWith('/rooms') ? { getRoomAvailability: (_id, month) => getRoomAvailability(month) } : {
      ...dates,
      getBookingDateWindow: () => ({
        today: '2026-11-28', lastSelectableDate: '2027-02-25', lastCheckInDate: '2027-02-24',
      }),
    },
    wx: { showToast: toast => toasts.push(toast.title) },
  })
  return {
    ...definition.methods,
    properties: { ...range, roomId: 1 },
    data: structuredClone(definition.data),
    events,
    toasts,
    setData(patch) { Object.assign(this.data, patch) },
    triggerEvent(name) { events.push(name) },
  }
}

test('房态未加载时不能确认默认日期', () => {
  const calendar = getCalendar({}, () => new Promise(() => {}))
  calendar.openCalendar()
  calendar.onConfirmTap()
  assert.deepEqual(calendar.events, [])
  assert.match(calendar.toasts[0], /读取/)
})

test('跨月住宿必须等中间月份读取成功', async () => {
  const calendar = getCalendar({ checkIn: '2026-11-30', checkOut: '2027-01-01' }, async month => {
    if (month === '2026-12') throw new Error('网络连接失败')
    return { unavailable_dates: [], nightly_prices: {} }
  })
  calendar.openCalendar()
  await new Promise(setImmediate)
  calendar.onConfirmTap()
  assert.deepEqual(calendar.events, [])
  assert.match(calendar.toasts[0], /重新/)
})

test('已满日期可作为退房日，但不能跨过已满间夜', async () => {
  const calendar = getCalendar({ checkIn: '2026-11-30', checkOut: '2026-12-01' }, async month => ({
    unavailable_dates: month === '2026-12' ? ['2026-12-01'] : [], nightly_prices: {},
  }))
  calendar.openCalendar()
  await new Promise(setImmediate)
  calendar.onConfirmTap()
  assert.deepEqual(calendar.events, ['confirm'])
  calendar.updateSelection('2026-11-30', '2026-12-02', 'checkin')
  calendar.onConfirmTap()
  assert.deepEqual(calendar.events, ['confirm'])
  assert.match(calendar.toasts[0], /已满/)
})
