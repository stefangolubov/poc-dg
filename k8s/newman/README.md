# Newman Collection Automation – Kubernetes Quick Start

This guide explains how to run an automated Newman (Postman) collection job in Kubernetes, and how to view the results via a web-based report viewer.

---

## Prerequisites

- Your Postman collection(s) are present on your local machine, e.g.:
    - `D:\DEV\Postman\collection.json`
- The `reports-pvc` Persistent Volume Claim is available in your cluster (see below).
- The [report viewer](#3-deploy-the-report-viewer) is deployed as described below.

---

## 1. Apply the Persistent Volume Claim

This PVC is used to store generated Newman reports and to share them with the report viewer.

```sh
kubectl apply -f reports\pvc.yaml
```

---

## 2. Deploy the Report Viewer

This deployment runs an Nginx server that serves the reports written to the PVC.

```sh
kubectl apply -f reports\deployment.yaml
```

---

## 3. Expose the Report Viewer with a Service

This service exposes the report viewer on a node port (default: 30080).

```sh
kubectl apply -f reports\service.yaml
```

---

## 4. Verify Report Viewer Works

After a minute or so, open your browser and visit:

```
http://<your-node-ip>:30080/
```

You should see the Nginx directory listing. (There may be no reports yet until the next step is completed.)

---

## 5. Run the Newman Job

This job runs your Postman collection with Newman and writes JSON/JUnit reports to the shared PVC.

```sh
kubectl apply -f newman-collection-job.yaml
```

You can monitor the job with:

```sh
kubectl get pods -w
kubectl logs -f <newman-collection-job-pod-name>
```

---

## 6. View the Newman Reports

After the Newman job completes, reports will appear in the `/newman` directory served by the report viewer.

- Open: `http://<your-node-ip>:30080/newman/`
    - Find the most recent `.json` or `.xml` files (`newman-report-<timestamp>.json`, etc.)

---

## 7. (Optional) Cleanup

To remove test resources:

```sh
kubectl delete job newman-collection-job
kubectl delete deployment report-viewer
kubectl delete service report-service
kubectl delete pvc reports-pvc
```

---

## Notes

- If you update your Postman collection, re-run the Newman job.
- If you need to mount the Newman collection from a different path, update the `newman-collection-job.yaml` volume definition and container path.
- You can run multiple Newman jobs; each will write a timestamped report to the shared PVC, visible in the report viewer.
- The same report viewer can be used for both ZAP and Newman jobs, as long as they both use the same reports PVC.

---