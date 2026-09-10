
<template>
  <div class="transfer-list">
    <div class="list-header">
      <h3>调配台账</h3>
      <div class="filter-section">
        <div class="date-filter">
          <input type="date" v-model="startDate" placeholder="开始日期" />
          <span class="date-separator">至</span>
          <input type="date" v-model="endDate" placeholder="结束日期" />
          <button @click="handleFilter">筛选</button>
          <button @click="handleReset">重置</button>
        </div>
      </div>
    </div>
    
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
import { ref, onMounted } from 'vue'
import { transferApi } from '../api'
import TransferPrint from './TransferPrint.vue'

const transferList = ref([])
const summary = ref(null)
const startDate = ref('')
const endDate = ref('')
const printRecord = ref(null)
const printVisible = ref(false)

const loadTransfers = async () => {
  try {
    const res = await transferApi.getAllTransfers()
    if (res.data.code === 200) {
      transferList.value = res.data.data
    }
  } catch (error) {
    console.error('加载调配记录失败:', error)
  }
}

const handleFilter = async () => {
  if (!startDate.value || !endDate.value) {
    alert('请选择开始日期和结束日期')
    return
  }
  
  try {
    const res = await transferApi.getTransfersByDateRange(startDate.value, endDate.value)
    if (res.data.code === 200) {
      transferList.value = res.data.data
    }
    
    const summaryRes = await transferApi.getTransferSummary(startDate.value, endDate.value)
    if (summaryRes.data.code === 200) {
      summary.value = summaryRes.data.data
    }
  } catch (error) {
    console.error('筛选调配记录失败:', error)
  }
}

const handleReset = () => {
  startDate.value = ''
  endDate.value = ''
  summary.value = null
  loadTransfers()
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

onMounted(() => {
  loadTransfers()
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
