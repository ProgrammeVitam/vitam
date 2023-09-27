# Signature électronique - Cookbook

Le présent repository décrit la modélisation des documents signés numériquement au format SEDA 2.3+ en vue de leur
archivage.

## Généralités

La version 2.3, la norme SEDA prévoit de pouvoir décrire les différents types de document signé numériquement, ainsi que
les éventuels documents annexes à la signature électronique.

La norme distingue 4 types de **rôle** (ou **étiquette**) :

- **Le document signé** : Il s'agit de tout contenu numérique, tel un rapport textuel, un contrat au format PDF, ou un
  document xml, qui fait l'objet d'une signature numérique.

- **La signature numérique** ou **signature électronique** : Il s'agit de l'équivalent numérique d'une "signature
  manuscrite" ou d'un "cachet" qui atteste de l'authenticité d'un document numérique et de l'identité de la personne ou
  de l'entité qui l'a signé. Elle est générée à l'aide de méthodes cryptographiques la rendant quasi-infalsifiable.

- **Horodatage** : Il s'agit d'un marqueur temporel appliqué à un document numérique. Cela permet de prouver l'existence
  du contenu numérique à une date/heure spécifique. L'horodatage implique l'ajout d'une empreinte cryptographique
  temporelle fiable et vérifiable, typiquement créée par une source de confiance.

- **Preuves complémentaires** : Ensemble des données renforçant la crédibilité de la signature numérique. Elles peuvent
  inclure des informations diverses sur le contexte de signature telles que l'adresse IP de l'appareil utilisé pour
  signer, preuves de connexion ou d'authentification, version technique du dispositif de signature ou de l'outil
  validant la signature...etc. Ces données sont typiquement dans un format non standardisé selon l'outil qui les
  génère.

Un seul objet binaire peut porter plusieurs rôles à la fois. Il peut par exemple contenir le document signé, une ou
plusieurs signatures numériques ainsi qu'un ou plusieurs horodatages dans un même binaire au format PDF ou XML.

À contrario, il est également possible que les rôles de signature puissent être portés par des binaires distincts. Par
exemple, un premier objet binaire contient le document signé, et un second contient la signature effective. On parle
alors de *signature détachée*.

## Prise en charge de la signature électronique dans la norme SEDA

> **Important** : Les versions `2.1` et `2.2` de la norme SEDA prévoient une balise `<Signature>` (au sein
> du `BaseObjectGroup`). Cette balise est dépréciée et supprimée à partir de la version 2.3 de la norme.
>
>  L'utilisation de la balise `<Signature>` est **fortement déconseillée**.

> **Important** : Vitam ne prévoit pas de migration ou de reprise de données pour les anciennes balises `<Signature>`

> **Important** : La description des informations de signature est purement déclarative. Vitam ne fait **AUCUN**
> contrôle de cohérence structurelle (ex. Hiérarchie des unités d'archives détachées), ou de validité technique
> (ex. format de signature, validation cryptographique de la signature, date de signature ou d'horodatage...etc.).

La norme SEDA 2.3 permet de modéliser les informations de signature électronique via une nouvelle balise englobante
`<SigningInformation>` qui remplace la balise `<Signature>` des précédentes versions du SEDA :

```mermaid
classDiagram
    SigningInformation --> SigningRoleType
    SigningInformation --> DetachedSigningRoleType
    SigningInformation --> SignatureDescriptionType
    SigningInformation --> TimestampingInformationType
    SigningInformation --> AdditionalProofType
    SignatureDescriptionType --> SignerType
    SignatureDescriptionType --> ValidatorType
    class SigningInformation {
        [SigningRoleType] SigningRole [1..n]
        [DetachedSigningRoleType] DetachedSigningRole [0..n]
        [xsd:IDREF] SignedDocumentReferenceId [0..n] - Unsupported by Vitam*
        [SignatureDescriptionType] SignatureDescription [0..n]
        [TimestampingInformationType] TimestampingInformation [0..n]
        [AdditionalProofType] AdditionalProof [0..n]
        [seda:OpenType] Extended [0..1]
    }
    class SigningRoleType {
         <<enumeration>>
         SignedDocument 
         Signature
         Timestamp
         AdditionalProof
    }
    class DetachedSigningRoleType {
         <<enumeration>>
         Signature
         Timestamp
         AdditionalProof
    }
    class SignatureDescriptionType {
        [SignerType] Signer [0..1]
        [ValidatorType] Validator [0..1]
        [NonEmptyTokenType] SigningType [0..1]
    }
    class SignerType {
        [seda:PersonOrEntityGroup]
        [xsd:dateTime] SigningTime [1..1]
        [seda:BusinessGroup]
    }
    class ValidatorType {
        [seda:PersonOrEntityGroup]
        [xsd:dateTime] ValidationTime [1..1]
        [seda:BusinessGroup]
    }
    class TimestampingInformationType {
        [xsd:dateTime] TimeStamp [0..1]
        [NonEmptyTokenType] AdditionalTimestampingInformation [0..1]
    }
    class AdditionalProofType {
        [NonEmptyTokenType] AdditionalProofInformation [0..n]
    }
```

La balise `<SigningInformation>` permet de décrire les informations de signature de l'objet binaire associé à l'unité
d'archive déclarante :

- `<SigningRole>` : Décrit le ou les rôles de signature de l'objet binaire. Ces rôles peuvent être combinés, mais pas ne
  doivent pas être répétés (pas de doublons) :
    - `SignedDocument` : Le binaire contient le document signé
    - `Signature` : Le binaire contient une ou plusieurs signatures
    - `Timestamp` : Le binaire contient un ou plusieurs données d'horodatage
    - `AdditionalProof` : Le binaire contient des preuves complémentaires.

- `<DetachedSigningRole>` : Décrit les éventuels rôles détachés relatifs au présent binaire, au sein d'un ou plusieurs
  autres objets binaires annexes. Ces rôles peuvent être combinés, mais pas ne doivent pas être répétés (pas de
  doublons) :
    - `Signature` : Une signature détachée du présent binaire est présente dans un binaire annexe.
    - `Timestamp` : Un horodatage détaché du présent binaire est présente dans un binaire annexe.
    - `AdditionalProof` : Des preuves complémentaires de signature du présent binaire sont présentes dans un binaire
      annexe

- `<SignedDocumentReferenceId>` : Référence technique (Id XML) de l'unité archivistique « racine » (qui porte le
  `SigningRole` de `SignedDocument`). Ce champ est **non supporté par Vitam** et est **ignoré** lors du processus de
  versement.

- `<SignatureDescription>` : Balise décrivant une ou plusieurs signatures. Cette balise est typiquement définie lorsque le
  champ `SigningRole` prend la valeur `Signature` pour décrire la ou les signatures définies dans le présent binaire.
  Optionnellement, elle peut également être utilisée lorsque le `DetachedSigningRole` prend la valeur `Signature`
  (l'unité d'archives déclarante décrit également des informations de signature détachée redondées ici à des fins
  d'indexation).

    - `<Signer>` : Balise décrivant l’identité du signataire qu’il s’agisse d’une personne physique ou
      morale (`FirstName`, `LastName`, `Corpname`, `Activity`, `Role`...), ainsi qu'une date et heure de
      signature `SigningTime`

    - `<Validator>` : Balise décrivant l’identité du validateur qu’il s’agisse d’une personne physique ou
      morale (`FirstName`, `LastName`, `Corpname`, `Activity`, `Role`...), ainsi qu'une date et heure de validation de
      la signature `ValidationTime`

    - `<SigningType>`: Décrit le type de signature, au sens juridique du terme. Par exemple, simple, avancée, qualifiée.

- `<TimestampingInformation>` : Balise décrivant le ou les horodatages. Cette balise est typiquement définie lorsque le
  champ `SigningRole` prend la valeur `Timestamp` pour décrire le ou les horodatage(s) définis dans le présent binaire.
  Optionnellement, elle peut également être utilisée lorsque le `DetachedSigningRole` prend la valeur `Timestamp`
  (l'unité d'archives déclarante décrit également des informations d'horodatage détaché redondées ici à des fins
  d'indexation).

    - `<TimeStamp>` : Date et heure d'horodatage
    - `<AdditionalTimestampingInformation>` : Champ textuel optionnel décrivant des informations complémentaires sur
      l'horodatage.

- `<AdditionalProof>` : Bloc permettant de conserver les preuves complémentaires dans un contexte de signature.

    - `<AdditionalProofInformation>` : Champ textuel optionnel décrivant des informations complémentaires sur
      l'horodatage.

- `<Extended>` : Permet d'enrichir les informations de signature avec des champs libres d'extension.

> **Important** : Afin de simplifier le versement d'archives avec signature électronique, les versions `7.0+` de Vitam
> supportent le versement d'unités d'archives avec la balise `<SigningInformation>` au sein d'archives SIP aux formats
> `2.1` et `2.2` de la norme SEDA, en tant que champ libre (extension).
>
> À noter cependant que dans ce cas, la balise `<SigningInformation>` est mutuellement incompatible avec les balises
> `<Signature>`, `<GPS>`, `<OriginatingSystemIdReplyTo>` et `<TextContent>`.
> Aussi, si plusieurs champs libres (extensions) sont présents, la balise `<SigningInformation>` doit être la
> première de la liste.
>
> Vitam recommande cependant l'utilisation de la version `2.3` de la norme SEDA, notamment en présence des balises
> `<SigningInformation>`.

## Principes de modélisation

Le présent cookbook est livré avec plusieurs exemples de SIP de modélisation de signature électroniques.

### Cas 1 - Document simple embarquant une signature et un horodatage

Dans le cas d'un binaire simple sans annexes (sans autres binaires complémentaires de signature, d'horodatage ou de
preuves complémentaires détachés), la modélisation est triviale :

- Un objet binaire décrit au sein d'une balise `<BinaryDataObject>` ordinaire (BinaryMaster_1)
- Une unité d'archive `<ArchiveUnit>` décrivant le binaire et les informations de signature le concernant au sein d'une
  balise `<SigningInformation>`

Exemple de document signé avec signature et horodatage embarqués :

```xml

<ArchiveTransfer>
    <!-- ... -->
    <DataObjectPackage>

        <!-- An Object Group with signed and timestamped document to archive (BinaryMaster_1) -->
        <DataObjectGroup id="ID10">
            <BinaryDataObject id="ID11">
                <DataObjectVersion>BinaryMaster_1</DataObjectVersion>
                <Uri>content/ID11.pdf</Uri>
                <!-- ... -->
            </BinaryDataObject>
        </DataObjectGroup>

        <DescriptiveMetadata>
            <ArchiveUnit id="ID12">
                <Content>
                    <DescriptionLevel>Item</DescriptionLevel>
                    <Title>Document</Title>
                    <Description>Signed Document with embedded Signature and TimeStamp</Description>
                    <!-- Other descriptive fields... -->

                    <!-- Signing information -->
                    <SigningInformation>

                        <SigningRole>SignedDocument</SigningRole>
                        <SigningRole>Signature</SigningRole>
                        <SigningRole>Timestamp</SigningRole>

                        <SignatureDescription>
                            <Signer>
                                <FullName>Caroline DISTRIQUIN</FullName>
                                <SigningTime>2023-01-27T10:54:54+01:00</SigningTime>
                            </Signer>
                        </SignatureDescription>
                        <TimestampingInformation>
                            <TimeStamp>2023-01-27T10:54:54+01:00</TimeStamp>
                        </TimestampingInformation>
                    </SigningInformation>
                </Content>

                <!-- The Archive Unit references the Object Group Id of the binary object (BinaryMaster_1) -->
                <DataObjectReference>
                    <DataObjectGroupReferenceId>ID10</DataObjectGroupReferenceId>
                </DataObjectReference>

            </ArchiveUnit>
        </DescriptiveMetadata>
        <!-- ... -->
    </DataObjectPackage>
    <!-- ... -->
</ArchiveTransfer>
```

### Cas 2 - Archive ZIP contenant le document signé et horodaté ainsi que ses preuves complémentaires

Ce cas démontre le cas où un binaire contenant à la fois le document signé et horodaté, ainsi que les preuves de
signature (XML, PDF et annexes), le tout packagé dans un seul binaire de type archive ZIP. La modélisation est similaire
au [cas 1](#cas-1---document-simple-embarquant-une-signature-et-un-horodatage)

- Un objet binaire décrit au sein d'une balise `<BinaryDataObject>` ordinaire
- Une unité d'archive `<ArchiveUnit>` décrivant le binaire et les informations de signature le concernant au sein d'une
  balise `<SigningInformation>`

```xml

<ArchiveTransfer>
    <!-- ... -->
    <DataObjectPackage>

        <!-- An Object Group defined the binary archive (BinaryMaster_1) -->
        <DataObjectGroup id="ID10">
            <BinaryDataObject id="ID11">
                <DataObjectVersion>BinaryMaster_1</DataObjectVersion>
                <Uri>content/ID11.zip</Uri>
                <!-- ... -->
            </BinaryDataObject>
        </DataObjectGroup>

        <DescriptiveMetadata>
            <ArchiveUnit id="ID12">
                <Content>
                    <DescriptionLevel>Item</DescriptionLevel>
                    <Title>Document</Title>
                    <Description>Archive containing the signed and timestamped document and additional proofs
                    </Description>
                    <!-- Other descriptive fields... -->

                    <!-- Signing information -->
                    <SigningInformation>

                        <SigningRole>SignedDocument</SigningRole>
                        <SigningRole>Signature</SigningRole>
                        <SigningRole>Timestamp</SigningRole>
                        <SigningRole>AdditionalProof</SigningRole>
                        <SignatureDescription>
                            <Signer>
                                <FullName>Caroline DISTRIQUIN</FullName>
                                <SigningTime>2023-01-27T10:54:54+01:00</SigningTime>
                            </Signer>
                        </SignatureDescription>
                        <TimestampingInformation>
                            <TimeStamp>2023-01-27T10:54:54+01:00</TimeStamp>
                        </TimestampingInformation>

                        <!-- Proof details -->
                        <AdditionalProof>
                            <AdditionalProofInformation>Additional proof: Appendix 1</AdditionalProofInformation>
                            <AdditionalProofInformation>Additional proof: Appendix 2</AdditionalProofInformation>
                            <AdditionalProofInformation>Additional proof: XML</AdditionalProofInformation>
                            <AdditionalProofInformation>Additional proof: PDF</AdditionalProofInformation>
                        </AdditionalProof>
                    </SigningInformation>
                </Content>

                <!-- The Archive Unit references the Object Group Id of the binary object (BinaryMaster_1) -->
                <DataObjectReference>
                    <DataObjectGroupReferenceId>ID10</DataObjectGroupReferenceId>
                </DataObjectReference>

            </ArchiveUnit>
        </DescriptiveMetadata>
        <!-- ... -->
    </DataObjectPackage>
    <!-- ... -->
</ArchiveTransfer>
```

### Cas 3 - Binaires multiples

Dans le cas d'un document signé accompagné de binaires détachés (Ex : signatures détachées, preuves complémentaires
détachées...), les binaires sont déclarés dans des groupes d'objets distincts, et décrits via dans une arborescence
d'unités d'archives.

Ainsi, pour décrire par exemple un document signé, ainsi qu'un ensemble de binaires de preuves complémentaires, la
modélisation serait ainsi :

- Objet binaire représentant le document signé décrit au sein d'une balise `<BinaryDataObject>` (BinaryMaster_1)
- Autres objets binaires de preuves complémentaires également décrits via des balises `<BinaryDataObject>`
  (BinaryMaster_1)
- Une unité d'archives « racine » `<ArchiveUnit>` décrit le document signé via la balise `<SigningInformation>`.
  Elle déclare également une balise `<DetachedSigningRole>` avec pour valeur `AdditionalProof` pour indiquer la présence
  de binaires détachés de type preuves complémentaires.
- Des unités d'archives « filles » `<ArchiveUnit>`, rattachées **directement** à l'unité racine, décrivent les
  différents documents détachés liés au document signé, via des balises `<SigningInformation>`.

```xml

<ArchiveTransfer>
    <!-- ... -->
    <DataObjectPackage>

        <!-- Signed document -->
        <DataObjectGroup id="ID10">
            <BinaryDataObject id="ID11">
                <DataObjectVersion>BinaryMaster_1</DataObjectVersion>
                <Uri>content/ID11.pdf</Uri>
                <!-- ... -->
            </BinaryDataObject>
        </DataObjectGroup>


        <!-- Additional proofs (detached) -->
        <DataObjectGroup id="ID17">
            <BinaryDataObject id="ID23">
                <DataObjectVersion>BinaryMaster_1</DataObjectVersion>
                <Uri>content/ID23.xml</Uri>
                <!-- ... -->
            </BinaryDataObject>
        </DataObjectGroup>
        <DataObjectGroup id="ID18">
            <BinaryDataObject id="ID21">
                <DataObjectVersion>BinaryMaster_1</DataObjectVersion>
                <Uri>content/ID21.pdf</Uri>
                <!-- ... -->
            </BinaryDataObject>
        </DataObjectGroup>
        <DataObjectGroup id="ID19">
            <BinaryDataObject id="ID22">
                <DataObjectVersion>BinaryMaster_1</DataObjectVersion>
                <Uri>content/ID22.pdf</Uri>
                <!-- ... -->
            </BinaryDataObject>
        </DataObjectGroup>
        <DataObjectGroup id="ID20">
            <BinaryDataObject id="ID24">
                <DataObjectVersion>BinaryMaster_1</DataObjectVersion>
                <Uri>content/ID24.pdf</Uri>
                <!-- ... -->
            </BinaryDataObject>
        </DataObjectGroup>

        <DescriptiveMetadata>
            <!-- Root archive unit referencing the signed document -->
            <ArchiveUnit id="ID12">
                <Content>
                    <DescriptionLevel>Item</DescriptionLevel>
                    <Title>Document</Title>
                    <Description>Signed Document with embedded Signature and TimeStamp</Description>
                    <!-- Other descriptive fields... -->

                    <!-- Signing information -->
                    <SigningInformation>

                        <SigningRole>SignedDocument</SigningRole>
                        <SigningRole>Signature</SigningRole>
                        <SigningRole>Timestamp</SigningRole>

                        <!-- Additional proof binaries are "detached" -->
                        <DetachedSigningRole>AdditionalProof</DetachedSigningRole>
                        <SignatureDescription>
                            <Signer>
                                <FullName>Caroline DISTRIQUIN</FullName>
                                <SigningTime>2023-01-27T10:54:54+01:00</SigningTime>
                            </Signer>
                        </SignatureDescription>
                        <TimestampingInformation>
                            <TimeStamp>2023-01-27T10:54:54+01:00</TimeStamp>
                        </TimestampingInformation>

                    </SigningInformation>
                </Content>

                <!-- Child archive units referencing detached binaries -->
                <ArchiveUnit id="ID16">
                    <Content>
                        <DescriptionLevel>Item</DescriptionLevel>
                        <Title>Additional proof: Appendix 2</Title>
                        <SigningInformation>
                            <SigningRole>AdditionalProof</SigningRole>
                            <AdditionalProof>
                                <AdditionalProofInformation>Additional proof: Appendix 2</AdditionalProofInformation>
                            </AdditionalProof>
                        </SigningInformation>
                    </Content>
                    <DataObjectReference>
                        <DataObjectGroupReferenceId>ID19</DataObjectGroupReferenceId>
                    </DataObjectReference>
                </ArchiveUnit>
                <ArchiveUnit id="ID15">
                    <Content>
                        <DescriptionLevel>Item</DescriptionLevel>
                        <Title>Additional proof: Appendix 1</Title>
                        <SigningInformation>
                            <SigningRole>AdditionalProof</SigningRole>
                            <AdditionalProof>
                                <AdditionalProofInformation>Additional proof: Appendix 1</AdditionalProofInformation>
                            </AdditionalProof>
                        </SigningInformation>
                    </Content>
                    <DataObjectReference>
                        <DataObjectGroupReferenceId>ID20</DataObjectGroupReferenceId>
                    </DataObjectReference>
                </ArchiveUnit>
                <ArchiveUnit id="ID13">
                    <Content>
                        <DescriptionLevel>Item</DescriptionLevel>
                        <Title>Additional proof: PDF</Title>
                        <SigningInformation>
                            <SigningRole>AdditionalProof</SigningRole>
                            <AdditionalProof>
                                <AdditionalProofInformation>Additional proof: PDF</AdditionalProofInformation>
                            </AdditionalProof>
                        </SigningInformation>
                    </Content>
                    <DataObjectReference>
                        <DataObjectGroupReferenceId>ID18</DataObjectGroupReferenceId>
                    </DataObjectReference>
                </ArchiveUnit>
                <ArchiveUnit id="ID14">
                    <Content>
                        <DescriptionLevel>Item</DescriptionLevel>
                        <Title>Additional proof: XML</Title>
                        <SigningInformation>
                            <SigningRole>AdditionalProof</SigningRole>
                            <AdditionalProof>
                                <AdditionalProofInformation>Additional proof: XML</AdditionalProofInformation>
                            </AdditionalProof>
                        </SigningInformation>
                    </Content>
                    <DataObjectReference>
                        <DataObjectGroupReferenceId>ID17</DataObjectGroupReferenceId>
                    </DataObjectReference>
                </ArchiveUnit>

                <!-- The root Archive Unit references the Object Group Id of the signed document (BinaryMaster_1) -->
                <DataObjectReference>
                    <DataObjectGroupReferenceId>ID10</DataObjectGroupReferenceId>
                </DataObjectReference>

            </ArchiveUnit>
        </DescriptiveMetadata>
        <!-- ... -->
    </DataObjectPackage>
    <!-- ... -->
</ArchiveTransfer>
```

### Cas 4 - Binaires multiples avec arborescence râteau

Dans le cas d'un document signé accompagné de binaires détachés, les unités d'archives peuvent être modélisées en mode
« râteau ».

La modélisation est similaire au [cas 3](#cas-3---binaires-multiples), seulement, les unités d'archives « filles » sont
déclarées à la racine de la balise `<DescriptiveMetadata>`, et sont référencées via la balise `<ArchiveUnitRefId>` au
niveau de l'unité d'archives « racine ».

```xml

<ArchiveTransfer>
    <!-- ... -->
    <DataObjectPackage>

        <!-- Signed document -->
        <DataObjectGroup id="ID10">
            <BinaryDataObject id="ID11">
                <DataObjectVersion>BinaryMaster_1</DataObjectVersion>
                <Uri>content/ID11.pdf</Uri>
                <!-- ... -->
            </BinaryDataObject>
        </DataObjectGroup>


        <!-- Additional proofs (detached) -->
        <DataObjectGroup id="ID17">
            <BinaryDataObject id="ID23">
                <DataObjectVersion>BinaryMaster_1</DataObjectVersion>
                <Uri>content/ID23.xml</Uri>
                <!-- ... -->
            </BinaryDataObject>
        </DataObjectGroup>
        <DataObjectGroup id="ID18">
            <BinaryDataObject id="ID21">
                <DataObjectVersion>BinaryMaster_1</DataObjectVersion>
                <Uri>content/ID21.pdf</Uri>
                <!-- ... -->
            </BinaryDataObject>
        </DataObjectGroup>
        <DataObjectGroup id="ID19">
            <BinaryDataObject id="ID22">
                <DataObjectVersion>BinaryMaster_1</DataObjectVersion>
                <Uri>content/ID22.pdf</Uri>
                <!-- ... -->
            </BinaryDataObject>
        </DataObjectGroup>
        <DataObjectGroup id="ID20">
            <BinaryDataObject id="ID24">
                <DataObjectVersion>BinaryMaster_1</DataObjectVersion>
                <Uri>content/ID24.pdf</Uri>
                <!-- ... -->
            </BinaryDataObject>
        </DataObjectGroup>

        <DescriptiveMetadata>
            <!-- Root archive unit referencing the signed document -->
            <ArchiveUnit id="ID12">
                <Content>
                    <DescriptionLevel>Item</DescriptionLevel>
                    <Title>Document</Title>
                    <Description>Signed Document with embedded Signature and TimeStamp</Description>
                    <SigningInformation>
                        <SigningRole>SignedDocument</SigningRole>
                        <SigningRole>Signature</SigningRole>
                        <SigningRole>Timestamp</SigningRole>

                        <!-- Additional proof binaries are "detached" -->
                        <DetachedSigningRole>AdditionalProof</DetachedSigningRole>
                        <SignatureDescription>
                            <Signer>
                                <FullName>Caroline DISTRIQUIN</FullName>
                                <SigningTime>2023-01-27T10:54:54+01:00</SigningTime>
                            </Signer>
                        </SignatureDescription>
                        <TimestampingInformation>
                            <TimeStamp>2023-01-27T10:54:54+01:00</TimeStamp>
                        </TimestampingInformation>

                    </SigningInformation>
                </Content>

                <!-- Reference to "child" archive units -->
                <ArchiveUnit id="ID121">
                    <ArchiveUnitRefId>ID16</ArchiveUnitRefId>
                </ArchiveUnit>
                <ArchiveUnit id="ID122">
                    <ArchiveUnitRefId>ID15</ArchiveUnitRefId>
                </ArchiveUnit>
                <ArchiveUnit id="ID123">
                    <ArchiveUnitRefId>ID13</ArchiveUnitRefId>
                </ArchiveUnit>
                <ArchiveUnit id="ID124">
                    <ArchiveUnitRefId>ID14</ArchiveUnitRefId>
                </ArchiveUnit>

                <!-- The root Archive Unit references the Object Group Id of the signed document (BinaryMaster_1) -->
                <DataObjectReference>
                    <DataObjectGroupReferenceId>ID10</DataObjectGroupReferenceId>
                </DataObjectReference>
            </ArchiveUnit>

            <!-- Child archive units referencing detached binaries -->
            <ArchiveUnit id="ID16">
                <Content>
                    <DescriptionLevel>Item</DescriptionLevel>
                    <Title>Additional proof: Appendix 2</Title>
                    <SigningInformation>
                        <SigningRole>AdditionalProof</SigningRole>
                        <AdditionalProof>
                            <AdditionalProofInformation>Additional proof: Appendix 2</AdditionalProofInformation>
                        </AdditionalProof>
                    </SigningInformation>
                </Content>
                <DataObjectReference>
                    <DataObjectGroupReferenceId>ID19</DataObjectGroupReferenceId>
                </DataObjectReference>
            </ArchiveUnit>
            <ArchiveUnit id="ID15">
                <Content>
                    <DescriptionLevel>Item</DescriptionLevel>
                    <Title>Additional proof: Appendix 1</Title>
                    <SigningInformation>
                        <SigningRole>AdditionalProof</SigningRole>
                        <AdditionalProof>
                            <AdditionalProofInformation>Additional proof: Appendix 1</AdditionalProofInformation>
                        </AdditionalProof>
                    </SigningInformation>
                </Content>
                <DataObjectReference>
                    <DataObjectGroupReferenceId>ID20</DataObjectGroupReferenceId>
                </DataObjectReference>
            </ArchiveUnit>
            <ArchiveUnit id="ID13">
                <Content>
                    <DescriptionLevel>Item</DescriptionLevel>
                    <Title>Additional proof: PDF</Title>
                    <SigningInformation>
                        <SigningRole>AdditionalProof</SigningRole>
                        <AdditionalProof>
                            <AdditionalProofInformation>Additional proof: PDF</AdditionalProofInformation>
                        </AdditionalProof>
                    </SigningInformation>
                </Content>
                <DataObjectReference>
                    <DataObjectGroupReferenceId>ID18</DataObjectGroupReferenceId>
                </DataObjectReference>
            </ArchiveUnit>
            <ArchiveUnit id="ID14">
                <Content>
                    <DescriptionLevel>Item</DescriptionLevel>
                    <Title>Additional proof: XML</Title>
                    <SigningInformation>
                        <SigningRole>AdditionalProof</SigningRole>
                        <AdditionalProof>
                            <AdditionalProofInformation>Additional proof: XML</AdditionalProofInformation>
                        </AdditionalProof>
                    </SigningInformation>
                </Content>
                <DataObjectReference>
                    <DataObjectGroupReferenceId>ID17</DataObjectGroupReferenceId>
                </DataObjectReference>

            </ArchiveUnit>
        </DescriptiveMetadata>
        <!-- ... -->
    </DataObjectPackage>
    <!-- ... -->
</ArchiveTransfer>
```

### Cas 5 - Duplication des informations de signature dans l'unité d'archive racine

Dans le cas d'un document signé accompagné de binaires détachés, il est possible de recopier/dupliquer les
informations de signature des unités d'archives « filles » au niveau de l'unité d'archives « racine ».

Ceci permet de décrire et d'indexer au sein d'une seule unité d'archives racine l'ensemble des informations de signature
et de simplifier certains types de requêtes.

> **Important :** Il est à noter que la duplication d'informations peut causer des incohérences dans la description des
> archives si des modifications partielles sont réalisées.
>
> Ceci rajoute également un surcoût de stockage pour l'indexation des données.

Ainsi, pour décrire par exemple un document signé, ainsi qu'un ensemble de binaires de preuves complémentaires avec
duplication des informations de signature dans l'unité racine, la modélisation serait ainsi :

- Objet binaire représentant le document signé portant la signature et l'horodatage décrit au sein d'une
  balise `<BinaryDataObject>` (BinaryMaster_1)
- Autres objets binaires de preuves complémentaires également décrits via des balises `<BinaryDataObject>`
  (BinaryMaster_1)
- Une unité d'archives « racine » `<ArchiveUnit>` décrit le document racine via la balise `<SigningInformation>`.
  Elle déclare également une balise `<DetachedSigningRole>` avec pour valeur `AdditionalProof` pour indiquer la présence
  de binaires détachés de type preuves complémentaires. Enfin, l'unité d'archives duplique également les informations
  des preuves complémentaires via une balise `<AdditionalProof>`.
- Des unités d'archives « filles » `<ArchiveUnit>`, rattachées à l'unité racine, décrivent les différents documents
  détachés liés au document signé, via des balises `<SigningInformation>`.

```xml

<ArchiveTransfer>
    <!-- ... -->
    <DataObjectPackage>

        <!-- Signed document -->
        <DataObjectGroup id="ID10">
            <BinaryDataObject id="ID11">
                <DataObjectVersion>BinaryMaster_1</DataObjectVersion>
                <Uri>content/ID11.pdf</Uri>
                <!-- ... -->
            </BinaryDataObject>
        </DataObjectGroup>


        <!-- Additional proofs (detached) -->
        <DataObjectGroup id="ID17">
            <BinaryDataObject id="ID23">
                <DataObjectVersion>BinaryMaster_1</DataObjectVersion>
                <Uri>content/ID23.xml</Uri>
                <!-- ... -->
            </BinaryDataObject>
        </DataObjectGroup>
        <DataObjectGroup id="ID18">
            <BinaryDataObject id="ID21">
                <DataObjectVersion>BinaryMaster_1</DataObjectVersion>
                <Uri>content/ID21.pdf</Uri>
                <!-- ... -->
            </BinaryDataObject>
        </DataObjectGroup>
        <DataObjectGroup id="ID19">
            <BinaryDataObject id="ID22">
                <DataObjectVersion>BinaryMaster_1</DataObjectVersion>
                <Uri>content/ID22.pdf</Uri>
                <!-- ... -->
            </BinaryDataObject>
        </DataObjectGroup>
        <DataObjectGroup id="ID20">
            <BinaryDataObject id="ID24">
                <DataObjectVersion>BinaryMaster_1</DataObjectVersion>
                <Uri>content/ID24.pdf</Uri>
                <!-- ... -->
            </BinaryDataObject>
        </DataObjectGroup>

        <DescriptiveMetadata>
            <!-- Root archive unit referencing the signed document -->
            <ArchiveUnit id="ID12">
                <Content>
                    <DescriptionLevel>Item</DescriptionLevel>
                    <Title>Document</Title>
                    <Description>Signed Document with embedded Signature and TimeStamp</Description>
                    <!-- Other descriptive fields... -->

                    <!-- Signing information -->
                    <SigningInformation>

                        <SigningRole>SignedDocument</SigningRole>
                        <SigningRole>Signature</SigningRole>
                        <SigningRole>Timestamp</SigningRole>

                        <!-- Additional proof binaries are "detached" -->
                        <DetachedSigningRole>AdditionalProof</DetachedSigningRole>
                        <SignatureDescription>
                            <Signer>
                                <FullName>Caroline DISTRIQUIN</FullName>
                                <SigningTime>2023-01-27T10:54:54+01:00</SigningTime>
                            </Signer>
                        </SignatureDescription>
                        <TimestampingInformation>
                            <TimeStamp>2023-01-27T10:54:54+01:00</TimeStamp>
                        </TimestampingInformation>

                        <!-- Duplication of detached addition proof binaries in root archive unit -->
                        <AdditionalProof>
                            <AdditionalProofInformation>Additional proof: Appendix 1</AdditionalProofInformation>
                            <AdditionalProofInformation>Additional proof: Appendix 2</AdditionalProofInformation>
                            <AdditionalProofInformation>Additional proof: XML</AdditionalProofInformation>
                            <AdditionalProofInformation>Additional proof: PDF</AdditionalProofInformation>
                        </AdditionalProof>

                    </SigningInformation>
                </Content>

                <!-- Child archive units referencing detached binaries -->
                <ArchiveUnit id="ID16">
                    <Content>
                        <DescriptionLevel>Item</DescriptionLevel>
                        <Title>Additional proof: Appendix 2</Title>
                        <SigningInformation>
                            <SigningRole>AdditionalProof</SigningRole>
                            <AdditionalProof>
                                <AdditionalProofInformation>Additional proof: Appendix 2</AdditionalProofInformation>
                            </AdditionalProof>
                        </SigningInformation>
                    </Content>
                    <DataObjectReference>
                        <DataObjectGroupReferenceId>ID19</DataObjectGroupReferenceId>
                    </DataObjectReference>
                </ArchiveUnit>
                <ArchiveUnit id="ID15">
                    <Content>
                        <DescriptionLevel>Item</DescriptionLevel>
                        <Title>Additional proof: Appendix 1</Title>
                        <SigningInformation>
                            <SigningRole>AdditionalProof</SigningRole>
                            <AdditionalProof>
                                <AdditionalProofInformation>Additional proof: Appendix 1</AdditionalProofInformation>
                            </AdditionalProof>
                        </SigningInformation>
                    </Content>
                    <DataObjectReference>
                        <DataObjectGroupReferenceId>ID20</DataObjectGroupReferenceId>
                    </DataObjectReference>
                </ArchiveUnit>
                <ArchiveUnit id="ID13">
                    <Content>
                        <DescriptionLevel>Item</DescriptionLevel>
                        <Title>Additional proof: PDF</Title>
                        <SigningInformation>
                            <SigningRole>AdditionalProof</SigningRole>
                            <AdditionalProof>
                                <AdditionalProofInformation>Additional proof: PDF</AdditionalProofInformation>
                            </AdditionalProof>
                        </SigningInformation>
                    </Content>
                    <DataObjectReference>
                        <DataObjectGroupReferenceId>ID18</DataObjectGroupReferenceId>
                    </DataObjectReference>
                </ArchiveUnit>
                <ArchiveUnit id="ID14">
                    <Content>
                        <DescriptionLevel>Item</DescriptionLevel>
                        <Title>Additional proof: XML</Title>
                        <SigningInformation>
                            <SigningRole>AdditionalProof</SigningRole>
                            <AdditionalProof>
                                <AdditionalProofInformation>Additional proof: XML</AdditionalProofInformation>
                            </AdditionalProof>
                        </SigningInformation>
                    </Content>
                    <DataObjectReference>
                        <DataObjectGroupReferenceId>ID17</DataObjectGroupReferenceId>
                    </DataObjectReference>
                </ArchiveUnit>

                <!-- The root Archive Unit references the Object Group Id of the signed document (BinaryMaster_1) -->
                <DataObjectReference>
                    <DataObjectGroupReferenceId>ID10</DataObjectGroupReferenceId>
                </DataObjectReference>

            </ArchiveUnit>
        </DescriptiveMetadata>
        <!-- ... -->
    </DataObjectPackage>
    <!-- ... -->
</ArchiveTransfer>
```

### Cas 6 - Rattachement de binaires à un document signé

Il se peut qu'un ou plusieurs binaires complémentaires aient besoin d'être versés ultérieurement. Ceci peut se produire
lorsque la signature, l'horodatage et/ou les preuves complémentaires ne sont pas versées au même moment que le document
signé.

Dans ce cas, il convient simplement de procéder un rattachement d'unités d'archives « filles » à l'unité d'archives
« racine » via la balise `<UpdateOperation>`.

Ainsi, un exemple de modélisation d'un premier versement contenant un document signé et horodaté serait comme suit :

```xml

<ArchiveTransfer>
    <!-- ... -->
    <DataObjectPackage>

        <!-- Signed document -->
        <DataObjectGroup id="ID10">
            <BinaryDataObject id="ID11">
                <DataObjectVersion>BinaryMaster_1</DataObjectVersion>
                <Uri>content/ID11.pdf</Uri>
                <!-- ... -->
            </BinaryDataObject>
        </DataObjectGroup>

        <DescriptiveMetadata>
            <!-- Root archive unit referencing the signed document -->
            <ArchiveUnit id="ID12">
                <Content>
                    <DescriptionLevel>Item</DescriptionLevel>
                    <Title>Document</Title>
                    <Description>Signed Document with embedded Signature and TimeStamp</Description>
                    <!-- Other descriptive fields... -->

                    <!-- Signing information -->
                    <SigningInformation>

                        <SigningRole>SignedDocument</SigningRole>
                        <SigningRole>Signature</SigningRole>
                        <SigningRole>Timestamp</SigningRole>

                        <!-- Additional proof binaries are "detached" -->
                        <DetachedSigningRole>AdditionalProof</DetachedSigningRole>
                        <SignatureDescription>
                            <Signer>
                                <FullName>Caroline DISTRIQUIN</FullName>
                                <SigningTime>2023-01-27T10:54:54+01:00</SigningTime>
                            </Signer>
                        </SignatureDescription>
                        <TimestampingInformation>
                            <TimeStamp>2023-01-27T10:54:54+01:00</TimeStamp>
                        </TimestampingInformation>

                    </SigningInformation>
                </Content>

                <!-- The root Archive Unit references the Object Group Id of the signed document (BinaryMaster_1) -->
                <DataObjectReference>
                    <DataObjectGroupReferenceId>ID10</DataObjectGroupReferenceId>
                </DataObjectReference>

            </ArchiveUnit>
        </DescriptiveMetadata>
        <!-- ... -->
    </DataObjectPackage>
    <!-- ... -->
</ArchiveTransfer>
```

Le rattachement de preuves complémentaires au précédent binaire se ferait via :

```xml

<ArchiveTransfer>
    <!-- ... -->
    <DataObjectPackage>

        <!-- Additional proofs (detached) -->
        <DataObjectGroup id="ID17">
            <BinaryDataObject id="ID23">
                <DataObjectVersion>BinaryMaster_1</DataObjectVersion>
                <Uri>content/ID23.xml</Uri>
                <!-- ... -->
            </BinaryDataObject>
        </DataObjectGroup>
        <DataObjectGroup id="ID18">
            <BinaryDataObject id="ID21">
                <DataObjectVersion>BinaryMaster_1</DataObjectVersion>
                <Uri>content/ID21.pdf</Uri>
                <!-- ... -->
            </BinaryDataObject>
        </DataObjectGroup>
        <DataObjectGroup id="ID19">
            <BinaryDataObject id="ID22">
                <DataObjectVersion>BinaryMaster_1</DataObjectVersion>
                <Uri>content/ID22.pdf</Uri>
                <!-- ... -->
            </BinaryDataObject>
        </DataObjectGroup>
        <DataObjectGroup id="ID20">
            <BinaryDataObject id="ID24">
                <DataObjectVersion>BinaryMaster_1</DataObjectVersion>
                <Uri>content/ID24.pdf</Uri>
                <!-- ... -->
            </BinaryDataObject>
        </DataObjectGroup>

        <DescriptiveMetadata>
            <!-- Root archive unit referenced by SystemId (guid) -->
            <ArchiveUnit id="ID12">
                <Management>
                    <UpdateOperation>
                        <SystemId>#### GUID OF ROOT ARCHIVE UNIT ####</SystemId>
                    </UpdateOperation>
                </Management>
                <Content>
                    <DescriptionLevel>Item</DescriptionLevel>
                    <Title>Document</Title>
                </Content>

                <!-- Child archive units referencing detached binaries -->
                <ArchiveUnit id="ID16">
                    <Content>
                        <DescriptionLevel>Item</DescriptionLevel>
                        <Title>Additional proof: Appendix 2</Title>
                        <SigningInformation>
                            <SigningRole>AdditionalProof</SigningRole>
                            <AdditionalProof>
                                <AdditionalProofInformation>Additional proof: Appendix 2</AdditionalProofInformation>
                            </AdditionalProof>
                        </SigningInformation>
                    </Content>
                    <DataObjectReference>
                        <DataObjectGroupReferenceId>ID19</DataObjectGroupReferenceId>
                    </DataObjectReference>
                </ArchiveUnit>
                <ArchiveUnit id="ID15">
                    <Content>
                        <DescriptionLevel>Item</DescriptionLevel>
                        <Title>Additional proof: Appendix 1</Title>
                        <SigningInformation>
                            <SigningRole>AdditionalProof</SigningRole>
                            <AdditionalProof>
                                <AdditionalProofInformation>Additional proof: Appendix 1</AdditionalProofInformation>
                            </AdditionalProof>
                        </SigningInformation>
                    </Content>
                    <DataObjectReference>
                        <DataObjectGroupReferenceId>ID20</DataObjectGroupReferenceId>
                    </DataObjectReference>
                </ArchiveUnit>
                <ArchiveUnit id="ID13">
                    <Content>
                        <DescriptionLevel>Item</DescriptionLevel>
                        <Title>Additional proof: PDF</Title>
                        <SigningInformation>
                            <SigningRole>AdditionalProof</SigningRole>
                            <AdditionalProof>
                                <AdditionalProofInformation>Additional proof: PDF</AdditionalProofInformation>
                            </AdditionalProof>
                        </SigningInformation>
                    </Content>
                    <DataObjectReference>
                        <DataObjectGroupReferenceId>ID18</DataObjectGroupReferenceId>
                    </DataObjectReference>
                </ArchiveUnit>
                <ArchiveUnit id="ID14">
                    <Content>
                        <DescriptionLevel>Item</DescriptionLevel>
                        <Title>Additional proof: XML</Title>
                        <SigningInformation>
                            <SigningRole>AdditionalProof</SigningRole>
                            <AdditionalProof>
                                <AdditionalProofInformation>Additional proof: XML</AdditionalProofInformation>
                            </AdditionalProof>
                        </SigningInformation>
                    </Content>
                    <DataObjectReference>
                        <DataObjectGroupReferenceId>ID17</DataObjectGroupReferenceId>
                    </DataObjectReference>
                </ArchiveUnit>

            </ArchiveUnit>
        </DescriptiveMetadata>
        <!-- ... -->
    </DataObjectPackage>
    <!-- ... -->
</ArchiveTransfer>
```

### Cas 7 - Extensions (champs libres)

La norme SEDA prévoit la possibilité de rajouter des champs libres (extensions) pour rajouter des informations
additionnelles via la balise `<Extended>`

> **Important** : Il est fortement recommandé de décrire les champs libres dans l'Ontologie et d'adapter la
> configuration du mapping elasticsearch de Vitam pour la bonne indexation de ces champs.

Exemple de document signé avec signature et horodatage embarqués avec champs libres :

```xml

<ArchiveTransfer>
    <!-- ... -->
    <DataObjectPackage>

        <!-- An Object Group with signed and timestamped document to archive (BinaryMaster_1) -->
        <DataObjectGroup id="ID10">
            <BinaryDataObject id="ID11">
                <DataObjectVersion>BinaryMaster_1</DataObjectVersion>
                <Uri>content/ID11.pdf</Uri>
                <!-- ... -->
            </BinaryDataObject>
        </DataObjectGroup>

        <DescriptiveMetadata>
            <ArchiveUnit id="ID12">
                <Content>
                    <DescriptionLevel>Item</DescriptionLevel>
                    <Title>Document</Title>
                    <Description>Signed Document with embedded Signature and TimeStamp</Description>
                    <!-- Other descriptive fields... -->

                    <!-- Signing information -->
                    <SigningInformation>

                        <SigningRole>SignedDocument</SigningRole>
                        <SigningRole>Signature</SigningRole>
                        <SigningRole>Timestamp</SigningRole>

                        <SignatureDescription>
                            <Signer>
                                <FullName>Caroline DISTRIQUIN</FullName>
                                <SigningTime>2023-01-27T10:54:54+01:00</SigningTime>
                            </Signer>
                        </SignatureDescription>
                        <TimestampingInformation>
                            <TimeStamp>2023-01-27T10:54:54+01:00</TimeStamp>
                        </TimestampingInformation>

                        <!-- Extended fields -->
                        <Extended>
                            <ExtraField1>Val1</ExtraField1>
                            <ExtraField1>Val2</ExtraField1>
                            <ExtraField2>
                                <SubField2>Val3</SubField2>
                            </ExtraField2>
                        </Extended>

                    </SigningInformation>
                </Content>

                <!-- The Archive Unit references the Object Group Id of the binary object (BinaryMaster_1) -->
                <DataObjectReference>
                    <DataObjectGroupReferenceId>ID10</DataObjectGroupReferenceId>
                </DataObjectReference>

            </ArchiveUnit>
        </DescriptiveMetadata>
        <!-- ... -->
    </DataObjectPackage>
    <!-- ... -->
</ArchiveTransfer>
```