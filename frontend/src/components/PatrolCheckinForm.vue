<template>
  <div class="modal-overlay" v-if="visible" @click="handleClose">
    <div class="modal-content" @click.stop>
      <div class="modal-header">
        <h3>夜间巡更打卡</h3>
        <button class="close-btn" @click="handleClose">×</button>
      </div>
      <div class="modal-body">
        <form @submit.prevent="handleSubmit">
          <div class="form-group">
            <label>母婴室: <span class="required">*</span></label>
            <select v-model="form.areaId" required>
              <option value="" disabled>请选择巡更的母婴室</option>
              <option v-for="area in areaOptions" :key="area.id" :value="area.id">{{ area.name }}</option>
            </select>
          </div>

          <div class="form-group">
            <label>巡更日期: <span class="required">*</span></label>
            <input type="date" v-model="form.patrolDate" required />
          </div>

          <div class="form-group">
            <label>巡更班次: <span class="required">*</span></label>
            <select v-model="form.shift" required>
              <option value="" disabled>请选择巡更班次</option>
              <option v-for="opt in shiftOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
            </select>
          </div>

          <div class="form-group">
            <label>巡更人（值班人员）: <span class="required">*</span></label>
            <input type="text" v-model="form.patrolPerson" required placeholder="请输入巡更人姓名" />
          </div>

          <div class="form-group">
            <label>打卡时间: <span class="required">*</span></label>
            <input type="datetime-local" v-model="form.checkinTime" required />
          </div>

          <div class="form-group">
            <label>备注:</label>
            <textarea v-model="form.remark" rows="2" placeholder="如：门窗正常，温奶器已断电"></textarea>
          </div>

          <p class="field-tip">同一母婴室同一巡更日期同一班次只能打一次卡，已打卡的房间不能再补同一班次</p>

          <div class="form-buttons">
            <button type="button" class="btn-cancel" @click="handleClose">取消</button>
            <button type="submit" class="btn-submit" :disabled="submitting">
              {{ submitting ? '提交中...' : '确认打卡' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { patrolApi, areaApi } from '../api'

const props = defineProps({
  visible: Boolean,
  // 从漏打房间进入补打卡时带入的预设值
  preset: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['close', 'success'])

const SHIFT_OPTIONS = [
  { value: 1, label: '前夜班（22:00-02:00）' },
  { value: 2, label: '后夜班（02:00-06:00）' }
]
const shiftOptions = SHIFT_OPTIONS

const areaOptions = ref([])
const submitting = ref(false)

const todayStr = () => new Date().toLocaleDateString('sv-SE')

const nowLocalDateTime = () => {
  const now = new Date()
  const pad = n => String(n).padStart(2, '0')
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}T${pad(now.getHours())}:${pad(now.getMinutes())}`
}

const defaultForm = () => ({
  areaId: props.preset.areaId ?? '',
  patrolDate: props.preset.patrolDate || todayStr(),
  shift: props.preset.shift ?? '',
  patrolPerson: '',
  checkinTime: nowLocalDateTime(),
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
    alert('请选择巡更的母婴室')
    return
  }
  if (!form.value.patrolDate) {
    alert('请选择巡更日期')
    return
  }
  if (form.value.shift === '' || form.value.shift === null) {
    alert('请选择巡更班次')
    return
  }
  if (!form.value.patrolPerson.trim()) {
    alert('请填写巡更人')
    return
  }
  if (!form.value.checkinTime) {
    alert('请选择打卡时间')
    return
  }

  submitting.value = true
  try {
    const data = {
      areaId: form.value.areaId,
      patrolDate: form.value.patrolDate,
      shift: Number(form.value.shift),
      patrolPerson: form.value.patrolPerson.trim(),
      checkinTime: form.value.checkinTime,
      remark: form.value.remark || null
    }

    const res = await patrolApi.createCheckin(data)
    if (res.data.code === 200) {
      emit('success')
      handleClose()
    } else {
      alert(res.data.message || '巡更打卡失败，请稍后重试')
    }
  } catch (error) {
    alert(error.response?.data?.message || '接口请求失败，巡更打卡未提交，请稍后重试')
    console.error('提交巡更打卡失败:', error)
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
