
<script setup>
import { ref, onMounted } from 'vue'
import AreaTree from './components/AreaTree.vue'
import EquipmentList from './components/EquipmentList.vue'
import EquipmentForm from './components/EquipmentForm.vue'
import TransferForm from './components/TransferForm.vue'
import TransferList from './components/TransferList.vue'
import InspectionPlanList from './components/InspectionPlanList.vue'
import InspectionList from './components/InspectionList.vue'
import RepairList from './components/RepairList.vue'
import { equipmentApi } from './api'

const activeTab = ref('equipment')
const equipmentList = ref([])
const selectedArea = ref(null)
const transferFormVisible = ref(false)
const transferEquipment = ref(null)
const equipmentFormVisible = ref(false)
const editEquipment = ref(null)

const loadEquipments = async (areaId = null) => {
  try {
    let res
    if (areaId) {
      res = await equipmentApi.getEquipmentsByArea(areaId)
    } else {
      res = await equipmentApi.getAllEquipments()
    }
    if (res.data.code === 200) {
      equipmentList.value = res.data.data
    }
  } catch (error) {
    console.error('加载设备列表失败:', error)
  }
}

const handleAreaSelected = (area) => {
  selectedArea.value = area
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

onMounted(() => {
  loadEquipments()
})
</script>

<template>
  <div class="app-container">
    <header class="app-header">
      <h1>大型超市母婴室配套设备管理系统</h1>
      <div class="header-nav">
        <button :class="['nav-btn', { active: activeTab === 'equipment' }]" @click="activeTab = 'equipment'; loadEquipments()">设备管理</button>
        <button :class="['nav-btn', { active: activeTab === 'transfer' }]" @click="activeTab = 'transfer'">调配台账</button>
        <button :class="['nav-btn', { active: activeTab === 'plan' }]" @click="activeTab = 'plan'">巡检计划</button>
        <button :class="['nav-btn', { active: activeTab === 'inspection' }]" @click="activeTab = 'inspection'">巡检记录</button>
        <button :class="['nav-btn', { active: activeTab === 'repair' }]" @click="activeTab = 'repair'">故障报修</button>
      </div>
    </header>
    
    <main class="app-main">
      <div v-if="activeTab === 'equipment'" class="equipment-page">
        <div class="sidebar">
          <AreaTree @area-selected="handleAreaSelected" />
        </div>
        <div class="content">
          <div class="content-header">
            <button class="add-btn" @click="handleAddEquipment">新建设备</button>
          </div>
          <EquipmentList :equipment-list="equipmentList" @edit="handleEdit" @transfer="handleTransfer" />
        </div>
      </div>
      
      <div v-else-if="activeTab === 'transfer'" class="transfer-page">
        <TransferList />
      </div>

      <div v-else-if="activeTab === 'plan'" class="sub-page">
        <InspectionPlanList />
      </div>

      <div v-else-if="activeTab === 'inspection'" class="sub-page">
        <InspectionList />
      </div>

      <div v-else class="sub-page">
        <RepairList />
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
