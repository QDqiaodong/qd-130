
<template>
  <div class="modal-overlay" v-if="visible" @click="handleClose">
    <div class="modal-content" @click.stop>
      <div class="modal-header">
        <h3>到货签收</h3>
        <button class="close-btn" @click="handleClose">×</button>
      </div>
      <div class="modal-body" v-if="record">
        <div class="transfer-summary">
          <div class="summary-item"><span class="label">调配单号:</span><span>{{ record.transferNo }}</span></div>
          <div class="summary-item"><span class="label">设备:</span><span>{{ record.equipmentName }}（{{ record.equipmentNo }}）</span></div>
          <div class="summary-item"><span class="label">目标区域:</span><span>{{ record.toAreaName || '-' }}</span></div>
          <div class="summary-item"><span class="label">调配日期:</span><span>{{ record.transferDate }}</span></div>
        </div>

        <p class="receipt-tip">签收完成前，设备档案仍保留在调出地且不能再次调配；请核对到货实物后再签收，签收后位置才变更为目标区域。</p>

        <form @submit.prevent="handleSubmit">
          <div class="form-group">
            <label>签收人: <span class="required">*</span></label>
            <input type="text" v-model="form.receiver" placeholder="请输入值班签收人姓名" />
          </div>

          <div class="form-group">
            <label>到货时间: <span class="required">*</span></label>
            <input type="datetime-local" v-model="form.arrivalTime" />
          </div>

          <div class="form-group">
            <label>外观是否完好: <span class="required">*</span></label>
            <div class="result-radio">
              <label><input type="radio" :value="true" v-model="form.appearanceIntact" /> 外观完好</label>
              <label><input type="radio" :value="false" v-model="form.appearanceIntact" /> 外观有破损</label>
            </div>
          </div>

          <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>

          <div class="form-buttons">
            <button type="button" class="btn-cancel" @click="handleClose">取消</button>
            <button type="submit" class="btn-submit" :disabled="submitting">
              {{ submitting ? '提交中...' : '确认签收' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { transferApi } from '../api'

const props = defineProps({
  visible: Boolean,
  record: Object
})

const emit = defineEmits(['close', 'signed'])

const submitting = ref(false)
const errorMessage = ref('')

const nowLocal = () => {
  const now = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}T${pad(now.getHours())}:${pad(now.getMinutes())}`
}

const defaultForm = () => ({
  receiver: '',
  arrivalTime: nowLocal(),
  appearanceIntact: null
})

const form = ref(defaultForm())

const validate = () => {
  if (!form.value.receiver.trim()) {
    errorMessage.value = '请填写签收人'
    return false
  }
  if (!form.value.arrivalTime) {
    errorMessage.value = '请选择到货时间'
    return false
  }
  if (form.value.appearanceIntact === null) {
    errorMessage.value = '请选择外观是否完好'
    return false
  }
  if (props.record && props.record.transferDate
      && form.value.arrivalTime.slice(0, 10) < props.record.transferDate) {
    errorMessage.value = '到货时间不能早于调配日期'
    return false
  }
  errorMessage.value = ''
  return true
}

const handleSubmit = async () => {
  if (submitting.value) return
  if (!validate()) return

  submitting.value = true
  try {
    const res = await transferApi.signTransferReceipt(props.record.id, {
      receiver: form.value.receiver.trim(),
      arrivalTime: form.value.arrivalTime,
      appearanceIntact: form.value.appearanceIntact
    })
    if (res.data.code === 200) {
      emit('signed')
      handleClose()
    } else {
      handleServerError(res.data.message)
    }
  } catch (error) {
    handleServerError(error.response?.data?.message, error)
    console.error('提交到货签收失败:', error)
  } finally {
    submitting.value = false
  }
}

const handleServerError = (message, error) => {
  if (message) {
    errorMessage.value = message
  } else if (error && error.code === 'ECONNABORTED') {
    errorMessage.value = '接口请求超时，签收未提交，请稍后重试'
  } else {
    errorMessage.value = '接口请求失败，签收未提交，请稍后重试'
  }
  // 其他值班已签收过时，刷新列表让签收标记与能否调出保持最新
  if (message && message.includes('重复签收')) {
    emit('signed')
  }
}

const handleClose = () => {
  form.value = defaultForm()
  errorMessage.value = ''
  emit('close')
}

watch(() => props.visible, (val) => {
  if (val) {
    form.value = defaultForm()
    errorMessage.value = ''
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
  max-width: 520px;
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

.transfer-summary {
  background: #f8f9fa;
  border-radius: 4px;
  padding: 12px;
  margin-bottom: 12px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px 16px;
}

.summary-item {
  display: flex;
  font-size: 13px;
  color: #333;
}

.summary-item .label {
  color: #999;
  min-width: 70px;
  flex-shrink: 0;
}

.receipt-tip {
  font-size: 12px;
  color: #e6a23c;
  background: #fdf6ec;
  border: 1px solid #f5dab1;
  border-radius: 4px;
  padding: 8px 12px;
  margin: 0 0 16px;
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

.form-group input[type="text"],
.form-group input[type="datetime-local"] {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  font-size: 14px;
  box-sizing: border-box;
}

.required {
  color: #f56c6c;
}

.result-radio {
  display: flex;
  gap: 16px;
}

.result-radio label {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #333;
  cursor: pointer;
  margin-bottom: 0;
}

.form-error {
  margin: 0 0 12px;
  padding: 8px 12px;
  background: #fef0f0;
  border: 1px solid #fbc4c4;
  border-radius: 4px;
  color: #f56c6c;
  font-size: 13px;
}

.form-buttons {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 8px;
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
  background: #67c23a;
  color: #fff;
  cursor: pointer;
}

.btn-submit:hover:not(:disabled) {
  background: #85ce61;
}

.btn-submit:disabled {
  background: #b3e19d;
  cursor: not-allowed;
}
</style>
