#!/bin/bash
set -e

pushd $(dirname $0)

echo -e "\n\n###################################\nCleaning up..."
rm *.crt || true
rm *.key || true
rm *.p12 || true
rm *.jks || true


echo -e "\n\n###################################\nGenerating root CA key and certificate..."
openssl genpkey -algorithm RSA -out root-ca.key
openssl req -new -x509 -key root-ca.key -out root-ca.crt -subj "/CN=root-ca" -days 36500 -addext "nsComment=CA Root" -addext "subjectKeyIdentifier=hash" -addext "authorityKeyIdentifier=keyid,issuer" -addext "basicConstraints=critical,CA:true,pathlen:1" -addext "keyUsage=keyCertSign,cRLSign" -addext "nsCertType=sslCA"


echo -e "\n\n###################################\nGenerating intermediate CA key and certificate..."
cat <<EOT > intermediate-ca.ext
nsComment                       = "CA Intermediate"
subjectKeyIdentifier            = hash
authorityKeyIdentifier          = keyid,issuer:always
basicConstraints                = critical,CA:true,pathlen:0
issuerAltName                   = issuer:copy
keyUsage                        = keyCertSign, cRLSign
nsCertType                      = sslCA
EOT

openssl genpkey -algorithm RSA -out intermediate-ca.key
openssl req -new -key intermediate-ca.key -out intermediate-ca.csr -subj "/CN=intermediate-ca"
openssl x509 -req -in intermediate-ca.csr -CA root-ca.crt -CAkey root-ca.key -CAcreateserial -out intermediate-ca.crt -days 36500 -extfile intermediate-ca.ext
rm intermediate-ca.csr intermediate-ca.ext


echo -e "\n\n###################################\nGenerating app server key and certificate..."
cat <<EOT > app.ext
subjectKeyIdentifier            = hash
authorityKeyIdentifier          = keyid,issuer:always
issuerAltName                   = issuer:copy
basicConstraints                = critical,CA:FALSE
keyUsage                        = digitalSignature, keyEncipherment
nsCertType                      = server
extendedKeyUsage                = serverAuth
EOT

openssl genpkey -algorithm RSA -out app.key
openssl req -new -key app.key -out app.csr -subj "/CN=app"
openssl x509 -req -in app.csr -CA intermediate-ca.crt -CAkey intermediate-ca.key -CAcreateserial -out app.crt -days 7300 -extfile app.ext
rm app.csr app.ext


echo -e "\n\n###################################\nGenerating reverse key and certificate..."
cat <<EOT > reverse.ext
subjectKeyIdentifier            = hash
authorityKeyIdentifier          = keyid,issuer:always
issuerAltName                   = issuer:copy
basicConstraints                = critical,CA:FALSE
keyUsage                        = digitalSignature, keyEncipherment
nsCertType                      = server
extendedKeyUsage                = serverAuth
EOT

openssl genpkey -algorithm RSA -out reverse.key
openssl req -new -key reverse.key -out reverse.csr -subj "/CN=reverse"
openssl x509 -req -in reverse.csr -CA intermediate-ca.crt -CAkey intermediate-ca.key -CAcreateserial -out reverse.crt -days 7300 -extfile reverse.ext
rm reverse.csr reverse.ext


echo -e "\n\n###################################\nGenerating client key and certificate..."
cat <<EOT > client.ext
subjectKeyIdentifier            = hash
authorityKeyIdentifier          = keyid,issuer:always
issuerAltName                   = issuer:copy
basicConstraints                = critical,CA:FALSE
keyUsage                        = digitalSignature
nsCertType                      = client
extendedKeyUsage                = clientAuth
EOT

openssl genpkey -algorithm RSA -out client.key
openssl req -new -key client.key -out client.csr -subj "/CN=client"
openssl x509 -req -in client.csr -CA intermediate-ca.crt -CAkey intermediate-ca.key -CAcreateserial -out client.crt -days 7300 -extfile client.ext
rm client.csr client.ext

echo -e "\n\n###################################\nVerifying the certificates..."
openssl verify -verbose -verify_depth 3 -CAfile root-ca.crt -untrusted intermediate-ca.crt app.crt
openssl verify -verbose -verify_depth 3 -CAfile root-ca.crt -untrusted intermediate-ca.crt reverse.crt
openssl verify -verbose -verify_depth 3 -CAfile root-ca.crt -untrusted intermediate-ca.crt client.crt


echo -e "\n\n###################################\nGenerating PKCS12 / JKS key stores..."
openssl pkcs12 -export -in app.crt -inkey app.key -out app.p12 -name "app" -password pass:azerty
openssl pkcs12 -export -in reverse.crt -inkey reverse.key -out reverse.p12 -name "reverse" -password pass:azerty
openssl pkcs12 -export -in client.crt -inkey client.key -out client.p12 -name "client" -password pass:azerty

keytool -importkeystore -srckeystore app.p12 -srcstoretype PKCS12 -srcstorepass azerty -destkeystore app.jks -deststoretype JKS -deststorepass azerty
keytool -importkeystore -srckeystore reverse.p12 -srcstoretype PKCS12 -srcstorepass azerty -destkeystore reverse.jks -deststoretype JKS -deststorepass azerty
keytool -importkeystore -srckeystore client.p12 -srcstoretype PKCS12 -srcstorepass azerty -destkeystore client.jks -deststoretype JKS -deststorepass azerty

echo -e "\n\n###################################\nGenerating trust stores..."
keytool -import -trustcacerts -alias root-ca -file root-ca.crt -keystore truststore.jks -storepass azerty -noprompt
keytool -import -trustcacerts -alias intermediate-ca -file intermediate-ca.crt -keystore truststore.jks -storepass azerty -noprompt
cat root-ca.crt intermediate-ca.crt > truststore.crt

echo -e "\n\n###################################\nCopying files..."
cp app.* ../app/
cp truststore.* ../app/

cp reverse.* ../reverse-httpd/
cp truststore.* ../reverse-httpd/
cp reverse.* ../reverse-nginx/
cp truststore.* ../reverse-nginx/

cp client.* ../client/
cp -f truststore.* ../client/

echo -e "\n\n###################################\nCleaning up..."
rm *.crt || true
rm *.key || true
rm *.p12 || true
rm *.jks || true

echo -e "\n\n###################################\nDONE !"
popd