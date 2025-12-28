# iExec Compute Market 后端手动部署指南

完整的手动部署流程，从零开始搭建运行环境。

---

## 📋 前置要求

### 系统要求
- **操作系统**：Windows 10/11, Linux, macOS
- **Java**：JDK 17 或更高版本
- **Maven**：3.6+ 
- **Node.js**：14.0+ （用于安装 iExec CLI）
- **网络**：需要能够访问以太坊测试网和 iExec 网络

### 检查已安装工具

```powershell
# Windows PowerShell

# 检查 Java 版本
java -version
# 应显示：java version "17.x.x" 或更高

# 检查 Maven 版本
mvn -version
# 应显示：Apache Maven 3.x.x

# 检查 Node.js 版本
node -v
# 应显示：v14.x.x 或更高

# 检查 npm 版本
npm -v
# 应显示：6.x.x 或更高
```

---

## 第一步：安装 iExec CLI

### Windows
```powershell
npm install -g iexec
```

### Linux/Mac
```bash
sudo npm install -g iexec
```

### 验证安装
```powershell
iexec --version
# 应显示：8.x.x 或更高版本
```

---

## 第二步：创建 iExec 工作目录

```powershell
# 创建工作目录
New-Item -ItemType Directory -Path "D:\practiecCode\java\compute-market\iexec-workspace" -Force

# 进入工作目录
cd D:\practiecCode\java\compute-market\iexec-workspace
```

---

## 第三步：初始化 iExec 环境

```powershell
# 在工作目录中执行
iexec init --skip-wallet
```

**预期输出：**
```
✔ iExec project initialized
✔ "iexec.json" created
✔ "chain.json" created
```

**生成的文件：**
- `iexec.json` - iExec 项目配置
- `chain.json` - 区块链网络配置

---

## 第四步：创建 iExec 钱包

### 4.1 创建新钱包

```powershell
iexec wallet create
```

**交互流程：**
```
? Please choose a password for wallet encryption: [输入密码]
  建议：MyWallet@2025!（至少8位，包含大小写字母、数字、特殊符号）

? Please confirm your password: [再次输入相同密码]

✔ Wallet created!
✔ Address: 0xYourWalletAddress...
✔ Wallet file saved to: .secrets/wallet/wallet.json
```

**重要说明：**
- 此密码仅用于本地加密钱包文件
- **务必记住此密码**，忘记后需要重新创建钱包
- 密码不是私钥，也不会上传到区块链

### 4.2 查看钱包信息

```powershell
iexec wallet show
```

输入刚才设置的密码后显示：
```
ℹ Current wallet address: 0xYourWalletAddress
ℹ Wallet file: .secrets/wallet/wallet.json
```

### 4.3 获取钱包私钥（关键步骤！）

```powershell
iexec wallet show --show-private-key
```

**输入密码后会显示：**
```
ℹ Wallet address: 0xYourWalletAddress
ℹ Private key: 0x1234567890abcdef... (64位十六进制字符)
```

**⚠️ 重要安全提示：**
1. **立即复制并保存私钥到安全的地方**
2. 私钥等同于钱包所有权，任何人获得私钥就能控制你的资产
3. 不要将私钥分享给任何人
4. 不要将私钥提交到 Git 仓库
5. 建议保存在密码管理器或加密文档中

**备份方式：**
```
选项1：手写在纸上，锁在保险柜
选项2：保存到 KeePass/1Password 等密码管理器
选项3：保存到加密 USB 设备
```

---

## 第五步：配置后端环境变量

### 5.1 进入后端目录

```powershell
cd D:\practiecCode\java\compute-market\compute-market\backend
```

### 5.2 创建 .env 文件

```powershell
# 使用记事本创建文件
notepad .env
```

### 5.3 填写环境变量配置

在 `.env` 文件中输入以下内容：

```properties
# ==========================================
# iExec 配置
# ==========================================

# iExec 工作目录（绝对路径）
IEXEC_WORKSPACE=D:\practiecCode\java\compute-market\iexec-workspace

# iExec 钱包私钥（从上一步获取）
# 格式：0x 开头，64位十六进制字符
IEXEC_WALLET_KEY=0x你的私钥粘贴在这里

# ==========================================
# 数据库配置
# ==========================================

# H2 内存数据库连接
SPRING_DATASOURCE_URL=jdbc:h2:mem:iexecdb

# ==========================================
# 区块链配置
# ==========================================

# Infura RPC 端点（需要注册获取 PROJECT_ID）
# 注册地址：https://infura.io/
WEB3J_CLIENT_ADDRESS=https://sepolia.infura.io/v3/YOUR_INFURA_PROJECT_ID

# 智能合约地址（部署后填写）
CONTRACT_ADDRESS=0xYOUR_CONTRACT_ADDRESS_HERE
```

**配置说明：**

| 变量名 | 说明 | 示例值 |
|--------|------|--------|
| `IEXEC_WORKSPACE` | iExec 工作目录路径 | `D:\practiecCode\java\compute-market\iexec-workspace` |
| `IEXEC_WALLET_KEY` | 钱包私钥 | `0x1234...abcd`（64位十六进制） |
| `SPRING_DATASOURCE_URL` | 数据库连接 | `jdbc:h2:mem:iexecdb`（开发环境） |
| `WEB3J_CLIENT_ADDRESS` | 以太坊 RPC 节点 | Infura/Alchemy 提供的 URL |
| `CONTRACT_ADDRESS` | 合约地址 | 部署后获得的合约地址 |

### 5.4 保存文件

按 `Ctrl+S` 保存，然后关闭记事本。

### 5.5 验证 .env 文件

```powershell
# 查看文件内容（隐藏私钥）
Get-Content .env | Select-String -Pattern "IEXEC_WORKSPACE|CONTRACT_ADDRESS"
```

---

## 第六步：获取测试网代币（可选但推荐）

为了能够在 iExec 网络上提交真实任务，需要获取测试网代币。

### 6.1 获取 Sepolia ETH

⚠️ **重要提示**：许多水龙头要求主网余额（0.001 ETH）以防止滥用。如果遇到此问题，请尝试以下方法：

**方式1：Google Cloud Sepolia Faucet（推荐）**
1. 访问：https://cloud.google.com/application/web3/faucet/ethereum/sepolia
2. 使用 Google 账号登录
3. 输入钱包地址：`0x5f5b0900BF7D55fD4ADE4F419039054d7eE89e3F`
4. 每 24 小时可领取 0.5 Sepolia ETH
5. **无需主网余额要求**

**方式2：Infura Sepolia Faucet**
1. 访问：https://www.infura.io/faucet/sepolia
2. 登录或注册 Infura 账号
3. 输入钱包地址
4. 每 24 小时可领取 0.5 Sepolia ETH
5. **无需主网余额要求**

**方式3：QuickNode Faucet**
1. 访问：https://faucet.quicknode.com/ethereum/sepolia
2. 输入钱包地址
3. 完成简单验证
4. 领取测试 ETH

**方式4：PoW Faucet（通过挖矿获取）**
1. 访问：https://sepolia-faucet.pk910.de/
2. 输入钱包地址
3. 点击 "Start Mining"
4. 在浏览器中挖矿几分钟即可获得测试币
5. **不需要任何前置条件**

**方式5：Alchemy Faucet（需要主网余额）**
1. 访问：https://www.alchemy.com/faucets/ethereum-sepolia
2. 连接钱包或输入地址
3. 需要主网至少 0.001 ETH
4. 领取测试 ETH

**方式6：Sepolia Faucet（需要主网余额）**
1. 访问：https://sepoliafaucet.com/
2. 输入钱包地址
3. 需要主网至少 0.001 ETH
4. 完成验证后领取测试币

**方式7：社区水龙头**
- 加入 [Ethereum Discord](https://discord.gg/ethereum-org) 或相关 Telegram 群组
- 在水龙头频道请求测试币
- 提供你的钱包地址：`0x5f5b0900BF7D55fD4ADE4F419039054d7eE89e3F`

**方式8：跳过此步骤（仅开发测试）**
- 如果只是测试后端功能，可以暂时跳过
- 后端应用可以正常启动和测试 API
- 仅在需要提交真实 iExec 任务时才需要测试币

### 6.2 获取 iExec RLC（测试币）

⚠️ **重要说明**：iExec 官方水龙头目前可能无法访问。以下是替代方案：

**方式1：跳过 RLC 代币（推荐用于开发阶段）**
- ✅ 后端应用可以完全正常启动和运行
- ✅ 所有 API 端点可以正常测试
- ✅ 数据库和监控功能完全可用
- ⚠️ 仅在需要提交**真实** iExec 计算任务时才需要 RLC
- 💡 在开发和测试阶段，可以使用模拟数据测试所有功能

**方式2：通过 iExec CLI 购买 RLC（需要 ETH）**
```powershell
# 在 iExec workspace 目录执行
cd D:\practiecCode\java\compute-market\iexec-workspace

# 查看账户信息
iexec account show --chain bellecour

# 注意：购买 RLC 需要先有 ETH 余额
# Bellecour 链上需要使用 xDAI（跨链桥转换）
```

**方式3：从交易所获取 RLC（生产环境）**
1. 在支持 RLC 的交易所购买（如 Binance, Coinbase）
2. 提币到 Ethereum 主网地址：`0x5f5b0900BF7D55fD4ADE4F419039054d7eE89e3F`
3. 使用跨链桥转移到 iExec Bellecour 链

**方式4：加入 iExec 社区寻求帮助**
- Discord: https://discord.gg/iexec
- Telegram: https://t.me/iexec_discussion
- 说明你是开发者，请求少量测试 RLC
- 提供你的钱包地址：`0x5f5b0900BF7D55fD4ADE4F419039054d7eE89e3F`

**方式5：iExec 官方水龙头（如果恢复）**
- 原网址：https://faucet.iex.ec/
- 新网址：可能已迁移，关注官方公告
- 备选：https://faucets.iex.ec/ 或社区提供的水龙头

---

**🎯 推荐开发流程：**

```
阶段1：本地开发（0 RLC）
  ✅ 启动后端应用
  ✅ 测试所有 API 端点
  ✅ 验证数据库功能
  ✅ 测试监控和调度算法
  ✅ 使用模拟数据测试业务逻辑

阶段2：集成测试（需要 RLC）
  ⚠️ 连接真实 iExec 网络
  ⚠️ 提交实际计算任务
  ⚠️ 测试端到端流程

阶段3：生产部署（购买 RLC）
  💰 从交易所购买 RLC
  🔄 转账到部署地址
  🚀 上线运行
```
 --chain bellecour
```

输入密码后会显示：
```
ℹ Current wallet address: 0x5f5b0900BF7D55fD4ADE4F419039054d7eE89e3F
ℹ Wallet balance: 0.5 ETH
ℹ RLC balance: 100 RLC
```

**如果余额为 0：**
- 不影响后端应用的启动和测试
- 可以测试所有本地 API 端点
- 仅在需要提交真实 iExec 计算任务时才需要代币

**在线查看余额（无需密码）：**
- Sepolia 浏览器：https://sepolia.etherscan.io/address/0x5f5b0900BF7D55fD4ADE4F419039054d7eE89e3F
- iExec 浏览器：https://explorer.iex.ec/bellecour/address/0x5f5b0900BF7D55fD4ADE4F419039054d7eE89e3F

输入密码后会显示：
```
ℹ Wallet balance: 0.5 ETH
ℹ RLC balance: 100 RLC
```

---

## 第七步：配置 Infura 项目（可选）

如果需要连接以太坊主网或测试网，需要配置 Infura。

### 7.1 注册 Infura 账号

1. 访问：https://infura.io/
2. 点击 "Sign Up" 注册账号
3. 验证邮箱

### 7.2 创建项目

1. 登录后点击 "Create New API Key"
2. 选择产品：**Ethereum**
3. 输入项目名称：`iExec-Compute-Market`
4. 点击 "Create"

### 7.3 获取 Project ID

在项目详情页面，复制 **Project ID**：
```
示例：9a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p
```

### 7.4 更新 .env 文件

```powershell
notepad .env
```

修改以下行：
```properties
WEB3J_CLIENT_ADDRESS=https://sepolia.infura.io/v3/你的ProjectID
```

保存并关闭。

---

## 第八步：编译后端项目

```powershell
# 确保在 backend 目录
cd D:\practiecCode\java\compute-market\compute-market\backend

# 清理并编译项目
mvn clean compile
```

**预期输出：**
```
[INFO] Scanning for projects...
[INFO] Building iExec 0.0.1-SNAPSHOT
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- maven-clean-plugin:3.2.0:clean (default-clean) @ iExec ---
[INFO] --- maven-resources-plugin:3.3.1:resources (default-resources) @ iExec ---
[INFO] Copying 1 resource
[INFO] --- maven-compiler-plugin:3.14.1:compile (default-compile) @ iExec ---
[INFO] Compiling 20 source files to target\classes
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

如果出现错误，检查：
- Java 版本是否正确（`java -version`）
- Maven 配置是否正确（`mvn -version`）
- 网络连接是否正常（Maven 需要下载依赖）

---

## 第九步：运行后端应用

### 9.1 启动应用

```powershell
mvn spring-boot:run
```

### 9.2 观察启动日志

**成功启动的标志：**
```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/

 :: Spring Boot ::                (v3.5.9)

2025-12-23 12:00:00 - Starting IExecApplication using Java 21.0.7
2025-12-23 12:00:02 - Tomcat started on port 8080 (http) with context path '/api'
2025-12-23 12:00:02 - Started IExecApplication in 6.5 seconds
2025-12-23 12:00:02 - Starting task monitoring cycle
2025-12-23 12:00:02 - No running tasks to monitor
```

**关键信息：**
- ✅ Spring Boot 版本：3.5.9
- ✅ 运行端口：8080
- ✅ 上下文路径：/api
- ✅ 启动时间：约 6-8 秒
- ✅ 任务监控：已启动

### 9.3 验证数据库初始化

查看日志中的数据库表创建信息：
```
Hibernate: create table reputation (...)
Hibernate: create table task_history (...)
Hibernate: create table task_entity (...)
```

应该看到 3 个表被成功创建。

---

## 第十步：测试 API 端点

打开新的 PowerShell 窗口（不要关闭运行应用的窗口）。

### 10.1 测试健康检查

```powershell
# 使用 curl 测试（Windows 自带）
curl http://localhost:8080/api/actuator/health
```

**预期响应：**
```json
{"status":"UP"}
```

### 10.2 测试任务监控统计

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/monitor/stats" -Method Get | ConvertTo-Json
```

**预期响应：**
```json
{
  "runningTasksCount": 0,
  "timeoutTasksCount": 0,
  "completedTasksCount": 0,
  "failedTasksCount": 0
}
```

### 10.3 测试调度策略

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/monitor/strategy" -Method Get | ConvertTo-Json
```

**预期响应：**
```json
{
  "throughput": 0.0,
  "averageResponseTime": 0.0,
  "recommendation": "LOW_LOAD",
  "maxConcurrentTasks": 10
}
```

### 10.4 测试 H2 数据库控制台

1. 打开浏览器
2. 访问：http://localhost:8080/api/h2-console
3. 连接信息：
   - **JDBC URL**: `jdbc:h2:mem:iexecdb`
   - **User Name**: `sa`
   - **Password**: （留空）
4. 点击 "Connect"

可以看到三张表：
- `REPUTATION` - 用户信誉表
- `TASK_ENTITY` - 任务实体表
- `TASK_HISTORY` - 任务历史表

---

## 第十一步：停止应用

在运行应用的 PowerShell 窗口中按 `Ctrl+C`：

```
Terminate batch job (Y/N)? Y

[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 2:30 min
[INFO] Finished at: 2025-12-23T12:05:00+08:00
[INFO] ------------------------------------------------------------------------
```

---

## 🎯 完整命令速查表

### 一键启动（环境已配置）

```powershell
# 1. 进入后端目录
cd D:\practiecCode\java\compute-market\compute-market\backend

# 2. 启动应用
mvn spring-boot:run
```

### 环境检查命令

```powershell
# 检查 Java
java -version

# 检查 Maven
mvn -version

# 检查 iExec CLI
iexec --version

# 检查钱包
cd D:\practiecCode\java\compute-market\iexec-workspace
iexec wallet show

# 查看环境变量
cd D:\practiecCode\java\compute-market\compute-market\backend
Get-Content .env
```

### 常用 iExec 命令

```powershell
# 进入工作目录
cd D:\practiecCode\java\compute-market\iexec-workspace

# 查看钱包余额
iexec wallet show

# 查看钱包私钥
iexec wallet show --show-private-key

# 初始化项目
iexec init --skip-wallet

# 查看账户信息
iexec account show
```

---

## ⚙️ 高级配置

### 修改监控间隔

编辑 `application.properties`：
```properties
# 监控间隔（毫秒）默认 30 秒
task.monitor.interval=30000

# 修改为 60 秒
task.monitor.interval=60000
```

### 修改任务超时时间

```properties
# 任务超时（毫秒）默认 1 小时
task.monitor.timeout=3600000

# 修改为 2 小时
task.monitor.timeout=7200000
```

### 切换 iExec 网络

```properties
# 使用测试网（推荐开发环境）
iexec.chain=viviani

# 使用主网（生产环境）
iexec.chain=bellecour
```

### 生产环境数据库配置

替换 H2 内存数据库为 MySQL：

**1. 添加 MySQL 依赖到 pom.xml**
```xml
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.33</version>
</dependency>
```

**2. 修改 .env 文件**
```properties
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/iexecdb
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=your_password
```

**3. 更新 application.properties**
```properties
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect
```

---

## 🐛 常见问题排查

### 问题1：应用启动失败 - 端口被占用

**错误信息：**
```
Web server failed to start. Port 8080 was already in use.
```

**解决方法：**
```powershell
# 查找占用端口的进程
netstat -ano | findstr :8080

# 结束进程（替换 PID 为实际进程ID）
taskkill /PID <PID> /F

# 或修改端口
# 在 application.properties 中添加：
server.port=8081
```

### 问题2：找不到 iExec CLI

**错误信息：**
```
'iexec' 不是内部或外部命令
```

**解决方法：**
```powershell
# 重新安装 iExec CLI
npm install -g iexec

# 验证安装
iexec --version

# 如果还不行，检查 npm 全局路径
npm config get prefix

# 将路径添加到系统 PATH 环境变量
```

### 问题3：Maven 依赖下载失败

**错误信息：**
```
Failed to execute goal ... Could not resolve dependencies
```

**解决方法：**
```powershell
# 清理 Maven 缓存
mvn clean

# 强制更新依赖
mvn clean install -U

# 如果网络问题，配置国内镜像
# 编辑 %USERPROFILE%\.m2\settings.xml
```

### 问题4：数据库连接失败

**错误信息：**
```
Failed to configure a DataSource
```

**解决方法：**
```powershell
# 检查 .env 文件是否存在
Test-Path .env

# 检查环境变量是否加载
# 在 application.properties 中添加：
spring.config.import=optional:file:.env[.properties]
```

### 问题5：iExec 钱包密码忘记

**解决方法：**
- ❌ 无法恢复密码
- ✅ 如果有私钥备份，可以导入：
  ```powershell
  iexec wallet import <your-private-key>
  ```
- ✅ 如果没有备份，需要重新创建钱包

### 问题6：Infura 请求限额

**错误信息：**
```
daily request count exceeded, request rate limited
```

**解决方法：**
- 升级 Infura 计划
- 或使用其他 RPC 提供商（Alchemy, QuickNode）

---

## 📚 API 端点清单

### 任务管理
- `GET /api/tasks` - 获取所有任务
- `GET /api/tasks/{id}` - 获取指定任务
- `POST /api/tasks` - 创建新任务（需要智能合约触发）

### 监控统计
- `GET /api/monitor/stats` - 获取监控统计
- `GET /api/monitor/strategy` - 获取调度策略
- `GET /api/monitor/predict/{serviceId}` - 预测完成时间
- `GET /api/monitor/resources/{serviceId}` - 预测资源需求
- `GET /api/monitor/compare/{serviceId}` - 性能对比分析

### 数据库管理
- `GET /api/h2-console` - H2 数据库控制台

### 健康检查
- `GET /api/actuator/health` - 应用健康状态

---

## 🔐 安全建议

### 开发环境
- ✅ 使用测试网钱包
- ✅ 使用 .env 文件存储敏感信息
- ✅ 将 .env 添加到 .gitignore
- ✅ 使用测试网代币

### 生产环境
- ✅ 使用环境变量或密钥管理服务（如 AWS Secrets Manager）
- ✅ 启用 HTTPS
- ✅ 配置防火墙规则
- ✅ 使用硬件钱包或多签钱包
- ✅ 定期备份数据库
- ✅ 监控异常活动
- ✅ 设置访问控制和身份验证

---

## 📞 获取帮助

### 官方文档
- [iExec 开发者文档](https://docs.iex.ec/)
- [Spring Boot 文档](https://spring.io/projects/spring-boot)
- [Web3j 文档](https://docs.web3j.io/)

### 社区支持
- [iExec Discord](https://discord.gg/iexec)
- [iExec GitHub](https://github.com/iExecBlockchainComputing)
- [Stack Overflow](https://stackoverflow.com/questions/tagged/iexec)

### 项目相关
- GitHub Issues：提交 Bug 或功能请求
- 项目文档：查看 B_PART_IMPLEMENTATION.md

---

## ✅ 部署检查清单

完成以下所有步骤后，系统应该可以正常运行：

- [ ] Java 17+ 已安装并配置
- [ ] Maven 3.6+ 已安装
- [ ] Node.js 和 npm 已安装
- [ ] iExec CLI 已全局安装
- [ ] iExec 工作目录已创建
- [ ] iExec 环境已初始化（chain.json 存在）
- [ ] iExec 钱包已创建
- [ ] 钱包私钥已安全备份
- [ ] .env 文件已创建并配置
- [ ] 环境变量已正确设置
- [ ] 测试网代币已获取（可选）
- [ ] Infura 项目已创建（可选）
- [ ] Maven 依赖已下载
- [ ] 应用可成功编译
- [ ] 应用可成功启动
- [ ] API 端点可正常访问
- [ ] H2 控制台可正常访问
- [ ] 任务监控服务正常运行

**恭喜！** 🎉 如果以上所有项都已完成，你的 iExec Compute Market 后端已经成功部署并运行！
