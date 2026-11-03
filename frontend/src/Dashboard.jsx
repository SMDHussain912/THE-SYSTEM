import './Dashboard.css'

const attributes = [
  { name: 'STRENGTH', value: 18, detail: 'Physical capability', symbol: 'STR' },
  { name: 'INTELLIGENCE', value: 24, detail: 'Learning & knowledge', symbol: 'INT' },
  { name: 'AGILITY', value: 16, detail: 'Adaptability & speed', symbol: 'AGI' },
  { name: 'VITALITY', value: 21, detail: 'Energy & endurance', symbol: 'VIT' },
  { name: 'DISCIPLINE', value: 27, detail: 'Consistency & habits', symbol: 'DIS' },
  { name: 'WISDOM', value: 19, detail: 'Experience & insight', symbol: 'WIS' },
]

const overview = [
  { label: 'CURRENT STREAK', value: '07', unit: 'DAYS', symbol: '↗' },
  { label: 'QUESTS COMPLETED', value: '24', unit: 'TOTAL', symbol: '✓' },
  { label: 'ACTIVE GOALS', value: '03', unit: 'IN PROGRESS', symbol: '◎' },
]

function Dashboard({ account, onLogout, submitting }) {
  const displayName = account?.displayName || 'PLAYER'

  return (
    <main className="player-dashboard">
      <div className="dashboard-heading">
        <div>
          <p className="kicker"><span /> PLAYER PROFILE // 001</p>
          <h1>Player <span>status.</span></h1>
          <p className="dashboard-subtitle">Your progress, translated into power.</p>
        </div>
        <div className="dashboard-actions">
          <span className="player-online"><i /> SYSTEM ONLINE</span>
          <button className="logout-button" type="button" onClick={onLogout} disabled={submitting}>
            {submitting ? 'SIGNING OUT…' : 'SIGN OUT'} <span aria-hidden="true">↗</span>
          </button>
        </div>
      </div>

      <section className="player-overview" aria-label="Player overview">
        <div className="player-identity">
          <div className="player-emblem" aria-hidden="true"><span>◆</span></div>
          <div>
            <p className="micro-label">REGISTERED PLAYER</p>
            <h2>{displayName}</h2>
            <p className="player-email">{account?.email}</p>
            <span className="rank-label">E-RANK <b> // AWAKENED</b></span>
          </div>
        </div>
        <div className="level-block">
          <div className="level-line">
            <div><span className="micro-label">CURRENT LEVEL</span><strong>12</strong></div>
            <div className="xp-value"><b>650</b><span> / 1,000 XP</span></div>
          </div>
          <div className="xp-track"><span /></div>
          <div className="xp-foot"><span>LEVEL 12</span><span>350 XP TO NEXT LEVEL</span></div>
        </div>
        <div className="overall-power">
          <span className="micro-label">TOTAL POWER</span>
          <strong>125</strong>
          <span className="power-caption">ATTRIBUTE POINTS</span>
        </div>
      </section>

      <div className="dashboard-grid">
        <section className="attributes-section">
          <div className="section-heading">
            <div><p className="kicker"><span /> CHARACTER DEVELOPMENT</p><h2>Core attributes</h2></div>
            <span className="section-index">01 / 03</span>
          </div>
          <div className="attribute-grid">
            {attributes.map((attribute, index) => (
              <article className="attribute-card" key={attribute.name}>
                <div className="attribute-card-top">
                  <span className="attribute-symbol">{attribute.symbol}</span>
                  <span className="attribute-number">0{index + 1}</span>
                </div>
                <div className="attribute-info">
                  <div><h3>{attribute.name}</h3><strong>{attribute.value}</strong></div>
                  <p>{attribute.detail}</p>
                </div>
                <div className="attribute-track"><span style={{ width: `${attribute.value / 30 * 100}%` }} /></div>
                <div className="attribute-card-foot"><span>ATTRIBUTE LEVEL</span><span>{attribute.value} / 30</span></div>
              </article>
            ))}
          </div>
          <p className="prototype-note">PROTOTYPE DATA · ATTRIBUTE VALUES ARE PLACEHOLDERS</p>
        </section>

        <aside className="dashboard-side">
          <section className="overview-section">
            <div className="section-heading compact">
              <div><p className="kicker"><span /> FIELD RECORD</p><h2>Activity overview</h2></div>
              <span className="section-index">02 / 03</span>
            </div>
            <div className="overview-list">
              {overview.map((item) => (
                <div className="overview-item" key={item.label}>
                  <span className="overview-symbol">{item.symbol}</span>
                  <div className="overview-label"><b>{item.label}</b><span>{item.unit}</span></div>
                  <strong>{item.value}</strong>
                </div>
              ))}
            </div>
          </section>

          <section className="system-message">
            <div className="message-top"><span className="message-pulse" /> SYSTEM MESSAGE <span>03 / 03</span></div>
            <div className="message-icon" aria-hidden="true">✧</div>
            <h3>Your journey has begun.</h3>
            <p>Every completed quest is a step forward. Build your attributes through consistent action.</p>
            <span className="message-code">KEEP MOVING FORWARD // PLAYER</span>
          </section>
        </aside>
      </div>
    </main>
  )
}

export default Dashboard
