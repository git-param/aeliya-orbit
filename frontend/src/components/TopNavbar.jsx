// Top navigation bar component
import React from "react";
import { Icon, Button, Heading } from "../UI.jsx";
import "../modules/dashboard/Dashboard.css"; // shared styles

export default function TopNavbar({ darkMode, setDarkMode, onLogout, currentUser }) {

  // Update the document color scheme when darkMode changes
  React.useEffect(() => {
    document.documentElement.style.colorScheme = darkMode ? "dark" : "light";
  }, [darkMode]);

  const currentRole = currentUser?.role?.replace("ROLE_", "") || "USER";

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
            <strong>{currentUser?.username || "Current User"}</strong>
            <span>{currentRole}</span>
          </div>
        </div>
        <Button className="logout-button" onClick={onLogout}>Logout</Button>
      </div>
    </header>
  );
}
