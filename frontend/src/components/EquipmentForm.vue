
<template>
  <div class="modal-overlay" v-if="visible" @click="handleClose">
    <div class="modal-content" @click.stop>
      <div class="modal-header">
        <h3>{{ equipment?.id ? '编辑设备' : '新建设备' }}</h3>
        <button class="close-btn" @click="handleClose">×</button>
      </div>
      <div class="modal-body">
        <form @submit.prevent="handleSubmit">
          <div class="form-group">
            <label>设备编号:</label>
            <input type="text" v-model="form.equipmentNo" :disabled="equipment?.id" placeholder="自动生成" />
          </div>
          
          <div class="form-group">
            <label>设备名称:</label>
            <input type="text" v-model="form.equipmentName" required placeholder="请输入设备名称" />
          </div>
          
          <div class="form-group">
            <label>功能类型:</label>
            <input type="text" v-model="form.equipmentType" required placeholder="如：温奶器、护理台" />
          </div>
          
          <div class="form-group">
            <label>型号规格:</label>
            <input type="text" v-model="form.model" placeholder="请输入型号规格" />
          </div>
          
          <div class="form-group">
            <label>品牌:</label>
            <input type="text" v-model="form.brand" placeholder="请输入品牌" />
          </div>
          
          <div class="form-group">
            <label>设备图片URL:</label>
            <input type="text" v-model="form.imageUrl" placeholder="请输入设备图片URL" />
          </div>
          
          <div class="form-group">
            <label>初始区域:</label>
            <select v-model="form.initialAreaId" required>
              <option value="" disabled>请选择初始区域</option>
              <option v-for="area in areaOptions" :key="area.id" :value="area.id">
                {{ area.name }}
              </option>
            </select>
          </div>
          
          <div class="form-group">
            <label>当前区域:</label>
            <select v-model="form.currentAreaId" required>
              <option value="" disabled>请选择当前区域</option>
              <option v-for="area in areaOptions" :key="area.id" :value="area.id">
                {{ area.name }}
              </option>
            </select>
          </div>
          
          <div class="form-group">
            <label>状态:</label>
            <select v-model="form.status">
              <option :value="1">使用中</option>
              <option :value="0">停用</option>
            </select>
          </div>
          
          <div class="form-group">
            <label>备注:</label>
            <textarea v-model="form.remark" rows="2" placeholder="请输入备注信息"></textarea>
          </div>
          
          <div class="form-buttons">
            <button type="button" class="btn-cancel" @click="handleClose">取消</button>
            <button type="submit" class="btn-submit">{{ equipment?.id ? '保存' : '创建' }}</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { areaApi, equipmentApi } from '../api'

const props = defineProps({
  visible: Boolean,
  equipment: Object
})

const emit = defineEmits(['close', 'success'])

const areaOptions = ref([])
const form = ref({
  equipmentNo: '',
  equipmentName: '',
  equipmentType: '',
  model: '',
  brand: '',
  imageUrl: '',
  initialAreaId: '',
  currentAreaId: '',
  status: 1,
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
  try {
    let res
    if (props.equipment?.id) {
      res = await equipmentApi.updateEquipment(props.equipment.id, form.value)
    } else {
      res = await equipmentApi.createEquipment(form.value)
    }
    if (res.data.code === 200) {
      emit('success')
      handleClose()
    }
  } catch (error) {
    console.error('提交设备信息失败:', error)
    alert('提交失败: ' + (error.response?.data?.message || '未知错误'))
  }
}

const handleClose = () => {
  form.value = {
    equipmentNo: '',
    equipmentName: '',
    equipmentType: '',
    model: '',
    brand: '',
    imageUrl: '',
    initialAreaId: '',
    currentAreaId: '',
    status: 1,
    remark: ''
  }
  emit('close')
}

watch(() => props.visible, (val) => {
  if (val) {
    loadAreas()
    if (props.equipment) {
      form.value = {
        equipmentNo: props.equipment.equipmentNo || '',
        equipmentName: props.equipment.equipmentName || '',
        equipmentType: props.equipment.equipmentType || '',
        model: props.equipment.model || '',
        brand: props.equipment.brand || '',
        imageUrl: props.equipment.imageUrl || '',
        initialAreaId: props.equipment.initialAreaId || '',
        currentAreaId: props.equipment.currentAreaId || '',
        status: props.equipment.status != null ? props.equipment.status : 1,
        remark: props.equipment.remark || ''
      }
    }
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

.form-group input:disabled {
  background: #f5f5f5;
  color: #999;
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
