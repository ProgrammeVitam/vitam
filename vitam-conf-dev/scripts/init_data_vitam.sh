#!/bin/bash

if [ -d /vitam/data/offer ]; then
  rm -rf /vitam/data/offer/*_backup
fi

echo "Import security profile"
echo "Begin"
curl -d '[{"Name": "admin-security-profile", "Identifier": "admin-security-profile", "FullAccess": true, "Permissions": null }]' -H "Content-Type: application/json" -X POST http://functional-administration.service.consul:18004/v1/admin/securityprofiles
echo "End"

echo "Import context"
echo "Begin"
curl -d '[{"Name": "admin-context", "Status": "ACTIVE","Identifier": "CT-000001", "SecurityProfile": "admin-security-profile", "Permissions": [ {"tenant": 0, "AccessContracts": [], "IngestContracts": []},{"tenant": 1, "AccessContracts": [], "IngestContracts": []}, {"tenant": 2, "AccessContracts": [], "IngestContracts": []} ]}]' -H "Content-Type: application/json" -X POST http://functional-administration.service.consul:18004/v1/admin/contexts
echo "End"

echo "Import ihm-demo certificate"
echo "Begin"
curl -d '{ "contextId" : "CT-000001" , "certificate" : "'$(keytool -keystore ../conf/ihm-demo/keystore_ihm-demo.p12 -storetype pkcs12 -storepass azerty4 -exportcert -alias ihm-demo | base64 -w 0)'"}' -H "Content-Type: application/json" -X POST http://security-internal.service.consul:28005/v1/api/identity
echo "End"

echo "Import ihm-recette certificate"
echo "Begin"
curl -d '{ "contextId" : "CT-000001" , "certificate" : "'$(keytool -keystore ../conf/ihm-recette/keystore_ihm-recette.p12 -storetype pkcs12 -storepass azerty5 -exportcert -alias ihm-recette | base64 -w 0)'"}' -H "Content-Type: application/json" -X POST http://security-internal.service.consul:28005/v1/api/identity
echo "End"

echo "Import personal certificate"
echo "Begin"
curl -XPOST -H "Content-type: application/octet-stream" --data-binary @../../deployment/environments/certs/client-vitam-users/clients/userOK.crt 'http://security-internal.service.consul:28005/v1/api/personalCertificate'
echo "End"

echo "Import ontologies"
echo "Begin"
curl -XPOST -H "Content-type: application/json" -H "X-Tenant-Id: 1" -H "Force-Update: true" --data-binary @../../deployment/ansible-vitam/roles/init_contexts_and_security_profiles/files/VitamOntology.json 'http://functional-administration.service.consul:18004/v1/admin/ontologies'
echo "End"
