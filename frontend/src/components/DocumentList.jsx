import { useState } from 'react'
import { deleteDocument } from '../api.js'

function formatSize(bytes) {
  if (bytes == null) return '—'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

function formatTime(iso) {
  if (!iso) return '—'
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

export default function DocumentList({ documents, loading, error, onDeleted, onRetry }) {
  const [pendingId, setPendingId] = useState(null)

  async function remove(doc) {
    if (pendingId != null) return
    const confirmed = window.confirm(
      `确定注销《${doc.filename}》？该文档及其向量条目都会被一并移除。`,
    )
    if (!confirmed) return
    setPendingId(doc.id)
    try {
      await deleteDocument(doc.id)
      onDeleted?.()
    } catch (e) {
      window.alert(e.message)
    } finally {
      setPendingId(null)
    }
  }

  return (
    <section className="card">
      <h2 className="card__title">
        在架档案 <span className="card__count">{documents.length}</span>
      </h2>

      {loading && <p className="muted">正在调阅…</p>}

      {!loading && error && (
        <div className="notice notice--err">
          <span>{error}</span>
          <button className="linkish" type="button" onClick={onRetry}>
            重试
          </button>
        </div>
      )}

      {!loading && !error && documents.length === 0 && (
        <p className="muted">架上暂无档案，先在上面入库一份。</p>
      )}

      <ul className="shelf">
        {documents.map((doc, i) => (
          <li key={doc.id} className="slip" style={{ animationDelay: `${i * 55}ms` }}>
            <span className="slip__no">{String(doc.id).padStart(3, '0')}</span>
            <div className="slip__body">
              <p className="slip__name" title={doc.filename}>
                {doc.filename}
              </p>
              <p className="slip__meta">
                {formatSize(doc.fileSize)} · {doc.charCount} 字 · <strong>{doc.chunkCount}</strong> 块
              </p>
              <p className="slip__meta slip__meta--dim">{formatTime(doc.createdAt)}</p>
            </div>
            <button
              className="slip__action"
              type="button"
              onClick={() => remove(doc)}
              disabled={pendingId === doc.id}
            >
              {pendingId === doc.id ? '…' : '注销'}
            </button>
          </li>
        ))}
      </ul>
    </section>
  )
}
