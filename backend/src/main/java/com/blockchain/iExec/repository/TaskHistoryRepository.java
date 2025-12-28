package com.blockchain.iExec.repository;

import com.blockchain.iExec.model.TaskHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务历史数据访问层
 */
@Repository
public interface TaskHistoryRepository extends JpaRepository<TaskHistoryEntity, Long> {
    
    /**
     * 根据服务ID查询历史任务，按创建时间倒序
     */
    List<TaskHistoryEntity> findByServiceIdOrderByCreatedAtDesc(String serviceId);
    
    /**
     * 根据用户地址查询历史任务
     */
    List<TaskHistoryEntity> findByUserAddress(String userAddress);
    
    /**
     * 根据状态查询任务
     */
    List<TaskHistoryEntity> findByStatus(String status);
    
    /**
     * 查询指定时间段内的任务
     */
    List<TaskHistoryEntity> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    
    /**
     * 查询已完成的任务（用于统计分析）
     */
    @Query("SELECT t FROM TaskHistoryEntity t WHERE t.status = 'Completed' AND t.completedAt IS NOT NULL")
    List<TaskHistoryEntity> findCompletedTasks();
    
    /**
     * 获取指定服务的平均完成时间
     */
    @Query("SELECT AVG(t.actualTime) FROM TaskHistoryEntity t WHERE t.serviceId = :serviceId AND t.status = 'Completed'")
    Double getAverageCompletionTime(@Param("serviceId") String serviceId);
    
    /**
     * 获取指定服务最近N条已完成任务
     */
    @Query("SELECT t FROM TaskHistoryEntity t WHERE t.serviceId = :serviceId AND t.status = 'Completed' ORDER BY t.completedAt DESC")
    List<TaskHistoryEntity> findRecentCompletedTasks(@Param("serviceId") String serviceId);
}
