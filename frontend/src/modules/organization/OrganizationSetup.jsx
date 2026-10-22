import { useEffect, useRef, useState } from 'react';
import { ApiError, organizationApi } from '../../api/client';
import { Icon } from '../../components/UI.jsx';
import '../dashboard/Dashboard.css';
import './OrganizationSetup.css';

const kinds = [
  { key: 'DEPARTMENT', label: 'Departments', singular: 'department', help: 'Functional teams employees belong to.' },
  { key: 'DESIGNATION', label: 'Designations', singular: 'designation', help: 'Job titles used in employee profiles.' },
  { key: 'LEVEL', label: 'Levels', singular: 'level', help: 'Organization levels used in employee profiles.' },
];

export default function OrganizationSetup() {
  const [kind, setKind] = useState(kinds[0]);
  const [items, setItems] = useState([]);
  const [name, setName] = useState('');
  const [editing, setEditing] = useState(null);
  const [editName, setEditName] = useState('');
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const requestId = useRef(0);

  const load = async (key = kind.key) => {
    const current = ++requestId.current;
    setLoading(true);
    try { const result = await organizationApi.list(key); if (current === requestId.current) { setItems(result); setError(''); } }
    catch { if (current === requestId.current) setError('Unable to load organization setup. Try again.'); }
    finally { if (current === requestId.current) setLoading(false); }
  };
  useEffect(() => { load(kind.key); }, [kind.key]);

  const run = async (work, success) => {
    setBusy(true); setError(''); setNotice('');
    try { await work(); await load(); setNotice(success); return true; }
    catch (cause) { setError(cause instanceof ApiError && cause.status === 409 ? 'That name already exists in this category.' : 'The change could not be saved. Try again.'); return false; }
    finally { setBusy(false); }
  };
  const add = async event => {
    event.preventDefault();
    const trimmed = name.trim();
    if (!trimmed) return;
    if (await run(() => organizationApi.create(kind.key, trimmed), `${kind.singular[0].toUpperCase()}${kind.singular.slice(1)} added.`)) setName('');
  };
  const rename = async event => {
    event.preventDefault();
    const trimmed = editName.trim();
    if (!trimmed || !editing) return;
    if (await run(() => organizationApi.rename(editing.id, trimmed), 'Name updated.')) setEditing(null);
  };
  const changeStatus = item => {
    const action = item.active ? 'Archive' : 'Restore';
    if (item.active && !window.confirm(`Archive ${item.name}? Existing employee profiles keep this value, but it will not be offered for new assignments.`)) return;
    run(() => organizationApi.setActive(item.id, !item.active), `${action} complete.`);
  };

  return <div className="organization-setup">
    <div className="setup-intro"><div><h2>Organization structure</h2><p>Maintain the values available when creating and editing employee profiles.</p></div></div>
    <div className="setup-tabs" role="tablist" aria-label="Organization settings">
      {kinds.map(entry => <button key={entry.key} type="button" role="tab" aria-selected={kind.key === entry.key} disabled={busy}
        className={kind.key === entry.key ? 'selected' : ''} onClick={() => { setKind(entry); setEditing(null); setName(''); setError(''); setNotice(''); }}>
        {entry.label}
      </button>)}
    </div>
    {error && <p className="banner error" role="alert">{error}</p>}
    {notice && <p className="banner" role="status">{notice}</p>}
    <section className="card setup-card" aria-label={kind.label}>
      <div className="setup-card-head"><div><h3>{kind.label}</h3><p>{kind.help}</p></div><span className="muted">{items.filter(item => item.active).length} active</span></div>
      <form className="setup-add" onSubmit={add}>
        <label htmlFor="option-name">New {kind.singular}</label>
        <div><input id="option-name" maxLength={120} required value={name} onChange={event => setName(event.target.value)} placeholder={`Enter ${kind.singular} name`} />
          <button className="btn primary" disabled={busy || !name.trim()}><Icon name="plus" size={16} /> Add</button></div>
      </form>
      {loading ? <p className="setup-empty" role="status">Loading {kind.label.toLowerCase()}…</p> : items.length === 0 ?
        <p className="setup-empty">No {kind.label.toLowerCase()} configured yet. Add the first one above.</p> :
        <div className="table-wrap"><table><thead><tr><th>Name</th><th>Status</th><th>Actions</th></tr></thead><tbody>
          {items.map(item => <tr key={item.id}>
            <td>{editing?.id === item.id ? <form className="setup-rename" onSubmit={rename}><input aria-label={`Rename ${item.name}`} maxLength={120} required value={editName} onChange={event => setEditName(event.target.value)} autoFocus /><button className="btn primary" disabled={busy}>Save</button><button type="button" className="btn" onClick={() => setEditing(null)}>Cancel</button></form> : <strong>{item.name}</strong>}</td>
            <td><span className={`status-pill ${item.active ? '' : 'off'}`}>{item.active ? 'Active' : 'Archived'}</span></td>
            <td><div className="setup-actions"><button type="button" className="btn" disabled={busy} onClick={() => { setEditing(item); setEditName(item.name); }}>Rename</button><button type="button" className={`btn ${item.active ? 'danger' : ''}`} disabled={busy} onClick={() => changeStatus(item)}>{item.active ? 'Archive' : 'Restore'}</button></div></td>
          </tr>)}
        </tbody></table></div>}
    </section>
  </div>;
}
