// frontend/src/modules/login/Login.jsx
import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import './Login.css';
import logo from '../../assets/logo.png';

const Login = () => {
    const [credentials, setCredentials] = useState({ username: '', password: '' });
    const [showPassword, setShowPassword] = useState(false);
    const [error, setError] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const navigate = useNavigate();

    const handleChange = (e) => {
        const { name, value } = e.target;
        setCredentials((prev) => ({ ...prev, [name]: value }));
    };

    const togglePasswordVisibility = (e) => {
        e.preventDefault();
        setShowPassword(!showPassword);
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setIsLoading(true);

        try {
            const response = await fetch('http://localhost:8080/api/auth/login', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(credentials),
            });

            if (response.ok) {
                const data = await response.json();
                
                localStorage.setItem('token', data.token);
                localStorage.setItem('role', data.role);

                navigate('/dashboard');
            } else {
                const errorData = await response.text();
                setError(errorData || 'Invalid username or password.');
            }
        } catch (err) {
            console.error('Login error:', err);
            setError('Unable to reach the authentication server. Is your backend active?');
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="login-container">
            <div className="brand-header">
                <img src={logo} alt="Aeliya Marine Tech Logo" className="brand-logo" />
            </div>

            <div className="login-card">
                <h2 className="login-title">Login to Aeliya Orbit</h2>
                <div className="login-subtitle">Development Foundation</div>

                <form onSubmit={handleSubmit}>
                    <div className="form-group">
                        <label htmlFor="username">Username</label>
                        <div className="input-wrapper">
                            <span className="input-icon">
                                <svg fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                                    <path strokeLinecap="round" strokeLinejoin="round" d="M15.75 6a3.75 3.75 0 11-7.5 0 3.75 3.75 0 01-7.5 0zM4.501 20.118a7.5 7.5 0 0114.998 0A17.933 17.933 0 0112 21.75c-2.676 0-5.216-.584-7.499-1.632z"></path>
                                </svg>
                            </span>
                            <input
                                type="text"
                                id="username"
                                name="username"
                                value={credentials.username}
                                onChange={handleChange}
                                placeholder="Enter your username"
                                required
                                autoComplete="username"
                            />
                        </div>
                    </div>

                    <div className="form-group">
                        <label htmlFor="password">Password</label>
                        <div className="input-wrapper">
                            <span className="input-icon">
                                <svg fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                                    <path strokeLinecap="round" strokeLinejoin="round" d="M16.5 10.5V6.75a4.5 4.5 0 10-9 0v3.75m-.75 11.25h10.5a2.25 2.25 0 002.25-2.25v-6.75a2.25 2.25 0 00-2.25-2.25H6.75a2.25 2.25 0 00-2.25 2.25v6.75a2.25 2.25 0 002.25 2.25z"></path>
                                </svg>
                            </span>
                            <input
                                type={showPassword ? 'text' : 'password'}
                                id="password"
                                name="password"
                                value={credentials.password}
                                onChange={handleChange}
                                placeholder="••••••••"
                                required
                                autoComplete="current-password"
                            />
                            <button type="button" className="password-action" onClick={togglePasswordVisibility}>
                                {showPassword ? 'Hide' : 'Show'}
                            </button>
                        </div>
                    </div>

                    <div className="forgot-password-container">
                        <a href="#forgot" className="forgot-password-link" onClick={(e) => { e.preventDefault(); alert('Please contact system admin to reset credentials.'); }}>
                            Forgot Password?
                        </a>
                    </div>

                    <button type="submit" className="login-btn" disabled={isLoading}>
                        {isLoading ? 'Authenticating...' : 'Login'}
                    </button>

                    {error && (
                        <div className="error-alert">
                            <svg className="error-alert-icon" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                                <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"></path>
                            </svg>
                            <span>{error}</span>
                        </div>
                    )}
                </form>
            </div>

            <div className="login-footer">
                &copy; 2026 Aeliya Orbit Development Foundation. All rights reserved.
            </div>
        </div>
    );
};

export default Login;