
<template>
  <div class="inspection-list">
    <div class="list-header">
      <h3>巡检记录</h3>
      <button class="add-btn" @click="formVisible = true">登记巡检</button>
    </div>

    <div class="filter-section">
      <div v-if="drillTip" class="drill-banner">
        <span>{{ drillTip }}</span>
        <button class="btn-back" @click="backToDashboard">返回看板</button>
        <button class="btn-clear-drill" @click="clearDrillFilters">清除看板筛选</button>
      </div>
      <div class="filter-row">
        <label class="filter-label">巡检日</label>
        <input type="date" v-model="filters.startDate" placeholder="开始日期" />
        <span class="separator">至</span>
        <input type="date" v-model="filters.endDate" placeholder="结束日期" />
        <label class="filter-label">母婴室</label>
        <select v-model="filters.areaId">
          <option value="">全部母婴室</option>
          <option v-for="area in areaOptions" :key="area.id" :value="area.id">{{ area.name }}</option>
        </select>
        <select v-model="filters.equipmentType">
          <option value="">全部设备类型</option>
          <option v-for="type in typeOptions" :key="type" :value="type">{{ type }}</option>
        </select>
        <select v-model="filters.result">
          <option value="">全部结果</option>
          <option :value="1">正常</option>
          <option :value="2">异常</option>
        </select>
        <select v-model="filters.reviewStatus">
          <option value="">全部复核状态</option>
          <option :value="-1">待复核</option>
          <option :value="1">属实</option>
          <option :value="2">不属实</option>
        </select>
        <select v-model="filters.repairStatus">
          <option value="">全部维修状态</option>
          <option :value="-1">未报修</option>
          <option :value="0">待处理</option>
          <option :value="1">维修中</option>
          <option :value="2">已恢复</option>
        </select>
        <button class="btn-filter" @click="applyFilters">筛选</button>
        <button class="btn-reset" @click="handleReset">重置</button>
      </div>
      <p v-if="filterError" class="filter-error">{{ filterError }}</p>
    </div>

    <div v-if="loadError" class="load-banner error">
      <span>{{ loadError }}</span>
      <button class="btn-retry" :disabled="loading" @click="loadInspections">
        {{ loading ? '加载中...' : '重试' }}
      </button>
    </div>

    <table class="inspection-table">
      <thead>
        <tr>
          <th>巡检单号</th>
          <th>设备编号</th>
          <th>设备名称</th>
          <th>区域</th>
          <th>巡检日期</th>
          <th>巡检结果</th>
          <th>复核状态</th>
          <th>异常描述</th>
          <th>维修状态</th>
          <th>巡检员</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="record in inspectionList" :key="record.id">
          <td>{{ record.inspectionNo }}</td>
          <td>{{ record.equipmentNo }}</td>
          <td>{{ record.equipmentName }}</td>
          <td>{{ record.areaName || '-' }}</td>
          <td>{{ record.inspectionDate }}</td>
          <td>
            <span :class="['result-tag', record.result === 1 ? 'normal' : 'abnormal']">
              {{ record.result === 1 ? '正常' : '异常' }}
            </span>
          </td>
          <td>
            <span v-if="record.result === 2"
                  :class="['review-tag', reviewStatusClass(record.reviewResult)]">
              {{ reviewStatusText(record.reviewResult) }}
            </span>
            <span v-else class="no-review">无需复核</span>
          </td>
          <td class="desc-cell" :title="record.abnormalDesc">{{ record.abnormalDesc || '-' }}</td>
          <td>
            <span v-if="record.repairStatus !== null && record.repairStatus !== undefined"
                  :class="['repair-tag', repairStatusClass(record.repairStatus)]">
              {{ repairStatusText(record.repairStatus) }}
            </span>
            <span v-else class="no-repair">未报修</span>
          </td>
          <td>{{ record.inspector || '-' }}</td>
          <td>
            <button class="btn-detail" @click="handleDetail(record)">详情</button>
            <button v-if="record.result === 2 && record.reviewResult === null"
                    class="btn-review" @click="handleReview(record)">复核</button>
            <button v-if="record.canRepair"
                    class="btn-repair" @click="handleCreateRepair(record)">报修</button>
            <span v-if="record.result === 2 && record.reviewResult === 2" class="no-repair">不可报修</span>
          </td>
        </tr>
        <tr v-if="!loadError && inspectionList.length === 0">
          <td colspan="11" class="empty">{{ loading ? '加载中...' : '暂无巡检记录' }}</td>
        </tr>
      </tbody>
    </table>

    <InspectionForm :visible="formVisible" @close="formVisible = false" @success="handleFormSuccess" />
    <InspectionDetail :visible="detailVisible" :inspection-id="detailId" @close="detailVisible = false" />
    <InspectionReview :visible="reviewVisible" :inspection="reviewRecord"
                      @close="reviewVisible = false" @reviewed="handleReviewed" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { inspectionApi, repairApi, areaApi, equipmentApi } from '../api'
import InspectionForm from './InspectionForm.vue'
import InspectionDetail from './InspectionDetail.vue'
import InspectionReview from './InspectionReview.vue'

const FILTER_STORAGE_KEY = 'inspection.list.filters'

const props = defineProps({
  initialFilters: {
    type: Object,
    default: () => ({})
  }
})
const emit = defineEmits(['back-dashboard'])

const inspectionList = ref([])
const areaOptions = ref([])
const typeOptions = ref([])
const loading = ref(false)
const loadError = ref('')
const filterError = ref('')
const formVisible = ref(false)
const detailVisible = ref(false)
const detailId = ref(null)
const reviewVisible = ref(false)
const reviewRecord = ref(null)

const defaultFilters = () => ({
  startDate: '',
  endDate: '',
  areaId: '',
  equipmentType: '',
  result: '',
  reviewStatus: '',
  repairStatus: ''
})

const loadSavedFilters = () => {
  try {
    const saved = localStorage.getItem(FILTER_STORAGE_KEY)
    if (saved) {
      return { ...defaultFilters(), ...JSON.parse(saved) }
    }
  } catch (error) {
    console.warn('读取本地巡检筛选条件失败:', error)
  }
  return defaultFilters()
}

const filters = ref(defaultFilters())

const persistFilters = () => {
  try {
    localStorage.setItem(FILTER_STORAGE_KEY, JSON.stringify(filters.value))
  } catch (error) {
    console.warn('保存巡检筛选条件失败:', error)
  }
}

const drillTip = computed(() => {
  const parts = []
  if (filters.value.startDate && filters.value.endDate) {
    parts.push(`周期 ${filters.value.startDate} 至 ${filters.value.endDate}`)
  }
  if (filters.value.areaId !== '') {
    const area = areaOptions.value.find(a => a.id === filters.value.areaId)
    if (area) parts.push(`区域「${area.name}」`)
  }
  if (filters.value.equipmentType) parts.push(`类型「${filters.value.equipmentType}」`)
  if (filters.value.result === 2) parts.push('仅异常巡检')
  if (props.initialFilters && Object.keys(props.initialFilters).length > 0 && parts.length > 0) {
    return `来自健康看板的筛选：${parts.join('，')}`
  }
  return ''
})

const validateDateRange = () => {
  if (filters.value.startDate && filters.value.endDate
      && filters.value.startDate > filters.value.endDate) {
    filterError.value = '巡检日开始日期不能晚于结束日期'
    return false
  }
  filterError.value = ''
  return true
}

const loadInspections = async () => {
  if (!validateDateRange()) {
    inspectionList.value = []
    return
  }
  loading.value = true
  loadError.value = ''
  try {
    const params = {}
    if (filters.value.startDate) params.startDate = filters.value.startDate
    if (filters.value.endDate) params.endDate = filters.value.endDate
    if (filters.value.areaId !== '') params.areaId = filters.value.areaId
    if (filters.value.equipmentType !== '') params.equipmentType = filters.value.equipmentType
    if (filters.value.result !== '') params.result = filters.value.result
    if (filters.value.reviewStatus !== '') params.reviewStatus = filters.value.reviewStatus
    if (filters.value.repairStatus !== '') params.repairStatus = filters.value.repairStatus

    const res = await inspectionApi.getInspections(params)
    if (res.data.code === 200) {
      inspectionList.value = res.data.data
    } else {
      loadError.value = res.data.message || '加载巡检记录失败，请稍后重试'
    }
  } catch (error) {
    loadError.value = error.response?.data?.message || '接口请求失败，巡检记录加载失败，请稍后重试'
    console.error('加载巡检记录失败:', error)
  } finally {
    loading.value = false
  }
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
  // 看板下钻条件优先，其次沿用本地保存的筛选，刷新后口径保持一致
  filters.value = { ...defaultFilters(), ...loadSavedFilters(), ...init }
}

const applyFilters = () => {
  persistFilters()
  loadInspections()
}

const handleReset = () => {
  filters.value = defaultFilters()
  persistFilters()
  loadInspections()
}

const clearDrillFilters = () => {
  filters.value = defaultFilters()
  persistFilters()
  loadInspections()
}

const backToDashboard = () => {
  emit('back-dashboard')
}

const handleDetail = (record) => {
  detailId.value = record.id
  detailVisible.value = true
}

const handleReview = (record) => {
  reviewRecord.value = record
  reviewVisible.value = true
}

const handleReviewed = () => {
  loadInspections()
}

const handleCreateRepair = async (record) => {
  if (!confirm(`确定为设备「${record.equipmentName}」创建报修单吗？维修期间设备不可调配。`)) return

  try {
    const res = await repairApi.createRepair({ inspectionId: record.id, reporter: record.inspector })
    if (res.data.code === 200) {
      loadInspections()
    } else {
      alert(res.data.message || '创建报修单失败，请稍后重试')
      loadInspections()
    }
  } catch (error) {
    alert(error.response?.data?.message || '接口请求失败，报修单未创建，请稍后重试')
    loadInspections()
  }
}

const handleFormSuccess = () => {
  persistFilters()
  loadInspections()
}

const reviewStatusText = (reviewResult) => {
  if (reviewResult === 1) return '属实'
  if (reviewResult === 2) return '不属实'
  return '待复核'
}

const reviewStatusClass = (reviewResult) => {
  if (reviewResult === 1) return 'confirmed'
  if (reviewResult === 2) return 'rejected'
  return 'pending'
}

const repairStatusText = (status) => {
  if (status === 0) return '待处理'
  if (status === 1) return '维修中'
  if (status === 2) return '已恢复'
  return '-'
}

const repairStatusClass = (status) => {
  if (status === 0) return 'pending'
  if (status === 1) return 'repairing'
  if (status === 2) return 'restored'
  return ''
}

onMounted(() => {
  applyInitialFilters()
  loadAreas()
  loadTypes()
  loadInspections()
})
</script>

<style scoped>
.inspection-list {
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 16px;
  background: #fff;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.list-header h3 {
  margin: 0;
  font-size: 16px;
  color: #333;
  border-bottom: 1px solid #e0e0e0;
  padding-bottom: 8px;
  flex: 1;
  margin-right: 16px;
}

.add-btn {
  padding: 8px 20px;
  border: none;
  border-radius: 4px;
  background: #67c23a;
  color: #fff;
  cursor: pointer;
  font-size: 14px;
}

.add-btn:hover {
  background: #85ce61;
}

.filter-section {
  margin-bottom: 16px;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 4px;
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

.btn-back {
  padding: 3px 12px;
  border: none;
  border-radius: 4px;
  background: #409eff;
  color: #fff;
  cursor: pointer;
  font-size: 12px;
}

.btn-clear-drill {
  padding: 3px 12px;
  border: 1px solid #e6a23c;
  border-radius: 4px;
  background: #fff;
  color: #e6a23c;
  cursor: pointer;
  font-size: 12px;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.filter-label {
  font-size: 13px;
  color: #666;
}

.filter-error {
  margin: 8px 0 0;
  color: #f56c6c;
  font-size: 13px;
}

.load-banner {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 4px;
  margin-bottom: 12px;
  font-size: 13px;
}

.load-banner.error {
  background: #fef0f0;
  border: 1px solid #fbc4c4;
  color: #f56c6c;
}

.btn-retry {
  padding: 4px 14px;
  border: none;
  border-radius: 4px;
  background: #409eff;
  color: #fff;
  cursor: pointer;
  font-size: 12px;
}

.btn-retry:disabled {
  background: #a0cfff;
  cursor: not-allowed;
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

.inspection-table {
  width: 100%;
  border-collapse: collapse;
}

.inspection-table th,
.inspection-table td {
  padding: 12px 8px;
  text-align: left;
  border-bottom: 1px solid #e0e0e0;
  font-size: 14px;
}

.inspection-table th {
  background: #f8f9fa;
  color: #666;
  font-weight: bold;
}

.desc-cell {
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.result-tag {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.result-tag.normal {
  background: #f0f9eb;
  color: #67c23a;
}

.result-tag.abnormal {
  background: #fef0f0;
  color: #f56c6c;
}

.review-tag {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.review-tag.pending {
  background: #fdf6ec;
  color: #e6a23c;
}

.review-tag.confirmed {
  background: #f0f9eb;
  color: #67c23a;
}

.review-tag.rejected {
  background: #f4f4f5;
  color: #909399;
}

.no-review {
  color: #c0c4cc;
  font-size: 12px;
}

.repair-tag {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.repair-tag.pending {
  background: #fdf6ec;
  color: #e6a23c;
}

.repair-tag.repairing {
  background: #ecf5ff;
  color: #409eff;
}

.repair-tag.restored {
  background: #f0f9eb;
  color: #67c23a;
}

.no-repair {
  color: #909399;
  font-size: 12px;
}

.inspection-table td button {
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

.btn-review {
  background: #67c23a;
  color: #fff;
}

.btn-repair {
  background: #e6a23c;
  color: #fff;
}

.empty {
  text-align: center;
  color: #999;
  padding: 40px;
}
</style>
