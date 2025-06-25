#!/bin/bash

if [ "$#" -ne 3 ]; then
    echo "Usage: $0 <nexus_repo_url> <image_name> <image_tag>"
    echo "Example: $0 192.168.127.2:30500/myrepo myapp 1.0.0"
    exit 1
fi

NEXUS_REPO_URL="$1"
IMAGE_NAME="$2"
IMAGE_TAG="$3"
FULL_IMAGE="${NEXUS_REPO_URL}/${IMAGE_NAME}:${IMAGE_TAG}"
REGISTRY_HOSTPORT=$(echo "$NEXUS_REPO_URL" | cut -d/ -f1)

# --- STEP 1: Require Docker login at the beginning ---
echo "Logging in to $REGISTRY_HOSTPORT (required for push)..."
docker login "$REGISTRY_HOSTPORT"
if [ $? -ne 0 ]; then
    echo "Docker login failed. Not building or pushing image."
    read -p "Press [Enter] to exit..."
    exit 1
fi

echo "Building Java application with Maven..."
mvn clean package -DskipTests
if [ $? -ne 0 ]; then
    echo "Maven build failed. Fix errors and try again."
    read -p "Press [Enter] to exit..."
    exit 1
fi

echo "Building Docker image: $FULL_IMAGE"
docker build -t "$FULL_IMAGE" .
if [ $? -ne 0 ]; then
    echo "Docker build failed. Fix errors and try again."
    read -p "Press [Enter] to exit..."
    exit 1
fi

echo "Scanning Docker image with Trivy..."
trivy image --exit-code 1 --severity CRITICAL,HIGH "$FULL_IMAGE"
SCAN_RESULT=$?

if [ $SCAN_RESULT -ne 0 ]; then
    echo "Vulnerabilities found! Not pushing $FULL_IMAGE to Nexus."
    read -p "Press [Enter] to exit..."
    exit 1
else
    echo "No critical/high vulnerabilities found. Pushing $FULL_IMAGE to Nexus..."
    docker push "$FULL_IMAGE"
    read -p "Push complete. Press [Enter] to exit..."
fi