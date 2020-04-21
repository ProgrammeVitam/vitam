#!/bin/bash
# According to https://github.com/minio/minio/issues/6820

echo "Prepare OpenSSL config file..."

cat > openssl.conf <<- EOM
[req]
distinguished_name = req_distinguished_name
x509_extensions = v3_req
prompt = no

[req_distinguished_name]
C = FR
ST = ID
L = Paris
O = vitam
OU = dev
CN = 127.0.0.1

[v3_req]
subjectAltName = @alt_names

[alt_names]
IP.1 = 172.17.0.2
IP.2 = 127.0.0.1
EOM

echo "Generate certificate..."
openssl req -x509 -nodes -days 730 -newkey rsa:2048 -keyout private.key -out public.crt -config openssl.conf
echo "To check public.crt, use command : openssl x509 -noout -text -in public.crt"

echo "Generate p12 from key/crt couple..."
openssl pkcs12 -export \
        -inkey "private.key" \
        -in "public.crt" \
        -name "127.0.0.1" \
        -out "cert.p12" \
        -passout pass:"pass"

echo "Generate TrustStore..."
keytool -importkeystore \
        -srckeystore cert.p12 -srcstorepass pass -srcstoretype PKCS12 \
        -destkeystore s3TrustStore.jks -storepass s3pass \
        -keypass s3pass -deststorepass s3pass \
        -destkeypass s3pass -deststoretype JKS  \
        -keysize 2048 -keyalg RSA -noprompt

echo "To check JKS file, use command : keytool -list -v -keystore s3TrustStore.jks"
echo "Purge useless temp files..."
rm -f openssl.conf
rm -f cert.p12

