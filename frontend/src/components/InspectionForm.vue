
<template>
  <div class="modal-overlay" v-if="visible" @click="handleClose">
    <div class="modal-content" @click.stop>
      <div class="modal-header">
        <h3>巡检登记</h3>
        <button class="close-btn" @click="handleClose">×</button>
      </div>
      <div class="modal-body">
        <form @submit.prevent="handleSubmit">
          <div class="form-group">
            <label>关联计划（可选）:</label>
            <select v-model="form.planId" @change="handlePlanChange">
              <option value="">不关联计划</option>
              <option v-for="plan in planOptions" :key="plan.id" :value="plan.id">
                {{ plan.planName }}（{{ plan.cycleTypeName }}）
              </option>
            </select>
          </div>

          <div class="form-group">
            <label>巡检设备:</label>
            <select v-model="form.equipmentId" required>
              <option value="" disabled>请选择设备</option>
              <option v-for="eq in equipmentOptions" :key="eq.id" :value="eq.id">
                {{ eq.equipmentName }}（{{ eq.equipmentNo }}）- {{ eq.currentAreaName || '未分配区域' }}
              </option>
            </select>
          </div>

          <div class="form-group">
            <label>巡检日期:</label>
            <input type="date" v-model="form.inspectionDate" required />
          </div>

          <div class="form-group">
            <label>巡检结果:</label>
            <div class="result-radio">
              <label><input type="radio" :value="1" v-model="form.result" /> 正常</label>
              <label><input type="radio" :value="2" v-model="form.result" /> 异常</label>
            </div>
          </div>

          <template v-if="form.result === 2">
            <div class="form-group">
              <label>异常描述: <span class="required">*</span></label>
              <textarea v-model="form.abnormalDesc" rows="3" required placeholder="请描述设备异常情况"></textarea>
            </div>

            <div class="form-group">
              <label>照片地址:</label>
              <input type="text" v-model="form.photoUrl" placeholder="请输入异常照片URL" />
            </div>

            <p class="review-flow-tip">异常巡检提交后需值班复核，复核属实才能转报修</p>
          </template>

          <div class="form-group">
            <label>巡检员:</label>
            <input type="text" v-model="form.inspector" placeholder="请输入巡检员姓名" />
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
import { inspectionApi, inspectionPlanApi, equipmentApi } from '../api'

const props = defineProps({
  visible: Boolean
})

const emit = defineEmits(['close', 'success'])

const planOptions = ref([])
const equipmentOptions = ref([])
const submitting = ref(false)

const defaultForm = () => ({
  planId: '',
  equipmentId: '',
  inspectionDate: new Date().toISOString().split('T')[0],
  result: 1,
  abnormalDesc: '',
  photoUrl: '',
  inspector: '',
  remark: ''
})

const form = ref(defaultForm())

const loadOptions = async () => {
  try {
    const [planRes, eqRes] = await Promise.all([
      inspectionPlanApi.getAllPlans(),
      equipmentApi.getAllEquipments()
    ])
    if (planRes.data.code === 200) {
      planOptions.value = planRes.data.data.filter(p => p.status === 1)
    }
    if (eqRes.data.code === 200) {
      equipmentOptions.value = eqRes.data.data
    }
  } catch (error) {
    console.error('加载选项失败:', error)
  }
}

const handlePlanChange = () => {
  const plan = planOptions.value.find(p => p.id === form.value.planId)
  if (plan && plan.equipmentId) {
    form.value.equipmentId = plan.equipmentId
  }
  if (plan && plan.inspector && !form.value.inspector) {
    form.value.inspector = plan.inspector
  }
}

const handleSubmit = async () => {
  if (submitting.value) return

  if (form.value.result === 2 && !form.value.abnormalDesc.trim()) {
    alert('巡检结果异常时必须填写异常描述')
    return
  }

  submitting.value = true
  try {
    const data = {
      planId: form.value.planId || null,
      equipmentId: form.value.equipmentId,
      inspectionDate: form.value.inspectionDate,
      result: form.value.result,
      abnormalDesc: form.value.result === 2 ? form.value.abnormalDesc : null,
      photoUrl: form.value.result === 2 ? form.value.photoUrl : null,
      inspector: form.value.inspector,
      remark: form.value.remark
    }

    const res = await inspectionApi.createInspection(data)
    if (res.data.code === 200) {
      emit('success')
      handleClose()
    } else {
      alert(res.data.message || '提交失败')
    }
  } catch (error) {
    alert(error.response?.data?.message || '提交巡检记录失败')
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
    loadOptions()
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
  max-width: 500px;
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

.checkbox-group label {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #e6a23c;
  cursor: pointer;
}

.review-flow-tip {
  margin: 0 0 16px;
  padding: 8px 12px;
  background: #fdf6ec;
  border: 1px solid #f5dab1;
  border-radius: 4px;
  color: #e6a23c;
  font-size: 12px;
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
