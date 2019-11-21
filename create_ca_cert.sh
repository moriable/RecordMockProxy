#!/bin/sh

openssl genrsa -out ca.key 2048
openssl req -new -x509 -days 3650 -key ca.key -sha512 -out ca.crt -subj "/CN=RecordMockProxyCA"
