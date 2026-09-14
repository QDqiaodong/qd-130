
<template>
  <div class="modal-overlay" v-if="visible" @click="handleClose">
    <div class="modal-content" @click.stop>
      <div class="modal-header">
        <h3>温奶器抽检详情</h3>
        <button class="close-btn" @click="handleClose">×</button>
      </div>

      <div v-if="loadError" class="modal-body load-error">
        <p>{{ loadError }}</p>
        <button class="btn-retry" @click="loadDetail">重试</button>
      </div>

      <div class="modal-body" v-else-if="detail">
        <div class="detail-section">
          <h4>抽检信息</h4>
          <div class="info-grid">
            <div class="info-item"><span class="label">抽检单号:</span><span>{{ detail.record.spotCheckNo }}</span></div>
            <div class="info-item">
              <span class="label">抽检结论:</span>
              <span :class="['result-tag', detail.record.qualified ? 'normal' : 'abnormal']">
                {{ detail.record.qualified ? '合格' : '不合格' }}
              </span>
            </div>
            <div class="info-item">
              <span class="label">实测水温:</span>
              <span :class="{ 'temp-bad': !detail.record.qualified }">
                {{ detail.record.temperature }}℃
              </span>
            </div>
            <div class="info-item"><span class="label">体温枪编号:</span><span>{{ detail.record.thermometerNo || '-' }}</span></div>
            <div class="info-item"><span class="label">抽检人:</span><span>{{ detail.record.inspector || '-' }}</span></div>
            <div class="info-item">
              <span class="label">当班复核人:</span>
              <span v-if="detail.record.reviewer">{{ detail.record.reviewer }}</span>
              <span v-else-if="!detail.record.qualified" class="review-missing">未补复核人</span>
              <span v-else>-</span>
            </div>
            <div class="info-item">
              <span class="label">温奶器:</span>
              <span>{{ detail.record.equipmentName }}（{{ detail.record.equipmentNo }}）</span>
            </div>
            <div class="info-item"><span class="label">母婴室:</span><span>{{ detail.record.areaName || '-' }}</span></div>
            <div class="info-item"><span class="label">抽检日期:</span><span>{{ detail.record.checkDate }}</span></div>
            <div class="info-item"><span class="label">登记时间:</span><span>{{ formatTime(detail.record.createdAt) }}</span></div>
            <div class="info-item full" v-if="detail.record.abnormalDesc">
              <span class="label">不合格说明:</span><span>{{ detail.record.abnormalDesc }}</span>
            </div>
            <div class="info-item full" v-if="detail.record.remark">
              <span class="label">备注:</span><span>{{ detail.record.remark }}</span>
            </div>
          </div>
          <div class="photo-preview" v-if="detail.record.photoUrl">
            <span class="label">抽检照片:</span>
            <img :src="detail.record.photoUrl" alt="抽检照片" @error="photoError = true" v-show="!photoError" />
            <a v-if="photoError" :href="detail.record.photoUrl" target="_blank">{{ detail.record.photoUrl }}</a>
          </div>
        </div>

        <div class="detail-section">
          <h4>报修情况</h4>
          <div v-if="detail.repairOrder" class="info-grid">
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
          <p v-else-if="detail.record.qualified" class="empty-line">合格抽检无需报修</p>
          <div v-else class="no-repair-box">
            <template v-if="!detail.record.reviewer">
              <span>该抽检不合格且尚未报修，需先补填当班复核人才能转报修</span>
              <button class="btn-review-inline" @click="reviewVisible = true">补复核人</button>
            </template>
            <template v-else>
              <span>该抽检不合格且尚未报修</span>
              <button class="btn-repair-inline" :disabled="repairing" @click="handleCreateRepair">
                {{ repairing ? '提交中...' : '去补报修' }}
              </button>
            </template>
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
            <p v-if="!detail.timeline || detail.timeline.length === 0" class="empty-line">暂无时间线记录</p>
          </div>
        </div>
      </div>
    </div>
    <SpotCheckReview :visible="reviewVisible" :record="detail && detail.record"
                     @close="reviewVisible = false" @reviewed="handleReviewed" />
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { spotCheckApi } from '../api'
import SpotCheckReview from './SpotCheckReview.vue'

const props = defineProps({
  visible: Boolean,
  spotCheckId: Number
})

const emit = defineEmits(['close', 'repair-created', 'reviewed'])

const detail = ref(null)
const photoError = ref(false)
const loadError = ref('')
const repairing = ref(false)
const reviewVisible = ref(false)

const loadDetail = async () => {
  if (!props.spotCheckId) return
  loadError.value = ''
  try {
    const res = await spotCheckApi.getSpotCheckDetail(props.spotCheckId)
    if (res.data.code === 200) {
      detail.value = res.data.data
    } else {
      loadError.value = res.data.message || '加载抽检详情失败'
    }
  } catch (error) {
    loadError.value = error.response?.data?.message || '接口请求失败，抽检详情加载失败，请稍后重试'
    console.error('加载抽检详情失败:', error)
  }
}

const handleCreateRepair = async () => {
  if (repairing.value || !detail.value) return
  if (!detail.value.record.reviewer) {
    alert('该不合格抽检尚未补填当班复核人，请先补填复核人后再转报修')
    return
  }
  repairing.value = true
  try {
    const res = await spotCheckApi.createRepair(detail.value.record.id, {
      reporter: detail.value.record.inspector
    })
    if (res.data.code === 200) {
      emit('repair-created')
      await loadDetail()
    } else {
      alert(res.data.message || '创建报修单失败')
    }
  } catch (error) {
    alert(error.response?.data?.message || '接口请求失败，报修单未创建，请稍后重试')
    console.error('抽检补报修失败:', error)
  } finally {
    repairing.value = false
  }
}

const handleReviewed = async () => {
  emit('reviewed')
  await loadDetail()
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
  return String(time).replace('T', ' ').substring(0, 19)
}

const handleClose = () => {
  detail.value = null
  photoError.value = false
  loadError.value = ''
  reviewVisible.value = false
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

.load-error {
  color: #f56c6c;
  font-size: 14px;
}

.btn-retry {
  margin-top: 10px;
  padding: 6px 16px;
  border: none;
  border-radius: 4px;
  background: #409eff;
  color: #fff;
  cursor: pointer;
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
  min-width: 76px;
  flex-shrink: 0;
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

.empty-line {
  color: #909399;
  font-size: 13px;
}

.no-repair-box {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  background: #fef0f0;
  border: 1px solid #fbc4c4;
  border-radius: 4px;
  color: #f56c6c;
  font-size: 13px;
}

.btn-repair-inline {
  padding: 5px 14px;
  border: none;
  border-radius: 4px;
  background: #e6a23c;
  color: #fff;
  cursor: pointer;
  font-size: 13px;
}

.btn-repair-inline:disabled {
  background: #f3d19e;
  cursor: not-allowed;
}

.btn-review-inline {
  padding: 5px 14px;
  border: none;
  border-radius: 4px;
  background: #f56c6c;
  color: #fff;
  cursor: pointer;
  font-size: 13px;
}

.review-missing {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  background: #fef0f0;
  color: #f56c6c;
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

.timeline-dot.spotcheck {
  background: #f56c6c;
}

.timeline-dot.review {
  background: #67c23a;
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
