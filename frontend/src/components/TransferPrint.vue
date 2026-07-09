
<template>
  <div class="modal-overlay" v-if="visible" @click="handleClose">
    <div class="modal-content" @click.stop>
      <div class="modal-header">
        <h3>调配单据打印预览</h3>
        <button class="close-btn" @click="handleClose">×</button>
      </div>
      <div class="modal-body">
        <div class="print-container" id="print-content">
          <div class="print-header">
            <div class="company-name">大型超市母婴室配套设备管理系统</div>
            <div class="document-title">跨区域调配单</div>
          </div>
          
          <div class="print-body" v-if="record">
            <div class="info-row">
              <div class="info-item">
                <span class="label">调配单号:</span>
                <span class="value">{{ record.transferNo }}</span>
              </div>
              <div class="info-item">
                <span class="label">调配日期:</span>
                <span class="value">{{ record.transferDate }}</span>
              </div>
            </div>
            
            <div class="section">
              <h4>设备信息</h4>
              <div class="info-row">
                <div class="info-item">
                  <span class="label">设备编号:</span>
                  <span class="value">{{ record.equipmentNo }}</span>
                </div>
                <div class="info-item">
                  <span class="label">设备名称:</span>
                  <span class="value">{{ record.equipmentName }}</span>
                </div>
              </div>
            </div>
            
            <div class="section">
              <h4>区域调配信息</h4>
              <div class="transfer-row">
                <div class="transfer-item">
                  <span class="label">原区域:</span>
                  <span class="value from-area">{{ record.fromAreaName }}</span>
                </div>
                <div class="arrow">→</div>
                <div class="transfer-item">
                  <span class="label">目标区域:</span>
                  <span class="value to-area">{{ record.toAreaName }}</span>
                </div>
              </div>
            </div>
            
            <div class="section">
              <h4>其他信息</h4>
              <div class="info-row">
                <div class="info-item">
                  <span class="label">调配原因:</span>
                  <span class="value">{{ record.reason || '-' }}</span>
                </div>
              </div>
              <div class="info-row">
                <div class="info-item">
                  <span class="label">操作人:</span>
                  <span class="value">{{ record.operator || '-' }}</span>
                </div>
                <div class="info-item">
                  <span class="label">状态:</span>
                  <span class="value">{{ record.status === 1 ? '已完成' : '已取消' }}</span>
                </div>
              </div>
              <div class="info-row" v-if="record.remark">
                <div class="info-item full">
                  <span class="label">备注:</span>
                  <span class="value">{{ record.remark }}</span>
                </div>
              </div>
            </div>
            
            <div class="print-footer">
              <div class="signature">
                <span>操作人签字:</span>
                <span class="signature-line"></span>
              </div>
              <div class="signature">
                <span>日期:</span>
                <span class="signature-line"></span>
              </div>
            </div>
            
            <div class="print-barcode">
              <span>单号: {{ record.transferNo }}</span>
            </div>
          </div>
        </div>
        
        <div class="print-actions">
          <button @click="handleClose">关闭</button>
          <button @click="handlePrint">打印单据</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  visible: Boolean,
  record: Object
})

const emit = defineEmits(['close'])

const handleClose = () => {
  emit('close')
}

const handlePrint = () => {
  window.print()
}
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
  max-width: 600px;
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

.print-container {
  background: #fff;
  padding: 24px;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.print-header {
  text-align: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 2px solid #333;
}

.company-name {
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
}

.document-title {
  font-size: 24px;
  font-weight: bold;
  color: #333;
}

.print-body {
  font-size: 14px;
}

.info-row {
  display: flex;
  margin-bottom: 12px;
}

.info-item {
  display: flex;
  flex: 1;
}

.info-item.full {
  flex: 2;
}

.info-item .label {
  color: #666;
  width: 80px;
}

.info-item .value {
  color: #333;
  font-weight: bold;
}

.section {
  margin-bottom: 20px;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 4px;
}

.section h4 {
  margin: 0 0 12px 0;
  font-size: 14px;
  color: #333;
  border-bottom: 1px solid #e0e0e0;
  padding-bottom: 6px;
}

.transfer-row {
  display: flex;
  align-items: center;
  gap: 16px;
}

.transfer-item {
  flex: 1;
  display: flex;
}

.transfer-item .label {
  color: #666;
  width: 80px;
}

.transfer-item .value {
  font-weight: bold;
}

.transfer-item .value.from-area {
  color: #f56c6c;
}

.transfer-item .value.to-area {
  color: #67c23a;
}

.arrow {
  font-size: 24px;
  color: #409eff;
}

.print-footer {
  display: flex;
  justify-content: space-around;
  margin-top: 32px;
  padding-top: 16px;
  border-top: 1px dashed #ddd;
}

.signature {
  display: flex;
  align-items: center;
  gap: 8px;
}

.signature-line {
  width: 150px;
  height: 1px;
  background: #333;
}

.print-barcode {
  text-align: center;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #ddd;
  font-size: 12px;
  color: #999;
}

.print-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  margin-top: 24px;
}

.print-actions button {
  padding: 10px 32px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.print-actions button:first-child {
  background: #f0f0f0;
  color: #666;
}

.print-actions button:last-child {
  background: #409eff;
  color: #fff;
}

@media print {
  .modal-overlay,
  .modal-header,
  .print-actions {
    display: none !important;
  }
  
  .print-container {
    border: none;
    padding: 0;
    margin: 0;
  }
  
  body {
    background: #fff;
  }
}
</style>
