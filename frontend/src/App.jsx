import { useEffect, useState } from 'react'
import './App.css'

function App() {
  const [apiStatus, setApiStatus] = useState('checking')

  useEffect(() => {
    const controller = new AbortController()

    fetch('/api/health', { signal: controller.signal })
      .then((response) => {
        if (!response.ok) {
          throw new Error(`Health check failed with status ${response.status}`)
        }

        return response.json()
      })
      .then((health) => setApiStatus(health.status === 'ok' ? 'online' : 'unavailable'))
      .catch(() => {
        if (!controller.signal.aborted) {
          setApiStatus('unavailable')
        }
      })

    return () => controller.abort()
  }, [])

  return (
    <main className="workspace">
      <header className="topbar">
        <a className="wordmark" href="/" aria-label="The System home">
          THE SYSTEM
        </a>
        <span className="eyebrow">PROJECT STARTER</span>
      </header>

      <section className="welcome" aria-labelledby="welcome-heading">
        <p className="eyebrow">A fresh start</p>
        <h1 id="welcome-heading">Your workspace is ready.</h1>
        <p className="intro">
          The foundation is in place. We can shape the experience together as the
          project takes form.
        </p>
      </section>

      <section className="status-card" aria-labelledby="status-heading">
        <div>
          <p className="eyebrow">Development status</p>
          <h2 id="status-heading">Application services</h2>
        </div>
        <p
          className={`status status--${apiStatus}`}
          role="status"
          aria-live="polite"
        >
          <span className="status-indicator" aria-hidden="true" />
          {apiStatus === 'checking'
            ? 'Checking backend'
            : apiStatus === 'online'
              ? 'Backend connected'
              : 'Backend unavailable'}
        </p>
      </section>

      {apiStatus === 'unavailable' && (
        <p className="status-help">
          Start the backend and refresh this page to check the connection.
        </p>
      )}
    </main>
  )
}

export default App
