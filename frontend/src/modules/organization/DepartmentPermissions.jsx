import { useEffect, useState } from 'react';
import { ApiError, departmentPermissionApi } from '../../api/client';
import './DepartmentPermissions.css';

export default function DepartmentPermissions() {
  const [modules, setModules] = useState([]);
  const [departments, setDepartments] = useState([]);
  const [drafts, setDrafts] = useState({});
  const [busy, setBusy] = useState(null);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  const load = async () => {
    const [catalog, rows] = await Promise.all([departmentPermissionApi.modules(), departmentPermissionApi.departments()]);
    setModules(catalog);
    setDepartments(rows);
    setDrafts(Object.fromEntries(rows.map(row => [row.id, row.moduleIds])));
  };
  useEffect(() => { load().catch(() => setError('Unable to load department permissions.')); }, []);

  const toggle = (departmentId, moduleId) => setDrafts(current => {
    const selected = new Set(current[departmentId] || []);
    if (selected.has(moduleId)) selected.delete(moduleId); else selected.add(moduleId);
    return {...current, [departmentId]: [...selected]};
  });
  const save = async department => {
    setBusy(department.id); setError(''); setMessage('');
    try {
      const updated = await departmentPermissionApi.replace(department.id, department.version, drafts[department.id] || []);
      setDepartments(rows => rows.map(row => row.id === updated.id ? updated : row));
      setDrafts(current => ({...current, [updated.id]: updated.moduleIds}));
      setMessage(`${department.name} permissions saved.`);
    } catch (cause) {
      if (cause instanceof ApiError && cause.status === 409) {
        await load().catch(() => {});
        setError('Permissions changed elsewhere. Latest values have been loaded; review and save again.');
      } else setError('Unable to save department permissions.');
    } finally { setBusy(null); }
  };

  return <div className="department-permissions-page">
    <header><h1>Department Permissions</h1><p>Assign employee modules to everyone in a department.</p></header>
    {error && <p role="alert" className="banner error">{error}</p>}
    {message && <p role="status" className="banner">{message}</p>}
    <div className="department-permissions-list">
      {departments.map(department => <section className="card department-permission-card" key={department.id}>
        <div><h2>{department.name}</h2>{!department.active && <span className="muted">Inactive department</span>}</div>
        <div className="department-module-grid">
          {modules.map(module => <label key={module.id}>
            <input type="checkbox" checked={(drafts[department.id] || []).includes(module.id)}
              onChange={() => toggle(department.id, module.id)} disabled={busy === department.id} />
            <span><strong>{module.name}</strong><small>Module {module.id}</small></span>
          </label>)}
        </div>
        <button className="btn primary" disabled={busy === department.id ||
          JSON.stringify([...(drafts[department.id] || [])].sort()) === JSON.stringify([...department.moduleIds].sort())}
          onClick={() => save(department)}>{busy === department.id ? 'Saving…' : 'Save permissions'}</button>
      </section>)}
      {!departments.length && !error && <p>No departments have been configured yet. Add one in Organization Setup.</p>}
    </div>
  </div>;
}
