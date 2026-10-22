import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api, hrApi } from '../../api/client';
import './Dashboard.css';

function localParts(now, timezone) {
  const parts = Object.fromEntries(new Intl.DateTimeFormat('en-US', {timeZone: timezone,
    year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit',
    second: '2-digit', hourCycle: 'h23'}).formatToParts(now).filter(p => p.type !== 'literal').map(p => [p.type, p.value]));
  return {date: `${parts.year}-${parts.month}-${parts.day}`, seconds: Number(parts.hour) * 3600 + Number(parts.minute) * 60 + Number(parts.second), hour: Number(parts.hour)};
}
function duration(seconds) {
  const value = Math.max(0, Math.floor(seconds));
  return `${String(Math.floor(value / 3600)).padStart(2, '0')}:${String(Math.floor(value % 3600 / 60)).padStart(2, '0')}:${String(value % 60).padStart(2, '0')}`;
}
function status(now, settings, holidays) {
  if (!settings) return 'Workday settings unavailable';
  const current = localParts(now, settings.timezone);
  const day = new Date(`${current.date}T12:00:00Z`).toLocaleDateString('en-US', {weekday: 'long', timeZone: 'UTC'}).toUpperCase();
  if (settings.weeklyOffDays.includes(day)) return 'Weekly off';
  if (holidays.some(h => h.date === current.date)) return 'Holiday';
  const seconds = time => {const [hour, minute] = time.split(':').map(Number); return hour * 3600 + minute * 60;};
  const start = seconds(settings.workStart), end = seconds(settings.workEnd);
  if (current.seconds < start) return `Work begins in ${duration(start - current.seconds)}`;
  if (current.seconds < end) return `${duration(end - current.seconds)} remaining`;
  return 'Workday complete';
}

export default function Dashboard({ user, onLogout }) {
  const [dark, setDark] = useState(false);
  const [now, setNow] = useState(new Date());
  const [data, setData] = useState({health: null, employee: null, settings: null, holidays: [], birthdays: [], breaks: null, count: null});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [birthdayIndex, setBirthdayIndex] = useState(0);
  const [breakBusy, setBreakBusy] = useState(false);
  const canViewEmployees = user.permissions.includes('EMPLOYEE_VIEW');
  useEffect(() => {const timer = setInterval(() => setNow(new Date()), 1000); return () => clearInterval(timer);}, []);
  useEffect(() => {document.documentElement.style.colorScheme = dark ? 'dark' : 'light';}, [dark]);
  useEffect(() => {
    let active = true;
    Promise.allSettled([api.health(), hrApi.ownEmployee(), hrApi.settings(), hrApi.holidays(), hrApi.birthdays(), hrApi.breakToday(), canViewEmployees ? hrApi.employeeCount() : Promise.resolve(null)])
      .then(results => {
        if (!active) return;
        const get = i => results[i].status === 'fulfilled' ? results[i].value : null;
        setData({health: get(0), employee: get(1), settings: get(2), holidays: get(3) || [], birthdays: get(4) || [], breaks: get(5), count: get(6)});
        if (results[0].status === 'rejected') setError('Backend status is unavailable.');
        setLoading(false);
      });
    return () => {active = false;};
  }, [canViewEmployees]);
  const action = async operation => {
    setBreakBusy(true); setError('');
    try {const breaks = operation === 'start' ? await hrApi.startBreak() : await hrApi.endBreak(); setData(previous => ({...previous, breaks}));}
    catch {setError('Break action could not be completed. Refresh and try again.');}
    finally {setBreakBusy(false);}
  };
  const timezone = data.settings?.timezone || Intl.DateTimeFormat().resolvedOptions().timeZone;
  const parts = localParts(now, timezone);
  const greeting = parts.hour < 12 ? 'Good morning' : parts.hour < 17 ? 'Good afternoon' : 'Good evening';
  const monday = new Date(`${parts.date}T12:00:00Z`);
  monday.setUTCDate(monday.getUTCDate() - ((monday.getUTCDay() + 6) % 7));
  const week = Array.from({length: 7}, (_, i) => {
    const date = new Date(monday); date.setUTCDate(date.getUTCDate() + i);
    const key = date.toISOString().slice(0, 10);
    const name = date.toLocaleDateString('en-US', {weekday: 'long', timeZone: 'UTC'});
    return {key, name, state: data.holidays.some(h => h.date === key) ? 'Holiday' : data.settings?.weeklyOffDays.includes(name.toUpperCase()) ? 'Weekly off' : 'Unfinalized'};
  });
  const elapsed = data.breaks ? data.breaks.consumedSeconds + (data.breaks.activeSince ? Math.max(0, (now.getTime() - new Date(data.breaks.activeSince).getTime()) / 1000) : 0) : 0;
  const birthday = data.birthdays[birthdayIndex];
  return <div className="app-shell" data-theme={dark ? 'dark' : 'light'}>
    <aside className="sidebar"><div className="brand"><div className="brand-mark">◉</div><div><div className="brand-name">Aeliya</div><div className="brand-product">ORBIT</div></div></div><div className="nav-label">WORKSPACE</div><nav className="navigation"><Link className="nav-item active" to="/dashboard">Dashboard</Link>{canViewEmployees && <Link className="nav-item" to="/employees">Employees</Link>}</nav></aside>
    <main className="main-content"><header className="topbar"><div className="heading-group"><div className="eyebrow">CONTROL CENTER</div><h1>Overview</h1></div><div className="header-actions"><div className="mode-switch"><span>☀</span><button className={`switch ${dark ? 'on' : ''}`} type="button" aria-label="Toggle theme" onClick={() => setDark(v => !v)}><span /></button><span>☾</span></div><div className="profile"><div className="avatar">{(data.employee?.fullName || user.username).slice(0, 2).toUpperCase()}</div><div className="profile-copy"><strong>{data.employee?.fullName || user.username}</strong><span>{data.employee?.designation || user.role.replace('ROLE_', '')}</span></div></div><button className="logout-button" onClick={async () => {try {await onLogout();} catch {setError('Logout failed. Please try again.');}}}>Logout</button></div></header>
      <div className="dashboard"><section className="welcome-row"><div><h2>{greeting}{data.employee ? `, ${data.employee.fullName}` : ''}</h2><p>{new Intl.DateTimeFormat('en-IN', {timeZone: timezone, dateStyle: 'full'}).format(now)}</p></div><div className="date-badge"><span className="live-dot" />{data.health?.status === 'UP' ? 'Backend connected' : 'Backend unavailable'}</div></section>
        {error && <p role="alert" className="dashboard-error">{error}</p>}{loading && <p role="status">Loading dashboard…</p>}
        <section className="status-card"><div className="status-icon">{data.health?.status === 'UP' ? '✓' : '!'}</div><div className="status-copy"><span className="status-kicker">TODAY</span><h3>{status(now, data.settings, data.holidays)}</h3><p>Attendance status, check-in, check-out, and work duration are unavailable until attendance records exist.</p></div></section>
        <section className="stats-grid" aria-label="Dashboard summary"><article className="stat-card"><div className="stat-topline">My profile</div><strong className="stat-value">{data.employee?.employeeCode || 'Not linked'}</strong><p>{data.employee?.department || 'No department assigned'}</p>{data.employee && <img className="dashboard-photo" src={data.employee.pictureUrl} alt="Profile" onError={event => {event.currentTarget.style.display = 'none';}} />}</article><article className="stat-card"><div className="stat-topline">Active employees</div><strong className="stat-value">{data.count ? data.count.activeEmployees : '—'}</strong><p>{canViewEmployees ? 'From employee records' : 'Not available to your account'}</p></article><article className="stat-card"><div className="stat-topline">Break remaining</div><strong className="stat-value">{data.breaks ? duration(data.breaks.allowanceMinutes * 60 - elapsed) : '—'}</strong><p>{data.breaks ? `${duration(elapsed)} consumed today` : 'Link an employee profile to use breaks'}</p>{data.breaks && <button disabled={breakBusy} onClick={() => action(data.breaks.activeSince ? 'end' : 'start')}>{data.breaks.activeSince ? 'End Break' : 'Start Break'}</button>}</article><article className="stat-card"><div className="stat-topline">Upcoming holidays</div><strong className="stat-value">{data.holidays.length}</strong><p>{data.holidays[0] ? `${data.holidays[0].name} · ${data.holidays[0].date}` : 'No configured holidays ahead'}</p></article></section>
        <div className="dashboard-panels"><section className="activity-card"><h3>Weekly attendance</h3><p>Monday–Sunday · attendance records not yet available</p><div className="week-grid">{week.map(day => <div className="week-day" key={day.key}><strong>{day.name.slice(0, 3)}</strong><span>{day.key.slice(8)}</span><small>{day.state}</small></div>)}</div></section><section className="activity-card"><h3>Today’s absentees</h3><p>Unavailable until finalized attendance records exist.</p></section><section className="activity-card"><h3>Upcoming approved leaves</h3><p>Unavailable until leave approvals are recorded.</p></section><section className="activity-card"><h3>Birthdays</h3>{birthday ? <><p>{birthday.employeeName} · {birthday.today ? 'Today' : birthday.nextOccurrence.slice(5)}</p><div className="carousel-actions"><button disabled={birthdayIndex === 0} onClick={() => setBirthdayIndex(i => i - 1)}>Previous</button><span>{birthdayIndex + 1} / {data.birthdays.length}</span><button disabled={birthdayIndex + 1 >= data.birthdays.length} onClick={() => setBirthdayIndex(i => i + 1)}>Next</button></div></> : <p>No upcoming birthdays in the next month.</p>}</section><section className="activity-card"><h3>Leave balance</h3><p>Unavailable until leave balances are recorded.</p></section><section className="activity-card"><h3>Recent activity</h3><p>Unavailable until an authorized activity source is connected.</p></section></div>
      </div></main></div>;
}
