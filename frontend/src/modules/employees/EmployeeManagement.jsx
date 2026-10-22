import { useEffect, useState } from 'react';
import { hrApi } from '../../api/client';
import { Icon, initials } from '../../components/UI.jsx';
import '../dashboard/Dashboard.css';
import './EmployeeManagement.css';

const blank = {employeeCode: '', fullName: '', workEmail: '', dateOfJoining: '', address: '', phone: '', dateOfBirth: '', departmentId: null, designationId: null, levelId: null, reportingManagerId: null};

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
  const [picture, setPicture] = useState(null);
  const [options, setOptions] = useState({DEPARTMENT: [], DESIGNATION: [], LEVEL: []});
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const canCreate = user.permissions.includes('EMPLOYEE_CREATE');
  const canDeactivate = user.permissions.includes('EMPLOYEE_DEACTIVATE');
  const canUpdate = user.permissions.includes('EMPLOYEE_UPDATE');
  const set = (key, value) => setForm(f => ({...f, [key]: value}));
  const close = () => {setDrawer(false); setEditingId(null); setForm(blank); setPicture(null);};

  const load = async () => {
    setBusy(true);
    try { const result = await hrApi.employees(page, query); setEmployees(result.content); setPages(result.totalPages); setError(''); }
    catch { setError('Unable to load employees. Refresh and try again.'); }
    finally { setBusy(false); }
  };
  useEffect(() => { const t = setTimeout(() => { setQuery(search.trim()); setPage(0); }, 300); return () => clearTimeout(t); }, [search]);
  useEffect(() => { load(); }, [page, query]);
  useEffect(() => {
    Promise.all(['DEPARTMENT', 'DESIGNATION', 'LEVEL'].map(hrApi.options))
      .then(([DEPARTMENT, DESIGNATION, LEVEL]) => setOptions({DEPARTMENT, DESIGNATION, LEVEL}))
      .catch(() => setError('Department, designation and level lists are unavailable.'));
  }, []);
  useEffect(() => {
    if (!drawer) return;
    const onKey = e => e.key === 'Escape' && close();
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [drawer]);

  const save = async event => {
    event.preventDefault(); setBusy(true); setError('');
    try {
      const input = {...form, dateOfBirth: form.dateOfBirth || null};
      const saved = editingId ? await hrApi.updateEmployee(editingId, input) : await hrApi.createEmployee(input);
      if (picture) await hrApi.uploadPicture(saved.id, picture);
      close(); setPage(0); await load();
    } catch { setError('Employee could not be saved. Check the required fields and that the ID and email are unique.'); }
    finally { setBusy(false); }
  };
  const edit = async employee => {
    try {
      const d = await hrApi.employee(employee.id);
      setEditingId(d.id);
      setForm({employeeCode: d.employeeCode, fullName: d.fullName, workEmail: d.workEmail, dateOfJoining: d.dateOfJoining,
        address: d.address || '', phone: d.phone || '', dateOfBirth: d.dateOfBirth || '', departmentId: d.departmentId,
        designationId: d.designationId, levelId: d.levelId, reportingManagerId: d.reportingManagerId});
      setDrawer(true);
    } catch { setError('Employee details could not be loaded.'); }
  };
  const deactivate = async employee => {
    if (!window.confirm(`Deactivate ${employee.fullName}? Their login will be disabled and their records kept.`)) return;
    try { await hrApi.deactivateEmployee(employee.id); await load(); }
    catch { setError('Employee could not be deactivated.'); }
  };
  const select = (label, key, list) => <Field label={label}>
    <select value={form[key] || ''} onChange={e => set(key, e.target.value ? Number(e.target.value) : null)}>
      <option value="">Not assigned</option>{list.map(o => <option key={o.id} value={o.id}>{o.name}</option>)}
    </select></Field>;

  return <>
    {error && <p role="alert" className="banner error">{error}</p>}
    <section className="card directory">
      <div className="toolbar">
        <div className="search"><Icon name="search" size={16} /><input aria-label="Search employees" placeholder="Search by name or employee ID" value={search} onChange={e => setSearch(e.target.value)} /></div>
        <div className="toolbar-end">{busy && <span role="status" className="muted">Loading…</span>}
          {canCreate && <button className="btn primary" onClick={() => setDrawer(true)}><Icon name="plus" size={15} /> Add employee</button>}</div>
      </div>
      <div className="table-wrap"><table>
        <thead><tr><th>Employee</th><th>Work email</th><th>Department</th><th>Status</th>{(canUpdate || canDeactivate) && <th aria-label="Actions" />}</tr></thead>
        <tbody>{employees.map(e => <tr key={e.id}>
          <td><div className="who"><span className="person-avatar">{initials(e.fullName)}</span><div><strong>{e.fullName}</strong><small>{e.employeeCode}</small></div></div></td>
          <td>{e.workEmail}</td><td>{e.department || '—'}</td>
          <td><span className={`status-pill ${e.active ? '' : 'off'}`}>{e.active ? 'Active' : 'Inactive'}</span></td>
          {(canUpdate || canDeactivate) && <td><div className="row-actions">
            {canUpdate && <button className="icon-button" title="Edit" aria-label={`Edit ${e.fullName}`} onClick={() => edit(e)}><Icon name="edit" size={15} /></button>}
            {canDeactivate && e.active && <button className="icon-button danger" title="Deactivate" aria-label={`Deactivate ${e.fullName}`} onClick={() => deactivate(e)}><Icon name="ban" size={15} /></button>}
          </div></td>}
        </tr>)}</tbody>
      </table></div>
      {!busy && employees.length === 0 && <div className="empty"><Icon name="users" size={24} /><span>{query ? 'No employees match your search' : 'No employees yet'}</span></div>}
      <div className="pager"><span>Page {page + 1} of {Math.max(1, pages)}</span>
        <div><button className="btn" disabled={page === 0} onClick={() => setPage(page - 1)}>Previous</button><button className="btn" disabled={page + 1 >= pages} onClick={() => setPage(page + 1)}>Next</button></div></div>
    </section>

    {drawer && <>
      <div className="scrim" onClick={close} />
      <aside className="drawer" role="dialog" aria-modal="true" aria-label={editingId ? 'Edit employee' : 'Add employee'}>
        <div className="drawer-head"><h2>{editingId ? 'Edit employee' : 'Add employee'}</h2><button className="icon-button" aria-label="Close" onClick={close}><Icon name="x" /></button></div>
        <form className="employee-form" onSubmit={save}>
          <div className="form-body">
            <fieldset><legend>Personal</legend>
              <Field label="Full name" wide><input required value={form.fullName} onChange={e => set('fullName', e.target.value)} /></Field>
              <Field label="Employee ID"><input required disabled={Boolean(editingId)} value={form.employeeCode} onChange={e => set('employeeCode', e.target.value)} /></Field>
              <Field label="Date of birth"><input type="date" value={form.dateOfBirth} onChange={e => set('dateOfBirth', e.target.value)} /></Field>
              <Field label="Profile picture" wide><input type="file" accept="image/png,image/jpeg,image/webp" onChange={e => setPicture(e.target.files?.[0] || null)} /></Field>
            </fieldset>
            <fieldset><legend>Work</legend>
              <Field label="Work email" wide><input required type="email" value={form.workEmail} onChange={e => set('workEmail', e.target.value)} /></Field>
              <Field label="Date of joining"><input required type="date" value={form.dateOfJoining} onChange={e => set('dateOfJoining', e.target.value)} /></Field>
              {select('Department', 'departmentId', options.DEPARTMENT)}
              {select('Designation', 'designationId', options.DESIGNATION)}
              {select('Level', 'levelId', options.LEVEL)}
              <Field label="Reporting manager" wide><select value={form.reportingManagerId || ''} onChange={e => set('reportingManagerId', e.target.value ? Number(e.target.value) : null)}>
                <option value="">Not assigned</option>{employees.filter(e => e.active && e.id !== editingId).map(e => <option key={e.id} value={e.id}>{e.fullName}</option>)}</select></Field>
            </fieldset>
            <fieldset><legend>Contact</legend>
              <Field label="Phone"><input value={form.phone} onChange={e => set('phone', e.target.value)} /></Field>
              <Field label="Address" wide><input value={form.address} onChange={e => set('address', e.target.value)} /></Field>
            </fieldset>
          </div>
          <div className="form-foot"><button type="button" className="btn" onClick={close}>Cancel</button><button className="btn primary" disabled={busy}>{editingId ? 'Save changes' : 'Create employee'}</button></div>
        </form>
      </aside>
    </>}
  </>;
}