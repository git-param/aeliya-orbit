// Persistent top bar. Title follows the current route; everything else stays put.
import React, { useState } from "react";
import { useLocation } from "react-router-dom";
import { Icon, Button, Heading, initials } from "./UI.jsx";
import "../modules/dashboard/Dashboard.css";

const pages = {
  "/dashboard": { title: "Overview", subtitle: () => new Intl.DateTimeFormat("en-IN", { dateStyle: "full" }).format(new Date()) },
  "/employees": { title: "Employee Directory", subtitle: () => "Profiles and employment details" },
  "/organization-setup": { title: "Organization Setup", subtitle: () => "Departments, designations, and levels" },
};

export default function TopNavbar({ darkMode, setDarkMode, onLogout, currentUser }) {
  const { pathname } = useLocation();
  const [error, setError] = useState("");
  const page = pages[pathname.toLowerCase()] || pages["/dashboard"];
  const username = currentUser?.username || "Current User";
  const role = currentUser?.role?.replace("ROLE_", "") || "USER";

  return (
    <>
      <header className="topbar">
        <div className="heading-group">
          <Heading as="h1">{page.title}</Heading>
          <p>{page.subtitle()}</p>
        </div>
        <div className="header-actions">
          <Button className="icon-button" label={`Switch to ${darkMode ? "light" : "dark"} theme`} onClick={() => setDarkMode((v) => !v)}>
            <Icon name={darkMode ? "sun" : "moon"} />
          </Button>
          <div className="profile">
            <div className="avatar">{initials(username)}</div>
            <div className="profile-copy">
              <strong>{username}</strong>
              <span>{role}</span>
            </div>
          </div>
          <Button className="icon-button" label="Log out" title="Log out"
            onClick={async () => { try { await onLogout(); } catch { setError("Logout failed. Try again."); } }}>
            <Icon name="logout" />
          </Button>
        </div>
      </header>
      {error && <p role="alert" className="banner error">{error}</p>}
    </>
  );
}
