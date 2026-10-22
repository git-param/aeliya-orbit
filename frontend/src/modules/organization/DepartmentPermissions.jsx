import { useEffect, useState } from 'react';
import { ApiError, departmentPermissionApi } from '../../api/client';
import './DepartmentPermissions.css';

const initialSections = ['Dashboard', 'Employee', 'Configuration'];

export default function DepartmentPermissions() {
  const [modules, setModules] = useState([]);
  const [departments, setDepartments] = useState([]);
  const [drafts, setDrafts] = useState({});
  const [openId, setOpenId] = useState(null);
  const [busy, setBusy] = useState(null);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  const load = async () => {
    const [catalog, rows] = await Promise.all([departmentPermissionApi.modules(), departmentPermissionApi.departments()]);
    setModules(catalog);
    setDepartments(rows);
    setDrafts(Object.fromEntries(rows.map(row => [row.id, row.moduleIds])));
  };
  useEffect(() => {
    load().catch(() => setError('Unable to load department permissions.')).finally(() => setLoading(false));
  }, []);

  const sections = [...new Set([...initialSections, ...modules.map(module => module.section)])];
  const open = department => {
    if (busy !== null) return;
    setDrafts(current => ({ ...current, [department.id]: [...department.moduleIds] }));
    setOpenId(department.id);
    setError('');
    setMessage('');
  };
  const cancel = department => {
    setDrafts(current => ({ ...current, [department.id]: [...department.moduleIds] }));
    setOpenId(null);
    setError('');
  };
  const toggle = (departmentId, moduleId) => setDrafts(current => {
    const selected = new Set(current[departmentId] || []);
    if (selected.has(moduleId)) selected.delete(moduleId); else selected.add(moduleId);
    return { ...current, [departmentId]: [...selected] };
  });
  const save = async department => {
    setBusy(department.id);
    setError('');
    setMessage('');
    try {
      const updated = await departmentPermissionApi.replace(department.id, department.version, drafts[department.id] || []);
      setDepartments(rows => rows.map(row => row.id === updated.id ? updated : row));
      setDrafts(current => ({ ...current, [updated.id]: updated.moduleIds }));
      setOpenId(null);
      setMessage(`${department.name} permissions saved.`);
    } catch (cause) {
      if (cause instanceof ApiError && cause.status === 409) {
        try {
          await load();
          setError('Permissions changed elsewhere. Latest values have been loaded; review and save again.');
        } catch {
          setError('Permissions changed elsewhere. Reload the page before saving again.');
        }
      } else setError('Unable to save department permissions.');
    } finally { setBusy(null); }
  };

  return <div className="department-permissions-page">
    <header><h1>Department Permissions</h1><p>Choose which permissions are available to each department.</p></header>
    {error && <p role="alert" className="banner error">{error}</p>}
    {message && <p role="status" className="banner">{message}</p>}
    <div className="department-permissions-list">
      {departments.map(department => {
        const expanded = openId === department.id;
        const selected = drafts[department.id] || [];
        const changed = selected.length !== department.moduleIds.length ||
          selected.some(id => !department.moduleIds.includes(id));
        return <section className={`card department-permission-card${expanded ? ' expanded' : ''}`} key={department.id}>
          <button className="department-card-toggle" type="button" aria-expanded={expanded}
            aria-controls={`department-editor-${department.id}`} onClick={() => expanded ? cancel(department) : open(department)}
            disabled={busy !== null}>
            <span className="department-card-details">
              <strong>{department.name}</strong>
              <span className="department-card-meta">
                {department.moduleIds.length} of {modules.length} permissions allowed
                {!department.active && <span className="department-inactive">Inactive</span>}
              </span>
            </span>
            <span className="department-card-action">{expanded ? 'Close' : 'Edit permissions'} <span aria-hidden="true">{expanded ? '−' : '→'}</span></span>
          </button>
          {expanded && <div className="department-editor" id={`department-editor-${department.id}`}>
            <p className="department-editor-intro">Select the permissions allowed for {department.name}.</p>
            {sections.map(section => {
              const entries = modules.filter(module => module.section === section);
              return <section className="department-permission-section" key={section} aria-label={`${section} permissions`}>
                <div className="department-section-heading"><h3>{section}</h3><span>{entries.length} {entries.length === 1 ? 'permission' : 'permissions'}</span></div>
                {entries.length ? <div className="department-module-grid">
                  {entries.map(module => <label className="department-module-option" key={module.id}>
                    <input type="checkbox" checked={selected.includes(module.id)}
                      onChange={() => toggle(department.id, module.id)} disabled={busy === department.id} />
                    <span><strong>{module.name}</strong><small>Module {module.id}</small></span>
                  </label>)}
                </div> : <p className="department-section-empty">No permissions available in this section yet.</p>}
              </section>;
            })}
            <div className="department-editor-actions">
              <button className="btn primary" type="button" disabled={busy === department.id || !changed}
                onClick={() => save(department)}>{busy === department.id ? 'Saving…' : 'Save permissions'}</button>
              <button className="btn" type="button" onClick={() => cancel(department)} disabled={busy === department.id}>Cancel</button>
            </div>
          </div>}
        </section>;
      })}
      {!loading && !departments.length && !error && <p>No departments have been configured yet. Add one in Organization Setup.</p>}
    </div>
  </div>;
}
