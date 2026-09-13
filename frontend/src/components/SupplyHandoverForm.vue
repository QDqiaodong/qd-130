<template>
  <div class="modal-overlay" v-if="visible" @click="handleClose">
    <div class="modal-content" @click.stop>
      <div class="modal-header">
        <h3>值班交班登记</h3>
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
            <label>交班日期: <span class="required">*</span></label>
            <input type="date" v-model="form.handoverDate" required />
          </div>

          <div class="form-group">
            <label>交班人（值班人员）: <span class="required">*</span></label>
            <input type="text" v-model="form.handoverPerson" required placeholder="请输入交班人姓名" />
          </div>

          <div class="form-group">
            <label>接班人: <span class="required">*</span></label>
            <input type="text" v-model="form.receiver" required placeholder="没写接班人不能交出班" />
          </div>

          <div class="form-group">
            <label>湿巾件数: <span class="required">*</span></label>
            <input type="number" v-model="form.wipesCount" min="0" step="1" required placeholder="盘点后填写湿巾剩余件数" />
          </div>

          <div class="form-group">
            <label>纸尿裤件数: <span class="required">*</span></label>
            <input type="number" v-model="form.diaperCount" min="0" step="1" required placeholder="盘点后填写纸尿裤剩余件数" />
          </div>

          <div class="form-group">
            <label>备注:</label>
            <textarea v-model="form.remark" rows="2" placeholder="如：湿巾余量偏少，已通知补货"></textarea>
          </div>

          <p class="field-tip">登记后状态为「未交接」，接班人核对件数确认交接后才算交出班</p>

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
import { supplyHandoverApi, areaApi } from '../api'

const props = defineProps({
  visible: Boolean
})

const emit = defineEmits(['close', 'success'])

const areaOptions = ref([])
const submitting = ref(false)

const todayStr = () => new Date().toLocaleDateString('sv-SE')

const defaultForm = () => ({
  areaId: '',
  handoverDate: todayStr(),
  handoverPerson: '',
  receiver: '',
  wipesCount: '',
  diaperCount: '',
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

const isValidCount = (value) => {
  if (value === '' || value === null || value === undefined) return false
  const num = Number(value)
  return Number.isInteger(num) && num >= 0
}

const handleSubmit = async () => {
  if (submitting.value) return

  if (!form.value.areaId) {
    alert('请选择交班的母婴室')
    return
  }
  if (!form.value.handoverDate) {
    alert('请选择交班日期')
    return
  }
  if (!form.value.handoverPerson.trim()) {
    alert('请填写交班人')
    return
  }
  if (!form.value.receiver.trim()) {
    alert('请填写接班人，没写接班人不能交出班')
    return
  }
  if (!isValidCount(form.value.wipesCount)) {
    alert('请填写湿巾件数（不小于0的整数）')
    return
  }
  if (!isValidCount(form.value.diaperCount)) {
    alert('请填写纸尿裤件数（不小于0的整数）')
    return
  }

  submitting.value = true
  try {
    const data = {
      areaId: form.value.areaId,
      handoverDate: form.value.handoverDate,
      handoverPerson: form.value.handoverPerson.trim(),
      receiver: form.value.receiver.trim(),
      wipesCount: Number(form.value.wipesCount),
      diaperCount: Number(form.value.diaperCount),
      remark: form.value.remark || null
    }

    const res = await supplyHandoverApi.createHandover(data)
    if (res.data.code === 200) {
      emit('success')
      handleClose()
    } else {
      alert(res.data.message || '交班登记失败，请稍后重试')
    }
  } catch (error) {
    alert(error.response?.data?.message || '接口请求失败，交班登记未提交，请稍后重试')
    console.error('提交交班登记失败:', error)
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
.form-group input[type="number"],
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
