import { useEffect, useState } from 'react';
import { profileApi } from '../../api/client';
import { initials } from '../../components/UI.jsx';
import './Profile.css';

export default function Profile() {
  const [profile, setProfile] = useState(null);
  const [address, setAddress] = useState('');
  const [focusX, setFocusX] = useState(50);
  const [focusY, setFocusY] = useState(50);
  const [phone, setPhone] = useState('');
  const [photoVersion, setPhotoVersion] = useState(0);
  const [busy, setBusy] = useState('');
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [missing, setMissing] = useState(false);

  const apply = data => {
    setProfile(data);
    setAddress(data.address || '');
    setFocusX(data.focusX);
    setFocusY(data.focusY);
  };
  useEffect(() => {
    profileApi.get().then(apply).catch(cause => {
      if (cause.status === 404) setMissing(true);
      else setError('Unable to load your profile.');
    });
  }, []);
  const save = async event => {
    event.preventDefault(); setBusy('details'); setError(''); setNotice('');
    try {
      apply(await profileApi.update(address, focusX, focusY));
      setNotice('Profile details saved.');
      window.dispatchEvent(new Event('profile-updated'));
    } catch { setError('Profile details could not be saved.'); }
    finally { setBusy(''); }
  };
  const upload = async (kind, file) => {
    if (!file) return;
    setBusy(kind); setError(''); setNotice('');
    try {
      await profileApi.upload(kind, file);
      apply(await profileApi.get());
      if (kind === 'picture') { setPhotoVersion(Date.now()); window.dispatchEvent(new Event('profile-updated')); }
      setNotice(`${kind === 'picture' ? 'Portrait' : kind === 'aadhar' ? 'Aadhaar card' : 'PAN card'} uploaded.`);
    } catch { setError('Upload failed. Use a PNG, JPEG, WebP, or PDF file. Portraits must be images.'); }
    finally { setBusy(''); }
  };
  const removePicture = async () => {
    setBusy('picture'); setError(''); setNotice('');
    try {
      await profileApi.deletePicture();
      apply(await profileApi.get());
      setPhotoVersion(Date.now());
      window.dispatchEvent(new Event('profile-updated'));
      setNotice('Portrait removed. Your initials will appear instead.');
    } catch { setError('Portrait could not be removed. Please try again.'); }
    finally { setBusy(''); }
  };
  const addPhone = async event => {
    event.preventDefault(); setBusy('phone'); setError(''); setNotice('');
    try {
      const secondaryPhones = await profileApi.addPhone(phone);
      setProfile(current => ({ ...current, secondaryPhones }));
      setPhone(''); setNotice('Secondary mobile number added.');
    } catch { setError('Could not add that mobile number. Check its format and that it is not already listed.'); }
    finally { setBusy(''); }
  };
  const deletePhone = async id => {
    setBusy('phone'); setError(''); setNotice('');
    try {
      const secondaryPhones = await profileApi.deletePhone(id);
      setProfile(current => ({ ...current, secondaryPhones }));
      setNotice('Secondary mobile number removed.');
    } catch { setError('Could not remove that mobile number.'); }
    finally { setBusy(''); }
  };

  if (missing) return <div className="profile-page"><h2>My profile</h2><p>No employee profile is linked to this account.</p></div>;
  if (!profile) return <div className="profile-page"><h2>My profile</h2>{error ? <p role="alert" className="banner error">{error}</p> : <p>Loading profile…</p>}</div>;
  return <div className="profile-page">
    <div className="profile-intro"><h2>My profile</h2><p>Manage your details and the portrait shown around Orbit.</p></div>
    {error && <p role="alert" className="banner error">{error}</p>}
    {notice && <p role="status" className="banner">{notice}</p>}
    <div className="profile-layout">
      <section className="card profile-portrait-card">
        <h3>Portrait</h3>
        <div className="profile-portrait-preview">
          <div className="profile-full-photo"><span>{initials(profile.fullName)}</span>{profile.pictureUploaded && <img key={photoVersion} src={`${profile.pictureUrl}?v=${photoVersion}`} alt="Full portrait" onError={e => { e.currentTarget.style.display = 'none'; }} />}</div>
          <div><span className="muted">Profile preview</span><div className="profile-crop-preview"><span>{initials(profile.fullName)}</span>{profile.pictureUploaded && <img key={photoVersion} src={`${profile.pictureUrl}?v=${photoVersion}`} alt="Profile crop preview" style={{objectPosition: `${focusX}% ${focusY}%`}} onError={e => { e.currentTarget.style.display = 'none'; }} />}</div></div>
        </div>
        <div className="profile-photo-actions"><label className="profile-upload">{profile.pictureUploaded ? 'Replace portrait' : 'Upload portrait'}<input type="file" accept="image/png,image/jpeg,image/webp" disabled={Boolean(busy)} onChange={e => { upload('picture', e.target.files?.[0]); e.target.value = ''; }} /></label>
          {profile.pictureUploaded && <button className="btn" type="button" disabled={Boolean(busy)} onClick={removePicture}>{busy === 'picture' ? 'Removing…' : 'Remove portrait'}</button>}
        </div>
        <p className="muted">The original portrait appears in the birthday carousel. Adjust the preview position for your profile photo.</p>
        <form onSubmit={save} className="profile-edit-form">
          <label>Horizontal position <span>{focusX}%</span><input type="range" min="0" max="100" value={focusX} onChange={e => setFocusX(Number(e.target.value))} /></label>
          <label>Vertical position <span>{focusY}%</span><input type="range" min="0" max="100" value={focusY} onChange={e => setFocusY(Number(e.target.value))} /></label>
          <label>Address<textarea value={address} maxLength={500} rows={3} onChange={e => setAddress(e.target.value)} /></label>
          <button className="btn primary" disabled={Boolean(busy)}>{busy === 'details' ? 'Saving…' : 'Save profile details'}</button>
        </form>
      </section>
      <div className="profile-side">
        <section className="card profile-info-card">
          <h3>Account and contact</h3>
          <dl><dt>Name</dt><dd>{profile.fullName}</dd><dt>Employee ID</dt><dd>{profile.employeeCode}</dd><dt>Work email</dt><dd>{profile.workEmail}</dd><dt>Primary mobile</dt><dd>{profile.primaryPhone || 'Not set by admin or HR'}</dd></dl>
          <p className="muted">Your primary mobile number is managed by admin or HR.</p>
          <h4>Secondary mobile numbers</h4>
          <ul className="profile-phone-list">{profile.secondaryPhones.map(item => <li key={item.id}><span>{item.phone}</span><button className="btn" type="button" disabled={Boolean(busy)} onClick={() => deletePhone(item.id)} aria-label={`Remove ${item.phone}`}>Remove</button></li>)}</ul>
          <form className="profile-add-phone" onSubmit={addPhone}><input type="tel" value={phone} onChange={e => setPhone(e.target.value)} placeholder="Add secondary mobile" aria-label="Secondary mobile number" required /><button className="btn" disabled={Boolean(busy)}>Add</button></form>
        </section>
        <section className="card profile-documents"><h3>Identity documents</h3><p className="muted">Only you can open these uploads from your profile.</p>
          {[["aadhar", "Aadhaar card", profile.aadhaarUploaded], ["pan", "PAN card", profile.panUploaded]].map(([kind, label, uploaded]) => <div className="profile-document" key={kind}>
            <div><strong>{label}</strong><span className="muted">{uploaded ? 'Uploaded' : 'Not uploaded'}</span></div>
            {uploaded && <a className="btn" href={`/api/profile/${kind}`} target="_blank" rel="noreferrer">View</a>}
            <label className="btn">{uploaded ? 'Replace' : 'Upload'}<input type="file" accept="image/png,image/jpeg,image/webp,application/pdf" disabled={Boolean(busy)} onChange={e => { upload(kind, e.target.files?.[0]); e.target.value = ''; }} /></label>
          </div>)}
        </section>
      </div>
    </div>
  </div>;
}
