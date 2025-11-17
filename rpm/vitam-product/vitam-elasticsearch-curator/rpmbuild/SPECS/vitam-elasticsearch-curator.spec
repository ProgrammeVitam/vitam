Name:          vitam-elasticsearch-curator
Version:       8.0.21
Release:       1%{?dist}
Summary:       Curator is an open source ElasticSearch tool to manage indices lifecycle
Group:         Applications/File
License:       Apache-2.0
URL:           https://github.com/elastic/curator

# GitHub source
Source0:       https://github.com/elastic/curator/archive/v%{version}.tar.gz

BuildArch:     x86_64

Requires:      vitam-user-vitam

# System build requirements (commented because built on Debian-based CI system)
# BuildRequires: python3
# BuildRequires: python3-virtualenv
# BuildRequires: tar

%global debug_package %{nil}
%global appfolder /vitam/bin/curator

%description
Curator is an open source (Apache 2.0) Elasticsearch tool to manage indices lifecycle.

# --------------------------
# Unpack source archive (RPM macro handles this cleanly)
# --------------------------
%prep
%setup -q -n curator-%{version}

# --------------------------
# Build Curator binary inside an isolated virtualenv
# --------------------------
%build
echo "=== Creating isolated virtualenv ==="
python3 -m venv venv

# Activate it and install pyinstaller
. venv/bin/activate
# Upgrade pip inside the venv and install required packages for building Curator
pip install --upgrade pip
pip install pyinstaller click elasticsearch8 voluptuous es_client

# Build the single-file binary with PyInstaller
pyinstaller --onefile run_curator.py

deactivate

# --------------------------
# Install files into the RPM image
# --------------------------
%install
mkdir -p %{buildroot}%{appfolder}

# Copy built binary and licencing files
cp -va dist/run_curator CONTRIBUTORS LICENSE NOTICE %{buildroot}%{appfolder}

# --------------------------
# Cleanup phase (optional in modern RPM)
# --------------------------
%clean
rm -rf %{buildroot}

# --------------------------
# File ownership and permissions
# --------------------------
%files
%dir %{appfolder}
%attr(750, vitam, vitam) %{appfolder}/run_curator
%attr(640, vitam, vitam) %{appfolder}/CONTRIBUTORS
%attr(640, vitam, vitam) %{appfolder}/LICENSE
%attr(640, vitam, vitam) %{appfolder}/NOTICE

%doc

# ------------------------------------------------------
# Changelog
# ------------------------------------------------------
%changelog
* Tue Nov 11 2025 French Prime Minister Office / SGMAP / DINSIC / Vitam Program <contact@programmevitam.fr> - 8.0.21-1
- Build Curator using Python virtualenv with PyInstaller
