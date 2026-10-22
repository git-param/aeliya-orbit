import { useEffect, useState } from 'react';
import { api, hrApi } from '../../api/client';
import { Icon, initials } from '../../components/UI.jsx';
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
const shortDate = iso => new Date(`${iso}T12:00:00Z`).toLocaleDateString('en-IN', {day: 'numeric', month: 'short', timeZone: 'UTC'});

// Returns what the hourglass needs: kind, big text, caption and 0..1 progress.
function workday(now, settings, holidays) {
  if (!settings) return {kind: 'idle', time: '--:--:--', caption: 'Workday settings unavailable', progress: 0};
  const current = localParts(now, settings.timezone);
  const day = new Date(`${current.date}T12:00:00Z`).toLocaleDateString('en-US', {weekday: 'long', timeZone: 'UTC'}).toUpperCase();
  if (settings.weeklyOffDays.includes(day)) return {kind: 'idle', time: 'Day off', caption: 'Weekly off', progress: 0};
  const holiday = holidays.find(h => h.date === current.date);
  if (holiday) return {kind: 'idle', time: 'Holiday', caption: holiday.name || 'Enjoy the day', progress: 0};
  const seconds = t => {const [h, m] = t.split(':').map(Number); return h * 3600 + m * 60;};
  const start = seconds(settings.workStart), end = seconds(settings.workEnd);
  if (current.seconds < start) return {kind: 'before', time: duration(start - current.seconds), caption: 'until work begins', progress: 0};
  if (current.seconds < end) return {kind: 'running', time: duration(end - current.seconds), caption: 'left in your workday', progress: (current.seconds - start) / (end - start)};
  return {kind: 'done', time: 'Done', caption: 'Workday complete', progress: 1};
}

function Hourglass({ progress, running }) {
  const p = Math.min(1, Math.max(0, progress));
  return <svg className="hourglass" viewBox="0 0 120 180" role="img" aria-label={`${Math.round(p * 100)}% of the workday has passed`}>
    <defs><clipPath id="glass"><path d="M24 12H96C96 55 70 72 64 90C70 108 96 125 96 168H24C24 125 50 108 56 90C50 72 24 55 24 12Z" /></clipPath></defs>
    <g clipPath="url(#glass)">
      <rect className="sand" x="0" y="12" width="120" height="78" style={{transformOrigin: '60px 90px', transform: `scaleY(${1 - p})`}} />
      <rect className="sand" x="0" y="90" width="120" height="78" style={{transformOrigin: '60px 168px', transform: `scaleY(${p})`}} />
      {running && <line className="sand-stream" x1="60" y1="88" x2="60" y2="168" />}
    </g>
    <path className="glass-outline" d="M24 12H96C96 55 70 72 64 90C70 108 96 125 96 168H24C24 125 50 108 56 90C50 72 24 55 24 12Z" />
    <rect className="glass-cap" x="16" y="4" width="88" height="8" rx="4" />
    <rect className="glass-cap" x="16" y="168" width="88" height="8" rx="4" />
  </svg>;
}

const Empty = ({ icon, text }) => <div className="empty"><Icon name={icon} size={22} /><span>{text}</span></div>;

export default function Dashboard({ user }) {
  const [now, setNow] = useState(new Date());
  const [data, setData] = useState({health: null, employee: null, settings: null, holidays: [], birthdays: [], breaks: null, count: null});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [breakBusy, setBreakBusy] = useState(false);
  const canViewEmployees = user.permissions.includes('EMPLOYEE_VIEW');
  useEffect(() => {const timer = setInterval(() => setNow(new Date()), 1000); return () => clearInterval(timer);}, []);
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
  // To show Present / Absent, set `state` to 'Present' or 'Absent' here once an attendance API exists.
  const week = Array.from({length: 7}, (_, i) => {
    const date = new Date(monday); date.setUTCDate(date.getUTCDate() + i);
    const key = date.toISOString().slice(0, 10);
    const name = date.toLocaleDateString('en-US', {weekday: 'long', timeZone: 'UTC'});
    const state = data.holidays.some(h => h.date === key) ? 'Holiday' : data.settings?.weeklyOffDays.includes(name.toUpperCase()) ? 'Weekly off' : 'Pending';
    return {key, name, state, today: key === parts.date};
  });
  const elapsed = data.breaks ? data.breaks.consumedSeconds + (data.breaks.activeSince ? Math.max(0, (now.getTime() - new Date(data.breaks.activeSince).getTime()) / 1000) : 0) : 0;
  const allowance = data.breaks ? data.breaks.allowanceMinutes * 60 : 0;
  const breakPct = allowance ? Math.min(100, elapsed / allowance * 100) : 0;
  const day = workday(now, data.settings, data.holidays);
  const nextHoliday = data.holidays[0];
  const online = data.health?.status === 'UP';

  return <>
    <div className="welcome-row">
      <h2>{greeting}{data.employee ? `, ${data.employee.fullName.split(' ')[0]}` : ''}</h2>
      <span className={`pill ${online ? 'ok' : 'bad'}`}><span className="live-dot" />{online ? 'Backend connected' : 'Backend unavailable'}</span>
    </div>
    {error && <p role="alert" className="banner error">{error}</p>}
    {loading && <p role="status" className="banner">Loading dashboard…</p>}

    <div className="board">
      <section className="card timer-card" aria-label="Workday timer">
        <Hourglass progress={day.progress} running={day.kind === 'running'} />
        <div className="timer-time">{day.time}</div>
        <div className="timer-caption">{day.caption}</div>
      </section>

      <div className="stats">
        <article className="card stat">
          <div className="stat-head"><span className="chip violet"><Icon name="user" /></span>My profile</div>
          <div className="stat-body">
            <div><strong className="stat-value">{data.employee?.employeeCode || 'Not linked'}</strong><p>{data.employee?.department || 'No department assigned'}</p></div>
            {data.employee && <div className="photo"><span>{initials(data.employee.fullName)}</span><img src={data.employee.pictureUrl} alt="" onError={e => {e.currentTarget.style.display = 'none';}} /></div>}
          </div>
        </article>
        <article className="card stat">
          <div className="stat-head"><span className="chip blue"><Icon name="users" /></span>Active employees</div>
          <strong className="stat-value">{data.count ? data.count.activeEmployees : '—'}</strong>
          <p>{canViewEmployees ? 'Across the organization' : 'Not available to your account'}</p>
        </article>
        <article className="card stat">
          <div className="stat-head"><span className="chip amber"><Icon name="coffee" /></span>Break remaining</div>
          <strong className="stat-value">{data.breaks ? duration(allowance - elapsed) : '—'}</strong>
          {data.breaks ? <>
            <div className="bar" aria-hidden="true"><i style={{width: `${breakPct}%`}} /></div>
            <div className="stat-foot"><p>{duration(elapsed)} used today</p>
              <button className={`btn ${data.breaks.activeSince ? 'danger' : 'primary'}`} disabled={breakBusy} onClick={() => action(data.breaks.activeSince ? 'end' : 'start')}>{data.breaks.activeSince ? 'End break' : 'Start break'}</button></div>
          </> : <p>Link an employee profile to use breaks</p>}
        </article>
        <article className="card stat">
          <div className="stat-head"><span className="chip green"><Icon name="calendar" /></span>Upcoming holidays</div>
          <strong className="stat-value">{data.holidays.length}</strong>
          <p>{nextHoliday ? `${nextHoliday.name} on ${shortDate(nextHoliday.date)}` : 'No holidays ahead'}</p>
        </article>
      </div>

      <section className="card week-card">
        <div className="card-title"><h3>This week</h3><span className="muted">Monday to Sunday</span></div>
        <div className="week-grid">{week.map(d => <div className={`week-day ${d.today ? 'today' : ''}`} key={d.key}>
          <small>{d.name.slice(0, 3)}</small>
          <span className={`day-circle ${d.state.toLowerCase().replace(' ', '-')}`} title={d.state}>{Number(d.key.slice(8))}</span>
        </div>)}</div>
        <ul className="legend"><li><i className="dot present" />Present</li><li><i className="dot absent" />Absent</li><li><i className="dot holiday" />Holiday</li><li><i className="dot weekly-off" />Weekly off</li></ul>
      </section>

      <section className="card birthday-card">
        <div className="card-title"><h3>Birthdays</h3><Icon name="cake" /></div>
        {data.birthdays.length ? <ul className="list">{data.birthdays.map(b => <li key={`${b.employeeName}${b.nextOccurrence}`}>
          <span className="person-avatar">{initials(b.employeeName)}</span><span className="grow">{b.employeeName}</span>
          <span className={`tag ${b.today ? 'today' : ''}`}>{b.today ? 'Today' : shortDate(b.nextOccurrence)}</span></li>)}</ul>
          : <Empty icon="cake" text="No birthdays in the next month" />}
      </section>

      <section className="card small-card"><div className="card-title"><h3>Absent today</h3><Icon name="users" /></div><Empty icon="inbox" text="Appears once attendance is finalized" /></section>
      <section className="card small-card third"><div className="card-title"><h3>Approved leaves</h3><Icon name="plane" /></div><Empty icon="inbox" text="No approved leaves recorded" /></section>
      <section className="card small-card third"><div className="card-title"><h3>Leave balance</h3><Icon name="calendar" /></div><Empty icon="inbox" text="No balances recorded yet" /></section>
      <section className="card small-card third"><div className="card-title"><h3>Recent activity</h3><Icon name="pulse" /></div><Empty icon="inbox" text="No activity source connected" /></section>
    </div>
  </>;
}