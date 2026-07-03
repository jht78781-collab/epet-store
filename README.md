# 宠物商店课程项目

这是根据课程 PDF 和上机素材补齐的 Java 控制台版宠物商店项目，包含实体类、DAO 层、业务逻辑层、测试入口和数据库脚本。

## 项目结构

```text
src/cn/turing/entity          实体类
src/cn/turing/dao             DAO 接口和 BaseDao
src/cn/turing/dao/impl        DAO 实现类
src/cn/turing/manager         业务接口
src/cn/turing/manager/impl    业务实现类
src/cn/turing/test            控制台测试入口
src/cn/turing/web             前端 HTTP/API 入口
web                           前端页面、样式、脚本和图片资源
database/epet.sql             MySQL 建库和测试数据
database/epet_supabase.sql    Supabase/PostgreSQL 建表和测试数据
Dockerfile                    Render 后端容器部署
vercel.json                   Vercel 前端部署配置
```

## 运行步骤

1. 在 MySQL 中执行 `database/epet.sql`。
2. 准备 MySQL JDBC 驱动，例如 `mysql-connector-j-8.x.x.jar`。
3. 编译：

```bash
javac -encoding UTF-8 -d out $(find src -name "*.java")
```

在 Windows PowerShell 中可使用：

```powershell
javac -encoding UTF-8 -d out (Get-ChildItem -Recurse src -Filter *.java | ForEach-Object FullName)
```

4. 运行：

```bash
java -cp "out;mysql-connector-j-8.x.x.jar" cn.turing.test.PetStoreTest
```

默认数据库配置为 `root/root`，数据库名为 `epet`。如需修改，可通过 JVM 参数覆盖：

```bash
java -Depet.db.user=root -Depet.db.password=你的密码 -cp "out;mysql-connector-j-8.x.x.jar" cn.turing.test.PetStoreTest
```

## 前端运行

编译后启动 Web 服务：

```powershell
java -cp "out;lib\mysql-connector-j-8.4.0.jar" cn.turing.web.PetStoreWebServer
```

Windows 下也可以直接运行：

```powershell
.\run-web.cmd
```

浏览器打开：

```text
http://127.0.0.1:8080/
```

如需修改端口或数据库密码：

```powershell
java -Depet.web.port=8081 -Depet.db.password=你的密码 -cp "out;lib\mysql-connector-j-8.4.0.jar" cn.turing.web.PetStoreWebServer
```

测试账号：

```text
tuling / 123456
itboy  / 123456
```

## Vercel + Render + Supabase 部署

Supabase 使用 PostgreSQL，不是 MySQL。本项目已支持通过 JDBC 连接 PostgreSQL，并把购买逻辑从 MySQL 存储过程改成 Java 事务。

### 第一阶段：先上线前端

1. 把代码上传到 GitHub。
2. Vercel 连接 GitHub。
3. 选择这个项目，一键 Deploy。
4. 先得到前端网址，例如：

```text
https://xxx.vercel.app
```

第一次部署时，前端还没有后端地址，页面请求接口会失败，这是正常的。等第四阶段配置 `VITE_API_URL` 后重新部署即可。

### 第二阶段：上线后端

1. Render 连接 GitHub。
2. 创建 Web Service。
3. 选择这个项目。
4. 本项目后端是 Java，不是 Node。如果用 Docker 部署，Render 会读取项目根目录的 `Dockerfile`，不用填 `npm start`。

如果不用 Docker，Render 启动命令可参考：

```text
java -jar target/epet-store.jar
```

后端上线后会得到地址，例如：

```text
https://xxx.onrender.com
```

先检查健康接口：

```text
https://xxx.onrender.com/api/health
```

返回 `{"status":"ok"}` 说明后端服务已启动。

### 第三阶段：接数据库

1. Supabase 创建项目。
2. 打开 SQL Editor。
3. 执行 `database/epet_supabase.sql` 建表并插入测试数据。
4. 在 Supabase 项目页点击 Connect。
5. 复制 Session pooler connection string，形如：

```text
postgres://postgres.xxxxx:你的密码@aws-xxx.pooler.supabase.com:5432/postgres
```

Session pooler 是 IPv4 连接，适合 Render 这类后端服务。如果密码包含 `@`、`#`、`:` 等特殊字符，建议在 Supabase 重置成简单密码，或先做 URL 编码。

Render 后端环境变量：

```text
DATABASE_URL=你的 Supabase Session pooler connection string
EPET_DB_DRIVER=org.postgresql.Driver
EPET_CORS_ORIGINS=https://xxx.vercel.app
```

Render 官方推荐用环境变量保存数据库地址、密钥等敏感配置，不要把真实密码写进代码。

### 第四阶段：前后端连起来

前端请求后端的形式是：

```text
fetch("https://你的后端.onrender.com/api/xxx")
```

在 Vercel 里配置前端环境变量：

```text
VITE_API_URL=https://xxx.onrender.com
```

然后重新 Deploy。仓库根目录的 `vercel.json` 会执行：

```text
npm run build
```

构建时会把 `VITE_API_URL` 写入 `web/config.js`，前端就会把 `/api` 请求发到 Render 后端。Vercel 支持 GitHub 自动部署和环境变量配置，后续推送代码会自动触发部署。
