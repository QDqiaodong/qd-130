import axios from 'axios'

const BASE_URL = '/api'

const api = axios.create({
  baseURL: BASE_URL,
  timeout: 10000
})

export const areaApi = {
  getAllAreas: () => api.get('/areas'),
  getAreaTree: () => api.get('/areas/tree'),
  getAreaById: (id) => api.get(`/areas/${id}`),
  createArea: (data) => api.post('/areas', data),
  updateArea: (id, data) => api.put(`/areas/${id}`, data),
  deleteArea: (id) => api.delete(`/areas/${id}`)
}

export const equipmentApi = {
  getAllEquipments: () => api.get('/equipments'),
  getEquipmentById: (id) => api.get(`/equipments/${id}`),
  getEquipmentByNo: (no) => api.get(`/equipments/byNo/${no}`),
  getEquipmentsByArea: (areaId) => api.get(`/equipments/byArea/${areaId}`),
  getEquipmentTypes: () => api.get('/equipments/types'),
  createEquipment: (data) => api.post('/equipments', data),
  updateEquipment: (id, data) => api.put(`/equipments/${id}`, data),
  deleteEquipment: (id) => api.delete(`/equipments/${id}`),
  bindArea: (id, areaId) => api.put(`/equipments/${id}/bindArea`, { areaId })
}

export const transferApi = {
  getAllTransfers: () => api.get('/transfers'),
  getTransfersByDateRange: (startDate, endDate, extraParams = {}) =>
    api.get('/transfers/filter', { params: { startDate, endDate, ...extraParams } }),
  getTransferById: (id) => api.get(`/transfers/${id}`),
  getTransferByNo: (no) => api.get(`/transfers/byNo/${no}`),
  getTransferSummary: (startDate, endDate, extraParams = {}) =>
    api.get('/transfers/summary', { params: { startDate, endDate, ...extraParams } }),
  createTransfer: (data) => api.post('/transfers', data),
  cancelTransfer: (id) => api.put(`/transfers/${id}/cancel`)
}

export const inspectionPlanApi = {
  getAllPlans: () => api.get('/inspection-plans'),
  getPlanById: (id) => api.get(`/inspection-plans/${id}`),
  createPlan: (data) => api.post('/inspection-plans', data),
  updatePlan: (id, data) => api.put(`/inspection-plans/${id}`, data),
  updatePlanStatus: (id, status) => api.put(`/inspection-plans/${id}/status`, { status })
}

export const inspectionApi = {
  getInspections: (params) => api.get('/inspections', { params }),
  getInspectionDetail: (id) => api.get(`/inspections/${id}`),
  createInspection: (data) => api.post('/inspections', data)
}

export const repairApi = {
  getRepairs: (params) => api.get('/repairs', { params }),
  getRepairById: (id) => api.get(`/repairs/${id}`),
  createRepair: (data) => api.post('/repairs', data),
  updateRepairStatus: (id, data) => api.put(`/repairs/${id}/status`, data),
  getOverdueRepairs: (params) => api.get('/repairs/overdue', { params }),
  urgeRepair: (id, data) => api.put(`/repairs/${id}/urge`, data)
}

export const dashboardApi = {
  getHealthDashboard: (params) => api.get('/dashboard/health', { params })
}

export default api