// 后端接口统一封装。所有请求都走相对路径 /api，
// 由 Vite 的开发代理转发到 localhost:8080（见 vite.config.js）。

const BASE = '/api'

/** 把非 2xx 响应统一转成带状态码的异常，调用方只需 catch 一处。 */
async function ensureOk(res, action) {
  if (res.ok) return res
  let detail = ''
  try {
    detail = (await res.text()).slice(0, 300)
  } catch {
    // 读不出响应体就算了，不影响抛错
  }
  throw new Error(`${action}失败（HTTP ${res.status}）${detail ? '：' + detail : ''}`)
}

/** 文档列表（摘要，不含全文）。 */
export async function listDocuments() {
  const res = await ensureOk(await fetch(`${BASE}/documents`), '读取文档列表')
  return res.json()
}

/** 上传文档：后端会自动完成 提取 → 切块 → 落库 → 向量化。 */
export async function ingestDocument(file) {
  const form = new FormData()
  form.append('file', file)
  const res = await ensureOk(
    await fetch(`${BASE}/documents/ingest`, { method: 'POST', body: form }),
    '上传文档',
  )
  return res.json()
}

/** 删除文档及其向量块。后端返回空响应体，所以这里不解析 JSON。 */
export async function deleteDocument(id) {
  await ensureOk(await fetch(`${BASE}/documents/${id}`, { method: 'DELETE' }), '删除文档')
}

/** RAG 问答：返回 { question, answer, sourceCount, sources[] }。 */
export async function ask(question, topK = 4) {
  const res = await ensureOk(
    await fetch(`${BASE}/rag/ask`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ question, topK }),
    }),
    '问答',
  )
  return res.json()
}
