// Top navigation bar component
import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Icon, Button, Heading } from "../UI.jsx";
import "../modules/dashboard/Dashboard.css"; // shared styles

export default function TopNavbar({ darkMode, setDarkMode, onLogout }) {
  const [darkMode, setDarkMode] = useState(false);
  const navigate = useNavigate();

  // Update the document color scheme when darkMode changes
  React.useEffect(() => {
    document.documentElement.style.colorScheme = darkMode ? "dark" : "light";
  }, [darkMode]);

  const handleLogout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("role");
    if (onLogout) onLogout();
    navigate("/login", { replace: true });
  };

  // Retrieve current role for display
  const rawRole = localStorage.getItem("role") || "USER";
  const currentRole = rawRole.replace("ROLE_", "");

  return (
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
            onClick={() => setDarkMode((v) => !v)}
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
  );
}
