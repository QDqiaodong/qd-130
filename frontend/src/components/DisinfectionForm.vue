<template>
  <div class="modal-overlay" v-if="visible" @click="handleClose">
    <div class="modal-content" @click.stop>
      <div class="modal-header">
        <h3>母婴室消毒登记</h3>
        <button class="close-btn" @click="handleClose">×</button>
      </div>
      <div class="modal-body">
        <form @submit.prevent="handleSubmit">
          <div class="form-group">
            <label>母婴室: <span class="required">*</span></label>
            <select v-model="form.areaId" required>
              <option value="" disabled>请选择母婴室区域</option>
              <option v-for="area in areaOptions" :key="area.id" :value="area.id">{{ area.name }}</option>
            </select>
          </div>

          <div class="form-group">
            <label>消毒日期: <span class="required">*</span></label>
            <input type="date" v-model="form.disinfectDate" required />
          </div>

          <div class="form-group">
            <label>消毒人（值班人员）: <span class="required">*</span></label>
            <input type="text" v-model="form.operator" required placeholder="请输入消毒人姓名" />
          </div>

          <div class="form-group">
            <label>完成时间: <span class="required">*</span></label>
            <input type="datetime-local" v-model="form.finishTime" required />
          </div>

          <div class="form-group">
            <label>消毒液: <span class="required">*</span></label>
            <input type="text" v-model="form.disinfectant" required placeholder="如：84消毒液（1:100）" />
          </div>

          <div class="form-group">
            <label>通风是否做完: <span class="required">*</span></label>
            <div class="radio-row">
              <label><input type="radio" value="true" v-model="form.ventilationDone" /> 已做完</label>
              <label><input type="radio" value="false" v-model="form.ventilationDone" /> 未做完</label>
            </div>
            <p class="field-tip">通风未做完不能算当日闭环，需填写未完成原因，当日可再次登记补齐</p>
          </div>

          <div class="form-group" v-if="form.ventilationDone === 'false'">
            <label>未完成原因: <span class="required">*</span></label>
            <textarea v-model="form.incompleteReason" rows="2"
                      placeholder="通风未做完必须填写未完成原因"></textarea>
          </div>

          <div class="form-group">
            <label>备注:</label>
            <textarea v-model="form.remark" rows="2" placeholder="请输入备注信息"></textarea>
          </div>

          <div class="form-buttons">
            <button type="button" class="btn-cancel" @click="handleClose">取消</button>
            <button type="submit" class="btn-submit" :disabled="submitting">
              {{ submitting ? '提交中...' : '提交' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { disinfectionApi, areaApi } from '../api'

const props = defineProps({
  visible: Boolean,
  presetAreaId: { type: [Number, String], default: '' },
  presetDate: { type: String, default: '' }
})

const emit = defineEmits(['close', 'success'])

const areaOptions = ref([])
const submitting = ref(false)

const todayStr = () => new Date().toLocaleDateString('sv-SE')

const nowLocalDateTime = () => {
  const now = new Date()
  const pad = n => String(n).padStart(2, '0')
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}T${pad(now.getHours())}:${pad(now.getMinutes())}`
}

const defaultForm = () => ({
  areaId: '',
  disinfectDate: todayStr(),
  operator: '',
  finishTime: nowLocalDateTime(),
  disinfectant: '',
  ventilationDone: '',
  incompleteReason: '',
  remark: ''
})

const form = ref(defaultForm())

const loadAreas = async () => {
  try {
    const res = await areaApi.getAllAreas()
    if (res.data.code === 200) {
      areaOptions.value = res.data.data.filter(a => a.status === 1)
    } else {
      alert(res.data.message || '加载母婴室列表失败')
    }
  } catch (error) {
    alert(error.response?.data?.message || '接口请求失败，母婴室列表加载失败，请稍后重试')
    console.error('加载区域列表失败:', error)
  }
}

const handleSubmit = async () => {
  if (submitting.value) return

  if (!form.value.areaId) {
    alert('请选择消毒的母婴室')
    return
  }
  if (!form.value.disinfectDate) {
    alert('请选择消毒日期')
    return
  }
  if (!form.value.operator.trim()) {
    alert('请填写消毒人')
    return
  }
  if (!form.value.finishTime) {
    alert('请选择完成时间')
    return
  }
  if (!form.value.disinfectant.trim()) {
    alert('请填写消毒液')
    return
  }
  if (form.value.ventilationDone !== 'true' && form.value.ventilationDone !== 'false') {
    alert('请选择通风是否做完')
    return
  }
  const ventilationDone = form.value.ventilationDone === 'true'
  if (!ventilationDone && !form.value.incompleteReason.trim()) {
    alert('通风未做完时必须填写未完成原因')
    return
  }

  submitting.value = true
  try {
    const data = {
      areaId: form.value.areaId,
      disinfectDate: form.value.disinfectDate,
      operator: form.value.operator.trim(),
      finishTime: form.value.finishTime,
      disinfectant: form.value.disinfectant.trim(),
      ventilationDone,
      incompleteReason: ventilationDone ? null : form.value.incompleteReason.trim(),
      remark: form.value.remark || null
    }

    const res = await disinfectionApi.createDisinfection(data)
    if (res.data.code === 200) {
      emit('success')
      handleClose()
    } else {
      alert(res.data.message || '消毒登记失败，请稍后重试')
    }
  } catch (error) {
    alert(error.response?.data?.message || '接口请求失败，消毒登记未提交，请稍后重试')
    console.error('提交消毒登记失败:', error)
  } finally {
    submitting.value = false
  }
}

const handleClose = () => {
  form.value = defaultForm()
  emit('close')
}

watch(() => props.visible, (val) => {
  if (val) {
    form.value = defaultForm()
    if (props.presetAreaId !== '' && props.presetAreaId !== null) {
      form.value.areaId = props.presetAreaId
    }
    if (props.presetDate) {
      form.value.disinfectDate = props.presetDate
    }
    loadAreas()
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
.form-group input[type="date"],
.form-group input[type="datetime-local"],
.form-group select,
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

.field-tip {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}

.radio-row {
  display: flex;
  gap: 20px;
}

.radio-row label {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #333;
  cursor: pointer;
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
</style>
