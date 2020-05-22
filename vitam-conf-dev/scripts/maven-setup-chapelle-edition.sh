#!/bin/bash
###############################################################################
###############################################################################
###
### This script is used for setting up Maven's certificates
###
###############################################################################
###############################################################################

###############################################################################
###
###  VARIABLES
###
###############################################################################

# CONF MAVEN DIRECTORY
SETTINGS_DIR=${HOME}/.m2

# CONF MAVEN SECURITY
SETTINGS_SECURITY_FILE=${SETTINGS_DIR}/settings-security.xml

# CONF MAVEN
SETTINGS_FILE=${SETTINGS_DIR}/settings.xml

# CONF NPM
NPMRC_FILE=${HOME}/.npmrc

# DOWNLOADING DIR
DOWNLOAD_PATH="/tmp"

WGET_OPTS=" --no-check-certificate "

# ALIAS's certificate
ALIAS_ISRGROOTX1="isrgrootx1"
ALIAS_X1="lets-encrypt-x1-cross-signed"
ALIAS_X2="lets-encrypt-x2-cross-signed"
ALIAS_X3="lets-encrypt-x3-cross-signed"
ALIAS_X4="lets-encrypt-x4-cross-signed"

# Default PWD for cacerts
DEFAULT_PWD_CACERTS="changeit"

# Script name
script_name=`basename $0`

echo ""
echo "${script_name} started"
echo ""
echo "#########################################################"
echo "Check system parameters"
echo " "

# Check java is installed on the system
wget_java=`which java`
if [ $? -ne 0 ] ; then
	echo "[fatal] java is not installed on the system"
	echo "Exit"
	exit 1
fi

# Check javac is installed on the system
wget_javac=`which javac`
if [ $? -ne 0 ] ; then
        echo "[fatal] javac is not installed on the system"
        echo "Exit"
        exit 2
fi

# OS name
OS="`uname -s`"

# Check wget is installed on the system
wget_check=`which wget`
if [ $? -ne 0 ] ; then
	echo "[fatal] No wget detected on the system"
	echo "Exit"
	exit 3
fi

wget_check_url=`wget $WGET_OPTS -nv --spider https://letsencrypt.org 1>/dev/null 2>&1`
if [ $? -ne 0 ] ; then
	echo "[fatal] letsencrypt.org cannot be reached"
	echo "Exit"
	exit 4
fi

# Check java_home is set to the java directory
if [ -z "${JAVA_HOME}" ] ; then
	echo "[fatal] Variable JAVA_HOME is not set"
	echo "Exit"
	exit 5
fi

# Path to certificates
JAVA_SECURITY_PATH="`find ${JAVA_HOME}/lib -name security -type d`"
if [ ! -d "${JAVA_SECURITY_PATH}" ] ; then
	echo "[fatal] Error with certificates directory"
	echo "Exit"
	exit 6
fi

# .npmrc check
if [ -f ${NPMRC_FILE} ]; then
		echo "Fichier .npmrc déjà présent, veuillez bien vouloir le déplacer/supprimer avant de relancer ce script"
		exit 1
fi

echo "OS : ${OS}"

###############################################################################
###
###  CHECKS
###	- java version
### - maven version
###
###############################################################################

# Checking JAVA version
#  1- Finding string "version"
#  2- Extrating the third argument (version)
#  3- Deleting the first caractère if :" and the last one if :"
JAVA_VERSION=`java -version 2>&1 | grep 'version' | awk '{print $3}' | sed -e 's/^"//' -e 's/"$//'`

# Checking JAVA version
if [ -z "${JAVA_VERSION}" ] ; then
	echo "[fatal] No OpenJDK detected on the system"
	echo "Exit"
    	exit 7
else
	echo "Version Java  : ${JAVA_VERSION}"
fi

# Checking MAVEN version
MAVEN_VERSION=`mvn --version 2>&1 | grep ' Maven' | awk '{print $3}'`

if [ -z "${MAVEN_VERSION}" ] ; then
	echo "[fatal] No Maven detected on the system"
	echo "If installed, please check PATH"
	echo "Exit"
    	exit 8
else
	echo "Version Maven : ${MAVEN_VERSION}"
fi

###############################################################################
###
###  setting up Maven's certificates
###
###############################################################################

echo ""
echo "#########################################################"
echo "Set up maven certificates"
echo " "

#
if [ ! -d "$SETTINGS_DIR" ]
then
	mkdir ${HOME}/.m2
fi

# Setting up MASTER_PWD
if [ -f "$SETTINGS_SECURITY_FILE" ]
then
	echo "File $SETTINGS_SECURITY_FILE already exists. Begin reused."
else
	echo "Generating master password : Please Enter a new master password"

	read -s MASTER_PWD
	MASTER_PWD=$(mvn --encrypt-master-password $MASTER_PWD)

	echo "Encrypted master password : $MASTER_PWD"
	echo "Creating file $SETTINGS_SECURITY_FILE..."

	cat > ${SETTINGS_SECURITY_FILE} <<-EOF
	<settingsSecurity>
  		<master>$MASTER_PWD</master>
	</settingsSecurity>
	EOF
fi

# Setting up NEXUS_PWD
if [ -f "$SETTINGS_FILE" ]
then
	echo "Attention : Le fichier $SETTINGS_FILE existe déjà ! Merci de bien vouloir le déplacer / supprimer avant d'exécuter ce script."
	exit 7
else
	echo "Génération du mot de passe serveur (nexus) ; merci d'entrer les informations suivantes :"
	echo "Login plate-forme VITAM :"

	read NEXUS_USER

	echo "Password plate-forme VITAM :"

	read -s NEXUS_PWD
	NEXUS_PWD_ENCRYPTED=$(mvn --encrypt-password $NEXUS_PWD)

	echo "Credentials chiffrés : $NEXUS_USER / $NEXUS_PWD_ENCRYPTED"
	echo "Création du fichier $SETTINGS_FILE..."

	cat > ${SETTINGS_FILE} <<-EOF
	<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
		xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
		xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
		https://maven.apache.org/xsd/settings-1.0.0.xsd">
		<servers>
			<server>
				<id>vitam</id>
				<username>$NEXUS_USER</username>
				<password>$NEXUS_PWD_ENCRYPTED</password>
			</server>
		</servers>
		<!--<mirrors>
			<mirror>
				<id>vitam-local</id>
		      		<name>Vitam Local</name>
		      		<url>https://nexus.dev.programmevitam.fr/repository/maven-central/</url>
		      		<mirrorOf>vitam</mirrorOf>
		    	</mirror>
		</mirrors>-->
	</settings>
	EOF
fi

echo ""
echo "#########################################################"
echo "Download and Store letsencrypt certificates into the java truststore"
echo " "

# DOWNLOAD_PATH dir
cd ${DOWNLOAD_PATH}

# Getting the certificates from lets encrypt
# delete certificate in the folder (in case they already exist because it would create xxx.1 files), install the certificats
# If the -trustcacerts option has been specified, additional certificates are considered for the chain of trust, namely the certificates in a file named "cacerts ».
# cacerts : Default pwd (changeit)

rm -f ${ALIAS_ISRGROOTX1}.pem
wget $WGET_OPTS -nv https://letsencrypt.org/certs/${ALIAS_ISRGROOTX1}.pem
if [ -f "${DOWNLOAD_PATH}/${ALIAS_ISRGROOTX1}.pem" ]
then
	sudo keytool -trustcacerts -keystore ${JAVA_SECURITY_PATH}/cacerts -storepass ${DEFAULT_PWD_CACERTS} -noprompt -importcert -alias ${ALIAS_ISRGROOTX1} -file ${DOWNLOAD_PATH}/${ALIAS_ISRGROOTX1}.pem
	rm -f ${DOWNLOAD_PATH}/${ALIAS_ISRGROOTX1}.pem
else
	echo "Error downloading certificate: ${ALIAS_ISRGROOTX1}.pem"
	echo "Exit"
	exit 9
fi

rm -f /tmp/${ALIAS_X1}.pem
wget $WGET_OPTS -nv https://letsencrypt.org/certs/${ALIAS_X1}.pem
if [ -f "${DOWNLOAD_PATH}/${ALIAS_X1}.pem" ]
then
	sudo keytool -trustcacerts -keystore ${JAVA_SECURITY_PATH}/cacerts -storepass ${DEFAULT_PWD_CACERTS} -noprompt -importcert -alias ${ALIAS_X1} -file ${DOWNLOAD_PATH}/${ALIAS_X1}.pem
	rm -f ${DOWNLOAD_PATH}/${ALIAS_X1}.pem
else
	echo "Error downloading certificate: ${ALIAS_X1}.pem"
	echo "Exit"
	exit 10
fi

rm -f /tmp/${ALIAS_X2}.pem
wget $WGET_OPTS -nv https://letsencrypt.org/certs/${ALIAS_X2}.pem
if [ -f "${DOWNLOAD_PATH}/${ALIAS_X2}.pem" ]
then
	sudo keytool -trustcacerts -keystore ${JAVA_SECURITY_PATH}/cacerts -storepass ${DEFAULT_PWD_CACERTS} -noprompt -importcert -alias ${ALIAS_X2} -file ${DOWNLOAD_PATH}/${ALIAS_X2}.pem
	rm -f ${DOWNLOAD_PATH}/${ALIAS_X2}.pem
else
	echo "Error downloading certificate: ${ALIAS_X2}.pem"
	echo "Exit"
	exit 11
fi

rm -f /tmp/${ALIAS_X3}.pem
wget $WGET_OPTS -nv https://letsencrypt.org/certs/${ALIAS_X3}.pem
if [ -f "${DOWNLOAD_PATH}/${ALIAS_X3}.pem" ]
then
	sudo keytool -trustcacerts -keystore ${JAVA_SECURITY_PATH}/cacerts -storepass ${DEFAULT_PWD_CACERTS} -noprompt -importcert -alias ${ALIAS_X3} -file ${DOWNLOAD_PATH}/${ALIAS_X3}.pem
	rm -f ${DOWNLOAD_PATH}/${ALIAS_X3}.pem
else
	echo "Error downloading certificate: ${ALIAS_X3}.pem"
	echo "Exit"
	exit 12
fi

rm -f /tmp/${ALIAS_X4}.pem
wget $WGET_OPTS -nv https://letsencrypt.org/certs/${ALIAS_X4}.pem
if [ -f "${DOWNLOAD_PATH}/${ALIAS_X4}.pem" ]
then
	sudo keytool -trustcacerts -keystore ${JAVA_SECURITY_PATH}/cacerts -storepass ${DEFAULT_PWD_CACERTS} -noprompt -importcert -alias ${ALIAS_X4} -file ${DOWNLOAD_PATH}/${ALIAS_X4}.pem
	rm -f ${DOWNLOAD_PATH}/${ALIAS_X4}.pem
else
	echo "Error downloading certificate: ${ALIAS_X4}.pem"
	echo "Exit"
	exit 13
fi

echo ""
echo "#########################################################"
echo "List certificates"
echo ""
keytool -keystore ${JAVA_SECURITY_PATH}/cacerts -storepass changeit -list | grep -E "${ALIAS_ISRGROOTX1}|${ALIAS_X1}|${ALIAS_X2}|${ALIAS_X3}|${ALIAS_X4}"
echo ""
echo "Fin de l'ajout des certificats racine let's encrypt dans le truststore java"
echo "##########################################################"

if [ -z "${M2_HOME}" ] ; then
  	echo "[warning] Variable M2_HOME is not set"
		echo "Please add M2_HOME variable"
fi

echo ""
echo "#########################################################"
echo "Configuration du .npmrc"
echo ""

echo "Création du .npmrc"
echo "# Trace des requetes http (optionnel)" 																				>  ${NPMRC_FILE}
echo "# loglevel=http"																															>> ${NPMRC_FILE}
echo "# Registry"																																		>> ${NPMRC_FILE}
echo "registry=https://nexus.dev.programmevitam.fr/repository/vitam-npm/"		>> ${NPMRC_FILE}
echo "# registry=https://registry.npmjs.org/"																				>> ${NPMRC_FILE}
echo "# Obligatoire si utilisation du registry vitam"																>> ${NPMRC_FILE}
echo "always-auth=true"																															>> ${NPMRC_FILE}
echo "# Credentials "																																>> ${NPMRC_FILE}
echo "_auth=\"$(echo -n ${NEXUS_USER}:${NEXUS_PWD} | base64)\"" 							>> ${NPMRC_FILE}

echo ""
echo "Fin de configuration du .npmrc"
echo "##########################################################"
