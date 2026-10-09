# RAG-QA

上传文档，然后对着这些文档提问。回答会标出引用了哪几段原文。

![界面截图](docs/screenshot.png)

## 做什么

把 txt / md / pdf / docx 丢进去，后端提取文本、切成带重叠的片段、算成向量存进 PostgreSQL。提问时把问题也向量化，从库里取出最相关的几段交给 DeepSeek 组织成答案，同时把用到的片段一并返回——文件名、第几块、相似度分数都列出来。

检索不到相关内容时不硬凑。低于阈值的片段会被丢掉，结果为空就直接返回「资料库中没有找到相关信息」，不调用大模型。

## 数据流

```
入库
  文件 ──► 文本提取 ──► 切块 ──► Embedding ──► pgvector
         (按扩展名)  (500字/50重叠) (SiliconFlow)

提问
  问题 ──► Embedding ──► 余弦相似度取 top-K ──► 过滤低分
                                                   │
                 DeepSeek ◄── 拼 prompt ◄──────────┘
                    │
                    └──► 答案 + 引用条目
```

## 技术栈

Java 17 + Spring Boot 3.4.5，Spring AI 1.0.9。

对话走 DeepSeek，embedding 走 SiliconFlow 的 Qwen3-Embedding-0.6B（1024 维）。DeepSeek 不提供 embedding 接口，这两件事必须分开找服务商。

文档元信息和全文用 JPA 存，向量用 pgvector 存，在同一个 PostgreSQL 库里。PDF 和 Word 的文本提取分别用 PDFBox 3 和 POI 5；扫描件需要 OCR，没做。

前端 React 18 + Vite 5。

## 跑起来

需要 JDK 17、Node.js 18+、PostgreSQL 17（带 pgvector）、DeepSeek API key、SiliconFlow API key。

数据库用 Docker 起最省事：

```bash
docker run -d --name rag-pg -e POSTGRES_USER=raguser -e POSTGRES_PASSWORD=你的密码 \
  -e POSTGRES_DB=rag_db -p 5432:5432 pgvector/pgvector:pg17
```

官方镜像已经编译好 vector 扩展。应用启动时会自己执行 `CREATE EXTENSION` 并建表，不用手动初始化。

后端：

```bash
export JAVA_HOME=/path/to/jdk17
export DB_PASSWORD=你的数据库密码
export DEEPSEEK_API_KEY=sk-xxxx
export EMBEDDING_API_KEY=sk-xxxx

./mvnw spring-boot:run
```

前端另开一个终端，后端保持运行：

```bash
cd frontend
npm install
npm run dev
```

浏览器打开 http://localhost:5173。前端的 `/api` 请求由 Vite 转发到 8080（配置在 `frontend/vite.config.js`），后端不需要任何 CORS 设置。

只想调接口不要界面的话，用 IDEA 打开根目录的 `api.http` 逐个点运行就行。

## 打包成单个 jar

```bash
cd frontend
npm run build

cd ..
./mvnw clean package -DskipTests
java -jar target/rag-0.0.1-SNAPSHOT.jar
```

前端构建产物写进 `src/main/resources/static`，所以打出的 jar 自带界面，运行只要这一个进程，访问 http://localhost:8080 即可。

这个目录是构建产物，已排除在 Git 之外。克隆之后要先跑 `npm run build`，否则打出的 jar 里没有界面。

## 接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/health` | 健康检查 |
| POST | `/api/chat` | 直接和大模型对话，不查库 |
| POST | `/api/documents/parse` | 只解析：返回提取的全文和切块结果 |
| POST | `/api/documents/ingest` | 解析 + 落库 + 向量化入库 |
| GET | `/api/documents` | 文档列表（摘要，不含全文） |
| GET | `/api/documents/{id}` | 文档详情（含全文） |
| DELETE | `/api/documents/{id}` | 删除文档及其向量块 |
| POST | `/api/search` | 纯检索，返回 top-K 个带分数的片段 |
| POST | `/api/rag/ask` | 检索 + 生成，返回答案和引用 |

### POST /api/rag/ask

```bash
curl.exe -X POST http://localhost:8080/api/rag/ask \
  -H "Content-Type: application/json" \
  -d "{\"question\":\"免费版能登录几台设备\",\"topK\":4}"
```

```json
{
  "question": "免费版能登录几台设备",
  "answer": "免费版最多同时在 3 台设备上登录。（来源：handbook.md）",
  "sourceCount": 1,
  "sources": [
    {
      "text": "能同时登录几台设备：免费版 3 台，会员版不限。",
      "score": 0.71,
      "sourceName": "handbook.md",
      "index": 2
    }
  ]
}
```

`score` 是余弦相似度，0 到 1，越大越相关。检索不到内容时 `sourceCount` 为 0，`sources` 是空数组。

### POST /api/documents/ingest

```bash
curl.exe -X POST http://localhost:8080/api/documents/ingest -F "file=@samples/handbook.md"
```

```json
{
  "filename": "handbook.md",
  "charCount": 862,
  "chunkCount": 3,
  "storedCount": 3,
  "documentId": 1
}
```

Windows 上用 `curl.exe` 而不是 `curl`——后者在 PowerShell 里是 `Invoke-WebRequest` 的别名，参数不通用。

## 配置

凭据全部走环境变量，代码里只有本地开发的默认值。

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/rag_db` | 数据库连接串 |
| `DB_USERNAME` | `raguser` | 数据库用户名 |
| `DB_PASSWORD` | 空 | 数据库密码 |
| `DEEPSEEK_API_KEY` | 空 | DeepSeek key |
| `DEEPSEEK_BASE_URL` | `https://api.deepseek.com` | 对话端点 |
| `EMBEDDING_API_KEY` | 空 | SiliconFlow key |
| `EMBEDDING_BASE_URL` | `https://api.siliconflow.cn` | embedding 端点，**不要带 `/v1`** |

模型和调优参数在 `application.yml`：

| 配置 | 值 | 说明 |
| --- | --- | --- |
| `rag.chunk-size` | 500 | 每块最大字符数，中文按字符算 |
| `rag.chunk-overlap` | 50 | 相邻块重叠的字符数 |
| `rag.similarity-threshold` | 0.35 | 检索阈值，低于它的片段丢弃 |
| `spring.ai.vectorstore.pgvector.dimensions` | 1024 | 必须和 embedding 输出维度一致 |
| `spring.ai.vectorstore.pgvector.id-type` | TEXT | 见下方说明 |
| `spring.ai.vectorstore.pgvector.index-type` | HNSW | 近似最近邻索引 |
| `spring.ai.vectorstore.pgvector.distance-type` | COSINE_DISTANCE | 余弦距离 |
| `spring.servlet.multipart.max-file-size` | 10MB | 上传大小上限 |

对话模型 `deepseek-flash`，embedding 模型 `Qwen/Qwen3-Embedding-0.6B`。

## 几个踩过的坑

### 检索必须加相似度阈值

向量检索不管相关不相关，永远会返回 top-K 条。不加阈值的话，「检索结果为空」就代表不了「库里没有内容」——问一个库里根本不存在的问题，照样能拿到一段不相干的文字，被当作出处显示给用户。

阈值最后定在 0.35，是实测出来的：无关问题得分约 0.17，相关问题在 0.54 到 0.67 之间。

### 重试策略漏了网络异常

Spring AI 自动配置的 `RetryTemplate` 只重试 HTTP 错误码类的异常，漏掉了网络层的 `ResourceAccessException`。这类异常一发生就直接抛 500，一次重试都没有。

有意思的是 Spring AI 内部另有一套重试模板，那套是包含网络异常的，只是自动配置这版没同步过来。`RetryConfig` 自定义了一个补上，重试 3 次，退避 1s → 2s → 4s。

没用默认的 10 次。那个配合指数退避会涨到 180 秒，交互式问答能卡十几分钟——要的是轻微抖动能自动恢复，不是网络断了也死等。

### 向量表的 id 类型

Spring AI 的 pgvector 默认把 id 列建成 UUID 类型。如果把向量 id 设计成自定义字符串（这里用的是「文档id-块序号」，比如 `2-0`），插入时会报 `Invalid UUID string`。要在配置里显式指定 `id-type: TEXT`。

改完还得把旧表删掉重建——建表用的是 `CREATE TABLE IF NOT EXISTS`，表已存在就不会动列类型。

### Hibernate 在 PostgreSQL 上的 @Lob

`@Lob String` 在 PostgreSQL 上会被映射成 oid（大对象），读写都要绕一圈额外 API。文档全文这个字段改用 `@Column(columnDefinition = "text")` 更直接。

## 目录结构

```
RAG
├── pom.xml
├── mvnw / mvnw.cmd                 # Maven Wrapper
├── api.http                        # IDEA HTTP 客户端脚本
├── LICENSE
├── samples/handbook.md             # 示例文档，用来试接口
├── docs/                           # 截图
├── src
│   ├── main
│   │   ├── java/com/example/rag
│   │   │   ├── RagApplication.java
│   │   │   ├── config/RetryConfig.java
│   │   │   ├── controller/         # Health / Chat / Document / Search / Rag
│   │   │   ├── document/           # 文本提取、切块、实体、仓库
│   │   │   ├── dto/                # 请求和响应体
│   │   │   └── service/            # VectorStore / Rag / Document
│   │   └── resources/application.yml
│   └── test/java/com/example/rag   # 单元测试
└── frontend                        # React + Vite
    ├── vite.config.js              # /api → 8080 代理
    └── src
        ├── App.jsx
        ├── api.js                  # 接口封装
        ├── styles.css
        └── components/             # UploadPanel / DocumentList / AskPanel
```

## 测试

```bash
./mvnw test
```

39 个单元测试，覆盖文本提取、切块、向量服务、文档服务和各个控制器。控制器用 `@WebMvcTest` 切片测试，不需要真实数据库。

## License

MIT
