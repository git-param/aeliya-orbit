# Existing account password management

- Admins can search and reset `ROLE_USER` account passwords and their own password. Super Admins can search and reset every account, including other Super Admin accounts. Admins cannot see other Admin or Super Admin accounts here.
- The reset endpoint hashes the new password with the configured password encoder and writes `password_change_audit` with the actor, target, and timestamp in the same transaction. Passwords and hashes are not written to the audit. Password-bearing request types redact their `toString()` output.
- Login stores a server-side password hash snapshot in the session because Spring Security erases the password from the authenticated principal. The session filter compares that snapshot with the current account hash. A mismatch invalidates that session on its next request. Resetting one's own password signs the browser out.
- Flyway V10 changes both `profile_focus_x` and `profile_focus_y` to `INTEGER`, matching the manual SQL already run. V9 stays unchanged so its Flyway checksum remains valid. The type change is safe if either or both columns are already `INTEGER`.
