
<template>
  <div class="inspection-list">
    <div class="list-header">
      <h3>巡检记录</h3>
      <button class="add-btn" @click="formVisible = true">登记巡检</button>
    </div>

    <div class="filter-section">
      <div class="filter-row">
        <input type="date" v-model="filters.startDate" placeholder="开始日期" />
        <span class="separator">至</span>
        <input type="date" v-model="filters.endDate" placeholder="结束日期" />
        <select v-model="filters.areaId">
          <option value="">全部区域</option>
          <option v-for="area in areaOptions" :key="area.id" :value="area.id">{{ area.name }}</option>
        </select>
        <select v-model="filters.result">
          <option value="">全部结果</option>
          <option :value="1">正常</option>
          <option :value="2">异常</option>
        </select>
        <select v-model="filters.repairStatus">
          <option value="">全部维修状态</option>
          <option :value="-1">未报修</option>
          <option :value="0">待处理</option>
          <option :value="1">维修中</option>
          <option :value="2">已恢复</option>
        </select>
        <button class="btn-filter" @click="loadInspections">筛选</button>
        <button class="btn-reset" @click="handleReset">重置</button>
      </div>
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
            <button v-if="record.result === 2 && !record.repairOrderId"
                    class="btn-repair" @click="handleCreateRepair(record)">报修</button>
          </td>
        </tr>
        <tr v-if="inspectionList.length === 0">
          <td colspan="10" class="empty">暂无巡检记录</td>
        </tr>
      </tbody>
    </table>

    <InspectionForm :visible="formVisible" @close="formVisible = false" @success="handleFormSuccess" />
    <InspectionDetail :visible="detailVisible" :inspection-id="detailId" @close="detailVisible = false" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { inspectionApi, repairApi, areaApi } from '../api'
import InspectionForm from './InspectionForm.vue'
import InspectionDetail from './InspectionDetail.vue'

const inspectionList = ref([])
const areaOptions = ref([])
const formVisible = ref(false)
const detailVisible = ref(false)
const detailId = ref(null)

const defaultFilters = () => ({
  startDate: '',
  endDate: '',
  areaId: '',
  result: '',
  repairStatus: ''
})

const filters = ref(defaultFilters())

const loadInspections = async () => {
  try {
    const params = {}
    if (filters.value.startDate) params.startDate = filters.value.startDate
    if (filters.value.endDate) params.endDate = filters.value.endDate
    if (filters.value.areaId !== '') params.areaId = filters.value.areaId
    if (filters.value.result !== '') params.result = filters.value.result
    if (filters.value.repairStatus !== '') params.repairStatus = filters.value.repairStatus

    const res = await inspectionApi.getInspections(params)
    if (res.data.code === 200) {
      inspectionList.value = res.data.data
    }
  } catch (error) {
    console.error('加载巡检记录失败:', error)
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

const handleReset = () => {
  filters.value = defaultFilters()
  loadInspections()
}

const handleDetail = (record) => {
  detailId.value = record.id
  detailVisible.value = true
}

const handleCreateRepair = async (record) => {
  if (!confirm(`确定为设备「${record.equipmentName}」创建报修单吗？维修期间设备不可调配。`)) return

  try {
    const res = await repairApi.createRepair({ inspectionId: record.id, reporter: record.inspector })
    if (res.data.code === 200) {
      loadInspections()
    } else {
      alert(res.data.message || '创建报修单失败')
    }
  } catch (error) {
    alert(error.response?.data?.message || '创建报修单失败')
  }
}

const handleFormSuccess = () => {
  loadInspections()
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
  loadInspections()
  loadAreas()
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

.filter-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
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
