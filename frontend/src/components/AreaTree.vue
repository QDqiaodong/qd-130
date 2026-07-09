
<template>
  <div class="area-tree">
    <h3>商超功能区域</h3>
    <div class="tree-container">
      <ul v-if="treeData.length > 0">
        <li v-for="node in treeData" :key="node.id">
          <div class="tree-node" :style="{ paddingLeft: '0px' }" @click="selectArea(node)">
            <span class="expand-icon" v-if="node.children && node.children.length > 0" @click.stop="toggleExpand(node)">
              {{ expandedIds.includes(node.id) ? '−' : '+' }}
            </span>
            <span v-else class="expand-placeholder">·</span>
            <span :class="['area-name', { 'selected': selectedArea?.id === node.id }]">{{ node.name }}</span>
            <span class="area-code">{{ node.code }}</span>
          </div>
          <ul v-if="node.children && node.children.length > 0 && expandedIds.includes(node.id)">
            <li v-for="child in node.children" :key="child.id">
              <div class="tree-node" :style="{ paddingLeft: '20px' }" @click="selectArea(child)">
                <span class="expand-icon" v-if="child.children && child.children.length > 0" @click.stop="toggleExpand(child)">
                  {{ expandedIds.includes(child.id) ? '−' : '+' }}
                </span>
                <span v-else class="expand-placeholder">·</span>
                <span :class="['area-name', { 'selected': selectedArea?.id === child.id }]">{{ child.name }}</span>
                <span class="area-code">{{ child.code }}</span>
              </div>
              <ul v-if="child.children && child.children.length > 0 && expandedIds.includes(child.id)">
                <li v-for="grandchild in child.children" :key="grandchild.id">
                  <div class="tree-node" :style="{ paddingLeft: '40px' }" @click="selectArea(grandchild)">
                    <span class="area-name" :class="{ 'selected': selectedArea?.id === grandchild.id }">{{ grandchild.name }}</span>
                    <span class="area-code">{{ grandchild.code }}</span>
                  </div>
                </li>
              </ul>
            </li>
          </ul>
        </li>
      </ul>
      <p v-else class="empty">暂无区域数据</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { areaApi } from '../api'

const emit = defineEmits(['area-selected'])

const treeData = ref([])
const expandedIds = ref([])
const selectedArea = ref(null)

const loadTree = async () => {
  try {
    const res = await areaApi.getAreaTree()
    if (res.data.code === 200) {
      treeData.value = res.data.data
      res.data.data.forEach(node => {
        if (node.children && node.children.length > 0) {
          expandedIds.value.push(node.id)
        }
      })
    }
  } catch (error) {
    console.error('加载区域树失败:', error)
  }
}

const toggleExpand = (node) => {
  const index = expandedIds.value.indexOf(node.id)
  if (index > -1) {
    expandedIds.value.splice(index, 1)
  } else {
    expandedIds.value.push(node.id)
  }
}

const selectArea = (area) => {
  selectedArea.value = area
  emit('area-selected', area)
}

onMounted(() => {
  loadTree()
})

defineExpose({ loadTree })
</script>

<style scoped>
.area-tree {
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 16px;
  background: #fff;
}

.area-tree h3 {
  margin: 0 0 16px 0;
  font-size: 16px;
  color: #333;
  border-bottom: 1px solid #e0e0e0;
  padding-bottom: 8px;
}

.tree-container {
  max-height: 400px;
  overflow-y: auto;
}

.tree-container ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.tree-container li {
  margin: 4px 0;
}

.tree-node {
  display: flex;
  align-items: center;
  padding: 6px 8px;
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.2s;
}

.tree-node:hover {
  background: #f5f5f5;
}

.expand-icon, .expand-placeholder {
  width: 20px;
  text-align: center;
  color: #666;
  font-weight: bold;
}

.area-name {
  flex: 1;
  color: #333;
  font-size: 14px;
}

.area-name.selected {
  color: #409eff;
  font-weight: bold;
}

.area-code {
  color: #999;
  font-size: 12px;
  margin-left: 8px;
}

.empty {
  text-align: center;
  color: #999;
  padding: 20px;
}
</style>
