SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS maternal_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE maternal_db;

DROP TABLE IF EXISTS transfer_record;
DROP TABLE IF EXISTS equipment;
DROP TABLE IF EXISTS area;

CREATE TABLE area (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '区域ID',
    name VARCHAR(100) NOT NULL COMMENT '区域名称',
    code VARCHAR(50) UNIQUE NOT NULL COMMENT '区域编码',
    parent_id BIGINT DEFAULT NULL COMMENT '父区域ID',
    level INT NOT NULL DEFAULT 1 COMMENT '层级',
    path VARCHAR(500) COMMENT '路径',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    sort_order INT DEFAULT 0 COMMENT '排序号',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_parent_id (parent_id),
    INDEX idx_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='功能区域树形表';

CREATE TABLE equipment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '设备ID',
    equipment_no VARCHAR(50) UNIQUE NOT NULL COMMENT '设备编号',
    equipment_name VARCHAR(100) NOT NULL COMMENT '设备名称',
    equipment_type VARCHAR(50) NOT NULL COMMENT '功能类型',
    model VARCHAR(100) COMMENT '型号规格',
    brand VARCHAR(100) COMMENT '品牌',
    image_url VARCHAR(500) COMMENT '设备实拍图URL',
    current_area_id BIGINT NOT NULL COMMENT '当前所在区域ID',
    initial_area_id BIGINT NOT NULL COMMENT '初始母婴室区域ID',
    status TINYINT DEFAULT 1 COMMENT '状态：0-停用，1-使用中',
    remark VARCHAR(500) COMMENT '备注',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_equipment_no (equipment_no),
    INDEX idx_current_area_id (current_area_id),
    INDEX idx_initial_area_id (initial_area_id),
    INDEX idx_equipment_type (equipment_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='母婴配套设备表';

CREATE TABLE transfer_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '调配记录ID',
    transfer_no VARCHAR(50) UNIQUE NOT NULL COMMENT '调配单号',
    equipment_id BIGINT NOT NULL COMMENT '设备ID',
    from_area_id BIGINT NOT NULL COMMENT '原区域ID',
    to_area_id BIGINT NOT NULL COMMENT '目标区域ID',
    transfer_date DATE NOT NULL COMMENT '调配日期',
    reason VARCHAR(500) COMMENT '调配原因',
    operator VARCHAR(50) COMMENT '操作人',
    status TINYINT DEFAULT 1 COMMENT '状态：0-取消，1-已完成',
    remark VARCHAR(500) COMMENT '备注',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_transfer_no (transfer_no),
    INDEX idx_equipment_id (equipment_id),
    INDEX idx_from_area_id (from_area_id),
    INDEX idx_to_area_id (to_area_id),
    INDEX idx_transfer_date (transfer_date),
    INDEX idx_from_to_area (from_area_id, to_area_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='跨区域调配记录表';

INSERT INTO area (name, code, parent_id, level, path, sort_order) VALUES
('商超A区', 'AREA-A', NULL, 1, '/AREA-A', 1),
('商超B区', 'AREA-B', NULL, 1, '/AREA-B', 2),
('商超C区', 'AREA-C', NULL, 1, '/AREA-C', 3),
('A1母婴室', 'AREA-A1', 1, 2, '/AREA-A/AREA-A1', 1),
('A2母婴室', 'AREA-A2', 1, 2, '/AREA-A/AREA-A2', 2),
('B1母婴室', 'AREA-B1', 2, 2, '/AREA-B/AREA-B1', 1),
('B2母婴室', 'AREA-B2', 2, 2, '/AREA-B/AREA-B2', 2),
('C1母婴室', 'AREA-C1', 3, 2, '/AREA-C/AREA-C1', 1);

INSERT INTO equipment (equipment_no, equipment_name, equipment_type, model, brand, current_area_id, initial_area_id) VALUES
('EQ-001', '温奶器', '温奶器', 'WM-2024', '贝亲', 4, 4),
('EQ-002', '婴儿护理台', '护理台', 'HLT-2024', '康贝', 4, 4),
('EQ-003', '温奶器', '温奶器', 'WM-2024', '贝亲', 5, 5),
('EQ-004', '婴儿护理台', '护理台', 'HLT-2024', '康贝', 5, 5),
('EQ-005', '温奶器', '温奶器', 'WM-2024', '贝亲', 6, 6),
('EQ-006', '婴儿护理台', '护理台', 'HLT-2024', '康贝', 6, 6),
('EQ-007', '温奶器', '温奶器', 'WM-2024', '贝亲', 7, 7),
('EQ-008', '婴儿护理台', '护理台', 'HLT-2024', '康贝', 7, 7);
