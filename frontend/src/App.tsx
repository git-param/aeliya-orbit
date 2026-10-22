import { useEffect, useState } from 'react';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import Login from './modules/login/Login.jsx';
import Dashboard from './modules/dashboard/Dashboard.jsx';
import EmployeeManagement from './modules/employees/EmployeeManagement.jsx';
import { api, type CurrentUser } from './api/client';

export default function App() {
  const [user, setUser] = useState<CurrentUser | null>(null);
  const [checking, setChecking] = useState(true);

  useEffect(() => {
    api.me().then(setUser).catch(() => setUser(null)).finally(() => setChecking(false));
  }, []);

  useEffect(() => {
    const expired = () => setUser(null);
    window.addEventListener('auth-expired', expired);
    return () => window.removeEventListener('auth-expired', expired);
  }, []);

  const logout = async () => {
    await api.logout();
    setUser(null);
  };

  if (checking) return <div role="status">Checking session…</div>;
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={user ? <Navigate to="/dashboard" replace /> : <Login onAuthenticated={setUser} />} />
        <Route path="/dashboard" element={user ? <Dashboard user={user} onLogout={logout} /> : <Navigate to="/login" replace />} />
        <Route path="/employees" element={user && user.permissions.includes('EMPLOYEE_VIEW') ? <EmployeeManagement user={user} onLogout={logout} /> : <Navigate to={user ? '/dashboard' : '/login'} replace />} />
        <Route path="*" element={<Navigate to={user ? '/dashboard' : '/login'} replace />} />
      </Routes>
    </BrowserRouter>
  );
}
