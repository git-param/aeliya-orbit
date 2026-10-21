import React, { createElement, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import "./Dashboard.css";

const iconPaths = {
  dashboard: (
    <>
      <rect x="3" y="3" width="7" height="7" rx="2" />
      <rect x="14" y="3" width="7" height="7" rx="2" />
      <rect x="3" y="14" width="7" height="7" rx="2" />
      <rect x="14" y="14" width="7" height="7" rx="2" />
    </>
  ),
  users: (
    <>
      <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
      <circle cx="9" cy="7" r="4" />
      <path d="M22 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75" />
    </>
  ),
  analytics: (
    <>
      <path d="M4 19V9M10 19V5M16 19v-7M22 19H2" />
    </>
  ),
  settings: (
    <>
      <circle cx="12" cy="12" r="3" />
      <path d="M19.4 15a1.7 1.7 0 0 0 .34 1.88l.06.06-2.83 2.83-.06-.06a1.7 1.7 0 0 0-1.88-.34 1.7 1.7 0 0 0-1.03 1.56V21h-4v-.08A1.7 1.7 0 0 0 8.95 19.4a1.7 1.7 0 0 0-1.88.34l-.06.06-2.83-2.83.06-.06A1.7 1.7 0 0 0 4.6 15 1.7 1.7 0 0 0 3.08 14H3v-4h.08A1.7 1.7 0 0 0 4.6 9a1.7 1.7 0 0 0-.34-1.88l-.06-.06 2.83-2.83.06.06A1.7 1.7 0 0 0 9 4.6 1.7 1.7 0 0 0 10 3.08V3h4v.08A1.7 1.7 0 0 0 15 4.6a1.7 1.7 0 0 0 1.88-.34l.06-.06 2.83 2.83-.06.06A1.7 1.7 0 0 0 19.4 9 1.7 1.7 0 0 0 20.92 10H21v4h-.08A1.7 1.7 0 0 0 19.4 15Z" />
    </>
  ),
  calendar: (
    <>
      <rect x="3" y="4" width="18" height="18" rx="2" ry="2" />
      <line x1="16" y1="2" x2="16" y2="6" />
      <line x1="8" y1="2" x2="8" y2="6" />
      <line x1="3" y1="10" x2="21" y2="10" />
    </>
  ),
  sun: (
    <>
      <circle cx="12" cy="12" r="4" />
      <path d="M12 2v2M12 20v2M4.93 4.93l1.42 1.42M17.66 17.66l1.41 1.41M2 12h2M20 12h2M4.93 19.07l1.42-1.42M17.66 6.34l1.41-1.41" />
    </>
  ),
  moon: <path d="M20.6 15.5A9 9 0 0 1 8.5 3.4 9 9 0 1 0 20.6 15.5Z" />,
  arrow: <path d="m9 18 6-6-6-6" />,
  check: <path d="m5 12 4 4L19 6" />,
  error: (
    <>
      <circle cx="12" cy="12" r="9" />
      <path d="M12 8v4M12 16h.01" />
    </>
  ),
  activity: <path d="M3 12h4l2.5-7 5 14 2.5-7h4" />,
  load: (
    <>
      <path d="M12 2v10l7 4" />
      <circle cx="12" cy="12" r="9" />
    </>
  ),
};

function Icon({ name, size = 20 }) {
  return (
    <svg aria-hidden="true" className="icon" fill="none" height={size} viewBox="0 0 24 24" width={size}>
      {iconPaths[name]}
    </svg>
  );
}

function Button({ children, className, label, onClick }) {
  return createElement(
    "button",
    { className, onClick, "aria-label": label, type: "button" },
    children
  );
}

function Heading({ as, children }) {
  return createElement(as, null, children);
}

const allNavItems = [
  { icon: "dashboard", label: "Dashboard", allowedRoles: ["ALL"] },
  { icon: "users", label: "User", allowedRoles: ["SUPER_ADMIN", "ADMIN", "HR", "OPS_MANAGER"] },
  { icon: "settings", label: "Config", allowedRoles: ["SUPER_ADMIN", "ADMIN"] },
  { icon: "calendar", label: "Leave", allowedRoles: ["ALL"] },
];

const stats = [
  { icon: "users", label: "Total Users", value: "24,892", trend: "+12.5%", variant: "violet" },
  { icon: "activity", label: "Active Sessions", value: "1,429", trend: "+8.2%", variant: "green" },
  { icon: "load", label: "Server Load", value: "42.8%", trend: "Healthy", variant: "amber" },
  { icon: "error", label: "Error Rate", value: "0.12%", trend: "-0.04%", variant: "blue" },
];

const activities = [
  { event: "User authentication", meta: "Web application", person: "Olivia Martin", initials: "OM", time: "2 min ago", status: "Success" },
  { event: "API key generated", meta: "Developer settings", person: "Marcus Chen", initials: "MC", time: "18 min ago", status: "Success" },
  { event: "Failed login attempt", meta: "Unknown device · London, UK", person: "Sophia Rivera", initials: "SR", time: "42 min ago", status: "Attention" },
  { event: "Team permission updated", meta: "Workspace administration", person: "Daniel Foster", initials: "DF", time: "1 hr ago", status: "Success" },
];

export default function Dashboard() {
  const [darkMode, setDarkMode] = useState(false);
  const navigate = useNavigate();

  const rawRole = localStorage.getItem('role') || 'USER';
  const currentRole = rawRole.replace('ROLE_', '');

  // Filter navigation items based on the user's role
  const visibleNavItems = allNavItems.filter((item) => {
    if (item.allowedRoles.includes("ALL")) return true;
    return item.allowedRoles.includes(currentRole);
  });

  useEffect(() => {
    document.documentElement.style.colorScheme = darkMode ? "dark" : "light";
  }, [darkMode]);

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('role');
    navigate('/login', { replace: true });
  };

  return (
    <div className="app-shell" data-theme={darkMode ? "dark" : "light"}>
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
          {visibleNavItems.map((item, index) => (
            <Button className={`nav-item ${index === 0 ? "active" : ""}`} key={item.label}>
              <Icon name={item.icon} />
              <span>{item.label}</span>
              {index === 0 && <Icon name="arrow" size={16} />}
            </Button>
          ))}
        </nav>
      </aside>

      <main className="main-content">
        <header className="topbar">
          <div className="heading-group">
            <div className="eyebrow">CONTROL CENTER</div>
            <Heading as="h1">Overview</Heading>
          </div>
          <div className="header-actions">
            <div className="mode-switch">
              <Icon name="sun" size={17} />
              <Button
                className={`switch ${darkMode ? "on" : ""}`}
                label={`Switch to ${darkMode ? "light" : "dark"} mode`}
                onClick={() => setDarkMode((value) => !value)}
              >
                <span />
              </Button>
              <Icon name="moon" size={17} />
            </div>
            <div className="profile">
              <div className="avatar">AD</div>
              <div className="profile-copy">
                <strong>Current User</strong>
                <span>{currentRole}</span>
              </div>
            </div>
            <Button className="logout-button" onClick={handleLogout}>Logout</Button>
          </div>
        </header>

        <div className="dashboard">
          <section className="welcome-row">
            <div>
              <Heading as="h2">Good morning</Heading>
              <p>Here’s what’s happening across Aeliya Orbit today.</p>
            </div>
            <div className="date-badge">
              <span className="live-dot" />
              Live data · Connected
            </div>
          </section>

          <section className="status-card">
            <div className="status-icon">
              <Icon name="check" size={22} />
            </div>
            <div className="status-copy">
              <span className="status-kicker">SYSTEM STATUS</span>
              <Heading as="h3">All systems operational</Heading>
              <p>
                Connected! Backend reports: <strong>UP</strong> (Aeliya Orbit Backend)
              </p>
            </div>
            <div className="operational-pill">
              <span />
              Connected
            </div>
          </section>

          <section className="stats-grid" aria-label="Quick statistics">
            {stats.map((stat) => (
              <article className="stat-card" key={stat.label}>
                <div className={`stat-icon ${stat.variant}`}>
                  <Icon name={stat.icon} size={21} />
                </div>
                <div className="stat-topline">
                  <span>{stat.label}</span>
                  <span className={stat.label === "Server Load" ? "neutral-trend" : "trend"}>
                    {stat.trend}
                  </span>
                </div>
                <strong className="stat-value">{stat.value}</strong>
                <div className="sparkline" aria-hidden="true">
                  <svg preserveAspectRatio="none" viewBox="0 0 180 34">
                    <path className={`spark-fill ${stat.variant}`} d="M0 31 C18 28,24 17,38 21 S60 30,76 20 S94 8,110 14 S132 26,145 14 S163 11,180 4 V34 H0Z" />
                    <path className={`spark-stroke ${stat.variant}`} d="M0 31 C18 28,24 17,38 21 S60 30,76 20 S94 8,110 14 S132 26,145 14 S163 11,180 4" />
                  </svg>
                </div>
              </article>
            ))}
          </section>

          <section className="activity-card">
            <div className="card-heading">
              <div>
                <Heading as="h3">Recent activity</Heading>
                <p>Latest events from your organization</p>
              </div>
              <Button className="view-all">
                View all activity <Icon name="arrow" size={15} />
              </Button>
            </div>

            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>EVENT</th>
                    <th>USER</th>
                    <th>TIME</th>
                    <th>STATUS</th>
                  </tr>
                </thead>
                <tbody>
                  {activities.map((item) => (
                    <tr key={`${item.event}-${item.person}`}>
                      <td>
                        <div className="event-name">{item.event}</div>
                        <div className="event-meta">{item.meta}</div>
                      </td>
                      <td>
                        <div className="person">
                          <span className="person-avatar">{item.initials}</span>
                          <span>{item.person}</span>
                        </div>
                      </td>
                      <td className="time">{item.time}</td>
                      <td>
                        <span className={`status-badge ${item.status === "Attention" ? "attention" : ""}`}>
                          <span />
                          {item.status}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>
        </div>
      </main>
    </div>
  );
}
