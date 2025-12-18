#!/bin/bash
set -e

CURATOR_VERSION=8.0.21
CURATOR_FILE="curator-${CURATOR_VERSION}.tar.gz"
INTERNAL_REPO="${SERVICE_REPOSITORY_URL}/vitam-product-binaries"
WORKING_FOLDER=$(dirname $0)
CURATOR_DIR="${WORKING_FOLDER}/vitam-elasticsearch-curator/vitam/bin/curator"
TARGET_DIR="${WORKING_FOLDER}/target"

mkdir -p "${TARGET_DIR}"

echo "Downloading ${CURATOR_FILE}..."
# Download curator file (prefer internal repo, fallback to GitHub)
if curl --head --silent --fail "${INTERNAL_REPO}/${CURATOR_FILE}" > /dev/null; then
    echo "File exists in internal cache repository."
    curl -k -L "${INTERNAL_REPO}/${CURATOR_FILE}" -o "${CURATOR_FILE}"
else
    echo "File does not exist in internal cache repository."
    curl -k -L "https://github.com/elastic/curator/archive/v${CURATOR_VERSION}.tar.gz" -o "${CURATOR_FILE}"
fi

# Unpack and build Curator
echo "Unpacking ${CURATOR_FILE}..."
tar -xvzf "${CURATOR_FILE}"

CURATOR_SRC_DIR="curator-${CURATOR_VERSION}"

echo "=== Creating isolated virtualenv ==="
# Create an isolated Python virtual environment for build
python3.11 -m venv "${CURATOR_SRC_DIR}/venv"

# Activate it
. "${CURATOR_SRC_DIR}/venv/bin/activate"

# Upgrade pip inside the venv and install required packages for building Curator
pip3.11 install --upgrade pip
pip3.11 install pyinstaller click elasticsearch8 voluptuous es_client

# Build the single-file binary with PyInstaller
pyinstaller --onefile "${CURATOR_SRC_DIR}/run_curator.py" --distpath "${CURATOR_SRC_DIR}/dist"

# Deactivate and remove virtualenv after build
deactivate
rm -rf "${CURATOR_SRC_DIR}/venv"

# Copy built binary and licencing files
mkdir -p "${CURATOR_DIR}"
cp -va "${CURATOR_SRC_DIR}/dist/run_curator" "${CURATOR_SRC_DIR}/CONTRIBUTORS" "${CURATOR_SRC_DIR}/LICENSE" "${CURATOR_SRC_DIR}/NOTICE" "${CURATOR_DIR}/"
chmod 750 "${CURATOR_DIR}/run_curator"

# Cleanup build directory and archive
rm -rf "${CURATOR_SRC_DIR}" "${CURATOR_FILE}"

# Build Debian package
dpkg-deb --build "${WORKING_FOLDER}/vitam-elasticsearch-curator" "${TARGET_DIR}"

# Clean up curator directory
echo "Cleaning curator directory..."
rm -rf "${CURATOR_DIR:?}/"*
