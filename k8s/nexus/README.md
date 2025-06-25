# Nexus 3 on Kubernetes with Docker Registry Support

This repository contains Kubernetes manifests for deploying Sonatype Nexus 3 as a private Docker registry on your cluster, as well as a helper script (`scan-and-push.sh`) to build, scan, and securely push Docker images to your Nexus registry.

---

## Table of Contents

- [Prerequisites](#prerequisites)
- [Kubernetes Deployment](#kubernetes-deployment)
- [Initial Nexus Setup](#initial-nexus-setup)
- [Configuring Docker with Rancher Desktop (Windows)](#configuring-docker-with-rancher-desktop-windows)
- [Using the `scan-and-push.sh` Script](#using-the-scan-and-pushsh-script)
    - [Script Workflow](#script-workflow)
    - [Example Usage](#example-usage)
    - [Script Details](#script-details)

---

## Prerequisites

- Kubernetes cluster (minikube, Rancher Desktop, or cloud provider)
- `kubectl` installed and configured
- [Rancher Desktop](https://rancherdesktop.io/) if running locally on Windows
- [Docker](https://www.docker.com/)
- [Maven](https://maven.apache.org/)
- [Trivy](https://aquasecurity.github.io/trivy/)
- (Optional) `wsl` for Rancher Desktop configuration on Windows

---

## Kubernetes Deployment

Apply the manifests in this repository to deploy Nexus 3, expose Docker and HTTP ports, and provision persistent storage.

> **Note:** The full content of the manifests (Deployment, Service, PersistentVolumeClaim) is in the codebase. You may adjust them to your needs.

**Deployment steps:**
```sh
kubectl create namespace nexus
kubectl apply -f persistentvolumeclaim.yaml
kubectl apply -f deployment.yaml
kubectl apply -f service.yaml
```

---

## Initial Nexus Setup

1. **Access Nexus UI:**  
   Open `http://<node-ip>:30081` in your browser (where `<node-ip>` is your node or host IP).

2. **Login:**  
   Default credentials are `admin` / `admin123` (or check `/nexus-data/admin.password`).

3. **Create a Docker (hosted) repository:**
    - Go to **Settings → Repositories → Create repository**
    - Select **docker (hosted)**
    - Set a name (e.g., `myrepo`)
    - Make sure the HTTP port matches the Docker port exposed (default: 5000)
    - Save the repository

---

## Configuring Docker with Rancher Desktop (Windows)

If you are using **Rancher Desktop** on Windows, you must configure Docker to trust your local Nexus registry (as an "insecure" registry) before you can push images.

> **Steps:**
>
> 1. Open a terminal and run:
     >    ```sh
>    wsl -d rancher-desktop
>    ```
> 2. Edit `/etc/docker/daemon.json` and add your Nexus NodePort registry (e.g., `192.168.127.2:30500`):
     >    ```json
>    {
>      "insecure-registries": ["192.168.127.2:30500"]
>    }
>    ```
> 3. Restart Rancher Desktop.

---

## Using the `scan-and-push.sh` Script

This script automates the process of building a Java (Maven) project, building a Docker image, scanning it for vulnerabilities using Trivy, and pushing to your Nexus Docker registry **only if no critical/high vulnerabilities are found**.

### **Script Workflow**

1. **Docker login** to Nexus registry (interactive, in a real terminal like PowerShell or CMD)
2. **Build Java application** with Maven (`mvn clean package -DskipTests`)
3. **Build Docker image** and tag it for the Nexus registry
4. **Scan Docker image** with Trivy (blocks push if critical/high vulnerabilities exist)
5. **Push Docker image** to Nexus if scan passes

### **Example Usage**

```sh
./scan-and-push.sh <nexus_repo_url> <image_name> <image_tag>
# Example:
./scan-and-push.sh 192.168.127.2:30500/myrepo myapp 1.0.0
```

- **`nexus_repo_url`**: The full registry path, e.g., `192.168.127.2:30500/myrepo`
- **`image_name`**: The Docker image name, e.g., `myapp`
- **`image_tag`**: The Docker image tag, e.g., `1.0.0`

**Note:** You must have previously created the `myrepo` Docker (hosted) repo in Nexus UI.

### **Script Details**

The script will:
- Require Docker login at the beginning (run from a real terminal, not Git Bash on Windows).
- Build your Java application with Maven.
- Build and tag the Docker image.
- Scan the image with Trivy, blocking the push if critical/high vulnerabilities are found.
- Push the image to Nexus if the scan passes.

#### **Important Notes:**

- **Interactive Docker login:**  
  Run the script from a real terminal (PowerShell or CMD on Windows) for interactive login. Avoid Git Bash for `docker login` on Windows due to TTY issues.
- **Manual repo creation:**  
  The Nexus Docker (hosted) repo (e.g., `myrepo`) must be created manually before pushing images.
- **Insecure registry:**  
  Your Docker daemon (especially with Rancher Desktop) must trust the Nexus Docker registry as insecure if using HTTP and NodePort.

---

## Troubleshooting

- **Can't push images / `no basic auth credentials`:**  
  Make sure you logged in (`docker login <registry-host:port>`) and your Docker daemon trusts the registry.
- **Script fails on Docker login:**  
  Use a supported terminal (PowerShell or CMD for Windows).
- **Trivy not found:**  
  [Install Trivy](https://aquasecurity.github.io/trivy/v0.19.2/docs/installation/)

---

## References

- [Nexus 3 Documentation](https://help.sonatype.com/repomanager3)
- [Trivy Documentation](https://aquasecurity.github.io/trivy/)
- [Rancher Desktop Docs](https://docs.rancherdesktop.io/)

---