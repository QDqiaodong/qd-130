
<template>
  <div class="modal-overlay" v-if="visible" @click="handleClose">
    <div class="modal-content" @click.stop>
      <div class="modal-header">
        <h3>巡检详情</h3>
        <button class="close-btn" @click="handleClose">×</button>
      </div>
      <div class="modal-body" v-if="detail">
        <div class="detail-section">
          <h4>巡检信息</h4>
          <div class="info-grid">
            <div class="info-item"><span class="label">巡检单号:</span><span>{{ detail.record.inspectionNo }}</span></div>
            <div class="info-item"><span class="label">设备:</span><span>{{ detail.record.equipmentName }}（{{ detail.record.equipmentNo }}）</span></div>
            <div class="info-item"><span class="label">所在区域:</span><span>{{ detail.record.areaName || '-' }}</span></div>
            <div class="info-item"><span class="label">巡检日期:</span><span>{{ detail.record.inspectionDate }}</span></div>
            <div class="info-item">
              <span class="label">巡检结果:</span>
              <span :class="['result-tag', detail.record.result === 1 ? 'normal' : 'abnormal']">
                {{ detail.record.result === 1 ? '正常' : '异常' }}
              </span>
            </div>
            <div class="info-item"><span class="label">巡检员:</span><span>{{ detail.record.inspector || '-' }}</span></div>
            <div class="info-item" v-if="detail.record.planName"><span class="label">关联计划:</span><span>{{ detail.record.planName }}</span></div>
            <div class="info-item full" v-if="detail.record.abnormalDesc">
              <span class="label">异常描述:</span><span>{{ detail.record.abnormalDesc }}</span>
            </div>
            <div class="info-item full" v-if="detail.record.remark">
              <span class="label">备注:</span><span>{{ detail.record.remark }}</span>
            </div>
          </div>
          <div class="photo-preview" v-if="detail.record.photoUrl">
            <span class="label">异常照片:</span>
            <img :src="detail.record.photoUrl" alt="异常照片" @error="photoError = true" v-show="!photoError" />
            <a v-if="photoError" :href="detail.record.photoUrl" target="_blank">{{ detail.record.photoUrl }}</a>
          </div>
        </div>

        <div class="detail-section" v-if="detail.repairOrder">
          <h4>报修单信息</h4>
          <div class="info-grid">
            <div class="info-item"><span class="label">报修单号:</span><span>{{ detail.repairOrder.repairNo }}</span></div>
            <div class="info-item">
              <span class="label">维修状态:</span>
              <span :class="['repair-tag', repairStatusClass(detail.repairOrder.status)]">
                {{ repairStatusText(detail.repairOrder.status) }}
              </span>
            </div>
            <div class="info-item"><span class="label">报修人:</span><span>{{ detail.repairOrder.reporter || '-' }}</span></div>
            <div class="info-item"><span class="label">维修人:</span><span>{{ detail.repairOrder.repairman || '-' }}</span></div>
            <div class="info-item full" v-if="detail.repairOrder.repairNote">
              <span class="label">维修说明:</span><span>{{ detail.repairOrder.repairNote }}</span>
            </div>
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
</template>

<script setup>
import { ref, watch } from 'vue'
import { inspectionApi } from '../api'

const props = defineProps({
  visible: Boolean,
  inspectionId: Number
})

const emit = defineEmits(['close'])

const detail = ref(null)
const photoError = ref(false)

const loadDetail = async () => {
  if (!props.inspectionId) return
  try {
    const res = await inspectionApi.getInspectionDetail(props.inspectionId)
    if (res.data.code === 200) {
      detail.value = res.data.data
    }
  } catch (error) {
    console.error('加载巡检详情失败:', error)
  }
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

const formatTime = (time) => {
  if (!time) return '-'
  return time.replace('T', ' ').substring(0, 19)
}

const handleClose = () => {
  detail.value = null
  photoError.value = false
  emit('close')
}

watch(() => props.visible, (val) => {
  if (val) {
    photoError.value = false
    loadDetail()
  }
})
</script>

<style scoped>
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
  max-width: 640px;
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

.empty {
  color: #999;
  font-size: 14px;
}
</style>
