
<template>
  <div class="modal-overlay" v-if="visible" @click="handleClose">
    <div class="modal-content" @click.stop>
      <div class="modal-header">
        <h3>调配签收详情</h3>
        <button class="close-btn" @click="handleClose">×</button>
      </div>

      <div class="modal-body">
        <div v-if="loading" class="state loading">详情加载中，请稍候...</div>

        <div v-else-if="errorMessage" class="state error">
          <span>{{ errorMessage }}</span>
          <button class="btn-retry" :disabled="loading" @click="loadDetail">重试</button>
        </div>

        <template v-else-if="detail">
          <div class="info-grid">
            <div class="info-item"><span class="label">调配单号:</span><span>{{ detail.transferNo }}</span></div>
            <div class="info-item"><span class="label">调配状态:</span>
              <span :class="detail.status === 1 ? (detail.signed ? 'ok' : 'muted') : 'muted'">{{ statusText(detail) }}</span>
            </div>
            <div class="info-item"><span class="label">设备编号:</span><span>{{ detail.equipmentNo }}</span></div>
            <div class="info-item"><span class="label">设备名称:</span><span>{{ detail.equipmentName }}</span></div>
            <div class="info-item"><span class="label">原区域:</span><span>{{ detail.fromAreaName || '-' }}</span></div>
            <div class="info-item"><span class="label">目标区域:</span><span>{{ detail.toAreaName || '-' }}</span></div>
            <div class="info-item"><span class="label">调配日期:</span><span>{{ detail.transferDate }}</span></div>
            <div class="info-item"><span class="label">调出人:</span><span>{{ detail.operator || '-' }}</span></div>
            <div class="info-item full" v-if="detail.reason"><span class="label">调配原因:</span><span>{{ detail.reason }}</span></div>
          </div>

          <div class="receipt-section">
            <h4>到货签收</h4>
            <template v-if="detail.signed">
              <div class="info-grid">
                <div class="info-item">
                  <span class="label">签收人:</span><span class="strong">{{ detail.receiver || '-' }}</span>
                </div>
                <div class="info-item">
                  <span class="label">到货时间:</span><span class="strong">{{ formatDateTime(detail.arrivalTime) }}</span>
                </div>
                <div class="info-item full">
                  <span class="label">外观结论:</span>
                  <span :class="['conclusion', detail.appearanceIntact ? 'intact' : 'damaged']">
                    {{ detail.appearanceIntact ? '外观完好' : '外观有破损' }}
                  </span>
                </div>
              </div>
            </template>
            <div v-else class="unsigned-tip">该调配单尚未到货签收，签收前设备不可再次调出</div>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { transferApi } from '../api'

const props = defineProps({
  visible: Boolean,
  transferId: [Number, String]
})

const emit = defineEmits(['close'])

const detail = ref(null)
const loading = ref(false)
const errorMessage = ref('')

const loadDetail = async () => {
  if (!props.transferId) return
  loading.value = true
  errorMessage.value = ''
  try {
    const res = await transferApi.getTransferById(props.transferId)
    if (res.data.code === 200) {
      detail.value = res.data.data
    } else {
      errorMessage.value = res.data.message || '加载签收详情失败，请稍后重试'
    }
  } catch (error) {
    errorMessage.value = error.response?.data?.message
      || (error.code === 'ECONNABORTED' ? '接口请求超时，请稍后重试' : '加载签收详情失败，请检查网络或稍后重试')
    console.error('加载签收详情失败:', error)
  } finally {
    loading.value = false
  }
}

const formatDateTime = (value) => {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 16)
}

const statusText = (detail) => {
  if (detail.status !== 1) return '已取消'
  return detail.signed ? '已签收' : '待签收（设备仍在调出地）'
}

const handleClose = () => {
  detail.value = null
  errorMessage.value = ''
  emit('close')
}

watch(() => props.visible, (val) => {
  if (val) loadDetail()
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
  max-width: 600px;
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

.state {
  padding: 12px 16px;
  border-radius: 4px;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.state.loading {
  background: #ecf5ff;
  color: #409eff;
  border: 1px solid #b3d8ff;
}

.state.error {
  background: #fef0f0;
  color: #f56c6c;
  border: 1px solid #fbc4c4;
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

.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px 16px;
  margin-bottom: 8px;
}

.info-item {
  display: flex;
  font-size: 14px;
  color: #333;
}

.info-item.full {
  grid-column: 1 / -1;
}

.info-item .label {
  color: #999;
  min-width: 76px;
  flex-shrink: 0;
}

.info-item .strong {
  font-weight: bold;
}

.ok {
  color: #67c23a;
}

.muted {
  color: #909399;
}

.receipt-section {
  margin-top: 12px;
  border-top: 1px dashed #e0e0e0;
  padding-top: 12px;
}

.receipt-section h4 {
  margin: 0 0 10px;
  font-size: 15px;
  color: #333;
}

.conclusion {
  font-weight: bold;
  padding: 2px 10px;
  border-radius: 4px;
  font-size: 13px;
}

.conclusion.intact {
  color: #67c23a;
  background: #f0f9eb;
}

.conclusion.damaged {
  color: #f56c6c;
  background: #fef0f0;
}

.unsigned-tip {
  padding: 10px 12px;
  background: #fdf6ec;
  border: 1px solid #f5dab1;
  border-radius: 4px;
  color: #e6a23c;
  font-size: 13px;
}
</style>
