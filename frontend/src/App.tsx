import { useCallback, useEffect, useState } from 'react'
import { api, setToken, type Group, type Lesson, type Page, type Session, type Student } from './api'

const DAYS = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri']
const FIRST = 8, LAST = 18
const hue = (s: string) => [...s].reduce((a, c) => (a * 31 + c.charCodeAt(0)) % 360, 7)

function Login({ onDone }: { onDone: (s: Session) => void }) {
  const [username, setU] = useState(''), [password, setP] = useState(''), [err, setErr] = useState('')
  const go = async () => {
    try { onDone(await api<Session>('/auth/login', 'POST', { username, password })) } catch (e) { setErr((e as Error).message) }
  }
  return (
    <main className="login">
      <h1>GroupBoard</h1>
      <p>Sign in to manage study groups and their weekly schedule.</p>
      <label>Username<input value={username} onChange={e => setU(e.target.value)} autoFocus /></label>
      <label>Password<input type="password" value={password} onChange={e => setP(e.target.value)} onKeyDown={e => e.key === 'Enter' && go()} /></label>
      {err && <p className="err" role="alert">{err}</p>}
      <button className="primary" onClick={go}>Sign in</button>
    </main>
  )
}

function Timetable({ lessons, onDelete }: { lessons: Lesson[]; onDelete: (l: Lesson) => void }) {
  const hours = Array.from({ length: LAST - FIRST }, (_, i) => FIRST + i)
  return (
    <div className="tt" role="table" aria-label="Weekly schedule">
      <div className="tt-corner" />
      {DAYS.map(d => <div key={d} className="tt-day" role="columnheader">{d}</div>)}
      <div className="tt-hours">{hours.map(h => <span key={h}>{h}:00</span>)}</div>
      {DAYS.map((d, i) => (
        <div key={d} className="tt-col">
          {lessons.filter(l => l.weekday === i + 1).map(l => (
            <button key={l.id} className="lesson" title="Remove lesson" onClick={() => onDelete(l)}
              style={{ top: `calc(${l.startHour - FIRST} * var(--row))`, height: `calc(${l.endHour - l.startHour} * var(--row) - 4px)`, ['--h' as string]: hue(l.subject) }}>
              <b>{l.subject}</b><span>{l.startHour}:00–{l.endHour}:00{l.room ? ` · ${l.room}` : ''}</span>
            </button>
          ))}
        </div>
      ))}
    </div>
  )
}

export default function App() {
  const [me, setMe] = useState<Session | null>(null)
  const [dark, setDark] = useState(matchMedia('(prefers-color-scheme: dark)').matches)
  const [q, setQ] = useState(''), [page, setPage] = useState(0)
  const [data, setData] = useState<Page<Group>>({ items: [], page: 0, pages: 0, total: 0 })
  const [sel, setSel] = useState<Group | null>(null)
  const [students, setStudents] = useState<Student[]>([])
  const [lessons, setLessons] = useState<Lesson[]>([])
  const [msg, setMsg] = useState('')
  const [ng, setNg] = useState({ name: '', course: '' })
  const [ns, setNs] = useState({ fullName: '', email: '' })
  const [nl, setNl] = useState({ subject: '', room: '', weekday: 1, startHour: 9, endHour: 10 })
  const admin = me?.role === 'ADMIN'

  useEffect(() => { document.documentElement.dataset.theme = dark ? 'dark' : 'light' }, [dark])
  const run = async (f: () => Promise<unknown>) => { setMsg(''); try { await f() } catch (e) { setMsg((e as Error).message) } }

  const loadGroups = useCallback(() => run(async () => setData(await api<Page<Group>>(`/groups?q=${encodeURIComponent(q)}&page=${page}`))), [q, page])
  const loadGroup = useCallback((g: Group) => run(async () => {
    setStudents(await api<Student[]>(`/groups/${g.id}/students`)); setLessons(await api<Lesson[]>(`/groups/${g.id}/lessons`))
  }), [])
  useEffect(() => { if (me) loadGroups() }, [me, loadGroups])
  useEffect(() => { if (sel) loadGroup(sel) }, [sel, loadGroup])

  if (!me) return <Login onDone={s => { setToken(s.token); setMe(s) }} />

  return (
    <div className="app">
      <header>
        <h1>GroupBoard</h1>
        <span className="who">{me.name} · {me.role.toLowerCase()}</span>
        <button onClick={() => setDark(!dark)} aria-label="Toggle theme">{dark ? 'Light' : 'Dark'} theme</button>
        <button onClick={() => { setToken(''); setMe(null); setSel(null) }}>Sign out</button>
      </header>
      {msg && <p className="err" role="alert">{msg}</p>}
      <div className="layout">
        <aside>
          <input placeholder="Search groups" value={q} onChange={e => { setQ(e.target.value); setPage(0) }} />
          <ul className="groups">
            {data.items.map(g => (
              <li key={g.id}><button className={sel?.id === g.id ? 'on' : ''} onClick={() => setSel(g)}>
                <b>{g.name}</b><span>{g.course} · {g.students} students</span></button></li>
            ))}
            {data.items.length === 0 && <li className="empty">No groups found{admin ? '. Add one below.' : '.'}</li>}
          </ul>
          <div className="pager">
            <button disabled={page <= 0} onClick={() => setPage(page - 1)}>Previous</button>
            <span>{data.pages ? page + 1 : 0} of {data.pages}</span>
            <button disabled={page + 1 >= data.pages} onClick={() => setPage(page + 1)}>Next</button>
          </div>
          {admin && <form onSubmit={e => { e.preventDefault(); run(async () => { await api('/groups', 'POST', ng); setNg({ name: '', course: '' }); loadGroups() }) }}>
            <input placeholder="Group name" value={ng.name} onChange={e => setNg({ ...ng, name: e.target.value })} />
            <input placeholder="Course" value={ng.course} onChange={e => setNg({ ...ng, course: e.target.value })} />
            <button className="primary">Add group</button>
          </form>}
        </aside>
        <section>
          {!sel ? <p className="empty">Pick a group to see its week.</p> : <>
            <div className="title"><h2>{sel.name}</h2>
              {admin && <button onClick={() => run(async () => { await api(`/groups/${sel.id}`, 'DELETE'); setSel(null); loadGroups() })}>Delete group</button>}</div>
            <Timetable lessons={lessons} onDelete={l => run(async () => { await api(`/lessons/${l.id}`, 'DELETE'); loadGroup(sel) })} />
            <form className="row" onSubmit={e => { e.preventDefault(); run(async () => { await api('/lessons', 'POST', { ...nl, groupId: sel.id }); loadGroup(sel) }) }}>
              <input placeholder="Subject" value={nl.subject} onChange={e => setNl({ ...nl, subject: e.target.value })} />
              <input placeholder="Room" value={nl.room} onChange={e => setNl({ ...nl, room: e.target.value })} />
              <select value={nl.weekday} onChange={e => setNl({ ...nl, weekday: +e.target.value })}>{DAYS.map((d, i) => <option key={d} value={i + 1}>{d}</option>)}</select>
              <input type="number" min={7} max={20} value={nl.startHour} onChange={e => setNl({ ...nl, startHour: +e.target.value })} aria-label="Start hour" />
              <input type="number" min={8} max={21} value={nl.endHour} onChange={e => setNl({ ...nl, endHour: +e.target.value })} aria-label="End hour" />
              <button className="primary">Add lesson</button>
            </form>
            <h3>Students</h3>
            <ul className="students">
              {students.map(s => <li key={s.id}><span>{s.fullName}</span><small>{s.email}</small>
                {admin && <button onClick={() => run(async () => { await api(`/students/${s.id}`, 'DELETE'); loadGroup(sel); loadGroups() })}>Remove</button>}</li>)}
              {students.length === 0 && <li className="empty">No students in this group yet.</li>}
            </ul>
            {admin && <form className="row" onSubmit={e => { e.preventDefault(); run(async () => { await api('/students', 'POST', { ...ns, groupId: sel.id }); setNs({ fullName: '', email: '' }); loadGroup(sel); loadGroups() }) }}>
              <input placeholder="Full name" value={ns.fullName} onChange={e => setNs({ ...ns, fullName: e.target.value })} />
              <input placeholder="Email" value={ns.email} onChange={e => setNs({ ...ns, email: e.target.value })} />
              <button className="primary">Add student</button>
            </form>}
          </>}
        </section>
      </div>
    </div>
  )
}
