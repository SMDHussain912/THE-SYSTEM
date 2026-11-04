import { useState } from 'react'
import './Dashboard.css'

const navigation = ['Home', 'Activity', 'Tasks', 'Progress', 'About You']

function Dashboard({ account, onboarding, onLogout, submitting, onReplayAwakening, error }) {
  const [activePage, setActivePage] = useState('Home')
  const displayName = account?.displayName?.trim()
  const onboardingStatus = onboarding?.status
  const canReplay = onboardingStatus === 'COMPLETED' || onboardingStatus === 'SKIPPED'

  return (
    <>
      <header className="topbar dashboard-topbar">
        <a className="brand" href="/" aria-label="The System home">
          <span className="brand-mark" aria-hidden="true"><span /></span>
          <span>THE SYSTEM</span>
        </a>
        <nav className="system-navigation" aria-label="Main navigation">
          {navigation.map((item) => (
            <button
              className={activePage === item ? 'system-nav-link is-active' : 'system-nav-link'}
              key={item}
              type="button"
              aria-current={activePage === item ? 'page' : undefined}
              onClick={() => setActivePage(item)}
            >
              {item}
            </button>
          ))}
          <button className="system-signout" type="button" onClick={onLogout} disabled={submitting}>
            {submitting ? 'Signing out…' : 'Sign out'}
          </button>
        </nav>
      </header>

      <main className="system-workspace">
        {error && <p className="system-error" role="alert">{error}</p>}
        <section className="system-home" aria-live="polite">
          {activePage === 'Home' ? (
            <div className="system-welcome">
              <p className="kicker"><span /> {canReplay ? 'PLAYER STATUS' : 'SYSTEM HOME'}</p>
              <h1>{'Welcome back' + (displayName ? ', ' + displayName : '') + '.'}</h1>
              {onboarding && (
                <div className="system-status-card">
                  <span>Awakening state: {onboardingStatus || 'ACTIVE'}</span>
                  {canReplay && (
                    <button className="text-button" type="button" onClick={onReplayAwakening} disabled={submitting}>
                      Replay awakening
                    </button>
                  )}
                </div>
              )}
            </div>
          ) : (
            <>
              <h1>{activePage}</h1>
              <p className="system-placeholder">This section is being prepared.</p>
            </>
          )}
        </section>
      </main>
    </>
  )
}

export default Dashboard
