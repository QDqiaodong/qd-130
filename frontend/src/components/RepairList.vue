
<template>
  <div class="repair-list">
    <div class="list-header">
      <h3>故障报修</h3>
    </div>

    <div class="filter-section">
      <div v-if="drillTip" class="drill-banner">
        <span>{{ drillTip }}</span>
        <button class="btn-back" @click="backToDashboard">返回看板</button>
        <button class="btn-clear-drill" @click="clearDrillFilters">清除看板筛选</button>
      </div>
      <div class="filter-row">
        <input type="date" v-model="filters.startDate" placeholder="开始日期" />
        <span class="separator">至</span>
        <input type="date" v-model="filters.endDate" placeholder="结束日期" />
        <select v-model="filters.areaId" @change="handleAreaSelectChange">
          <option value="">全部区域</option>
          <option v-for="area in areaOptions" :key="area.id" :value="area.id">{{ area.name }}</option>
        </select>
        <select v-model="filters.equipmentType">
          <option value="">全部设备类型</option>
          <option v-for="type in typeOptions" :key="type" :value="type">{{ type }}</option>
        </select>
        <select v-model="filters.status">
          <option value="">全部状态</option>
          <option :value="0">待处理</option>
          <option :value="1">维修中</option>
          <option :value="2">已恢复</option>
        </select>
        <select v-model="filters.trialStatus">
          <option value="">试机情况：全部</option>
          <option :value="1">已试机</option>
          <option :value="0">未试机</option>
        </select>
        <button class="btn-filter" @click="applyFilters">筛选</button>
        <button class="btn-reset" @click="handleReset">重置</button>
      </div>
      <p v-if="filterError" class="repair-error">{{ filterError }}</p>
    </div>

    <div v-if="loadError" class="repair-banner error">
      <span>{{ loadError }}</span>
      <button class="btn-retry" :disabled="loading" @click="loadRepairs">
        {{ loading ? '加载中...' : '重新加载' }}
      </button>
    </div>

    <div class="overdue-panel">
      <div class="overdue-header">
        <h4>超时催办</h4>
        <span class="overdue-tip">按报修单记录区域（含下级）统计超过约定等待小时仍停在待处理的报修单</span>
      </div>
      <div class="overdue-controls">
        <label class="overdue-label">区域</label>
        <select v-model="overdueAreaId">
          <option value="">全部区域</option>
          <option v-for="area in areaOptions" :key="area.id" :value="area.id">{{ area.name }}</option>
        </select>
        <label class="overdue-label">约定等待小时</label>
        <input type="text" v-model="overdueHours" class="overdue-hours-input" placeholder="如 24"
               @keyup.enter="loadOverdue" />
        <button class="btn-overdue-stat" :disabled="overdueLoading" @click="loadOverdue">
          {{ overdueLoading ? '统计中...' : '统计' }}
        </button>
        <template v-if="overdueResult">
          <span class="overdue-result-text">超时催办</span>
          <button class="overdue-count" :disabled="overdueResult.overdueCount === 0"
                  title="点击查看催办清单" @click="openUrgeList">
            {{ overdueResult.overdueCount }}
          </button>
          <span class="overdue-result-text">件</span>
          <span class="overdue-scope">
            （{{ overdueResult.areaName || '全部区域' }} · 约定 {{ overdueResult.waitHours }} 小时）
          </span>
          <span v-if="overdueStale" class="overdue-stale">统计条件已修改，请重新统计</span>
        </template>
      </div>
      <p v-if="overdueHoursError" class="overdue-error">{{ overdueHoursError }}</p>
      <div v-if="overdueError" class="overdue-banner error">
        <span>{{ overdueError }}</span>
        <button class="btn-overdue-retry" :disabled="overdueLoading" @click="loadOverdue">重试</button>
      </div>
      <div v-else-if="overdueResult && overdueResult.overdueCount === 0" class="overdue-banner empty">
        当前条件下没有超时待处理的报修单，无需催办
      </div>
    </div>

    <table class="repair-table">
      <thead>
        <tr>
          <th>报修单号</th>
          <th>来源单号</th>
          <th>设备编号</th>
          <th>设备名称</th>
          <th>区域</th>
          <th>故障描述</th>
          <th>报修时间</th>
          <th>状态</th>
          <th>维修人</th>
          <th>复用前试机</th>
          <th>恢复时间</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="order in repairList" :key="order.id">
          <td>{{ order.repairNo }}</td>
          <td>
            <span v-if="order.inspectionNo">巡检 {{ order.inspectionNo }}</span>
            <span v-else-if="order.spotCheckNo" class="spotcheck-source">抽检 {{ order.spotCheckNo }}</span>
            <span v-else>-</span>
          </td>
          <td>{{ order.equipmentNo }}</td>
          <td>{{ order.equipmentName }}</td>
          <td>{{ order.areaName || '-' }}</td>
          <td class="desc-cell" :title="order.faultDesc">{{ order.faultDesc || '-' }}</td>
          <td>{{ formatTime(order.createdAt) }}</td>
          <td>
            <span :class="['status-tag', statusClass(order.status)]">{{ statusText(order.status) }}</span>
          </td>
          <td>{{ order.repairman || '-' }}</td>
          <td>
            <span v-if="order.trialDone" class="trial-tag done" :title="order.trialResult">已试机</span>
            <span v-else class="trial-tag pending">未试机</span>
          </td>
          <td>{{ order.finishTime ? formatTime(order.finishTime) : '-' }}</td>
          <td>
            <button class="btn-detail" @click="handleDetail(order)">详情</button>
            <button v-if="order.status === 0" class="btn-start" @click="openTransition(order, 1)">开始维修</button>
            <button v-if="order.status === 1" class="btn-finish" @click="openTransition(order, 2)">确认恢复</button>
          </td>
        </tr>
        <tr v-if="repairList.length === 0">
          <td colspan="12" class="empty">
            {{ loading ? '加载中...' : (loadFinished ? '当前筛选条件下没有报修单，请调整筛选条件后重试' : '暂无报修单') }}
          </td>
        </tr>
      </tbody>
    </table>

    <div class="modal-overlay" v-if="transitionVisible" @click="transitionVisible = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>{{ transitionTarget === 1 ? '开始维修' : '确认恢复' }}</h3>
          <button class="close-btn" @click="transitionVisible = false">×</button>
        </div>
        <div class="modal-body" v-if="currentOrder">
          <div class="order-info">
            <span class="label">报修单号:</span><span>{{ currentOrder.repairNo }}</span>
          </div>
          <div class="order-info">
            <span class="label">设备:</span><span>{{ currentOrder.equipmentName }}（{{ currentOrder.equipmentNo }}）</span>
          </div>
          <div class="order-info">
            <span class="label">故障描述:</span><span>{{ currentOrder.faultDesc || '-' }}</span>
          </div>
          <p class="transition-tip" v-if="transitionTarget === 1">开始维修后设备进入维修中状态，期间不可调配。</p>
          <p class="transition-tip" v-else>确认恢复即结单，设备恢复可调配。结单前必须先做复用前试机并填写试机结论，未试机不能结单。</p>

          <div class="form-group">
            <label>维修人:</label>
            <input type="text" v-model="transitionForm.repairman" placeholder="请输入维修人姓名" />
          </div>
          <div class="form-group" v-if="transitionTarget === 2">
            <label>维修说明:</label>
            <textarea v-model="transitionForm.repairNote" rows="3" placeholder="请输入维修处理说明"></textarea>
          </div>
          <div class="form-group" v-if="transitionTarget === 2">
            <label class="required-label">复用前试机结论:</label>
            <textarea v-model="transitionForm.trialResult" rows="3" maxlength="500"
                      placeholder="必填：请填写复用前试机情况与结论，如通电试运行结果、功能是否正常、能否恢复复用"></textarea>
            <p class="field-hint">未填写试机结论不能结单，结论将随恢复时间一并记入报修单时间线</p>
          </div>
          <p v-if="transitionError" class="overdue-error">{{ transitionError }}</p>

          <div class="form-buttons">
            <button type="button" class="btn-cancel" @click="transitionVisible = false">取消</button>
            <button type="button" class="btn-submit" :disabled="submitting" @click="handleTransition">
              {{ submitting ? '提交中...' : '确认' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <div class="modal-overlay" v-if="detailVisible" @click="detailVisible = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>报修单详情</h3>
          <button class="close-btn" @click="detailVisible = false">×</button>
        </div>
        <div class="modal-body" v-if="detail">
          <div class="detail-section">
            <h4>报修信息</h4>
            <div class="info-grid">
              <div class="info-item"><span class="label">报修单号:</span><span>{{ detail.repairNo }}</span></div>
              <div class="info-item">
                <span class="label">来源单号:</span>
                <span v-if="detail.inspectionNo">巡检 {{ detail.inspectionNo }}</span>
                <span v-else-if="detail.spotCheckNo">抽检 {{ detail.spotCheckNo }}</span>
                <span v-else>-</span>
              </div>
              <div class="info-item"><span class="label">设备:</span><span>{{ detail.equipmentName }}（{{ detail.equipmentNo }}）</span></div>
              <div class="info-item"><span class="label">区域:</span><span>{{ detail.areaName || '-' }}</span></div>
              <div class="info-item">
                <span class="label">状态:</span>
                <span :class="['status-tag', statusClass(detail.status)]">{{ statusText(detail.status) }}</span>
              </div>
              <div class="info-item"><span class="label">报修人:</span><span>{{ detail.reporter || '-' }}</span></div>
              <div class="info-item full"><span class="label">故障描述:</span><span>{{ detail.faultDesc || '-' }}</span></div>
              <div class="info-item full" v-if="detail.repairNote"><span class="label">维修说明:</span><span>{{ detail.repairNote }}</span></div>
              <div class="info-item full" v-if="detail.trialDone">
                <span class="label">试机结论:</span>
                <span>{{ detail.trialResult }}<template v-if="detail.trialTime">（{{ formatTime(detail.trialTime) }}）</template></span>
              </div>
              <div class="info-item full" v-else-if="detail.status === 1">
                <span class="label">试机结论:</span><span class="trial-missing">尚未试机，恢复结单前必须补填复用前试机结论</span>
              </div>
              <div class="info-item full" v-if="detail.urgeNote">
                <span class="label">最近催办:</span><span>{{ detail.urgeNote }}（{{ formatTime(detail.urgeTime) }}）</span>
              </div>
            </div>
            <div class="photo-preview" v-if="detail.photoUrl">
              <span class="label">故障照片:</span>
              <img :src="detail.photoUrl" alt="故障照片" @error="photoError = true" v-show="!photoError" />
              <a v-if="photoError" :href="detail.photoUrl" target="_blank">{{ detail.photoUrl }}</a>
            </div>
          </div>

          <div class="detail-section">
            <h4>处理时间线</h4>
            <div class="timeline">
              <div v-for="(item, index) in detail.timeline" :key="index" class="timeline-item">
                <div :class="['timeline-dot', item.type]"></div>
                <div class="timeline-content">
                  <div class="timeline-title">{{ item.title }}</div>
                  <div class="timeline-desc">{{ item.description }}</div>
                  <div class="timeline-time">{{ formatTime(item.time) }}</div>
                </div>
              </div>
              <p v-if="!detail.timeline || detail.timeline.length === 0" class="empty">暂无时间线记录</p>
            </div>
          </div>
        </div>
      </div>
    </div>
    <div class="modal-overlay" v-if="urgeListVisible" @click="urgeListVisible = false">
      <div class="modal-content urge-list-modal" @click.stop>
        <div class="modal-header">
          <h3>超时催办清单</h3>
          <button class="close-btn" @click="urgeListVisible = false">×</button>
        </div>
        <div class="modal-body" v-if="overdueResult">
          <div class="urge-list-summary">
            <span>
              {{ overdueResult.areaName || '全部区域' }} · 约定等待 {{ overdueResult.waitHours }} 小时 ·
              仅显示已超时的待处理单，共 {{ overdueResult.overdueCount }} 件
            </span>
            <button class="btn-urge-refresh" :disabled="overdueLoading" @click="loadOverdue">
              {{ overdueLoading ? '刷新中...' : '刷新' }}
            </button>
          </div>
          <p v-if="urgeListTip" class="urge-list-tip">{{ urgeListTip }}</p>
          <table class="urge-table">
            <thead>
              <tr>
                <th>报修单号</th>
                <th>设备</th>
                <th>区域</th>
                <th>报修时间</th>
                <th>已等待</th>
                <th>跟进人</th>
                <th>最近催办说明</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="order in overdueResult.orders" :key="order.id">
                <td>{{ order.repairNo }}</td>
                <td>{{ order.equipmentName }}（{{ order.equipmentNo }}）</td>
                <td>{{ order.areaName || '-' }}</td>
                <td>{{ formatTime(order.createdAt) }}</td>
                <td><span class="waited-tag">{{ order.waitedHours }} 小时</span></td>
                <td>{{ order.repairman || '-' }}</td>
                <td class="desc-cell" :title="order.urgeNote">{{ order.urgeNote || '-' }}</td>
                <td><button class="btn-urge" @click="openUrgeForm(order)">催办</button></td>
              </tr>
              <tr v-if="overdueResult.orders.length === 0">
                <td colspan="8" class="empty">没有超时待处理的报修单</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <div class="modal-overlay" v-if="urgeFormVisible" @click="urgeFormVisible = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>催办跟进</h3>
          <button class="close-btn" @click="urgeFormVisible = false">×</button>
        </div>
        <div class="modal-body" v-if="urgeOrder">
          <div class="order-info">
            <span class="label">报修单号:</span><span>{{ urgeOrder.repairNo }}</span>
          </div>
          <div class="order-info">
            <span class="label">设备:</span><span>{{ urgeOrder.equipmentName }}（{{ urgeOrder.equipmentNo }}）</span>
          </div>
          <div class="order-info">
            <span class="label">已等待:</span>
            <span>{{ urgeOrder.waitedHours }} 小时（约定 {{ overdueResult ? overdueResult.waitHours : '-' }} 小时）</span>
          </div>
          <p class="transition-tip">催办不改变报修单状态，仅更新跟进人并留下催办说明。</p>
          <div class="form-group">
            <label>跟进人:</label>
            <input type="text" v-model="urgeForm.followUpPerson" placeholder="留空则不修改原跟进人" />
          </div>
          <div class="form-group">
            <label>催办说明:</label>
            <textarea v-model="urgeForm.urgeNote" rows="3" placeholder="请填写催办说明（必填）"></textarea>
          </div>
          <p v-if="urgeFormError" class="overdue-error">{{ urgeFormError }}</p>
          <div class="form-buttons">
            <button type="button" class="btn-cancel" @click="urgeFormVisible = false">取消</button>
            <button type="button" class="btn-submit" :disabled="urging" @click="handleUrge">
              {{ urging ? '提交中...' : '确认催办' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { repairApi, areaApi, equipmentApi } from '../api'

const props = defineProps({
  initialFilters: {
    type: Object,
    default: () => ({})
  }
})
const emit = defineEmits(['back-dashboard'])

const repairList = ref([])
const areaOptions = ref([])
const typeOptions = ref([])
const loading = ref(false)
const loadError = ref('')
const loadFinished = ref(false)
const filterError = ref('')
const transitionVisible = ref(false)
const transitionTarget = ref(1)
const currentOrder = ref(null)
const transitionForm = ref({ repairman: '', repairNote: '', trialResult: '' })
const transitionError = ref('')
const submitting = ref(false)
const detailVisible = ref(false)
const detail = ref(null)
const photoError = ref(false)

// 超时催办：约定等待小时 + 区域 → 件数 → 催办清单
const overdueAreaId = ref('')
const overdueHours = ref('24')
const overdueResult = ref(null)
const overdueLoading = ref(false)
const overdueError = ref('')
const overdueHoursError = ref('')
const overdueStale = ref(false)
const urgeListVisible = ref(false)
const urgeListTip = ref('')
const urgeFormVisible = ref(false)
const urgeOrder = ref(null)
const urgeForm = ref({ followUpPerson: '', urgeNote: '' })
const urgeFormError = ref('')
const urging = ref(false)

const FILTER_STORAGE_KEY = 'repair.list.filters'

const defaultFilters = () => ({
  startDate: '',
  endDate: '',
  areaId: '',
  equipmentType: '',
  status: '',
  trialStatus: '',
  equipmentCurrentAreaId: null
})

const loadSavedFilters = () => {
  try {
    const saved = localStorage.getItem(FILTER_STORAGE_KEY)
    if (saved) {
      return { ...defaultFilters(), ...JSON.parse(saved) }
    }
  } catch (error) {
    console.warn('读取本地报修单筛选条件失败:', error)
  }
  return defaultFilters()
}

const persistFilters = () => {
  try {
    // 看板下钻的临时口径不落本地，避免刷新后残留与当前视图不符的筛选
    const toSave = { ...filters.value, equipmentCurrentAreaId: null }
    localStorage.setItem(FILTER_STORAGE_KEY, JSON.stringify(toSave))
  } catch (error) {
    console.warn('保存报修单筛选条件失败:', error)
  }
}

const filters = ref(defaultFilters())

const drillTip = computed(() => {
  if (!props.initialFilters || Object.keys(props.initialFilters).length === 0) return ''
  const parts = []
  if (filters.value.equipmentCurrentAreaId) {
    const area = areaOptions.value.find(a => a.id === filters.value.equipmentCurrentAreaId)
    if (area) parts.push(`设备当前所在区域「${area.name}」（含下级区域）`)
  } else if (filters.value.areaId !== '') {
    const area = areaOptions.value.find(a => a.id === filters.value.areaId)
    if (area) parts.push(`区域「${area.name}」（按报修单记录区域）`)
  }
  if (filters.value.equipmentType) parts.push(`类型「${filters.value.equipmentType}」`)
  if (filters.value.status === 0) parts.push('状态：待处理（当前状态，不含周期条件）')
  if (filters.value.status === 1) parts.push('状态：维修中（当前状态，不含周期条件）')
  if (filters.value.startDate && filters.value.endDate) {
    parts.push(`报修时间 ${filters.value.startDate} 至 ${filters.value.endDate}`)
  }
  return parts.length > 0 ? `来自健康看板的筛选：${parts.join('，')}` : ''
})

const validateDateRange = () => {
  if (filters.value.startDate && filters.value.endDate
      && filters.value.startDate > filters.value.endDate) {
    filterError.value = '报修开始日期不能晚于结束日期'
    return false
  }
  filterError.value = ''
  return true
}

const loadRepairs = async () => {
  if (!validateDateRange()) {
    repairList.value = []
    return
  }
  loading.value = true
  loadError.value = ''
  loadFinished.value = false
  try {
    const params = {}
    if (filters.value.startDate) params.startDate = filters.value.startDate
    if (filters.value.endDate) params.endDate = filters.value.endDate
    if (filters.value.equipmentCurrentAreaId) {
      params.equipmentCurrentAreaId = filters.value.equipmentCurrentAreaId
    } else if (filters.value.areaId !== '') {
      params.areaId = filters.value.areaId
    }
    if (filters.value.equipmentType !== '') params.equipmentType = filters.value.equipmentType
    if (filters.value.status !== '') params.status = filters.value.status
    // 是否已试机由服务端按落库的试机结论统一计算，刷新后口径一致
    if (filters.value.trialStatus !== '') params.trialStatus = filters.value.trialStatus

    const res = await repairApi.getRepairs(params)
    if (res.data.code === 200) {
      repairList.value = res.data.data
    } else {
      loadError.value = res.data.message || '加载报修单失败，请稍后重试'
    }
  } catch (error) {
    loadError.value = error.response?.data?.message
      || (error.code === 'ECONNABORTED' ? '接口请求超时，请稍后重试' : '接口请求失败，报修单加载失败，请检查网络或稍后重试')
    console.error('加载报修单失败:', error)
  } finally {
    loading.value = false
    loadFinished.value = true
  }
}

const applyFilters = () => {
  persistFilters()
  loadRepairs()
}

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

const loadTypes = async () => {
  try {
    const res = await equipmentApi.getEquipmentTypes()
    if (res.data.code === 200) {
      typeOptions.value = res.data.data
    }
  } catch (error) {
    console.error('加载设备类型失败:', error)
  }
}

const applyInitialFilters = () => {
  const init = props.initialFilters || {}
  if (Object.keys(init).length > 0) {
    // 看板下钻：只使用看板条件，不叠加本地筛选，保证明细数与看板卡片一致
    filters.value = {
      ...defaultFilters(),
      ...init,
      areaId: init.equipmentCurrentAreaId ? '' : (init.areaId ?? '')
    }
    return
  }
  // 普通进入/刷新：恢复本地保存的筛选（含是否已试机），刷新后仍能看出
  filters.value = loadSavedFilters()
}

const handleReset = () => {
  filters.value = defaultFilters()
  persistFilters()
  loadRepairs()
}

const clearDrillFilters = () => {
  filters.value = defaultFilters()
  persistFilters()
  loadRepairs()
}

const handleAreaSelectChange = () => {
  // 用户手动改区域下拉后，解除看板锁定的「设备当前区域」口径
  filters.value.equipmentCurrentAreaId = null
}

const backToDashboard = () => {
  emit('back-dashboard')
}

// 校验约定等待小时数，填错时给出提示并阻止请求
const validateOverdueHours = () => {
  overdueHoursError.value = ''
  const raw = String(overdueHours.value).trim()
  if (!raw) {
    overdueHoursError.value = '请填写约定等待小时数'
    return null
  }
  if (!/^\d+$/.test(raw)) {
    overdueHoursError.value = '约定等待小时数需为大于0的整数'
    return null
  }
  const hours = Number(raw)
  if (hours <= 0) {
    overdueHoursError.value = '约定等待小时数必须大于0'
    return null
  }
  if (hours > 8760) {
    overdueHoursError.value = '约定等待小时数不能超过8760（约一年）'
    return null
  }
  return hours
}

// 统计超时催办：件数与清单来自同一响应，刷新后两者始终一致
const loadOverdue = async () => {
  const hours = validateOverdueHours()
  if (hours === null) return
  overdueLoading.value = true
  overdueError.value = ''
  try {
    const params = { waitHours: hours }
    if (overdueAreaId.value !== '') params.areaId = overdueAreaId.value
    const res = await repairApi.getOverdueRepairs(params)
    if (res.data.code === 200) {
      overdueResult.value = res.data.data
      overdueStale.value = false
    } else {
      overdueError.value = res.data.message || '超时催办统计失败，请稍后重试'
    }
  } catch (error) {
    overdueError.value = error.response?.data?.message
      || (error.code === 'ECONNABORTED' ? '接口请求超时，请稍后重试' : '超时催办统计请求失败，请检查网络或稍后重试')
  } finally {
    overdueLoading.value = false
  }
}

// 改小时或区域后提示需重新统计，避免旧结果被误读
watch([overdueHours, overdueAreaId], () => {
  if (overdueResult.value) overdueStale.value = true
})

const openUrgeList = () => {
  if (!overdueResult.value || overdueResult.value.overdueCount === 0) return
  urgeListTip.value = ''
  urgeListVisible.value = true
}

const openUrgeForm = (order) => {
  urgeOrder.value = order
  urgeForm.value = { followUpPerson: order.repairman || '', urgeNote: '' }
  urgeFormError.value = ''
  urgeFormVisible.value = true
}

const handleUrge = async () => {
  if (urging.value || !urgeOrder.value) return
  if (!urgeForm.value.urgeNote || !urgeForm.value.urgeNote.trim()) {
    urgeFormError.value = '请填写催办说明'
    return
  }
  urging.value = true
  urgeFormError.value = ''
  try {
    const res = await repairApi.urgeRepair(urgeOrder.value.id, {
      followUpPerson: urgeForm.value.followUpPerson,
      urgeNote: urgeForm.value.urgeNote.trim()
    })
    if (res.data.code === 200) {
      urgeFormVisible.value = false
      urgeListTip.value = res.data.message || '催办成功'
      loadRepairs()
      // 件数与清单一同刷新，保持一致
      await loadOverdue()
    } else {
      urgeFormError.value = res.data.message || '催办失败，请稍后重试'
    }
  } catch (error) {
    urgeFormError.value = error.response?.data?.message || '催办请求失败，请稍后重试'
  } finally {
    urging.value = false
  }
}

// 状态流转后同步刷新超时统计（已开始维修的单不再属于超时待处理）
const refreshOverdueIfLoaded = () => {
  if (overdueResult.value) loadOverdue()
}

const openTransition = (order, target) => {
  currentOrder.value = order
  transitionTarget.value = target
  transitionForm.value = { repairman: order.repairman || '', repairNote: '', trialResult: '' }
  transitionError.value = ''
  transitionVisible.value = true
}

const handleTransition = async () => {
  if (submitting.value || !currentOrder.value) return
  // 结单恢复前必须填写复用前试机结论，未试机不能结单
  if (transitionTarget.value === 2
      && (!transitionForm.value.trialResult || !transitionForm.value.trialResult.trim())) {
    transitionError.value = '请先试机并填写复用前试机结论，未试机不能结单恢复'
    return
  }
  transitionError.value = ''
  submitting.value = true

  try {
    const payload = {
      status: transitionTarget.value,
      repairman: transitionForm.value.repairman,
      repairNote: transitionForm.value.repairNote
    }
    if (transitionTarget.value === 2) {
      payload.trialResult = transitionForm.value.trialResult.trim()
    }
    const res = await repairApi.updateRepairStatus(currentOrder.value.id, payload)
    if (res.data.code === 200) {
      transitionVisible.value = false
      persistFilters()
      loadRepairs()
      refreshOverdueIfLoaded()
    } else {
      transitionError.value = res.data.message || '状态更新失败，请稍后重试'
    }
  } catch (error) {
    transitionError.value = error.response?.data?.message
      || (error.code === 'ECONNABORTED' ? '接口请求超时，请稍后重试' : '状态更新请求失败，请检查网络或稍后重试')
  } finally {
    submitting.value = false
  }
}

const handleDetail = async (order) => {
  photoError.value = false
  try {
    const res = await repairApi.getRepairById(order.id)
    if (res.data.code === 200) {
      detail.value = res.data.data
      detailVisible.value = true
    } else {
      alert(res.data.message || '加载报修单详情失败，请稍后重试')
    }
  } catch (error) {
    alert(error.response?.data?.message
      || (error.code === 'ECONNABORTED' ? '接口请求超时，请稍后重试' : '加载报修单详情失败，请检查网络或稍后重试'))
    console.error('加载报修单详情失败:', error)
  }
}

const statusText = (status) => {
  if (status === 0) return '待处理'
  if (status === 1) return '维修中'
  if (status === 2) return '已恢复'
  return '-'
}

const statusClass = (status) => {
  if (status === 0) return 'pending'
  if (status === 1) return 'repairing'
  if (status === 2) return 'restored'
  return ''
}

const formatTime = (time) => {
  if (!time) return '-'
  return time.replace('T', ' ').substring(0, 19)
}

onMounted(() => {
  applyInitialFilters()
  loadAreas()
  loadTypes()
  loadRepairs()
})
</script>

<style scoped>
.repair-list {
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
}

.filter-section {
  margin-bottom: 16px;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 4px;
}

.drill-banner {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 10px;
  padding: 8px 12px;
  background: #fdf6ec;
  border: 1px solid #f5dab1;
  border-radius: 4px;
  color: #e6a23c;
  font-size: 13px;
}

.btn-back {
  padding: 3px 12px;
  border: none;
  border-radius: 4px;
  background: #409eff;
  color: #fff;
  cursor: pointer;
  font-size: 12px;
}

.btn-clear-drill {
  padding: 3px 12px;
  border: 1px solid #e6a23c;
  border-radius: 4px;
  background: #fff;
  color: #e6a23c;
  cursor: pointer;
  font-size: 12px;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.filter-row input,
.filter-row select {
  padding: 6px 10px;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  font-size: 14px;
  background: #fff;
}

.separator {
  color: #999;
}

.btn-filter {
  padding: 6px 16px;
  border: none;
  border-radius: 4px;
  background: #409eff;
  color: #fff;
  cursor: pointer;
  font-size: 14px;
}

.btn-reset {
  padding: 6px 16px;
  border: none;
  border-radius: 4px;
  background: #f0f0f0;
  color: #666;
  cursor: pointer;
  font-size: 14px;
}

.repair-table {
  width: 100%;
  border-collapse: collapse;
}

.repair-table th,
.repair-table td {
  padding: 12px 8px;
  text-align: left;
  border-bottom: 1px solid #e0e0e0;
  font-size: 14px;
}

.repair-table th {
  background: #f8f9fa;
  color: #666;
  font-weight: bold;
}

.desc-cell {
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status-tag {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.status-tag.pending {
  background: #fdf6ec;
  color: #e6a23c;
}

.status-tag.repairing {
  background: #ecf5ff;
  color: #409eff;
}

.status-tag.restored {
  background: #f0f9eb;
  color: #67c23a;
}

.repair-table td button {
  padding: 4px 10px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  margin-right: 4px;
}

.btn-detail {
  background: #409eff;
  color: #fff;
}

.btn-start {
  background: #e6a23c;
  color: #fff;
}

.btn-finish {
  background: #67c23a;
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
  max-width: 560px;
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

.order-info {
  display: flex;
  margin-bottom: 8px;
  font-size: 14px;
}

.order-info .label {
  color: #999;
  width: 80px;
  flex-shrink: 0;
}

.transition-tip {
  padding: 8px 12px;
  background: #fdf6ec;
  border-radius: 4px;
  color: #e6a23c;
  font-size: 13px;
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

.detail-section {
  margin-bottom: 20px;
}

.detail-section h4 {
  margin: 0 0 12px 0;
  font-size: 15px;
  color: #333;
  border-left: 3px solid #409eff;
  padding-left: 8px;
}

.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px 16px;
}

.info-item {
  display: flex;
  font-size: 14px;
}

.info-item.full {
  grid-column: 1 / -1;
}

.info-item .label {
  color: #999;
  min-width: 70px;
  flex-shrink: 0;
}

.photo-preview {
  margin-top: 12px;
}

.photo-preview .label {
  display: block;
  color: #999;
  font-size: 14px;
  margin-bottom: 6px;
}

.photo-preview img {
  max-width: 100%;
  max-height: 200px;
  border-radius: 4px;
  border: 1px solid #e0e0e0;
}

.timeline {
  position: relative;
  padding-left: 20px;
}

.timeline::before {
  content: '';
  position: absolute;
  left: 5px;
  top: 8px;
  bottom: 8px;
  width: 2px;
  background: #e0e0e0;
}

.timeline-item {
  position: relative;
  padding-bottom: 16px;
}

.timeline-dot {
  position: absolute;
  left: -19px;
  top: 4px;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #409eff;
  border: 2px solid #fff;
}

.timeline-dot.inspection {
  background: #e6a23c;
}

.timeline-dot.repair {
  background: #409eff;
}

.timeline-dot.urge {
  background: #f56c6c;
}

.overdue-panel {
  margin-bottom: 16px;
  padding: 12px;
  background: #fdf6ec;
  border: 1px solid #f5dab1;
  border-radius: 4px;
}

.overdue-header {
  display: flex;
  align-items: baseline;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}

.overdue-header h4 {
  margin: 0;
  font-size: 15px;
  color: #333;
  border-left: 3px solid #e6a23c;
  padding-left: 8px;
}

.overdue-tip {
  color: #c0c4cc;
  font-size: 12px;
}

.overdue-controls {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.overdue-label {
  color: #666;
  font-size: 14px;
}

.overdue-controls select,
.overdue-controls input {
  padding: 6px 10px;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  font-size: 14px;
  background: #fff;
}

.overdue-hours-input {
  width: 90px;
}

.btn-overdue-stat {
  padding: 6px 16px;
  border: none;
  border-radius: 4px;
  background: #e6a23c;
  color: #fff;
  cursor: pointer;
  font-size: 14px;
}

.btn-overdue-stat:disabled {
  background: #f3d19e;
  cursor: not-allowed;
}

.overdue-result-text {
  color: #666;
  font-size: 14px;
}

.overdue-count {
  border: none;
  background: none;
  color: #f56c6c;
  font-size: 20px;
  font-weight: bold;
  cursor: pointer;
  padding: 0 2px;
  text-decoration: underline;
}

.overdue-count:disabled {
  color: #c0c4cc;
  cursor: default;
  text-decoration: none;
}

.overdue-scope {
  color: #909399;
  font-size: 13px;
}

.overdue-stale {
  color: #e6a23c;
  font-size: 12px;
}

.overdue-error {
  margin: 8px 0 0;
  color: #f56c6c;
  font-size: 13px;
}

.overdue-banner {
  margin-top: 10px;
  padding: 8px 12px;
  border-radius: 4px;
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.overdue-banner.error {
  background: #fef0f0;
  color: #f56c6c;
  border: 1px solid #fbc4c4;
}

.overdue-banner.empty {
  background: #f0f9eb;
  color: #67c23a;
  border: 1px solid #c2e7b0;
}

.btn-overdue-retry {
  padding: 3px 12px;
  border: none;
  border-radius: 4px;
  background: #f56c6c;
  color: #fff;
  cursor: pointer;
  font-size: 12px;
  white-space: nowrap;
}

.urge-list-modal {
  max-width: 960px;
}

.urge-list-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 10px;
  color: #666;
  font-size: 13px;
}

.btn-urge-refresh {
  padding: 4px 14px;
  border: none;
  border-radius: 4px;
  background: #409eff;
  color: #fff;
  cursor: pointer;
  font-size: 13px;
}

.btn-urge-refresh:disabled {
  background: #a0cfff;
  cursor: not-allowed;
}

.urge-list-tip {
  margin: 0 0 10px;
  padding: 6px 10px;
  background: #f0f9eb;
  color: #67c23a;
  border-radius: 4px;
  font-size: 13px;
}

.urge-table {
  width: 100%;
  border-collapse: collapse;
}

.urge-table th,
.urge-table td {
  padding: 10px 8px;
  text-align: left;
  border-bottom: 1px solid #f0f0f0;
  font-size: 13px;
}

.urge-table th {
  background: #f8f9fa;
  color: #666;
  font-weight: bold;
}

.waited-tag {
  padding: 2px 8px;
  border-radius: 4px;
  background: #fef0f0;
  color: #f56c6c;
  font-size: 12px;
  white-space: nowrap;
}

.btn-urge {
  padding: 4px 10px;
  border: none;
  border-radius: 4px;
  background: #f56c6c;
  color: #fff;
  cursor: pointer;
  font-size: 12px;
}

.timeline-title {
  font-size: 14px;
  font-weight: bold;
  color: #333;
}

.timeline-desc {
  font-size: 13px;
  color: #666;
  margin-top: 2px;
}

.timeline-time {
  font-size: 12px;
  color: #999;
  margin-top: 2px;
}

.timeline-dot.trial {
  background: #67c23a;
}

.trial-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  white-space: nowrap;
}

.trial-tag.done {
  background: #f0f9eb;
  color: #67c23a;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.trial-tag.pending {
  background: #fef0f0;
  color: #f56c6c;
}

.trial-missing {
  color: #f56c6c;
}

.required-label::before {
  content: '*';
  color: #f56c6c;
  margin-right: 4px;
}

.field-hint {
  margin: 4px 0 0;
  color: #e6a23c;
  font-size: 12px;
}

.repair-error {
  margin: 8px 0 0;
  color: #f56c6c;
  font-size: 13px;
}

.repair-banner {
  margin-bottom: 16px;
  padding: 10px 16px;
  border-radius: 4px;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.repair-banner.error {
  background: #fef0f0;
  color: #f56c6c;
  border: 1px solid #fbc4c4;
}

.repair-banner.empty {
  background: #f4f4f5;
  color: #909399;
  border: 1px solid #e9e9eb;
  justify-content: center;
}

.btn-retry {
  padding: 4px 14px;
  border: none;
  border-radius: 4px;
  background: #f56c6c;
  color: #fff;
  cursor: pointer;
  font-size: 13px;
  white-space: nowrap;
}

.btn-retry:disabled {
  background: #fab6b6;
  cursor: not-allowed;
}
</style>
