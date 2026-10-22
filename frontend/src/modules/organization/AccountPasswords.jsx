import { useEffect, useState } from 'react';
import { passwordManagementApi } from '../../api/client';
import './AccountPasswords.css';

export default function AccountPasswords({ user }) {
  const [search, setSearch] = useState('');
  const [query, setQuery] = useState('');
  const [page, setPage] = useState(0);
  const [result, setResult] = useState({content: [], totalElements: 0, totalPages: 0});
  const [selected, setSelected] = useState(null);
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  useEffect(() => {
    const timer = setTimeout(() => { setQuery(search.trim()); setPage(0); }, 250);
    return () => clearTimeout(timer);
  }, [search]);
  useEffect(() => {
    let active = true;
    setLoading(true);
    passwordManagementApi.accounts(page, query)
      .then(data => { if (active) { setResult(data); setError(''); } })
      .catch(() => { if (active) setError('Unable to load accounts.'); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [page, query]);

  const close = () => { setSelected(null); setPassword(''); setConfirm(''); };
  const save = async event => {
    event.preventDefault();
    if (password !== confirm) { setError('Passwords do not match.'); return; }
    setSaving(true); setError(''); setNotice('');
    try {
      await passwordManagementApi.change(selected.id, password);
      const own = selected.id === user.id;
      close();
      if (own) window.dispatchEvent(new Event('auth-expired'));
      else setNotice(`${selected.username}'s password was changed. Their existing sessions will expire on the next request.`);
    } catch { setError('Password could not be changed. Check its length and try again.'); }
    finally { setSaving(false); }
  };

  return <div className="account-passwords-page">
    <div className="account-passwords-intro"><h2>Account passwords</h2><p>Reset a password for an existing account. Admins can manage employee accounts and their own; Super Admins can manage all accounts.</p></div>
    {error && !selected && <p role="alert" className="banner error">{error}</p>}
    {notice && <p role="status" className="banner">{notice}</p>}
    <section className="card account-passwords-card">
      <div className="account-passwords-toolbar"><input aria-label="Search accounts" placeholder="Search by username or email" value={search} onChange={e => setSearch(e.target.value)} /><span className="muted">{result.totalElements} accounts</span></div>
      <div className="table-wrap"><table><thead><tr><th>Username</th><th>Role</th><th>Status</th><th aria-label="Actions" /></tr></thead>
        <tbody>{result.content.map(account => <tr key={account.id}>
          <td>{account.username}{account.id === user.id && <span className="muted"> (you)</span>}</td>
          <td>{account.role.replace('ROLE_', '').replace('_', ' ')}</td>
          <td>{account.active ? 'Active' : 'Inactive'}</td>
          <td><button className="btn" type="button" onClick={() => { setSelected(account); setPassword(''); setConfirm(''); setError(''); setNotice(''); }}>Change password</button></td>
        </tr>)}</tbody></table></div>
      {!loading && !result.content.length && <p className="account-passwords-empty">No accounts found.</p>}
      <div className="pager"><span>Page {page + 1} of {Math.max(1, result.totalPages)}</span><div>
        <button className="btn" disabled={page === 0 || loading} onClick={() => setPage(page - 1)}>Previous</button>
        <button className="btn" disabled={page + 1 >= result.totalPages || loading} onClick={() => setPage(page + 1)}>Next</button>
      </div></div>
    </section>
    {selected && <div className="account-passwords-overlay" role="presentation" onMouseDown={event => { if (event.target === event.currentTarget) close(); }}>
      <form className="card account-passwords-dialog" role="dialog" aria-modal="true" aria-label={`Change password for ${selected.username}`} onSubmit={save}>
        <h3>Change password</h3><p>Set a new password for <strong>{selected.username}</strong>.</p>
        {error && <p role="alert" className="banner error">{error}</p>}
        <label>New password<input autoFocus required type="password" autoComplete="new-password" minLength={8} value={password} onChange={e => setPassword(e.target.value)} /></label>
        <label>Confirm password<input required type="password" autoComplete="new-password" minLength={8} value={confirm} onChange={e => setConfirm(e.target.value)} /></label>
        <p className="muted">Use at least 8 characters. Existing sessions will expire after the password changes.</p>
        <div className="account-passwords-actions"><button className="btn" type="button" onClick={close} disabled={saving}>Cancel</button><button className="btn primary" disabled={saving}>{saving ? 'Saving…' : 'Save password'}</button></div>
      </form>
    </div>}
  </div>;
}
