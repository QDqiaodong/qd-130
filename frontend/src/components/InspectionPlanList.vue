
<template>
  <div class="plan-list">
    <div class="list-header">
      <h3>巡检计划</h3>
      <button class="add-btn" @click="handleAdd">新建计划</button>
    </div>

    <table class="plan-table">
      <thead>
        <tr>
          <th>计划编号</th>
          <th>计划名称</th>
          <th>巡检对象</th>
          <th>巡检周期</th>
          <th>下次巡检日期</th>
          <th>巡检员</th>
          <th>状态</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="plan in planList" :key="plan.id">
          <td>{{ plan.planNo }}</td>
          <td>{{ plan.planName }}</td>
          <td>
            <span v-if="plan.equipmentId">设备：{{ plan.equipmentName }}（{{ plan.equipmentNo }}）</span>
            <span v-else>区域：{{ plan.areaName }}</span>
          </td>
          <td>{{ plan.cycleTypeName }}</td>
          <td>{{ plan.nextInspectionDate || '-' }}</td>
          <td>{{ plan.inspector || '-' }}</td>
          <td :class="['status', plan.status === 1 ? 'active' : 'disabled']">
            {{ plan.status === 1 ? '启用' : '停用' }}
          </td>
          <td>
            <button class="btn-edit" @click="handleEdit(plan)">编辑</button>
            <button class="btn-toggle" @click="handleToggle(plan)">
              {{ plan.status === 1 ? '停用' : '启用' }}
            </button>
          </td>
        </tr>
        <tr v-if="planList.length === 0">
          <td colspan="8" class="empty">暂无巡检计划</td>
        </tr>
      </tbody>
    </table>

    <div class="modal-overlay" v-if="formVisible" @click="handleClose">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>{{ editPlan ? '编辑巡检计划' : '新建巡检计划' }}</h3>
          <button class="close-btn" @click="handleClose">×</button>
        </div>
        <div class="modal-body">
          <form @submit.prevent="handleSubmit">
            <div class="form-group">
              <label>计划名称:</label>
              <input type="text" v-model="form.planName" required placeholder="请输入计划名称" />
            </div>

            <div class="form-group">
              <label>巡检对象:</label>
              <div class="scope-radio">
                <label><input type="radio" value="equipment" v-model="form.scopeType" /> 按设备</label>
                <label><input type="radio" value="area" v-model="form.scopeType" /> 按区域</label>
              </div>
            </div>

            <div class="form-group" v-if="form.scopeType === 'equipment'">
              <label>关联设备:</label>
              <select v-model="form.equipmentId" required>
                <option value="" disabled>请选择设备</option>
                <option v-for="eq in equipmentOptions" :key="eq.id" :value="eq.id">
                  {{ eq.equipmentName }}（{{ eq.equipmentNo }}）
                </option>
              </select>
            </div>

            <div class="form-group" v-else>
              <label>关联区域:</label>
              <select v-model="form.areaId" required>
                <option value="" disabled>请选择区域</option>
                <option v-for="area in areaOptions" :key="area.id" :value="area.id">
                  {{ area.name }}
                </option>
              </select>
            </div>

            <div class="form-group">
              <label>巡检周期:</label>
              <select v-model="form.cycleType" required>
                <option :value="1">每日</option>
                <option :value="2">每周</option>
                <option :value="3">每月</option>
              </select>
            </div>

            <div class="form-group">
              <label>下次巡检日期:</label>
              <input type="date" v-model="form.nextInspectionDate" required />
            </div>

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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { inspectionPlanApi, equipmentApi, areaApi } from '../api'

const planList = ref([])
const equipmentOptions = ref([])
const areaOptions = ref([])
const formVisible = ref(false)
const editPlan = ref(null)
const submitting = ref(false)

const defaultForm = () => ({
  planName: '',
  scopeType: 'equipment',
  equipmentId: '',
  areaId: '',
  cycleType: 2,
  nextInspectionDate: new Date().toISOString().split('T')[0],
  inspector: '',
  remark: ''
})

const form = ref(defaultForm())

const loadPlans = async () => {
  try {
    const res = await inspectionPlanApi.getAllPlans()
    if (res.data.code === 200) {
      planList.value = res.data.data
    }
  } catch (error) {
    console.error('加载巡检计划失败:', error)
  }
}

const loadOptions = async () => {
  try {
    const [eqRes, areaRes] = await Promise.all([
      equipmentApi.getAllEquipments(),
      areaApi.getAllAreas()
    ])
    if (eqRes.data.code === 200) {
      equipmentOptions.value = eqRes.data.data
    }
    if (areaRes.data.code === 200) {
      areaOptions.value = areaRes.data.data.filter(a => a.status === 1)
    }
  } catch (error) {
    console.error('加载选项失败:', error)
  }
}

const handleAdd = () => {
  editPlan.value = null
  form.value = defaultForm()
  formVisible.value = true
}

const handleEdit = (plan) => {
  editPlan.value = plan
  form.value = {
    planName: plan.planName,
    scopeType: plan.equipmentId ? 'equipment' : 'area',
    equipmentId: plan.equipmentId || '',
    areaId: plan.areaId || '',
    cycleType: plan.cycleType,
    nextInspectionDate: plan.nextInspectionDate || new Date().toISOString().split('T')[0],
    inspector: plan.inspector || '',
    remark: plan.remark || ''
  }
  formVisible.value = true
}

const handleSubmit = async () => {
  if (submitting.value) return
  submitting.value = true

  const data = {
    planName: form.value.planName,
    equipmentId: form.value.scopeType === 'equipment' ? form.value.equipmentId : null,
    areaId: form.value.scopeType === 'area' ? form.value.areaId : null,
    cycleType: form.value.cycleType,
    nextInspectionDate: form.value.nextInspectionDate,
    inspector: form.value.inspector,
    remark: form.value.remark
  }

  try {
    const res = editPlan.value
      ? await inspectionPlanApi.updatePlan(editPlan.value.id, data)
      : await inspectionPlanApi.createPlan(data)
    if (res.data.code === 200) {
      formVisible.value = false
      loadPlans()
    } else {
      alert(res.data.message || '保存失败')
    }
  } catch (error) {
    alert(error.response?.data?.message || '保存巡检计划失败')
  } finally {
    submitting.value = false
  }
}

const handleToggle = async (plan) => {
  const target = plan.status === 1 ? 0 : 1
  try {
    const res = await inspectionPlanApi.updatePlanStatus(plan.id, target)
    if (res.data.code === 200) {
      loadPlans()
    } else {
      alert(res.data.message || '状态更新失败')
    }
  } catch (error) {
    alert(error.response?.data?.message || '状态更新失败')
  }
}

const handleClose = () => {
  formVisible.value = false
}

onMounted(() => {
  loadPlans()
  loadOptions()
})
</script>

<style scoped>
.plan-list {
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 16px;
  background: #fff;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.list-header h3 {
  margin: 0;
  font-size: 16px;
  color: #333;
  border-bottom: 1px solid #e0e0e0;
  padding-bottom: 8px;
  flex: 1;
  margin-right: 16px;
}

.add-btn {
  padding: 8px 20px;
  border: none;
  border-radius: 4px;
  background: #67c23a;
  color: #fff;
  cursor: pointer;
  font-size: 14px;
}

.add-btn:hover {
  background: #85ce61;
}

.plan-table {
  width: 100%;
  border-collapse: collapse;
}

.plan-table th,
.plan-table td {
  padding: 12px 8px;
  text-align: left;
  border-bottom: 1px solid #e0e0e0;
  font-size: 14px;
}

.plan-table th {
  background: #f8f9fa;
  color: #666;
  font-weight: bold;
}

.status.active {
  color: #67c23a;
}

.status.disabled {
  color: #909399;
}

.plan-table td button {
  padding: 4px 10px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  margin-right: 4px;
}

.btn-edit {
  background: #409eff;
  color: #fff;
}

.btn-toggle {
  background: #e6a23c;
  color: #fff;
}

.empty {
  text-align: center;
  color: #999;
  padding: 40px;
}

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

.scope-radio {
  display: flex;
  gap: 16px;
}

.scope-radio label {
  display: flex;
  align-items: center;
  gap: 4px;
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
