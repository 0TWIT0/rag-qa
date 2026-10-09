import { useRef, useState } from 'react'
import { ingestDocument } from '../api.js'

export default function UploadPanel({ onIngested }) {
  const inputRef = useRef(null)
  const [dragging, setDragging] = useState(false)
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState(null) // { kind: 'ok' | 'err', text }

  async function send(file) {
    if (!file || busy) return
    setBusy(true)
    setMessage(null)
    try {
      const r = await ingestDocument(file)
      setMessage({
        kind: 'ok',
        text: `《${r.filename}》已归档 · 编号 ${String(r.documentId).padStart(3, '0')} · ${r.chunkCount} 条`,
      })
      onIngested?.()
    } catch (e) {
      setMessage({ kind: 'err', text: e.message })
    } finally {
      setBusy(false)
    }
  }

  function handleDrop(e) {
    e.preventDefault()
    setDragging(false)
    send(e.dataTransfer.files?.[0])
  }

  return (
    <section className="card">
      <h2 className="card__title">入 库</h2>

      <div
        className={`dropzone${dragging ? ' is-dragging' : ''}${busy ? ' is-busy' : ''}`}
        role="button"
        tabIndex={0}
        onClick={() => !busy && inputRef.current?.click()}
        onKeyDown={(e) => {
          if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault()
            if (!busy) inputRef.current?.click()
          }
        }}
        onDragOver={(e) => {
          e.preventDefault()
          setDragging(true)
        }}
        onDragLeave={() => setDragging(false)}
        onDrop={handleDrop}
      >
        <input
          ref={inputRef}
          type="file"
          accept=".txt,.md,.markdown,.pdf,.docx"
          hidden
          onChange={(e) => {
            send(e.target.files?.[0])
            e.target.value = '' // 允许连续上传同一个文件
          }}
        />
        {busy ? (
          <>
            <p className="dropzone__main">正在归档…</p>
            <p className="dropzone__hint">提取文本 → 切块 → 向量化入库</p>
          </>
        ) : (
          <>
            <p className="dropzone__main">拖入文件，或点击选择</p>
            <p className="dropzone__hint">支持 txt / md / pdf（文字型）/ docx，单个不超过 10 MB</p>
          </>
        )}
      </div>

      {message && <p className={`notice notice--${message.kind}`}>{message.text}</p>}
    </section>
  )
}
