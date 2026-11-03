import { useEffect, useState } from 'react'
import './App.css'
import Dashboard from './Dashboard.jsx'

async function getCsrfToken() {
  const response = await fetch('/api/auth/csrf', { credentials: 'same-origin' })
  if (!response.ok) {
    throw new Error('The system could not be reached. Please try again.')
  }

  return response.json()
}

async function readError(response) {
  const body = await response.json().catch(() => null)
  return body?.message || 'Something went wrong. Please try again.'
}

function App() {
  const [mode, setMode] = useState('login')
  const [account, setAccount] = useState(null)
  const [csrf, setCsrf] = useState(null)
  const [apiReady, setApiReady] = useState(false)
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [showPassword, setShowPassword] = useState(false)
  const [form, setForm] = useState({ displayName: '', email: '', password: '' })
  const [error, setError] = useState('')

  useEffect(() => {
    const controller = new AbortController()

    async function initialize() {
      try {
        const csrfResponse = await fetch('/api/auth/csrf', {
          credentials: 'same-origin',
          signal: controller.signal,
        })
        if (!csrfResponse.ok) {
          throw new Error('The system could not be reached. Start the backend and try again.')
        }

        const csrfToken = await csrfResponse.json()
        setCsrf(csrfToken)
        const profileResponse = await fetch('/api/auth/me', {
          credentials: 'same-origin',
          signal: controller.signal,
        })

        if (profileResponse.ok) {
          setAccount(await profileResponse.json())
        } else if (profileResponse.status !== 401) {
          throw new Error('Could not check your account. Please refresh the page.')
        }

        setApiReady(true)
      } catch (requestError) {
        if (!controller.signal.aborted) {
          setError(requestError.message)
        }
      } finally {
        if (!controller.signal.aborted) {
          setLoading(false)
        }
      }
    }

    initialize()
    return () => controller.abort()
  }, [])

  function updateField(event) {
    setForm((current) => ({ ...current, [event.target.name]: event.target.value }))
  }

  function changeMode(nextMode) {
    setMode(nextMode)
    setError('')
  }

  async function submitForm(event) {
    event.preventDefault()
    setError('')
    setSubmitting(true)

    try {
      const response = await fetch(`/api/auth/${mode === 'register' ? 'register' : 'login'}`, {
        method: 'POST',
        credentials: 'same-origin',
        headers: {
          'Content-Type': 'application/json',
          [csrf.headerName]: csrf.token,
        },
        body: JSON.stringify(
          mode === 'register'
            ? form
            : { email: form.email, password: form.password },
        ),
      })

      if (!response.ok) {
        throw new Error(await readError(response))
      }

      setAccount(await response.json())
    } catch (requestError) {
      setError(
        requestError instanceof TypeError
          ? 'The system could not be reached. Check your connection and try again.'
          : requestError.message || 'Please try again.',
      )
    } finally {
      setSubmitting(false)
    }
  }

  async function logout() {
    setError('')
    setSubmitting(true)

    try {
      const response = await fetch('/api/auth/logout', {
        method: 'POST',
        credentials: 'same-origin',
        headers: { [csrf.headerName]: csrf.token },
      })

      if (!response.ok) {
        throw new Error(await readError(response))
      }

      setAccount(null)
      setForm({ displayName: '', email: '', password: '' })
      setMode('login')
      setCsrf(await getCsrfToken())
    } catch (requestError) {
      setError(
        requestError instanceof TypeError
          ? 'The system could not be reached. Check your connection and try again.'
          : requestError.message || 'Could not sign out. Please try again.',
      )
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="entry-page">
      <div className="ambient ambient--one" aria-hidden="true" />
      <div className="ambient ambient--two" aria-hidden="true" />

      <header className="topbar">
        <a className="brand" href="/" aria-label="The System home">
          <span className="brand-mark" aria-hidden="true">
            <span />
          </span>
          <span>THE SYSTEM</span>
        </a>
        <div className="connection">
          <span className={`connection-dot${apiReady ? ' is-online' : ''}`} />
          {loading ? 'CONNECTING' : apiReady ? 'SYSTEM ONLINE' : 'SYSTEM OFFLINE'}
        </div>
      </header>

      {account ? (
        <Dashboard account={account} onLogout={logout} submitting={submitting} />
      ) : (
        <section className="entry-layout">
        <div className="intro-panel">
          <div className="intro-copy">
            <p className="kicker"><span /> YOUR NEXT CHAPTER STARTS HERE</p>
            <h1>
              Make your
              <br />
              <span>potential</span>
              <br />
              tangible.
            </h1>
            <p className="intro-text">
              A space to turn the things you want to do into progress you can
              actually see.
            </p>
          </div>

          <div className="portal-art" aria-hidden="true">
            <div className="portal-grid" />
            <div className="portal-ring portal-ring--outer" />
            <div className="portal-ring portal-ring--inner" />
            <div className="portal-core"><span /></div>
            <span className="portal-label portal-label--top">PERSONAL GROWTH // 001</span>
            <span className="portal-label portal-label--bottom">BEGIN WHEN YOU ARE READY</span>
          </div>

          <p className="intro-footer">A fresh start. Your pace. Your story.</p>
        </div>

        <section className="access-panel" aria-labelledby="access-heading">
          <div className="panel-topline">
            <span>IDENTITY GATE</span>
            <span>NO. 001</span>
          </div>

          {account ? (
            <div className="signed-in">
              <div className="success-emblem" aria-hidden="true"><span /></div>
              <p className="kicker">ACCESS GRANTED</p>
              <h2 id="access-heading">Welcome, {account.displayName}.</h2>
              <p className="panel-description">
                Your account is ready. This is where your next chapter begins.
              </p>
              <p className="account-email">{account.email}</p>
              <button
                className="submit-button"
                type="button"
                onClick={logout}
                disabled={submitting}
              >
                {submitting ? 'PLEASE WAIT' : 'SIGN OUT'}
                <span aria-hidden="true">↗</span>
              </button>
            </div>
          ) : (
            <>
              <div className="panel-heading">
                <p className="kicker">{mode === 'login' ? 'WELCOME BACK' : 'CREATE YOUR PROFILE'}</p>
                <h2 id="access-heading">
                  {mode === 'login' ? 'Enter the system.' : 'Begin your journey.'}
                </h2>
                <p className="panel-description">
                  {mode === 'login'
                    ? 'Sign in to pick up where you left off.'
                    : 'Set up your profile to get started.'}
                </p>
              </div>

              <div className="mode-switch" aria-label="Choose sign-in or registration">
                <button
                  type="button"
                  className={mode === 'login' ? 'is-selected' : ''}
                  aria-pressed={mode === 'login'}
                  onClick={() => changeMode('login')}
                >
                  SIGN IN
                </button>
                <button
                  type="button"
                  className={mode === 'register' ? 'is-selected' : ''}
                  aria-pressed={mode === 'register'}
                  onClick={() => changeMode('register')}
                >
                  REGISTER
                </button>
              </div>

              <form className="access-form" onSubmit={submitForm}>
                {mode === 'register' && (
                  <label className="field">
                    <span>DISPLAY NAME</span>
                    <input
                      autoComplete="nickname"
                      maxLength={50}
                      name="displayName"
                      onChange={updateField}
                      placeholder="What should we call you?"
                      required
                      value={form.displayName}
                    />
                  </label>
                )}

                <label className="field">
                  <span>EMAIL ADDRESS</span>
                  <input
                    autoComplete="email"
                    maxLength={254}
                    name="email"
                    onChange={updateField}
                    placeholder="you@example.com"
                    required
                    type="email"
                    value={form.email}
                  />
                </label>

                <label className="field">
                  <span>PASSWORD</span>
                  <span className="password-input">
                    <input
                      autoComplete={mode === 'register' ? 'new-password' : 'current-password'}
                      maxLength={72}
                      minLength={mode === 'register' ? 10 : undefined}
                      name="password"
                      onChange={updateField}
                      placeholder={mode === 'register' ? 'At least 10 characters' : 'Enter your password'}
                      required
                      type={showPassword ? 'text' : 'password'}
                      value={form.password}
                    />
                    <button
                      aria-label={showPassword ? 'Hide password' : 'Show password'}
                      className="password-toggle"
                      onClick={() => setShowPassword((visible) => !visible)}
                      type="button"
                    >
                      {showPassword ? 'HIDE' : 'SHOW'}
                    </button>
                  </span>
                </label>

                {error && <p className="form-error" role="alert">{error}</p>}

                {!apiReady && !loading && (
                  <p className="form-note" role="status">
                    Start the backend to create an account or sign in.
                  </p>
                )}

                <button
                  className="submit-button"
                  disabled={!apiReady || !csrf || submitting}
                  type="submit"
                >
                  <span>
                    {submitting
                      ? 'PLEASE WAIT'
                      : mode === 'login'
                        ? 'ENTER THE SYSTEM'
                        : 'CREATE ACCOUNT'}
                  </span>
                  <span aria-hidden="true">↗</span>
                </button>
              </form>

              <p className="panel-footnote">
                {mode === 'login' ? 'NEW HERE?' : 'ALREADY HAVE AN ACCOUNT?'}{' '}
                <button
                  className="text-button"
                  onClick={() => changeMode(mode === 'login' ? 'register' : 'login')}
                  type="button"
                >
                  {mode === 'login' ? 'CREATE AN ACCOUNT' : 'SIGN IN'}
                </button>
              </p>
            </>
          )}
        </section>
        </section>
      )}

      <footer className="page-footer">
        <span>THE SYSTEM <span className="footer-separator">/</span> YOUR PROGRESS, YOUR WAY</span>
        <span>TAKE THE FIRST STEP</span>
      </footer>
    </main>
  )
}

export default App
