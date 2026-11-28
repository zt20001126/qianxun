const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')

const checkIn = '2026-12-01'
const checkOut = '2026-12-04'
const room = { id: 1, price: '100.00', name: '测试房', images: [], facilities: [] }

function createPage(name, roomService) {
  const modules = {
    '../../services/rooms': roomService,
    '../../utils/invitation': { consumePendingInvitation() {} },
    '../../utils/stay-date': {
      addDays: () => checkOut,
      daysBetween: () => 3,
      getBookingDateWindow: () => ({
        today: checkIn, lastCheckInDate: '2027-02-27', lastSelectableDate: '2027-02-28',
      }),
    },
  }
  let definition
  const source = fs.readFileSync(path.join(__dirname, '../pages', name, 'index.ts'), 'utf8')
  const { outputText } = ts.transpileModule(source, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 },
  })
  vm.runInNewContext(outputText, {
    exports: {},
    require: id => modules[id] || {},
    Page: value => { definition = value },
    wx: { setNavigationBarTitle() {} },
  })
  return {
    ...definition,
    data: structuredClone(definition.data),
    setData(patch) { Object.assign(this.data, patch) },
  }
}

for (const name of ['home', 'rooms', 'room-detail']) {
  test(name + ' 返回页面时刷新原行程的锁房或退款状态', async () => {
    let available = true
    const requests = []
    const page = createPage(name, {
      async listRooms(start, end) {
        requests.push([start, end])
        return available ? [room] : []
      },
      async getRoomDetail() { return room },
      async checkRoomAvailability(id, start, end) {
        requests.push([start, end])
        return { available, room_amount: '100.00' }
      },
    })
    const visibleRoomCount = () => {
      if (name === 'home') return page.data.roomCount
      if (name === 'rooms') return page.data.rooms.length
      return Number(page.data.availability?.available)
    }
    const loading = page.onLoad({ id: '1', check_in: checkIn, check_out: checkOut })
    await page.onShow?.()
    await loading
    assert.equal(visibleRoomCount(), 1)

    available = false
    await page.onShow?.()
    assert.equal(visibleRoomCount(), 0)

    available = true
    await page.onShow?.()
    assert.equal(visibleRoomCount(), 1)
    assert.deepEqual(requests, Array.from({ length: 3 }, () => [checkIn, checkOut]))
  })
}
