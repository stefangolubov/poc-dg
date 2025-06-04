# Kubernetes Security Scanning Demo

This project demonstrates a security scanning setup with ZAP scanner running against a vulnerable demo application in Kubernetes. The infrastructure includes scheduled and on-demand scans, report storage, and a report viewer.

## Architecture Overview

1. **Vulnerable App**: Spring Boot application (`poc-zap-app`)
2. **Scanner**:
    - OWASP ZAP (scheduled and on-demand scans)
3. **Report Viewer**: NGINX web server with automated report comparison
4. **Storage**: Persistent volume for scan reports

## Components

### 1. Application Deployment
- `poc-zap-app`: Vulnerable Spring Boot application with OpenAPI docs
- Exposes:
    - API endpoints on port 9999
    - Actuator health checks
    - Swagger UI at `/swagger-ui.html`
    - OpenAPI docs at `/v2/api-docs`

### 2. Security Scanners
- **ZAP**:
    - Scheduled daily scan at 2 AM
    - On-demand scan job
    - API scanning mode using OpenAPI spec

### 3. Reporting
- Persistent volume (5GB) for storing reports
- Report viewer with:
    - Directory listing of all reports

## Deployment Commands

Execute these commands in order:

```bash
# 1. Create the namespace (if needed)
kubectl create namespace security-scanning

# 2. Create the PVC for reports
kubectl apply -f pvc.yaml

# 3. Deploy the vulnerable application
kubectl apply -f app-deployment.yaml
kubectl apply -f app-service.yaml

# 4. Deploy ZAP components
kubectl apply -f zap-deployment.yaml
kubectl apply -f zap-service.yaml
kubectl apply -f config-map.yaml

# 5. Deploy ZAP scan jobs
kubectl apply -f zap-cron-job.yaml
kubectl apply -f zap-pentest-job.yaml

# 6. Deploy the report viewer
kubectl apply -f report-viewer-deployment.yaml
kubectl apply -f report-viewer-service.yaml
```

## Quick Start

### Accessing Reports

1. Get the NodePort for the report viewer:
   ```sh
   kubectl get svc report-service
   ```
2. Access the reports at:
   ```sh
   http://<node-ip>:30080/
   ```
3. Access the comparison JSON at:
   ```sh
   http://<node-ip>:30080/compare
   ```

### Sample API Endpoints

These vulnerable endpoints are available for scanning:

- http://poc-zap-service:9999/api/users?id=1
- http://poc-zap-service:9999/api/admin/env
- http://poc-zap-service:9999/api/greet?username=test
- http://poc-zap-service:9999/api/ping?host=127.0.0.1
- http://poc-zap-service:9999/api/public
- http://poc-zap-service:9999/api/hello?name=test

### Usage

#### Running On-Demand Scans

```bash
# Run ZAP scan
kubectl create job --from=cronjob/zap-scheduled-scan zap-manual-scan

```
## Configuration Details

### Scanner Settings

| Scanner   | Image                                      | Schedule    | Resource Limits       | Scan Type           |
|-----------|--------------------------------------------|-------------|-----------------------|---------------------|
| ZAP       | zaproxy/zap-stable                         | 2 AM daily  | 4Gi RAM, 2 CPU        | API Scan (OpenAPI)  |

### Resource Allocation

| Component       | CPU Requests | CPU Limits | Memory Requests | Memory Limits |
|-----------------|--------------|------------|-----------------|---------------|
| ZAP Scanner     | 1            | 2          | 3Gi             | 4Gi           | 
| Vulnerable App  | Default      | Default    | Default         | Default       |
| Report Viewer   | Default      | Default    | Default         | Default       |