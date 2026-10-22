import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { hrApi } from '../../api/client';
import '../dashboard/Dashboard.css';
import './EmployeeManagement.css';

const blank = {employeeCode: '', fullName: '', workEmail: '', dateOfJoining: '', address: '', phone: '', dateOfBirth: '', departmentId: null, designationId: null, levelId: null};
export default function EmployeeManagement({ user, onLogout }) {
  const [employees, setEmployees] = useState([]);
  const [page, setPage] = useState(0);
  const [pages, setPages] = useState(0);
  const [search, setSearch] = useState('');
  const [form, setForm] = useState(blank);
  const [editingId, setEditingId] = useState(null);
  const [picture, setPicture] = useState(null);
  const [options, setOptions] = useState({DEPARTMENT: [], DESIGNATION: [], LEVEL: []});
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const canCreate = user.permissions.includes('EMPLOYEE_CREATE');
  const canDeactivate = user.permissions.includes('EMPLOYEE_DEACTIVATE');
  const canUpdate = user.permissions.includes('EMPLOYEE_UPDATE');
  const load = async () => {
    setBusy(true);
    try { const result = await hrApi.employees(page, search); setEmployees(result.content); setPages(result.totalPages); setError(''); }
    catch { setError('Unable to load employees.'); }
    finally { setBusy(false); }
  };
  useEffect(() => { load(); }, [page, search]);
  useEffect(() => {
    Promise.all(['DEPARTMENT', 'DESIGNATION', 'LEVEL'].map(hrApi.options))
      .then(([DEPARTMENT, DESIGNATION, LEVEL]) => setOptions({DEPARTMENT, DESIGNATION, LEVEL}))
      .catch(() => setError('Organization options are unavailable.'));
  }, []);
  const save = async (event) => {
    event.preventDefault(); setBusy(true); setError('');
    try {
      const input = {...form, dateOfBirth: form.dateOfBirth || null};
      const saved = editingId ? await hrApi.updateEmployee(editingId, input) : await hrApi.createEmployee(input);
      if (picture) await hrApi.uploadPicture(saved.id, picture);
      setForm(blank); setEditingId(null); setPicture(null); setPage(0); await load();
    } catch { setError('Employee could not be saved. Check required fields and uniqueness.'); }
    finally { setBusy(false); }
  };
  const edit = async (employee) => {
    try {
      const details = await hrApi.employee(employee.id);
      setEditingId(details.id);
      setForm({employeeCode: details.employeeCode, fullName: details.fullName, workEmail: details.workEmail,
        dateOfJoining: details.dateOfJoining, address: details.address || '', phone: details.phone || '',
        dateOfBirth: details.dateOfBirth || '', departmentId: details.departmentId,
        designationId: details.designationId, levelId: details.levelId, reportingManagerId: details.reportingManagerId});
    } catch { setError('Employee details could not be loaded.'); }
  };
  const deactivate = async (employee) => {
    if (!window.confirm(`Deactivate ${employee.fullName}?`)) return;
    try { await hrApi.deactivateEmployee(employee.id); await load(); }
    catch { setError('Employee could not be deactivated.'); }
  };
  return <div className="app-shell"><aside className="sidebar"><div className="brand"><div className="brand-name">Aeliya</div><div className="brand-product">ORBIT</div></div><nav className="navigation"><Link className="nav-item" to="/dashboard">Dashboard</Link><Link className="nav-item active" to="/employees">Employees</Link></nav></aside>
    <main className="main-content"><header className="topbar"><div className="heading-group"><div className="eyebrow">WORKSPACE</div><h1>Employees</h1></div><div className="header-actions"><span>{user.username}</span><button className="logout-button" onClick={async () => {try {await onLogout();} catch {setError('Logout failed. Please try again.');}}}>Logout</button></div></header>
      <div className="dashboard employee-page"><section className="activity-card"><div className="card-heading"><div><h2>Employee directory</h2><p>Active and historical profiles</p></div></div>
        <input aria-label="Search employees" placeholder="Search name or employee ID" value={search} onChange={e => {setSearch(e.target.value); setPage(0);}} />
        {error && <p role="alert">{error}</p>}{busy && <p role="status">Loading…</p>}
        <div className="table-wrap"><table><thead><tr><th>ID</th><th>NAME</th><th>EMAIL</th><th>DEPARTMENT</th><th>STATUS</th><th>ACTION</th></tr></thead><tbody>
          {employees.map(e => <tr key={e.id}><td>{e.employeeCode}</td><td>{e.fullName}</td><td>{e.workEmail}</td><td>{e.department || '—'}</td><td>{e.active ? 'Active' : 'Inactive'}</td><td>{canUpdate && <button onClick={() => edit(e)}>Edit</button>} {canDeactivate && e.active && <button onClick={() => deactivate(e)}>Deactivate</button>}</td></tr>)}
        </tbody></table></div>{!busy && employees.length === 0 && <p>No employees found.</p>}
        <div className="employee-pagination"><button disabled={page === 0} onClick={() => setPage(page - 1)}>Previous</button><span>Page {page + 1} of {Math.max(1, pages)}</span><button disabled={page + 1 >= pages} onClick={() => setPage(page + 1)}>Next</button></div>
      </section>
      {(canCreate || editingId) && <section className="activity-card"><h2>{editingId ? 'Edit employee' : 'Add employee'}</h2><form className="employee-form" onSubmit={save}>
        {['employeeCode', 'fullName', 'workEmail', 'dateOfJoining', 'address', 'phone', 'dateOfBirth'].map(key => <label key={key}>{key.replace(/([A-Z])/g, ' $1')}<input disabled={key === 'employeeCode' && Boolean(editingId)} required={['employeeCode','fullName','workEmail','dateOfJoining'].includes(key)} type={key.startsWith('date') ? 'date' : key === 'workEmail' ? 'email' : 'text'} value={form[key]} onChange={e => setForm({...form, [key]: e.target.value})} /></label>)}
        {['DEPARTMENT','DESIGNATION','LEVEL'].map(kind => <label key={kind}>{kind}<select value={form[kind.toLowerCase() + 'Id'] || ''} onChange={e => setForm({...form, [kind.toLowerCase() + 'Id']: e.target.value ? Number(e.target.value) : null})}><option value="">Unassigned</option>{options[kind].map(o => <option key={o.id} value={o.id}>{o.name}</option>)}</select></label>)}
        <label>Reporting manager<select value={form.reportingManagerId || ''} onChange={e => setForm({...form, reportingManagerId: e.target.value ? Number(e.target.value) : null})}><option value="">Unassigned</option>{employees.filter(e => e.active && e.id !== editingId).map(e => <option key={e.id} value={e.id}>{e.fullName}</option>)}</select></label>
        <label>Profile picture<input type="file" accept="image/png,image/jpeg,image/webp" onChange={e => setPicture(e.target.files?.[0] || null)} /></label>
        <button className="logout-button" disabled={busy}>{editingId ? 'Save changes' : 'Create employee'}</button>{editingId && <button type="button" onClick={() => {setEditingId(null); setForm(blank); setPicture(null);}}>Cancel</button>}
      </form></section>}
      </div></main></div>;
}
