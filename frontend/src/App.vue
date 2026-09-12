
<script setup>
import { ref, onMounted, nextTick } from 'vue'
import AreaTree from './components/AreaTree.vue'
import EquipmentList from './components/EquipmentList.vue'
import EquipmentForm from './components/EquipmentForm.vue'
import TransferForm from './components/TransferForm.vue'
import TransferList from './components/TransferList.vue'
import TransferReceiptList from './components/TransferReceiptList.vue'
import InspectionPlanList from './components/InspectionPlanList.vue'
import InspectionList from './components/InspectionList.vue'
import SpotCheckList from './components/SpotCheckList.vue'
import RepairList from './components/RepairList.vue'
import HealthDashboard from './components/HealthDashboard.vue'
import { equipmentApi } from './api'

const activeTab = ref('equipment')
const equipmentList = ref([])
const selectedArea = ref(null)
const transferFormVisible = ref(false)
const transferEquipment = ref(null)
const equipmentFormVisible = ref(false)
const editEquipment = ref(null)

// 看板下钻时带入明细页的初始筛选条件
const drillFilters = ref({
  inspection: { key: 0, value: {} },
  repair: { key: 0, value: {} },
  transfer: { key: 0, value: {} }
})
const equipmentInitialAreaId = ref(null)
const equipmentInitialType = ref('')

const loadEquipments = async (areaId = null) => {
  try {
    let res
    if (areaId) {
      res = await equipmentApi.getEquipmentsByArea(areaId)
    } else {
      res = await equipmentApi.getAllEquipments()
    }
    if (res.data.code === 200) {
      let list = res.data.data
      if (equipmentInitialType.value) {
        list = list.filter(e => e.equipmentType === equipmentInitialType.value)
      }
      equipmentList.value = list
    }
  } catch (error) {
    console.error('加载设备列表失败:', error)
  }
}

const handleAreaSelected = (area) => {
  selectedArea.value = area
  equipmentInitialType.value = ''
  loadEquipments(area.id)
}

const handleTransfer = (equipment) => {
  transferEquipment.value = equipment
  transferFormVisible.value = true
}

const handleEdit = (equipment) => {
  editEquipment.value = equipment
  equipmentFormVisible.value = true
}

const handleAddEquipment = () => {
  editEquipment.value = null
  equipmentFormVisible.value = true
}

const handleTransferSuccess = () => {
  loadEquipments(selectedArea.value?.id)
}

const handleEquipmentSuccess = () => {
  loadEquipments(selectedArea.value?.id)
}

const switchTab = (tab) => {
  activeTab.value = tab
  if (tab === 'equipment') {
    loadEquipments(selectedArea.value?.id)
  }
}

const handleDrilldown = ({ target, params }) => {
  if (target === 'equipment') {
    equipmentInitialAreaId.value = params.areaId || null
    equipmentInitialType.value = params.equipmentType || ''
    selectedArea.value = params.areaId ? { id: params.areaId } : null
    activeTab.value = 'equipment'
    nextTick(() => loadEquipments(params.areaId || null))
    return
  }

  if (target === 'inspection' || target === 'repair' || target === 'transfer') {
    drillFilters.value[target] = {
      key: drillFilters.value[target].key + 1,
      value: params
    }
    activeTab.value = target
  }
}

onMounted(() => {
  loadEquipments()
})
</script>

<template>
  <div class="app-container">
    <header class="app-header">
      <h1>大型超市母婴室配套设备管理系统</h1>
      <div class="header-nav">
        <button :class="['nav-btn', { active: activeTab === 'dashboard' }]" @click="switchTab('dashboard')">健康看板</button>
        <button :class="['nav-btn', { active: activeTab === 'equipment' }]" @click="switchTab('equipment')">设备管理</button>
        <button :class="['nav-btn', { active: activeTab === 'transfer' }]" @click="switchTab('transfer')">调配台账</button>
        <button :class="['nav-btn', { active: activeTab === 'receipt' }]" @click="switchTab('receipt')">到货签收</button>
        <button :class="['nav-btn', { active: activeTab === 'plan' }]" @click="switchTab('plan')">巡检计划</button>
        <button :class="['nav-btn', { active: activeTab === 'inspection' }]" @click="switchTab('inspection')">巡检记录</button>
        <button :class="['nav-btn', { active: activeTab === 'spotcheck' }]" @click="switchTab('spotcheck')">温奶器抽检</button>
        <button :class="['nav-btn', { active: activeTab === 'repair' }]" @click="switchTab('repair')">故障报修</button>
      </div>
    </header>

    <main class="app-main">
      <div v-if="activeTab === 'dashboard'" class="sub-page">
        <HealthDashboard @drilldown="handleDrilldown" />
      </div>

      <div v-else-if="activeTab === 'equipment'" class="equipment-page">
        <div class="sidebar">
          <AreaTree @area-selected="handleAreaSelected" />
        </div>
        <div class="content">
          <div class="content-header">
            <button class="add-btn" @click="handleAddEquipment">新建设备</button>
            <span v-if="equipmentInitialType" class="drill-tip">
              按设备类型「{{ equipmentInitialType }}」筛选 · <a @click="equipmentInitialType = ''; loadEquipments(selectedArea.value?.id)">清除</a>
            </span>
          </div>
          <EquipmentList :equipment-list="equipmentList" @edit="handleEdit" @transfer="handleTransfer" />
        </div>
      </div>

      <div v-else-if="activeTab === 'transfer'" class="transfer-page">
        <TransferList :key="`transfer-${drillFilters.transfer.key}`" :initial-filters="drillFilters.transfer.value" @back-dashboard="switchTab('dashboard')" />
      </div>

      <div v-else-if="activeTab === 'receipt'" class="sub-page">
        <TransferReceiptList />
      </div>

      <div v-else-if="activeTab === 'plan'" class="sub-page">
        <InspectionPlanList />
      </div>

      <div v-else-if="activeTab === 'inspection'" class="sub-page">
        <InspectionList :key="`inspection-${drillFilters.inspection.key}`" :initial-filters="drillFilters.inspection.value" @back-dashboard="switchTab('dashboard')" />
      </div>

      <div v-else-if="activeTab === 'spotcheck'" class="sub-page">
        <SpotCheckList />
      </div>

      <div v-else class="sub-page">
        <RepairList :key="`repair-${drillFilters.repair.key}`" :initial-filters="drillFilters.repair.value" @back-dashboard="switchTab('dashboard')" />
      </div>
    </main>

    <EquipmentForm
      :visible="equipmentFormVisible"
      :equipment="editEquipment"
      @close="equipmentFormVisible = false"
      @success="handleEquipmentSuccess"
    />

    <TransferForm
      :visible="transferFormVisible"
      :equipment="transferEquipment"
      @close="transferFormVisible = false"
      @success="handleTransferSuccess"
    />
  </div>
</template>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  background: #f5f5f5;
}

.app-container {
  min-height: 100vh;
}

.app-header {
  background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
  color: #fff;
  padding: 16px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.app-header h1 {
  font-size: 20px;
  font-weight: bold;
}

.header-nav {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.nav-btn {
  padding: 8px 20px;
  border: none;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}

.nav-btn:hover {
  background: rgba(255, 255, 255, 0.3);
}

.nav-btn.active {
  background: #fff;
  color: #409eff;
}

.app-main {
  padding: 24px;
  max-width: 1400px;
  margin: 0 auto;
}

.equipment-page {
  display: grid;
  grid-template-columns: 300px 1fr;
  gap: 24px;
}

.sidebar {
  position: sticky;
  top: 24px;
  height: fit-content;
}

.content {
  min-height: 400px;
}

.content-header {
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.drill-tip {
  font-size: 13px;
  color: #e6a23c;
}

.drill-tip a {
  color: #409eff;
  cursor: pointer;
  text-decoration: underline;
}

.add-btn {
  padding: 8px 20px;
  border: none;
  border-radius: 4px;
  background: #67c23a;
  color: #fff;
  cursor: pointer;
  font-size: 14px;
  transition: background 0.2s;
}

.add-btn:hover {
  background: #85ce61;
}

.transfer-page {
  width: 100%;
}

.sub-page {
  width: 100%;
}

@media (max-width: 768px) {
  .equipment-page {
    grid-template-columns: 1fr;
  }

  .app-header {
    flex-direction: column;
    gap: 12px;
    text-align: center;
  }

  .header-nav {
    margin-top: 8px;
  }
}
</style>
