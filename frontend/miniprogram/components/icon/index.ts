/** 将 Lucide 风格图标映射到小程序本地 SVG 资源。 */
const ICON_NAMES: Record<string, string> = {
  'arrow-right': 'arrow-right',
  moon: 'moon',
  landmark: 'landmark',
  'bed-double': 'bed-double',
  sprout: 'sprout',
  'map-pin': 'map-pin',
  'chevron-right': 'chevron-right',
  calendar: 'calendar',
  user: 'user',
  'maximize-2': 'maximize-2',
  'chevron-left': 'chevron-left',
  users: 'users',
  wind: 'wind',
  bath: 'bath',
  wifi: 'wifi',
  tv: 'tv',
  phone: 'phone',
  'file-text': 'file-text',
  info: 'info',
  'message-circle-white': 'message-circle-white',
  copy: 'copy',
  'clipboard-list': 'clipboard-list',
  home: 'home',
  'check-white': 'check-white',
  close: 'close',
  'chevron-right-white': 'chevron-right-white',
  'chevron-left-white': 'chevron-left-white',
  'chevron-right-gray': 'chevron-right-gray',
  'x-circle': 'x-circle',
  smartphone: 'smartphone',
  gift: 'gift',
  'gift-celadon': 'gift-celadon',
  'award-celadon': 'award-celadon',
  'rotate-ccw-celadon': 'rotate-ccw-celadon',
  'sparkles-celadon': 'sparkles-celadon',
  ticket: 'ticket',
  coins: 'coins',
  'crown-wood': 'crown-wood',
}

Component({
  properties: {
    name: { type: String, value: '' },
    size: { type: Number, value: 32 },
  },
  data: {
    source: '',
  },
  observers: {
    name(name: string) {
      this.setData({ source: ICON_NAMES[name] ? '/assets/icons/' + ICON_NAMES[name] + '.svg' : '' })
    },
  },
  lifetimes: {
    attached() {
      const name = this.data.name
      this.setData({ source: ICON_NAMES[name] ? '/assets/icons/' + ICON_NAMES[name] + '.svg' : '' })
    },
  },
})

