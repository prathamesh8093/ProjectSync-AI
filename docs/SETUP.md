# ProjectSync AI — Complete Windows Setup & Developer Guide

This guide is designed for teammates setting up and running **ProjectSync AI** on a new Windows computer from scratch. Follow each step sequentially.

---

## Step A — Install Prerequisites

Before downloading or running the project code, ensure your computer has the four core tools installed.

### 1. Java Development Kit (JDK 21)
The backend requires **Java 21 (LTS)**.
- **Official Download Link**: [Eclipse Temurin 21 (Adoptium)](https://adoptium.net/temurin/releases/?version=21)
- **Installer Type**: Windows `.msi` (x64)
- **Crucial Setup Step**: During installation, on the "Custom Setup" step, make sure to select:
  - *"Set JAVA_HOME variable"* -> Select **Will be installed on local hard drive**
  - *"Add to PATH"* -> Select **Will be installed on local hard drive**
- **Verification Command** (Run in PowerShell):
  ```powershell
  java -version
  ```
- **Expected Output**:
  ```text
  openjdk version "21.0.x" ...
  OpenJDK Runtime Environment Temurin ...
  ```

### 2. Node.js and npm
The frontend is built with React 18 and Vite, requiring Node.js.
- **Official Download Link**: [Node.js Official Website (LTS Version)](https://nodejs.org/)
- **Installer Type**: Windows `.msi` (x64, Version 20.x or 22.x LTS)
- **Verification Commands** (Run in PowerShell):
  ```powershell
  node --version
  npm --version
  ```
- **Expected Output**:
  ```text
  v20.x.x (or v22.x.x)
  10.x.x (or higher)
  ```

### 3. Git
Required for version control and cloning the repository.
- **Official Download Link**: [Git for Windows](https://git-scm.com/download/win)
- **Verification Command**:
  ```powershell
  git --version
  ```
- **Expected Output**:
  ```text
  git version 2.4x.x.windows.1
  ```

### What if a command is not recognized?
If PowerShell displays `The term 'java' is not recognized as the name of a cmdlet, function, script file, or operable program`:
1. Close all open PowerShell and terminal windows.
2. Search Windows for **"Edit the system environment variables"** and open it.
3. Click **Environment Variables**.
4. Check under **System variables**:
   - Ensure `JAVA_HOME` points to `C:\Program Files\Eclipse Adoptium\jdk-21.x.x-hotspot\`.
   - Ensure `Path` includes `%JAVA_HOME%\bin` and `C:\Program Files\nodejs\`.
5. Re-open a fresh PowerShell window and re-test.

---

## Step B — Clone the Correct Branch

1. Open PowerShell and navigate to your preferred workspace folder (e.g., your Desktop or Projects folder):
   ```powershell
   cd C:\Users\$env:USERNAME\Desktop
   ```
2. Clone the team repository:
   ```powershell
   git clone https://github.com/prathamesh8093/ProjectSync-AI.git
   ```
3. Enter the project directory:
   ```powershell
   cd ProjectSync-AI
   ```
4. Fetch all remote branches:
   ```powershell
   git fetch --all
   ```
5. Switch to the team feature branch:
   ```powershell
   git checkout feature/project-upload
   ```
6. Confirm your active branch:
   ```powershell
   git branch
   ```
   *You should see `* feature/project-upload` in green.*

> [!IMPORTANT]
> **Do not push directly to the `main` branch.** All changes must be developed in personal feature branches and submitted via Pull Requests.

---

## Step C — Configure the Backend

### About the Maven Wrapper (`mvnw.cmd`)
You **do not** need to install Apache Maven manually. The repository includes `mvnw.cmd` (Maven Wrapper for Windows) and `.mvn/wrapper/maven-wrapper.properties`. When you run `.\mvnw.cmd`, it automatically downloads and invokes the exact required Maven runtime.

### Configuration Files
Backend configuration files are located in:
`backend/src/main/resources/`
- `application.properties`: Primary configuration file with environment variable hooks.
- `application-local.properties`: Standalone offline profile with embedded H2 database.
- `application-supabase.properties`: Shared team database profile.

### Creating your Local Configuration
Copy the template file to create your local `.env`:
```powershell
# From project root
Copy-Item .env.example backend\.env
```

---

## Step D — Configure the Database

ProjectSync AI offers two database options. Choose the one that matches your environment:

### Option 1: Standalone Offline Mode (Zero Setup - Recommended for First Run)
If you want to run and test immediately without configuring Supabase:
- Run the backend with `-Dspring-boot.run.profiles=local`.
- The application will automatically create an embedded H2 database at `backend/data/projectsync-local-db`.
- Hibernate will automatically create all 18 tables on boot (`spring.jpa.hibernate.ddl-auto=update`).
- The application automatically seeds sample accounts:
  - **Student**: `student@projectsync.local` | `Password123!`
  - **Faculty**: `faculty@projectsync.local` | `Password123!`
- The embedded H2 web console is enabled at `http://localhost:8081/h2-console`.

### Option 2: Shared Supabase Cloud Database (Team Collaboration Mode)
To collaborate with real shared team data:
1. Contact your team lead (`prathamesh8093` or `ankishaKshah18`) to obtain the `SUPABASE_DB_PASSWORD`.
2. Set the environment variable in your PowerShell session:
   ```powershell
   $env:SUPABASE_DB_PASSWORD="your-team-db-password"
   ```
3. In this mode, the backend connects to the PostgreSQL pooler at `aws-0-ap-south-1.pooler.supabase.com:5432/postgres`.
4. Hibernate validation mode (`ddl-auto=none` / `validate`) ensures no existing team tables or rows are accidentally dropped or modified.

> [!CAUTION]
> When using the shared Supabase database, **never drop tables, alter schema definitions, or delete other members' data.**

---

## Step E — Configure External Services

### 1. Google Gemini AI (Optional)
The application integrates with Gemini AI for automated project proposal drafting, sprint task breakdown, standup summarization, code quality inspection, and rubric-based grading.
- If you have an API key, set it before starting the backend:
  ```powershell
  $env:GEMINI_API_KEY="your-gemini-key"
  ```
- **Fallback Behavior**: If `GEMINI_API_KEY` is not provided, the entire core platform (Kanban, Sprints, Git VCS, Profiles, Authentication) continues to work normally. Only AI generation endpoints will return a clean notice asking for an API key.

### 2. Supabase Storage (Optional)
Used for uploading and downloading project attachments.
- By default, `application.properties` connects to bucket `project-files`.
- If unconfigured, the file upload service will notify the user with a configuration warning, without affecting other features.

---

## Step F — Start the Backend

1. Open PowerShell and navigate to the `backend` directory:
   ```powershell
   cd C:\path\to\ProjectSync-AI\backend
   ```
2. Run the application:
   - **For Local Offline Mode (H2)**:
     ```powershell
     .\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local
     ```
   - **For Supabase Cloud Mode**:
     ```powershell
     $env:SUPABASE_DB_PASSWORD="your-supabase-password"
     .\mvnw.cmd spring-boot:run
     ```
3. Wait for the startup banner and confirmation log:
   ```text
   2026-10-20 ... INFO ... : Tomcat started on port 8081 (http) with context path '/'
   2026-10-20 ... INFO ... : Started BackendApplication in 5.432 seconds
   ```
4. **Verify Health**:
   Open a browser or run in PowerShell:
   ```powershell
   Invoke-RestMethod http://localhost:8081/api/vcs/health
   ```
   Output: `{"status":"UP", "storageMode":"local", ...}`

---

## Step G — Start the Frontend

1. Open a **second** PowerShell window (keep the backend running in the first window).
2. Navigate to the `frontend` directory:
   ```powershell
   cd C:\path\to\ProjectSync-AI\frontend
   ```
3. Install the dependencies using the lockfile:
   ```powershell
   npm install
   ```
4. Start the Vite development server:
   ```powershell
   npm run dev
   ```
5. Vite will display the local development URL:
   ```text
     VITE v5.4.0  ready in 280 ms

     ➜  Local:   http://localhost:5173/
   ```

---

## Step H — Open and Test the Application

1. Open Google Chrome, Edge, or Firefox and go to:
   [**http://localhost:5173**](http://localhost:5173)
2. **Log In**:
   - If running in **Local Profile**, log in using:
     - Email: `student@projectsync.local`
     - Password: `Password123!`
   - If running with **Supabase**, click **Register**, create your account with the `STUDENT` or `FACULTY` role, and log in.
3. **Smoke Test**:
   - Verify that your dashboard loads with navigation links for Dashboard, Profile, Team, Proposal, Projects, and VCS.
   - Click **VCS** in the sidebar: verify the Version Control System dashboard loads and displays repository management controls.

---

## Step I — How to Stop and Restart

### Stopping the Services
In each PowerShell terminal, press:
```text
Ctrl + C
```
Type `Y` and press Enter when prompted.

### Restarting
1. Terminal 1: Navigate to `backend` and run `.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local`
2. Terminal 2: Navigate to `frontend` and run `npm run dev`

### Resolving Port In Use Conflicts (8081 or 5173)
If a process failed to shut down and is locking port 8081 or 5173, run this in PowerShell:
```powershell
# Check for process holding port 8081
$port = Get-NetTCPConnection -LocalPort 8081 -ErrorAction SilentlyContinue
if ($port) {
    Stop-Process -Id $port.OwningProcess -Force
    Write-Host "Killed process on port 8081"
}
```
Replace `8081` with `5173` to free the frontend port if needed.
