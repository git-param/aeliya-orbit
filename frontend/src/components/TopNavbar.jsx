// Persistent top bar. Title follows the current route; everything else stays put.
import React, { useEffect, useState } from "react";
import { Link, useLocation } from "react-router-dom";
import { profileApi } from "../api/client";
import { Icon, Button, Heading, initials } from "./UI.jsx";
import "../modules/dashboard/Dashboard.css";

const pages = {
  "/dashboard": { title: "Overview", subtitle: () => new Intl.DateTimeFormat("en-IN", { dateStyle: "full" }).format(new Date()) },
  "/employees": { title: "Employee Directory", subtitle: () => "Profiles and employment details" },
  "/profile": { title: "My Profile", subtitle: () => "Personal details and documents" },
  "/account-passwords": { title: "Account Passwords", subtitle: () => "Reset existing account passwords" },
  "/organization-setup": { title: "Organization Setup", subtitle: () => "Departments, designations, and levels" },
};

export default function TopNavbar({ darkMode, setDarkMode, onLogout, currentUser }) {
  const { pathname } = useLocation();
  const [error, setError] = useState("");
  const [profile, setProfile] = useState(null);
  const [photoVersion, setPhotoVersion] = useState(0);
  useEffect(() => {
    let active = true;
    const refresh = () => profileApi.get().then(result => { if (active) { setProfile(result); setPhotoVersion(Date.now()); } }).catch(() => {});
    refresh();
    window.addEventListener('profile-updated', refresh);
    return () => { active = false; window.removeEventListener('profile-updated', refresh); };
  }, []);
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
          <Link className="profile" to="/profile" aria-label="Open my profile">
            <div className="avatar">{initials(profile?.fullName || username)}{profile?.pictureUploaded && <img key={photoVersion} src={`${profile.pictureUrl}?v=${photoVersion}`} alt="" style={{objectPosition: `${profile.focusX}% ${profile.focusY}%`}} onError={e => { e.currentTarget.style.display = 'none'; }} />}</div>
            <div className="profile-copy">
              <strong>{profile?.fullName || username}</strong>
              <span>{role}</span>
            </div>
          </Link>
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
