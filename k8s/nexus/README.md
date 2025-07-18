# Nexus 3 on Kubernetes with Docker Registry Support

This repository contains Kubernetes manifests for deploying Sonatype Nexus 3 as a private Docker registry on your cluster, as well as automation for secure CI/CD using GitHub Actions and Trivy vulnerability scanning.

---

## Table of Contents

- [Prerequisites](#prerequisites)
- [Kubernetes Deployment](#kubernetes-deployment)
- [Initial Nexus Setup](#initial-nexus-setup)
- [Nexus Roles, Users, and Privileges](#nexus-roles-users-and-privileges)
- [Configuring Docker with Rancher Desktop (Windows)](#configuring-docker-with-rancher-desktop-windows)
- [GitHub Actions CI/CD Integration](#github-actions-cicd-integration)
    - [Workflow Prerequisites](#workflow-prerequisites)
    - [Available Workflows](#available-workflows)
    - [Flow Use Cases](#flow-use-cases)
    - [Workflow Steps Explained](#workflow-steps-explained)
    - [Trivy Dependency (SCA) Scanning](#trivy-dependency-sca-scanning)
    - [Trivy Database Sharing and Versioning](#trivy-database-sharing-and-versioning)
- [Troubleshooting](#troubleshooting)
- [References](#references)

---

## Prerequisites

- **Kubernetes cluster**: minikube, Rancher Desktop, or cloud provider
- **kubectl** installed and configured
- **[Rancher Desktop](https://rancherdesktop.io/)** (if running locally on Windows)
- **Docker** (with access to your Kubernetes cluster)
- **Maven** for Java builds
- **Trivy** for image and dependency scanning (via CLI or GitHub Actions)
- **GitHub account** with repository access

---

## Kubernetes Deployment

Apply the manifests in this repository to deploy Nexus 3, expose Docker and HTTP ports, and provision persistent storage.

```sh
kubectl create namespace nexus
kubectl apply -f persistentvolumeclaim.yaml
kubectl apply -f deployment.yaml
kubectl apply -f service.yaml
```

> **Note:** Adjust manifest settings as needed for your environment.

---

## Initial Nexus Setup

1. **Access Nexus UI:** Open `http://<node-ip>:30081` in your browser.
2. **Login:** Default credentials are `admin` / `admin123` (or see `/nexus-data/admin.password`).
3. **Create Docker (hosted) repositories:**
    - Go to **Settings → Repositories → Create repository**
    - Select **docker (hosted)**
    - Create two repositories:
        - `docker-app-images` (port 5002)
        - `docker-base-images` (port 5001)
    - Set the HTTP port for each repo to match your Kubernetes service NodePorts.
    - Set deployment policy to **Allow redeploy** for both.
    - Save

---

## Nexus Roles, Users, and Privileges

> **It is critical to configure Nexus roles and privileges correctly for secure CI/CD.**

### **Roles**

#### `docker-admins`
For administrators; full access to both repos.

- `nx-repository-view-docker-docker-app-images-*`
- `nx-repository-view-docker-docker-base-images-*`

#### `docker-devs`
For CI/CD bots and developers; push access to app images, read-only for base images.

- `nx-repository-view-docker-docker-app-images-add`
- `nx-repository-view-docker-docker-app-images-browse`
- `nx-repository-view-docker-docker-app-images-edit`
- `nx-repository-view-docker-docker-app-images-read`
- `nx-repository-view-docker-docker-base-images-read`

### **Users**

- `ci-admin` — assigned to `docker-admins`
- `ci-bot` — assigned to `docker-devs`

### **Repository-to-Port Mapping**

| Repository Name      | Type    | NodePort | Used By           |
|----------------------|---------|----------|-------------------|
| docker-app-images    | hosted  | 30502    | app images, CI/CD |
| docker-base-images   | hosted  | 30501    | base images only  |

---

## Configuring Docker with Rancher Desktop (Windows)

If using **Rancher Desktop** on Windows, configure Docker to trust your local Nexus registry as an "insecure" registry:

1. Open a terminal and run:
    ```sh
    wsl -d rancher-desktop
    ```
2. Edit `/etc/docker/daemon.json`, adding your Nexus NodePort registries (e.g., `192.168.127.2:30501`, `192.168.127.2:30502`):
    ```json
    {
      "insecure-registries": ["192.168.127.2:30501", "192.168.127.2:30502"]
    }
    ```
3. Restart Rancher Desktop.

---

## GitHub Actions CI/CD Integration

This repository includes three GitHub Actions workflows for CI/CD scenarios, all with Trivy dependency and image scanning before push.

### 🗂 **Workflow Enhancements**

#### **Trivy CLI Caching (applies to all workflows)**

All workflows now use a persistent Trivy CLI cache stored on the self-hosted runner (by default at `C:/trivy-cache/<version>`).
- **On the first run:** Trivy is downloaded and cached per version.
- **On subsequent runs:** The cached Trivy binary is reused, speeding up workflow execution.

No manual steps are required; the workflow will create and manage the cache automatically.  
**All workflows are now standardized to use Trivy CLI version `0.64.0` (except Admin, which allows per-run version selection).**  
If you previously used a different version in any workflow, this has been unified for consistency.

#### **Trivy Dependency (SCA) Scanning (NEW, applies to all workflows)**

All workflows now include a Trivy Software Composition Analysis (SCA) step to scan Maven dependencies (`pom.xml`) before building and pushing Docker images.
- **Any detected CRITICAL or HIGH vulnerabilities in dependencies will block the push to Nexus** (unless explicitly overridden in the Admin workflow).
- This provides an extra layer of security by ensuring vulnerable dependencies are not shipped, not just vulnerable images.

#### **Vulnerability Push Override and Trivy Version Selection (Admin workflow only)**

The **Admin - Build, Scan, and Push Docker Image to Nexus** workflow now supports the following **additional inputs**:

- `allow_push_on_vulnerabilities`: If set to `true`, allows admins to push Docker images **even when Trivy finds CRITICAL or HIGH vulnerabilities** in either dependencies or image.
    - **Default:** `false` (push is blocked if vulnerabilities found)
    - **Use with caution:** This should only be enabled by trusted users and is logged in the workflow run for auditability.

- `trivy_version`: Allows admins to specify which version of the Trivy CLI to use for the scan.
    - **Default:** `0.64.0`
    - **Benefit:** Admins can test/roll back Trivy versions per-run without editing the workflow file. Each version is cached in its own subdirectory for efficiency.

**These two inputs apply only to the Admin workflow**.  
The other workflows use the default Trivy version (`0.64.0`) and do not allow push overrides.

---

### Workflow Prerequisites

- **GitHub Secrets:**  
  In your repository, go to **Settings → Secrets and variables → Actions** and add:
    - `NEXUS_ADMIN_USERNAME`, `NEXUS_ADMIN_PASSWORD` — for admin automation
    - `NEXUS_CI_BOT_USERNAME`, `NEXUS_CI_BOT_PASSWORD` — for CI/CD bot automation

- **Self-hosted Runner:**  
  Because Windows with Rancher Desktop's Docker engine is not available in GitHub-hosted runners, you need to:
    1. [Download and configure a GitHub Actions runner](https://docs.github.com/en/actions/hosting-your-own-runners/adding-self-hosted-runners).
    2. **Install the runner as your regular (interactive) user** (not as `Administrator`, `Local System`, or `Network Service`).
    3. Start Rancher Desktop as your user and ensure `docker version` works in the same terminal as the runner.
    4. From that terminal, start the runner with `run.cmd`.

---

### Available Workflows

#### **1. Admin - Build, Scan, and Push Docker Image to Nexus**

- **User:** `ci-admin` (full privileges)
- **Can push to:** both `docker-app-images` (30502) and `docker-base-images` (30501)
- **Secrets:** `NEXUS_ADMIN_USERNAME`, `NEXUS_ADMIN_PASSWORD`
- **Trivy cache:** Used for fast Trivy CLI startup
- **Dependency Scanning:** Trivy SCA step scans Maven dependencies for vulnerabilities before push.
- **Additional inputs:**
    - **`allow_push_on_vulnerabilities`:** Set to `true` to allow pushing images with vulnerabilities (use with caution).
    - **`trivy_version`:** Select Trivy CLI version per run (default: `0.64.0`).

#### **2. CI/CD - Build, Scan, and Push Docker Image to Nexus**

- **User:** `ci-bot` (developer privileges)
- **Can push to:** `docker-app-images` (30502) only
- **Secrets:** `NEXUS_CI_BOT_USERNAME`, `NEXUS_CI_BOT_PASSWORD`
- **Cannot push to:** `docker-base-images` (30501) — will receive unauthorized error (by design)
- **Trivy cache:** Used for fast Trivy CLI startup
- **Dependency Scanning:** Trivy SCA step scans Maven dependencies for vulnerabilities before push.
- **Uses Trivy CLI version:** `0.64.0` (standardized across all workflows)

#### **3. Manual - Build, Scan, and Push Docker Image to Nexus (Per-user Login)**

- **User:** Prompts for Nexus username and password as workflow inputs
- **Use case:** Ad-hoc, testing, onboarding, or per-user auditing
- **Can push to:** whichever repo the credentials permit
- **Trivy cache:** Used for fast Trivy CLI startup
- **Dependency Scanning:** Trivy SCA step scans Maven dependencies for vulnerabilities before push.
- **Uses Trivy CLI version:** `0.64.0` (standardized across all workflows)

---

### Flow Use Cases

| Workflow    | Intended User      | Push Target(s)        | Use Case                                                            |
|-------------|-------------------|-----------------------|---------------------------------------------------------------------|
| Admin       | ci-admin          | base & app images     | Admin/maintenance, base image updates                               |
| CI/CD       | ci-bot            | app images only       | Automated builds, production CI/CD                                  |
| Manual      | Any user          | app or base images    | Manual pushes, onboarding, troubleshooting, per-user auditing       |

- **ci-bot** cannot push to base images (enforced by Nexus roles).
- All workflows block the push if Trivy finds CRITICAL/HIGH vulnerabilities in dependencies or the image, **except for the Admin workflow if `allow_push_on_vulnerabilities` is set to `true`**.

---

### Workflow Steps Explained

- **Parameter Inputs:**  
  Each workflow allows you to specify the Nexus registry URL, image name, and tag (and, for manual flow, Nexus credentials).  
  **Admin workflow** also allows you to set `allow_push_on_vulnerabilities` and `trivy_version`.

- **Secrets Management:**  
  Docker registry credentials are securely accessed via GitHub secrets (or as workflow input for manual flow).

- **Self-hosted Runner:**  
  Ensures the workflow runs in your environment with access to Rancher Desktop's Docker daemon.

- **Trivy Dependency (SCA) Scanning:**  
  The workflow downloads Trivy to a persistent cache (`C:/trivy-cache/<version>`) and scans the Maven project directory for vulnerable dependencies before building the Docker image.
    - If any critical/high vulnerabilities are found in dependencies, the workflow fails and the image is **not pushed** (unless explicitly overridden in the Admin workflow).

- **Trivy Image Scanning:**  
  After building the Docker image, Trivy scans the image for vulnerabilities. Critical/high vulnerabilities block the push unless overridden in the Admin workflow.

- **Push on Success:**  
  The image is only pushed if all previous steps succeed (or if override is enabled in Admin workflow).

- **PowerShell Adaptation:**  
  All shell commands needing environment variable interpolation are written for PowerShell compatibility on Windows.

---

### Trivy Dependency (SCA) Scanning

Trivy SCA (Software Composition Analysis) scanning is integrated in all workflows:
- **Scans the Maven project (`pom.xml`) for known-vulnerable dependencies using Trivy's `fs` scanner.**
- **Blocks the image push if critical/high vulnerabilities are found,** except in the Admin workflow if the override flag is used.

**Why?**  
This ensures you do not ship Docker images that contain known-vulnerable Java dependencies, adding an extra layer of security to your supply chain.

**Sample step (in PowerShell):**
```powershell
& "$trivyCacheDir\trivy.exe" fs --scanners vuln --severity CRITICAL,HIGH --exit-code 1 --format table .
```

---

### Trivy Database Sharing and Versioning

Trivy downloads and caches a vulnerability database (`db`) in the directory specified by the `TRIVY_CACHE_DIR` environment variable (e.g., `C:/trivy-cache/0.64.0/db`).

#### **Shared vs. Per-Version DB**

- **By default, all Trivy versions will use the same DB if `TRIVY_CACHE_DIR` is shared** (e.g., `C:/trivy-cache`).
- **In these workflows, each Trivy version is cached in its own subdirectory** (e.g., `C:/trivy-cache/0.64.0`), minimizing conflicts and keeping things clean.

#### **Why does this matter?**

If you run multiple workflows simultaneously with different Trivy versions **and use a shared DB**, you may experience:
- Database corruption
- Repeated re-downloads
- Scan failures due to DB schema incompatibility

**Best Practice:**  
Each workflow uses a version-specific cache directory (`C:/trivy-cache/<version>`) to avoid conflicts and ensure reliable, parallel execution even if different Trivy versions are in use.

**If you consistently use only one Trivy version and never run concurrent jobs with different versions, a shared DB is fine. For most CI/CD environments, per-version caching is safer.**

---

## Troubleshooting

- **Image push fails / `no basic auth credentials`:**
    - Make sure you are logged in to the registry (`docker login <registry-host:port>`) and your Docker daemon trusts the registry.
    - Ensure the correct Nexus user/role is used for the correct repo/port.
- **Trivy not found:**
    - The workflow downloads and caches Trivy CLI automatically. For local use, install [Trivy](https://aquasecurity.github.io/trivy/).
- **Trivy scan fails or reports DB errors:**
    - If running multiple Trivy versions in parallel with a shared cache, use per-version cache directories to avoid DB conflicts.
- **Runner cannot access Docker:**
    - The runner must be started by your regular user (not as a service), and Rancher Desktop must be running in that user session.
- **GitHub workflow fails at Bash-specific steps:**
    - All steps are adapted for PowerShell—do not use Bash syntax in Windows runner steps.
- **Unauthorized on push to 30501 with ci-bot:**
    - This is expected if the user/role doesn't have `add`/`edit` on `docker-base-images`.

---

## References

- [Nexus 3 Documentation](https://help.sonatype.com/repomanager3)
- [Trivy Documentation](https://aquasecurity.github.io/trivy/)
- [Rancher Desktop Docs](https://docs.rancherdesktop.io/)
- [GitHub Actions Self-Hosted Runners](https://docs.github.com/en/actions/hosting-your-own-runners/adding-self-hosted-runners)

---

## Additional Notes

- For advanced workflow customization or multi-stage builds, refer to the `.github/workflows/` directory.
- All flows are compatible with Windows self-hosted runners using PowerShell and Rancher Desktop Docker engine.
