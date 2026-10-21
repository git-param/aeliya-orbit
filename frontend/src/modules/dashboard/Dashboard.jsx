// frontend/src/modules/dashboard/Dashboard.jsx
import React from 'react';
import { useNavigate } from 'react-router-dom';
import './Dashboard.css';

const Dashboard = () => {
    const navigate = useNavigate();

    const handleLogout = () => {
        // Clear authentication data
        localStorage.removeItem('token');
        localStorage.removeItem('role');
        // Redirect to login
        navigate('/login');
    };

    return (
        <div className="dashboard-layout">
            <header className="dashboard-header">
                <h1>Aeliya Orbit - Development Foundation</h1>
                <button className="header-logout-btn" onClick={handleLogout}>
                    Logout
                </button>
            </header>

            <main className="dashboard-content">
                <div className="status-card">
                    <h2>System Status</h2>
                    <p>Connected! Backend reports: UP (Aeliya Orbit Backend)</p>
                </div>
            </main>
        </div>
    );
};

export default Dashboard;