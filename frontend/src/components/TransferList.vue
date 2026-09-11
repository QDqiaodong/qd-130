
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

    <div class="summary-section" v-if="summary">
      <div class="summary-item">
        <span class="summary-label">统计周期:</span>
        <span class="summary-value">{{ summary.period }}</span>
      </div>
      <div class="summary-item">
        <span class="summary-label">调配总量:</span>
        <span class="summary-value highlight">{{ summary.totalCount }} 次</span>
      </div>
    </div>
    
    <table class="transfer-table">
      <thead>
        <tr>
          <th>调配单号</th>
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
          <td>{{ record.equipmentNo }}</td>
          <td>{{ record.equipmentName }}</td>
          <td>{{ record.fromAreaName }}</td>
          <td>{{ record.toAreaName }}</td>
          <td>{{ formatDate(record.transferDate) }}</td>
          <td>{{ record.operator || '-' }}</td>
          <td :class="['status', record.status === 1 ? 'active' : 'cancelled']">
            {{ record.status === 1 ? '已完成' : '已取消' }}
          </td>
          <td>
            <button @click="handlePrint(record)">打印</button>
            <button v-if="record.status === 1" @click="handleCancel(record)">取消</button>
          </td>
        </tr>
        <tr v-if="transferList.length === 0">
          <td colspan="9" class="empty">暂无调配记录</td>
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

const drillTip = computed(() => {
  if (!props.initialFilters || Object.keys(props.initialFilters).length === 0) return ''
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

const loadTransfers = async () => {
  dateError.value = ''
  try {
    const res = await transferApi.getAllTransfers()
    if (res.data.code === 200) {
      transferList.value = res.data.data
    } else {
      alert(res.data.message || '加载调配记录失败')
    }
  } catch (error) {
    alert(error.response?.data?.message || '加载调配记录失败，请稍后重试')
    console.error('加载调配记录失败:', error)
  }
}

const buildExtraParams = () => {
  const params = {}
  if (extraFilters.value.areaId !== '') params.areaId = extraFilters.value.areaId
  if (extraFilters.value.equipmentType !== '') params.equipmentType = extraFilters.value.equipmentType
  return params
}

const handleFilter = async () => {
  dateError.value = ''
  if (!startDate.value || !endDate.value) {
    dateError.value = '请选择开始日期和结束日期'
    return
  }
  if (startDate.value > endDate.value) {
    dateError.value = '开始日期不能晚于结束日期'
    return
  }

  try {
    const extraParams = buildExtraParams()
    const res = await transferApi.getTransfersByDateRange(startDate.value, endDate.value, extraParams)
    if (res.data.code === 200) {
      transferList.value = res.data.data
    } else {
      alert(res.data.message || '筛选调配记录失败')
      return
    }

    const summaryRes = await transferApi.getTransferSummary(startDate.value, endDate.value)
    if (summaryRes.data.code === 200) {
      const data = summaryRes.data.data
      // 看板下钻带区域/类型条件时，总量与明细保持一致（汇总接口仅按日期统计，需用明细长度覆盖）
      if (extraParams.areaId || extraParams.equipmentType) {
        data.totalCount = transferList.value.length
      }
      summary.value = data
    }
  } catch (error) {
    dateError.value = error.response?.data?.message || '筛选调配记录失败，请稍后重试'
    console.error('筛选调配记录失败:', error)
  }
}

const handleReset = () => {
  startDate.value = ''
  endDate.value = ''
  extraFilters.value = { areaId: '', equipmentType: '' }
  dateError.value = ''
  summary.value = null
  loadTransfers()
}

const clearDrillFilters = () => {
  startDate.value = ''
  endDate.value = ''
  extraFilters.value = { areaId: '', equipmentType: '' }
  dateError.value = ''
  summary.value = null
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

const applyInitialFilters = () => {
  const init = props.initialFilters || {}
  startDate.value = init.startDate || ''
  endDate.value = init.endDate || ''
  extraFilters.value = {
    areaId: init.areaId ?? '',
    equipmentType: init.equipmentType || ''
  }
}

const handlePrint = (record) => {
  printRecord.value = record
  printVisible.value = true
}

const refreshList = () => {
  if (startDate.value && endDate.value) {
    handleFilter()
  } else {
    loadTransfers()
  }
}

const handleCancel = async (record) => {
  if (!confirm('确定要取消这条调配记录吗？')) return

  try {
    const res = await transferApi.cancelTransfer(record.id)
    if (res.data.code === 200) {
      refreshList()
    } else {
      alert(res.data.message || '取消调配记录失败')
    }
  } catch (error) {
    alert(error.response?.data?.message || '取消调配记录失败')
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
  if (startDate.value && endDate.value) {
    handleFilter()
  } else {
    loadTransfers()
  }
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

.status.active {
  color: #67c23a;
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
