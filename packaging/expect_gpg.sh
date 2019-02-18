#!/usr/bin/expect -f
  
### rpm-sign.exp -- Sign RPMs by sending the passphrase.

spawn rpm --addsign {*}$argv
expect -exact "Enter pass phrase: "
send -- "$env(GPG_PASSPHRASE)\r"
expect eof