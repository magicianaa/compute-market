package com.blockchain.iExec.controller;

import com.blockchain.iExec.service.TaskMonitorService;
import com.blockchain.iExec.service.TaskSchedulerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 监控与调度 API 控制器
 * 提供任务监控统计和调度算法相关接口
 */
@RestController
@RequestMapping("/monitor")
public class MonitorController {
    
    @Autowired
    private TaskMonitorService taskMonitorService;
    
    @Autowired
    private TaskSchedulerService taskSchedulerService;
    
    /**
     * 获取监控统计信息
     */
    @GetMapping("/stats")
    public ResponseEntity<TaskMonitorService.MonitoringStats> getMonitoringStats() {
        TaskMonitorService.MonitoringStats stats = taskMonitorService.getMonitoringStats();
        return ResponseEntity.ok(stats);
    }
    
    /**
     * 手动触发任务监控
     */
    @PostMapping("/trigger/{taskId}")
    public ResponseEntity<String> triggerMonitoring(@PathVariable String taskId) {
        taskMonitorService.triggerManualMonitoring(taskId);
        return ResponseEntity.ok("Monitoring triggered for task: " + taskId);
    }
    
    /**
     * 预测任务完成时间
     */
    @GetMapping("/predict/{serviceId}")
    public ResponseEntity<Long> predictCompletionTime(@PathVariable String serviceId) {
        long predictedTime = taskSchedulerService.predictCompletionTime(serviceId);
        return ResponseEntity.ok(predictedTime);
    }
    
    /**
     * 获取自适应调度策略
     */
    @GetMapping("/strategy")
    public ResponseEntity<TaskSchedulerService.SchedulingStrategy> getSchedulingStrategy() {
        TaskSchedulerService.SchedulingStrategy strategy = taskSchedulerService.getAdaptiveSchedulingStrategy();
        return ResponseEntity.ok(strategy);
    }
    
    /**
     * 性能对比分析
     */
    @GetMapping("/compare/{serviceId}")
    public ResponseEntity<TaskSchedulerService.PerformanceComparison> comparePerformance(@PathVariable String serviceId) {
        TaskSchedulerService.PerformanceComparison comparison = taskSchedulerService.compareWithBaseline(serviceId);
        return ResponseEntity.ok(comparison);
    }
    
    /**
     * 预测资源需求
     */
    @GetMapping("/resources/{serviceId}")
    public ResponseEntity<TaskSchedulerService.ResourceRequirement> predictResources(@PathVariable String serviceId) {
        TaskSchedulerService.ResourceRequirement requirement = taskSchedulerService.predictResourceRequirement(serviceId);
        return ResponseEntity.ok(requirement);
    }
}
