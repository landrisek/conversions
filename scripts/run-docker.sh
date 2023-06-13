#!/bin/bash

# Build Docker image
docker build -t actors -f ./artifacts/actors/Dockerfile .

# Stop and remove any existing container with the same name
docker stop actors >/dev/null 2>&1
docker rm actors >/dev/null 2>&1

# Run Docker container
docker run --name actors actors
