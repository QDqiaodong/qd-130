SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS maternal_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE maternal_db;

DROP TABLE IF EXISTS repair_order;
DROP TABLE IF EXISTS spot_check_record;
DROP TABLE IF EXISTS patrol_checkin;
DROP TABLE IF EXISTS disinfection_record;
DROP TABLE IF EXISTS supply_handover;
DROP TABLE IF EXISTS room_opening_record;
DROP TABLE IF EXISTS inspection_record;
DROP TABLE IF EXISTS inspection_plan;
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
    receiver VARCHAR(50) COMMENT '到货签收人（值班）',
    arrival_time DATETIME COMMENT '到货签收时间',
    appearance_intact TINYINT(1) COMMENT '到货外观是否完好：0-有破损，1-完好',
    damage_part VARCHAR(200) COMMENT '外观破损部位：外观有破损签收时必填',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_transfer_no (transfer_no),
    INDEX idx_equipment_id (equipment_id),
    INDEX idx_from_area_id (from_area_id),
    INDEX idx_to_area_id (to_area_id),
    INDEX idx_transfer_date (transfer_date),
    INDEX idx_from_to_area (from_area_id, to_area_id),
    INDEX idx_arrival_time (arrival_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='跨区域调配记录表';

CREATE TABLE inspection_plan (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '计划ID',
    plan_no VARCHAR(50) UNIQUE NOT NULL COMMENT '计划编号',
    plan_name VARCHAR(100) NOT NULL COMMENT '计划名称',
    equipment_id BIGINT DEFAULT NULL COMMENT '关联设备ID（按设备巡检）',
    area_id BIGINT DEFAULT NULL COMMENT '关联区域ID（按区域巡检）',
    cycle_type INT NOT NULL COMMENT '巡检周期：1-每日，2-每周，3-每月',
    next_inspection_date DATE COMMENT '下次巡检日期',
    inspector VARCHAR(50) COMMENT '默认巡检员',
    status TINYINT DEFAULT 1 COMMENT '状态：0-停用，1-启用',
    remark VARCHAR(500) COMMENT '备注',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_plan_no (plan_no),
    INDEX idx_equipment_id (equipment_id),
    INDEX idx_area_id (area_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备巡检计划表';

CREATE TABLE inspection_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '巡检记录ID',
    inspection_no VARCHAR(50) UNIQUE NOT NULL COMMENT '巡检单号',
    plan_id BIGINT DEFAULT NULL COMMENT '关联巡检计划ID',
    equipment_id BIGINT NOT NULL COMMENT '设备ID',
    area_id BIGINT NOT NULL COMMENT '巡检时所在区域ID',
    inspection_date DATE NOT NULL COMMENT '巡检日期',
    result TINYINT NOT NULL COMMENT '巡检结果：1-正常，2-异常',
    abnormal_desc VARCHAR(500) COMMENT '异常描述',
    photo_url VARCHAR(500) COMMENT '异常照片地址',
    inspector VARCHAR(50) COMMENT '巡检员',
    remark VARCHAR(500) COMMENT '备注',
    reviewer VARCHAR(50) COMMENT '复核人（值班）',
    review_time DATETIME COMMENT '复核时间',
    review_result TINYINT COMMENT '复核结论：1-属实，2-不属实',
    review_note VARCHAR(500) COMMENT '复核说明（不属实必填）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_inspection_equipment_date (equipment_id, inspection_date),
    INDEX idx_inspection_no (inspection_no),
    INDEX idx_plan_id (plan_id),
    INDEX idx_area_id (area_id),
    INDEX idx_inspection_date (inspection_date),
    INDEX idx_result (result),
    INDEX idx_review_result (review_result)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备巡检记录表';

CREATE TABLE spot_check_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '抽检记录ID',
    spot_check_no VARCHAR(50) UNIQUE NOT NULL COMMENT '抽检单号',
    equipment_id BIGINT NOT NULL COMMENT '温奶器设备ID',
    area_id BIGINT NOT NULL COMMENT '抽检时所在母婴室区域ID',
    check_date DATE NOT NULL COMMENT '抽检日期',
    temperature DECIMAL(5,2) NOT NULL COMMENT '实测水温（℃）',
    qualified TINYINT(1) NOT NULL COMMENT '抽检结论：0-不合格，1-合格（按40~50℃区间判定）',
    abnormal_desc VARCHAR(500) COMMENT '不合格说明',
    photo_url VARCHAR(500) COMMENT '抽检照片地址',
    inspector VARCHAR(50) NOT NULL COMMENT '抽检人（值班人员）',
    remark VARCHAR(500) COMMENT '备注',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_spotcheck_equipment_date (equipment_id, check_date),
    INDEX idx_spot_check_no (spot_check_no),
    INDEX idx_area_id (area_id),
    INDEX idx_check_date (check_date),
    INDEX idx_qualified (qualified)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='温奶器温度抽检记录表';

CREATE TABLE disinfection_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '消毒登记ID',
    disinfection_no VARCHAR(50) UNIQUE NOT NULL COMMENT '消毒登记单号',
    area_id BIGINT NOT NULL COMMENT '母婴室区域ID',
    disinfect_date DATE NOT NULL COMMENT '消毒日期',
    operator VARCHAR(50) NOT NULL COMMENT '消毒人（值班人员）',
    finish_time DATETIME NOT NULL COMMENT '消毒完成时间',
    disinfectant VARCHAR(100) NOT NULL COMMENT '使用的消毒液',
    ventilation_done TINYINT(1) NOT NULL COMMENT '通风是否做完：0-未做完，1-已做完',
    closed_loop TINYINT(1) NOT NULL COMMENT '当日是否闭环：0-未闭环，1-已闭环（仅通风做完算闭环，服务端落库）',
    incomplete_reason VARCHAR(500) COMMENT '未完成原因（通风未做完必填）',
    remark VARCHAR(500) COMMENT '备注',
    closed_token VARCHAR(80) GENERATED ALWAYS AS (IF(closed_loop = 1, CONCAT(area_id, '_', disinfect_date), NULL)) STORED COMMENT '闭环唯一令牌（同一母婴室每天仅允许一条闭环）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_disinfection_closed_token (closed_token),
    INDEX idx_disinfection_no (disinfection_no),
    INDEX idx_area_id (area_id),
    INDEX idx_disinfect_date (disinfect_date),
    INDEX idx_closed_loop (closed_loop)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='母婴室消毒登记表';

CREATE TABLE patrol_checkin (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '打卡记录ID',
    checkin_no VARCHAR(50) UNIQUE NOT NULL COMMENT '打卡单号',
    area_id BIGINT NOT NULL COMMENT '母婴室区域ID',
    patrol_date DATE NOT NULL COMMENT '巡更日期（夜间班次归属的日期）',
    shift INT NOT NULL COMMENT '巡更班次：1-前夜班（22:00-02:00），2-后夜班（02:00-06:00）',
    patrol_person VARCHAR(50) NOT NULL COMMENT '巡更人（值班人员）',
    checkin_time DATETIME NOT NULL COMMENT '打卡时间',
    remark VARCHAR(500) COMMENT '备注',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_patrol_area_date_shift (area_id, patrol_date, shift),
    INDEX idx_checkin_no (checkin_no),
    INDEX idx_area_id (area_id),
    INDEX idx_patrol_date (patrol_date),
    INDEX idx_shift (shift)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='母婴室夜间巡更打卡表';

CREATE TABLE supply_handover (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '交接单ID',
    handover_no VARCHAR(50) UNIQUE NOT NULL COMMENT '交接单号',
    area_id BIGINT NOT NULL COMMENT '母婴室区域ID',
    handover_date DATE NOT NULL COMMENT '交班日期',
    handover_person VARCHAR(50) NOT NULL COMMENT '交班人（值班人员）',
    receiver VARCHAR(50) NOT NULL COMMENT '接班人',
    wipes_count INT NOT NULL COMMENT '湿巾盘点件数',
    diaper_count INT NOT NULL COMMENT '纸尿裤盘点件数',
    handed_over TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已交接：0-未交接，1-已交接（接班人确认后落库）',
    handover_time DATETIME DEFAULT NULL COMMENT '交接确认时间',
    remark VARCHAR(500) COMMENT '备注',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_handover_no (handover_no),
    INDEX idx_area_id (area_id),
    INDEX idx_handover_date (handover_date),
    INDEX idx_handed_over (handed_over)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='母婴室值班交接用品盘点表';

CREATE TABLE room_opening_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '开放登记ID',
    record_no VARCHAR(50) UNIQUE NOT NULL COMMENT '开放登记单号',
    area_id BIGINT NOT NULL COMMENT '母婴室区域ID',
    open_date DATE NOT NULL COMMENT '开放日期',
    open_time DATETIME DEFAULT NULL COMMENT '当日开门时刻（临时关闭时为空）',
    close_time DATETIME DEFAULT NULL COMMENT '当日关门时刻（临时关闭时为空）',
    temporarily_closed TINYINT(1) NOT NULL COMMENT '是否临时关闭：0-正常开放，1-临时关闭',
    close_reason VARCHAR(500) DEFAULT NULL COMMENT '临时关闭原因（临时关闭必填）',
    remark VARCHAR(500) COMMENT '备注',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_room_open_date (area_id, open_date),
    INDEX idx_record_no (record_no),
    INDEX idx_area_id (area_id),
    INDEX idx_open_date (open_date),
    INDEX idx_temporarily_closed (temporarily_closed)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='母婴室开放日开关门登记表';

CREATE TABLE repair_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '报修单ID',
    repair_no VARCHAR(50) UNIQUE NOT NULL COMMENT '报修单号',
    inspection_id BIGINT DEFAULT NULL COMMENT '来源巡检记录ID（巡检异常报修）',
    spot_check_id BIGINT DEFAULT NULL COMMENT '来源温奶器抽检记录ID（抽检不合格报修）',
    equipment_id BIGINT NOT NULL COMMENT '设备ID',
    area_id BIGINT NOT NULL COMMENT '报修时所在区域ID',
    fault_desc VARCHAR(500) COMMENT '故障描述',
    photo_url VARCHAR(500) COMMENT '故障照片地址',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-待处理，1-维修中，2-已恢复',
    reporter VARCHAR(50) COMMENT '报修人',
    repairman VARCHAR(50) COMMENT '维修人',
    start_time DATETIME COMMENT '开始维修时间',
    finish_time DATETIME COMMENT '恢复完成时间',
    repair_note VARCHAR(500) COMMENT '维修说明',
    trial_result VARCHAR(500) COMMENT '复用前试机结论（恢复结单前必填，未试机不能结单）',
    trial_time DATETIME COMMENT '复用前试机时间',
    urge_note VARCHAR(500) COMMENT '最近催办说明',
    urge_time DATETIME COMMENT '最近催办时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_repair_inspection (inspection_id),
    UNIQUE KEY uk_repair_spotcheck (spot_check_id),
    INDEX idx_repair_no (repair_no),
    INDEX idx_equipment_id (equipment_id),
    INDEX idx_area_id (area_id),
    INDEX idx_spot_check_id (spot_check_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备故障报修单表';

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

INSERT INTO inspection_plan (plan_no, plan_name, equipment_id, area_id, cycle_type, next_inspection_date, inspector, status, remark) VALUES
('PL202609010001', '温奶器每周巡检', 1, NULL, 2, '2026-09-15', '张工', 1, '重点检查加热与温控功能'),
('PL202609010002', 'A1母婴室每日巡检', NULL, 4, 1, '2026-09-11', '李工', 1, '每日开店前完成巡检');

INSERT INTO inspection_record (inspection_no, plan_id, equipment_id, area_id, inspection_date, result, abnormal_desc, photo_url, inspector, remark, reviewer, review_time, review_result, review_note) VALUES
('IN202609070001', 2, 2, 4, '2026-09-07', 2, '护理台安全带卡扣损坏，存在脱落风险', 'https://example.com/photos/eq002-buckle.jpg', '李工', '已现场围挡停用', '赵值班', '2026-09-07 10:30:00', 1, '现场核对卡扣确实断裂，情况属实'),
('IN202609080001', 1, 1, 4, '2026-09-08', 2, '温奶器加热异常，指示灯不亮', 'https://example.com/photos/eq001-fault.jpg', '张工', '已断电停用待修', '赵值班', '2026-09-08 09:40:00', 1, '通电复核确认无法加热，情况属实'),
('IN202609090001', 2, 2, 4, '2026-09-09', 1, NULL, NULL, '李工', '维修后复检正常', NULL, NULL, NULL, NULL),
('IN202609100001', NULL, 6, 6, '2026-09-10', 2, '护理台护栏松动，疑似卡扣未锁紧', NULL, '王工', NULL, '钱值班', '2026-09-10 15:20:00', 2, '值班现场复测护栏已锁紧，为巡检误判，无需报修'),
('IN202609110001', NULL, 8, 7, '2026-09-11', 2, '护理台软包破损，海绵外露', NULL, '钱工', NULL, NULL, NULL, NULL, NULL);

INSERT INTO repair_order (repair_no, inspection_id, equipment_id, area_id, fault_desc, photo_url, status, reporter, repairman, start_time, finish_time, repair_note, trial_result, trial_time) VALUES
('RP202609070001', 1, 2, 4, '护理台安全带卡扣损坏，存在脱落风险', 'https://example.com/photos/eq002-buckle.jpg', 2, '李工', '王师傅', '2026-09-07 14:00:00', '2026-09-08 17:30:00', '更换原厂卡扣，拉力测试合格，设备恢复使用', '复用前通电试机：卡扣开合顺畅、锁紧牢固，承重测试无松动，可恢复使用', '2026-09-08 17:15:00'),
('RP202609080001', 2, 1, 4, '温奶器加热异常，指示灯不亮', 'https://example.com/photos/eq001-fault.jpg', 0, '张工', NULL, NULL, NULL, NULL, NULL, NULL);

INSERT INTO spot_check_record (spot_check_no, equipment_id, area_id, check_date, temperature, qualified, abnormal_desc, photo_url, inspector, remark) VALUES
('SC202609100001', 3, 5, '2026-09-10', 45.50, 1, NULL, 'https://example.com/photos/eq003-ok.jpg', '赵值班', '开店后2小时实测'),
('SC202609100002', 5, 6, '2026-09-10', 55.20, 0, '抽检温度55.20℃，不在合格区间40.00~50.00℃；水温偏高，疑似温控失灵', 'https://example.com/photos/eq005-hot.jpg', '赵值班', '已现场断电'),
('SC202609100003', 7, 7, '2026-09-10', 36.00, 0, '抽检温度36.00℃，不在合格区间40.00~50.00℃；温度偏低', 'https://example.com/photos/eq007-cold.jpg', '钱值班', NULL);

INSERT INTO repair_order (repair_no, spot_check_id, equipment_id, area_id, fault_desc, photo_url, status, reporter, repairman, start_time, finish_time, repair_note) VALUES
('RP202609100001', 2, 5, 6, '抽检温度55.20℃，不在合格区间40.00~50.00℃；水温偏高，疑似温控失灵', 'https://example.com/photos/eq005-hot.jpg', 0, '赵值班', NULL, NULL, NULL, NULL);

INSERT INTO disinfection_record (disinfection_no, area_id, disinfect_date, operator, finish_time, disinfectant, ventilation_done, closed_loop, incomplete_reason, remark) VALUES
('DS202609110001', 4, '2026-09-11', '赵值班', '2026-09-11 09:30:00', '84消毒液（1:100）', 1, 1, NULL, '开店前完成消毒并通风30分钟'),
('DS202609110002', 5, '2026-09-11', '赵值班', '2026-09-11 09:50:00', '84消毒液（1:100）', 0, 0, '排风扇故障未能通风，已报物业检修', '通风恢复后需补登记闭环'),
('DS202609110003', 6, '2026-09-11', '钱值班', '2026-09-11 10:10:00', '季铵盐消毒液', 1, 1, NULL, NULL),
('DS202609110004', 5, '2026-09-11', '钱值班', '2026-09-11 16:40:00', '84消毒液（1:100）', 1, 1, NULL, '排风扇修复后补做通风，当日闭环');

INSERT INTO supply_handover (handover_no, area_id, handover_date, handover_person, receiver, wipes_count, diaper_count, handed_over, handover_time, remark) VALUES
('SH202609130001', 4, '2026-09-13', '赵值班', '钱值班', 12, 30, 1, '2026-09-13 08:05:00', '早班交接，件数双方核对无误'),
('SH202609130002', 5, '2026-09-13', '赵值班', '钱值班', 8, 22, 1, '2026-09-13 08:10:00', '湿巾余量偏少，已通知补货'),
('SH202609130003', 6, '2026-09-13', '钱值班', '孙值班', 15, 40, 0, NULL, NULL),
('SH202609120001', 7, '2026-09-12', '孙值班', '赵值班', 10, 25, 1, '2026-09-12 21:05:00', '晚班交接');

INSERT INTO patrol_checkin (checkin_no, area_id, patrol_date, shift, patrol_person, checkin_time, remark) VALUES
('PC202609130001', 4, '2026-09-13', 1, '赵值班', '2026-09-13 22:10:00', '前夜班巡更，门窗正常'),
('PC202609130002', 5, '2026-09-13', 1, '赵值班', '2026-09-13 22:25:00', NULL),
('PC202609130003', 4, '2026-09-13', 2, '钱值班', '2026-09-14 02:15:00', '后夜班巡更，室内无异常'),
('PC202609120001', 6, '2026-09-12', 1, '孙值班', '2026-09-12 22:05:00', '历史打卡仅供回看');

INSERT INTO room_opening_record (record_no, area_id, open_date, open_time, close_time, temporarily_closed, close_reason, remark) VALUES
('OP202609130001', 4, '2026-09-13', '2026-09-13 08:00:00', '2026-09-13 22:00:00', 0, NULL, '正常开放'),
('OP202609130002', 5, '2026-09-13', NULL, NULL, 1, '室内水管爆裂抢修，当日临时关闭，预计次日恢复', '已在门口张贴闭室告示'),
('OP202609130003', 6, '2026-09-13', '2026-09-13 10:00:00', '2026-09-13 21:30:00', 0, NULL, NULL),
('OP202609120001', 7, '2026-09-12', '2026-09-12 08:30:00', '2026-09-12 21:00:00', 0, NULL, '历史登记仅供回看');
