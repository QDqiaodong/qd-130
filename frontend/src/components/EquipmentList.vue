
<template>
  <div class="equipment-list">
    <h3>设备列表</h3>
    <div class="equipment-grid">
      <div v-for="equipment in equipmentList" :key="equipment.id" class="equipment-card">
        <div class="equipment-image">
          <img :src="equipment.imageUrl || 'data:image/svg+xml,%3Csvg xmlns=%27http://www.w3.org/2000/svg%27 width=%27200%27 height=%27200%27 viewBox=%270 0 200 200%27%3E%3Crect fill=%27%23f0f0f0%27 width=%27200%27 height=%27200%27/%3E%3Ctext fill=%27%23999%27 font-family=%27sans-serif%27 font-size=%2714%27 x=%2750%25%27 y=%2750%25%27 text-anchor=%27middle%27 dy=%27.3em%27%3E设备图片%3C/text%3E%3C/svg%3E'" alt="设备图片" />
        </div>
        <div class="equipment-info">
          <div class="equipment-no">{{ equipment.equipmentNo }}</div>
          <div class="equipment-name">{{ equipment.equipmentName }}</div>
          <div class="equipment-type">{{ equipment.equipmentType }}</div>
          <div class="equipment-area">
            <span>当前区域:</span>
            <span>{{ equipment.currentAreaName || '-' }}</span>
          </div>
          <div class="equipment-buttons">
            <button @click="$emit('edit', equipment)">编辑</button>
            <button @click="$emit('transfer', equipment)">调配</button>
          </div>
        </div>
      </div>
    </div>
    <p v-if="equipmentList.length === 0" class="empty">暂无设备数据</p>
  </div>
</template>

<script setup>
defineProps({
  equipmentList: {
    type: Array,
    default: () => []
  }
})

defineEmits(['edit', 'transfer'])
</script>

<style scoped>
.equipment-list {
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 16px;
  background: #fff;
}

.equipment-list h3 {
  margin: 0 0 16px 0;
  font-size: 16px;
  color: #333;
  border-bottom: 1px solid #e0e0e0;
  padding-bottom: 8px;
}

.equipment-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.equipment-card {
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  overflow: hidden;
  transition: box-shadow 0.2s;
}

.equipment-card:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.equipment-image {
  width: 100%;
  height: 180px;
  background: #f8f8f8;
  display: flex;
  align-items: center;
  justify-content: center;
}

.equipment-image img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.equipment-info {
  padding: 12px;
}

.equipment-no {
  font-size: 12px;
  color: #999;
  margin-bottom: 4px;
}

.equipment-name {
  font-size: 16px;
  font-weight: bold;
  color: #333;
  margin-bottom: 4px;
}

.equipment-type {
  font-size: 13px;
  color: #666;
  margin-bottom: 8px;
}

.equipment-area {
  font-size: 12px;
  color: #666;
  margin-bottom: 12px;
}

.equipment-area span:first-child {
  color: #999;
}

.equipment-buttons {
  display: flex;
  gap: 8px;
}

.equipment-buttons button {
  flex: 1;
  padding: 6px 12px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  transition: background 0.2s;
}

.equipment-buttons button:first-child {
  background: #f0f0f0;
  color: #666;
}

.equipment-buttons button:first-child:hover {
  background: #e0e0e0;
}

.equipment-buttons button:last-child {
  background: #409eff;
  color: #fff;
}

.equipment-buttons button:last-child:hover {
  background: #66b1ff;
}

.empty {
  text-align: center;
  color: #999;
  padding: 40px;
}
</style>
