Collection Certificate
######################

Utilisation de la collection Certificate
========================================

La collection Certificate permet de référencer et décrire unitairement les certificats utilisés par les contextes applicatifs.

Exemple de JSON stocké en base comprenant l'exhaustivité des champs
===================================================================

::
           
 {
    "_id": "aeaaaaaaaahwgpj2aa2fgak7cxqdy6aaaaaq",
    "SubjectDN": "CN=ihm-recette, O=vitam, L=paris, ST=idf, C=fr",
    "ContextId": "CT-000001",
    "SerialNumber": 254,
    "IssuerDN": "CN=ca_intermediate_client-external, OU=authorities, O=vitam, L=paris, ST=idf, C=fr",
    "Certificate": "Q2VydGlmaWNhdGU6CiAgICBEYXRhOgogICAgICAgIFZlcnNpb246IDMgKDB4MikKICA
    
        [...]
    
    kbE4KM08yV1dIRlJMWnpQRWZ4eXlxMm1TbVdsaUUvUzZUbzJVVEswamxobStpbThPa29mZmlLbXlodVpWS3
    S0tRU5EIENFUlRJRklDQVRFLS0tLS0="
 }

Détail des champs du JSON stocké dans la collection
===================================================

**"_id":** identifiant unique du certificat applicatif

  * Champ peuplé par la solution logicielle Vitam
  * Il s'agit d'une chaîne de 36 caractères correspondant à un GUID.
  * Cardinalité : 1-1
  
**"SubjectDN":** Identifiant unique (Distinguished Name) du certificat applicatif

  * Il s'agit d'une chaîne de caractères
  * Cadinalité : 1-1

**"ContextId":** Identifiant signifiant (Identifier) du contexte utilisant le certificat applicatif

  * Il s'agit d'une chaîne de caractères correspondant à l'identifiant signifiant d'un contexte
  * Cadinalité : 1-1

**"SerialNumber":** Numéro de série du certificat applicatif

  * Il s'agit d'un entier
  * Cadinalité : 1-1

**"IssuerDN":** Identifiant unique (Distinguished Name) de l'autorité de certification

  * Il s'agit d'une chaîne de caractères
  * Cadinalité : 1-1

**"Certificate":** Certificat

  * Il s'agit d'une chaîne de caractères
  * Cadinalité : 1-1
