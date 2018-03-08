#!/usr/bin/env bash

cat /dev/random | tr -dc 'a-zA-Z0-9!@#$%^&*_+-.\\/:<>=?@{}|~[]^' | head -c 16 | base64
