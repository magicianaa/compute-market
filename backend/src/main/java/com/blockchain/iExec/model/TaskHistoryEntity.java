package com.blockchain.iExec.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 任务历史实体 - 用于存储任务执行历史数据，支持动态调度算法
 */
@Entity
@Table(name = "task_history")
public class TaskHistoryEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String taskId;  // 链上任务ID
    
    @Column(nullable = false)
    private String iexecTaskId;  // iExec任务ID
    
    @Column(nullable = false)
    private String serviceId;  // 服务ID
    
    @Column(nullable = false)
    private String userAddress;  // 用户地址
    
    @Column(nullable = false)
    private String status;  // 最终状态
    
    private Long estimatedTime;  // 预估完成时间（秒）
    
    private Long actualTime;  // 实际完成时间（秒）
    
    private Integer priority;  // 任务优先级
    
    private String resourceRequirement;  // 资源需求（JSON格式）
    
    private Double costAmount;  // 成本金额
    
    @Column(nullable = false)
    private LocalDateTime createdAt;  // 创建时间
    
    private LocalDateTime startedAt;  // 开始执行时间
    
    private LocalDateTime completedAt;  // 完成时间
    
    private String errorMessage;  // 错误信息（如果失败）
    
    private String resultHash;  // IPFS结果哈希
    
    // Getters and Setters
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getTaskId() {
        return taskId;
    }
    
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
    
    public String getIexecTaskId() {
        return iexecTaskId;
    }
    
    public void setIexecTaskId(String iexecTaskId) {
        this.iexecTaskId = iexecTaskId;
    }
    
    public String getServiceId() {
        return serviceId;
    }
    
    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }
    
    public String getUserAddress() {
        return userAddress;
    }
    
    public void setUserAddress(String userAddress) {
        this.userAddress = userAddress;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public Long getEstimatedTime() {
        return estimatedTime;
    }
    
    public void setEstimatedTime(Long estimatedTime) {
        this.estimatedTime = estimatedTime;
    }
    
    public Long getActualTime() {
        return actualTime;
    }
    
    public void setActualTime(Long actualTime) {
        this.actualTime = actualTime;
    }
    
    public Integer getPriority() {
        return priority;
    }
    
    public void setPriority(Integer priority) {
        this.priority = priority;
    }
    
    public String getResourceRequirement() {
        return resourceRequirement;
    }
    
    public void setResourceRequirement(String resourceRequirement) {
        this.resourceRequirement = resourceRequirement;
    }
    
    public Double getCostAmount() {
        return costAmount;
    }
    
    public void setCostAmount(Double costAmount) {
        this.costAmount = costAmount;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getStartedAt() {
        return startedAt;
    }
    
    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }
    
    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
    
    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public String getResultHash() {
        return resultHash;
    }
    
    public void setResultHash(String resultHash) {
        this.resultHash = resultHash;
    }
}
