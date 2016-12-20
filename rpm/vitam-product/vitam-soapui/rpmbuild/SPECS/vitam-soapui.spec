Name:          vitam-soapui
Version:       5.2.1
Release:       1%{?dist}
Summary:       SoapUI is the world's most widely-used open source API testing tool for SOAP and REST APIs.
Group:         Applications/File
License:       European Union Public Licence V. 1.1
BuildArch:     noarch
URL:           https://www.soapui.org/
Source0:       http://cdn01.downloads.smartbear.com/soapui/%{version}/SoapUI-%{version}-linux-bin.tar.gz

Requires:      java-1.8.0
Requires:      vitam-user-vitam

%description
SoapUI is the world's most widely-used open source API testing tool for SOAP and REST APIs. SoapUI offers SOA Web Services functional testing, REST API functional testing, WSDL coverage, message assertion testing and test refactoring. With over 10 years of experience backed by a vast open source community, SoapUI is the de facto method for ensuring quality when developing APIs and Web Services.

%prep
%setup -q -c

%build

%install
# On crée l'arborescence cible
mkdir -p %{buildroot}/vitam/app/soapui
# On pousse les fichiers 
cp -rp SoapUI-5.2.1/* %{buildroot}/vitam/app/soapui
# OMA: add-ons for HTTPBuilder
# FIXME : find a better solution later !
curl -k -L http://central.maven.org/maven2/org/codehaus/groovy/modules/http-builder/http-builder/0.6/http-builder-0.6.jar -o %{buildroot}/vitam/app/soapui/bin/ext/http-builder-0.6.jar
curl -k -L http://central.maven.org/maven2/xml-resolver/xml-resolver/1.1/xml-resolver-1.1.jar -o %{buildroot}/vitam/app/soapui/bin/ext/xml-resolver-1.1.jar
%pre

%post

%preun

%postun

%clean
rm -rf %{buildroot}

%files
%defattr(-,root,root,-)
%dir /vitam/app/soapui
%attr(755, vitam, vitam) /vitam/app/soapui

%doc

%changelog
* Thu Nov 8 2016 French Prime minister Office/SGMAP/DINSIC/Vitam Program <contact.vitam@culture.gouv.fr>
- Initial version