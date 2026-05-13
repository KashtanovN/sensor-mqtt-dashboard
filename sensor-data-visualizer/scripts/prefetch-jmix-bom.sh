#!/usr/bin/env bash
# Prefetch Jmix BOM into ~/.m2/repository so Gradle can pick it up via mavenLocal().
# Uses curl with retries (often more tolerant than Gradle on bad networks).
set -euo pipefail
JMIX_VERSION="${1:-2.8.1}"
BASE="https://global.repo.jmix.io/repository/public/io/jmix/bom/jmix-bom/${JMIX_VERSION}"
DEST="${HOME}/.m2/repository/io/jmix/bom/jmix-bom/${JMIX_VERSION}"
mkdir -p "${DEST}"
echo "Downloading ${BASE}/jmix-bom-${JMIX_VERSION}.pom -> ${DEST}"
curl -fL --retry 30 --retry-delay 5 --connect-timeout 60 --max-time 0 \
  -o "${DEST}/jmix-bom-${JMIX_VERSION}.pom" \
  "${BASE}/jmix-bom-${JMIX_VERSION}.pom"
echo "Done. Now run: cd \"$(dirname "$0")/..\" && ./gradlew --stop && ./gradlew bootRun --refresh-dependencies"
