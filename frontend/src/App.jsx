import { useEffect, useState } from 'react'
import './App.css'
import Dashboard from './Dashboard.jsx'

const INITIAL_PROFILE_FORM = {
  playerName: '',
  mainChallenge: '',
  additionalChallenges: '',
  goal: '',
  situation: '',
}

const CHAPTER_COPY = {
  INITIALIZATION: {
    title: 'CHAPTER I — INITIALIZATION',
    description: 'The System identifies the player, calibrates the dungeon, and prepares the awakening ritual.',
  },
  IDENTIFICATION: {
    title: 'CHAPTER II — IDENTIFICATION',
    description: 'The main challenge becomes the boss. The smaller obstacles become hostile units. The player observes the pattern.',
  },
  ENCOUNTER: {
    title: 'CHAPTER III — ENCOUNTER',
    description: 'The awakening is interactive. Every decision shapes the next moment, but the path always leads forward.',
  },
  DESPAIR: {
    title: 'CHAPTER IV — DESPAIR',
    description: 'The pressure peaks. The dungeon closes in. The player reaches a critical condition and the System offers a way out.',
  },
  SYSTEM_OFFER: {
    title: 'CHAPTER V — THE SYSTEM OFFER',
    description: 'A unique opportunity has been detected. Choose whether to become a Player or step back and revisit the story later.',
  },
  AWAKENING: {
    title: 'CHAPTER VI — AWAKENING',
    description: 'The dungeon collapses. Blue energy surges. The System completes the registration and the path merges with the dashboard.',
  },
}

const CHOICE_OPTIONS = {
  INITIALIZATION: ['CONTINUE'],
  IDENTIFICATION: ['CONTINUE'],
  ENCOUNTER: ['CONFRONT', 'ADAPT', 'OBSERVE'],
  DESPAIR: ['CONTINUE'],
}

async function getCsrfToken() {
  const response = await fetch('/api/auth/csrf', { credentials: 'same-origin' })
  if (!response.ok) {
    throw new Error('The system could not be reached. Please try again.')
  }

  return response.json()
}

async function readError(response) {
  const body = await response.json().catch(() => null)
  return body?.message || `Request failed (${response.status}). Please try again.`
}

function PlayerOnboardingForm({
  onboarding,
  profileForm,
  setProfileForm,
  submitting,
  onSave,
  error,
  notice,
}) {
  return (
    <section className="onboarding-shell">
      <div className="onboarding-header">
        <p className="kicker"><span /> PLAYER INITIALIZATION</p>
        <h2>Complete your profile before the awakening begins.</h2>
        <p className="panel-description">
          This information personalizes the dungeon without becoming a permanent label. It can be updated later.
        </p>
      </div>

      <form className="onboarding-form" onSubmit={onSave}>
        <label className="field">
          <span>PLAYER NAME</span>
          <input
            name="playerName"
            value={profileForm.playerName}
            onChange={(event) => setProfileForm((current) => ({ ...current, playerName: event.target.value }))}
            maxLength={50}
            placeholder="How shall the System identify you?"
            required
          />
        </label>

        <label className="field">
          <span>MAIN CHALLENGE</span>
          <input
            name="mainChallenge"
            value={profileForm.mainChallenge}
            onChange={(event) => setProfileForm((current) => ({ ...current, mainChallenge: event.target.value }))}
            maxLength={200}
            placeholder="Procrastination, fear, burnout..."
            required
          />
        </label>

        <label className="field">
          <span>ADDITIONAL CHALLENGES</span>
          <input
            name="additionalChallenges"
            value={profileForm.additionalChallenges}
            onChange={(event) => setProfileForm((current) => ({ ...current, additionalChallenges: event.target.value }))}
            maxLength={500}
            placeholder="Separate with commas"
          />
        </label>

        <label className="field">
          <span>GOAL</span>
          <input
            name="goal"
            value={profileForm.goal}
            onChange={(event) => setProfileForm((current) => ({ ...current, goal: event.target.value }))}
            maxLength={500}
            placeholder="What do you want to become or achieve?"
            required
          />
        </label>

        <label className="field">
          <span>CURRENT SITUATION</span>
          <textarea
            name="situation"
            value={profileForm.situation}
            onChange={(event) => setProfileForm((current) => ({ ...current, situation: event.target.value }))}
            maxLength={2000}
            rows={4}
            placeholder="Optional context the System can use to personalize the experience."
          />
        </label>

        {error && <p className="form-error">{error}</p>}
        {notice && <p className="form-status">{notice}</p>}

        <button className="submit-button" type="submit" disabled={submitting}>
          {submitting ? 'SAVING PROFILE' : 'SAVE PROFILE'}
          <span aria-hidden="true">↗</span>
        </button>
      </form>
    </section>
  )
}

function AwakeningExperience({
  onboarding,
  selectedChoice,
  setSelectedChoice,
  submitting,
  onChoiceAdvance,
  onAction,
  error,
  notice,
}) {
  const chapterDefinition = CHAPTER_COPY[onboarding.currentChapter] || CHAPTER_COPY.INITIALIZATION
  const availableChoices = CHOICE_OPTIONS[onboarding.currentChapter] || ['CONTINUE']

  return (
    <section className="onboarding-shell">
      <div className="onboarding-header">
        <p className="kicker"><span /> {chapterDefinition.title}</p>
        <h2>{chapterDefinition.title}</h2>
        <p className="panel-description">{chapterDefinition.description}</p>
      </div>

      {onboarding.currentChapter === 'SYSTEM_OFFER' ? (
        <div className="offer-panel">
          <h3>!!! SYSTEM ALERT !!!</h3>
          <p>YOU ARE IN A LIFE-OR-DEATH SITUATION.</p>
          <p>A UNIQUE OPPORTUNITY HAS BEEN DETECTED.</p>
          <p>DO YOU WISH TO BECOME A PLAYER?</p>
          <div className="choice-grid compact">
            <button className="submit-button tertiary" type="button" onClick={() => onAction('accept')} disabled={submitting}>
              {submitting ? 'PROCESSING...' : 'ACCEPT'}
            </button>
            <button className="secondary-button" type="button" onClick={() => onAction('decline')} disabled={submitting}>
              {submitting ? 'PROCESSING...' : 'DECLINE'}
            </button>
          </div>
        </div>
      ) : onboarding.currentChapter === 'AWAKENING' ? (
        <div className="offer-panel">
          <h3>PLAYER REGISTRATION COMPLETE</h3>
          <p>WELCOME, PLAYER.</p>
          <p>THE SYSTEM IS ONLINE.</p>
          <button className="submit-button" type="button" onClick={() => onAction('complete')} disabled={submitting}>
            {submitting ? 'FINALIZING...' : 'COMPLETE AWAKENING'}
            <span aria-hidden="true">↗</span>
          </button>
        </div>
      ) : (
        <div className="choice-panel">
          <div className="choice-grid">
            {availableChoices.map((choice) => (
              <button
                type="button"
                key={choice}
                className={selectedChoice === choice ? 'choice-button is-selected' : 'choice-button'}
                onClick={() => setSelectedChoice(choice)}
              >
                {choice}
              </button>
            ))}
          </div>

          <button className="submit-button" type="button" onClick={onChoiceAdvance} disabled={submitting}>
            {submitting ? 'PROCESSING...' : 'CONTINUE'}
            <span aria-hidden="true">↗</span>
          </button>
        </div>
      )}

      <div className="onboarding-meta">
        <button className="text-button" type="button" onClick={() => onAction('skip')} disabled={submitting}>
          Skip cinematic
        </button>
        <button className="text-button" type="button" onClick={() => onAction('replay')} disabled={submitting}>
          Replay awakening
        </button>
      </div>

      {error && <p className="form-error">{error}</p>}
      {notice && <p className="form-status">{notice}</p>}
    </section>
  )
}

function App() {
  const initialResetToken = new URLSearchParams(window.location.search).get('resetToken')
  const initialAuthError = new URLSearchParams(window.location.search).get('authError')
  const [mode, setMode] = useState(initialResetToken ? 'reset' : 'login')
  const [resetToken] = useState(initialResetToken || '')
  const [pendingEmail, setPendingEmail] = useState('')
  const [verificationCode, setVerificationCode] = useState('')
  const [otpCode, setOtpCode] = useState('')
  const [resendSeconds, setResendSeconds] = useState(0)
  const [account, setAccount] = useState(null)
  const [onboarding, setOnboarding] = useState(null)
  const [profileForm, setProfileForm] = useState(INITIAL_PROFILE_FORM)
  const [selectedChoice, setSelectedChoice] = useState('CONTINUE')
  const [csrf, setCsrf] = useState(null)
  const [apiReady, setApiReady] = useState(false)
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [showPassword, setShowPassword] = useState(false)
  const [form, setForm] = useState({ displayName: '', email: '', password: '', confirmPassword: '' })
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  useEffect(() => {
    if (resendSeconds <= 0) return undefined
    const timer = window.setTimeout(() => setResendSeconds((seconds) => seconds - 1), 1000)
    return () => window.clearTimeout(timer)
  }, [resendSeconds])

  async function refreshOnboarding() {
    try {
      const response = await fetch('/api/player/onboarding', { credentials: 'same-origin' })
      if (response.status === 401) {
        setOnboarding(null)
        return null
      }
      if (!response.ok) {
        throw new Error(await readError(response))
      }

      const profileState = await response.json()
      const currentOptions = CHOICE_OPTIONS[profileState.currentChapter] || ['CONTINUE']
      setSelectedChoice(currentOptions[0] || 'CONTINUE')
      setProfileForm({
        playerName: profileState.playerName ?? '',
        mainChallenge: profileState.mainChallenge ?? '',
        additionalChallenges: Array.isArray(profileState.additionalChallenges) ? profileState.additionalChallenges.join(', ') : '',
        goal: profileState.goal ?? '',
        situation: profileState.situation ?? '',
      })
      setOnboarding(profileState)
      return profileState
    } catch (requestError) {
      setError(requestError.message || 'Could not load your awakening state.')
      return null
    }
  }

  useEffect(() => {
    const controller = new AbortController()

    async function initialize() {
      try {
        if (initialAuthError === 'google') {
          setError('Google sign-in did not complete. Check the OAuth client settings and authorized redirect URI, then try again.')
          window.history.replaceState({}, '', window.location.pathname)
        }

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

        if (profileResponse.ok && !initialResetToken) {
          const nextAccount = await profileResponse.json()
          setAccount(nextAccount)
          await refreshOnboarding()
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
  }, [initialAuthError, initialResetToken])

  function updateField(event) {
    setForm((current) => ({ ...current, [event.target.name]: event.target.value }))
  }

  function changeMode(nextMode) {
    setMode(nextMode)
    setError('')
    setNotice('')
  }

  async function submitForm(event) {
    event.preventDefault()
    setError('')
    setNotice('')
    setSubmitting(true)

    try {
      if (mode === 'reset' && form.password !== form.confirmPassword) throw new Error('Your passwords do not match.')
      const endpoint = mode === 'register' ? 'register' : mode === 'verify' ? 'email/verify' : mode === 'otp-request' ? 'email/otp/request' : mode === 'otp-verify' ? 'email/otp/verify' : mode === 'forgot' ? 'password/forgot' : mode === 'reset' ? 'password/reset' : 'login'
      const payload = mode === 'register' ? form : mode === 'verify' ? { email: pendingEmail, code: verificationCode } : mode === 'otp-request' ? { email: form.email } : mode === 'otp-verify' ? { email: pendingEmail, code: otpCode } : mode === 'forgot' ? { email: form.email } : mode === 'reset' ? { token: resetToken, newPassword: form.password } : { email: form.email, password: form.password }

      const response = await fetch(`/api/auth/${endpoint}`, {
        method: 'POST',
        credentials: 'same-origin',
        headers: {
          'Content-Type': 'application/json',
          [csrf.headerName]: csrf.token,
        },
        body: JSON.stringify(payload),
      })

      if (!response.ok) {
        throw new Error(await readError(response))
      }

      if (mode === 'register') {
        setPendingEmail(form.email.trim().toLowerCase())
        setVerificationCode('')
        setMode('verify')
        setForm((current) => ({ ...current, password: '', confirmPassword: '' }))
        setNotice('A verification code has been sent to your email address.')
      } else if (mode === 'otp-request') {
        setPendingEmail(form.email.trim().toLowerCase())
        setOtpCode('')
        setMode('otp-verify')
        setResendSeconds(60)
        const result = await response.json()
        setNotice(result.message)
      } else if (mode === 'otp-verify') {
        const nextAccount = await response.json()
        setAccount(nextAccount)
        await refreshOnboarding()
      } else if (mode === 'forgot' || mode === 'reset') {
        const result = await response.json()
        setMode('login')
        setForm((current) => ({ ...current, password: '', confirmPassword: '' }))
        window.history.replaceState({}, '', '/')
        setNotice(result.message)
      } else {
        const nextAccount = await response.json()
        setAccount(nextAccount)
        await refreshOnboarding()
      }
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

  async function resendEmailOtp() {
    setError('')
    setNotice('')
    setSubmitting(true)

    try {
      const response = await fetch('/api/auth/email/otp/request', {
        method: 'POST',
        credentials: 'same-origin',
        headers: { 'Content-Type': 'application/json', [csrf.headerName]: csrf.token },
        body: JSON.stringify({ email: pendingEmail }),
      })
      if (!response.ok) throw new Error(await readError(response))
      const result = await response.json()
      setNotice(result.message)
      setResendSeconds(60)
    } catch (requestError) {
      setError(
        requestError instanceof TypeError
          ? 'The system could not be reached. Check your connection and try again.'
          : requestError.message || 'Could not request another code.',
      )
    } finally {
      setSubmitting(false)
    }
  }

  async function saveProfile(event) {
    event.preventDefault()
    if (!onboarding || !csrf) return

    setError('')
    setNotice('')
    setSubmitting(true)

    try {
      const additionalChallenges = profileForm.additionalChallenges
        .split(',')
        .map((entry) => entry.trim())
        .filter(Boolean)

      const response = await fetch('/api/player/profile', {
        method: 'PUT',
        credentials: 'same-origin',
        headers: {
          'Content-Type': 'application/json',
          [csrf.headerName]: csrf.token,
        },
        body: JSON.stringify({
          playerName: profileForm.playerName.trim(),
          mainChallenge: profileForm.mainChallenge.trim(),
          additionalChallenges,
          goal: profileForm.goal.trim(),
          situation: profileForm.situation.trim(),
          version: onboarding.version,
        }),
      })

      if (!response.ok) {
        throw new Error(await readError(response))
      }

      const updatedOnboarding = await response.json()
      setOnboarding(updatedOnboarding)
      setNotice('Your player profile has been saved. The awakening can continue.')
    } catch (requestError) {
      setError(requestError.message || 'Could not save your profile.')
    } finally {
      setSubmitting(false)
    }
  }

  async function advanceChapter() {
    if (!onboarding || !csrf) return

    setError('')
    setNotice('')
    setSubmitting(true)

    try {
      const response = await fetch('/api/player/awakening/choices', {
        method: 'POST',
        credentials: 'same-origin',
        headers: {
          'Content-Type': 'application/json',
          [csrf.headerName]: csrf.token,
        },
        body: JSON.stringify({
          chapter: onboarding.currentChapter,
          choice: selectedChoice,
          version: onboarding.version,
        }),
      })

      if (!response.ok) {
        throw new Error(await readError(response))
      }

      const updatedOnboarding = await response.json()
      setOnboarding(updatedOnboarding)
      setNotice('The System has accepted your choice.')
      const nextOptions = CHOICE_OPTIONS[updatedOnboarding.currentChapter] || ['CONTINUE']
      setSelectedChoice(nextOptions[0] || 'CONTINUE')
    } catch (requestError) {
      setError(requestError.message || 'Could not continue the awakening.')
    } finally {
      setSubmitting(false)
    }
  }

  async function triggerAwakeningAction(action) {
    if (!onboarding || !csrf) return

    setError('')
    setNotice('')
    setSubmitting(true)

    try {
      const actionCsrf = await getCsrfToken()
      setCsrf(actionCsrf)
      const response = await fetch(`/api/player/awakening/${action}`, {
        method: 'POST',
        credentials: 'same-origin',
        headers: {
          'Content-Type': 'application/json',
          [actionCsrf.headerName]: actionCsrf.token,
        },
        body: JSON.stringify({ version: onboarding.version }),
      })

      if (!response.ok) {
        throw new Error(await readError(response))
      }

      const updatedOnboarding = await response.json()
      setOnboarding(updatedOnboarding)
      await refreshOnboarding()
      const actionCopy = {
        accept: 'The offer has been accepted. The awakening proceeds.',
        decline: 'The offer was declined. You may revisit the path later.',
        skip: 'The cinematic has been skipped. The System marks the journey complete.',
        complete: 'The awakening is complete. Welcome, Player.',
        replay: 'The dungeon has been reset. Your path begins again.',
      }
      setNotice(actionCopy[action] || 'The System has updated your progress.')
      if (updatedOnboarding.currentChapter && CHOICE_OPTIONS[updatedOnboarding.currentChapter]) {
        const nextOptions = CHOICE_OPTIONS[updatedOnboarding.currentChapter] || ['CONTINUE']
        setSelectedChoice(nextOptions[0] || 'CONTINUE')
      }
    } catch (requestError) {
      setError(requestError.message || 'The awakening could not be updated.')
    } finally {
      setSubmitting(false)
    }
  }

  async function replayAwakening() {
    await triggerAwakeningAction('replay')
  }

  function returnToPasswordLogin() {
    setMode('login')
    setForm((current) => ({ ...current, email: pendingEmail || current.email }))
    setError('')
    setNotice('')
  }

  async function logout() {
    setError('')
    setSubmitting(true)

    try {
      const logoutCsrf = await getCsrfToken()
      setCsrf(logoutCsrf)
      const response = await fetch('/api/auth/logout', {
        method: 'POST',
        credentials: 'same-origin',
        headers: { [logoutCsrf.headerName]: logoutCsrf.token },
      })

      if (!response.ok) {
        throw new Error(await readError(response))
      }

      setAccount(null)
      setOnboarding(null)
      setProfileForm(INITIAL_PROFILE_FORM)
      setForm({ displayName: '', email: '', password: '', confirmPassword: '' })
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

  const onboardingView = account && onboarding && (
    onboarding.status === 'PROFILE_REQUIRED'
      ? (
        <PlayerOnboardingForm
          onboarding={onboarding}
          profileForm={profileForm}
          setProfileForm={setProfileForm}
          submitting={submitting}
          onSave={saveProfile}
          error={error}
          notice={notice}
        />
      )
      : onboarding.status === 'READY' || onboarding.status === 'IN_PROGRESS'
        ? (
          <AwakeningExperience
            onboarding={onboarding}
            selectedChoice={selectedChoice}
            setSelectedChoice={setSelectedChoice}
            submitting={submitting}
            onChoiceAdvance={advanceChapter}
            onAction={triggerAwakeningAction}
            error={error}
            notice={notice}
          />
        )
        : (
          <Dashboard
            account={account}
            onboarding={onboarding}
            onLogout={logout}
            submitting={submitting}
            onReplayAwakening={replayAwakening}
            error={error}
          />
        )
  )

  return (
    <main className={`entry-page${account ? ' is-authenticated' : ''}`}>
      <div className="ambient ambient--one" aria-hidden="true" />
      <div className="ambient ambient--two" aria-hidden="true" />

      {!account && <header className="topbar">
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
      </header>}

      {account ? (
        onboarding === null ? (
          <section className="onboarding-shell onboarding-shell--loading">
            <p className="kicker"><span /> SYSTEM BOOT</p>
            <h2>Loading your awakening status…</h2>
          </section>
        ) : onboardingView
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

            <div className="panel-heading">
              <p className="kicker">{mode === 'login' ? 'WELCOME BACK' : mode === 'register' ? 'CREATE YOUR PROFILE' : mode === 'verify' ? 'VERIFY YOUR EMAIL' : mode === 'otp-request' || mode === 'otp-verify' ? 'EMAIL SIGN-IN' : mode === 'forgot' ? 'ACCOUNT RECOVERY' : 'SECURE YOUR ACCOUNT'}</p>
              <h2 id="access-heading">
                {mode === 'login' ? 'Enter the system.' : mode === 'register' ? 'Begin your journey.' : mode === 'verify' || mode === 'otp-verify' ? 'Check your inbox.' : mode === 'otp-request' ? 'Sign in with email.' : mode === 'forgot' ? 'Recover access.' : 'Set a new password.'}
              </h2>
              <p className="panel-description">
                {mode === 'login' ? 'Sign in to pick up where you left off.' : mode === 'register' ? 'Set up your profile to get started.' : mode === 'verify' ? `Enter the 6-digit code sent to ${pendingEmail}.` : mode === 'otp-request' ? 'We’ll email you a one-time sign-in code.' : mode === 'otp-verify' ? `Enter the 6-digit sign-in code for ${pendingEmail}.` : mode === 'forgot' ? 'Enter your email to receive a reset link if an account exists.' : 'Choose a new password for your account.'}
              </p>
            </div>

            {(mode === 'login' || mode === 'register') && <a className="google-signin" href="/oauth2/authorization/google">
              <span className="google-mark" aria-hidden="true">G</span>
              CONTINUE WITH GOOGLE
            </a>}

            {(mode === 'login' || mode === 'register') && <div className="auth-divider"><span>OR USE EMAIL</span></div>}

            {(mode === 'login' || mode === 'register') && <div className="mode-switch" aria-label="Choose sign-in or registration">
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
            </div>}

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

              {mode === 'verify' && <label className="field">
                <span>6-DIGIT VERIFICATION CODE</span>
                <input autoComplete="one-time-code" inputMode="numeric" maxLength={6} minLength={6} name="verificationCode" onChange={(event) => setVerificationCode(event.target.value.replace(/\D/g, '').slice(0, 6))} placeholder="000000" required value={verificationCode} />
              </label>}

              {mode === 'otp-verify' && <label className="field">
                <span>6-DIGIT SIGN-IN CODE</span>
                <input autoComplete="one-time-code" inputMode="numeric" maxLength={6} minLength={6} name="otpCode" onChange={(event) => setOtpCode(event.target.value.replace(/\D/g, '').slice(0, 6))} placeholder="000000" required value={otpCode} />
              </label>}

              {mode !== 'reset' && mode !== 'verify' && mode !== 'otp-verify' && <label className="field">
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
              </label>}

              {mode !== 'forgot' && mode !== 'verify' && mode !== 'otp-request' && mode !== 'otp-verify' && <label className="field">
                <span>{mode === 'reset' ? 'NEW PASSWORD' : 'PASSWORD'}</span>
                <div className="password-input">
                  <input
                    autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
                    maxLength={128}
                    minLength={mode === 'login' ? undefined : 10}
                    name="password"
                    onChange={updateField}
                    placeholder={mode === 'login' ? 'Enter your password' : 'At least 10 characters'}
                    required
                    type={showPassword ? 'text' : 'password'}
                    value={form.password}
                  />
                  <button className="password-toggle" type="button" onClick={() => setShowPassword((current) => !current)}>
                    {showPassword ? 'HIDE' : 'SHOW'}
                  </button>
                </div>
              </label>}

              {(mode === 'register' || mode === 'reset') && (
                <label className="field">
                  <span>CONFIRM PASSWORD</span>
                  <input
                    autoComplete="new-password"
                    maxLength={128}
                    name="confirmPassword"
                    onChange={updateField}
                    placeholder="Repeat your password"
                    required
                    type={showPassword ? 'text' : 'password'}
                    value={form.confirmPassword}
                  />
                </label>
              )}

              {error && <p className="form-error">{error}</p>}
              {notice && <p className="form-status">{notice}</p>}

              <button className="submit-button" type="submit" disabled={submitting}>
                {submitting
                  ? mode === 'otp-request'
                    ? 'REQUESTING CODE'
                    : mode === 'otp-verify'
                      ? 'VERIFYING CODE'
                      : mode === 'register'
                        ? 'CREATING ACCOUNT'
                        : mode === 'verify'
                          ? 'VERIFYING EMAIL'
                          : mode === 'forgot'
                            ? 'SENDING RESET'
                            : mode === 'reset'
                              ? 'UPDATING PASSWORD'
                              : 'SIGNING IN'
                  : mode === 'otp-request'
                    ? 'REQUEST CODE'
                    : mode === 'otp-verify'
                      ? 'VERIFY CODE'
                      : mode === 'register'
                        ? 'CREATE ACCOUNT'
                        : mode === 'verify'
                          ? 'VERIFY EMAIL'
                          : mode === 'forgot'
                            ? 'SEND RESET LINK'
                            : mode === 'reset'
                              ? 'RESET PASSWORD'
                              : 'SIGN IN'}
                <span aria-hidden="true">↗</span>
              </button>

              {mode === 'login' && (
                <p className="panel-footnote">
                  Need a new code?{' '}
                  <button className="text-button" type="button" onClick={() => changeMode('otp-request')}>
                    Sign in with email
                  </button>
                </p>
              )}

              {mode === 'otp-verify' && (
                <p className="panel-footnote">
                  {resendSeconds > 0 ? `Send another code in ${resendSeconds}s` : 'Didn’t receive it?'}{' '}
                  <button
                    className="text-button"
                    type="button"
                    onClick={resendEmailOtp}
                    disabled={submitting || resendSeconds > 0}
                  >
                    {resendSeconds > 0 ? 'Wait for resend' : 'Send another code'}
                  </button>
                </p>
              )}

              {mode === 'verify' && (
                <p className="panel-footnote">
                  <button className="text-button" type="button" onClick={returnToPasswordLogin}>
                    Return to sign in
                  </button>
                </p>
              )}

              {mode === 'forgot' && (
                <p className="panel-footnote">
                  <button className="text-button" type="button" onClick={() => changeMode('login')}>
                    Back to sign in
                  </button>
                </p>
              )}

              {mode === 'reset' && (
                <p className="panel-footnote">
                  <button className="text-button" type="button" onClick={() => changeMode('login')}>
                    Back to sign in
                  </button>
                </p>
              )}
            </form>
          </section>
        </section>
      )}

      <footer className="page-footer">
        <span>THE SYSTEM</span>
        <span className="footer-separator">//</span>
        <span>PLAYER ENGAGEMENT</span>
      </footer>
    </main>
  )
}

export default App
