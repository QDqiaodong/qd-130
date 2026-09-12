
<template>
  <div class="modal-overlay" v-if="visible" @click="handleClose">
    <div class="modal-content" @click.stop>
      <div class="modal-header">
        <h3>跨区域调配登记</h3>
        <button class="close-btn" @click="handleClose">×</button>
      </div>
      <div class="modal-body" v-if="equipment">
        <div class="equipment-info">
          <span class="label">设备编号:</span>
          <span>{{ equipment.equipmentNo }}</span>
        </div>
        <div class="equipment-info">
          <span class="label">设备名称:</span>
          <span>{{ equipment.equipmentName }}</span>
        </div>
        <div class="equipment-info">
          <span class="label">当前区域:</span>
          <span>{{ equipment.currentAreaName }}</span>
        </div>
        
        <form @submit.prevent="handleSubmit">
          <div class="form-group">
            <label>目标区域:</label>
            <select v-model="form.toAreaId" required>
              <option value="" disabled>请选择目标区域</option>
              <option v-for="area in areaOptions" :key="area.id" :value="area.id">
                {{ area.name }}
              </option>
            </select>
          </div>
          
          <div class="form-group">
            <label>调配日期:</label>
            <input type="date" v-model="form.transferDate" required />
          </div>
          
          <div class="form-group">
            <label>调配原因:</label>
            <textarea v-model="form.reason" rows="3" placeholder="请输入调配原因"></textarea>
          </div>
          
          <div class="form-group">
            <label>操作人:</label>
            <input type="text" v-model="form.operator" placeholder="请输入操作人姓名" />
          </div>
          
          <div class="form-group">
            <label>备注:</label>
            <textarea v-model="form.remark" rows="2" placeholder="请输入备注信息"></textarea>
          </div>
          
          <div class="form-buttons">
            <button type="button" class="btn-cancel" @click="handleClose">取消</button>
            <button type="submit" class="btn-submit">提交</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { areaApi, transferApi } from '../api'

const props = defineProps({
  visible: Boolean,
  equipment: Object
})

const emit = defineEmits(['close', 'success'])

const areaOptions = ref([])
const form = ref({
  toAreaId: '',
  transferDate: new Date().toISOString().split('T')[0],
  reason: '',
  operator: '',
  remark: ''
})

const loadAreas = async () => {
  try {
    const res = await areaApi.getAllAreas()
    if (res.data.code === 200) {
      areaOptions.value = res.data.data.filter(a => a.status === 1)
    }
  } catch (error) {
    console.error('加载区域列表失败:', error)
  }
}

const handleSubmit = async () => {
  if (!props.equipment) return
  
  try {
    const data = {
      equipmentId: props.equipment.id,
      toAreaId: form.value.toAreaId,
      transferDate: form.value.transferDate,
      reason: form.value.reason,
      operator: form.value.operator,
      remark: form.value.remark
    }
    
    const res = await transferApi.createTransfer(data)
    if (res.data.code === 200) {
      emit('success')
      handleClose()
    } else {
      alert('提交失败: ' + (res.data.message || '未知错误'))
    }
  } catch (error) {
    console.error('提交调配记录失败:', error)
    alert('提交失败: ' + (error.response?.data?.message
      || (error.code === 'ECONNABORTED' ? '接口请求超时，请稍后重试' : '未知错误')))
  }
}

const handleClose = () => {
  form.value = {
    toAreaId: '',
    transferDate: new Date().toISOString().split('T')[0],
    reason: '',
    operator: '',
    remark: ''
  }
  emit('close')
}

watch(() => props.visible, (val) => {
  if (val) {
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

.equipment-info {
  display: flex;
  margin-bottom: 8px;
}

.equipment-info .label {
  color: #999;
  width: 80px;
}

.equipment-info span:last-child {
  color: #333;
  font-weight: bold;
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

.form-group input,
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
  transition: background 0.2s;
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
  transition: background 0.2s;
}

.btn-submit:hover {
  background: #66b1ff;
}
</style>
