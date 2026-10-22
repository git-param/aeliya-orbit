// Sidebar component extracted from Dashboard.jsx
// Sidebar component extracted from Dashboard.jsx – now uses shared UI primitives
import React from "react";
import { useNavigate, useLocation } from "react-router-dom";
import { Icon, Button, Heading } from "../UI.jsx"; // shared primitives
import "../modules/dashboard/Dashboard.css"; // shared styles

const allNavItems = [
  { icon: "dashboard", label: "Dashboard", allowedRoles: ["ALL"] },
  { icon: "users", label: "User", allowedRoles: ["SUPER_ADMIN", "ADMIN", "HR", "OPS_MANAGER"] },
  { icon: "settings", label: "Config", allowedRoles: ["SUPER_ADMIN", "ADMIN"] },
  { icon: "calendar", label: "Leave", allowedRoles: ["ALL"] },
];

export default function Sidebar({ currentUser }) {
  const navigate = useNavigate();
  const location = useLocation();
  const rawRole = currentUser?.role || 'USER';
  const currentRole = rawRole.replace('ROLE_', '');

  const visibleNavItems = allNavItems.filter(item => {
    if (item.allowedRoles.includes("ALL")) return true;
    return item.allowedRoles.includes(currentRole);
  });

  const handleNav = (label) => {
    // Placeholder navigation – all go to /dashboard for now
    navigate("/dashboard");
  };

  return (
    <aside className="sidebar">
      <div className="brand">
        <div className="brand-mark">
          <svg viewBox="0 0 24 24" fill="none" stroke="#fff" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" height="24" width="24">
            <circle cx="12" cy="12" r="9" strokeDasharray="10 4" />
            <circle cx="12" cy="12" r="3" fill="#fff" />
          </svg>
        </div>
        <div>
          <div className="brand-name">Aeliya</div>
          <div className="brand-product">ORBIT</div>
        </div>
      </div>

      <div className="nav-label">WORKSPACE</div>
      <nav className="navigation" aria-label="Main navigation">
        {visibleNavItems.map((item, index) => {
          const isActive = location.pathname.toLowerCase().includes(item.label.toLowerCase());
          return (
            <Button
              className={`nav-item ${isActive ? "active" : ""}`}
              key={item.label}
              onClick={() => handleNav(item.label)}
            >
              <Icon name={item.icon} />
              <span>{item.label}</span>
              {index === 0 && <Icon name="arrow" size={16} />}
            </Button>
          );
        })}
      </nav>
    </aside>
  );
}
