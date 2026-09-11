
<template>
  <div class="repair-list">
    <div class="list-header">
      <h3>故障报修</h3>
    </div>

    <div class="filter-section">
      <div v-if="drillTip" class="drill-banner">
        <span>{{ drillTip }}</span>
        <button class="btn-back" @click="backToDashboard">返回看板</button>
        <button class="btn-clear-drill" @click="clearDrillFilters">清除看板筛选</button>
      </div>
      <div class="filter-row">
        <input type="date" v-model="filters.startDate" placeholder="开始日期" />
        <span class="separator">至</span>
        <input type="date" v-model="filters.endDate" placeholder="结束日期" />
        <select v-model="filters.areaId" @change="handleAreaSelectChange">
          <option value="">全部区域</option>
          <option v-for="area in areaOptions" :key="area.id" :value="area.id">{{ area.name }}</option>
        </select>
        <select v-model="filters.equipmentType">
          <option value="">全部设备类型</option>
          <option v-for="type in typeOptions" :key="type" :value="type">{{ type }}</option>
        </select>
        <select v-model="filters.status">
          <option value="">全部状态</option>
          <option :value="0">待处理</option>
          <option :value="1">维修中</option>
          <option :value="2">已恢复</option>
        </select>
        <button class="btn-filter" @click="loadRepairs">筛选</button>
        <button class="btn-reset" @click="handleReset">重置</button>
      </div>
    </div>

    <table class="repair-table">
      <thead>
        <tr>
          <th>报修单号</th>
          <th>巡检单号</th>
          <th>设备编号</th>
          <th>设备名称</th>
          <th>区域</th>
          <th>故障描述</th>
          <th>报修时间</th>
          <th>状态</th>
          <th>维修人</th>
          <th>恢复时间</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="order in repairList" :key="order.id">
          <td>{{ order.repairNo }}</td>
          <td>{{ order.inspectionNo || '-' }}</td>
          <td>{{ order.equipmentNo }}</td>
          <td>{{ order.equipmentName }}</td>
          <td>{{ order.areaName || '-' }}</td>
          <td class="desc-cell" :title="order.faultDesc">{{ order.faultDesc || '-' }}</td>
          <td>{{ formatTime(order.createdAt) }}</td>
          <td>
            <span :class="['status-tag', statusClass(order.status)]">{{ statusText(order.status) }}</span>
          </td>
          <td>{{ order.repairman || '-' }}</td>
          <td>{{ order.finishTime ? formatTime(order.finishTime) : '-' }}</td>
          <td>
            <button class="btn-detail" @click="handleDetail(order)">详情</button>
            <button v-if="order.status === 0" class="btn-start" @click="openTransition(order, 1)">开始维修</button>
            <button v-if="order.status === 1" class="btn-finish" @click="openTransition(order, 2)">确认恢复</button>
          </td>
        </tr>
        <tr v-if="repairList.length === 0">
          <td colspan="11" class="empty">暂无报修单</td>
        </tr>
      </tbody>
    </table>

    <div class="modal-overlay" v-if="transitionVisible" @click="transitionVisible = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>{{ transitionTarget === 1 ? '开始维修' : '确认恢复' }}</h3>
          <button class="close-btn" @click="transitionVisible = false">×</button>
        </div>
        <div class="modal-body" v-if="currentOrder">
          <div class="order-info">
            <span class="label">报修单号:</span><span>{{ currentOrder.repairNo }}</span>
          </div>
          <div class="order-info">
            <span class="label">设备:</span><span>{{ currentOrder.equipmentName }}（{{ currentOrder.equipmentNo }}）</span>
          </div>
          <div class="order-info">
            <span class="label">故障描述:</span><span>{{ currentOrder.faultDesc || '-' }}</span>
          </div>
          <p class="transition-tip" v-if="transitionTarget === 1">开始维修后设备进入维修中状态，期间不可调配。</p>
          <p class="transition-tip" v-else>确认恢复后设备维修完成，可重新调配。</p>

          <div class="form-group">
            <label>维修人:</label>
            <input type="text" v-model="transitionForm.repairman" placeholder="请输入维修人姓名" />
          </div>
          <div class="form-group" v-if="transitionTarget === 2">
            <label>维修说明:</label>
            <textarea v-model="transitionForm.repairNote" rows="3" placeholder="请输入维修处理说明"></textarea>
          </div>

          <div class="form-buttons">
            <button type="button" class="btn-cancel" @click="transitionVisible = false">取消</button>
            <button type="button" class="btn-submit" :disabled="submitting" @click="handleTransition">
              {{ submitting ? '提交中...' : '确认' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <div class="modal-overlay" v-if="detailVisible" @click="detailVisible = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>报修单详情</h3>
          <button class="close-btn" @click="detailVisible = false">×</button>
        </div>
        <div class="modal-body" v-if="detail">
          <div class="detail-section">
            <h4>报修信息</h4>
            <div class="info-grid">
              <div class="info-item"><span class="label">报修单号:</span><span>{{ detail.repairNo }}</span></div>
              <div class="info-item"><span class="label">巡检单号:</span><span>{{ detail.inspectionNo || '-' }}</span></div>
              <div class="info-item"><span class="label">设备:</span><span>{{ detail.equipmentName }}（{{ detail.equipmentNo }}）</span></div>
              <div class="info-item"><span class="label">区域:</span><span>{{ detail.areaName || '-' }}</span></div>
              <div class="info-item">
                <span class="label">状态:</span>
                <span :class="['status-tag', statusClass(detail.status)]">{{ statusText(detail.status) }}</span>
              </div>
              <div class="info-item"><span class="label">报修人:</span><span>{{ detail.reporter || '-' }}</span></div>
              <div class="info-item full"><span class="label">故障描述:</span><span>{{ detail.faultDesc || '-' }}</span></div>
              <div class="info-item full" v-if="detail.repairNote"><span class="label">维修说明:</span><span>{{ detail.repairNote }}</span></div>
            </div>
            <div class="photo-preview" v-if="detail.photoUrl">
              <span class="label">故障照片:</span>
              <img :src="detail.photoUrl" alt="故障照片" @error="photoError = true" v-show="!photoError" />
              <a v-if="photoError" :href="detail.photoUrl" target="_blank">{{ detail.photoUrl }}</a>
            </div>
          </div>

          <div class="detail-section">
            <h4>处理时间线</h4>
            <div class="timeline">
              <div v-for="(item, index) in detail.timeline" :key="index" class="timeline-item">
                <div :class="['timeline-dot', item.type]"></div>
                <div class="timeline-content">
                  <div class="timeline-title">{{ item.title }}</div>
                  <div class="timeline-desc">{{ item.description }}</div>
                  <div class="timeline-time">{{ formatTime(item.time) }}</div>
                </div>
              </div>
              <p v-if="!detail.timeline || detail.timeline.length === 0" class="empty">暂无时间线记录</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { repairApi, areaApi, equipmentApi } from '../api'

const props = defineProps({
  initialFilters: {
    type: Object,
    default: () => ({})
  }
})
const emit = defineEmits(['back-dashboard'])

const repairList = ref([])
const areaOptions = ref([])
const typeOptions = ref([])
const transitionVisible = ref(false)
const transitionTarget = ref(1)
const currentOrder = ref(null)
const transitionForm = ref({ repairman: '', repairNote: '' })
const submitting = ref(false)
const detailVisible = ref(false)
const detail = ref(null)
const photoError = ref(false)

const defaultFilters = () => ({
  startDate: '',
  endDate: '',
  areaId: '',
  equipmentType: '',
  status: '',
  equipmentCurrentAreaId: null
})

const filters = ref(defaultFilters())

const drillTip = computed(() => {
  if (!props.initialFilters || Object.keys(props.initialFilters).length === 0) return ''
  const parts = []
  if (filters.value.equipmentCurrentAreaId) {
    const area = areaOptions.value.find(a => a.id === filters.value.equipmentCurrentAreaId)
    if (area) parts.push(`设备当前所在区域「${area.name}」（含下级区域）`)
  } else if (filters.value.areaId !== '') {
    const area = areaOptions.value.find(a => a.id === filters.value.areaId)
    if (area) parts.push(`区域「${area.name}」（按报修单记录区域）`)
  }
  if (filters.value.equipmentType) parts.push(`类型「${filters.value.equipmentType}」`)
  if (filters.value.status === 0) parts.push('状态：待处理（当前状态，不含周期条件）')
  if (filters.value.status === 1) parts.push('状态：维修中（当前状态，不含周期条件）')
  if (filters.value.startDate && filters.value.endDate) {
    parts.push(`报修时间 ${filters.value.startDate} 至 ${filters.value.endDate}`)
  }
  return parts.length > 0 ? `来自健康看板的筛选：${parts.join('，')}` : ''
})

const loadRepairs = async () => {
  try {
    const params = {}
    if (filters.value.startDate) params.startDate = filters.value.startDate
    if (filters.value.endDate) params.endDate = filters.value.endDate
    if (filters.value.equipmentCurrentAreaId) {
      params.equipmentCurrentAreaId = filters.value.equipmentCurrentAreaId
    } else if (filters.value.areaId !== '') {
      params.areaId = filters.value.areaId
    }
    if (filters.value.equipmentType !== '') params.equipmentType = filters.value.equipmentType
    if (filters.value.status !== '') params.status = filters.value.status

    const res = await repairApi.getRepairs(params)
    if (res.data.code === 200) {
      repairList.value = res.data.data
    } else {
      alert(res.data.message || '加载报修单失败')
    }
  } catch (error) {
    alert(error.response?.data?.message || '加载报修单失败，请稍后重试')
    console.error('加载报修单失败:', error)
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
  filters.value = {
    ...defaultFilters(),
    ...init,
    // 看板当前状态下钻锁定设备当前区域口径，下拉区域不再生效，避免明细数与卡片不一致
    areaId: init.equipmentCurrentAreaId ? '' : (init.areaId ?? '')
  }
}

const handleReset = () => {
  filters.value = defaultFilters()
  loadRepairs()
}

const clearDrillFilters = () => {
  filters.value = defaultFilters()
  loadRepairs()
}

const handleAreaSelectChange = () => {
  // 用户手动改区域下拉后，解除看板锁定的「设备当前区域」口径
  filters.value.equipmentCurrentAreaId = null
}

const backToDashboard = () => {
  emit('back-dashboard')
}

const openTransition = (order, target) => {
  currentOrder.value = order
  transitionTarget.value = target
  transitionForm.value = { repairman: order.repairman || '', repairNote: '' }
  transitionVisible.value = true
}

const handleTransition = async () => {
  if (submitting.value || !currentOrder.value) return
  submitting.value = true

  try {
    const res = await repairApi.updateRepairStatus(currentOrder.value.id, {
      status: transitionTarget.value,
      repairman: transitionForm.value.repairman,
      repairNote: transitionForm.value.repairNote
    })
    if (res.data.code === 200) {
      transitionVisible.value = false
      loadRepairs()
    } else {
      alert(res.data.message || '状态更新失败')
    }
  } catch (error) {
    alert(error.response?.data?.message || '状态更新失败')
  } finally {
    submitting.value = false
  }
}

const handleDetail = async (order) => {
  photoError.value = false
  try {
    const res = await repairApi.getRepairById(order.id)
    if (res.data.code === 200) {
      detail.value = res.data.data
      detailVisible.value = true
    }
  } catch (error) {
    console.error('加载报修单详情失败:', error)
  }
}

const statusText = (status) => {
  if (status === 0) return '待处理'
  if (status === 1) return '维修中'
  if (status === 2) return '已恢复'
  return '-'
}

const statusClass = (status) => {
  if (status === 0) return 'pending'
  if (status === 1) return 'repairing'
  if (status === 2) return 'restored'
  return ''
}

const formatTime = (time) => {
  if (!time) return '-'
  return time.replace('T', ' ').substring(0, 19)
}

onMounted(() => {
  applyInitialFilters()
  loadAreas()
  loadTypes()
  loadRepairs()
})
</script>

<style scoped>
.repair-list {
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

.repair-table {
  width: 100%;
  border-collapse: collapse;
}

.repair-table th,
.repair-table td {
  padding: 12px 8px;
  text-align: left;
  border-bottom: 1px solid #e0e0e0;
  font-size: 14px;
}

.repair-table th {
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

.status-tag {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.status-tag.pending {
  background: #fdf6ec;
  color: #e6a23c;
}

.status-tag.repairing {
  background: #ecf5ff;
  color: #409eff;
}

.status-tag.restored {
  background: #f0f9eb;
  color: #67c23a;
}

.repair-table td button {
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

.btn-start {
  background: #e6a23c;
  color: #fff;
}

.btn-finish {
  background: #67c23a;
  color: #fff;
}

.empty {
  text-align: center;
  color: #999;
  padding: 40px;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: #fff;
  border-radius: 8px;
  width: 90%;
  max-width: 560px;
  max-height: 90vh;
  overflow-y: auto;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #e0e0e0;
}

.modal-header h3 {
  margin: 0;
  font-size: 18px;
  color: #333;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  color: #999;
  cursor: pointer;
}

.close-btn:hover {
  color: #666;
}

.modal-body {
  padding: 16px;
}

.order-info {
  display: flex;
  margin-bottom: 8px;
  font-size: 14px;
}

.order-info .label {
  color: #999;
  width: 80px;
  flex-shrink: 0;
}

.transition-tip {
  padding: 8px 12px;
  background: #fdf6ec;
  border-radius: 4px;
  color: #e6a23c;
  font-size: 13px;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 6px;
  color: #666;
  font-size: 14px;
}

.form-group input,
.form-group textarea {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  font-size: 14px;
  box-sizing: border-box;
}

.form-group textarea {
  resize: vertical;
}

.form-buttons {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 24px;
}

.btn-cancel {
  padding: 8px 24px;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  background: #fff;
  color: #666;
  cursor: pointer;
}

.btn-cancel:hover {
  background: #f5f5f5;
}

.btn-submit {
  padding: 8px 24px;
  border: none;
  border-radius: 4px;
  background: #409eff;
  color: #fff;
  cursor: pointer;
}

.btn-submit:hover:not(:disabled) {
  background: #66b1ff;
}

.btn-submit:disabled {
  background: #a0cfff;
  cursor: not-allowed;
}

.detail-section {
  margin-bottom: 20px;
}

.detail-section h4 {
  margin: 0 0 12px 0;
  font-size: 15px;
  color: #333;
  border-left: 3px solid #409eff;
  padding-left: 8px;
}

.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px 16px;
}

.info-item {
  display: flex;
  font-size: 14px;
}

.info-item.full {
  grid-column: 1 / -1;
}

.info-item .label {
  color: #999;
  min-width: 70px;
  flex-shrink: 0;
}

.photo-preview {
  margin-top: 12px;
}

.photo-preview .label {
  display: block;
  color: #999;
  font-size: 14px;
  margin-bottom: 6px;
}

.photo-preview img {
  max-width: 100%;
  max-height: 200px;
  border-radius: 4px;
  border: 1px solid #e0e0e0;
}

.timeline {
  position: relative;
  padding-left: 20px;
}

.timeline::before {
  content: '';
  position: absolute;
  left: 5px;
  top: 8px;
  bottom: 8px;
  width: 2px;
  background: #e0e0e0;
}

.timeline-item {
  position: relative;
  padding-bottom: 16px;
}

.timeline-dot {
  position: absolute;
  left: -19px;
  top: 4px;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #409eff;
  border: 2px solid #fff;
}

.timeline-dot.inspection {
  background: #e6a23c;
}

.timeline-dot.repair {
  background: #409eff;
}

.timeline-title {
  font-size: 14px;
  font-weight: bold;
  color: #333;
}

.timeline-desc {
  font-size: 13px;
  color: #666;
  margin-top: 2px;
}

.timeline-time {
  font-size: 12px;
  color: #999;
  margin-top: 2px;
}
</style>
