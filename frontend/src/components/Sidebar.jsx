// Persistent sidebar. Rendered once by Layout, so it stays mounted while pages change.
import React from "react";
import { NavLink } from "react-router-dom";
import { Icon } from "./UI.jsx";
import "../modules/dashboard/Dashboard.css";

const groups = [
  { label: "WORKSPACE", items: [{ icon: "dashboard", label: "Dashboard", to: "/dashboard" }] },
  { label: "PEOPLE", items: [{ icon: "users", label: "Employees", to: "/employees", modules: ["101", "102"] }] },
  { label: "ADMINISTRATION", items: [
    { icon: "users", label: "Account Passwords", to: "/account-passwords", roles: ["ROLE_ADMIN", "ROLE_SUPER_ADMIN"] },
    { icon: "settings", label: "Organization Setup", to: "/organization-setup", permission: "DEPARTMENT_MANAGE", roles: ["ROLE_ADMIN", "ROLE_SUPER_ADMIN"] },
    { icon: "settings", label: "Department Permissions", to: "/department-permissions", permission: "DEPARTMENT_MANAGE", roles: ["ROLE_ADMIN", "ROLE_SUPER_ADMIN"] },
  ] },
];

export default function Sidebar({ currentUser }) {
  const permissions = currentUser?.permissions || [];
  const moduleIds = currentUser?.moduleIds || [];
  const visible = groups.map(group => ({...group, items: group.items.filter(item =>
    (!item.permission || permissions.includes(item.permission)) && (!item.roles || item.roles.includes(currentUser?.role)) &&
    (!item.modules || item.modules.some(id => moduleIds.includes(id)) ||
      (['ROLE_ADMIN', 'ROLE_SUPER_ADMIN'].includes(currentUser?.role) && (permissions.includes('EMPLOYEE_VIEW') || permissions.includes('EMPLOYEE_CREATE'))))
  )})).filter(group => group.items.length);

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
        {visible.map(group => <div className="nav-group" key={group.label}>
          <span className="nav-label">{group.label}</span>
          {group.items.map(item => <NavLink key={item.to} to={item.to} className={({ isActive }) => `nav-item ${isActive ? "active" : ""}`}>
            <Icon name={item.icon} /><span>{item.label}</span>
          </NavLink>)}
        </div>)}
      </nav>
    </aside>
  );
}
