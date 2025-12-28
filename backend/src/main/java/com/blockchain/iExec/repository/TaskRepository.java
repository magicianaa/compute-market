package com.blockchain.iExec.repository;

import com.blockchain.iExec.model.TaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<TaskEntity, Long> {
    TaskEntity findByTaskId(String taskId);
    TaskEntity findByIexecTaskId(String iexecTaskId);
    List<TaskEntity> findByStatus(String status);
    List<TaskEntity> findByUserAddress(String userAddress);
}