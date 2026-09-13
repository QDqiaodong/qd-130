
<template>
  <div class="modal-overlay" v-if="visible" @click="handleClose">
    <div class="modal-content" @click.stop>
      <div class="modal-header">
        <h3>温奶器抽检登记</h3>
        <button class="close-btn" @click="handleClose">×</button>
      </div>
      <div class="modal-body">
        <form @submit.prevent="handleSubmit">
          <div class="form-group">
            <label>所属母婴室:</label>
            <select v-model="form.areaId" @change="handleAreaChange">
              <option value="" disabled>请选择母婴室区域</option>
              <option v-for="area in warmerAreaOptions" :key="area.id" :value="area.id">
                {{ area.name }}（{{ area.warmerCount }}台在用温奶器）
              </option>
            </select>
          </div>

          <div class="form-group">
            <label>在用温奶器: <span class="required">*</span></label>
            <select v-model="form.equipmentId" required>
              <option value="" disabled>请选择温奶器</option>
              <option v-for="warmer in warmerOptionsInArea" :key="warmer.id" :value="warmer.id">
                {{ warmer.equipmentName }}（{{ warmer.equipmentNo }}）
              </option>
            </select>
            <p v-if="form.areaId && warmerOptionsInArea.length === 0" class="field-tip warn">
              该母婴室当前没有在用温奶器，无法登记抽检
            </p>
          </div>

          <div class="form-group">
            <label>抽检日期: <span class="required">*</span></label>
            <input type="date" v-model="form.checkDate" required />
          </div>

          <div class="form-group">
            <label>实测水温（℃）: <span class="required">*</span></label>
            <input type="number" step="0.1" min="0" max="100" v-model="form.temperature"
                   required placeholder="请输入实测水温，如 45.0" />
            <p class="field-tip">合格水温区间 {{ MIN_TEMP }}℃ ~ {{ MAX_TEMP }}℃，抽检结论按温度自动判定</p>
            <p v-if="temperatureConclusion" :class="['conclusion-tag', temperatureConclusion.qualified ? 'ok' : 'bad']">
              {{ temperatureConclusion.text }}
            </p>
          </div>

          <div class="form-group">
            <label>抽检照片:</label>
            <input type="text" v-model="form.photoUrl" placeholder="请输入抽检照片URL（不合格时建议上传）" />
            <div v-if="form.photoUrl" class="photo-preview">
              <img :src="form.photoUrl" alt="抽检照片" @error="photoError = true" v-show="!photoError" />
              <a v-if="photoError" :href="form.photoUrl" target="_blank">{{ form.photoUrl }}</a>
            </div>
          </div>

          <template v-if="temperatureConclusion && !temperatureConclusion.qualified">
            <div class="form-group">
              <label>不合格说明:</label>
              <textarea v-model="form.abnormalDesc" rows="2"
                        placeholder="可补充异常现象，不填将按温度自动生成说明"></textarea>
            </div>

            <div class="form-group checkbox-group">
              <label>
                <input type="checkbox" v-model="form.createRepair" /> 同时提交报修单（不合格建议立即补报修）
              </label>
            </div>
          </template>

          <div class="form-group">
            <label>抽检人（值班人员）: <span class="required">*</span></label>
            <input type="text" v-model="form.inspector" required placeholder="请输入抽检人姓名" />
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
import { ref, computed, watch } from 'vue'
import { spotCheckApi, areaApi, equipmentApi } from '../api'

const props = defineProps({
  visible: Boolean
})

const emit = defineEmits(['close', 'success'])

const MIN_TEMP = 40
const MAX_TEMP = 50

const allWarmers = ref([])
const allAreas = ref([])
const submitting = ref(false)
const photoError = ref(false)

const defaultForm = () => ({
  areaId: '',
  equipmentId: '',
  checkDate: new Date().toISOString().split('T')[0],
  temperature: '',
  abnormalDesc: '',
  photoUrl: '',
  inspector: '',
  remark: '',
  createRepair: true
})

const form = ref(defaultForm())

const warmerAreaOptions = computed(() => {
  return allAreas.value
    .filter(area => allWarmers.value.some(eq => eq.currentAreaId === area.id))
    .map(area => ({
      ...area,
      warmerCount: allWarmers.value.filter(eq => eq.currentAreaId === area.id).length
    }))
})

const warmerOptionsInArea = computed(() =>
  form.value.areaId ? allWarmers.value.filter(eq => eq.currentAreaId === form.value.areaId) : []
)

const temperatureConclusion = computed(() => {
  const value = parseFloat(form.value.temperature)
  if (Number.isNaN(value)) return null
  if (value < 0 || value > 100) {
    return { qualified: false, text: '温度超出0~100℃范围，请确认是否填错' }
  }
  if (value < MIN_TEMP) {
    return { qualified: false, text: `实测${value}℃低于${MIN_TEMP}℃，抽检结论：不合格` }
  }
  if (value > MAX_TEMP) {
    return { qualified: false, text: `实测${value}℃高于${MAX_TEMP}℃，抽检结论：不合格` }
  }
  return { qualified: true, text: `实测${value}℃在合格区间内，抽检结论：合格` }
})

const loadOptions = async () => {
  try {
    const [areaRes, eqRes] = await Promise.all([
      areaApi.getAllAreas(),
      equipmentApi.getAllEquipments()
    ])
    if (areaRes.data.code === 200) {
      allAreas.value = areaRes.data.data.filter(a => a.status === 1)
    } else {
      alert(areaRes.data.message || '加载区域列表失败')
    }
    if (eqRes.data.code === 200) {
      allWarmers.value = eqRes.data.data.filter(
        eq => eq.equipmentType === '温奶器' && (eq.status === 1 || eq.status == null)
      )
    } else {
      alert(eqRes.data.message || '加载温奶器列表失败')
    }
  } catch (error) {
    alert(error.response?.data?.message || '加载基础数据失败，请稍后重试')
    console.error('加载抽检基础数据失败:', error)
  }
}

const handleAreaChange = () => {
  form.value.equipmentId = ''
}

const handleSubmit = async () => {
  if (submitting.value) return

  if (!form.value.equipmentId) {
    alert('请选择在用温奶器')
    return
  }
  if (!form.value.checkDate) {
    alert('请选择抽检日期')
    return
  }
  const value = parseFloat(form.value.temperature)
  if (Number.isNaN(value)) {
    alert('请填写抽检温度')
    return
  }
  if (value < 0 || value > 100) {
    alert('抽检温度不合法，请填写0~100℃之间的实测水温')
    return
  }
  if (!form.value.inspector.trim()) {
    alert('请填写抽检人')
    return
  }

  const qualified = value >= MIN_TEMP && value <= MAX_TEMP
  submitting.value = true
  try {
    const data = {
      equipmentId: form.value.equipmentId,
      checkDate: form.value.checkDate,
      temperature: value,
      abnormalDesc: !qualified ? form.value.abnormalDesc : null,
      photoUrl: form.value.photoUrl || null,
      inspector: form.value.inspector.trim(),
      remark: form.value.remark || null,
      createRepair: !qualified && form.value.createRepair
    }

    const res = await spotCheckApi.createSpotCheck(data)
    if (res.data.code === 200) {
      const dto = res.data.data
      if (dto && dto.repairMessage) {
        alert(`抽检记录已登记为不合格，但报修单未创建：${dto.repairMessage}\n请在列表中补填当班复核人后点击「去补报修」手动补录。`)
      }
      emit('success')
      handleClose()
    } else {
      alert(res.data.message || '抽检登记失败，请稍后重试')
    }
  } catch (error) {
    alert(error.response?.data?.message || '接口请求失败，抽检记录未提交，请稍后重试')
    console.error('提交抽检记录失败:', error)
  } finally {
    submitting.value = false
  }
}

const handleClose = () => {
  form.value = defaultForm()
  photoError.value = false
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
.form-group input[type="number"],
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

.field-tip {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}

.field-tip.warn {
  color: #e6a23c;
}

.conclusion-tag {
  margin-top: 6px;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 13px;
  display: inline-block;
}

.conclusion-tag.ok {
  background: #f0f9eb;
  color: #67c23a;
}

.conclusion-tag.bad {
  background: #fef0f0;
  color: #f56c6c;
}

.checkbox-group label {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #e6a23c;
  cursor: pointer;
}

.photo-preview {
  margin-top: 8px;
}

.photo-preview img {
  max-width: 160px;
  max-height: 120px;
  border-radius: 4px;
  border: 1px solid #e0e0e0;
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
