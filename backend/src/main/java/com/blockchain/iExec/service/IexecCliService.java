package com.blockchain.iExec.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * iExec CLI 服务 - 封装所有 iExec 命令行工具调用
 * 负责与 iExec 去中心化算力网络的交互
 */
@Service
public class IexecCliService {
    
    private static final Logger logger = LoggerFactory.getLogger(IexecCliService.class);
    
    @Value("${iexec.workspace.dir:/tmp/iexec-workspace}")
    private String workspaceDir;
    
    @Value("${iexec.command.timeout:300}")
    private int commandTimeout;
    
    @Value("${iexec.chain:bellecour}")
    private String chain;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 初始化 iExec 工作环境
     */
    public void initializeWorkspace() throws IOException, InterruptedException {
        logger.info("Initializing iExec workspace at: {}", workspaceDir);
        
        // 创建工作目录
        Files.createDirectories(Paths.get(workspaceDir));
        
        // 初始化 iExec 配置
        try {
            executeCommand("iexec init --skip-wallet");
            logger.info("iExec workspace initialized successfully");
        } catch (Exception e) {
            logger.warn("Workspace already initialized or initialization skipped", e);
        }
    }
    
    /**
     * 部署应用到 iExec 网络
     * @param appName 应用名称
     * @param dockerImage Docker镜像地址
     * @return 部署的应用地址
     */
    public String deployApp(String appName, String dockerImage) throws IOException, InterruptedException {
        logger.info("Deploying app: {} with image: {}", appName, dockerImage);
        
        // 创建应用配置
        String appConfig = String.format(
            "{\"name\":\"%s\",\"multiaddr\":\"%s\",\"checksum\":\"0x0000000000000000000000000000000000000000000000000000000000000000\"}",
            appName, dockerImage
        );
        
        // 写入配置文件
        Path configPath = Paths.get(workspaceDir, "iexec.json");
        Files.writeString(configPath, appConfig);
        
        // 执行部署
        String output = executeCommand("iexec app deploy --chain " + chain);
        
        // 解析应用地址
        String appAddress = extractAddress(output, "app deployed");
        logger.info("App deployed successfully at: {}", appAddress);
        
        return appAddress;
    }
    
    /**
     * 创建并发布应用订单
     * @param appAddress 应用地址
     * @param price 价格（nRLC）
     * @param volume 订单数量
     * @return 订单哈希
     */
    public String createAppOrder(String appAddress, String price, int volume) throws IOException, InterruptedException {
        logger.info("Creating app order for: {} with price: {} and volume: {}", appAddress, price, volume);
        
        // 初始化订单
        String initOutput = executeCommand(String.format(
            "iexec order init --app --chain %s", chain
        ));
        
        // 设置订单参数
        String orderPath = Paths.get(workspaceDir, ".iexec", "orders.json").toString();
        updateOrderJson(orderPath, appAddress, price, volume);
        
        // 签名订单
        executeCommand("iexec order sign --app --chain " + chain);
        
        // 发布订单
        String publishOutput = executeCommand("iexec order publish --app --chain " + chain);
        
        // 解析订单哈希
        String orderHash = extractOrderHash(publishOutput);
        logger.info("App order created and published: {}", orderHash);
        
        return orderHash;
    }
    
    /**
     * 创建工作池订单（使用公共工作池）
     * @return 工作池订单哈希
     */
    public String getPublicWorkerpoolOrder() throws IOException, InterruptedException {
        logger.info("Fetching public workerpool order");
        
        // 查询公共工作池订单
        String output = executeCommand("iexec orderbook workerpool --chain " + chain);
        
        // 解析第一个可用的工作池订单
        String orderHash = extractFirstWorkerpoolOrder(output);
        logger.info("Using public workerpool order: {}", orderHash);
        
        return orderHash;
    }
    
    /**
     * 创建请求订单
     * @param appAddress 应用地址
     * @param params 计算参数
     * @return 请求订单哈希
     */
    public String createRequestOrder(String appAddress, String params) throws IOException, InterruptedException {
        logger.info("Creating request order for app: {}", appAddress);
        
        // 初始化请求订单
        executeCommand("iexec order init --request --chain " + chain);
        
        // 设置请求参数
        String orderPath = Paths.get(workspaceDir, ".iexec", "orders.json").toString();
        updateRequestOrderJson(orderPath, appAddress, params);
        
        // 签名订单
        executeCommand("iexec order sign --request --chain " + chain);
        
        logger.info("Request order created successfully");
        return "request-order-created";
    }
    
    /**
     * 匹配订单并创建交易（Deal）
     * @param appOrderHash 应用订单哈希
     * @param workerpoolOrderHash 工作池订单哈希
     * @return Deal ID
     */
    public String createDeal(String appOrderHash, String workerpoolOrderHash) throws IOException, InterruptedException {
        logger.info("Creating deal with app order: {} and workerpool order: {}", appOrderHash, workerpoolOrderHash);
        
        // 执行订单匹配
        String output = executeCommand(String.format(
            "iexec order fill --app %s --workerpool %s --chain %s",
            appOrderHash, workerpoolOrderHash, chain
        ));
        
        // 解析 Deal ID 和 Task ID
        String dealId = extractDealId(output);
        logger.info("Deal created successfully: {}", dealId);
        
        return dealId;
    }
    
    /**
     * 查询任务状态
     * @param taskId 任务ID
     * @return 任务状态信息
     */
    public IexecTaskStatus getTaskStatus(String taskId) {
        try {
            logger.debug("Querying task status for: {}", taskId);
            
            String output = executeCommand(String.format(
                "iexec task show %s --chain %s --raw", taskId, chain
            ));
            
            // 解析 JSON 输出
            JsonNode taskNode = objectMapper.readTree(output);
            
            IexecTaskStatus status = new IexecTaskStatus();
            status.setTaskId(taskId);
            status.setStatus(taskNode.path("statusName").asText());
            status.setDealId(taskNode.path("dealid").asText());
            
            // 解析结果信息
            JsonNode results = taskNode.path("results");
            if (!results.isMissingNode()) {
                status.setResultStorage(results.path("storage").asText());
                status.setResultLocation(results.path("location").asText());
            }
            
            logger.debug("Task {} status: {}", taskId, status.getStatus());
            return status;
            
        } catch (Exception e) {
            logger.error("Failed to get task status for: {}", taskId, e);
            return null;
        }
    }
    
    /**
     * 下载任务结果
     * @param taskId 任务ID
     * @return 结果文件路径
     */
    public String downloadTaskResult(String taskId) throws IOException, InterruptedException {
        logger.info("Downloading result for task: {}", taskId);
        
        String output = executeCommand(String.format(
            "iexec task download %s --chain %s", taskId, chain
        ));
        
        // 结果保存在 workspace/.iexec/tasks/{taskId}/
        String resultPath = Paths.get(workspaceDir, ".iexec", "tasks", taskId).toString();
        logger.info("Task result downloaded to: {}", resultPath);
        
        return resultPath;
    }
    
    /**
     * 读取任务结果内容
     * @param taskId 任务ID
     * @return 结果内容
     */
    public String readTaskResultContent(String taskId) {
        try {
            String resultPath = Paths.get(workspaceDir, ".iexec", "tasks", taskId, "result.txt").toString();
            return Files.readString(Paths.get(resultPath));
        } catch (IOException e) {
            logger.error("Failed to read result for task: {}", taskId, e);
            return null;
        }
    }
    
    // ==================== 工具方法 ====================
    
    /**
     * 执行 Shell 命令
     */
    private String executeCommand(String command) throws IOException, InterruptedException {
        logger.debug("Executing command: {}", command);
        
        ProcessBuilder pb = new ProcessBuilder();
        
        // Windows 和 Linux/Mac 的命令执行方式不同
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            pb.command("cmd.exe", "/c", command);
        } else {
            pb.command("bash", "-c", command);
        }
        
        pb.directory(new File(workspaceDir));
        pb.redirectErrorStream(true);
        
        Process process = pb.start();
        
        // 读取输出
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                logger.trace("Command output: {}", line);
            }
        }
        
        // 等待命令完成
        boolean finished = process.waitFor(commandTimeout, TimeUnit.SECONDS);
        if (!finished) {
            process.destroy();
            throw new InterruptedException("Command timeout after " + commandTimeout + " seconds");
        }
        
        int exitCode = process.exitValue();
        if (exitCode != 0) {
            logger.error("Command failed with exit code {}: {}", exitCode, output);
            throw new IOException("Command failed with exit code " + exitCode);
        }
        
        return output.toString();
    }
    
    /**
     * 从输出中提取以太坊地址
     */
    private String extractAddress(String output, String context) {
        Pattern pattern = Pattern.compile("(0x[a-fA-F0-9]{40})");
        Matcher matcher = pattern.matcher(output);
        if (matcher.find()) {
            return matcher.group(1);
        }
        logger.warn("Failed to extract address from context: {}", context);
        return null;
    }
    
    /**
     * 从输出中提取订单哈希
     */
    private String extractOrderHash(String output) {
        Pattern pattern = Pattern.compile("orderHash:\\s*(0x[a-fA-F0-9]{64})");
        Matcher matcher = pattern.matcher(output);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
    
    /**
     * 从输出中提取 Deal ID
     */
    private String extractDealId(String output) {
        Pattern pattern = Pattern.compile("dealid:\\s*(0x[a-fA-F0-9]{64})");
        Matcher matcher = pattern.matcher(output);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
    
    /**
     * 提取第一个可用的工作池订单
     */
    private String extractFirstWorkerpoolOrder(String output) {
        Pattern pattern = Pattern.compile("orderHash:\\s*(0x[a-fA-F0-9]{64})");
        Matcher matcher = pattern.matcher(output);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
    
    /**
     * 更新订单 JSON 文件
     */
    private void updateOrderJson(String filePath, String appAddress, String price, int volume) throws IOException {
        // 实际实现中应该读取并修改 JSON 文件
        // 这里简化处理
        logger.debug("Updating order JSON at: {}", filePath);
    }
    
    /**
     * 更新请求订单 JSON 文件
     */
    private void updateRequestOrderJson(String filePath, String appAddress, String params) throws IOException {
        logger.debug("Updating request order JSON at: {}", filePath);
    }
    
    /**
     * iExec 任务状态信息类
     */
    public static class IexecTaskStatus {
        private String taskId;
        private String status;  // ACTIVE, REVEALING, COMPLETED, FAILED
        private String dealId;
        private String resultStorage;  // ipfs
        private String resultLocation;  // IPFS Hash
        
        // Getters and Setters
        public String getTaskId() {
            return taskId;
        }
        
        public void setTaskId(String taskId) {
            this.taskId = taskId;
        }
        
        public String getStatus() {
            return status;
        }
        
        public void setStatus(String status) {
            this.status = status;
        }
        
        public String getDealId() {
            return dealId;
        }
        
        public void setDealId(String dealId) {
            this.dealId = dealId;
        }
        
        public String getResultStorage() {
            return resultStorage;
        }
        
        public void setResultStorage(String resultStorage) {
            this.resultStorage = resultStorage;
        }
        
        public String getResultLocation() {
            return resultLocation;
        }
        
        public void setResultLocation(String resultLocation) {
            this.resultLocation = resultLocation;
        }
        
        public boolean isCompleted() {
            return "COMPLETED".equalsIgnoreCase(status);
        }
        
        public boolean isFailed() {
            return "FAILED".equalsIgnoreCase(status) || "TIMEOUT".equalsIgnoreCase(status);
        }
    }
}
