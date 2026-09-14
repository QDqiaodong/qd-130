<template>
  <div class="patrol-list">
    <div class="list-header">
      <h3>夜间巡更打卡</h3>
      <button class="add-btn" @click="openCreateForm">巡更打卡</button>
    </div>

    <div class="range-tip">值班员按母婴室对前夜班（22:00-02:00）、后夜班（02:00-06:00）分别打卡；同一母婴室同一班次只能打一次，漏打的房间单独列在下方</div>

    <div class="filter-section">
      <div class="filter-row">
        <label class="filter-label">巡更日期</label>
        <input type="date" v-model="filters.patrolDate" />

        <label class="filter-label">班次</label>
        <select v-model="filters.shift">
          <option value="">全部班次</option>
          <option v-for="opt in shiftOptions" :key="opt.value" :value="String(opt.value)">{{ opt.label }}</option>
        </select>

        <label class="filter-label">母婴室</label>
        <select v-model="filters.areaId" :disabled="areasLoading">
          <option value="">{{ areasLoading ? '母婴室加载中...' : '全部在用母婴室' }}</option>
          <option v-for="area in areaOptions" :key="area.id" :value="String(area.id)">{{ area.name }}</option>
        </select>

        <button class="btn-filter" @click="applyFilters">筛选</button>
        <button class="btn-reset" @click="handleReset">重置</button>
      </div>
      <p v-if="filterError" class="filter-error">{{ filterError }}</p>
      <p v-if="areaError" class="filter-error">
        {{ areaError }}
        <button class="inline-retry" type="button" :disabled="areasLoading" @click="loadAreas">
          {{ areasLoading ? '重试中...' : '重试' }}
        </button>
      </p>
    </div>

    <div v-if="loadError" class="load-banner error">
      <span>{{ loadError }}</span>
      <button class="btn-retry" :disabled="loading" @click="loadBoard">
        {{ loading ? '加载中...' : '重试' }}
      </button>
    </div>

    <template v-if="!loadError">
      <div v-if="!loading && records.length === 0" class="empty-banner">
        <span>当前筛选条件下暂无打卡记录</span>
        <button type="button" class="empty-reset" @click="handleReset">清空筛选条件</button>
      </div>

      <table class="patrol-table" v-if="records.length > 0">
        <thead>
          <tr>
            <th>打卡单号</th>
            <th>母婴室</th>
            <th>巡更日期</th>
            <th>班次</th>
            <th>巡更人</th>
            <th>打卡时间</th>
            <th>备注</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="record in records" :key="record.id">
            <td>{{ record.checkinNo }}</td>
            <td>{{ record.areaName || '-' }}</td>
            <td>{{ record.patrolDate }}</td>
            <td>
              <span :class="['shift-tag', record.shift === 1 ? 'first' : 'second']">
                {{ record.shiftName || '-' }}
              </span>
            </td>
            <td>{{ record.patrolPerson || '-' }}</td>
            <td>{{ formatDateTime(record.checkinTime) }}</td>
            <td class="remark-cell">{{ record.remark || '-' }}</td>
          </tr>
        </tbody>
      </table>

      <div class="missed-panel">
        <div class="missed-header">
          <h4>漏打房间（{{ boardDate || '-' }} · {{ currentShiftLabel }}）</h4>
          <span v-if="missedRooms.length > 0" class="missed-count">漏打 {{ missedRooms.length }} 间次</span>
        </div>

        <div v-if="!loading && missedRooms.length === 0" class="missed-empty">
          当前班次没有漏打的房间，所有在用母婴室均已打卡
        </div>

        <table class="patrol-table" v-if="missedRooms.length > 0">
          <thead>
            <tr>
              <th>母婴室</th>
              <th>漏打班次</th>
              <th>巡更日期</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="room in missedRooms" :key="`${room.areaId}-${room.shift}`" class="row-missed">
              <td>{{ room.areaName || '-' }}</td>
              <td>
                <span :class="['shift-tag', room.shift === 1 ? 'first' : 'second']">
                  {{ room.shiftName || '-' }}
                </span>
              </td>
              <td>{{ room.patrolDate }}</td>
              <td>
                <button class="btn-fill" @click="openFillForm(room)">补打卡</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </template>

    <PatrolCheckinForm :visible="formVisible" :preset="formPreset"
                       @close="formVisible = false" @success="handleFormSuccess" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { patrolApi, areaApi } from '../api'
import PatrolCheckinForm from './PatrolCheckinForm.vue'

const FILTER_STORAGE_KEY = 'patrolCheckin.list.filters'

const SHIFT_OPTIONS = [
  { value: 1, label: '前夜班（22:00-02:00）' },
  { value: 2, label: '后夜班（02:00-06:00）' }
]
const shiftOptions = SHIFT_OPTIONS

const records = ref([])
const missedRooms = ref([])
const boardDate = ref('')
const areaOptions = ref([])
const loading = ref(false)
const areasLoading = ref(false)
const loadError = ref('')
const areaError = ref('')
const filterError = ref('')
const formVisible = ref(false)
const formPreset = ref({})

const todayStr = () => new Date().toLocaleDateString('sv-SE')

const defaultFilters = () => ({
  patrolDate: todayStr(),
  shift: '',
  areaId: ''
})

const loadSavedFilters = () => {
  try {
    const saved = localStorage.getItem(FILTER_STORAGE_KEY)
    if (saved) {
      return { ...defaultFilters(), ...JSON.parse(saved) }
    }
  } catch (error) {
    console.warn('读取本地夜间巡更筛选条件失败:', error)
  }
  return defaultFilters()
}

const filters = ref(loadSavedFilters())

const persistFilters = () => {
  try {
    localStorage.setItem(FILTER_STORAGE_KEY, JSON.stringify(filters.value))
  } catch (error) {
    console.warn('保存夜间巡更筛选条件失败:', error)
  }
}

const currentShiftLabel = computed(() => {
  if (filters.value.shift === '') return '全部班次'
  const opt = SHIFT_OPTIONS.find(o => String(o.value) === String(filters.value.shift))
  return opt ? opt.label : '全部班次'
})

const loadBoard = async () => {
  if (!filters.value.patrolDate) {
    filterError.value = '请选择巡更日期'
    records.value = []
    missedRooms.value = []
    return
  }
  if (filters.value.areaId !== '' && areaOptions.value.length > 0
      && !areaOptions.value.some(area => String(area.id) === String(filters.value.areaId))) {
    filterError.value = '所选母婴室不存在或已停用，请重新选择区域'
    records.value = []
    missedRooms.value = []
    return
  }
  filterError.value = ''
  loading.value = true
  loadError.value = ''
  try {
    const params = { patrolDate: filters.value.patrolDate }
    if (filters.value.shift !== '') params.shift = filters.value.shift
    if (filters.value.areaId !== '') params.areaId = filters.value.areaId

    const res = await patrolApi.getBoard(params)
    if (res.data.code === 200) {
      const board = res.data.data || {}
      records.value = board.records || []
      missedRooms.value = board.missedRooms || []
      boardDate.value = board.patrolDate || filters.value.patrolDate
    } else {
      records.value = []
      missedRooms.value = []
      loadError.value = res.data.message || '加载巡更打卡记录失败，请稍后重试'
    }
  } catch (error) {
    records.value = []
    missedRooms.value = []
    loadError.value = error.response?.data?.message || '接口请求失败，巡更打卡记录加载失败，请稍后重试'
    console.error('加载巡更看板失败:', error)
  } finally {
    loading.value = false
  }
}

const loadAreas = async () => {
  areasLoading.value = true
  areaError.value = ''
  try {
    const res = await areaApi.getAllAreas()
    if (res.data.code === 200) {
      areaOptions.value = (res.data.data || []).filter(a => a.status === 1)
      if (filters.value.areaId !== ''
          && !areaOptions.value.some(area => String(area.id) === String(filters.value.areaId))) {
        filters.value.areaId = ''
        filterError.value = '原筛选母婴室不存在或已停用，已切换为全部在用母婴室'
      }
    } else {
      areaOptions.value = []
      areaError.value = res.data.message || '加载母婴室列表失败，请稍后重试'
    }
  } catch (error) {
    areaOptions.value = []
    areaError.value = error.response?.data?.message || '接口请求失败，母婴室列表加载失败，请稍后重试'
    console.error('加载区域列表失败:', error)
  } finally {
    areasLoading.value = false
  }
}

const applyFilters = () => {
  persistFilters()
  loadBoard()
}

const handleReset = () => {
  filters.value = defaultFilters()
  persistFilters()
  loadBoard()
}

const openCreateForm = () => {
  formPreset.value = {
    patrolDate: filters.value.patrolDate || todayStr(),
    shift: filters.value.shift === '' ? '' : Number(filters.value.shift)
  }
  formVisible.value = true
}

const openFillForm = (room) => {
  formPreset.value = {
    areaId: room.areaId,
    patrolDate: room.patrolDate,
    shift: room.shift
  }
  formVisible.value = true
}

const handleFormSuccess = () => {
  persistFilters()
  loadBoard()
}

const formatDateTime = (value) => {
  if (!value) return '-'
  return value.replace('T', ' ').substring(0, 16)
}

onMounted(() => {
  loadAreas()
  loadBoard()
})
</script>

<style scoped>
.patrol-list {
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

.filter-row select,
.filter-row input[type="date"] {
  padding: 6px 10px;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  font-size: 14px;
  background: #fff;
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

.inline-retry {
  margin-left: 8px;
  padding: 2px 10px;
  border: none;
  border-radius: 4px;
  background: #409eff;
  color: #fff;
  cursor: pointer;
  font-size: 12px;
}

.inline-retry:hover:not(:disabled) {
  background: #66b1ff;
}

.inline-retry:disabled {
  background: #a0cfff;
  cursor: not-allowed;
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

.patrol-table {
  width: 100%;
  border-collapse: collapse;
}

.patrol-table th,
.patrol-table td {
  padding: 12px 8px;
  text-align: left;
  border-bottom: 1px solid #e0e0e0;
  font-size: 14px;
}

.patrol-table th {
  background: #f8f9fa;
  color: #666;
  font-weight: bold;
}

.patrol-table tbody tr.row-missed {
  background: #fffbf0;
}

.shift-tag {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  white-space: nowrap;
}

.shift-tag.first {
  background: #ecf5ff;
  color: #409eff;
}

.shift-tag.second {
  background: #f4f4f5;
  color: #909399;
}

.remark-cell {
  max-width: 200px;
  word-break: break-all;
  color: #666;
  font-size: 13px;
}

.missed-panel {
  margin-top: 20px;
  border: 1px solid #fde2c4;
  border-radius: 8px;
  padding: 12px 16px 16px;
  background: #fffdf8;
}

.missed-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}

.missed-header h4 {
  margin: 0;
  font-size: 14px;
  color: #b88230;
}

.missed-count {
  padding: 2px 10px;
  border-radius: 10px;
  background: #fdf6ec;
  color: #e6a23c;
  font-size: 12px;
}

.missed-empty {
  padding: 20px;
  border: 1px dashed #f0c78a;
  border-radius: 4px;
  color: #67c23a;
  background: #fbfdf8;
  font-size: 13px;
  text-align: center;
}

.patrol-table td button {
  padding: 4px 10px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
}

.btn-fill {
  background: #e6a23c;
  color: #fff;
  white-space: nowrap;
}

.btn-fill:hover {
  background: #ebb563;
}

.empty-banner {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 32px;
  border: 1px dashed #dcdfe6;
  border-radius: 4px;
  color: #909399;
  background: #fafafa;
  font-size: 14px;
}

.empty-reset {
  padding: 4px 12px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: #fff;
  color: #606266;
  cursor: pointer;
  font-size: 12px;
}

.empty-reset:hover {
  border-color: #409eff;
  color: #409eff;
}
</style>
