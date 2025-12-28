# 🚀 运行脚本使用指南

本目录包含了自动化运行和测试的 PowerShell 脚本。

---

## 📁 脚本文件列表

| 脚本文件 | 功能描述 |
|---------|---------|
| `quick-start.ps1` | 🎯 一键启动（推荐） |
| `setup-iexec.ps1` | ⚙️ iExec 环境初始化（首次运行） |
| `run.ps1` | ▶️ 启动 Spring Boot 应用 |
| `test-api.ps1` | 🧪 测试 API 接口 |

---

## 🎯 快速开始（首次使用）

### 1️⃣ 一键启动（最简单）

```powershell
cd D:\practiecCode\java\compute-market\compute-market\backend
.\quick-start.ps1
```

脚本会询问是否需要初始化 iExec 环境，选择 `Y` 即可自动完成所有设置。

---

## ⚙️ 详细使用步骤

### **步骤 1：初始化 iExec 环境（首次运行）**

```powershell
.\setup-iexec.ps1
```

**脚本会自动：**
- ✅ 检查 iExec CLI 安装
- ✅ 创建工作目录
- ✅ 创建 iExec 钱包（如果不存在）
- ✅ 设置环境变量
- ✅ 初始化 iExec 配置
- ✅ 创建 .env 文件

**输出示例：**
```
========================================
   iExec Environment Setup Script
========================================

[1/6] Checking iExec CLI installation...
✓ iExec CLI installed: 8.x.x

[2/6] Creating workspace directory...
✓ Workspace created at: D:\practiecCode\java\compute-market\iexec-workspace

[3/6] Checking iExec wallet...
No wallet found. Creating new wallet...
✓ Wallet created!
  Address: 0x1234567890abcdef1234567890abcdef12345678
  Private Key: 0xabcdef12...5678

⚠️  IMPORTANT: Please backup your private key!
Private Key: 0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890

[4/6] Setting environment variables...
✓ IEXEC_WORKSPACE = D:\practiecCode\java\compute-market\iexec-workspace
✓ IEXEC_WALLET_KEY = 0xabcdef12...

[5/6] Initializing iExec configuration...
✓ iExec initialized

[6/6] Creating .env file...
✓ .env file created

========================================
   Setup Complete!
========================================
```

**重要提醒：**
- 📝 请务必备份显示的私钥！
- 💰 需要从测试网水龙头获取测试资金

---

### **步骤 2：获取测试资金**

使用脚本结束后显示的钱包地址，从以下水龙头获取测试资金：

1. **Sepolia ETH 水龙头**
   ```
   https://sepoliafaucet.com/
   ```

2. **iExec RLC 水龙头（Viviani 测试网）**
   ```
   https://faucet.iex.ec/
   ```

---

### **步骤 3：启动应用**

```powershell
.\run.ps1
```

**脚本会自动：**
- ✅ 加载环境变量
- ✅ 检查 Maven 安装
- ✅ 编译项目
- ✅ 启动 Spring Boot 应用

**输出示例：**
```
========================================
   Starting iExec Compute Market
========================================

[1/4] Loading environment variables...
  ✓ Loaded: IEXEC_WORKSPACE
  ✓ Loaded: IEXEC_WALLET_KEY
  ✓ IEXEC_WORKSPACE = D:\practiecCode\java\compute-market\iexec-workspace

[2/4] Checking Maven installation...
  ✓ Maven found: Apache Maven 3.9.x

[3/4] Compiling project...
  ✓ Compilation successful

[4/4] Starting Spring Boot application...

========================================
   Application Starting...
========================================

📋 API Documentation:
  - Base URL: http://localhost:8080/api
  - H2 Console: http://localhost:8080/api/h2-console

📊 Monitoring Endpoints:
  - Stats: http://localhost:8080/api/monitor/stats
  - Strategy: http://localhost:8080/api/monitor/strategy

Press Ctrl+C to stop the application
========================================

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.5.9)
```

应用启动后，可以访问：
- **API 端点**：http://localhost:8080/api
- **H2 数据库控制台**：http://localhost:8080/api/h2-console

---

### **步骤 4：测试 API（可选）**

打开新的 PowerShell 窗口：

```powershell
.\test-api.ps1
```

**脚本会测试：**
- ✅ 健康检查
- ✅ 监控统计
- ✅ 调度策略
- ✅ 完成时间预测
- ✅ 资源需求预测
- ✅ 性能对比分析
- ✅ 任务列表

**输出示例：**
```
========================================
   API Testing Script
========================================

Testing: Health Check
  ✓ Success
{
  "status": "UP"
}

Testing: Monitoring Statistics
  ✓ Success
{
  "runningTasksCount": 0,
  "completedTasksCount": 0,
  "failedTasksCount": 0
}

========================================
   Testing Complete!
========================================
```

---

## 🔧 手动命令（高级用户）

如果你想手动执行各个步骤：

### **创建 iExec 钱包**
```powershell
cd D:\practiecCode\java\compute-market\iexec-workspace
iexec wallet create
```

### **设置环境变量**
```powershell
$env:IEXEC_WORKSPACE="D:\practiecCode\java\compute-market\iexec-workspace"
$env:IEXEC_WALLET_KEY="0x你的私钥"
```

### **初始化 iExec**
```powershell
cd D:\practiecCode\java\compute-market\iexec-workspace
iexec init --skip-wallet
```

### **启动应用**
```powershell
cd D:\practiecCode\java\compute-market\compute-market\backend
mvn spring-boot:run
```

---

## ⚠️ 常见问题

### 1. **脚本无法执行**
```powershell
# 问题：提示"无法加载文件，因为在此系统上禁止运行脚本"
# 解决方案：设置执行策略
Set-ExecutionPolicy -Scope CurrentUser -ExecutionPolicy RemoteSigned
```

### 2. **Maven 未安装**
```powershell
# 下载并安装 Maven
# https://maven.apache.org/download.cgi
# 添加到系统 PATH
```

### 3. **iExec CLI 未安装**
```powershell
npm install -g iexec
```

### 4. **端口 8080 被占用**
```powershell
# 修改 application.properties
server.port=8081
```

### 5. **钱包已存在但需要重新输入私钥**
```powershell
# 运行 setup 脚本时会提示输入
.\setup-iexec.ps1
```

---

## 📚 其他资源

- **项目文档**：[README.md](./README.md)
- **B部分实现文档**：[B_PART_IMPLEMENTATION.md](./B_PART_IMPLEMENTATION.md)
- **iExec 官方文档**：https://docs.iex.ec/
- **Spring Boot 文档**：https://spring.io/projects/spring-boot

---

## 🎓 学习建议

1. **首次运行**：使用 `quick-start.ps1` 快速上手
2. **理解流程**：查看各个独立脚本了解每个步骤
3. **调试问题**：手动执行命令逐步排查
4. **生产部署**：参考脚本修改为生产环境配置

---

**祝你使用愉快！** 🚀
