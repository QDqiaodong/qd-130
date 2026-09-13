<template>
  <div class="modal-overlay" v-if="visible" @click="handleClose">
    <div class="modal-content" @click.stop>
      <div class="modal-header">
        <h3>补填当班复核人</h3>
        <button class="close-btn" @click="handleClose">×</button>
      </div>
      <div class="modal-body" v-if="record">
        <div class="record-summary">
          <div class="summary-item"><span class="label">抽检单号:</span><span>{{ record.spotCheckNo }}</span></div>
          <div class="summary-item"><span class="label">温奶器:</span><span>{{ record.equipmentName }}（{{ record.equipmentNo }}）</span></div>
          <div class="summary-item"><span class="label">实测水温:</span><span class="temp-bad">{{ record.temperature }}℃</span></div>
          <div class="summary-item"><span class="label">抽检结论:</span><span class="result-tag abnormal">不合格</span></div>
        </div>

        <p class="review-tip">不合格抽检须先补填当班复核人，补填后才允许转报修。</p>

        <form @submit.prevent="handleSubmit">
          <div class="form-group">
            <label>当班复核人: <span class="required">*</span></label>
            <input type="text" v-model="reviewer" maxlength="50" placeholder="请输入当班复核人姓名" />
          </div>

          <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>

          <div class="form-buttons">
            <button type="button" class="btn-cancel" @click="handleClose">取消</button>
            <button type="submit" class="btn-submit" :disabled="submitting">
              {{ submitting ? '提交中...' : '确认补填' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { spotCheckApi } from '../api'

const props = defineProps({
  visible: Boolean,
  record: Object
})

const emit = defineEmits(['close', 'reviewed'])

const reviewer = ref('')
const submitting = ref(false)
const errorMessage = ref('')

const handleSubmit = async () => {
  if (submitting.value || !props.record) return
  if (!reviewer.value.trim()) {
    errorMessage.value = '请填写当班复核人，未补填复核人不能转报修'
    return
  }

  submitting.value = true
  errorMessage.value = ''
  try {
    const res = await spotCheckApi.updateReviewer(props.record.id, {
      reviewer: reviewer.value.trim()
    })
    if (res.data.code === 200) {
      emit('reviewed')
      handleClose()
    } else {
      errorMessage.value = res.data.message || '补填复核人失败，请稍后重试'
    }
  } catch (error) {
    errorMessage.value = error.response?.data?.message || '接口请求失败，复核人未保存，请稍后重试'
    console.error('补填当班复核人失败:', error)
  } finally {
    submitting.value = false
  }
}

const handleClose = () => {
  reviewer.value = ''
  errorMessage.value = ''
  emit('close')
}

watch(() => props.visible, (val) => {
  if (val) {
    reviewer.value = ''
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
  z-index: 1100;
}

.modal-content {
  background: #fff;
  border-radius: 8px;
  width: 90%;
  max-width: 480px;
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

.record-summary {
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
  min-width: 60px;
  flex-shrink: 0;
}

.temp-bad {
  color: #f56c6c;
  font-weight: bold;
}

.result-tag.abnormal {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  background: #fef0f0;
  color: #f56c6c;
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

.form-group input[type="text"] {
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
