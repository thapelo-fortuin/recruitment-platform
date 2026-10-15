import { useEffect, useMemo, useState } from 'react'
import {
  ArrowDown, ArrowRight, ArrowUpRight, Bell, BriefcaseBusiness, Check,
  ChevronDown, CircleHelp, Clock3,
  Compass, Database, Filter, GraduationCap, LayoutDashboard, LogOut, Menu, Plus,
  Search, Settings2, ShieldCheck, Sparkles, Send, Users, X,
} from 'lucide-react'

const API = '/api/v1'
const STARTER_JOBS = [
  { id: 101, title: 'Junior Product Designer', department: 'Design', description: 'Help shape thoughtful digital experiences with a curious, collaborative product team.', requiredSkills: 'Figma, UX research, prototyping', status: 'OPEN', company: 'Northstar Studio', location: 'Cape Town · Hybrid', type: 'Graduate programme', posted: '2 days ago', color: 'lilac' },
  { id: 102, title: 'Graduate Software Engineer', department: 'Engineering', description: 'Build reliable tools that help people and businesses do their best work.', requiredSkills: 'Java, APIs, problem solving', status: 'OPEN', company: 'Goodkind Labs', location: 'Johannesburg · Hybrid', type: 'Full-time', posted: '4 days ago', color: 'peach' },
  { id: 103, title: 'Marketing Graduate', department: 'Marketing', description: 'Bring fresh ideas to campaigns, research and the stories we tell our customers.', requiredSkills: 'Writing, social media, analytics', status: 'OPEN', company: 'Common Ground', location: 'Remote · South Africa', type: 'Graduate programme', posted: '1 week ago', color: 'blue' },
  { id: 104, title: 'Junior Data Analyst', department: 'Data & Analytics', description: 'Turn curious questions into clear insights that make everyday decisions better.', requiredSkills: 'Excel, SQL, communication', status: 'OPEN', company: 'Mosaic Finance', location: 'Pretoria · On-site', type: 'Entry level', posted: '1 week ago', color: 'mint' },
]

const readSaved = (key, fallback) => {
  try { return JSON.parse(localStorage.getItem(key)) ?? fallback } catch { return fallback }
}

async function request(path, options = {}, token) {
  const response = await fetch(`${API}${path}`, {
    ...options,
    headers: {
      ...(options.body ? { 'Content-Type': 'application/json' } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers,
    },
  })
  const raw = await response.text()
  let body = raw
  try { body = raw ? JSON.parse(raw) : null } catch { /* Keep plain-text API messages readable. */ }
  if (!response.ok) throw new Error(typeof body === 'string' ? body : body?.message || 'Something went wrong. Please try again.')
  return body
}

function initials(name = 'New friend') {
  return name.split(/[ @._-]/).filter(Boolean).slice(0, 2).map((part) => part[0]?.toUpperCase()).join('') || 'N'
}

function roleLabel(role) { return role === 'RECRUITER' ? 'Recruiter' : 'Candidate' }

function App() {
  const [user, setUser] = useState(() => readSaved('nextstep-user', null))
  const [jobs, setJobs] = useState(() => readSaved('nextstep-jobs', STARTER_JOBS))
  const [applications, setApplications] = useState(() => readSaved('nextstep-applications', []))
  const [securityData, setSecurityData] = useState({ alerts: [], events: [] })
  const [page, setPage] = useState(() => {
    const savedUser = readSaved('nextstep-user', null)
    if (savedUser?.role === 'ADMIN') return 'admin-dashboard'
    if (savedUser?.role === 'RECRUITER') return 'overview'
    return savedUser ? 'roles' : 'home'
  })
  const [search, setSearch] = useState('')
  const [filter, setFilter] = useState('All roles')
  const [locationFilter, setLocationFilter] = useState('All locations')
  const [typeFilter, setTypeFilter] = useState('All role types')
  const [sortOrder, setSortOrder] = useState('newest')
  const [savedJobs, setSavedJobs] = useState(() => readSaved('nextstep-saved-jobs', []))
  const [savedOnly, setSavedOnly] = useState(false)
  const [authOpen, setAuthOpen] = useState(false)
  const [authMode, setAuthMode] = useState('signup')
  const [authRole, setAuthRole] = useState('CANDIDATE')
  const [jobOpen, setJobOpen] = useState(false)
  const [mobileNav, setMobileNav] = useState(false)
  const [toast, setToast] = useState('')
  const [loading, setLoading] = useState(false)
  const [apiOnline, setApiOnline] = useState(true)

  const candidate = user?.role === 'CANDIDATE'
  const recruiter = user?.role === 'RECRUITER'
  const admin = user?.role === 'ADMIN'
  const activeJobs = useMemo(() => jobs.filter((job) => (job.status || 'OPEN') === 'OPEN'), [jobs])
  const ownJobs = useMemo(() => recruiter ? jobs.filter((job) => job.postedByEmail === user.email || job.demo) : [], [jobs, recruiter, user?.email])
  const visibleJobs = useMemo(() => {
    const query = search.trim().toLowerCase()
    const filtered = activeJobs.filter((job) => {
      const matchesText = !query || [job.title, job.department, job.description, job.requiredSkills, job.company].some((value) => value?.toLowerCase().includes(query))
      const matchesLocation = locationFilter === 'All locations' || (job.location || 'South Africa · Hybrid').toLowerCase().includes(locationFilter.toLowerCase())
      const matchesType = typeFilter === 'All role types' || (job.type || 'Graduate opportunity') === typeFilter
      const matchesSaved = !savedOnly || savedJobs.some((id) => Number(id) === Number(job.id))
      return matchesText && matchesLocation && matchesType && matchesSaved && (filter === 'All roles' || job.department === filter)
    })
    return [...filtered].sort((a, b) => sortOrder === 'title' ? (a.title || '').localeCompare(b.title || '') : Number(b.id) - Number(a.id))
  }, [activeJobs, filter, locationFilter, typeFilter, savedOnly, savedJobs, search, sortOrder])

  useEffect(() => { localStorage.setItem('nextstep-jobs', JSON.stringify(jobs)) }, [jobs])
  useEffect(() => { localStorage.setItem('nextstep-applications', JSON.stringify(applications)) }, [applications])
  useEffect(() => { localStorage.setItem('nextstep-saved-jobs', JSON.stringify(savedJobs)) }, [savedJobs])
  useEffect(() => { user ? localStorage.setItem('nextstep-user', JSON.stringify(user)) : localStorage.removeItem('nextstep-user') }, [user])

  useEffect(() => {
    let alive = true
    async function sync() {
      try {
        const freshJobs = await request('/jobs', {}, user?.demo ? undefined : user?.token)
        if (alive && Array.isArray(freshJobs)) {
          setJobs(freshJobs.map((job) => ({ ...job, company: job.company || 'A great local team', location: job.location || 'South Africa · Hybrid', type: 'Graduate opportunity', posted: 'New this week' })))
          setApiOnline(true)
        }
        if (user?.token && user.role === 'CANDIDATE') {
          const mine = await request('/applications', {}, user.token)
          if (alive && Array.isArray(mine)) setApplications(mine)
        } else if (user?.token && user.role === 'RECRUITER') {
          const inbox = await request('/applications/recruiter', {}, user.token)
          if (alive && Array.isArray(inbox)) setApplications(inbox)
        } else if (user?.token && user.role === 'ADMIN') {
          const [alerts, events] = await Promise.all([
            request('/soc/alerts', {}, user.token),
            request('/soc/events', {}, user.token),
          ])
          if (alive) setSecurityData({ alerts: Array.isArray(alerts) ? alerts : [], events: Array.isArray(events) ? events : [] })
        }
      } catch {
        if (alive && !user?.demo) setApiOnline(false)
      }
    }
    sync()
    return () => { alive = false }
  }, [user?.email, user?.role, user?.token, user?.demo])

  useEffect(() => {
    if (!toast) return undefined
    const timer = window.setTimeout(() => setToast(''), 3600)
    return () => window.clearTimeout(timer)
  }, [toast])

  function openAuth(mode = 'signup', role = 'CANDIDATE') {
    setAuthMode(mode); setAuthRole(role); setAuthOpen(true); setMobileNav(false)
  }

  function enterDemo(role) {
    const demoUser = role === 'RECRUITER'
      ? { name: 'Taylor Morgan', email: 'taylor@northstar.demo', role, demo: true }
      : { name: 'Alex Morgan', email: 'alex@graduate.demo', role, demo: true }
    setUser(demoUser)
    setPage(role === 'RECRUITER' ? 'overview' : 'roles')
    setAuthOpen(false)
    setToast(`Welcome to your ${roleLabel(role).toLowerCase()} preview`)
  }

  function logout() { setUser(null); setPage('home'); setApplications(readSaved('nextstep-applications', [])); setToast('You’re all signed out') }

  async function submitAuth(event) {
    event.preventDefault()
    const data = new FormData(event.currentTarget)
    const body = Object.fromEntries(data.entries())
    setLoading(true)
    try {
      if (authMode === 'admin-setup') {
        const result = await request('/auth/admin-setup', { method: 'POST', body: JSON.stringify({ name: body.name, email: body.email, password: body.password, setupKey: body.setupKey }) })
        const nextUser = { ...result, role: 'ADMIN' }
        setUser(nextUser)
        setPage('admin-dashboard')
        setAuthOpen(false)
        setToast('Administrator account created. Welcome to the admin console.')
        return
      }
      if (authMode === 'signup') {
        await request('/auth/register', { method: 'POST', body: JSON.stringify({ name: body.name, email: body.email, password: body.password, role: authRole }) })
      }
      const result = await request('/auth/login', { method: 'POST', body: JSON.stringify({ email: body.email, password: body.password }) })
      const nextUser = { ...result, role: result.role?.toUpperCase() }
      setUser(nextUser)
      setPage(nextUser.role === 'ADMIN' ? 'admin-dashboard' : nextUser.role === 'RECRUITER' ? 'overview' : 'roles')
      setAuthOpen(false)
      setToast(authMode === 'signup' ? 'Your account is ready. Welcome to Nextstep!' : `Welcome back, ${result.name || result.email.split('@')[0]}`)
    } catch (error) {
      setToast(error.message || 'We couldn’t connect to the service. Try the preview below.')
    } finally { setLoading(false) }
  }

  async function applyToJob(job) {
    if (!user) { openAuth('signup', 'CANDIDATE'); return }
    if (!candidate) { setToast('Switch to a candidate account to apply for roles'); return }
    if (applications.some((app) => Number(app.jobId) === Number(job.id))) { setToast('You’ve already applied to this role'); return }
    if (!window.confirm(`Submit your application for ${job.title}? You can track its progress in My applications.`)) return
    if (user.demo) {
      setApplications((current) => [{ id: Date.now(), jobId: job.id, candidateEmail: user.email, status: 'APPLIED', appliedAt: new Date().toISOString(), demo: true }, ...current])
      setPage('applications'); setToast('Application sent. Track your progress in My applications.')
      return
    }
    try {
      const created = await request('/applications', { method: 'POST', body: JSON.stringify({ jobId: Number(job.id) }) }, user.token)
      setApplications((current) => [created, ...current])
      setPage('applications'); setToast('Application sent. Track your progress in My applications.')
    } catch (error) { setToast(error.message) }
  }

  async function createJob(event) {
    event.preventDefault()
    const data = Object.fromEntries(new FormData(event.currentTarget).entries())
    const job = { ...data, status: 'OPEN', postedByEmail: user.email, company: 'Your company', location: data.location || 'South Africa · Hybrid', type: 'Graduate opportunity', posted: 'Just posted' }
    if (!user.demo) {
      try {
        const created = await request('/jobs', { method: 'POST', body: JSON.stringify({ title: data.title, department: data.department, description: data.description, requiredSkills: data.requiredSkills }) }, user.token)
        setJobs((current) => [created, ...current])
      } catch (error) { setToast(error.message); return }
    } else {
      job.id = Date.now(); job.demo = true
      setJobs((current) => [job, ...current])
    }
    setJobOpen(false); setPage('jobs'); setToast('Your opportunity is live')
  }

  async function closeJob(job) {
    if (user.demo) {
      setJobs((current) => current.map((item) => item.id === job.id ? { ...item, status: 'CLOSED' } : item))
      setToast('Role closed. You can still review existing applications.')
      return
    }
    try {
      const updated = await request(`/jobs/${job.id}/close`, { method: 'PUT' }, user.token)
      setJobs((current) => current.map((item) => item.id === job.id ? { ...item, ...updated } : item))
      setToast('Role closed. You can still review existing applications.')
    } catch (error) { setToast(error.message) }
  }

  async function updateApplication(application, status) {
    if (user.demo) {
      setApplications((current) => current.map((item) => item.id === application.id ? { ...item, status } : item))
      setToast('Application status updated')
      return
    }
    try {
      const updated = await request(`/applications/${application.id}/status`, { method: 'PUT', body: JSON.stringify({ status }) }, user.token)
      setApplications((current) => current.map((item) => item.id === application.id ? { ...item, ...updated } : item))
      setToast('Application status updated')
    } catch (error) { setToast(error.message) }
  }

  const go = (nextPage) => { setPage(nextPage); setMobileNav(false) }
  const displayName = user?.name || user?.email?.split('@')[0] || 'friend'
  const tabs = candidate
    ? [{ id: 'roles', label: 'Find a role', icon: Compass }, { id: 'applications', label: 'My applications', icon: BriefcaseBusiness }]
    : admin
      ? [{ id: 'admin-dashboard', label: 'Admin dashboard', icon: LayoutDashboard }, { id: 'security', label: 'Security insights', icon: ShieldCheck }]
      : [{ id: 'overview', label: 'Overview', icon: LayoutDashboard }, { id: 'jobs', label: 'My opportunities', icon: BriefcaseBusiness }, { id: 'applicants', label: 'Applicants', icon: Users }]
  const navigationTabs = tabs

  return (
    <div className="app-shell">
      <header className="topbar">
        <a className="brand" href="#home" onClick={(event) => { event.preventDefault(); go(user ? (candidate ? 'roles' : 'overview') : 'home') }} aria-label="Nextstep home">
          <span className="brand-mark"><span /></span><span>nextstep<span className="brand-period">.</span></span>
        </a>
        <nav className={`top-links ${mobileNav ? 'is-open' : ''}`}>
          {user ? navigationTabs.map((tab) => <button key={tab.id} className={`top-link ${page === tab.id ? 'active' : ''}`} onClick={() => go(tab.id)}>{tab.label}</button>) : <><a href="#opportunities" onClick={() => go('home')}>Explore roles</a><a href="#how-it-works">How it works</a><button className="top-link" onClick={() => openAuth('signup', 'RECRUITER')}>For recruiters</button></>}
          {user && <button className="mobile-logout" onClick={logout}><LogOut size={16} /> Sign out</button>}
        </nav>
        <div className="top-actions">
          {user ? <><button className="icon-button notification-button" aria-label="Notifications"><Bell size={18} /><i /></button><button className="profile-chip" onClick={logout} title="Sign out"><span className="avatar avatar-small">{initials(displayName)}</span><span className="profile-name">{displayName.split(' ')[0]}</span><ChevronDown size={15} /></button></> : <><button className="login-link" onClick={() => openAuth('login')}>Log in</button><button className="button button-dark button-small" onClick={() => openAuth('signup')}>Get started <ArrowUpRight size={15} /></button></>}
          <button className="icon-button mobile-menu" aria-label="Toggle navigation" onClick={() => setMobileNav((value) => !value)}>{mobileNav ? <X size={19} /> : <Menu size={19} />}</button>
        </div>
      </header>

      {!user && page === 'home' ? <Landing jobs={activeJobs} onExplore={() => { go('roles'); setUser(null) }} onApply={applyToJob} onAuth={openAuth} /> : user ? (
        <main className="workspace">
          <aside className="sidebar">
            <div className="sidebar-greeting"><span className="eyebrow">{admin ? 'SYSTEM ADMINISTRATION' : 'YOUR SPACE'}</span><h2>{candidate ? 'Career launchpad' : admin ? 'Admin console' : 'Talent studio'}</h2><p>{candidate ? 'Your next chapter starts here.' : admin ? 'Monitor platform security.' : 'Find your next great hire.'}</p></div>
            <span className="side-label">WORKSPACE</span>
            {navigationTabs.map(({ id, label, icon: Icon }) => <button key={id} className={`side-link ${page === id ? 'selected' : ''}`} onClick={() => go(id)}><Icon size={17} strokeWidth={1.8} />{label}{id === 'applicants' && applications.length > 0 && <span className="side-count">{applications.length}</span>}</button>)}
            <div className="sidebar-bottom"><div className="help-card"><span className="help-icon"><CircleHelp size={17} /></span><strong>Need a hand?</strong><p>We’re here to help you take the next step.</p><button onClick={() => setToast('Reach our team at hello@nextstep.careers')}>Visit help centre <ArrowRight size={14} /></button></div><button className="side-link settings-link" onClick={logout}><Settings2 size={17} />Sign out</button></div>
          </aside>
      <section className="workspace-main">
            {!apiOnline && !user.demo && <div className="api-note"><span className="status-dot" /> API is offline. You can explore the interface in preview mode; live changes will need the services running.</div>}
            {user.demo && <div className="api-note preview-note"><span className="status-dot" /> Preview mode: changes are saved in this browser only.</div>}
            {candidate && page === 'roles' && <CandidateRoles user={user} jobs={visibleJobs} allJobs={activeJobs} search={search} setSearch={setSearch} filter={filter} setFilter={setFilter} locationFilter={locationFilter} setLocationFilter={setLocationFilter} typeFilter={typeFilter} setTypeFilter={setTypeFilter} sortOrder={sortOrder} setSortOrder={setSortOrder} savedJobs={savedJobs} setSavedJobs={setSavedJobs} savedOnly={savedOnly} setSavedOnly={setSavedOnly} applications={applications} onApply={applyToJob} onExplore={() => { setSearch(''); setFilter('All roles'); setLocationFilter('All locations'); setTypeFilter('All role types'); setSavedOnly(false) }} />}
            {candidate && page === 'applications' && <CandidateApplications applications={applications} jobs={jobs} displayName={displayName} onBrowse={() => go('roles')} />}
            {recruiter && page === 'overview' && <RecruiterOverview jobs={ownJobs} applications={applications} onCreate={() => setJobOpen(true)} onGo={go} displayName={displayName} />}
            {recruiter && page === 'jobs' && <RecruiterJobs jobs={ownJobs} onCreate={() => setJobOpen(true)} onClose={closeJob} onApplicants={() => go('applicants')} />}
            {recruiter && page === 'applicants' && <RecruiterApplicants applications={applications} jobs={jobs} onUpdate={updateApplication} />}
            {admin && page === 'admin-dashboard' && <AdminDashboard user={user} alerts={securityData.alerts} events={securityData.events} onOpenSecurity={() => go('security')} />}
            {admin && page === 'security' && <SecurityQuery token={user.token} />}
          </section>
        </main>
      ) : <main className="workspace guest-workspace"><CandidateRoles user={null} jobs={visibleJobs} allJobs={activeJobs} search={search} setSearch={setSearch} filter={filter} setFilter={setFilter} locationFilter={locationFilter} setLocationFilter={setLocationFilter} typeFilter={typeFilter} setTypeFilter={setTypeFilter} sortOrder={sortOrder} setSortOrder={setSortOrder} savedJobs={savedJobs} setSavedJobs={setSavedJobs} savedOnly={savedOnly} setSavedOnly={setSavedOnly} applications={applications} onApply={applyToJob} onExplore={() => { setSearch(''); setFilter('All roles'); setLocationFilter('All locations'); setTypeFilter('All role types'); setSavedOnly(false) }} /></main>}

      <footer className="site-footer"><a className="brand footer-brand" href="#home" onClick={(e) => { e.preventDefault(); go(user ? (candidate ? 'roles' : 'overview') : 'home') }}><span className="brand-mark"><span /></span><span>nextstep<span className="brand-period">.</span></span></a><span>Made for the beginning of something good.</span><span>© {new Date().getFullYear()} Nextstep</span></footer>
      {authOpen && <AuthModal mode={authMode} role={authRole} setMode={setAuthMode} setRole={setAuthRole} onSubmit={submitAuth} onClose={() => setAuthOpen(false)} onDemo={enterDemo} loading={loading} />}
      {jobOpen && <JobModal onClose={() => setJobOpen(false)} onSubmit={createJob} />}
      {toast && <div className="toast" role="status"><span className="toast-check"><Check size={15} /></span>{toast}<button onClick={() => setToast('')} aria-label="Dismiss"><X size={15} /></button></div>}
    </div>
  )
}

function Landing({ jobs, onExplore, onApply, onAuth }) {
  return <>
    <main className="landing">
      <section className="hero">
        <div className="hero-copy"><div className="pill"><span className="pill-spark"><Sparkles size={13} /></span> THE START OF SOMETHING GOOD</div>
          <h1>Your first big<br />move starts <span className="hero-highlight">here.</span><span className="hero-orbit">✳</span></h1>
          <p className="hero-subtitle">A little less job hunting. A lot more <strong>finding your thing.</strong> Meet opportunities made for fresh starts — and people who can’t wait to meet you.</p>
          <div className="hero-actions"><button className="button button-dark button-large" onClick={onExplore}>Find your first role <ArrowRight size={17} /></button><button className="button button-light button-large" onClick={() => onAuth('signup', 'RECRUITER')}>I’m hiring <ArrowUpRight size={16} /></button></div>
          <div className="hero-proof"><div className="avatar-stack"><span className="avatar avatar-peach">JM</span><span className="avatar avatar-blue">AK</span><span className="avatar avatar-yellow">LN</span><span className="avatar avatar-lilac">+</span></div><div><div className="stars">★★★★★ <span>4.9/5</span></div><p>Made for where you are right now</p></div></div>
        </div>
        <div className="hero-art" aria-label="Illustration of graduates and job opportunities">
          <div className="art-backdrop art-blob-one" /><div className="art-backdrop art-blob-two" /><div className="art-orbit orbit-a" /><div className="art-orbit orbit-b" />
          <div className="art-sun"><Sparkles size={22} /></div>
          <div className="graduate graduate-main"><div className="grad-hair" /><div className="grad-face"><span className="face-eyes">• &nbsp; •</span><span className="face-smile" /></div><div className="grad-neck" /><div className="grad-body"><span className="body-seam" /><span className="body-arm" /></div><span className="grad-tassel">✦</span></div>
          <div className="graduate graduate-small"><div className="grad-hair" /><div className="grad-face"><span className="face-eyes">• &nbsp; •</span><span className="face-smile" /></div><div className="grad-neck" /><div className="grad-body"><span className="body-seam" /></div></div>
          <div className="floating-card match-card"><span className="match-icon"><GraduationCap size={17} /></span><div><small>YOUR NEXT STEP</small><strong>It’s a match!</strong><span>4 new roles this week</span></div><span className="match-dot" /></div>
          <div className="floating-card people-card"><span className="mini-avatars"><i>J</i><i>M</i><i>A</i></span><span><strong>Good people</strong><small>are hiring for you</small></span><ArrowUpRight size={15} /></div>
          <div className="floating-card stat-card"><span className="stat-stars">★★★★★</span><strong>Small steps.<br />Big things.</strong><span className="stat-line" /></div>
          <svg className="hero-squiggle" viewBox="0 0 140 22" fill="none" aria-hidden="true"><path d="M2 10C18 10 18 2 34 2S50 10 66 10 82 2 98 2s16 8 32 8" stroke="#A789FF" strokeWidth="2" strokeLinecap="round"/><path d="M2 18c16 0 16-8 32-8s16 8 32 8 16-8 32-8 16 8 32 8" stroke="#C4B2FF" strokeWidth="2" strokeLinecap="round"/></svg>
          <span className="hero-note note-left">A fresh start,<br />on your terms <ArrowDown size={15} /></span><span className="hero-note note-right">the good stuff<br />is out there ↗</span>
        </div>
        <div className="hero-bottom-line"><span>YOUR DEGREE IS THE BEGINNING.</span><span className="line-dash"/><span>LET’S FIND WHAT’S NEXT.</span><span className="line-star">✳</span></div>
      </section>
      <section className="value-strip" id="how-it-works"><div><span className="value-number">01</span><div><strong>Find your fit</strong><p>Good roles, made for new grads.</p></div></div><div><span className="value-number">02</span><div><strong>Show up as you</strong><p>Your first job should feel like you.</p></div></div><div><span className="value-number">03</span><div><strong>Make a real start</strong><p>Apply, follow along, take the next step.</p></div></div><button className="text-link" onClick={onExplore}>Explore opportunities <ArrowRight size={16} /></button></section>
      <section className="preview-section" id="opportunities"><div className="section-heading"><div><span className="eyebrow">A GOOD PLACE TO BEGIN</span><h2>Roles with room<br />to <span>become.</span></h2></div><button className="button button-outline" onClick={onExplore}>See all roles <ArrowRight size={16} /></button></div><div className="job-grid">{jobs.slice(0, 3).map((job) => <JobCard key={job.id} job={job} onApply={onApply} applied={false} compact />)}</div></section>
      <section className="recruiter-banner"><div className="banner-orb">✳</div><span className="eyebrow">FOR THE PEOPLE BUILDING TEAMS</span><h2>Meet someone at the<br /><span>start of great.</span></h2><p>Find curious, capable graduates ready to grow with your team.</p><button className="button button-dark" onClick={() => onAuth('signup', 'RECRUITER')}>Find your next hire <ArrowRight size={16} /></button><span className="banner-doodle">good things<br />start somewhere</span></section>
    </main>
  </>
}

function AdminDashboard({ user, alerts, events, onOpenSecurity }) {
  const openAlerts = alerts.filter((alert) => (alert.status || 'OPEN').toUpperCase() === 'OPEN')
  const highAlerts = openAlerts.filter((alert) => ['HIGH', 'CRITICAL'].includes((alert.severity || '').toUpperCase()))
  const latest = [...alerts].sort((a, b) => new Date(b.createdAt || 0) - new Date(a.createdAt || 0)).slice(0, 6)
  return <div className="content-wrap admin-dashboard">
    <div className="welcome-row recruiter-welcome"><div><span className="eyebrow">ADMINISTRATOR CONSOLE</span><h1>Platform <span>security.</span></h1><p>Welcome, {user.name || user.email}. Review active alerts and investigate with Security insights.</p></div><button className="button button-dark" onClick={onOpenSecurity}><ShieldCheck size={16} /> Open security insights</button></div>
    <div className="metric-grid"><MetricCard label="Open alerts" value={openAlerts.length} note="Awaiting review" icon={Bell} tint="peach" /><MetricCard label="High severity" value={highAlerts.length} note="High or critical alerts" icon={ShieldCheck} tint="lilac" /><MetricCard label="Recorded events" value={events.length} note="Available in SOC event log" icon={Clock3} tint="mint" /></div>
    <section className="panel admin-alert-panel"><div className="panel-header"><div><span className="eyebrow">SECURITY OVERVIEW</span><h2>Recent alerts</h2></div><button className="text-link" onClick={onOpenSecurity}>Investigate alerts <ArrowRight size={15} /></button></div>
      {latest.length ? <div className="admin-alert-list">{latest.map((alert) => <article className="admin-alert-row" key={alert.alertId}><span className={`admin-severity severity-${(alert.severity || 'info').toLowerCase()}`}><i />{alert.severity || 'INFO'}</span><div><strong>{alert.ruleName || 'Security alert'}</strong><p>{alert.description || 'No additional details available.'}</p><small>{alert.affectedService || 'Platform'} · {alert.createdAt ? new Date(alert.createdAt).toLocaleString() : 'Time unavailable'}</small></div><ApplicationStatus status={alert.status || 'OPEN'} /></article>)}</div> : <div className="empty-inline"><p>No alerts have been recorded yet. New security alerts will appear here.</p></div>}
    </section>
    <p className="admin-data-note">Security records are visible to administrator accounts only.</p>
  </div>
}

function CandidateRoles({ user, jobs, allJobs, search, setSearch, filter, setFilter, locationFilter, setLocationFilter, typeFilter, setTypeFilter, sortOrder, setSortOrder, savedJobs, setSavedJobs, savedOnly, setSavedOnly, applications, onApply, onExplore }) {
  const departments = ['All roles', ...new Set(allJobs.map((job) => job.department).filter(Boolean))]
  const locations = ['All locations', ...new Set(allJobs.map((job) => job.location || 'South Africa · Hybrid'))]
  const types = ['All role types', ...new Set(allJobs.map((job) => job.type || 'Graduate opportunity'))]
  const toggleSaved = (jobId) => setSavedJobs((current) => current.some((id) => Number(id) === Number(jobId)) ? current.filter((id) => Number(id) !== Number(jobId)) : [...current, jobId])
  return <div className="content-wrap"><div className="welcome-row"><div><span className="eyebrow">{user ? 'YOUR NEXT CHAPTER' : 'A GOOD PLACE TO BEGIN'}</span><h1>{user ? <>Hey {user.name?.split(' ')[0] || 'there'}, <span>you’ve got this.</span></> : <>Find your <span>first thing.</span></>}</h1><p>{user ? 'Here are a few opportunities that could be the start of something.' : 'Your degree got you here. Let’s find the opportunity that takes you somewhere.'}</p></div><div className="welcome-sticker"><Sparkles size={18} /><span>Good things<br />are ahead</span></div></div>
    <div className="candidate-search"><div className="search-box"><Search size={18} /><input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Role, skill or company" aria-label="Search roles, skills, or companies" /><kbd>⌘ K</kbd></div><label className="filter-box"><Filter size={16} /><select value={filter} onChange={(event) => setFilter(event.target.value)} aria-label="Filter by department">{departments.map((item) => <option key={item}>{item}</option>)}</select><ChevronDown size={15} /></label><button className="button button-dark search-button" onClick={() => { setSearch(''); onExplore() }}>Clear filters <X size={15} /></button></div>
    <div className="candidate-extra-filters"><label className="filter-box"><span className="sr-only">Location</span><select value={locationFilter} onChange={(event) => setLocationFilter(event.target.value)} aria-label="Filter by location">{locations.map((item) => <option key={item}>{item}</option>)}</select><ChevronDown size={15} /></label><label className="filter-box"><span className="sr-only">Role type</span><select value={typeFilter} onChange={(event) => setTypeFilter(event.target.value)} aria-label="Filter by role type">{types.map((item) => <option key={item}>{item}</option>)}</select><ChevronDown size={15} /></label><button className={`saved-filter ${savedOnly ? 'selected' : ''}`} onClick={() => setSavedOnly((value) => !value)} aria-pressed={savedOnly}>♡ Saved roles ({savedJobs.length})</button></div>
    <div className="jobs-heading"><div><h2>{savedOnly ? 'Saved opportunities' : 'Fresh opportunities'} <span className="result-count">{jobs.length}</span></h2><p>Thoughtful teams looking for their next new grad.</p></div><label className="sort-control">Sort by <select value={sortOrder} onChange={(event) => setSortOrder(event.target.value)} aria-label="Sort opportunities"><option value="newest">Newest</option><option value="title">Role title</option></select></label></div>
    {jobs.length ? <div className="job-grid">{jobs.map((job) => <JobCard key={job.id} job={job} onApply={onApply} applied={applications.some((app) => Number(app.jobId) === Number(job.id))} saved={savedJobs.some((id) => Number(id) === Number(job.id))} onToggleSaved={() => toggleSaved(job.id)} />)}</div> : <EmptyState icon={Search} title={savedOnly ? 'No saved roles yet' : 'No roles match those filters'} text={savedOnly ? 'Save an opportunity with the heart icon and it will be easy to find here.' : 'Try changing your search or filters to see more opportunities.'} action="Clear filters" onAction={onExplore} />}
    <div className="candidate-nudge"><span className="nudge-icon"><Sparkles size={18} /></span><div><strong>Your first job is out there.</strong><p>Keep exploring — new opportunities are added every week.</p></div><span className="nudge-doodle">✳</span></div>
  </div>
}

function JobCard({ job, onApply, applied, saved = false, onToggleSaved, compact = false }) {
  const color = job.color || ['lilac', 'peach', 'blue', 'mint'][Math.abs(Number(job.id) || 0) % 4]
  return <article className={`job-card ${compact ? 'job-card-compact' : ''}`}><div className="job-card-top"><div className={`company-mark mark-${color}`}>{(job.company || job.department || 'N').split(' ').map((word) => word[0]).slice(0, 2).join('').toUpperCase()}</div><span className="job-posted"><span className="status-dot" />{job.posted || 'New this week'}</span><button className={`bookmark-button ${saved ? 'saved' : ''}`} aria-label={saved ? `Remove ${job.title} from saved roles` : `Save ${job.title}`} aria-pressed={saved} onClick={onToggleSaved}>{saved ? '♥' : '♡'}</button></div><span className="job-dept">{job.department || 'Early careers'}</span><h3>{job.title}</h3><p className="job-company">{job.company || 'A great local team'} <span>·</span> {job.location || 'South Africa · Hybrid'}</p><p className="job-description">{job.description || 'A chance to learn, contribute and grow with a thoughtful team.'}</p><div className="job-tags">{(job.requiredSkills || '').split(',').filter(Boolean).slice(0, 3).map((skill) => <span key={skill}>{skill.trim()}</span>)}<span>{job.type || 'Graduate opportunity'}</span></div><div className="job-card-bottom"><span><Clock3 size={14} /> Early career</span><button className={`apply-link ${applied ? 'is-applied' : ''}`} onClick={() => onApply(job)} disabled={applied}>{applied ? <>Applied <Check size={15} /></> : <>Quick apply <ArrowUpRight size={15} /></>}</button></div></article>
}

function CandidateApplications({ applications, jobs, displayName, onBrowse }) {
  return <div className="content-wrap"><div className="welcome-row"><div><span className="eyebrow">YOU’RE MAKING MOVES</span><h1>Look at you, <span>{displayName.split(' ')[0]}.</span></h1><p>Every application is a little step forward. Here’s where things stand.</p></div><div className="welcome-sticker"><BriefcaseBusiness size={18} /><span>Your next<br />chapter</span></div></div>
    <div className="jobs-heading application-heading"><div><h2>Your applications <span className="result-count">{applications.length}</span></h2><p>We’ll keep your progress easy to follow.</p></div></div>
    {!applications.length ? <EmptyState icon={BriefcaseBusiness} title="Your next step is waiting" text="When you apply to a role, you’ll see its progress here." action="Explore roles" onAction={onBrowse} /> : <div className="application-list">{applications.map((application) => { const job = jobs.find((item) => Number(item.id) === Number(application.jobId)); return <article className="application-row application-row-expanded" key={application.id}><div className={`company-mark mark-${job?.color || 'lilac'}`}>{(job?.company || 'N').split(' ').map((word) => word[0]).slice(0, 2).join('').toUpperCase()}</div><div className="application-info"><span className="job-dept">{job?.department || 'Graduate role'}</span><h3>{job?.title || `Opportunity #${application.jobId}`}</h3><p>{job?.company || 'Application submitted'} <span>·</span> Applied {application.appliedAt ? new Date(application.appliedAt).toLocaleDateString(undefined, { month: 'short', day: 'numeric' }) : 'recently'}</p><ApplicationProgress status={application.status} /></div><ApplicationStatus status={application.status} /></article> })}</div>}
  </div>
}

function RecruiterOverview({ jobs, applications, onCreate, onGo, displayName }) {
  const openJobs = jobs.filter((job) => job.status === 'OPEN')
  const recently = applications.slice(0, 3)
  return <div className="content-wrap"><div className="welcome-row recruiter-welcome"><div><span className="eyebrow">YOUR TALENT STUDIO</span><h1>Good morning, <span>{displayName.split(' ')[0]}.</span></h1><p>Let’s find someone with a bright future — and give them a great place to start.</p></div><button className="button button-dark" onClick={onCreate}><Plus size={17} /> Post an opportunity</button></div>
    <div className="metric-grid"><MetricCard label="Open opportunities" value={openJobs.length} note="Ready for new grads" icon={BriefcaseBusiness} tint="lilac" /><MetricCard label="Applicants to meet" value={applications.length} note="Across your opportunities" icon={Users} tint="peach" /><MetricCard label="Roles filled" value={jobs.filter((job) => job.status === 'FILLED').length} note="Great starts made" icon={Sparkles} tint="mint" /></div>
    <div className="split-panels"><section className="panel"><div className="panel-header"><div><span className="eyebrow">IN THE SPOTLIGHT</span><h2>Your opportunities</h2></div><button className="text-link" onClick={() => onGo('jobs')}>View all <ArrowRight size={15} /></button></div>{jobs.slice(0, 3).map((job) => <div className="mini-job-row" key={job.id}><span className={`company-mark mark-${job.color || 'lilac'}`}>{(job.department || 'N')[0]}</span><div><strong>{job.title}</strong><span>{job.department} · {job.location || 'South Africa'}</span></div><ApplicationStatus status={job.status} /></div>)}{!jobs.length && <EmptyInline text="Post your first opportunity and start meeting new grads." onClick={onCreate} />}</section>
      <section className="panel applicants-panel"><div className="panel-header"><div><span className="eyebrow">FRESH FACES</span><h2>Recent applicants</h2></div><button className="text-link" onClick={() => onGo('applicants')}>See all <ArrowRight size={15} /></button></div>{recently.map((app) => <div className="person-row" key={app.id}><span className="avatar avatar-lilac">{initials(app.candidateEmail)}</span><div><strong>{app.candidateEmail?.split('@')[0] || 'New candidate'}</strong><span>{jobs.find((job) => Number(job.id) === Number(app.jobId))?.title || 'Your opportunity'}</span></div><ApplicationStatus status={app.status} /></div>)}{!recently.length && <EmptyInline text="Your first applicant could be a great fit." onClick={() => onGo('jobs')} />}</section></div>
    <div className="recruiter-tip"><span className="tip-icon"><Sparkles size={18} /></span><span><strong>A little encouragement goes a long way.</strong><small>Keep candidates in the loop as they move through your process.</small></span><ArrowRight size={17} /></div>
  </div>
}

function RecruiterJobs({ jobs, onCreate, onClose, onApplicants }) {
  return <div className="content-wrap"><div className="welcome-row recruiter-welcome"><div><span className="eyebrow">MAKE ROOM FOR WHAT’S NEXT</span><h1>Your <span>opportunities.</span></h1><p>Thoughtful first roles can change everything. Here’s what you’re building.</p></div><button className="button button-dark" onClick={onCreate}><Plus size={17} /> Post an opportunity</button></div>
    {jobs.length ? <div className="recruiter-job-list">{jobs.map((job) => <article className="recruiter-job-card" key={job.id}><div className="recruiter-job-top"><div className={`company-mark mark-${job.color || 'lilac'}`}>{(job.department || 'N')[0]}</div><ApplicationStatus status={job.status} /></div><span className="job-dept">{job.department}</span><h3>{job.title}</h3><p>{job.location || 'South Africa · Hybrid'} <span>·</span> {job.type || 'Graduate opportunity'}</p><div className="recruiter-job-foot"><button className="text-link" onClick={onApplicants}><Users size={15} /> View applicants <ArrowRight size={15} /></button>{job.status === 'OPEN' && <button className="close-job-button" onClick={() => onClose(job)}>Close role</button>}</div></article>)}</div> : <EmptyState icon={BriefcaseBusiness} title="A great hire starts with an open door" text="Post your first opportunity and let new talent find you." action="Post an opportunity" onAction={onCreate} />}
  </div>
}

function RecruiterApplicants({ applications, jobs, onUpdate }) {
  const [filter, setFilter] = useState('All applicants')
  const [roleFilter, setRoleFilter] = useState('All opportunities')
  const [query, setQuery] = useState('')
  const rows = applications.filter((app) => {
    const role = jobs.find((job) => Number(job.id) === Number(app.jobId))
    const matchesStatus = filter === 'All applicants' || app.status === filter.toUpperCase()
    const matchesRole = roleFilter === 'All opportunities' || String(app.jobId) === roleFilter
    const matchesText = !query || `${app.candidateEmail || ''} ${role?.title || ''}`.toLowerCase().includes(query.toLowerCase())
    return matchesStatus && matchesRole && matchesText
  })
  return <div className="content-wrap"><div className="welcome-row recruiter-welcome"><div><span className="eyebrow">FRESH PERSPECTIVES AHEAD</span><h1>People with <span>potential.</span></h1><p>Every name here is someone ready to make a difference.</p></div></div>
    <div className="applicant-filters"><label className="search-box"><Search size={16} /><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search candidate or role" aria-label="Search applicants" /></label><label className="filter-box"><Filter size={16} /><select value={filter} onChange={(event) => setFilter(event.target.value)} aria-label="Filter by application status"><option>All applicants</option>{['APPLIED', 'SCREENING', 'INTERVIEW', 'OFFER', 'HIRED', 'REJECTED'].map((status) => <option key={status} value={status}>{status[0] + status.slice(1).toLowerCase()}</option>)}</select><ChevronDown size={15} /></label><label className="filter-box"><select value={roleFilter} onChange={(event) => setRoleFilter(event.target.value)} aria-label="Filter by opportunity"><option>All opportunities</option>{jobs.map((job) => <option key={job.id} value={String(job.id)}>{job.title}</option>)}</select><ChevronDown size={15} /></label></div>
    {rows.length ? <div className="applicant-table-wrap"><table className="applicant-table"><thead><tr><th>Candidate</th><th>Opportunity</th><th>Applied</th><th>Progress</th><th>Update</th></tr></thead><tbody>{rows.map((app) => <tr key={app.id}><td><div className="table-person"><span className="avatar avatar-blue">{initials(app.candidateEmail)}</span><span><strong>{app.candidateEmail?.split('@')[0] || 'New candidate'}</strong><small>{app.candidateEmail}</small></span></div></td><td>{jobs.find((job) => Number(job.id) === Number(app.jobId))?.title || `Opportunity #${app.jobId}`}</td><td>{app.appliedAt ? new Date(app.appliedAt).toLocaleDateString(undefined, { month: 'short', day: 'numeric' }) : 'Recently'}</td><td><ApplicationStatus status={app.status} /></td><td><select className="status-select" value={app.status} onChange={(event) => onUpdate(app, event.target.value)} aria-label={`Update status for ${app.candidateEmail}`}><option value="APPLIED">Applied</option><option value="SCREENING">Screening</option><option value="INTERVIEW">Interview</option><option value="OFFER">Offer</option><option value="HIRED">Hired</option><option value="REJECTED">Not moving forward</option></select></td></tr>)}</tbody></table></div> : <EmptyState icon={Users} title="Your next great teammate is out there" text="Once grads apply to your opportunities, you’ll meet them here." />}
  </div>
}

const SECURITY_QUESTIONS = [
  'Why was the latest alert created?',
  'Which users are linked to high-severity alerts?',
  'What controls mitigate the latest alert?',
  'What is the dependency impact if application-service fails?',
  'Which threats are linked to auth-service?',
]

function SecurityQuery({ token }) {
  const [question, setQuestion] = useState('')
  const [history, setHistory] = useState([])

  async function ask(value = question) {
    const cleanQuestion = value.trim()
    if (!cleanQuestion || history.some((item) => item.loading)) return

    const id = `${Date.now()}-${Math.random()}`
    setQuestion('')
    setHistory((current) => [...current, { id, question: cleanQuestion, loading: true }])
    try {
      const response = await fetch('/soc-api/rag/ask', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
        body: JSON.stringify({ question: cleanQuestion }),
      })
      const raw = await response.text()
      let result
      try { result = raw ? JSON.parse(raw) : null } catch { result = null }
      if (!response.ok) {
        const message = typeof result === 'string' ? result : result?.message || raw || 'The SOC service rejected this question.'
        throw new Error(message)
      }
      if (!result?.answer) throw new Error('The SOC service returned an empty answer. Check that the SOC service is running.')
      setHistory((current) => current.map((item) => item.id === id ? { ...item, loading: false, result } : item))
    } catch (error) {
      const message = error.message?.includes('Failed to fetch')
        ? 'Could not reach the SOC service. Start the backend stack and try again.'
        : error.message || 'The question could not be answered.'
      setHistory((current) => current.map((item) => item.id === id ? { ...item, loading: false, error: message } : item))
    }
  }

  function handleSubmit(event) {
    event.preventDefault()
    ask()
  }

  return <div className="security-page">
    <div className="security-heading"><div><span className="eyebrow">SECURITY OPERATIONS CENTRE</span><h1>Ask your <span>security graph.</span></h1><p>Explore alerts, threats, controls, users, and service dependencies in plain language.</p></div><div className="soc-live-badge"><span /> GRAPH QUERY</div></div>
    <div className="security-layout">
      <aside className="security-prompts"><div className="prompt-title"><span className="prompt-spark"><Sparkles size={15} /></span><div><strong>Try a question</strong><small>These prompts match the current graph query patterns.</small></div></div>{SECURITY_QUESTIONS.map((item) => <button className="prompt-chip" key={item} onClick={() => ask(item)} disabled={history.some((entry) => entry.loading)}>{item}<ArrowUpRight size={14} /></button>)}<div className="security-note"><ShieldCheck size={16} /><p>Answers are generated from retrieved Neo4j evidence using the project’s explainable, template-based RAG flow.</p></div></aside>
      <section className="security-chat" aria-label="Security question and answer interface">
        {!history.length && <div className="security-empty"><span className="security-empty-icon"><ShieldCheck size={22} /></span><h2>What would you like to understand?</h2><p>Ask about a recent alert, the users or services involved, or which controls and procedures can help.</p></div>}
        <div className="security-history" aria-live="polite">{history.map((item) => <article className="security-turn" key={item.id}><div className="security-question"><span className="question-avatar">YOU</span><p>{item.question}</p></div>{item.loading ? <div className="security-loading"><span className="loading-dots"><i /><i /><i /></span>Looking through the graph…</div> : item.error ? <div className="security-error"><CircleHelp size={16} /><span>{item.error}</span></div> : <div className="security-answer"><div className="answer-brand"><span className="answer-avatar"><ShieldCheck size={16} /></span><div><strong>Graph answer</strong><small>Grounded in retrieved security data</small></div></div><p className="answer-copy">{item.result.answer}</p><div className="retrieval-method"><span className="method-label"><Search size={14} /> RETRIEVAL METHOD</span><p>{item.result.method}</p></div><details className="evidence-details"><summary><span><Database size={14} /> Retrieved evidence</span><span className="evidence-count">{item.result.evidence?.length || 0} {item.result.evidence?.length === 1 ? 'result' : 'results'}</span></summary>{item.result.evidence?.length ? <div className="evidence-list">{item.result.evidence.map((row, index) => <div className="evidence-row" key={`${item.id}-${index}`}>{Object.entries(row).map(([key, value]) => <div className="evidence-field" key={key}><span>{key}</span><strong>{Array.isArray(value) ? (value.length ? value.map((entry) => typeof entry === 'object' && entry !== null ? JSON.stringify(entry) : entry).join(', ') : 'None') : typeof value === 'object' && value !== null ? JSON.stringify(value) : value ?? '—'}</strong></div>)}</div>)}</div> : <p className="no-evidence">No graph records matched this question. Try another suggested prompt.</p>}</details></div>}</article>)}</div>
        <form className="security-composer" onSubmit={handleSubmit}><label htmlFor="security-question">Ask a security question</label><div className="composer-field"><textarea id="security-question" value={question} onChange={(event) => setQuestion(event.target.value)} onKeyDown={(event) => { if (event.key === 'Enter' && (event.ctrlKey || event.metaKey)) { event.preventDefault(); ask() } }} placeholder="e.g. Why was the latest alert created?" rows="2" /><button className="send-question" type="submit" disabled={!question.trim() || history.some((item) => item.loading)} aria-label="Ask question"><Send size={17} /></button></div><span className="composer-hint">Press Ctrl + Enter to ask · Responses include their retrieval method and evidence</span></form>
      </section>
    </div>
  </div>
}

function ApplicationStatus({ status = 'OPEN' }) {
  const normalized = (status || 'OPEN').toUpperCase()
  const labels = { APPLIED: 'Applied', SCREENING: 'In review', INTERVIEW: 'Interview', OFFER: 'Offer', HIRED: 'Hired', REJECTED: 'Closed', OPEN: 'Open', CLOSED: 'Closed', FILLED: 'Filled' }
  return <span className={`status-pill status-${normalized.toLowerCase()}`}><i />{labels[normalized] || normalized}</span>
}

function ApplicationProgress({ status = 'APPLIED' }) {
  const stages = ['APPLIED', 'SCREENING', 'INTERVIEW', 'OFFER', 'HIRED']
  const current = stages.indexOf((status || 'APPLIED').toUpperCase())
  if ((status || '').toUpperCase() === 'REJECTED') return <div className="progress-note rejected-note">This application is no longer moving forward. You can keep exploring other opportunities.</div>
  return <div className="application-progress" aria-label={`Application progress: ${statusLabel(status)}`}>
    {stages.map((stage, index) => <div key={stage} className={`progress-stage ${index <= Math.max(current, 0) ? 'complete' : ''} ${index === current ? 'current' : ''}`}><span className="progress-dot">{index < current ? <Check size={10} /> : index + 1}</span><span>{statusLabel(stage)}</span></div>)}
  </div>
}

function statusLabel(status) {
  return ({ APPLIED: 'Applied', SCREENING: 'Screening', INTERVIEW: 'Interview', OFFER: 'Offer', HIRED: 'Hired', REJECTED: 'Not selected' })[(status || '').toUpperCase()] || status
}

function MetricCard({ label, value, note, icon: Icon, tint }) {
  return <article className="metric-card"><span className={`metric-icon metric-${tint}`}><Icon size={18} /></span><span className="metric-label">{label}</span><strong>{value}</strong><span className="metric-note">{note}</span></article>
}

function EmptyState({ icon: Icon, title, text, action, onAction }) {
  return <div className="empty-state"><span className="empty-icon"><Icon size={21} /></span><h3>{title}</h3><p>{text}</p>{action && <button className="button button-dark" onClick={onAction}>{action} <ArrowRight size={15} /></button>}</div>
}

function EmptyInline({ text, onClick }) { return <div className="empty-inline"><p>{text}</p>{onClick && <button className="text-link" onClick={onClick}>Let’s go <ArrowRight size={14} /></button>}</div> }

function AuthModal({ mode, role, setMode, setRole, onSubmit, onClose, onDemo, loading }) {
  const [visible, setVisible] = useState(false)
  const adminSetup = mode === 'admin-setup'
  return <div className="modal-backdrop" onMouseDown={(event) => event.target === event.currentTarget && onClose()}><section className="auth-modal modal-card" role="dialog" aria-modal="true" aria-labelledby="auth-title"><button className="modal-close" onClick={onClose} aria-label="Close"><X size={18} /></button><div className="modal-brand"><span className="brand-mark"><span /></span><span>nextstep<span className="brand-period">.</span></span></div><span className="eyebrow">{adminSetup ? 'ONE-TIME ADMINISTRATOR SETUP' : mode === 'signup' ? 'A NEW CHAPTER STARTS HERE' : 'GOOD TO SEE YOU AGAIN'}</span><h2 id="auth-title">{adminSetup ? <>Set up the <span>admin console.</span></> : mode === 'signup' ? <>Start something <span>good.</span></> : <>Welcome <span>back.</span></>}</h2><p className="modal-intro">{adminSetup ? 'Create the first administrator using the private setup key configured by your system owner.' : mode === 'signup' ? 'Make a little space for what comes next.' : 'Pick up where your next step begins.'}</p>
      {!adminSetup && <div className="role-switch" aria-label="Account type"><button type="button" className={role === 'CANDIDATE' ? 'selected' : ''} onClick={() => setRole('CANDIDATE')}><GraduationCap size={16} /> I’m a candidate</button><button type="button" className={role === 'RECRUITER' ? 'selected' : ''} onClick={() => setRole('RECRUITER')}><Users size={16} /> I’m hiring</button></div>}
      <form className="auth-form" onSubmit={onSubmit}>{(mode === 'signup' || adminSetup) && <label>Your name<input name="name" autoComplete="name" placeholder="What should we call you?" required /></label>}{adminSetup && <label>Private setup key<input name="setupKey" type="password" autoComplete="off" placeholder="Enter the key from your system owner" required /></label>}<label>Email address<input name="email" type="email" autoComplete="email" placeholder="you@example.com" required /></label><label>Password<span className="password-wrap"><input name="password" type={visible ? 'text' : 'password'} autoComplete={mode === 'signup' || adminSetup ? 'new-password' : 'current-password'} placeholder="At least 8 characters" minLength={8} required /><button type="button" onClick={() => setVisible((value) => !value)}>{visible ? 'Hide' : 'Show'}</button></span></label><button className="button button-dark auth-submit" disabled={loading}>{loading ? 'One moment…' : adminSetup ? 'Create administrator' : mode === 'signup' ? 'Create your account' : 'Log in'} <ArrowRight size={16} /></button></form>
      {!adminSetup && <><div className="auth-divider"><span />or take a look around<span /></div><button className="demo-button" onClick={() => onDemo(role)}>Explore as a {roleLabel(role).toLowerCase()} <ArrowUpRight size={15} /></button></>}
      <p className="auth-switch">{adminSetup ? 'Already have an administrator account?' : mode === 'signup' ? 'Already have an account?' : 'New to Nextstep?'} <button onClick={() => setMode(adminSetup || mode === 'signup' ? 'login' : 'signup')}>{adminSetup || mode === 'signup' ? 'Log in' : 'Create an account'}</button></p>
      {!adminSetup && <button className="admin-setup-link" onClick={() => setMode('admin-setup')}>Set up the first administrator</button>}
    </section></div>
}

function JobModal({ onClose, onSubmit }) {
  return <div className="modal-backdrop" onMouseDown={(event) => event.target === event.currentTarget && onClose()}><section className="job-modal modal-card" role="dialog" aria-modal="true" aria-labelledby="job-modal-title"><button className="modal-close" onClick={onClose} aria-label="Close"><X size={18} /></button><span className="eyebrow">MAKE ROOM FOR NEW TALENT</span><h2 id="job-modal-title">Post an <span>opportunity.</span></h2><p className="modal-intro">Tell new grads why they’ll want to grow with your team.</p><form className="auth-form" onSubmit={onSubmit}><label>Role title<input name="title" placeholder="e.g. Graduate Software Engineer" required /></label><label>Department<select name="department" required defaultValue=""><option value="" disabled>Choose a team</option>{['Engineering', 'Design', 'Marketing', 'Data & Analytics', 'Finance', 'People & Culture', 'Other'].map((name) => <option key={name}>{name}</option>)}</select></label><label>What will they work on?<textarea name="description" rows="3" placeholder="Share a little about the role and your team…" /></label><label>Skills to grow with<input name="requiredSkills" placeholder="e.g. curiosity, SQL, collaboration" required /></label><label>Location<input name="location" placeholder="e.g. Cape Town · Hybrid" /></label><button className="button button-dark auth-submit">Publish opportunity <ArrowRight size={16} /></button></form></section></div>
}

export default App
