#!/bin/bash

pushd pathfinder-ui;mvn clean package -Dskiptests;popd;podman build -f UIDockerfile -t quay.io/bholmes/pathfinder-ui:latest;podman push quay.io/bholmes/pathfinder-ui:latest;oc import-image pathfinder-ui-dev --from=quay.io/bholmes/pathfinder-ui:latest --confirm
