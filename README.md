# RAG-QA — AI 知识库问答项目

上传文档 → 向量化入库 → 用户提问 → 大模型结合文档内容给出**带出处**的回答。

> 当前进度：**阶段 6a 已完成**（文档管理 API + MySQL 持久化）。阶段 1（骨架）、阶段 2（接入 DeepSeek）、阶段 3（文档读取与切块）、阶段 4（向量化与向量存储）、阶段 5（RAG 检索问答）、阶段 6a 均已完成并通过验收。剩余：阶段 6b 换 pgvector（生产向量库）。

## 技术栈

- Java 17 + Spring Boot 3.4.5
- Spring Web / Spring Data JPA
- Spring AI 1.0.9（OpenAI 兼容，接 DeepSeek；Embedding 接 SiliconFlow）
- 向量存储：SimpleVectorStore（内存；pgvector 生产向量库后续再做）
- Apache PDFBox 3（PDF 文本提取）
- Apache POI 5（Word 文本提取）
- MySQL
- Maven（含 Maven Wrapper）

## 目录结构

```
RAG
├── pom.xml
├── mvnw / mvnw.cmd            # Maven Wrapper
├── .gitignore
├── README.md
└── src
    ├── main
    │   ├── java/com/example/rag
    │   │   ├── RagApplication.java
    │   │   ├── config
    │   │   │   └── VectorStoreConfig.java    # SimpleVectorStore Bean（阶段 4）
    │   │   ├── controller
    │   │   │   ├── HealthController.java     # GET  /api/health
    │   │   │   ├── ChatController.java       # POST /api/chat（阶段 2）
    │   │   │   ├── DocumentController.java   # 文档解析/入库/增删查接口
    │   │   │   ├── SearchController.java     # POST /api/search（阶段 4）
    │   │   │   └── RagController.java        # POST /api/rag/ask（阶段 5）
    │   │   ├── document
    │   │   │   ├── Chunk.java                # 文本片段（chunk）
    │   │   │   ├── DocumentReader.java       # 多格式文本提取
    │   │   │   ├── DocumentEntity.java       # 文档实体（MySQL，阶段 6）
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
```

## 前置要求

- JDK 17（需设置 `JAVA_HOME`）
- MySQL（已创建 `rag_db` 库）
- DeepSeek API key（`/api/chat` 需要）
- SiliconFlow API key（`/api/documents/ingest`、`/api/search` 需要，用于 Embedding）

## 如何运行

```bash
# 1. 设置环境变量（PowerShell 用 $env:XXX="..."）
export JAVA_HOME=/path/to/jdk17
export DB_PASSWORD=你的MySQL密码
export DEEPSEEK_API_KEY=sk-xxxx        # 测试 /api/chat 时需要
export EMBEDDING_API_KEY=sk-xxxx       # 测试入库/检索时需要（SiliconFlow）

# 2. 启动
./mvnw spring-boot:run            # Windows: .\mvnw.cmd spring-boot:run
```

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

- `POST /api/documents/ingest` —— 上传文档，提取 → 切块 → 落库 MySQL → 向量化入库（阶段 6）

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
| `DB_URL` | `jdbc:mysql://localhost:3306/rag_db?...` | MySQL 连接串 |
| `DB_USERNAME` | `root` | 数据库用户名 |
| `DB_PASSWORD` | （空） | 数据库密码 |
| `DEEPSEEK_API_KEY` | （空） | DeepSeek API key |
| `DEEPSEEK_BASE_URL` | `https://api.deepseek.com` | 大模型端点 |
| `EMBEDDING_API_KEY` | （空） | SiliconFlow API key（Embedding） |
| `EMBEDDING_BASE_URL` | `https://api.siliconflow.cn` | Embedding 端点（**不要带 /v1**，Spring AI 会自动拼） |

> 大模型 `model` 已设为 `deepseek-flash`；Embedding `model` 为 SiliconFlow 的 `Qwen/Qwen3-Embedding-0.6B`（1024 维）。

切块参数（`application.yml`）：

| 配置 | 默认值 | 说明 |
| --- | --- | --- |
| `rag.chunk-size` | 500 | 每块最大字符数（中文按字符计） |
| `rag.chunk-overlap` | 50 | 相邻块重叠字符数 |
| `spring.servlet.multipart.max-file-size` | 10MB | 上传文件大小上限 |

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

**阶段 6**（MySQL 持久化部分已完成，pgvector 待做）

- [x] `DocumentEntity` 落库 MySQL（`ddl-auto: update` 自动建表）
- [x] `GET /api/documents`、`GET /api/documents/{id}`、`DELETE /api/documents/{id}`
- [x] `ingest` 返回 `documentId`，向量块带 documentId 元数据，删除时同步删向量块
- [x] 单元测试全部通过

## 路线图

| 阶段 | 内容 |
| --- | --- |
| 0 | 准备环境与密钥 |
| 1 | 搭 Spring Boot 3 项目（已完成） |
| 2 | 接入大模型（已完成） |
| 3 | 文档读取与切块（已完成） |
| 4 | 向量化与向量存储（已完成） |
| 5 | RAG 检索问答（核心，已完成） |
| 6 | 文档管理 API + 持久化（进行中：MySQL 部分已完成，pgvector 待做） |
| 7 | 简单前端界面 |
| 8 | 收尾与交付 |
