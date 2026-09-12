<template>
  <div class="disinfection-list">
    <div class="list-header">
      <h3>母婴室消毒登记</h3>
      <button class="add-btn" @click="openCreateForm">登记消毒</button>
    </div>

    <div class="range-tip">通风做完才算当日闭环；同一母婴室当天不能重复闭环，未闭环可再次登记补齐</div>

    <div class="filter-section">
      <div class="filter-row">
        <label class="filter-label">消毒日</label>
        <input type="date" v-model="filters.startDate" />
        <span class="separator">至</span>
        <input type="date" v-model="filters.endDate" />

        <label class="filter-label">母婴室</label>
        <select v-model="filters.areaId">
          <option value="">全部母婴室</option>
          <option v-for="area in areaOptions" :key="area.id" :value="area.id">{{ area.name }}</option>
        </select>

        <select v-model="filters.closedLoop">
          <option value="">全部闭环状态</option>
          <option value="true">已闭环</option>
          <option value="false">未闭环</option>
        </select>

        <button class="btn-filter" @click="applyFilters">筛选</button>
        <button class="btn-reset" @click="handleReset">重置</button>
      </div>
      <p v-if="filterError" class="filter-error">{{ filterError }}</p>
    </div>

    <div v-if="loadError" class="load-banner error">
      <span>{{ loadError }}</span>
      <button class="btn-retry" :disabled="loading" @click="loadDisinfections">
        {{ loading ? '加载中...' : '重试' }}
      </button>
    </div>

    <table class="disinfection-table">
      <thead>
        <tr>
          <th>登记单号</th>
          <th>母婴室</th>
          <th>消毒日期</th>
          <th>消毒人</th>
          <th>完成时间</th>
          <th>消毒液</th>
          <th>通风</th>
          <th>闭环标记</th>
          <th>未完成原因</th>
          <th>能否再登记</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="record in disinfectionList" :key="record.id"
            :class="{ 'row-open': record.closedLoop === false }">
          <td>{{ record.disinfectionNo }}</td>
          <td>{{ record.areaName || '-' }}</td>
          <td>{{ record.disinfectDate }}</td>
          <td>{{ record.operator || '-' }}</td>
          <td>{{ formatDateTime(record.finishTime) }}</td>
          <td>{{ record.disinfectant || '-' }}</td>
          <td>
            <span :class="['vent-tag', record.ventilationDone ? 'done' : 'undone']">
              {{ record.ventilationDone ? '已做完' : '未做完' }}
            </span>
          </td>
          <td>
            <span :class="['loop-tag', record.closedLoop ? 'closed' : 'open']">
              {{ record.closedLoop ? '已闭环' : '未闭环' }}
            </span>
          </td>
          <td class="reason-cell">{{ record.closedLoop ? '-' : (record.incompleteReason || '-') }}</td>
          <td>
            <span v-if="record.canRegisterAgain" class="can-register">可再登记</span>
            <span v-else class="cannot-register">当日已闭环，不可再登记</span>
          </td>
          <td>
            <button v-if="record.canRegisterAgain" class="btn-again"
                    @click="openAgainForm(record)">再次登记</button>
            <span v-else class="no-action">-</span>
          </td>
        </tr>
        <tr v-if="!loadError && disinfectionList.length === 0">
          <td colspan="11" class="empty">{{ loading ? '加载中...' : '当前筛选条件下暂无消毒登记记录' }}</td>
        </tr>
      </tbody>
    </table>

    <DisinfectionForm :visible="formVisible" :preset-area-id="presetAreaId" :preset-date="presetDate"
                      @close="formVisible = false" @success="handleFormSuccess" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { disinfectionApi, areaApi } from '../api'
import DisinfectionForm from './DisinfectionForm.vue'

const FILTER_STORAGE_KEY = 'disinfection.list.filters'

const disinfectionList = ref([])
const areaOptions = ref([])
const loading = ref(false)
const loadError = ref('')
const filterError = ref('')
const formVisible = ref(false)
const presetAreaId = ref('')
const presetDate = ref('')

const defaultFilters = () => ({
  startDate: '',
  endDate: '',
  areaId: '',
  closedLoop: ''
})

const loadSavedFilters = () => {
  try {
    const saved = localStorage.getItem(FILTER_STORAGE_KEY)
    if (saved) {
      return { ...defaultFilters(), ...JSON.parse(saved) }
    }
  } catch (error) {
    console.warn('读取本地消毒登记筛选条件失败:', error)
  }
  return defaultFilters()
}

const filters = ref(loadSavedFilters())

const persistFilters = () => {
  try {
    localStorage.setItem(FILTER_STORAGE_KEY, JSON.stringify(filters.value))
  } catch (error) {
    console.warn('保存消毒登记筛选条件失败:', error)
  }
}

const validateDateRange = () => {
  if (filters.value.startDate && filters.value.endDate
      && filters.value.startDate > filters.value.endDate) {
    filterError.value = '消毒日开始日期不能晚于结束日期'
    return false
  }
  filterError.value = ''
  return true
}

const loadDisinfections = async () => {
  if (!validateDateRange()) {
    disinfectionList.value = []
    return
  }
  loading.value = true
  loadError.value = ''
  try {
    const params = {}
    if (filters.value.startDate) params.startDate = filters.value.startDate
    if (filters.value.endDate) params.endDate = filters.value.endDate
    if (filters.value.areaId !== '') params.areaId = filters.value.areaId
    if (filters.value.closedLoop !== '') params.closedLoop = filters.value.closedLoop

    const res = await disinfectionApi.getDisinfections(params)
    if (res.data.code === 200) {
      disinfectionList.value = res.data.data
    } else {
      loadError.value = res.data.message || '加载消毒登记记录失败，请稍后重试'
    }
  } catch (error) {
    loadError.value = error.response?.data?.message || '接口请求失败，消毒登记记录加载失败，请稍后重试'
    console.error('加载消毒登记记录失败:', error)
  } finally {
    loading.value = false
  }
}

const loadAreas = async () => {
  try {
    const res = await areaApi.getAllAreas()
    if (res.data.code === 200) {
      areaOptions.value = res.data.data.filter(a => a.status === 1)
    } else {
      alert(res.data.message || '加载母婴室列表失败')
    }
  } catch (error) {
    alert(error.response?.data?.message || '加载母婴室列表失败，请稍后重试')
    console.error('加载区域列表失败:', error)
  }
}

const applyFilters = () => {
  persistFilters()
  loadDisinfections()
}

const handleReset = () => {
  filters.value = defaultFilters()
  persistFilters()
  loadDisinfections()
}

const openCreateForm = () => {
  presetAreaId.value = ''
  presetDate.value = ''
  formVisible.value = true
}

const openAgainForm = (record) => {
  presetAreaId.value = record.areaId
  presetDate.value = record.disinfectDate
  formVisible.value = true
}

const handleFormSuccess = () => {
  persistFilters()
  loadDisinfections()
}

const formatDateTime = (value) => {
  if (!value) return '-'
  return value.replace('T', ' ').substring(0, 16)
}

onMounted(() => {
  loadAreas()
  loadDisinfections()
})
</script>

<style scoped>
.disinfection-list {
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 16px;
  background: #fff;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
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

.range-tip {
  font-size: 12px;
  color: #909399;
  margin-bottom: 12px;
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
  font-size: 13px;
  color: #666;
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

.disinfection-table {
  width: 100%;
  border-collapse: collapse;
}

.disinfection-table th,
.disinfection-table td {
  padding: 12px 8px;
  text-align: left;
  border-bottom: 1px solid #e0e0e0;
  font-size: 14px;
}

.disinfection-table th {
  background: #f8f9fa;
  color: #666;
  font-weight: bold;
}

.disinfection-table tbody tr.row-open {
  background: #fffbfb;
}

.vent-tag,
.loop-tag {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  white-space: nowrap;
}

.vent-tag.done,
.loop-tag.closed {
  background: #f0f9eb;
  color: #67c23a;
}

.vent-tag.undone,
.loop-tag.open {
  background: #fef0f0;
  color: #f56c6c;
}

.reason-cell {
  max-width: 220px;
  word-break: break-all;
  color: #666;
  font-size: 13px;
}

.can-register {
  color: #e6a23c;
  font-size: 13px;
  white-space: nowrap;
}

.cannot-register {
  color: #909399;
  font-size: 13px;
  white-space: nowrap;
}

.disinfection-table td button {
  padding: 4px 10px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
}

.btn-again {
  background: #e6a23c;
  color: #fff;
}

.no-action {
  color: #c0c4cc;
}

.empty {
  text-align: center;
  color: #999;
  padding: 40px;
}
</style>
