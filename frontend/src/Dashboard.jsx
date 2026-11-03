import { useState } from 'react'
import './Dashboard.css'

const navigation = ['Home', 'Activity', 'Tasks', 'Progress', 'About You']

function Dashboard({ account, onLogout, submitting }) {
  const [activePage, setActivePage] = useState('Home')
  const displayName = account?.displayName?.trim()

  return (
    <main className="system-workspace">
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

      <section className="system-home" aria-live="polite">
        {activePage === 'Home' ? (
          <>
            <p className="system-eyebrow">PERSONAL SYSTEM // ONLINE</p>
            <h1>{'Welcome back' + (displayName ? ', ' + displayName : '') + '.'}</h1>
          </>
        ) : (
          <>
            <p className="system-eyebrow">{'WORKSPACE // ' + activePage.toUpperCase()}</p>
            <h1>{activePage}</h1>
            <p className="system-placeholder">This section is being prepared.</p>
          </>
        )}
      </section>
    </main>
  )
}

export default Dashboard
