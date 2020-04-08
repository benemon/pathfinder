#!/bin/bash

pushd pathfinder-ui;mvn clean package -Dskiptests;popd;podman build -f UIDockerfile -t quay.io/bholmes/pathfinder-ui:latest;podman push quay.io/bholmes/pathfinder-ui:latest;oc import-image pathfinder-ui-dev --from=quay.io/bholmes/pathfinder-ui:latest --confirm
pushd pathfinder-server;mvn clean package -Dskiptests;popd;podman build -f ServerDockerfile -t quay.io/bholmes/pathfinder-server:latest;podman push quay.io/bholmes/pathfinder-server:latest;oc import-image pathfinder-server-dev --from=quay.io/bholmes/pathfinder-server:latest --confirm
