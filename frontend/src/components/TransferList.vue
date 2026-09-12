
<template>
  <div class="transfer-list">
    <div class="list-header">
      <h3>调配台账</h3>
      <div class="filter-section">
        <div v-if="drillTip" class="drill-banner">
          <span>{{ drillTip }}</span>
          <button class="btn-back" @click="backToDashboard">返回看板</button>
          <button class="btn-clear-drill" @click="clearDrillFilters">清除看板筛选</button>
        </div>
        <div class="date-filter">
          <input type="date" v-model="startDate" placeholder="开始日期" />
          <span class="date-separator">至</span>
          <input type="date" v-model="endDate" placeholder="结束日期" />
          <select v-model="extraFilters.areaId">
            <option value="">全部区域</option>
            <option v-for="area in areaOptions" :key="area.id" :value="area.id">{{ area.name }}</option>
          </select>
          <select v-model="extraFilters.equipmentType">
            <option value="">全部设备类型</option>
            <option v-for="type in typeOptions" :key="type" :value="type">{{ type }}</option>
          </select>
          <button @click="handleFilter">筛选</button>
          <button @click="handleReset">重置</button>
        </div>
      </div>
    </div>

    <div v-if="dateError" class="filter-error">{{ dateError }}</div>

    <div v-if="errorMessage" class="state-banner error">
      <span>{{ errorMessage }}</span>
      <button class="btn-retry" :disabled="loading" @click="refreshList">重新加载</button>
    </div>

    <div v-if="loading" class="state-banner loading">调配台账加载中，请稍候...</div>

    <div v-if="!loading && !errorMessage && loaded && transferList.length === 0" class="state-banner empty">
      所选周期/区域/类型下暂无调配记录，可调整筛选条件后重新查询
    </div>

    <div class="summary-section" v-if="summary && !errorMessage">
      <div class="summary-item">
        <span class="summary-label">统计周期:</span>
        <span class="summary-value">{{ summary.period }}</span>
      </div>
      <div class="summary-item">
        <span class="summary-label">调配总量:</span>
        <span class="summary-value highlight">{{ summary.totalCount }} 次</span>
      </div>
      <div v-if="summary.scoped" class="summary-item">
        <span class="summary-label">调出本区域:</span>
        <span class="summary-value out">{{ summary.outboundCount }} 次</span>
      </div>
      <div v-if="summary.scoped" class="summary-item">
        <span class="summary-label">调入本区域:</span>
        <span class="summary-value in">{{ summary.inboundCount }} 次</span>
      </div>
      <div v-if="summary.scoped" class="summary-item summary-hint">
        调出 + 调入与看板「调配次数」同一口径（区域内互调同时计入两侧）
      </div>
    </div>

    <table v-if="!loading && !errorMessage" class="transfer-table">
      <thead>
        <tr>
          <th>调配单号</th>
          <th>方向</th>
          <th>设备编号</th>
          <th>设备名称</th>
          <th>原区域</th>
          <th>目标区域</th>
          <th>调配日期</th>
          <th>操作人</th>
          <th>状态</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="record in transferList" :key="record.id">
          <td>{{ record.transferNo }}</td>
          <td>
            <span :class="['dir-tag', directionClass(record)]" :title="directionTitle(record)">
              {{ directionText(record) }}
            </span>
          </td>
          <td>{{ record.equipmentNo }}</td>
          <td>{{ record.equipmentName }}</td>
          <td>{{ record.fromAreaName }}</td>
          <td>{{ record.toAreaName }}</td>
          <td>{{ formatDate(record.transferDate) }}</td>
          <td>{{ record.operator || '-' }}</td>
          <td>
            <span v-if="record.status !== 1" class="status cancelled">已取消</span>
            <span v-else :class="['status', record.signed ? 'signed' : 'unsigned']">
              {{ record.signed ? '已签收' : '待签收' }}
            </span>
          </td>
          <td>
            <button @click="handlePrint(record)">打印</button>
            <button v-if="record.status === 1" @click="handleCancel(record)">取消</button>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
  
  <TransferPrint :record="printRecord" :visible="printVisible" @close="printVisible = false" />
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { transferApi, areaApi, equipmentApi } from '../api'
import TransferPrint from './TransferPrint.vue'

const props = defineProps({
  initialFilters: {
    type: Object,
    default: () => ({})
  }
})
const emit = defineEmits(['back-dashboard'])

// 调配台账筛选本地持久化：刷新后仍保持与健康看板卡片一致的周期/区域/类型口径
const STORAGE_KEY = 'transfer-ledger-filters'

const transferList = ref([])
const summary = ref(null)
const startDate = ref('')
const endDate = ref('')
const extraFilters = ref({ areaId: '', equipmentType: '' })
const areaOptions = ref([])
const typeOptions = ref([])
const dateError = ref('')
const printRecord = ref(null)
const printVisible = ref(false)

// 列表状态：加载中 / 接口失败 / 是否已完成至少一次成功请求（区分初始与空结果）
const loading = ref(false)
const loaded = ref(false)
const errorMessage = ref('')

const hasDrillFilters = computed(() =>
  !!(props.initialFilters && Object.keys(props.initialFilters).length > 0))

const drillTip = computed(() => {
  if (!hasDrillFilters.value) return ''
  const parts = []
  if (startDate.value && endDate.value) {
    parts.push(`周期 ${startDate.value} 至 ${endDate.value}`)
  }
  if (extraFilters.value.areaId !== '') {
    const area = areaOptions.value.find(a => a.id === extraFilters.value.areaId)
    if (area) parts.push(`区域「${area.name}」（含调出/调入）`)
  }
  if (extraFilters.value.equipmentType) parts.push(`类型「${extraFilters.value.equipmentType}」`)
  return parts.length > 0 ? `来自健康看板的筛选：${parts.join('，')}` : ''
})

// 当前筛选区域及其下级区域 id（方向标签与后端 scope 口径一致：父区域含全部下级）
const scopeAreaIds = computed(() => {
  const rootId = extraFilters.value.areaId
  if (rootId === '' || rootId === null || rootId === undefined) return null
  const root = areaOptions.value.find(a => a.id === rootId)
  if (!root) return new Set([Number(rootId)])
  const rootPath = root.path ?? `/a${root.id}`
  const ids = new Set()
  for (const a of areaOptions.value) {
    if (a.id === root.id || (a.path && a.path.startsWith(rootPath + '/'))) {
      ids.add(Number(a.id))
    }
  }
  if (!ids.size) ids.add(Number(rootId))
  return ids
})

const isInScope = (areaId) => {
  const scope = scopeAreaIds.value
  if (scope === null) return true
  return scope.has(Number(areaId))
}

const directionText = (record) => {
  if (scopeAreaIds.value === null) return '调配'
  const out = isInScope(record.fromAreaId)
  const into = isInScope(record.toAreaId)
  if (out && into) return '区域内'
  if (into) return '调入'
  if (out) return '调出'
  return '调配'
}

const directionClass = (record) => {
  switch (directionText(record)) {
    case '调入': return 'dir-in'
    case '调出': return 'dir-out'
    case '区域内': return 'dir-inner'
    default: return 'dir-none'
  }
}

const directionTitle = (record) =>
  `调出：${record.fromAreaName || record.fromAreaId} → 调入：${record.toAreaName || record.toAreaId}`

const buildExtraParams = () => {
  const params = {}
  if (extraFilters.value.areaId !== '' && extraFilters.value.areaId !== null) {
    params.areaId = extraFilters.value.areaId
  }
  if (extraFilters.value.equipmentType !== '') params.equipmentType = extraFilters.value.equipmentType
  return params
}

const loadTransfers = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    const res = await transferApi.getAllTransfers()
    if (res.data.code === 200) {
      transferList.value = res.data.data
      summary.value = null
      loaded.value = true
    } else {
      errorMessage.value = res.data.message || '加载调配记录失败，请稍后重试'
    }
  } catch (error) {
    errorMessage.value = error.response?.data?.message
      || (error.code === 'ECONNABORTED' ? '接口请求超时，请稍后重试' : '加载调配记录失败，请检查网络或稍后重试')
    console.error('加载调配记录失败:', error)
  } finally {
    loading.value = false
  }
}

const validateDates = () => {
  dateError.value = ''
  if (!startDate.value || !endDate.value) {
    dateError.value = '请选择开始日期和结束日期'
    return false
  }
  if (startDate.value > endDate.value) {
    dateError.value = '开始日期不能晚于结束日期'
    return false
  }
  return true
}

// 周期内带区域/类型筛选的明细 + 汇总，二者与看板卡片共用同一口径接口
const handleFilter = async () => {
  if (!validateDates()) return

  loading.value = true
  errorMessage.value = ''
  const extraParams = buildExtraParams()
  try {
    const [listRes, summaryRes] = await Promise.all([
      transferApi.getTransfersByDateRange(startDate.value, endDate.value, extraParams),
      transferApi.getTransferSummary(startDate.value, endDate.value, extraParams)
    ])

    if (listRes.data.code !== 200) {
      errorMessage.value = listRes.data.message || '筛选调配记录失败，请稍后重试'
      return
    }
    if (summaryRes.data.code !== 200) {
      errorMessage.value = summaryRes.data.message || '调配汇总加载失败，请稍后重试'
      return
    }

    transferList.value = listRes.data.data
    const data = summaryRes.data.data
    // 双保险：后端已按同口径返回，这里再以明细条数兜底，确保「汇总=明细条数」永不脱节
    data.totalCount = listRes.data.data.length
    data.scoped = Object.keys(extraParams).length > 0
    summary.value = data
    loaded.value = true
    persistFilters()
  } catch (error) {
    errorMessage.value = error.response?.data?.message
      || (error.code === 'ECONNABORTED' ? '接口请求超时，请稍后重试' : '筛选调配记录失败，请检查网络或稍后重试')
    console.error('筛选调配记录失败:', error)
  } finally {
    loading.value = false
  }
}

const persistFilters = () => {
  localStorage.setItem(STORAGE_KEY, JSON.stringify({
    startDate: startDate.value,
    endDate: endDate.value,
    areaId: extraFilters.value.areaId,
    equipmentType: extraFilters.value.equipmentType
  }))
}

const clearPersistedFilters = () => {
  localStorage.removeItem(STORAGE_KEY)
}

const loadSavedFilters = () => {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY) || 'null')
  } catch (e) {
    return null
  }
}

const resetFilters = () => {
  startDate.value = ''
  endDate.value = ''
  extraFilters.value = { areaId: '', equipmentType: '' }
  dateError.value = ''
  summary.value = null
  loaded.value = false
}

const handleReset = () => {
  resetFilters()
  clearPersistedFilters()
  loadTransfers()
}

const clearDrillFilters = () => {
  resetFilters()
  clearPersistedFilters()
  loadTransfers()
}

const backToDashboard = () => {
  emit('back-dashboard')
}

const loadAreas = async () => {
  try {
    const res = await areaApi.getAllAreas()
    if (res.data.code === 200) {
      areaOptions.value = res.data.data.filter(a => a.status === 1)
    }
  } catch (error) {
    console.error('加载区域列表失败:', error)
  }
}

const loadTypes = async () => {
  try {
    const res = await equipmentApi.getEquipmentTypes()
    if (res.data.code === 200) {
      typeOptions.value = res.data.data
    }
  } catch (error) {
    console.error('加载设备类型失败:', error)
  }
}

// 初始筛选优先级：看板下钻参数 > 本地持久化（刷新场景）> 空
const applyInitialFilters = () => {
  if (hasDrillFilters.value) {
    const init = props.initialFilters
    startDate.value = init.startDate || ''
    endDate.value = init.endDate || ''
    extraFilters.value = {
      areaId: init.areaId ?? '',
      equipmentType: init.equipmentType || ''
    }
    return
  }
  const saved = loadSavedFilters()
  if (saved && saved.startDate && saved.endDate) {
    startDate.value = saved.startDate
    endDate.value = saved.endDate
    extraFilters.value = {
      areaId: saved.areaId ?? '',
      equipmentType: saved.equipmentType || ''
    }
  }
}

const handlePrint = (record) => {
  printRecord.value = record
  printVisible.value = true
}

// 取消调配后按当前口径刷新：有周期走筛选接口，无周期走全量，保持条数持续对齐
const refreshList = () => {
  if (startDate.value && endDate.value) {
    handleFilter()
  } else {
    loadTransfers()
  }
}

const handleCancel = async (record) => {
  const unsignedTip = record.signed
    ? '该调配单已到货签收，取消后设备当前位置将按最近一次已签收调配重新计算。确定取消吗？'
    : '该调配单尚未到货签收，取消后设备仍在调出地。确定取消吗？'
  if (!confirm(unsignedTip)) return

  try {
    const res = await transferApi.cancelTransfer(record.id)
    if (res.data.code === 200) {
      refreshList()
    } else {
      alert('取消失败: ' + (res.data.message || '未知错误'))
    }
  } catch (error) {
    const message = error.response?.data?.message
      || (error.code === 'ECONNABORTED' ? '接口请求超时，取消未提交，请稍后重试' : '取消调配记录失败，请稍后重试')
    alert('取消失败: ' + message)
    console.error('取消调配记录失败:', error)
  }
}

const formatDate = (date) => {
  if (!date) return '-'
  return date
}

onMounted(async () => {
  applyInitialFilters()
  await Promise.all([loadAreas(), loadTypes()])
  refreshList()
})
</script>

<style scoped>
.transfer-list {
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 16px;
  background: #fff;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}

.list-header h3 {
  margin: 0;
  font-size: 16px;
  color: #333;
  border-bottom: 1px solid #e0e0e0;
  padding-bottom: 8px;
  flex: 1;
  min-width: 200px;
}

.date-filter {
  display: flex;
  align-items: center;
  gap: 8px;
}

.date-filter input {
  padding: 6px 10px;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  font-size: 14px;
}

.date-separator {
  color: #999;
}

.date-filter button {
  padding: 6px 16px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.date-filter button:first-of-type {
  background: #409eff;
  color: #fff;
}

.date-filter button:last-of-type {
  background: #f0f0f0;
  color: #666;
}

.filter-section {
  padding: 0;
  background: transparent;
}

.drill-banner {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 10px;
  padding: 8px 12px;
  background: #fdf6ec;
  border: 1px solid #f5dab1;
  border-radius: 4px;
  color: #e6a23c;
  font-size: 13px;
}

.drill-banner .btn-back {
  padding: 3px 12px;
  border: none;
  border-radius: 4px;
  background: #409eff;
  color: #fff;
  cursor: pointer;
  font-size: 12px;
}

.drill-banner .btn-clear-drill {
  padding: 3px 12px;
  border: 1px solid #e6a23c;
  border-radius: 4px;
  background: #fff;
  color: #e6a23c;
  cursor: pointer;
  font-size: 12px;
}

.date-filter select {
  padding: 6px 10px;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  font-size: 14px;
  background: #fff;
}

.filter-error {
  margin: -8px 0 16px;
  padding: 8px 12px;
  background: #fef0f0;
  border: 1px solid #fbc4c4;
  border-radius: 4px;
  color: #f56c6c;
  font-size: 13px;
}

.summary-section {
  display: flex;
  gap: 24px;
  margin-bottom: 16px;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 4px;
  flex-wrap: wrap;
  align-items: center;
}

.summary-item {
  display: flex;
  gap: 8px;
}

.summary-label {
  color: #999;
  font-size: 14px;
}

.summary-value {
  color: #333;
  font-weight: bold;
  font-size: 14px;
}

.summary-value.highlight {
  color: #409eff;
  font-size: 18px;
}

.summary-value.out {
  color: #e6a23c;
}

.summary-value.in {
  color: #67c23a;
}

.summary-hint {
  color: #909399;
  font-size: 12px;
  width: 100%;
}

.state-banner {
  padding: 12px 16px;
  border-radius: 4px;
  margin-bottom: 16px;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.state-banner.error {
  background: #fef0f0;
  color: #f56c6c;
  border: 1px solid #fbc4c4;
}

.state-banner.loading {
  background: #ecf5ff;
  color: #409eff;
  border: 1px solid #b3d8ff;
}

.state-banner.empty {
  background: #fdf6ec;
  color: #e6a23c;
  border: 1px solid #f5dab1;
}

.btn-retry {
  padding: 4px 14px;
  border: none;
  border-radius: 4px;
  background: #f56c6c;
  color: #fff;
  cursor: pointer;
  font-size: 13px;
  white-space: nowrap;
}

.btn-retry:disabled {
  background: #fab6b6;
  cursor: not-allowed;
}

.dir-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  white-space: nowrap;
}

.dir-tag.dir-out {
  background: #fdf6ec;
  color: #e6a23c;
}

.dir-tag.dir-in {
  background: #f0f9eb;
  color: #67c23a;
}

.dir-tag.dir-inner {
  background: #ecf5ff;
  color: #409eff;
}

.dir-tag.dir-none {
  background: #f4f4f5;
  color: #909399;
}

.transfer-table {
  width: 100%;
  border-collapse: collapse;
}

.transfer-table th,
.transfer-table td {
  padding: 12px 8px;
  text-align: left;
  border-bottom: 1px solid #e0e0e0;
  font-size: 14px;
}

.transfer-table th {
  background: #f8f9fa;
  color: #666;
  font-weight: bold;
}

.status.signed {
  color: #67c23a;
}

.status.unsigned {
  color: #e6a23c;
}

.status.cancelled {
  color: #f56c6c;
}

.transfer-table td button {
  padding: 4px 10px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  margin-right: 4px;
}

.transfer-table td button:first-child {
  background: #409eff;
  color: #fff;
}

.transfer-table td button:last-child {
  background: #f56c6c;
  color: #fff;
}

.empty {
  text-align: center;
  color: #999;
  padding: 40px;
}
</style>
