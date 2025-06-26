# Nexus 3 on Kubernetes with Docker Registry Support

This repository contains Kubernetes manifests for deploying Sonatype Nexus 3 as a private Docker registry on your cluster, as well as automation for secure CI/CD using GitHub Actions and Trivy vulnerability scanning.

---

## Table of Contents

- [Prerequisites](#prerequisites)
- [Kubernetes Deployment](#kubernetes-deployment)
- [Initial Nexus Setup](#initial-nexus-setup)
- [Configuring Docker with Rancher Desktop (Windows)](#configuring-docker-with-rancher-desktop-windows)
- [GitHub Actions CI/CD Integration](#github-actions-cicd-integration)
    - [Workflow Prerequisites](#workflow-prerequisites)
    - [How the Workflow Works](#how-the-workflow-works)
    - [Workflow Steps Explained](#workflow-steps-explained)
- [Using the `scan-and-push.sh` Script](#using-the-scan-and-pushsh-script)
- [Troubleshooting](#troubleshooting)
- [References](#references)

---

## Prerequisites

- **Kubernetes cluster:** minikube, Rancher Desktop, or cloud provider
- **kubectl** installed and configured
- **[Rancher Desktop](https://rancherdesktop.io/)** (if running locally on Windows)
- **Docker** (with access to your Kubernetes cluster)
- **Maven** for Java build
- **Trivy** for image scanning (CLI or via GitHub Actions)
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
3. **Create a Docker (hosted) repository:**
    - Go to **Settings → Repositories → Create repository**
    - Select **docker (hosted)**
    - Set a name (e.g., `myrepo`)
    - Ensure the HTTP port matches your service (default: 5000)
    - Save

---

## Configuring Docker with Rancher Desktop (Windows)

If using **Rancher Desktop** on Windows, configure Docker to trust your local Nexus registry (as an "insecure" registry):

1. Open a terminal and run:
    ```sh
    wsl -d rancher-desktop
    ```
2. Edit `/etc/docker/daemon.json`, adding your Nexus NodePort registry (e.g., `192.168.127.2:30500`):
    ```json
    {
      "insecure-registries": ["192.168.127.2:30500"]
    }
    ```
3. Restart Rancher Desktop.

---

## GitHub Actions CI/CD Integration

This repository includes a GitHub Actions workflow to automate CI/CD with secure image scanning **before** pushing to your Nexus registry.

### Workflow Prerequisites

- **GitHub Secrets:**  
  In your repository, go to **Settings → Secrets and variables → Actions** and add:
    - `NEXUS_USERNAME`: Nexus user with permission to push images
    - `NEXUS_PASSWORD`: Password for the above user

- **Self-hosted Runner:**  
  Because Windows with Rancher Desktop's Docker engine is not available in GitHub-hosted runners, you need to:
    1. [Download and configure a GitHub Actions runner](https://docs.github.com/en/actions/hosting-your-own-runners/adding-self-hosted-runners).
    2. **Install the runner as your regular (interactive) user** (not as `Administrator`, `Local System`, or `Network Service`).
        - Do **not** run the runner as a service unless you know how to grant Docker pipe access.
    3. Start Rancher Desktop as your user and ensure `docker version` works in the same terminal as the runner.
    4. From that terminal, start the runner with `run.cmd`.

---

### How the Workflow Works

The workflow is triggered manually with parameters:
- Nexus repository URL (e.g., `192.168.127.2:30500/myrepo`)
- Docker image name
- Docker image tag

The workflow will:
1. **Checkout code**
2. **Set up JDK**
3. **Build Java app with Maven**
4. **Set up Docker Buildx**
5. **Extract the Docker registry hostname and port**
6. **Log in to the Nexus Docker registry**
7. **Build the Docker image (PowerShell variable interpolation is used for Windows)**
8. **Download and run Trivy CLI to scan the image**
    - The image is blocked from being pushed if critical/high vulnerabilities are found.
9. **Push Docker image to Nexus**

---

### Workflow Steps Explained

- **Parameter Inputs:**  
  Allows you to specify the Nexus registry URL, image name, and tag when triggering the workflow.

- **Secrets Management:**  
  Docker registry credentials are securely accessed via GitHub secrets.

- **Self-hosted Runner:**  
  Ensures the workflow runs in your environment with access to the correct Docker daemon (Rancher Desktop).

- **Trivy Scanning:**  
  The workflow downloads Trivy and scans the built image. If any critical/high vulnerabilities are found, the workflow fails and the image is **not pushed**.

- **Push on Success:**  
  The image is only pushed if all previous steps succeed.

- **PowerShell Adaptation:**  
  All shell commands needing environment variable interpolation are written for PowerShell compatibility on Windows.

---

## Using the `scan-and-push.sh` Script

This script automates building, scanning, and pushing from your local machine.  
See details in the [original instructions above](#using-the-scan-and-pushsh-script).

---

## Troubleshooting

- **Image push fails / `no basic auth credentials`:**
    - Make sure you are logged in to the registry (`docker login <registry-host:port>`) and your Docker daemon trusts the registry.
- **Trivy not found:**
    - The workflow downloads and runs Trivy CLI automatically. For local use, install [Trivy](https://aquasecurity.github.io/trivy/).
- **Runner cannot access Docker:**
    - The runner must be started by your regular user (not as a service), and Rancher Desktop must be running in that user session.
- **GitHub workflow fails at Bash-specific steps:**
    - All steps are adapted for PowerShell—do not use Bash syntax in Windows runner steps.

---

## References

- [Nexus 3 Documentation](https://help.sonatype.com/repomanager3)
- [Trivy Documentation](https://aquasecurity.github.io/trivy/)
- [Rancher Desktop Docs](https://docs.rancherdesktop.io/)
- [GitHub Actions Self-Hosted Runners](https://docs.github.com/en/actions/hosting-your-own-runners/adding-self-hosted-runners)

---

## Additional Notes

- For advanced workflow customization or multi-stage builds, refer to the `.github/workflows/docker-nexus.yml` file.
- If you wish to document CI/CD specifics in more detail, consider adding a `README.md` inside `.github/workflows/`.
