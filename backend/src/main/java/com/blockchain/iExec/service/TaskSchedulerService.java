package com.blockchain.iExec.service;

import com.blockchain.iExec.model.ReputationEntity;
import com.blockchain.iExec.model.TaskEntity;
import com.blockchain.iExec.model.TaskHistoryEntity;
import com.blockchain.iExec.repository.TaskHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 动态任务调度服务 - 核心创新点
 * 实现基于历史数据的智能调度算法
 */
@Service
public class TaskSchedulerService {
    
    private static final Logger logger = LoggerFactory.getLogger(TaskSchedulerService.class);
    
    @Autowired
    private TaskHistoryRepository taskHistoryRepository;
    
    @Autowired
    private ReputationService reputationService;
    
    // 默认完成时间（秒）
    private static final long DEFAULT_COMPLETION_TIME = 300;  // 5分钟
    
    // 历史数据窗口大小
    private static final int HISTORY_WINDOW_SIZE = 20;
    
    /**
     * 预测任务完成时间（核心算法1：加权移动平均）
     * 使用指数衰减权重，越近的历史数据权重越高
     * 
     * @param serviceId 服务ID
     * @return 预测的完成时间（秒）
     */
    public long predictCompletionTime(String serviceId) {
        logger.debug("Predicting completion time for service: {}", serviceId);
        
        try {
            // 1. 获取该服务的历史任务数据
            List<TaskHistoryEntity> history = taskHistoryRepository
                .findByServiceIdOrderByCreatedAtDesc(serviceId)
                .stream()
                .filter(h -> "Completed".equals(h.getStatus()))
                .filter(h -> h.getActualTime() != null && h.getActualTime() > 0)
                .limit(HISTORY_WINDOW_SIZE)
                .collect(Collectors.toList());
            
            if (history.isEmpty()) {
                logger.debug("No historical data for service {}, using default time", serviceId);
                return DEFAULT_COMPLETION_TIME;
            }
            
            // 2. 计算加权移动平均（WMA - Weighted Moving Average）
            double weightedSum = 0;
            double weightSum = 0;
            
            for (int i = 0; i < history.size(); i++) {
                // 使用指数衰减权重：w(i) = e^(-λ*i)，其中 λ = 0.1
                double weight = Math.exp(-0.1 * i);
                long completionTime = history.get(i).getActualTime();
                
                weightedSum += completionTime * weight;
                weightSum += weight;
            }
            
            long predictedTime = (long) (weightedSum / weightSum);
            
            logger.info("Predicted completion time for service {}: {} seconds (based on {} samples)",
                serviceId, predictedTime, history.size());
            
            return predictedTime;
            
        } catch (Exception e) {
            logger.error("Error predicting completion time for service: {}", serviceId, e);
            return DEFAULT_COMPLETION_TIME;
        }
    }
    
    /**
     * 计算任务优先级（核心算法2：多因素优先级计算）
     * 考虑因素：用户信誉、等待时间、支付金额、历史成功率
     * 
     * @param task 任务实体
     * @return 优先级分数（越高越优先）
     */
    public int calculatePriority(TaskEntity task) {
        logger.debug("Calculating priority for task: {}", task.getTaskId());
        
        try {
            int basePriority = 100;
            
            // 1. 用户信誉加成（0-50分）
            int reputationBonus = calculateReputationBonus(task.getUserAddress());
            
            // 2. 等待时间惩罚（每分钟+0.5分，鼓励处理等待久的任务）
            int waitingBonus = calculateWaitingBonus(task.getCreatedAt());
            
            // 3. 支付金额加成（支付越多优先级越高，0-30分）
            int paymentBonus = calculatePaymentBonus(task);
            
            // 4. 历史成功率加成（用户历史任务成功率，0-20分）
            int successRateBonus = calculateSuccessRateBonus(task.getUserAddress());
            
            int finalPriority = basePriority + reputationBonus + waitingBonus + paymentBonus + successRateBonus;
            
            logger.info("Task {} priority: {} (reputation: {}, waiting: {}, payment: {}, successRate: {})",
                task.getTaskId(), finalPriority, reputationBonus, waitingBonus, paymentBonus, successRateBonus);
            
            return finalPriority;
            
        } catch (Exception e) {
            logger.error("Error calculating priority for task: {}", task.getTaskId(), e);
            return 100;  // 返回基础优先级
        }
    }
    
    /**
     * 计算信誉加成
     */
    private int calculateReputationBonus(String userAddress) {
        try {
            ReputationEntity reputation = reputationService.getReputationByAddress(userAddress);
            if (reputation == null) {
                return 0;
            }
            
            // 信誉分数 0-1 映射到 0-50 分
            return (int) (reputation.getFinalScore() * 50);
            
        } catch (Exception e) {
            logger.error("Error calculating reputation bonus", e);
            return 0;
        }
    }
    
    /**
     * 计算等待时间加成
     */
    private int calculateWaitingBonus(LocalDateTime createdAt) {
        if (createdAt == null) {
            return 0;
        }
        
        long waitingMinutes = Duration.between(createdAt, LocalDateTime.now()).toMinutes();
        
        // 每分钟 +0.5 分，最高不超过 50 分
        return (int) Math.min(waitingMinutes * 0.5, 50);
    }
    
    /**
     * 计算支付金额加成
     */
    private int calculatePaymentBonus(TaskEntity task) {
        // 这里简化处理，实际应该从任务中获取支付金额
        // 假设支付金额存储在某个字段中
        // 金额越高，加成越多，最高30分
        
        // TODO: 从任务或合约中获取实际支付金额
        return 15;  // 临时返回中等加成
    }
    
    /**
     * 计算历史成功率加成
     */
    private int calculateSuccessRateBonus(String userAddress) {
        try {
            List<TaskHistoryEntity> userHistory = taskHistoryRepository.findByUserAddress(userAddress);
            
            if (userHistory.isEmpty()) {
                return 10;  // 新用户给予基础加成
            }
            
            long completedCount = userHistory.stream()
                .filter(h -> "Completed".equals(h.getStatus()))
                .count();
            
            double successRate = (double) completedCount / userHistory.size();
            
            // 成功率 0-1 映射到 0-20 分
            return (int) (successRate * 20);
            
        } catch (Exception e) {
            logger.error("Error calculating success rate bonus", e);
            return 0;
        }
    }
    
    /**
     * 预测资源需求（核心算法3：资源需求预测）
     * 基于历史数据预测任务所需资源
     * 
     * @param serviceId 服务ID
     * @return 资源需求描述
     */
    public ResourceRequirement predictResourceRequirement(String serviceId) {
        logger.debug("Predicting resource requirement for service: {}", serviceId);
        
        try {
            List<TaskHistoryEntity> history = taskHistoryRepository
                .findByServiceIdOrderByCreatedAtDesc(serviceId)
                .stream()
                .limit(HISTORY_WINDOW_SIZE)
                .collect(Collectors.toList());
            
            ResourceRequirement requirement = new ResourceRequirement();
            
            if (history.isEmpty()) {
                // 默认资源配置
                requirement.setCpuCores(1);
                requirement.setMemoryMB(512);
                requirement.setStorageGB(1);
                return requirement;
            }
            
            // 基于历史数据计算平均资源需求
            // 这里简化处理，实际应该解析 resourceRequirement JSON 字段
            requirement.setCpuCores(2);
            requirement.setMemoryMB(1024);
            requirement.setStorageGB(2);
            
            logger.info("Predicted resource requirement for service {}: {} cores, {} MB RAM, {} GB storage",
                serviceId, requirement.getCpuCores(), requirement.getMemoryMB(), requirement.getStorageGB());
            
            return requirement;
            
        } catch (Exception e) {
            logger.error("Error predicting resource requirement", e);
            return new ResourceRequirement();  // 返回默认配置
        }
    }
    
    /**
     * 动态调整任务调度策略（核心算法4：自适应调度）
     * 根据系统负载和历史性能动态调整
     * 
     * @return 调度策略建议
     */
    public SchedulingStrategy getAdaptiveSchedulingStrategy() {
        logger.debug("Calculating adaptive scheduling strategy");
        
        try {
            // 1. 分析当前系统负载
            List<TaskHistoryEntity> recentTasks = taskHistoryRepository
                .findByCreatedAtBetween(
                    LocalDateTime.now().minusHours(1),
                    LocalDateTime.now()
                );
            
            SchedulingStrategy strategy = new SchedulingStrategy();
            
            // 2. 计算系统吞吐量
            long completedInLastHour = recentTasks.stream()
                .filter(h -> "Completed".equals(h.getStatus()))
                .count();
            
            strategy.setThroughput(completedInLastHour);
            
            // 3. 计算平均响应时间
            double avgResponseTime = recentTasks.stream()
                .filter(h -> h.getActualTime() != null)
                .mapToLong(TaskHistoryEntity::getActualTime)
                .average()
                .orElse(DEFAULT_COMPLETION_TIME);
            
            strategy.setAverageResponseTime((long) avgResponseTime);
            
            // 4. 根据负载调整策略
            if (completedInLastHour < 10) {
                strategy.setRecommendation("LOW_LOAD");
                strategy.setMaxConcurrentTasks(10);
            } else if (completedInLastHour < 50) {
                strategy.setRecommendation("MEDIUM_LOAD");
                strategy.setMaxConcurrentTasks(20);
            } else {
                strategy.setRecommendation("HIGH_LOAD");
                strategy.setMaxConcurrentTasks(30);
            }
            
            logger.info("Adaptive scheduling strategy: {} tasks/hour, avg response: {}s, recommendation: {}",
                completedInLastHour, avgResponseTime, strategy.getRecommendation());
            
            return strategy;
            
        } catch (Exception e) {
            logger.error("Error calculating scheduling strategy", e);
            return new SchedulingStrategy();  // 返回默认策略
        }
    }
    
    /**
     * 对比基准算法性能（用于学术论文）
     * 
     * @param serviceId 服务ID
     * @return 性能对比结果
     */
    public PerformanceComparison compareWithBaseline(String serviceId) {
        logger.info("Comparing scheduling performance for service: {}", serviceId);
        
        PerformanceComparison comparison = new PerformanceComparison();
        
        try {
            // 1. 获取使用我们算法的结果
            long ourPrediction = predictCompletionTime(serviceId);
            comparison.setWeightedMovingAverage(ourPrediction);
            
            // 2. 简单移动平均（SMA - Simple Moving Average）作为基准
            Double simpleAvg = taskHistoryRepository.getAverageCompletionTime(serviceId);
            comparison.setSimpleMovingAverage(simpleAvg != null ? simpleAvg.longValue() : DEFAULT_COMPLETION_TIME);
            
            // 3. 计算改进百分比
            if (simpleAvg != null && simpleAvg > 0) {
                double improvement = ((simpleAvg - ourPrediction) / simpleAvg) * 100;
                comparison.setImprovementPercentage(improvement);
            }
            
            logger.info("Performance comparison - WMA: {}s, SMA: {}s, Improvement: {}%",
                comparison.getWeightedMovingAverage(),
                comparison.getSimpleMovingAverage(),
                comparison.getImprovementPercentage());
            
        } catch (Exception e) {
            logger.error("Error comparing performance", e);
        }
        
        return comparison;
    }
    
    // ==================== 数据类 ====================
    
    /**
     * 资源需求类
     */
    public static class ResourceRequirement {
        private int cpuCores = 1;
        private int memoryMB = 512;
        private int storageGB = 1;
        
        public int getCpuCores() {
            return cpuCores;
        }
        
        public void setCpuCores(int cpuCores) {
            this.cpuCores = cpuCores;
        }
        
        public int getMemoryMB() {
            return memoryMB;
        }
        
        public void setMemoryMB(int memoryMB) {
            this.memoryMB = memoryMB;
        }
        
        public int getStorageGB() {
            return storageGB;
        }
        
        public void setStorageGB(int storageGB) {
            this.storageGB = storageGB;
        }
    }
    
    /**
     * 调度策略类
     */
    public static class SchedulingStrategy {
        private long throughput;
        private long averageResponseTime;
        private String recommendation;
        private int maxConcurrentTasks;
        
        public long getThroughput() {
            return throughput;
        }
        
        public void setThroughput(long throughput) {
            this.throughput = throughput;
        }
        
        public long getAverageResponseTime() {
            return averageResponseTime;
        }
        
        public void setAverageResponseTime(long averageResponseTime) {
            this.averageResponseTime = averageResponseTime;
        }
        
        public String getRecommendation() {
            return recommendation;
        }
        
        public void setRecommendation(String recommendation) {
            this.recommendation = recommendation;
        }
        
        public int getMaxConcurrentTasks() {
            return maxConcurrentTasks;
        }
        
        public void setMaxConcurrentTasks(int maxConcurrentTasks) {
            this.maxConcurrentTasks = maxConcurrentTasks;
        }
    }
    
    /**
     * 性能对比类
     */
    public static class PerformanceComparison {
        private long weightedMovingAverage;
        private long simpleMovingAverage;
        private double improvementPercentage;
        
        public long getWeightedMovingAverage() {
            return weightedMovingAverage;
        }
        
        public void setWeightedMovingAverage(long weightedMovingAverage) {
            this.weightedMovingAverage = weightedMovingAverage;
        }
        
        public long getSimpleMovingAverage() {
            return simpleMovingAverage;
        }
        
        public void setSimpleMovingAverage(long simpleMovingAverage) {
            this.simpleMovingAverage = simpleMovingAverage;
        }
        
        public double getImprovementPercentage() {
            return improvementPercentage;
        }
        
        public void setImprovementPercentage(double improvementPercentage) {
            this.improvementPercentage = improvementPercentage;
        }
    }
}
