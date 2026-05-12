import { defineStore } from 'pinia'
import { post } from '@/utils/request'
import { normalizeRegionFlatRows, buildNameKeyedRegionTree } from '@/utils/regionData'

const GOODS_PAGE_SIZE = 500

/** 合并并发 {@link bootstrap}，避免重复触发时并行请求 */
let bootstrapInflight = null

/**
 * 收银台用的地区目录与上架商品全量缓存。
 * 登录成功后由 {@link bootstrap} 预热；商品/地址变更后需刷新对应分区。
 */
export const useCashierCacheStore = defineStore('cashierCache', {
  state: () => ({
    regionFlat: [],
    regionsLoading: false,
    regionsError: '',
    goodsAll: [],
    goodsLoading: false,
    goodsError: '',
  }),

  getters: {
    regionY: (state) => {
      const { y } = buildNameKeyedRegionTree(state.regionFlat)
      return y
    },
  },

  actions: {
    clear() {
      bootstrapInflight = null
      this.regionFlat = []
      this.regionsError = ''
      this.goodsAll = []
      this.goodsError = ''
    },

    /** 地址管理页拉取后写入，避免收银台再请求一次 */
    setRegionCatalogFromNormalizedRows(rows) {
      this.regionFlat = Array.isArray(rows) ? rows : []
      this.regionsError = ''
    },

    async fetchRegions() {
      this.regionsLoading = true
      this.regionsError = ''
      try {
        const raw = await post('/region/all', {})
        this.regionFlat = normalizeRegionFlatRows(Array.isArray(raw) ? raw : [])
      } catch (e) {
        this.regionsError = e.message || '区域数据加载失败'
        this.regionFlat = []
        throw e
      } finally {
        this.regionsLoading = false
      }
    },

    async fetchGoodsOnShelf() {
      this.goodsLoading = true
      this.goodsError = ''
      try {
        const acc = []
        let pageNum = 1
        while (true) {
          const data = await post('/goods/page', {
            pageNum,
            pageSize: GOODS_PAGE_SIZE,
            status: 1,
          })
          const recs = data.records || []
          acc.push(...recs)
          if (recs.length < GOODS_PAGE_SIZE) break
          pageNum += 1
        }
        this.goodsAll = acc
      } catch (e) {
        this.goodsError = e.message || '商品加载失败'
        this.goodsAll = []
        throw e
      } finally {
        this.goodsLoading = false
      }
    },

    /** 登录成功后调用；已有数据则跳过对应请求；并发调用合并为一次 */
    async bootstrap() {
      if (bootstrapInflight) {
        return bootstrapInflight
      }
      const tasks = []
      if (!this.regionFlat.length) {
        tasks.push(this.fetchRegions().catch(() => {}))
      }
      if (!this.goodsAll.length) {
        tasks.push(this.fetchGoodsOnShelf().catch(() => {}))
      }
      if (!tasks.length) {
        return Promise.resolve()
      }
      bootstrapInflight = Promise.all(tasks).finally(() => {
        bootstrapInflight = null
      })
      return bootstrapInflight
    },

    async refreshRegions() {
      await this.fetchRegions()
    },

    async refreshGoodsOnShelf() {
      await this.fetchGoodsOnShelf()
    },
  },
})
