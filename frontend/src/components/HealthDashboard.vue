<template>
  <div class="health-dashboard">
    <div class="list-header">
      <h3>设备健康分析看板</h3>
    </div>

    <div class="filter-section">
      <div class="filter-row">
        <label class="filter-label">统计周期</label>
        <input type="date" v-model="filters.startDate" :max="today" />
        <span class="separator">至</span>
        <input type="date" v-model="filters.endDate" :max="today" />
        <label class="filter-label">区域</label>
        <select v-model="filters.areaId">
          <option value="">全部区域</option>
          <option v-for="area in areaOptions" :key="area.id" :value="area.id">{{ area.name }}</option>
        </select>
        <button class="btn-filter" :disabled="loading" @click="handleQuery">
          {{ loading ? '统计中...' : '查询' }}
        </button>
        <button class="btn-reset" :disabled="loading" @click="handleReset">重置</button>
        <span v-if="dashboard" class="period-tip">
          当前统计：{{ dashboard.startDate }} 至 {{ dashboard.endDate }}<template v-if="dashboard.areaName">（{{ dashboard.areaName }}）</template>
        </span>
      </div>
      <p v-if="dateError" class="filter-error">{{ dateError }}</p>
    </div>

    <div v-if="errorMessage" class="state-banner error">
      <span>{{ errorMessage }}</span>
      <button class="btn-retry" :disabled="loading" @click="loadDashboard">重新加载</button>
    </div>

    <template v-else>
      <div v-if="loading" class="state-banner loading">统计数据加载中，请稍候...</div>

      <div v-if="emptyMessage" class="state-banner empty">
        {{ emptyMessage }}，可调整统计周期或区域后重新查询
      </div>

      <div v-if="dashboard && !loading" class="kpi-grid">
        <div class="kpi-card clickable" @click="goEquipment()">
          <div class="kpi-title">设备总数</div>
          <div class="kpi-value">{{ m.totalEquipmentCount }}</div>
          <div class="kpi-desc">台 · 查看设备明细</div>
        </div>

        <div class="kpi-card warning clickable" @click="goRepair({ status: 1, dateScoped: false, currentArea: true })">
          <div class="kpi-title">当前维修数</div>
          <div class="kpi-value">{{ m.repairingCount }}</div>
          <div class="kpi-desc">台 · 维修中（当前状态，不受周期限制）</div>
        </div>

        <div class="kpi-card clickable" @click="goInspection()">
          <div class="kpi-title">巡检完成率</div>
          <div class="kpi-value">{{ formatRate(m.inspectionCompletionRate) }}</div>
          <div class="kpi-desc">
            实际 {{ m.inspectionCompletedCount }} / 计划 {{ m.inspectionPlannedCount }} · 查看巡检明细
          </div>
        </div>

        <div class="kpi-card danger clickable" @click="goInspection({ result: 2 })">
          <div class="kpi-title">异常率</div>
          <div class="kpi-value">{{ formatRate(m.abnormalRate) }}</div>
          <div class="kpi-desc">
            异常 {{ m.abnormalCount }} / 巡检 {{ m.inspectionCount }} · 查看异常巡检
          </div>
        </div>

        <div class="kpi-card warning clickable" @click="goRepair({ status: 0, dateScoped: false, currentArea: true })">
          <div class="kpi-title">待处理报修数</div>
          <div class="kpi-value">{{ m.pendingRepairCount }}</div>
          <div class="kpi-desc">单 · 当前待处理（点击查看明细）</div>
        </div>

        <div class="kpi-card clickable" @click="goTransfer()">
          <div class="kpi-title">调配次数</div>
          <div class="kpi-value">{{ m.transferCount }}</div>
          <div class="kpi-desc">次 · 周期内有效调配 · 查看调配明细</div>
        </div>
      </div>

      <div v-if="dashboard && !loading" class="rank-grid">
        <div class="rank-panel">
          <div class="rank-header">
            <h4>区域排行</h4>
            <span class="rank-tip">按设备总数排序，点击数字查看明细</span>
          </div>
          <table class="rank-table">
            <thead>
              <tr>
                <th>区域</th>
                <th>设备总数</th>
                <th>维修中</th>
                <th>巡检完成率</th>
                <th>异常率</th>
                <th>待处理</th>
                <th>调配(出/入)</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in dashboard.areaRankings" :key="row.areaId">
                <td class="rank-name">{{ row.areaName }}</td>
                <td>
                  <button class="link-btn" @click="goEquipment({ areaId: row.areaId })">{{ row.totalEquipmentCount }}</button>
                </td>
                <td>
                  <button class="link-btn warn" @click="goRepair({ areaId: row.areaId, status: 1, dateScoped: false, currentArea: true })">
                    {{ row.repairingCount }}
                  </button>
                </td>
                <td>
                  <button class="link-btn" :title="`实际 ${row.inspectionCompletedCount} / 计划 ${row.inspectionPlannedCount}`"
                          @click="goInspection({ areaId: row.areaId })">
                    {{ formatRate(row.inspectionCompletionRate) }}
                  </button>
                </td>
                <td>
                  <button class="link-btn danger" @click="goInspection({ areaId: row.areaId, result: 2 })">
                    {{ formatRate(row.abnormalRate) }}
                  </button>
                </td>
                <td>
                  <button class="link-btn warn" @click="goRepair({ areaId: row.areaId, status: 0, dateScoped: false, currentArea: true })">
                    {{ row.pendingRepairCount }}
                  </button>
                </td>
                <td>
                  <button class="link-btn" @click="goTransfer({ areaId: row.areaId })">
                    {{ row.transferOutCount }} / {{ row.transferInCount }}
                  </button>
                </td>
              </tr>
              <tr v-if="dashboard.areaRankings.length === 0">
                <td colspan="7" class="empty">该区域下暂无在管设备</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="rank-panel">
          <div class="rank-header">
            <h4>设备类型排行</h4>
            <span class="rank-tip">按设备总数排序，点击数字查看明细</span>
          </div>
          <table class="rank-table">
            <thead>
              <tr>
                <th>设备类型</th>
                <th>设备总数</th>
                <th>维修中</th>
                <th>巡检完成率</th>
                <th>异常率</th>
                <th>待处理</th>
                <th>调配次数</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in dashboard.typeRankings" :key="row.equipmentType">
                <td class="rank-name">{{ row.equipmentType }}</td>
                <td>
                  <button class="link-btn" @click="goEquipment({ equipmentType: row.equipmentType })">{{ row.totalEquipmentCount }}</button>
                </td>
                <td>
                  <button class="link-btn warn" @click="goRepair({ status: 1, equipmentType: row.equipmentType, dateScoped: false, currentArea: true })">
                    {{ row.repairingCount }}
                  </button>
                </td>
                <td>
                  <button class="link-btn" :title="`实际 ${row.inspectionCompletedCount} / 计划 ${row.inspectionPlannedCount}`"
                          @click="goInspection({ equipmentType: row.equipmentType })">
                    {{ formatRate(row.inspectionCompletionRate) }}
                  </button>
                </td>
                <td>
                  <button class="link-btn danger" @click="goInspection({ equipmentType: row.equipmentType, result: 2 })">
                    {{ formatRate(row.abnormalRate) }}
                  </button>
                </td>
                <td>
                  <button class="link-btn warn" @click="goRepair({ status: 0, equipmentType: row.equipmentType, dateScoped: false, currentArea: true })">
                    {{ row.pendingRepairCount }}
                  </button>
                </td>
                <td>
                  <button class="link-btn" @click="goTransfer({ equipmentType: row.equipmentType })">
                    {{ row.transferCount }}
                  </button>
                </td>
              </tr>
              <tr v-if="dashboard.typeRankings.length === 0">
                <td colspan="7" class="empty">暂无设备类型数据</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { dashboardApi, areaApi } from '../api'

const emit = defineEmits(['drilldown'])

const STORAGE_KEY = 'health-dashboard-filters'

const today = new Date().toISOString().slice(0, 10)
const defaultStart = () => {
  const d = new Date()
  d.setDate(d.getDate() - 29)
  return d.toISOString().slice(0, 10)
}

const defaultFilters = () => ({
  startDate: defaultStart(),
  endDate: today,
  areaId: ''
})

const loadSavedFilters = () => {
  try {
    const saved = JSON.parse(localStorage.getItem(STORAGE_KEY) || 'null')
    if (!saved) return defaultFilters()
    return {
      startDate: saved.startDate || defaultStart(),
      endDate: saved.endDate || today,
      areaId: saved.areaId || ''
    }
  } catch (e) {
    return defaultFilters()
  }
}

const filters = ref(loadSavedFilters())
const areaOptions = ref([])
const dashboard = ref(null)
const loading = ref(false)
const errorMessage = ref('')
const dateError = ref('')

const m = computed(() => dashboard.value?.metrics || {})

const emptyMessage = computed(() => {
  if (!dashboard.value) return ''
  const metrics = dashboard.value.metrics
  const hasAnyData = (metrics.totalEquipmentCount || 0) > 0
    || (metrics.inspectionCount || 0) > 0
    || (metrics.pendingRepairCount || 0) > 0
    || (metrics.repairingCount || 0) > 0
    || (metrics.transferCount || 0) > 0
  if (!hasAnyData) {
    return '所选条件下暂无设备、巡检、报修及调配数据'
  }
  return ''
})

const isValidDate = (v) => /^\d{4}-\d{2}-\d{2}$/.test(v) && !Number.isNaN(Date.parse(v))

const validateDates = () => {
  dateError.value = ''
  if (!filters.value.startDate || !filters.value.endDate) {
    dateError.value = '请同时选择开始日期和结束日期'
    return false
  }
  if (!isValidDate(filters.value.startDate) || !isValidDate(filters.value.endDate)) {
    dateError.value = '日期格式不合法，正确格式为 yyyy-MM-dd'
    return false
  }
  if (filters.value.startDate > filters.value.endDate) {
    dateError.value = '开始日期不能晚于结束日期'
    return false
  }
  if (filters.value.endDate > today) {
    dateError.value = '结束日期不能晚于今天'
    return false
  }
  return true
}

const persistFilters = () => {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(filters.value))
}

const loadDashboard = async () => {
  if (!validateDates()) {
    return
  }
  loading.value = true
  errorMessage.value = ''
  try {
    const params = {
      startDate: filters.value.startDate,
      endDate: filters.value.endDate
    }
    if (filters.value.areaId !== '') params.areaId = filters.value.areaId

    const res = await dashboardApi.getHealthDashboard(params)
    if (res.data.code === 200) {
      dashboard.value = res.data.data
      persistFilters()
    } else {
      errorMessage.value = res.data.message || '看板数据加载失败，请稍后重试'
    }
  } catch (error) {
    errorMessage.value = error.response?.data?.message
      || (error.code === 'ECONNABORTED' ? '接口请求超时，请稍后重试' : '看板接口请求失败，请检查网络或稍后重试')
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  loadDashboard()
}

const handleReset = () => {
  filters.value = defaultFilters()
  dateError.value = ''
  errorMessage.value = ''
  persistFilters()
  loadDashboard()
}

const loadAreas = async () => {
  try {
    const res = await areaApi.getAllAreas()
    if (res.data.code === 200) {
      areaOptions.value = res.data.data.filter(a => a.status === 1)
    }
  } catch (error) {
    // 区域下拉失败不阻塞看板主数据加载，仅记录日志
    console.error('加载区域列表失败:', error)
  }
}

const baseDrillParams = (overrides = {}) => {
  const params = { ...overrides }
  if (params.areaId === undefined && filters.value.areaId !== '') {
    params.areaId = filters.value.areaId
  }
  return params
}

const goInspection = (overrides = {}) => {
  const params = baseDrillParams(overrides)
  params.startDate = filters.value.startDate
  params.endDate = filters.value.endDate
  emit('drilldown', { target: 'inspection', params })
}

const goRepair = (overrides = {}) => {
  const { dateScoped, currentArea, ...rest } = overrides
  const params = baseDrillParams(rest)
  // 看板卡片的当前状态指标按「设备当前所在区域」统计，明细查询使用同一口径
  if (currentArea) {
    params.equipmentCurrentAreaId = rest.areaId !== undefined
      ? rest.areaId
      : (filters.value.areaId !== '' ? filters.value.areaId : undefined)
    delete params.areaId
  }
  // 当前维修数/待处理报修数是当前状态指标，不传日期，保证点击明细与卡片数字一致
  if (dateScoped) {
    params.startDate = filters.value.startDate
    params.endDate = filters.value.endDate
  }
  emit('drilldown', { target: 'repair', params })
}

const goTransfer = (overrides = {}) => {
  const params = baseDrillParams(overrides)
  params.startDate = filters.value.startDate
  params.endDate = filters.value.endDate
  emit('drilldown', { target: 'transfer', params })
}

const goEquipment = (overrides = {}) => {
  const params = baseDrillParams(overrides)
  emit('drilldown', { target: 'equipment', params })
}

const formatRate = (value) => {
  if (value === null || value === undefined) return '—'
  return `${value}%`
}

onMounted(() => {
  loadAreas()
  loadDashboard()
})
</script>

<style scoped>
.health-dashboard {
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
}

.filter-section {
  margin-bottom: 16px;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 4px;
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

.btn-filter {
  padding: 6px 16px;
  border: none;
  border-radius: 4px;
  background: #409eff;
  color: #fff;
  cursor: pointer;
  font-size: 14px;
}

.btn-filter:disabled {
  background: #a0cfff;
  cursor: not-allowed;
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

.period-tip {
  color: #909399;
  font-size: 13px;
}

.filter-error {
  margin: 8px 0 0;
  color: #f56c6c;
  font-size: 13px;
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

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.kpi-card {
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 16px 20px;
  background: #fff;
  border-left: 4px solid #409eff;
}

.kpi-card.warning {
  border-left-color: #e6a23c;
}

.kpi-card.danger {
  border-left-color: #f56c6c;
}

.kpi-card.clickable {
  cursor: pointer;
  transition: box-shadow 0.2s, transform 0.2s;
}

.kpi-card.clickable:hover {
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.18);
  transform: translateY(-2px);
}

.kpi-title {
  color: #909399;
  font-size: 13px;
  margin-bottom: 8px;
}

.kpi-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 6px;
}

.kpi-desc {
  color: #c0c4cc;
  font-size: 12px;
}

.rank-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.rank-panel {
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 12px 16px 16px;
  background: #fff;
}

.rank-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 12px;
  flex-wrap: wrap;
  gap: 8px;
}

.rank-header h4 {
  margin: 0;
  font-size: 15px;
  color: #333;
  border-left: 3px solid #409eff;
  padding-left: 8px;
}

.rank-tip {
  color: #c0c4cc;
  font-size: 12px;
}

.rank-table {
  width: 100%;
  border-collapse: collapse;
}

.rank-table th,
.rank-table td {
  padding: 10px 6px;
  text-align: left;
  border-bottom: 1px solid #f0f0f0;
  font-size: 13px;
  white-space: nowrap;
}

.rank-table th {
  background: #f8f9fa;
  color: #666;
  font-weight: bold;
}

.rank-name {
  color: #303133;
  font-weight: 500;
}

.link-btn {
  border: none;
  background: none;
  color: #409eff;
  cursor: pointer;
  font-size: 13px;
  padding: 0;
}

.link-btn:hover {
  text-decoration: underline;
}

.link-btn.warn {
  color: #e6a23c;
}

.link-btn.danger {
  color: #f56c6c;
}

.empty {
  text-align: center;
  color: #999;
  padding: 32px;
}

@media (max-width: 1100px) {
  .kpi-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .rank-grid {
    grid-template-columns: 1fr;
  }
}
</style>
