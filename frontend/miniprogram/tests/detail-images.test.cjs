const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')

function read(file) {
  return fs.readFileSync(path.join(__dirname, '..', file), 'utf8')
}

function capture(file, entryPoint, globals) {
  let definition
  const { outputText } = ts.transpileModule(read(file), {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 },
  })
  vm.runInNewContext(outputText, {
    [entryPoint]: value => { definition = value },
    exports: {},
    ...globals,
  }, { filename: file })
  return definition
}

function createInstance(definition) {
  return {
    ...definition,
    ...definition.methods,
    data: structuredClone(definition.data),
    setData(patch) {
      for (const [key, value] of Object.entries(patch)) {
        const indexedKey = /^(\w+)\[(\d+)\]$/.exec(key)
        if (indexedKey) this.data[indexedKey[1]][Number(indexedKey[2])] = value
        else this.data[key] = value
      }
    },
  }
}

test('下方图集仅在图片有可见面积时绑定原地址，并支持加载失败后重试', () => {
  let notify
  let disconnected = false
  const definition = capture('components/detail-image-list/index.ts', 'Component', {
    wx: { getWindowInfo: () => ({ windowWidth: 375, screenHeight: 812, safeArea: { bottom: 778 } }) },
  })
  const album = createInstance(definition)
  album.createIntersectionObserver = options => {
    assert.equal(options.observeAll, true)
    return {
      relativeToViewport(margins) {
        assert.equal(margins.bottom, -104)
        return this
      },
      observe(selector, callback) {
        assert.equal(selector, '.detail-image-slot')
        notify = callback
      },
      disconnect() { disconnected = true },
    }
  }
  definition.lifetimes.ready.call(album)
  const report = (index, intersectionRatio) => notify({ dataset: { index }, intersectionRatio })
  const imageEvent = index => ({ currentTarget: { dataset: { index } } })

  report(0, 0)
  assert.deepEqual(album.data.states, [])
  report(0, 0.1)
  assert.deepEqual(album.data.states, ['loading'])
  album.onImageLoad(imageEvent(0))
  report(0, 0)
  report(0, 0.5)
  assert.deepEqual(album.data.states, ['loaded'])
  report(1, 0.2)
  album.onImageError(imageEvent(1))
  report(1, 0.3)
  assert.deepEqual(album.data.states, ['loaded', 'failed'])
  album.onImageRetry(imageEvent(1))
  assert.deepEqual(album.data.states, ['loaded', 'loading'])
  definition.lifetimes.detached.call(album)
  report(2, 0.5)
  assert.equal(disconnected, true)
  assert.equal(album.data.states.length, 2)

  const markup = read('components/detail-image-list/index.wxml')
  assert.match(markup, /wx:for="{{images}}"/)
  assert.match(markup, /<image\s+wx:if="{{states\[index\] === 'loading' \|\| states\[index\] === 'loaded'}}"[\s\S]*?src="{{item}}"/)
})

for (const [pageName, itemName] of [['room-detail', 'room'], ['mall-detail', 'product']]) {
  test(`${pageName} 按需加载轮播，图集与轮播使用同一组地址`, () => {
    const page = createInstance(capture(`pages/${pageName}/index.ts`, 'Page', {
      require: () => ({}),
    }))
    page.data[itemName] = { images: ['first', 'second', 'third'] }
    assert.deepEqual(page.data.openedSlides, [true])
    page.onGalleryStep({ currentTarget: { dataset: { delta: '1' } } })
    assert.equal(page.data.imageIndex, 1)
    assert.deepEqual(page.data.openedSlides, [true, true])
    page.onGalleryChange({ detail: { current: 2 } })
    assert.equal(page.data.imageIndex, 2)
    assert.deepEqual(page.data.openedSlides, [true, true, true])

    const markup = read(`pages/${pageName}/index.wxml`)
    assert.ok(markup.includes('<image wx:if="{{openedSlides[index]}}" class="gallery-image" src="{{item}}"'))
    const album = markup.indexOf(`<detail-image-list wx:if="{{imageCount}}" images="{{${itemName}.images}}" />`)
    assert.ok(album > markup.indexOf('介绍</text>'))
    assert.ok(album < markup.indexOf('class="detail-footer"'))
    assert.match(read(`pages/${pageName}/index.wxss`), /\.detail-footer\s*{[^}]*position: fixed/)
  })
}
