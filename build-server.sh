#!/bin/bash

pushd pathfinder-server;mvn clean package -Dskiptests;popd;podman build -f ServerDockerfile -t quay.io/bholmes/pathfinder-server:latest;podman push quay.io/bholmes/pathfinder-server:latest;oc import-image pathfinder-server-dev --from=quay.io/bholmes/pathfinder-server:latest --confirm
