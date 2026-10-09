import { useState } from 'react'
import { ask } from '../api.js'

export default function AskPanel({ hasDocuments }) {
  const [question, setQuestion] = useState('')
  const [busy, setBusy] = useState(false)
  const [result, setResult] = useState(null)
  const [error, setError] = useState('')

  async function submit(e) {
    e?.preventDefault()
    const q = question.trim()
    if (!q || busy) return
    setBusy(true)
    setError('')
    setResult(null)
    try {
      setResult(await ask(q, 4))
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  const notFound = result != null && result.sourceCount === 0

  return (
    <section className="card card--desk">
      <h2 className="card__title">查 询</h2>

      <form className="query" onSubmit={submit}>
        <textarea
          className="query__input"
          rows={3}
          placeholder="就库中档案提问，例如：安装步骤是什么？"
          value={question}
          onChange={(e) => setQuestion(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter' && (e.metaKey || e.ctrlKey)) submit(e)
          }}
        />
        <div className="query__foot">
          <span className="query__tip">Ctrl / ⌘ + Enter 提交</span>
          <button className="btn" type="submit" disabled={busy || !question.trim()}>
            {busy ? '检索中…' : '提 问'}
          </button>
        </div>
      </form>

      {!hasDocuments && <p className="muted">库中尚未归档任何文献，回答将无从检索。</p>}
      {error && <div className="notice notice--err">{error}</div>}

      {result && (
        <article className="verdict">
          <p className="verdict__q">{result.question}</p>

          <div className="verdict__answer">
            {notFound && <span className="stamp">未 收 录</span>}
            <p>{result.answer}</p>
          </div>

          {result.sourceCount > 0 && (
            <>
              <h3 className="verdict__label">引用条目 · {result.sourceCount}</h3>
              <ol className="cites">
                {result.sources.map((s, i) => (
                  <li key={i} className="cite" style={{ animationDelay: `${i * 70}ms` }}>
                    <div className="cite__head">
                      <span className="cite__src">{s.sourceName}</span>
                      <span className="cite__no">第 {s.index + 1} 块</span>
                      <span className="cite__score" title="余弦相似度，越接近 1 越相关">
                        {s.score.toFixed(3)}
                      </span>
                    </div>
                    <p className="cite__text">{s.text}</p>
                  </li>
                ))}
              </ol>
            </>
          )}
        </article>
      )}
    </section>
  )
}
