/**
 * 与当前后端 OpenAPI 对齐的封装（baseURL `/api` 见 {@link '@/utils/request'}）。
 * 页面组件可直接使用 {@link post}/{@link get}/{@link uploadFile}；此处提供命名方法便于检索与复用。
 */
import { get, http, post, uploadFile } from '@/utils/request'

export const api = {
  auth: {
    login: (body) => post('/auth/login', body),
    logout: () => post('/auth/logout'),
    info: () => post('/auth/info'),
    updatePwd: (body) => post('/auth/updatePwd', body),
  },
  goods: {
    page: (body) => post('/goods/page', body),
    add: (body) => post('/goods/add', body),
    update: (body) => post('/goods/update', body),
    delete: (body) => post('/goods/delete', body),
    updateStatus: (body) => post('/goods/updateStatus', body),
    stockWarning: (body) => post('/goods/stockWarning', body),
  },
  category: {
    list: (body = {}) => post('/category/list', body),
    add: (body) => post('/category/add', body),
    update: (body) => post('/category/update', body),
    delete: (body) => post('/category/delete', body),
  },
  member: {
    page: (body) => post('/member/page', body),
    getByCardNo: (body) => post('/member/getByCardNo', body),
    add: (body) => post('/member/add', body),
    update: (body) => post('/member/update', body),
    recharge: (body) => post('/member/recharge', body),
  },
  order: {
    settle: (body) => post('/order/settle', body),
    page: (body) => post('/order/page', body),
    detail: (body) => post('/order/detail', body),
    edit: (body) => post('/order/edit', body),
    refund: (body) => post('/order/refund', body),
    today: () => post('/order/today'),
    attachmentDelete: (body) => post('/order/attachment/delete', body),
    repaymentDelete: (body) => post('/order/repayment/delete', body),
  },
  region: {
    /** GET/POST 等价 */
    allPost: () => post('/region/all'),
    allGet: () => get('/region/all'),
    childrenGet: (params) => get('/region/children', params),
    /** POST 与 GET 等价；参数通过 query 传递 parentId */
    childrenPost: (params) => http.post('/region/children', {}, { params }),
  },
  statistics: {
    dashboard: () => post('/statistics/dashboard'),
    repaymentReminder: (body = {}) => post('/statistics/repaymentReminder', body),
    salesTrend: (body) => post('/statistics/salesTrend', body),
    salesRanking: (body) => post('/statistics/salesRanking', body),
    categoryPie: () => post('/statistics/categoryPie'),
    cashierRanking: (body) => post('/statistics/cashierRanking', body),
  },
  file: {
    upload: (file) => uploadFile('/file/upload', file),
  },
  user: {
    page: (body) => post('/user/page', body),
    add: (body) => post('/user/add', body),
    update: (body) => post('/user/update', body),
    delete: (body) => post('/user/delete', body),
    resetPwd: (body) => post('/user/resetPwd', body),
    updateStatus: (body) => post('/user/updateStatus', body),
  },
  supplier: {
    page: (body) => post('/supplier/page', body),
    list: (body = {}) => post('/supplier/list', body),
    add: (body) => post('/supplier/add', body),
    update: (body) => post('/supplier/update', body),
    delete: (body) => post('/supplier/delete', body),
  },
  purchase: {
    page: (body) => post('/purchase/page', body),
    add: (body) => post('/purchase/add', body),
    delete: (body) => post('/purchase/delete', body),
  },
}

export default api
