
<template>
  <div class="spotcheck-list">
    <div class="list-header">
      <h3>温奶器温度抽检</h3>
      <button class="add-btn" @click="formVisible = true">登记抽检</button>
    </div>

    <div class="range-tip">合格水温区间 40.0℃ ~ 50.0℃，抽检结论按实测温度自动判定</div>

    <div class="filter-section">
      <div class="filter-row">
        <label class="filter-label">抽检日</label>
        <input type="date" v-model="filters.startDate" />
        <span class="separator">至</span>
        <input type="date" v-model="filters.endDate" />

        <label class="filter-label">母婴室</label>
        <select v-model="filters.areaId">
          <option value="">全部母婴室</option>
          <option v-for="area in areaOptions" :key="area.id" :value="area.id">{{ area.name }}</option>
        </select>

        <select v-model="filters.qualified">
          <option value="">全部结论</option>
          <option value="true">合格</option>
          <option value="false">不合格</option>
        </select>

        <select v-model="filters.repairStatus">
          <option value="">全部报修状态</option>
          <option value="-1">未报修</option>
          <option value="0">待处理</option>
          <option value="1">维修中</option>
          <option value="2">已恢复</option>
        </select>

        <select v-model="filters.reviewStatus">
          <option value="">全部复核情况</option>
          <option value="1">已补复核人</option>
          <option value="0">未补复核人</option>
        </select>

        <label class="filter-label">体温枪编号</label>
        <input type="text" v-model="filters.thermometerNo" placeholder="按体温枪编号筛选" />

        <button class="btn-filter" @click="applyFilters">筛选</button>
        <button class="btn-reset" @click="handleReset">重置</button>
      </div>
      <p v-if="filterError" class="filter-error">{{ filterError }}</p>
    </div>

    <div v-if="loadError" class="load-banner error">
      <span>{{ loadError }}</span>
      <button class="btn-retry" :disabled="loading" @click="loadSpotChecks">
        {{ loading ? '加载中...' : '重试' }}
      </button>
    </div>

    <table class="spotcheck-table">
      <thead>
        <tr>
          <th>抽检单号</th>
          <th>温奶器</th>
          <th>母婴室</th>
          <th>抽检日</th>
          <th>实测水温</th>
          <th>体温枪编号</th>
          <th>抽检结论</th>
          <th>照片</th>
          <th>报修状态</th>
          <th>抽检人</th>
          <th>当班复核人</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="record in spotCheckList" :key="record.id"
            :class="{ 'row-unqualified': record.qualified === false }">
          <td>{{ record.spotCheckNo }}</td>
          <td>{{ record.equipmentName }}（{{ record.equipmentNo }}）</td>
          <td>{{ record.areaName || '-' }}</td>
          <td>{{ record.checkDate }}</td>
          <td :class="{ 'temp-bad': record.qualified === false }">{{ record.temperature }}℃</td>
          <td>{{ record.thermometerNo || '-' }}</td>
          <td>
            <span :class="['result-tag', record.qualified ? 'normal' : 'abnormal']">
              {{ record.qualified ? '合格' : '不合格' }}
            </span>
          </td>
          <td>
            <a v-if="record.photoUrl" :href="record.photoUrl" target="_blank" class="photo-link">查看照片</a>
            <span v-else class="no-photo">无</span>
          </td>
          <td>
            <span v-if="record.repairStatus !== null && record.repairStatus !== undefined"
                  :class="['repair-tag', repairStatusClass(record.repairStatus)]">
              {{ repairStatusText(record.repairStatus) }}
            </span>
            <span v-else class="no-repair">未报修</span>
          </td>
          <td>{{ record.inspector || '-' }}</td>
          <td>
            <span v-if="record.reviewer">{{ record.reviewer }}</span>
            <span v-else-if="record.qualified === false" class="review-missing">未补复核人</span>
            <span v-else class="no-repair">-</span>
          </td>
          <td>
            <button class="btn-detail" @click="handleDetail(record)">详情</button>
            <template v-if="record.qualified === false && !record.repairOrderId">
              <button v-if="!record.reviewer" class="btn-review"
                      @click="handleFillReviewer(record)">补复核人</button>
              <button v-else class="btn-repair" @click="handleCreateRepair(record)">去补报修</button>
            </template>
          </td>
        </tr>
        <tr v-if="!loadError && spotCheckList.length === 0">
          <td colspan="12" class="empty">{{ loading ? '加载中...' : '当前筛选条件下暂无抽检记录' }}</td>
        </tr>
      </tbody>
    </table>

    <SpotCheckForm :visible="formVisible" @close="formVisible = false" @success="handleFormSuccess" />
    <SpotCheckDetail :visible="detailVisible" :spot-check-id="detailId"
                     @close="detailVisible = false" @repair-created="loadSpotChecks"
                     @reviewed="loadSpotChecks" />
    <SpotCheckReview :visible="reviewVisible" :record="reviewTarget"
                     @close="reviewVisible = false" @reviewed="handleReviewed" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { spotCheckApi, areaApi } from '../api'
import SpotCheckForm from './SpotCheckForm.vue'
import SpotCheckDetail from './SpotCheckDetail.vue'
import SpotCheckReview from './SpotCheckReview.vue'

const FILTER_STORAGE_KEY = 'spotcheck.list.filters'

const spotCheckList = ref([])
const areaOptions = ref([])
const loading = ref(false)
const loadError = ref('')
const filterError = ref('')
const formVisible = ref(false)
const detailVisible = ref(false)
const detailId = ref(null)
const reviewVisible = ref(false)
const reviewTarget = ref(null)

const defaultFilters = () => ({
  startDate: '',
  endDate: '',
  areaId: '',
  qualified: '',
  repairStatus: '',
  reviewStatus: '',
  thermometerNo: ''
})

const loadSavedFilters = () => {
  try {
    const saved = localStorage.getItem(FILTER_STORAGE_KEY)
    if (saved) {
      return { ...defaultFilters(), ...JSON.parse(saved) }
    }
  } catch (error) {
    console.warn('读取本地抽检筛选条件失败:', error)
  }
  return defaultFilters()
}

const filters = ref(loadSavedFilters())

const persistFilters = () => {
  try {
    localStorage.setItem(FILTER_STORAGE_KEY, JSON.stringify(filters.value))
  } catch (error) {
    console.warn('保存抽检筛选条件失败:', error)
  }
}

const validateDateRange = () => {
  if (filters.value.startDate && filters.value.endDate
      && filters.value.startDate > filters.value.endDate) {
    filterError.value = '抽检日开始日期不能晚于结束日期'
    return false
  }
  filterError.value = ''
  return true
}

const loadSpotChecks = async () => {
  if (!validateDateRange()) {
    spotCheckList.value = []
    return
  }
  loading.value = true
  loadError.value = ''
  try {
    const params = {}
    if (filters.value.startDate) params.startDate = filters.value.startDate
    if (filters.value.endDate) params.endDate = filters.value.endDate
    if (filters.value.areaId !== '') params.areaId = filters.value.areaId
    if (filters.value.qualified !== '') params.qualified = filters.value.qualified
    if (filters.value.repairStatus !== '') params.repairStatus = filters.value.repairStatus
    if (filters.value.reviewStatus !== '') params.reviewStatus = filters.value.reviewStatus
    const thermometerNo = (filters.value.thermometerNo || '').trim()
    if (thermometerNo !== '') params.thermometerNo = thermometerNo

    const res = await spotCheckApi.getSpotChecks(params)
    if (res.data.code === 200) {
      spotCheckList.value = res.data.data
    } else {
      loadError.value = res.data.message || '加载抽检记录失败，请稍后重试'
    }
  } catch (error) {
    loadError.value = error.response?.data?.message || '接口请求失败，抽检记录加载失败，请稍后重试'
    console.error('加载抽检记录失败:', error)
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
  loadSpotChecks()
}

const handleReset = () => {
  filters.value = defaultFilters()
  persistFilters()
  loadSpotChecks()
}

const handleDetail = (record) => {
  detailId.value = record.id
  detailVisible.value = true
}

const handleFillReviewer = (record) => {
  reviewTarget.value = record
  reviewVisible.value = true
}

const handleReviewed = () => {
  loadSpotChecks()
}

const handleCreateRepair = async (record) => {
  if (!record.reviewer) {
    alert('该不合格抽检尚未补填当班复核人，请先点击「补复核人」补填后再转报修')
    return
  }
  if (!confirm(`确定为温奶器「${record.equipmentName}（${record.equipmentNo}）」补提报修单吗？维修期间设备不可调配。`)) return
  try {
    const res = await spotCheckApi.createRepair(record.id, { reporter: record.inspector })
    if (res.data.code === 200) {
      loadSpotChecks()
    } else {
      alert(res.data.message || '创建报修单失败，请稍后重试')
    }
  } catch (error) {
    alert(error.response?.data?.message || '接口请求失败，报修单未创建，请稍后重试')
    console.error('抽检补报修失败:', error)
  }
}

const handleFormSuccess = () => {
  persistFilters()
  loadSpotChecks()
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
  loadAreas()
  loadSpotChecks()
})
</script>

<style scoped>
.spotcheck-list {
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

.spotcheck-table {
  width: 100%;
  border-collapse: collapse;
}

.spotcheck-table th,
.spotcheck-table td {
  padding: 12px 8px;
  text-align: left;
  border-bottom: 1px solid #e0e0e0;
  font-size: 14px;
}

.spotcheck-table th {
  background: #f8f9fa;
  color: #666;
  font-weight: bold;
}

.spotcheck-table tbody tr.row-unqualified {
  background: #fffbfb;
}

.temp-bad {
  color: #f56c6c;
  font-weight: bold;
}

.result-tag,
.repair-tag {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.result-tag.normal,
.repair-tag.restored {
  background: #f0f9eb;
  color: #67c23a;
}

.result-tag.abnormal {
  background: #fef0f0;
  color: #f56c6c;
}

.repair-tag.pending {
  background: #fdf6ec;
  color: #e6a23c;
}

.repair-tag.repairing {
  background: #ecf5ff;
  color: #409eff;
}

.no-repair,
.no-photo {
  color: #909399;
  font-size: 12px;
}

.photo-link {
  color: #409eff;
  font-size: 12px;
  text-decoration: underline;
}

.spotcheck-table td button {
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

.btn-repair {
  background: #e6a23c;
  color: #fff;
}

.btn-review {
  background: #f56c6c;
  color: #fff;
}

.review-missing {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  background: #fef0f0;
  color: #f56c6c;
}

.empty {
  text-align: center;
  color: #999;
  padding: 40px;
}
</style>
