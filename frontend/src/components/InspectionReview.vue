
<template>
  <div class="modal-overlay" v-if="visible" @click="handleClose">
    <div class="modal-content" @click.stop>
      <div class="modal-header">
        <h3>异常巡检复核</h3>
        <button class="close-btn" @click="handleClose">×</button>
      </div>
      <div class="modal-body" v-if="inspection">
        <div class="inspect-summary">
          <div class="summary-item"><span class="label">巡检单号:</span><span>{{ inspection.inspectionNo }}</span></div>
          <div class="summary-item"><span class="label">设备:</span><span>{{ inspection.equipmentName }}（{{ inspection.equipmentNo }}）</span></div>
          <div class="summary-item"><span class="label">母婴室:</span><span>{{ inspection.areaName || '-' }}</span></div>
          <div class="summary-item"><span class="label">巡检日期:</span><span>{{ inspection.inspectionDate }}</span></div>
          <div class="summary-item full" v-if="inspection.abnormalDesc">
            <span class="label">异常描述:</span><span>{{ inspection.abnormalDesc }}</span>
          </div>
        </div>

        <p class="review-tip">复核属实后才允许转报修；复核不属实必须填写说明，且该记录不能再报修。</p>

        <form @submit.prevent="handleSubmit">
          <div class="form-group">
            <label>复核人: <span class="required">*</span></label>
            <input type="text" v-model="form.reviewer" placeholder="请输入值班复核人姓名" />
          </div>

          <div class="form-group">
            <label>复核时间: <span class="required">*</span></label>
            <input type="datetime-local" v-model="form.reviewTime" />
          </div>

          <div class="form-group">
            <label>是否属实: <span class="required">*</span></label>
            <div class="result-radio">
              <label><input type="radio" :value="true" v-model="form.confirmed" /> 属实（可转报修）</label>
              <label><input type="radio" :value="false" v-model="form.confirmed" /> 不属实（不可报修）</label>
            </div>
          </div>

          <div class="form-group">
            <label>
              复核说明:
              <span v-if="form.confirmed === false" class="required">*（不属实必填）</span>
            </label>
            <textarea v-model="form.reviewNote" rows="3"
                      :placeholder="form.confirmed === false ? '请说明不属实原因，复核后该记录不能再报修' : '可填写复核情况说明（选填）'"></textarea>
          </div>

          <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>

          <div class="form-buttons">
            <button type="button" class="btn-cancel" @click="handleClose">取消</button>
            <button type="submit" class="btn-submit" :disabled="submitting">
              {{ submitting ? '提交中...' : '提交复核' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { inspectionApi } from '../api'

const props = defineProps({
  visible: Boolean,
  inspection: Object
})

const emit = defineEmits(['close', 'reviewed'])

const submitting = ref(false)
const errorMessage = ref('')

const nowLocal = () => {
  const now = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}T${pad(now.getHours())}:${pad(now.getMinutes())}`
}

const defaultForm = () => ({
  reviewer: '',
  reviewTime: nowLocal(),
  confirmed: null,
  reviewNote: ''
})

const form = ref(defaultForm())

const validate = () => {
  if (!form.value.reviewer.trim()) {
    errorMessage.value = '请填写复核人'
    return false
  }
  if (!form.value.reviewTime) {
    errorMessage.value = '请选择复核时间'
    return false
  }
  if (form.value.confirmed === null) {
    errorMessage.value = '请选择复核结论（是否属实）'
    return false
  }
  if (form.value.confirmed === false && !form.value.reviewNote.trim()) {
    errorMessage.value = '复核结论为不属实时必须填写复核说明'
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
    const res = await inspectionApi.reviewInspection(props.inspection.id, {
      reviewer: form.value.reviewer.trim(),
      reviewTime: form.value.reviewTime,
      confirmed: form.value.confirmed,
      reviewNote: form.value.reviewNote.trim() || null
    })
    if (res.data.code === 200) {
      emit('reviewed')
      handleClose()
    } else {
      handleServerError(res.data.message)
    }
  } catch (error) {
    handleServerError(error.response?.data?.message)
    console.error('提交复核失败:', error)
  } finally {
    submitting.value = false
  }
}

const handleServerError = (message) => {
  const msg = message || '接口请求失败，复核未提交，请稍后重试'
  errorMessage.value = msg
  // 其他值班已复核过时，刷新列表让复核结论与可否报修保持最新
  if (msg.includes('重复复核')) {
    emit('reviewed')
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

.inspect-summary {
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

.summary-item.full {
  grid-column: 1 / -1;
}

.summary-item .label {
  color: #999;
  min-width: 60px;
  flex-shrink: 0;
}

.review-tip {
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
.form-group input[type="datetime-local"],
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
</style>
