const roleOpts = [
  { label: '收银员', value: 0 },
  { label: '管理员', value: 1 },
]
const statusOpts = [
  { label: '禁用', value: 0 },
  { label: '启用', value: 1 },
]
const genderOpts = [
  { label: '未知', value: 0 },
  { label: '男', value: 1 },
  { label: '女', value: 2 },
]
const payTypeOpts = [
  { label: '现金', value: 0 },
  { label: '微信', value: 1 },
  { label: '支付宝', value: 2 },
  { label: '会员余额', value: 3 },
  { label: '银行卡', value: 4 },
]
/** 列表筛选：与后端 OrderQueryDTO.status 一致 */
const orderStatusOpts = [
  { label: '全部', value: null },
  { label: '未支付', value: 2 },
  { label: '已支付', value: 1 },
  { label: '已退款', value: 0 },
]

export { roleOpts, statusOpts, genderOpts, payTypeOpts, orderStatusOpts }

export function labelOf(opts, val) {
  return opts.find((o) => o.value === val)?.label ?? '-'
}

export function money(v) {
  return `￥${Number(v || 0).toFixed(2)}`
}

export const MODULES = {
  goods: {
    title: '商品管理',
    pageEndpoint: '/goods/page',
    addEndpoint: '/goods/add',
    updateEndpoint: '/goods/update',
    deleteEndpoint: '/goods/delete',
    statusEndpoint: '/goods/updateStatus',
    /** 弹窗：两列表单项（与截图一致） */
    formGridFieldSpan: 1,
    filters: [
      { key: 'name', label: '商品名称', type: 'input' },
      { key: 'categoryId', label: '分类', type: 'select', optionsKey: 'categoryOptions' },
      { key: 'status', label: '状态', type: 'select', options: statusOpts },
    ],
    formFields: [
      { key: 'name', label: '商品名称', type: 'input', required: true },
      { key: 'categoryId', label: '商品分类', type: 'select', required: true, optionsKey: 'categoryOptions' },
      { key: 'unit', label: '单位', type: 'input', required: true },
      {
        key: 'purchasePrice',
        label: '进货价',
        type: 'number',
        required: true,
        showStepper: true,
        step: 0.01,
        precision: 2,
      },
      {
        key: 'sellingPrice',
        label: '零售价',
        type: 'number',
        required: true,
        showStepper: true,
        step: 0.01,
        precision: 2,
      },
      { key: 'stock', label: '库存', type: 'number', required: true, showStepper: true, step: 1, precision: 0 },
      {
        key: 'stockWarning',
        label: '预警值',
        type: 'number',
        required: true,
        showStepper: true,
        step: 1,
        precision: 0,
      },
      { key: 'image', label: '商品图片', type: 'file' },
    ],
    createForm() {
      return {
        name: '',
        categoryId: null,
        unit: '件',
        purchasePrice: 0,
        sellingPrice: 0,
        stock: 0,
        stockWarning: 10,
        image: '',
      }
    },
    columns: [
      { title: '商品名称', key: 'name' },
      { title: '分类', key: 'categoryName' },
      { title: '单位', key: 'unit' },
      { title: '零售价', key: 'sellingPrice', format: 'money' },
      { title: '库存', key: 'stock' },
      { title: '预警值', key: 'stockWarning' },
      { title: '图片', key: 'image', format: 'image' },
      { title: '状态', key: 'status', format: 'statusTag' },
    ],
  },
  categories: {
    title: '品类管理',
    pageEndpoint: '/category/list',
    addEndpoint: '/category/add',
    updateEndpoint: '/category/update',
    deleteEndpoint: '/category/delete',
    noPagination: true,
    filters: [{ key: 'name', label: '分类名称', type: 'input' }],
    formFields: [
      { key: 'name', label: '分类名称', type: 'input', required: true },
      { key: 'parentId', label: '父级分类', type: 'number' },
      { key: 'sort', label: '排序', type: 'number' },
      { key: 'status', label: '状态', type: 'select', options: statusOpts, required: true },
    ],
    createForm() {
      return { name: '', parentId: 0, sort: 0, status: 1 }
    },
    columns: [
      { title: '分类名称', key: 'name' },
      { title: '父级ID', key: 'parentId' },
      { title: '排序', key: 'sort' },
      { title: '状态', key: 'status', format: 'statusTag' },
    ],
  },
  members: {
    title: '客户管理',
    pageEndpoint: '/member/page',
    actionColumnWidth: 168,
    addEndpoint: '/member/add',
    updateEndpoint: '/member/update',
    filters: [
      { key: 'name', label: '客户姓名', type: 'input' },
      { key: 'phone', label: '手机号', type: 'input' },
      { key: 'address', label: '地址', type: 'input' },
    ],
    formFields: [
      { key: 'name', label: '客户姓名', type: 'input', required: true },
      { key: 'phone', label: '手机号', type: 'input' },
      { key: 'gender', label: '性别', type: 'select', options: genderOpts },
      { key: 'address', label: '地址', type: 'input' },
      { key: 'remark', label: '备注', type: 'textarea', rows: 8, maxRows: 20, maxlength: 2000 },
      { key: 'discount', label: '折扣', type: 'number' },
    ],
    createForm() {
      return { name: '', phone: '', gender: 0, address: '', remark: '', discount: 1 }
    },
    columns: [
      { title: '姓名', key: 'name', width: 100, ellipsis: { tooltip: true } },
      { title: '手机号', key: 'phone', format: 'memberPhone', width: 124 },
      { title: '性别', key: 'gender', format: 'gender', width: 72 },
      { title: '地址', key: 'address', empty: '-', width: 160, ellipsis: { tooltip: true }, minWidth: 120 },
      {
        title: '备注',
        key: 'remark',
        empty: '-',
        width: 200,
        minWidth: 140,
        ellipsis: { tooltip: true },
      },
      { title: '目前欠款', key: 'totalDebt', format: 'memberTotalDebt', width: 112, align: 'right' },
    ],
  },
  orders: {
    title: '订单管理',
    pageEndpoint: '/order/page',
    detailEndpoint: '/order/detail',
    orderClientFilter: true,
    extraActions: ['detail', 'refund', 'editOrder'],
    filters: [
      { key: 'orderNo', label: '订单号', type: 'input' },
      { key: 'memberId', hidden: true },
      { key: 'customerName', label: '客户姓名', type: 'input', clientOnly: true },
      { key: 'customerPhone', label: '客户电话', type: 'input', clientOnly: true },
      { key: 'status', label: '状态', type: 'select', options: orderStatusOpts },
    ],
    columns: [
      { title: '客户', key: 'customerName', format: 'orderCustomerName' },
      { title: '订单日期', key: 'orderDate', format: 'orderDate', width: 112 },
      { title: '总金额', key: 'totalAmount', format: 'money' },
      { title: '欠款金额', key: '_debt', format: 'orderDebt' },
      { title: '状态', key: '_st', format: 'orderUiStatus' },
    ],
  },
  purchases: {
    title: '进货管理',
    pageEndpoint: '/purchase/page',
    addEndpoint: '/purchase/add',
    deleteEndpoint: '/purchase/delete',
    filters: [
      { key: 'purchaseNo', label: '进货单号', type: 'input' },
      { key: 'supplierId', label: '供应商', type: 'select', optionsKey: 'supplierOptions' },
    ],
    formFields: [
      { key: 'supplierId', label: '供应商', type: 'select', required: true, optionsKey: 'supplierOptions' },
      { key: 'goodsId', label: '商品ID', type: 'number', required: true },
      { key: 'quantity', label: '数量', type: 'number', required: true },
      { key: 'purchasePrice', label: '进货单价', type: 'number', required: true },
      { key: 'remark', label: '备注', type: 'input' },
    ],
    createForm() {
      return { supplierId: null, goodsId: null, quantity: 1, purchasePrice: 0, remark: '' }
    },
    columns: [
      { title: '进货单号', key: 'purchaseNo' },
      { title: '供应商', key: 'supplierName' },
      { title: '商品', key: 'goodsName' },
      { title: '数量', key: 'quantity' },
      { title: '单价', key: 'purchasePrice', format: 'money' },
      { title: '总金额', key: 'totalAmount', format: 'money' },
      { title: '备注', key: 'remark' },
    ],
  },
  suppliers: {
    title: '供应商管理',
    pageEndpoint: '/supplier/page',
    addEndpoint: '/supplier/add',
    updateEndpoint: '/supplier/update',
    deleteEndpoint: '/supplier/delete',
    filters: [
      { key: 'name', label: '供应商名称', type: 'input' },
      { key: 'contact', label: '联系人', type: 'input' },
    ],
    formFields: [
      { key: 'name', label: '供应商名称', type: 'input', required: true },
      { key: 'contact', label: '联系人', type: 'input' },
      { key: 'phone', label: '联系电话', type: 'input' },
      { key: 'address', label: '地址', type: 'input' },
    ],
    createForm() {
      return { name: '', contact: '', phone: '', address: '' }
    },
    columns: [
      { title: '名称', key: 'name' },
      { title: '联系人', key: 'contact' },
      { title: '电话', key: 'phone' },
      { title: '地址', key: 'address' },
    ],
  },
  users: {
    title: '用户管理',
    pageEndpoint: '/user/page',
    addEndpoint: '/user/add',
    updateEndpoint: '/user/update',
    deleteEndpoint: '/user/delete',
    statusEndpoint: '/user/updateStatus',
    extraActions: ['resetPwd'],
    filters: [
      { key: 'username', label: '用户名', type: 'input' },
      { key: 'realName', label: '姓名', type: 'input' },
      { key: 'role', label: '角色', type: 'select', options: roleOpts },
      { key: 'status', label: '状态', type: 'select', options: statusOpts },
    ],
    formFields: [
      { key: 'username', label: '用户名', type: 'input', required: true },
      { key: 'password', label: '密码', type: 'input' },
      { key: 'realName', label: '姓名', type: 'input', required: true },
      { key: 'phone', label: '手机号', type: 'input' },
      { key: 'role', label: '角色', type: 'select', required: true, options: roleOpts },
    ],
    createForm() {
      return { username: '', password: '', realName: '', phone: '', role: 0 }
    },
    columns: [
      { title: '用户名', key: 'username' },
      { title: '姓名', key: 'realName' },
      { title: '手机号', key: 'phone' },
      { title: '角色', key: 'role', format: 'role' },
      { title: '状态', key: 'status', format: 'statusTag' },
    ],
  },
}
