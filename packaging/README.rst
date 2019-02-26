Packaging
#########

In order by create a PROD packaging, we use the HROSPROD packaging, remove useless components for PROD environments & add signed RPMs.
To create signed RPM packages, the system needs in ${HOME} a privkey (either encrypted or not) ; if unencrypted, expected file is $HOME/.gnupg/private.key
To be able to use Programme VITAM's signed rpms, we provide the RPM key to import (file in PROD packaging is ``rpm_signed/RPM-GPG-KEY-vitam``).
