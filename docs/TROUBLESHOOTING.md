# ProjectSync AI — Comprehensive Troubleshooting Guide

This guide provides tested, actionable solutions for common issues encountered during setup, local development, and execution on Windows systems.

---

## 1. Java & Backend Build Issues

### Issue 1.1: `UnsupportedClassVersionError: ... has been compiled by a more recent version of the Java Runtime`
- **Symptom**: Spring Boot crashes immediately on boot with `Unsupported class file major version 65`.
- **Likely Cause**: Your environment is running an older Java version (such as Java 17 or Java 11). ProjectSync AI requires Java 21 (class file version 65).
- **Diagnostic Command**:
  ```powershell
  java -version
  ```
- **Safe Fix**:
  1. Install JDK 21 from [Adoptium Eclipse Temurin 21](https://adoptium.net/temurin/releases/?version=21).
  2. Set `JAVA_HOME` in your active PowerShell session:
     ```powershell
     $env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.4.7-hotspot"
     $env:Path = "$env:JAVA_HOME\bin;" + $env:Path
     ```
  3. Re-run `java -version` to confirm version `21.x.x`.

---

### Issue 1.2: `mvnw.cmd : Execution of scripts is disabled on this system`
- **Symptom**: PowerShell outputs `File ... cannot be loaded because running scripts is disabled on this system`.
- **Likely Cause**: Windows PowerShell default execution policy blocks unsigned `.ps1` and script wrappers.
- **Diagnostic Command**:
  ```powershell
  Get-ExecutionPolicy
  ```
  *(Returns `Restricted`)*
- **Safe Fix**:
  Allow process-level script execution without modifying system security globally:
  ```powershell
  Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
  ```
  Then re-run `.\mvnw.cmd spring-boot:run`.

---

### Issue 1.3: Maven Dependency Download Failure or Timeout
- **Symptom**: `Could not transfer artifact ... from/to central (https://repo.maven.apache.org/maven2): Connection timed out`.
- **Likely Cause**: Unstable internet connection, university proxy, or corrupted partial downloads in the local Maven cache (`~/.m2/repository`).
- **Diagnostic Command**:
  ```powershell
  Test-NetConnection -ComputerName repo.maven.apache.org -Port 443
  ```
- **Safe Fix**:
  Force Maven to update dependencies and clean failed cached artifacts:
  ```powershell
  .\mvnw.cmd clean compile -U
  ```

---

## 2. Port & Network Conflicts

### Issue 2.1: `Web server failed to start. Port 8081 was already in use.`
- **Symptom**: Backend terminates with `Port 8081 was already in use`.
- **Likely Cause**: A previous Spring Boot instance is still running in the background or another application is using port 8081.
- **Diagnostic Command**:
  ```powershell
  Get-NetTCPConnection -LocalPort 8081 -ErrorAction SilentlyContinue | Select-Object LocalPort, OwningProcess, State
  ```
- **Safe Fix**:
  Terminate the process holding port 8081:
  ```powershell
  $conn = Get-NetTCPConnection -LocalPort 8081 -ErrorAction SilentlyContinue
  if ($conn) {
      Stop-Process -Id $conn.OwningProcess -Force
      Write-Host "Process terminated successfully."
  }
  ```

---

### Issue 2.2: `Port 5173 is in use, trying another one...`
- **Symptom**: Vite starts on port `5174` instead of `5173`.
- **Likely Cause**: A previous `npm run dev` session is still running in a background terminal.
- **Diagnostic Command**:
  ```powershell
  Get-NetTCPConnection -LocalPort 5173 -ErrorAction SilentlyContinue
  ```
- **Safe Fix**:
  Either kill the previous process using `Stop-Process` or simply open your browser at the new port reported in the Vite console (e.g., `http://localhost:5174`). If using 5174, the backend CORS pattern `http://localhost:*` will continue to permit requests automatically.

---

## 3. Database Connectivity

### Issue 3.1: `FATAL: password authentication failed for user "postgres.htwzpyouqnkfnxuvbsot"`
- **Symptom**: Application fails on boot with `PSQLException: FATAL: password authentication failed`.
- **Likely Cause**: The `SUPABASE_DB_PASSWORD` environment variable has not been supplied or contains typos.
- **Diagnostic Command**:
  ```powershell
  $env:SUPABASE_DB_PASSWORD
  ```
- **Safe Fix**:
  1. Set the correct password:
     ```powershell
     $env:SUPABASE_DB_PASSWORD = "your-team-password"
     ```
  2. Or run in offline standalone mode without needing any password:
     ```powershell
     .\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local
     ```

---

### Issue 3.2: `Connection to aws-0-ap-south-1.pooler.supabase.com:5432 refused or timed out`
- **Symptom**: `Communications link failure` after 30 seconds.
- **Likely Cause**: Campus Wi-Fi or firewall blocking outbound TCP port 5432.
- **Diagnostic Command**:
  ```powershell
  Test-NetConnection -ComputerName aws-0-ap-south-1.pooler.supabase.com -Port 5432
  ```
- **Safe Fix**:
  - Connect your laptop to a mobile hotspot or home network.
  - Or switch to the local H2 profile (`-Dspring-boot.run.profiles=local`).

---

## 4. Frontend & API Communication

### Issue 4.1: `ERR_CONNECTION_REFUSED` in Browser Console
- **Symptom**: Frontend displays `Network Error` when attempting to log in or load projects.
- **Likely Cause**: The Spring Boot backend on port 8081 is not running.
- **Diagnostic Command**:
  In PowerShell, run:
  ```powershell
  Invoke-RestMethod http://localhost:8081/api/vcs/health
  ```
- **Safe Fix**:
  Start the backend in Terminal 1 and wait for the message `Started BackendApplication` before making requests from the frontend.

---

### Issue 4.2: CORS Blocked: `Access-Control-Allow-Origin header is missing`
- **Symptom**: Browser console logs `CORS policy: No 'Access-Control-Allow-Origin' header is present on the requested resource`.
- **Likely Cause**: Request origin does not match the configured CORS patterns in `SecurityConfig.java`.
- **Verification**:
  `SecurityConfig.java` allows `http://localhost:*` and `http://127.0.0.1:*`.
- **Safe Fix**:
  Always access the frontend via `http://localhost:5173` rather than a raw IP or custom domain.

---

### Issue 4.3: `401 Unauthorized` on All Protected Endpoints
- **Symptom**: Immediate redirection to `/login` when clicking dashboard links.
- **Likely Cause**: JWT token expired or `jwt.secret` changed after the token was issued.
- **Safe Fix**:
  1. In your browser, open Developer Tools (`F12`) -> **Application** -> **Session Storage** -> `http://localhost:5173`.
  2. Right-click and delete `projectsync_auth`.
  3. Re-login at `/login` to obtain a fresh token.

---

## 5. Gemini AI & VCS Issues

### Issue 5.1: `IllegalStateException: GEMINI_API_KEY is not configured.`
- **Symptom**: Clicking "Draft Proposal with AI" or "AI Code Inspection" displays a server error.
- **Likely Cause**: The optional `GEMINI_API_KEY` has not been defined in your environment.
- **Diagnostic Command**:
  ```powershell
  $env:GEMINI_API_KEY
  ```
- **Safe Fix**:
  Obtain a free API key from [Google AI Studio](https://aistudio.google.com/) and set it in your PowerShell session:
  ```powershell
  $env:GEMINI_API_KEY = "your-gemini-key"
  ```
  Then restart the backend.

---

### Issue 5.2: JGit File Lock Error on Windows (`File cannot be deleted or accessed by another process`)
- **Symptom**: Git operations fail with `Cannot delete file ... .git/index.lock`.
- **Likely Cause**: Windows file indexing or antivirus locking JGit index files.
- **Safe Fix**:
  1. `GitService.java` is already configured with `WindowCacheConfig.setPackedGitMMAP(false)` to prevent file handles from persisting.
  2. If a manual lock exists, navigate to your repository store (`$env:USERPROFILE\.projectsync\repositories\<repo-name>\.git\`) and delete any leftover `index.lock` file.
