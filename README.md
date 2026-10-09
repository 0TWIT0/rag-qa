# RAG-QA — AI 知识库问答项目

上传文档 → 向量化入库 → 用户提问 → 大模型结合文档内容给出**带出处**的回答。

> 当前进度：**阶段 1~8 全部完成**。从上传文档到带出处回答的完整链路已打通，并能打成一个自包含的可执行 jar 交付。剩最后一步：git 打 tag 归档。

## 技术栈

- Java 17 + Spring Boot 3.4.5
- Spring Web / Spring Data JPA
- Spring AI 1.0.9（OpenAI 兼容，接 DeepSeek；Embedding 接 SiliconFlow）
- 向量存储：pgvector（PostgreSQL 扩展，向量落盘、重启不丢；由 Spring AI starter 自动配置）
- Apache PDFBox 3（PDF 文本提取）
- Apache POI 5（Word 文本提取）
- PostgreSQL 17 + pgvector（文档表与向量表同一个库）
- 前端：React 18 + Vite 5（开发时由 Vite 代理转发 `/api` 到后端，后端无需 CORS 配置）
- Maven（含 Maven Wrapper）

## 目录结构

```
RAG
├── pom.xml
├── mvnw / mvnw.cmd            # Maven Wrapper
├── api.http                   # IDEA HTTP 客户端脚本（接口验收用）
├── .gitignore
├── README.md
├── src
    ├── main
    │   ├── java/com/example/rag
    │   │   ├── RagApplication.java
    │   │   ├── config
    │   │   │   └── RetryConfig.java          # 大模型调用重试策略（含网络异常）
    │   │   ├── controller
    │   │   │   ├── HealthController.java     # GET  /api/health
    │   │   │   ├── ChatController.java       # POST /api/chat（阶段 2）
    │   │   │   ├── DocumentController.java   # 文档解析/入库/增删查接口
    │   │   │   ├── SearchController.java     # POST /api/search（阶段 4）
    │   │   │   └── RagController.java        # POST /api/rag/ask（阶段 5）
    │   │   ├── document
    │   │   │   ├── Chunk.java                # 文本片段（chunk）
    │   │   │   ├── DocumentReader.java       # 多格式文本提取
    │   │   │   ├── DocumentEntity.java       # 文档实体（PostgreSQL，阶段 6）
    │   │   │   ├── DocumentRepository.java   # JPA 仓库（阶段 6）
    │   │   │   ├── DocumentNotFoundException.java
    │   │   │   └── TextSplitter.java         # 固定长度 + 重叠切块
    │   │   ├── service
    │   │   │   ├── VectorStoreService.java   # 向量化入库 + 相似度检索（阶段 4）
    │   │   │   ├── RagService.java           # 检索 + 拼 prompt + 带出处回答（阶段 5）
    │   │   │   └── DocumentService.java      # 文档增删查 + 落库（阶段 6）
    │   │   └── dto
    │   │       ├── AskRequest.java
    │   │       ├── AskResponse.java
    │   │       ├── ChatRequest.java
    │   │       ├── DocumentDetail.java
    │   │       ├── DocumentParseResponse.java
    │   │       ├── DocumentSummary.java
    │   │       ├── IngestResponse.java
    │   │       ├── SearchQuery.java
    │   │       ├── SearchResult.java
    │   │       └── SearchResponse.java
    │   └── resources
    │       └── application.yml
    └── test
        └── java/com/example/rag
            ├── config
            │   └── RetryConfigTest.java
            ├── controller
            │   ├── HealthControllerTest.java
            │   ├── ChatControllerTest.java
            │   ├── DocumentControllerTest.java
            │   ├── SearchControllerTest.java
            │   └── RagControllerTest.java
            ├── document
            │   ├── TextSplitterTest.java
            │   └── DocumentReaderTest.java
            └── service
                ├── VectorStoreServiceTest.java
                ├── RagServiceTest.java
                └── DocumentServiceTest.java
└── frontend                       # React + Vite 前端（阶段 7）
    ├── package.json
    ├── vite.config.js             # /api → localhost:8080 代理（后端免 CORS）
    ├── index.html
    └── src
        ├── main.jsx
        ├── App.jsx                # 版面组装
        ├── api.js                 # 后端接口封装
        ├── styles.css             # 档案室风格样式
        └── components
            ├── UploadPanel.jsx    # 上传入库
            ├── DocumentList.jsx   # 在架档案 + 注销
            └── AskPanel.jsx       # 查询 + 引用条目
```

## 前置要求

- JDK 17（需设置 `JAVA_HOME`）
- Node.js 18+（前端需要，`node -v` 能打印版本号即可）
- PostgreSQL 17 + pgvector 扩展（推荐用 Docker 一键起，见下）
- DeepSeek API key（`/api/chat` 需要）
- SiliconFlow API key（`/api/documents/ingest`、`/api/search` 需要，用于 Embedding）

### 起 PostgreSQL（Docker）

```bash
docker run -d --name rag-pg -e POSTGRES_USER=raguser -e POSTGRES_PASSWORD=你的密码 \
  -e POSTGRES_DB=rag_db -p 5432:5432 pgvector/pgvector:pg17
```

官方 `pgvector/pgvector` 镜像已内置 vector 扩展，无需自己编译。应用启动时会自动执行 `CREATE EXTENSION IF NOT EXISTS vector` 并建好向量表，不用手动初始化。

## 如何运行

```bash
# 1. 设置环境变量（PowerShell 用 $env:XXX="..."）
export JAVA_HOME=/path/to/jdk17
export DB_PASSWORD=你的PostgreSQL密码   # 用户名 / 库名默认 raguser / rag_db
export DEEPSEEK_API_KEY=sk-xxxx        # 测试 /api/chat 时需要
export EMBEDDING_API_KEY=sk-xxxx       # 测试入库/检索时需要（SiliconFlow）

# 2. 启动后端
./mvnw spring-boot:run            # Windows: .\mvnw.cmd spring-boot:run
```

### 起前端（另开一个终端，保持后端开着）

```bash
cd frontend
npm install            # 首次运行需要，装一次就行
npm run dev            # 启动开发服务器
```

浏览器打开 http://localhost:5173 即可使用。前端所有 `/api` 请求由 Vite 代理转发到 8080（配置见 `frontend/vite.config.js`），所以**后端不需要任何 CORS 配置**。

> 只想调后端接口、不要界面的话，用 IDEA 打开根目录的 `api.http` 逐个点运行即可，不必起前端。

## 打包与部署

开发时要开两个进程；交付时可以把前端打进后端，做成一个自包含的 jar，只跑一个进程。

```bash
# 1. 构建前端（产物写进 src/main/resources/static/，见 frontend/vite.config.js）
cd frontend
npm run build

# 2. 打包后端（静态资源会一并打进 jar）
cd ..
./mvnw clean package -DskipTests     # Windows: .\mvnw.cmd clean package -DskipTests

# 3. 运行——就这一条，界面和接口都在 8080
java -jar target/rag-0.0.1-SNAPSHOT.jar
```

浏览器打开 **http://localhost:8080** 即可。此时前端由 Spring Boot 直接托管，前后端同源，前端里的 `/api` 相对路径直接生效（不再需要开发时的代理）。

两种模式的区别：

| 模式 | 启动 | 前端地址 | 改完前端代码后 |
| --- | --- | --- | --- |
| 开发 | 两个进程（`spring-boot:run` + `npm run dev`） | 5173 | 热更新，刷新即见 |
| 交付 | 一个进程（`java -jar`） | 8080 | 需重新 `npm run build` 再打包 |

> `src/main/resources/static/` 是构建产物，已在 `.gitignore` 中排除。所以从仓库克隆后必须先执行第 1 步构建前端，直接 `mvnw package` 打出的 jar 是不含界面的。

## 接口

- `GET /api/health` —— 健康检查
- `POST /api/chat` —— 聊天问答

```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"question":"用一句话介绍你自己"}'
```

- `POST /api/documents/parse` —— 上传文档，返回提取全文 + 切块（阶段 3）

```bash
# Windows PowerShell 用 curl.exe
curl.exe -X POST http://localhost:8080/api/documents/parse -F "file=@C:\path\to\note.txt"
```

返回示例：

```json
{
  "filename": "note.txt",
  "text": "……提取出的全文……",
  "charCount": 1234,
  "chunkCount": 3,
  "chunks": [
    {"text": "第一块…", "index": 0, "sourceName": "note.txt"},
    {"text": "第二块…", "index": 1, "sourceName": "note.txt"}
  ]
}
```

- `POST /api/documents/ingest` —— 上传文档，提取 → 切块 → 落库 PostgreSQL → 向量化入库 pgvector（阶段 6）

```bash
curl.exe -X POST http://localhost:8080/api/documents/ingest -F "file=@C:\path\to\note.txt"
```

返回示例：

```json
{
  "filename": "note.txt",
  "charCount": 1234,
  "chunkCount": 3,
  "storedCount": 3,
  "documentId": 1
}
```

- `GET /api/documents` —— 文档列表（摘要，不含全文）（阶段 6）

- `GET /api/documents/{id}` —— 文档详情（含全文）（阶段 6）

- `DELETE /api/documents/{id}` —— 删除文档及其向量块（阶段 6）

- `POST /api/search` —— 相似度检索，返回 top-K 个最相关原文块（阶段 4）

```bash
curl.exe -X POST http://localhost:8080/api/search \
  -H "Content-Type: application/json" \
  -d "{\"query\":\"如何安装\",\"topK\":4}"
```

返回示例（`score` 为余弦相似度 0~1，越大越相关）：

```json
{
  "query": "如何安装",
  "resultCount": 2,
  "results": [
    {"text": "安装步骤……", "score": 0.83, "sourceName": "note.txt", "index": 1}
  ]
}
```

- `POST /api/rag/ask` —— RAG 问答：检索相关块 → 拼进 prompt → 大模型带出处回答（阶段 5）

```bash
curl.exe -X POST http://localhost:8080/api/rag/ask \
  -H "Content-Type: application/json" \
  -d "{\"question\":\"如何安装\",\"topK\":4}"
```

返回示例（检索不到相关块时走严格模式，直接返回「资料库中没有找到相关信息」且 `sources` 为空）：

```json
{
  "question": "如何安装",
  "answer": "根据资料，安装步骤是先下载再解压……（来源：note.txt）",
  "sourceCount": 2,
  "sources": [
    {"text": "安装步骤……", "score": 0.83, "sourceName": "note.txt", "index": 1}
  ]
}
```

> 提示：Windows PowerShell 5.1 显示中文 JSON 可能乱码（响应头无 charset 时默认按 Latin-1 解码）。这是终端显示问题，不影响数据本身；可用 IDEA 的 HTTP 客户端或 Postman 查看，或见「验收标准」里的 UTF-8 解码写法。

## 配置说明

凭据一律通过环境变量注入，代码里只有本地开发默认值：

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/rag_db` | PostgreSQL 连接串 |
| `DB_USERNAME` | `raguser` | 数据库用户名 |
| `DB_PASSWORD` | （空） | 数据库密码 |
| `DEEPSEEK_API_KEY` | （空） | DeepSeek API key |
| `DEEPSEEK_BASE_URL` | `https://api.deepseek.com` | 大模型端点 |
| `EMBEDDING_API_KEY` | （空） | SiliconFlow API key（Embedding） |
| `EMBEDDING_BASE_URL` | `https://api.siliconflow.cn` | Embedding 端点（**不要带 /v1**，Spring AI 会自动拼） |

> 大模型 `model` 已设为 `deepseek-flash`；Embedding `model` 为 SiliconFlow 的 `Qwen/Qwen3-Embedding-0.6B`（1024 维）。

切块与检索参数（`application.yml`）：

| 配置 | 默认值 | 说明 |
| --- | --- | --- |
| `rag.chunk-size` | 500 | 每块最大字符数（中文按字符计） |
| `rag.chunk-overlap` | 50 | 相邻块重叠字符数 |
| `rag.similarity-threshold` | 0.35 | 检索相似度阈值（0~1），低于它的块会被丢弃 |
| `spring.servlet.multipart.max-file-size` | 10MB | 上传文件大小上限 |

> `rag.similarity-threshold` 是让严格模式真正生效的关键。相似度检索默认**永远**返回 top-K 条（哪怕全是硬凑的不相关文本），不加阈值的话，「检索结果为空」就无法代表「没有相关内容」——无关问题会带着一个分数极低的伪出处返回给前端。实测 Qwen3-Embedding-0.6B：无关问题约 0.17，相关问题 0.54~0.67，故取中间的 0.35。若发现本该能回答的问题被拦成「资料库中没有找到相关信息」，把这个值调低。

向量库参数（`application.yml` 的 `spring.ai.vectorstore.pgvector`）：

| 配置 | 值 | 说明 |
| --- | --- | --- |
| `dimensions` | 1024 | 必须与 Embedding 模型输出维度一致（Qwen3-Embedding-0.6B = 1024），配错会报维度不匹配 |
| `initialize-schema` | true | 启动时自动执行 `CREATE EXTENSION vector`、建向量表、建索引 |
| `index-type` | HNSW | 近似最近邻索引；数据量很小时也可设 `NONE` |
| `distance-type` | COSINE_DISTANCE | 余弦距离，分数越大越相似 |

> 向量表默认叫 `vector_store`，由 Spring AI 自动建。它和 JPA 建的 `document` 表都在同一个 `rag_db` 库里。

### 大模型调用的重试

`RetryConfig` 自定义了一个 `RetryTemplate`，用来覆盖 Spring AI 自动配置的默认策略。原因是个实测踩出来的坑：自动配置那一版**只重试 HTTP 错误码类的异常**（`TransientAiException`），**漏掉了网络层异常**（`ResourceAccessException`）——而后者一旦发生，一次失败就直接把 500 抛给前端，不做任何重试。

（Spring AI 内部自带的 `RetryUtils.DEFAULT_RETRY_TEMPLATE` 其实是包含网络异常的，只是自动配置那版没有同步过来，这里补上。）

现在两类都重试：首次 + 最多 3 次，退避 1s → 2s → 4s。**刻意没沿用 Spring AI 默认的 10 次**——那是偏批处理场景的取值，配合指数退避会一路涨到 180 秒，交互式问答可能卡十几分钟。问答要的是"轻微抖动自动恢复"，不是"网络真断了也死等"。

> Chat 和 Embedding 共用这个模板，所以 DeepSeek 和 SiliconFlow 两边都受保护——阶段 4 那个偶发的 `Connection reset` 也一并覆盖了。

## 验收标准

**阶段 1**（已完成）

- [x] 项目能编译、启动
- [x] `GET /api/health` 返回 200 与 JSON

**阶段 2**（已完成）

- [x] 引入 Spring AI，`ChatClient` 就绪
- [x] `POST /api/chat` 返回 DeepSeek 的真实回答（中英文均验证）
- [x] key 走环境变量，未写入代码 / Git

**阶段 3**（已完成）

- [x] 引入 PDFBox / POI 依赖
- [x] 支持 txt / md / pdf（文字型）/ docx 文本提取
- [x] 固定长度 + 重叠切块，参数可配置
- [x] 单元测试全部通过（17 个）
- [x] 上传一个真实文档，`/api/documents/parse` 返回合理的全文与 chunks

**阶段 4**（已完成）

- [x] 引入向量存储依赖，`VectorStore` / `SimpleVectorStore` Bean 就绪
- [x] Embedding 走 SiliconFlow `Qwen/Qwen3-Embedding-0.6B`（key 走环境变量）
- [x] `POST /api/documents/ingest` 上传文档 → 切块 → 向量化入库
- [x] `POST /api/search` 返回 top-K 个带分数的相似块
- [x] 单元测试全部通过（23 个）
- [x] 上传真实文档入库后，`/api/search` 能检索到相关块

**阶段 5**（已完成）

- [x] `POST /api/rag/ask` 检索相关块 → 拼 prompt → 大模型带出处回答
- [x] 检索不到相关块时走严格模式，直接返回「资料库中没有找到相关信息」
- [x] 单元测试全部通过

**阶段 6**（已完成）

- [x] `DocumentEntity` 落库（`ddl-auto: update` 自动建表）
- [x] `GET /api/documents`、`GET /api/documents/{id}`、`DELETE /api/documents/{id}`
- [x] `ingest` 返回 `documentId`，向量块带 documentId 元数据，删除时同步删向量块
- [x] 向量库由 SimpleVectorStore（内存）换成 pgvector，向量落盘、重启不丢
- [x] 加入检索相似度阈值（`rag.similarity-threshold`），无关问题不再返回伪出处
- [x] 单元测试全部通过（37 个）
- [x] 应用重启后 `/api/rag/ask` 仍能检索到重启前入库的文档

**阶段 7**（已完成）

- [x] `npm install` + `npm run dev` 能正常启动，页面正常渲染
- [x] 上传文档后，在架档案列表自动刷新，显示编号、块数、时间
- [x] 提问能返回答案，并列出引用条目的来源文件名、块序号、相似度分数
- [x] 问库中没有的内容时，显示「未收录」印章且不列出引用条目
- [x] 注销文档后列表刷新，且该文档的向量块一并移除

**阶段 8**（收尾与交付）

- [x] 前端构建产物打进 jar，`java -jar` 单进程跑起完整应用
- [x] 在 8080 上完成一次完整流程（上传 → 提问 → 注销）
- [x] README 补齐打包与部署说明
- [x] 补上网络异常重试（`RetryConfig`），偶发的连接抖动不再直接变成 500
- [x] 单元测试全部通过（39 个）
- [ ] git 提交并打 tag

## 路线图

| 阶段 | 内容 |
| --- | --- |
| 0 | 准备环境与密钥 |
| 1 | 搭 Spring Boot 3 项目（已完成） |
| 2 | 接入大模型（已完成） |
| 3 | 文档读取与切块（已完成） |
| 4 | 向量化与向量存储（已完成） |
| 5 | RAG 检索问答（核心，已完成） |
| 6 | 文档管理 API + 持久化（已完成：PostgreSQL + pgvector） |
| 7 | 前端界面（React + Vite，已实现待验收） |
| 8 | 收尾与交付（已完成：打包为单个可执行 jar） |
