
package com.example.maternal.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

public class CodeGenerator {
    
    private static final AtomicInteger COUNTER = new AtomicInteger(0);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    
    public static String generateTransferNo() {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        int sequence = COUNTER.incrementAndGet() % 10000;
        return String.format("TF%s%04d", timestamp, sequence);
    }
    
    public static String generateEquipmentNo() {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        int sequence = COUNTER.incrementAndGet() % 10000;
        return String.format("EQ%s%04d", timestamp, sequence);
    }
    
    public static String generateAreaCode() {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        int sequence = COUNTER.incrementAndGet() % 1000;
        return String.format("AREA%s%03d", timestamp.substring(2), sequence);
    }

    public static String generatePlanNo() {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        int sequence = COUNTER.incrementAndGet() % 10000;
        return String.format("PL%s%04d", timestamp, sequence);
    }

    public static String generateInspectionNo() {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        int sequence = COUNTER.incrementAndGet() % 10000;
        return String.format("IN%s%04d", timestamp, sequence);
    }

    public static String generateSpotCheckNo() {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        int sequence = COUNTER.incrementAndGet() % 10000;
        return String.format("SC%s%04d", timestamp, sequence);
    }

    public static String generateRepairNo() {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        int sequence = COUNTER.incrementAndGet() % 10000;
        return String.format("RP%s%04d", timestamp, sequence);
    }

    public static String generateDisinfectionNo() {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        int sequence = COUNTER.incrementAndGet() % 10000;
        return String.format("DS%s%04d", timestamp, sequence);
    }

    public static String generateOpeningNo() {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        int sequence = COUNTER.incrementAndGet() % 10000;
        return String.format("OP%s%04d", timestamp, sequence);
    }

    public static String generateHandoverNo() {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        int sequence = COUNTER.incrementAndGet() % 10000;
        return String.format("HD%s%04d", timestamp, sequence);
    }

    public static String generateSupplyHandoverNo() {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        int sequence = COUNTER.incrementAndGet() % 10000;
        return String.format("SH%s%04d", timestamp, sequence);
    }
}
