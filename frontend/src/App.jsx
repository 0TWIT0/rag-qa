import { useCallback, useEffect, useState } from 'react'
import UploadPanel from './components/UploadPanel.jsx'
import DocumentList from './components/DocumentList.jsx'
import AskPanel from './components/AskPanel.jsx'
import { listDocuments } from './api.js'

export default function App() {
  const [documents, setDocuments] = useState([])
  const [loadingList, setLoadingList] = useState(true)
  const [listError, setListError] = useState('')

  const refresh = useCallback(async () => {
    setLoadingList(true)
    setListError('')
    try {
      setDocuments(await listDocuments())
    } catch (e) {
      setListError(e.message)
    } finally {
      setLoadingList(false)
    }
  }, [])

  useEffect(() => {
    refresh()
  }, [refresh])

  const totalChunks = documents.reduce((n, d) => n + d.chunkCount, 0)

  return (
    <div className="room">
      <header className="masthead">
        <div className="masthead__titles">
          <p className="masthead__eyebrow">RAG-QA · 检索增强问答</p>
          <h1>档案室</h1>
          <p className="masthead__sub">文献入库归档，提问时逐条给出出处</p>
        </div>
        <dl className="ledger">
          <div className="ledger__cell">
            <dt>在架</dt>
            <dd>{documents.length}</dd>
          </div>
          <div className="ledger__cell">
            <dt>条目</dt>
            <dd>{totalChunks}</dd>
          </div>
        </dl>
      </header>

      <main className="floor">
        <section className="stack">
          <UploadPanel onIngested={refresh} />
          <DocumentList
            documents={documents}
            loading={loadingList}
            error={listError}
            onDeleted={refresh}
            onRetry={refresh}
          />
        </section>

        <section className="stack">
          <AskPanel hasDocuments={documents.length > 0} />
        </section>
      </main>

      <footer className="colophon">
        Spring Boot 3.4 · Spring AI 1.0.9 · pgvector · DeepSeek
      </footer>
    </div>
  )
}
