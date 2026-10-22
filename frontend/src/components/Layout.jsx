// Route layout: Sidebar and TopNavbar mount once; only <Outlet /> changes between tabs.
import React, { useEffect, useState } from "react";
import { Outlet } from "react-router-dom";
import Sidebar from "./Sidebar.jsx";
import TopNavbar from "./TopNavbar.jsx";
import "../modules/dashboard/Dashboard.css";

export default function Layout({ user, onLogout }) {
  const [darkMode, setDarkMode] = useState(() => {
    try { return localStorage.getItem("orbit-theme") === "dark"; } catch { return false; }
  });
  useEffect(() => {
    document.documentElement.style.colorScheme = darkMode ? "dark" : "light";
    try { localStorage.setItem("orbit-theme", darkMode ? "dark" : "light"); } catch { /* storage unavailable */ }
  }, [darkMode]);

  return (
    <div className="app-shell" data-theme={darkMode ? "dark" : "light"}>
      <Sidebar currentUser={user} />
      <main className="main-content">
        <TopNavbar darkMode={darkMode} setDarkMode={setDarkMode} onLogout={onLogout} currentUser={user} />
        <div className="page"><Outlet /></div>
      </main>
    </div>
  );
}