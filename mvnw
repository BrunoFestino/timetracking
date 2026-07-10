#!/bin/sh
# ----------------------------------------------------------------------------
# Minimal Maven Wrapper for POSIX shells.
# Downloads Apache Maven on first use, caches it under ~/.m2/wrapper, then runs it.
# ----------------------------------------------------------------------------
set -e

MVN_VERSION="3.9.9"
WRAPPER_DIR="${HOME}/.m2/wrapper"
MVN_HOME="${WRAPPER_DIR}/apache-maven-${MVN_VERSION}"
MVN_URL="https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/${MVN_VERSION}/apache-maven-${MVN_VERSION}-bin.zip"
MVN_ZIP="${WRAPPER_DIR}/apache-maven-${MVN_VERSION}-bin.zip"

if [ ! -x "${MVN_HOME}/bin/mvn" ]; then
    mkdir -p "${WRAPPER_DIR}"
    echo "Downloading Apache Maven ${MVN_VERSION}..."
    if command -v curl >/dev/null 2>&1; then
        curl -fsSL "${MVN_URL}" -o "${MVN_ZIP}"
    elif command -v wget >/dev/null 2>&1; then
        wget -q "${MVN_URL}" -O "${MVN_ZIP}"
    else
        echo "Need curl or wget to download Maven" >&2
        exit 1
    fi
    echo "Extracting..."
    (cd "${WRAPPER_DIR}" && unzip -q "${MVN_ZIP}")
    rm -f "${MVN_ZIP}"
    chmod +x "${MVN_HOME}/bin/mvn"
fi

exec "${MVN_HOME}/bin/mvn" "$@"
