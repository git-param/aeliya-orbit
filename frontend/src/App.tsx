import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import Login from './modules/login/Login';
import Dashboard from './modules/dashboard/Dashboard'; // Import the new component

const ProtectedRoute = ({ children, allowedRoles }) => {
    const token = localStorage.getItem('token');
    const role = localStorage.getItem('role');

    if (!token) {
        return <Navigate to="/login" replace />;
    }

    if (allowedRoles && !allowedRoles.includes(role)) {
        return <div>Unauthorized Access</div>;
    }

    return children;
};

function App() {
    return (
        <Router>
            <Routes>
                <Route path="/login" element={<Login />} />

                <Route 
                    path="/dashboard" 
                    element={
                        <ProtectedRoute allowedRoles={['ROLE_SUPER_ADMIN', 'ROLE_ADMIN', 'ROLE_USER', 'SUPER_ADMIN', 'ADMIN', 'USER']}>
                            <Dashboard />
                        </ProtectedRoute>
                    } 
                />

                <Route path="*" element={<Navigate to="/login" replace />} />
            </Routes>
        </Router>
    );
}

export default App;