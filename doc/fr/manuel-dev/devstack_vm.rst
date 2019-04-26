################################################
Création d'une machine de dev contenant *Swift*
################################################

Afin de pouvoir tester facilement Swift en local, il est possible de créer en local une machine virtuelle contenant une implémentation de swift.
Cette documentation décrit la procédure d'installation d'une machine virtuelle basé sur devstack, avec comme hyperviseur Qemu/Kvm ou virtualbox.

Préparation de la machine virtuelle avec Qemu
=============================================

Télécharger une version d'ubuntu server [16.04](http://releases.ubuntu.com/16.04/ubuntu-16.04.3-server-amd64.iso).

Pendant la phase d'installation,  préciser bien comme locale en_US.UTF-8.

Exemple de commmande pour lancer une vm avec Qemu en spécifiant l'iso à utiliser :

qemu-system-x86_64 -enable-kvm -hda devstack_img -cdrom ../Téléchargements/ubuntu-16.04.3-server-amd64.iso -m 4096 -boot d

Le paramètre devstack_img correspond au fichier contenant le disque dur qui peut être crée avec la commande

qemu-img create -f raw devstack_img 10G

Préparation de la machine virtuelle avec Virtualbox
===================================================

// TODO

Installation de devstack
========================

Création d'un user stack

# sudo useradd -s /bin/bash -d /opt/stack -m stack
# echo "stack ALL=(ALL) NOPASSWD: ALL" | sudo tee /etc/sudoers.d/stack
# sudo su - stack

Cloner le projet :

# git clone https://git.openstack.org/openstack-dev/devstack
# cd devstack

Configurer devstack

# créer un fichier local.conf with:

[[local|localrc]]
ADMIN_PASSWORD=secret
DATABASE_PASSWORD=$ADMIN_PASSWORD
RABBIT_PASSWORD=$ADMIN_PASSWORD
SERVICE_PASSWORD=$ADMIN_PASSWORD
# FIXED_RANGE=10.0.0.0/24
HOST_IP=127.0.0.1
SWIFT_HASH=a4ef4e78cde09a21

OFFLINE=True

disable_all_services
enable_service key mysql s-proxy s-object s-container s-account

# lancer la commande
./stack.sh

Liste des ports à partager :

.. csv-table::
  :header: "Host","Guest"

  2222,22
  5000,5000
  8080,8080
  8000,80

Commande pour lancer Qemu avec le transfert de port :

qemu-system-x86_64 -enable-kvm -drive format=raw,file=devstack_img -m 4096 -net nic -net user,hostfwd=tcp::8080-:8080,hostfwd=tcp::5000-:5000,hostfwd=tcp::8000-:80,hostfwd=tcp::2222-:22
