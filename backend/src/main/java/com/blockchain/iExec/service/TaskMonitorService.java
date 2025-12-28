package com.blockchain.iExec.service;

import com.blockchain.iExec.model.TaskEntity;
import com.blockchain.iExec.model.TaskHistoryEntity;
import com.blockchain.iExec.repository.TaskHistoryRepository;
import com.blockchain.iExec.service.IexecCliService.IexecTaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务监控服务 - 定时轮询 iExec 网络获取任务状态
 * 负责同步计算结果到数据库并回写区块链状态
 */
@Service
public class TaskMonitorService {
    
    private static final Logger logger = LoggerFactory.getLogger(TaskMonitorService.class);
    
    @Autowired
    private IexecCliService iexecCliService;
    
    @Autowired
    private TaskService taskService;
    
    @Autowired
    private TaskHistoryRepository taskHistoryRepository;
    
    @Autowired
    private ReputationService reputationService;
    
    @Value("${task.monitor.interval:30000}")
    private long monitorInterval;
    
    @Value("${task.monitor.timeout:3600000}")
    private long taskTimeout;
    
    /**
     * 定时监控运行中的任务
     * 默认每30秒执行一次
     */
    @Scheduled(fixedDelayString = "${task.monitor.interval:30000}")
    public void monitorRunningTasks() {
        logger.debug("Starting task monitoring cycle");
        
        try {
            // 获取所有运行中的任务
            List<TaskEntity> runningTasks = taskService.getTasksByStatus("Running");
            
            if (runningTasks.isEmpty()) {
                logger.debug("No running tasks to monitor");
                return;
            }
            
            logger.info("Monitoring {} running tasks", runningTasks.size());
            
            for (TaskEntity task : runningTasks) {
                try {
                    monitorSingleTask(task);
                } catch (Exception e) {
                    logger.error("Error monitoring task {}: {}", task.getTaskId(), e.getMessage(), e);
                }
            }
            
        } catch (Exception e) {
            logger.error("Error in task monitoring cycle", e);
        }
    }
    
    /**
     * 监控单个任务
     */
    private void monitorSingleTask(TaskEntity task) {
        String taskId = task.getTaskId();
        String iexecTaskId = task.getIexecTaskId();
        
        logger.debug("Monitoring task: {} (iExec: {})", taskId, iexecTaskId);
        
        // 检查任务是否超时
        if (isTaskTimeout(task)) {
            handleTaskTimeout(task);
            return;
        }
        
        // 查询 iExec 任务状态
        IexecTaskStatus status = iexecCliService.getTaskStatus(iexecTaskId);
        
        if (status == null) {
            logger.warn("Failed to get status for task: {}", iexecTaskId);
            return;
        }
        
        // 根据状态处理任务
        if (status.isCompleted()) {
            handleTaskCompleted(task, status);
        } else if (status.isFailed()) {
            handleTaskFailed(task, status);
        } else {
            logger.debug("Task {} is still running, status: {}", taskId, status.getStatus());
        }
    }
    
    /**
     * 处理任务完成
     */
    private void handleTaskCompleted(TaskEntity task, IexecTaskStatus status) {
        String taskId = task.getTaskId();
        String resultHash = status.getResultLocation();
        
        logger.info("Task {} completed successfully, result: {}", taskId, resultHash);
        
        try {
            // 1. 更新任务状态和结果
            taskService.updateTaskResult(taskId, resultHash);
            
            // 2. 保存任务历史记录
            saveTaskHistory(task, "Completed", resultHash, null);
            
            // 3. 更新用户信誉
            updateUserReputation(task, true, true);
            
            // 4. 回写区块链状态（这里需要调用智能合约）
            // TODO: 调用合约的 completeTask 方法
            // contractService.completeTask(taskId, resultHash);
            
            logger.info("Task {} processing completed", taskId);
            
        } catch (Exception e) {
            logger.error("Error processing completed task {}: {}", taskId, e.getMessage(), e);
        }
    }
    
    /**
     * 处理任务失败
     */
    private void handleTaskFailed(TaskEntity task, IexecTaskStatus status) {
        String taskId = task.getTaskId();
        String errorMessage = "iExec task failed with status: " + status.getStatus();
        
        logger.error("Task {} failed: {}", taskId, errorMessage);
        
        try {
            // 1. 更新任务状态
            taskService.updateTaskErrorMessage(taskId, errorMessage);
            
            // 2. 保存任务历史记录
            saveTaskHistory(task, "Failed", null, errorMessage);
            
            // 3. 更新用户信誉（失败记录）
            updateUserReputation(task, false, false);
            
            // 4. 可以触发退款流程
            // TODO: 调用合约的 refundTask 方法
            // contractService.refundTask(taskId);
            
            logger.info("Task {} failure processed", taskId);
            
        } catch (Exception e) {
            logger.error("Error processing failed task {}: {}", taskId, e.getMessage(), e);
        }
    }
    
    /**
     * 处理任务超时
     */
    private void handleTaskTimeout(TaskEntity task) {
        String taskId = task.getTaskId();
        logger.warn("Task {} has timed out", taskId);
        
        try {
            String errorMessage = "Task execution timeout after " + (taskTimeout / 60000) + " minutes";
            taskService.updateTaskErrorMessage(taskId, errorMessage);
            
            // 保存任务历史记录
            saveTaskHistory(task, "Timeout", null, errorMessage);
            
            // 更新用户信誉
            updateUserReputation(task, false, false);
            
            // 触发退款
            // TODO: 调用合约的 refundTask 方法
            
        } catch (Exception e) {
            logger.error("Error processing timeout task {}: {}", taskId, e.getMessage(), e);
        }
    }
    
    /**
     * 检查任务是否超时
     */
    private boolean isTaskTimeout(TaskEntity task) {
        LocalDateTime createdAt = task.getCreatedAt();
        if (createdAt == null) {
            return false;
        }
        
        Duration duration = Duration.between(createdAt, LocalDateTime.now());
        return duration.toMillis() > taskTimeout;
    }
    
    /**
     * 保存任务历史记录
     */
    private void saveTaskHistory(TaskEntity task, String finalStatus, String resultHash, String errorMessage) {
        try {
            TaskHistoryEntity history = new TaskHistoryEntity();
            history.setTaskId(task.getTaskId());
            history.setIexecTaskId(task.getIexecTaskId());
            history.setUserAddress(task.getUserAddress());
            history.setStatus(finalStatus);
            history.setResultHash(resultHash);
            history.setErrorMessage(errorMessage);
            history.setCreatedAt(task.getCreatedAt());
            history.setCompletedAt(LocalDateTime.now());
            
            // 计算实际完成时间
            if (task.getCreatedAt() != null) {
                long actualTime = Duration.between(task.getCreatedAt(), LocalDateTime.now()).getSeconds();
                history.setActualTime(actualTime);
            }
            
            taskHistoryRepository.save(history);
            logger.debug("Task history saved for task: {}", task.getTaskId());
            
        } catch (Exception e) {
            logger.error("Error saving task history: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 更新用户信誉
     */
    private void updateUserReputation(TaskEntity task, boolean completed, boolean onTime) {
        try {
            String userAddress = task.getUserAddress();
            
            // 简化的质量评分（实际应该基于结果验证）
            double qualityScore = completed ? 0.9 : 0.0;
            
            reputationService.updateReputation(userAddress, completed, onTime, qualityScore);
            logger.debug("Reputation updated for user: {}", userAddress);
            
        } catch (Exception e) {
            logger.error("Error updating reputation: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 手动触发任务监控（用于测试或手动干预）
     */
    public void triggerManualMonitoring(String taskId) {
        logger.info("Manual monitoring triggered for task: {}", taskId);
        
        TaskEntity task = taskService.getTaskByTaskId(taskId);
        if (task == null) {
            logger.warn("Task not found: {}", taskId);
            return;
        }
        
        monitorSingleTask(task);
    }
    
    /**
     * 获取监控统计信息
     */
    public MonitoringStats getMonitoringStats() {
        MonitoringStats stats = new MonitoringStats();
        
        try {
            List<TaskEntity> runningTasks = taskService.getTasksByStatus("Running");
            stats.setRunningTasksCount(runningTasks.size());
            
            // 计算超时任务数
            long timeoutCount = runningTasks.stream()
                .filter(this::isTaskTimeout)
                .count();
            stats.setTimeoutTasksCount((int) timeoutCount);
            
            // 统计完成任务数
            List<TaskHistoryEntity> completedTasks = taskHistoryRepository.findByStatus("Completed");
            stats.setCompletedTasksCount(completedTasks.size());
            
            // 统计失败任务数
            List<TaskHistoryEntity> failedTasks = taskHistoryRepository.findByStatus("Failed");
            stats.setFailedTasksCount(failedTasks.size());
            
        } catch (Exception e) {
            logger.error("Error getting monitoring stats", e);
        }
        
        return stats;
    }
    
    /**
     * 监控统计信息类
     */
    public static class MonitoringStats {
        private int runningTasksCount;
        private int timeoutTasksCount;
        private int completedTasksCount;
        private int failedTasksCount;
        
        // Getters and Setters
        public int getRunningTasksCount() {
            return runningTasksCount;
        }
        
        public void setRunningTasksCount(int runningTasksCount) {
            this.runningTasksCount = runningTasksCount;
        }
        
        public int getTimeoutTasksCount() {
            return timeoutTasksCount;
        }
        
        public void setTimeoutTasksCount(int timeoutTasksCount) {
            this.timeoutTasksCount = timeoutTasksCount;
        }
        
        public int getCompletedTasksCount() {
            return completedTasksCount;
        }
        
        public void setCompletedTasksCount(int completedTasksCount) {
            this.completedTasksCount = completedTasksCount;
        }
        
        public int getFailedTasksCount() {
            return failedTasksCount;
        }
        
        public void setFailedTasksCount(int failedTasksCount) {
            this.failedTasksCount = failedTasksCount;
        }
    }
}
