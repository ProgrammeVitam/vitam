Collection ObjectGroup
######################

Utilisation de la collection ObjectGroup
========================================

La collection ObjectGroup contient les informations relatives aux groupes d'objets.

Exemple de XML
==============

Ci-après, la porpartietion d'un bordereau de transfert (manifest.xml) utilisée pour compléter les champs du JSON correspondant à un groupe d'objets.

::

  <PhysicalDataObject id="ID109">
      <DataObjectGroupReferenceId>ID0009</DataObjectGroupReferenceId>
      <DataObjectVersion>PhysicalMaster</DataObjectVersion>
      <PhysicalId>1 Num 1/191-3</PhysicalId>
      <PhysicalDimensions>
          <Height unit="centimetre">10.5</Height>
          <Length unit="centimetre">14.8</Length>
          <Thickness unit="micrometre">350</Thickness>
          <Weight unit="gram">3</Weight>
      </PhysicalDimensions>
      <Extent>1 carte imprimée</Extent>
      <Dimensions>10,5cm x 14,8cm</Dimensions>
      <Color>Noir et blanc</Color>
      <Framing>Paysage</Framing>
      <Technique>Phototypie</Technique>
  </PhysicalDataObject>
  <BinaryDataObject id="ID9">
      <DataObjectGroupId>ID0009</DataObjectGroupId>
      <DataObjectVersion>BinaryMaster</DataObjectVersion>
      <Uri>Content/1NUM_9.JPG</Uri>
      <MessageDigest algorithm="SHA-512">0e0cec05a1d72ee5610eaa5afbc904c012d190037cbc827d08272102cdecf0226efcad122b86e7699f767c661c9f3702379b8c2cb01c4f492f69deb200661bb9</MessageDigest>
      <Size>7702</Size>
      <FormatIdentification>
          <FormatLitteral>JPEG File Interchange Format</FormatLitteral>
          <MimeType>image/jpeg</MimeType>
          <FormatId>fmt/43</FormatId>
      </FormatIdentification>
      <FileInfo>
          <Filename>1NUM_9.JPG</Filename>
      </FileInfo>
      <Metadata>
          <Image>
              <Dimensions>117x76</Dimensions>
              <Width>117px</Width>
              <Height>76px</Height>
              <VerticalResolution>96ppp</VerticalResolution>
              <HorizontalResolution>96ppp</HorizontalResolution>
              <ColorDepth>24</ColorDepth>
          </Image>
      </Metadata>
  </BinaryDataObject>

Exemple de JSON stocké en base
==============================

Les champs présentés dans l'exemple ci-après ne font pas état de l'exhaustivité des champs disponibles dans le SEDA. Ceux-ci sont référencés dans la documentation SEDA disponible au lien suivant : https://redirect.francearchives.fr/seda/api_v2/doc.html

.. code-block:: json

  {
      "_id": "aebaaaaaaafgsz3wabcugak7ube6dxyaaabq",
      "_tenant": 0,
      "_profil": "Image",
      "FileInfo": {
          "Filename": "1NUM_9.JPG"
      },
      "_qualifiers": [
          {
              "qualifier": "PhysicalMaster",
              "_nbc": 1,
              "versions": [
                  {
                      "_id": "aeaaaaaaaafgsz3wabcugak7ube6dzqaaaca",
                      "DataObjectGroupId": "aebaaaaaaafgsz3wabcugak7ube6dxyaaabq",
                      "DataObjectVersion": "PhysicalMaster_1",
                      "PhysicalId": "1 Num 1/191-3",
                      "PhysicalDimensions": {
                          "Height": {
                              "unit": "centimetre",
                              "dValue": 10.5
                          },
                          "Length": {
                              "unit": "centimetre",
                              "dValue": 14.8
                          },
                          "Thickness": {
                              "unit": "micrometre",
                              "dValue": 350
                          },
                          "Weight": {
                              "unit": "gram",
                              "dValue": 3
                          }
                      },
                      "Extent": "1 carte imprimée",
                      "Dimensions": "10,5cm x 14,8cm",
                      "Color": "Noir et blanc",
                      "Framing": "Paysage",
                      "Technique": "Phototypie",
                      "_storage": {
                          "_nbc": 0,
                          "offerIds": [],
                          "strategyId": "default"
                      }
                  }
              ]
          },
          {
              "qualifier": "BinaryMaster",
              "_nbc": 1,
              "versions": [
                  {
                      "_id": "aeaaaaaaaafgsz3wabcugak7ube6dxyaaaba",
                      "DataObjectGroupId": "aebaaaaaaafgsz3wabcugak7ube6dxyaaabq",
                      "DataObjectVersion": "BinaryMaster_1",
                      "FormatIdentification": {
                          "FormatLitteral": "JPEG File Interchange Format",
                          "MimeType": "image/jpeg",
                          "FormatId": "fmt/43"
                      },
                      "FileInfo": {
                          "Filename": "1NUM_9.JPG"
                      },
                      "Metadata": {
                          "Image": {
                              "Dimensions": "117x76",
                              "Width": "117px",
                              "Height": "76px",
                              "VerticalResolution": "96ppp",
                              "HorizontalResolution": "96ppp",
                              "ColorDepth": 24
                          }
                      },
                      "Size": 7702,
                      "Uri": "Content/1NUM_9.JPG",
                      "MessageDigest": "0e0cec05a1d72ee5610eaa5afbc904c012d190037cbc827d08272102cdecf0226efcad122b86e7699f767c661c9f3702379b8c2cb01c4f492f69deb200661bb9",
                      "Algorithm": "SHA-512",
                      "_storage": {
                          "_nbc": 2,
                          "offerIds": [
                              "vitam-iaas-app-02.int",
                              "vitam-iaas-app-03.int"
                          ],
                          "strategyId": "default"
                      }
                  }
              ]
          }
      ],
      "_up": [
          "aeaqaaaaaafgsz3wabcugak7ube6d4qaaaaq"
      ],
      "_nbc": 0,
      "_ops": [
          "aedqaaaaachxqyktaai4aak7ube557iaaaaq"
      ],
      "_opi": "aedqaaaaachxqyktaai4aak7ube557iaaaaq",
      "_sp": "Vitam",
      "_sps": [
          "Vitam"
      ],
      "_storage": {
          "_nbc": 2,
          "offerIds": [
              "vitam-iaas-app-02.int",
              "vitam-iaas-app-03.int"
          ],
          "strategyId": "default"
      },
      "_v": 1
  }

Détail des champs du JSON
=========================

**"_id":** identifiant du groupe d'objets.
      
  * Il s'agit d'une chaîne de 36 caractères correspondant à un GUID.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1

**"_tenant":** identifiant du tenant.
      
  * Il s'agit d'un entier.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1

**"_profil":** catégorie de l'objet.
      
  * Repris du nom de la balise présente dans le bloc Metadata du DataObjectPackage présent dans le bordereau de transfert au niveau du BinaryMaster.

  Attention, il s'agit d'une reprise de la balise et non pas des valeurs à l'intérieur.
  Les valeurs possibles pour ce champ sont : Audio, Document, Text, Image et Video. Des extensions seront possibles (Database, Plan3D, ...).

  * Cardinalité : 1-1

**"FileInfo":** : informations sur le fichier constituant l'objet-données numérique de référence.

  * reprend le bloc FileInfo du BinaryMaster présent dans le bordereau de transfert.
  * L'objet de ce bloc est de pouvoir conserver les informations initiales du premier BinaryMaster (version de création), au cas où cette version serait éliminée (en application de règle de gestion).
  * Cardinalité : 1-1

**"_qualifiers":** tableau de structures décrivant les objets inclus dans ce groupe d'objets. Il est composé comme suit :

  - "qualifier": usage de l'objet.

    Ceci correspond à la valeur contenue dans le champ <DataObjectVersion> du bordereau. Par exemple pour <DataObjectVersion>BinaryMaster_1</DataObjectVersion>, c'est la valeur "BinaryMaster" qui est reportée.

    - "_nbc": nombre d'objets correspondant à cet usage.
    - "versions": tableau des objets par version (une version = une entrée dans le tableau).

      - "_id": identifiant de l'objet. Il s'agit d'une chaîne de 36 caractères corresppondant à un GUID, généré par la solution logicielle Vitam.
      - "DataObjectGroupId": identifiant du groupe d'objets, composé d'une chaîne de 36 caractères.
      - "DataObjectVersion": version de l'objet par rapport à son usage.

      Par exemple, si on a *BinaryMaster* sur l'usage, on aura au moins un objet *BinaryMaster_1*. Ces champs sont renseignés avec les valeurs récupérées dans les balises <DataObjectVersion> du bordereau de transfert.

      - "FormatIdentification": contient trois champs qui permettent d'identifier le format du fichier. Une vérification de la cohérence entre ce qui est déclaré dans le XML, ce qui existe dans le référentiel PRONOM et les valeurs que porte le document est faite.

        - "FormatLitteral" : nom du format. C'est une reprise de la valeur située entre les balises <FormatLitteral> du message ArchiveTransfer.
        - "MimeType" : type Mime. C'est une reprise de la valeur située entre les balises <MimeType> du message ArchiveTransfer ou des valeurs correspondant au format tel qu'identifié par la solution logicielle Vitam.
        - "FormatId" : PUID du format de l'objet. Il est défini par la solution logicielle Vitam à l'aide du référentiel PRONOM maintenu par The National Archives (UK) et correspondant à la valeur du champ PUID de la collection FileFormat.

      - "FileInfo": Contient les informations sur les fichiers.
          
          - "Filename": nom de l'objet.
          - "CreatingApplicationName": nom de l'application avec laquelle l'objet a été créé. Ce champ est renseigné avec la métadonnée correspondante portée par le message ArchiveTransfer. *Ce champ est facultatif et n'est pas présent systématiquement*.
          - "CreatingApplicationVersion": numéro de version de l'application avec laquelle le document a été créé. Ce champ est renseigné avec la métadonnée correspondante portée par le message ArchiveTransfer. *Ce champ est facultatif et n'est pas présent systématiquement*.
          - "CreatingOs": système d'exploitation avec lequel l'objet a été créé. Ce champ est renseigné avec la métadonnée correspondante portée par le message ArchiveTransfer. *Ce champ est facultatif et n'est pas présent systématiquement*.
          - "CreatingOsVersion": Version du système d'exploitation avec lequel l'objet a été créé. Ce champ est renseigné avec la métadonnée correspondante portée par le message ArchiveTransfer. *Ce champ est facultatif et n'est pas présent systématiquement*.
          - "LastModified" : date de dernière modification de l'objet au format ISO 8601 YYY-MM-DD + 'T' + hh:mm:ss.millisecondes "+" timezone hh:mm. ``Exemple : 2016-08-19T16:36:07.942+02:00`` Ce champ est optionnel, et est renseigné avec la métadonnée correspondante portée par le fichier.
          - "Size": taille de l'objet (en octet). Ce champ contient un nombre entier.

      - "PhysicalDimensions" : Ce champ contient les différentes informations concernant un objet physique (DataObjectVersion = PhysicalMaster). Il pourra donner des informations sur la taille, le poids, etc... de l'objet.
         
         - "Width" : largeur de l'objet. Ce champ contient 2 sous champs : "unit" (string) et "dValue" (double)
         - "Height" : hauteur de l'objet. Ce champ contient 2 sous champs : "unit" (string) et "dValue" (double)
         - "Depth" : profondeur de l'objet. Ce champ contient 2 sous champs : "unit" (string) et "dValue" (double)         
         - "Diameter" : diamètre de l'objet. Ce champ contient 2 sous champs : "unit" (string) et "dValue" (double)
         - "Length" : longueur de l'objet. Ce champ contient 2 sous champs : "unit" (string) et "dValue" (double)
         - "Thickness" : épaisseur de l'objet. Ce champ contient 2 sous champs : "unit" (string) et "dValue" (double)
         - "Weight" : poids de l'objet. Ce champ contient 2 sous champs : "unit" (string) et "dValue" (double)
         - "Shape" : forme de l'objet. Ce champ contient est de type String      
      - "OtherMetadata": Ce champ est renseigné avec les valeurs contenues entre les balises <OtherMetadata>, de l'une extension du schéma SEDA du message  ArchiveTransfer.
          
         
      - "Uri": localisation du fichier correspondant à l'objet dans le SIP.

        Chaîne de caractères

      - "MessageDigest": empreinte du fichier correspondant à l'objet. La valeur est calculée par la solution logicielle Vitam.

        Chaîne de caractères

      - "Algorithm": algorithme utilisé pour réaliser l'empreinte du fichier correspondant à l'objet.

        Chaîne de caractères

      - "_storage": contient trois champs qui permettent d'identifier les offres  de stockage.
          
          - "strategyId": identifiant de la stratégie de stockage.
          - "offerIds": liste des offres de stockage pour une stratégie donnée
          - "_nbc": nombre d'offres.

**"_up" (unit up):** tableau identifiant les unités archivistiques représentée par ce groupe d'objets.
        
  * Il s'agit d'un tableau de chaînes de 36 caractères correspondant au GUID contenu dans le champ _id des unités archivistiques enregistrées dans la collection Unit.
  * Champ peuplé par la solution logicielle Vitam.
  * Ne peut être vide
  * Cardinalité : 1-1

**"_nbc" (nbobjects):** nombre d'objets dans le groupe d'objets.
        
  * Il s'agit d'un entier.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1

**"_ops" (operations):** tableau des identifiants d'opérations auxquelles ce groupe d'objets a participé.
        
  * Il s'agit d'un tableau de chaînes de 36 caractères correspondant au GUID contenu dans le champ _id d'opération enregistré dans la collection LogBookOperation.
  * Champ peuplé par la solution logicielle Vitam.
  * Ne peut être vide
  * Cardinalité : 1-1

**"_opi" :** identifiant de l'opération à l'origine de la création de ce GOT.
        
  * Il s'agit d'une chaînes de 36 caractères correspondant au GUID contenue dans le champ _id de la collection LogBookOperation.
  * Champ peuplé par la solution logicielle Vitam.
  * Ne peut être vide
  * Cardinalité : 1-1

**"_sp":** service producteur déclaré dans le message ArchiveTransfer (OriginatingAgencyIdentifier)
        
  * Il s'agit d'une chaîne de caractères.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1

**"_sps":** service producteur d'origine déclaré lors de la prise en charge du groupe d'objets par la solution logicielle Vitam.
        
  * Il s'agit d'un tableau contenant tous les services producteurs référençant le groupe d'objets.    
  * Il s'agit d'un tableau de chaînes de caractères.
  * Champ peuplé par la solution logicielle Vitam.
  * Ne peut être vide
  * Cardinalité : 1-1

**"_v":** version de l'enregistrement décrit.
        
  * Il s'agit d'un entier.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1
  * 0 correspond à l'enregistrement d'origine. Si le numéro est supérieur à 0, alors il s'agit du numéro de version de l'enregistrement.