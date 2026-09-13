
<template>
  <div class="receipt-list">
    <div class="list-header">
      <h3>到货签收</h3>
      <div class="filter-section">
        <div class="filter-row">
          <label class="filter-label">调配日</label>
          <input type="date" v-model="filters.startDate" />
          <span class="separator">至</span>
          <input type="date" v-model="filters.endDate" />
          <label class="filter-label">目标区域</label>
          <select v-model="filters.toAreaId">
            <option value="">全部区域</option>
            <option v-for="area in areaOptions" :key="area.id" :value="area.id">{{ area.name }}</option>
          </select>
          <label class="filter-label">外观</label>
          <select v-model="filters.appearance">
            <option value="">全部</option>
            <option value="unsigned">待签收</option>
            <option value="intact">外观完好</option>
            <option value="damaged">外观有破损</option>
          </select>
          <span class="scope-hint">选择父区域时含全部下级母婴室</span>
          <button class="btn-filter" @click="handleFilter">筛选</button>
          <button class="btn-reset" @click="handleReset">重置</button>
        </div>
        <p v-if="filterError" class="filter-error">{{ filterError }}</p>
      </div>
    </div>

    <div v-if="loadError" class="load-banner error">
      <span>{{ loadError }}</span>
      <button class="btn-retry" :disabled="loading" @click="loadReceipts">
        {{ loading ? '加载中...' : '重新加载' }}
      </button>
    </div>

    <div v-if="loading" class="load-banner loading">签收台账加载中，请稍候...</div>

    <div v-if="!loading && !loadError && loaded && receiptList.length === 0" class="load-banner empty">
      当前筛选条件下没有匹配的调配单，可调整外观、调配日或目标区域后重新查询
    </div>

    <table v-if="!loading && !loadError" class="receipt-table">
      <thead>
        <tr>
          <th>调配单号</th>
          <th>设备编号</th>
          <th>设备名称</th>
          <th>原区域</th>
          <th>目标区域</th>
          <th>调配日期</th>
          <th>签收状态</th>
          <th>签收人</th>
          <th>到货时间</th>
          <th>外观结论</th>
          <th>破损部位</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="record in receiptList" :key="record.id"
            :class="{ clickable: true }" @click="handleDetail(record)">
          <td>{{ record.transferNo }}</td>
          <td>{{ record.equipmentNo }}</td>
          <td>{{ record.equipmentName }}</td>
          <td>{{ record.fromAreaName }}</td>
          <td>{{ record.toAreaName }}</td>
          <td>{{ record.transferDate }}</td>
          <td>
            <span :class="['sign-tag', record.signed ? 'signed' : 'unsigned']">
              {{ record.signed ? '已签收' : '待签收' }}
            </span>
          </td>
          <td>{{ record.signed ? (record.receiver || '-') : '-' }}</td>
          <td>{{ record.signed ? formatDateTime(record.arrivalTime) : '-' }}</td>
          <td>
            <span v-if="record.signed"
                  :class="['conclusion', record.appearanceIntact ? 'intact' : 'damaged']">
              {{ record.appearanceIntact ? '完好' : '有破损' }}
            </span>
            <span v-else>-</span>
          </td>
          <td>
            <span v-if="record.signed && !record.appearanceIntact" class="damage-part">
              {{ record.damagePart || '未登记破损部位' }}
            </span>
            <span v-else>-</span>
          </td>
          <td @click.stop>
            <button class="btn-detail" @click="handleDetail(record)">详情</button>
            <button v-if="!record.signed" class="btn-sign" @click="handleSign(record)">签收</button>
            <span v-else class="signed-hint">已完成</span>
          </td>
        </tr>
      </tbody>
    </table>

    <TransferReceipt :visible="signVisible" :record="signRecord"
                     @close="signVisible = false" @signed="handleSigned" />
    <TransferReceiptDetail :visible="detailVisible" :transfer-id="detailId"
                           @close="detailVisible = false" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { transferApi, areaApi } from '../api'
import TransferReceipt from './TransferReceipt.vue'
import TransferReceiptDetail from './TransferReceiptDetail.vue'

// 签收台账筛选本地持久化：刷新后签收标记与能否调出仍与服务端同一口径
const FILTER_STORAGE_KEY = 'transfer-receipt-filters'

const receiptList = ref([])
const areaOptions = ref([])
const loading = ref(false)
const loaded = ref(false)
const loadError = ref('')
const filterError = ref('')
const signVisible = ref(false)
const signRecord = ref(null)
const detailVisible = ref(false)
const detailId = ref(null)

const defaultFilters = () => ({ startDate: '', endDate: '', toAreaId: '', appearance: '' })

const filters = ref(defaultFilters())

const loadSavedFilters = () => {
  try {
    const saved = localStorage.getItem(FILTER_STORAGE_KEY)
    if (saved) {
      return { ...defaultFilters(), ...JSON.parse(saved) }
    }
  } catch (error) {
    console.warn('读取本地签收筛选条件失败:', error)
  }
  return defaultFilters()
}

const persistFilters = () => {
  localStorage.setItem(FILTER_STORAGE_KEY, JSON.stringify(filters.value))
}

const buildParams = () => {
  const params = {}
  if (filters.value.startDate) params.startDate = filters.value.startDate
  if (filters.value.endDate) params.endDate = filters.value.endDate
  if (filters.value.toAreaId !== '') params.toAreaId = filters.value.toAreaId
  if (filters.value.appearance !== '') params.appearance = filters.value.appearance
  return params
}

const resolveErrorMessage = (error, fallback) => error.response?.data?.message
  || (error.code === 'ECONNABORTED' ? '接口请求超时，请稍后重试' : fallback)

const loadReceipts = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const res = await transferApi.getTransferReceipts(buildParams())
    if (res.data.code === 200) {
      receiptList.value = res.data.data
      loaded.value = true
    } else {
      loadError.value = res.data.message || '加载签收台账失败，请稍后重试'
    }
  } catch (error) {
    loadError.value = resolveErrorMessage(error, '加载签收台账失败，请检查网络或稍后重试')
    console.error('加载签收台账失败:', error)
  } finally {
    loading.value = false
  }
}

const validateDates = () => {
  const { startDate, endDate } = filters.value
  if ((startDate && !endDate) || (!startDate && endDate)) {
    filterError.value = '请同时选择开始日期和结束日期，或都不选'
    return false
  }
  if (startDate && endDate && startDate > endDate) {
    filterError.value = '开始日期不能晚于结束日期'
    return false
  }
  filterError.value = ''
  return true
}

// 按当前筛选条件查询并持久化；签收标记、能否调出全部以服务端返回为准
const handleFilter = () => {
  if (!validateDates()) return
  persistFilters()
  loadReceipts()
}

const handleReset = () => {
  filters.value = defaultFilters()
  filterError.value = ''
  localStorage.removeItem(FILTER_STORAGE_KEY)
  loadReceipts()
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

const handleSign = (record) => {
  signRecord.value = record
  signVisible.value = true
}

const handleDetail = (record) => {
  detailId.value = record.id
  detailVisible.value = true
}

// 签收（或他人已签收导致的重复签收提示）后，按当前筛选口径刷新
const handleSigned = () => {
  signVisible.value = false
  loadReceipts()
}

const formatDateTime = (value) => {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 16)
}

onMounted(async () => {
  filters.value = loadSavedFilters()
  await loadAreas()
  await loadReceipts()
})
</script>

<style scoped>
.receipt-list {
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 16px;
  background: #fff;
}

.list-header {
  margin-bottom: 16px;
}

.list-header h3 {
  margin: 0 0 12px;
  font-size: 16px;
  color: #333;
  border-bottom: 1px solid #e0e0e0;
  padding-bottom: 8px;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.filter-label {
  color: #666;
  font-size: 14px;
}

.filter-row input,
.filter-row select {
  padding: 6px 10px;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  font-size: 14px;
  background: #fff;
}

.separator {
  color: #999;
}

.scope-hint {
  color: #909399;
  font-size: 12px;
}

.btn-filter {
  padding: 6px 16px;
  border: none;
  border-radius: 4px;
  background: #409eff;
  color: #fff;
  cursor: pointer;
  font-size: 14px;
}

.btn-reset {
  padding: 6px 16px;
  border: none;
  border-radius: 4px;
  background: #f0f0f0;
  color: #666;
  cursor: pointer;
  font-size: 14px;
}

.filter-error {
  margin: 10px 0 0;
  padding: 8px 12px;
  background: #fef0f0;
  border: 1px solid #fbc4c4;
  border-radius: 4px;
  color: #f56c6c;
  font-size: 13px;
}

.load-banner {
  padding: 12px 16px;
  border-radius: 4px;
  margin-bottom: 16px;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.load-banner.error {
  background: #fef0f0;
  color: #f56c6c;
  border: 1px solid #fbc4c4;
}

.load-banner.loading {
  background: #ecf5ff;
  color: #409eff;
  border: 1px solid #b3d8ff;
}

.load-banner.empty {
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

.receipt-table {
  width: 100%;
  border-collapse: collapse;
}

.receipt-table th,
.receipt-table td {
  padding: 12px 8px;
  text-align: left;
  border-bottom: 1px solid #e0e0e0;
  font-size: 14px;
}

.receipt-table th {
  background: #f8f9fa;
  color: #666;
  font-weight: bold;
}

tr.clickable {
  cursor: pointer;
}

.sign-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  white-space: nowrap;
}

.sign-tag.signed {
  background: #f0f9eb;
  color: #67c23a;
}

.sign-tag.unsigned {
  background: #fdf6ec;
  color: #e6a23c;
}

.conclusion {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.conclusion.intact {
  color: #67c23a;
  background: #f0f9eb;
}

.conclusion.damaged {
  color: #f56c6c;
  background: #fef0f0;
}

.damage-part {
  color: #f56c6c;
  font-size: 13px;
}

.btn-detail,
.btn-sign {
  padding: 4px 10px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  margin-right: 4px;
}

.btn-detail {
  background: #409eff;
  color: #fff;
}

.btn-sign {
  background: #67c23a;
  color: #fff;
}

.signed-hint {
  color: #909399;
  font-size: 12px;
}
</style>
