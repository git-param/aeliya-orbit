import { useEffect, useState } from 'react';
import { hrApi } from '../../api/client';
import { Icon, initials } from '../../components/UI.jsx';
import '../dashboard/Dashboard.css';
import './EmployeeManagement.css';

const blank = {employeeCode: '', password: '', fullName: '', workEmail: '', dateOfJoining: '', address: '', phone: '', dateOfBirth: '', departmentId: null, designationId: null, levelId: null, reportingManagerId: null};

function Field({ label, wide, children }) {
  return <label className={`field ${wide ? 'wide' : ''}`}>{label}{children}</label>;
}

export default function EmployeeManagement({ user }) {
  const [employees, setEmployees] = useState([]);
  const [page, setPage] = useState(0);
  const [pages, setPages] = useState(0);
  const [search, setSearch] = useState('');
  const [query, setQuery] = useState('');
  const [form, setForm] = useState(blank);
  const [editingId, setEditingId] = useState(null);
  const [drawer, setDrawer] = useState(false);
  const [details, setDetails] = useState(null);
  const [picture, setPicture] = useState(null);
  const [managerSearch, setManagerSearch] = useState('');
  const [managers, setManagers] = useState([]);
  const [options, setOptions] = useState({DEPARTMENT: [], DESIGNATION: [], LEVEL: []});
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [busy, setBusy] = useState(false);
  const privileged = user.role === 'ROLE_ADMIN' || user.role === 'ROLE_SUPER_ADMIN';
  const allows = (id, code) => user.moduleIds?.includes(id) || (privileged && user.permissions.includes(code));
  const canCreate = allows('102', 'EMPLOYEE_CREATE');
  const canView = allows('101', 'EMPLOYEE_VIEW');
  const canDeactivate = allows('104', 'EMPLOYEE_DEACTIVATE');
  const canUpdate = allows('103', 'EMPLOYEE_UPDATE');
  const set = (key, value) => setForm(f => ({...f, [key]: value}));
  const close = () => {setDrawer(false); setDetails(null); setEditingId(null); setForm(blank); setPicture(null); setManagerSearch('');};

  const load = async (targetPage = page, targetQuery = query) => {
    setBusy(true);
    try { const result = await hrApi.employees(targetPage, targetQuery); setEmployees(result.content); setPages(result.totalPages); setError(''); }
    catch { setError('Unable to load employees. Refresh and try again.'); }
    finally { setBusy(false); }
  };
  useEffect(() => { const t = setTimeout(() => { setQuery(search.trim()); setPage(0); }, 300); return () => clearTimeout(t); }, [search]);
  useEffect(() => { if (canView) load(); }, [page, query, canView]);
  useEffect(() => {
    Promise.all(['DEPARTMENT', 'DESIGNATION', 'LEVEL'].map(hrApi.options))
      .then(([DEPARTMENT, DESIGNATION, LEVEL]) => setOptions({DEPARTMENT, DESIGNATION, LEVEL}))
      .catch(() => setError('Department, designation and level lists are unavailable.'));
  }, []);
  useEffect(() => {
    if (!drawer || (details && !editingId)) return;
    let active = true;
    const timer = setTimeout(() => hrApi.managerOptions(managerSearch.trim())
      .then(result => { if (active) setManagers(result); })
      .catch(() => { if (active) setError('Reporting manager choices are unavailable.'); }), 250);
    return () => { active = false; clearTimeout(timer); };
  }, [drawer, details, editingId, managerSearch]);
  useEffect(() => {
    if (!drawer) return;
    const onKey = e => e.key === 'Escape' && close();
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [drawer]);

  const save = async event => {
    event.preventDefault(); setBusy(true); setError(''); setNotice('');
    try {
      const input = {...form, workEmail: form.workEmail.trim().toLowerCase(), dateOfBirth: form.dateOfBirth || null};
      if (editingId) delete input.password;
      const saved = editingId ? await hrApi.updateEmployee(editingId, input) : await hrApi.createEmployee(input);
      if (picture) {
        try { await hrApi.uploadPicture(saved.id, picture); }
        catch { setNotice('Employee saved, but the picture could not be uploaded. Edit the profile to try again.'); }
      } else setNotice(editingId ? 'Employee updated.' : 'Employee and login created. The work email is the username.');
      close(); setPage(0); if (canView) await load(0, query);
    } catch { setError('Employee could not be saved. Check the required fields, password, and that the ID and work email are unique.'); }
    finally { setBusy(false); }
  };
  const edit = async employee => {
    try {
      const d = await hrApi.employee(employee.id);
      setEditingId(d.id);
      setDetails(d);
      setForm({employeeCode: d.employeeCode, fullName: d.fullName, workEmail: d.workEmail, dateOfJoining: d.dateOfJoining,
        address: d.address || '', phone: d.phone || '', dateOfBirth: d.dateOfBirth || '', departmentId: d.departmentId,
        designationId: d.designationId, levelId: d.levelId, reportingManagerId: d.reportingManagerId});
      setDrawer(true);
    } catch { setError('Employee details could not be loaded.'); }
  };
  const view = async employee => {
    try { setDetails(await hrApi.employee(employee.id)); setEditingId(null); setDrawer(true); }
    catch { setError('Employee details could not be loaded.'); }
  };
  const deactivate = async employee => {
    if (!window.confirm(`Deactivate ${employee.fullName}? Their login will be disabled and their records kept.`)) return;
    try { await hrApi.deactivateEmployee(employee.id); await load(); setNotice('Employee deactivated; historical records were kept.'); }
    catch { setError('Employee could not be deactivated.'); }
  };
  const reactivate = async employee => {
    if (!window.confirm(`Reactivate ${employee.fullName}'s employee profile? Any linked login remains disabled until separately authorized.`)) return;
    try { await hrApi.reactivateEmployee(employee.id); await load(); setNotice('Employee profile reactivated. A linked login remains disabled.'); }
    catch { setError('Employee could not be reactivated.'); }
  };
  const select = (label, key, list, currentName) => <Field label={label}>
    <select value={form[key] || ''} onChange={e => set(key, e.target.value ? Number(e.target.value) : null)}>
      <option value="">Not assigned</option>{form[key] && !list.some(o => o.id === form[key]) && <option value={form[key]}>{currentName || 'Archived value'} (archived)</option>}{list.map(o => <option key={o.id} value={o.id}>{o.name}</option>)}
    </select></Field>;

  return <>
    {error && <p role="alert" className="banner error">{error}</p>}
    {notice && <p role="status" className="banner">{notice}</p>}
    <section className="card directory">
      <div className="toolbar">
        {canView ? <div className="search"><Icon name="search" size={16} /><input aria-label="Search employees" placeholder="Search by name or employee ID" value={search} onChange={e => setSearch(e.target.value)} /></div> : <p>Create employee records for your department.</p>}
        <div className="toolbar-end">{busy && <span role="status" className="muted">Loading…</span>}
          {canCreate && <button className="btn primary" onClick={() => {close(); setDrawer(true);}}><Icon name="plus" size={15} /> Add employee</button>}</div>
      </div>
      {canView && <><div className="table-wrap"><table>
        <thead><tr><th>Employee</th><th>Work email</th><th>Department</th><th>Designation</th><th>Status</th><th aria-label="Actions" /></tr></thead>
        <tbody>{employees.map(e => <tr key={e.id}>
          <td><div className="who"><span className="person-avatar">{initials(e.fullName)}</span><div><strong>{e.fullName}</strong><small>{e.employeeCode}</small></div></div></td>
          <td>{e.workEmail}</td><td>{e.department || '—'}</td><td>{e.designation || '—'}</td>
          <td><span className={`status-pill ${e.active ? '' : 'off'}`}>{e.active ? 'Active' : 'Inactive'}</span></td>
          <td><div className="row-actions">
            <button className="btn" onClick={() => view(e)}>View</button>
            {canUpdate && <button className="icon-button" title="Edit" aria-label={`Edit ${e.fullName}`} onClick={() => edit(e)}><Icon name="edit" size={15} /></button>}
            {canDeactivate && e.active && <button className="icon-button danger" title="Deactivate" aria-label={`Deactivate ${e.fullName}`} onClick={() => deactivate(e)}><Icon name="ban" size={15} /></button>}
            {canUpdate && !e.active && <button className="btn" onClick={() => reactivate(e)}>Reactivate</button>}
          </div></td>
        </tr>)}</tbody>
      </table></div>
      {!busy && employees.length === 0 && <div className="empty"><Icon name="users" size={24} /><span>{query ? 'No employees match your search' : 'No employees yet'}</span></div>}
      <div className="pager"><span>Page {page + 1} of {Math.max(1, pages)}</span>
        <div><button className="btn" disabled={page === 0} onClick={() => setPage(page - 1)}>Previous</button><button className="btn" disabled={page + 1 >= pages} onClick={() => setPage(page + 1)}>Next</button></div></div>
      </>}
    </section>

    {drawer && <>
      <div className="scrim" onClick={close} />
      <aside className="drawer" role="dialog" aria-modal="true" aria-label={editingId ? 'Edit employee' : details ? 'Employee details' : 'Add employee'}>
        <div className="drawer-head"><h2>{editingId ? 'Edit employee' : details ? 'Employee details' : 'Add employee'}</h2><button className="icon-button" aria-label="Close" onClick={close}><Icon name="x" /></button></div>
        {details && !editingId ? <div className="employee-detail">
          <div className="detail-person"><div className="detail-photo"><span>{initials(details.fullName)}</span><img src={details.pictureUrl} alt="" style={{objectPosition: `${details.profileFocusX}% ${details.profileFocusY}%`}} onError={event => {event.currentTarget.style.display = 'none';}} /></div><div><h3>{details.fullName}</h3><p>{details.employeeCode} · {details.active ? 'Active' : 'Inactive'}</p></div></div>
          <dl><dt>Work email</dt><dd>{details.workEmail}</dd><dt>Department</dt><dd>{details.department || 'Not assigned'}</dd><dt>Designation</dt><dd>{details.designation || 'Not assigned'}</dd><dt>Level</dt><dd>{details.level || 'Not assigned'}</dd><dt>Reporting manager</dt><dd>{details.reportingManagerName || 'Not assigned'}</dd><dt>Date of joining</dt><dd>{details.dateOfJoining}</dd><dt>Date of birth</dt><dd>{details.dateOfBirth || 'Not provided'}</dd><dt>Phone</dt><dd>{details.phone || 'Not provided'}</dd><dt>Address</dt><dd>{details.address || 'Not provided'}</dd></dl>
          {canUpdate && <button className="btn primary" onClick={() => edit(details)}>Edit profile</button>}
        </div> : <form className="employee-form" onSubmit={save}>
          <div className="form-body">
            <fieldset><legend>Personal</legend>
              <Field label="Full name" wide><input required value={form.fullName} onChange={e => set('fullName', e.target.value)} /></Field>
              <Field label="Employee ID"><input required disabled={Boolean(editingId)} value={form.employeeCode} onChange={e => set('employeeCode', e.target.value)} /></Field>
              <Field label="Date of birth"><input type="date" value={form.dateOfBirth} onChange={e => set('dateOfBirth', e.target.value)} /></Field>
              {canUpdate && <Field label="Profile picture" wide><input type="file" accept="image/png,image/jpeg,image/webp" onChange={e => setPicture(e.target.files?.[0] || null)} /></Field>}
            </fieldset>
            <fieldset><legend>Work</legend>
              <Field label="Work email (login username)" wide><input required type="email" autoComplete="email" value={form.workEmail} onChange={e => set('workEmail', e.target.value)} /></Field>
              {!editingId && <Field label="Login password (at least 8 characters)" wide><input required type="password" minLength={8} autoComplete="new-password" value={form.password} onChange={e => set('password', e.target.value)} /></Field>}
              <Field label="Date of joining"><input required type="date" value={form.dateOfJoining} onChange={e => set('dateOfJoining', e.target.value)} /></Field>
              {select('Department', 'departmentId', options.DEPARTMENT, details?.department)}
              {select('Designation', 'designationId', options.DESIGNATION, details?.designation)}
              {select('Level', 'levelId', options.LEVEL, details?.level)}
              <Field label="Reporting manager search" wide><input value={managerSearch} onChange={e => setManagerSearch(e.target.value)} placeholder="Search by name or employee ID" /></Field>
              <Field label="Reporting manager" wide><select value={form.reportingManagerId || ''} onChange={e => set('reportingManagerId', e.target.value ? Number(e.target.value) : null)}>
                <option value="">Not assigned</option>{form.reportingManagerId && !managers.some(manager => manager.id === form.reportingManagerId) && <option value={form.reportingManagerId}>{details?.reportingManagerName || 'Current manager'}</option>}{managers.filter(manager => manager.id !== editingId).map(manager => <option key={manager.id} value={manager.id}>{manager.fullName} · {manager.employeeCode}</option>)}</select></Field>
            </fieldset>
            <fieldset><legend>Contact</legend>
              <Field label="Primary mobile"><input value={form.phone} onChange={e => set('phone', e.target.value)} /></Field>
              <Field label="Address" wide><input value={form.address} onChange={e => set('address', e.target.value)} /></Field>
            </fieldset>
          </div>
          <div className="form-foot"><button type="button" className="btn" onClick={close}>Cancel</button><button className="btn primary" disabled={busy}>{editingId ? 'Save changes' : 'Create employee'}</button></div>
        </form>}
      </aside>
    </>}
  </>;
}
