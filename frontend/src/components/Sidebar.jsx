// Persistent sidebar. Rendered once by Layout, so it stays mounted while pages change.
import React from "react";
import { NavLink } from "react-router-dom";
import { Icon } from "./UI.jsx";
import "../modules/dashboard/Dashboard.css";

// permission: null = everyone, otherwise the user must hold that permission
const navItems = [
  { icon: "dashboard", label: "Dashboard", to: "/dashboard", permission: null },
  { icon: "users", label: "Employees", to: "/employees", permission: "EMPLOYEE_VIEW" },
];

export default function Sidebar({ currentUser }) {
  const permissions = currentUser?.permissions || [];
  const visible = navItems.filter((item) => !item.permission || permissions.includes(item.permission));

  return (
    <aside className="sidebar">
      <div className="brand">
        <div className="brand-mark"><Icon name="pulse" size={20} /></div>
        <div>
          <div className="brand-name">Aeliya</div>
          <div className="brand-product">Orbit</div>
        </div>
      </div>
      <nav className="navigation" aria-label="Main navigation">
        {visible.map((item) => (
          <NavLink key={item.to} to={item.to} className={({ isActive }) => `nav-item ${isActive ? "active" : ""}`}>
            <Icon name={item.icon} />
            <span>{item.label}</span>
          </NavLink>
        ))}
      </nav>
    </aside>
  );
}