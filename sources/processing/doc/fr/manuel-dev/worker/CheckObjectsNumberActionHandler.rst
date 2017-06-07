Nombre d'objets numériques conforme
######################################

Ce module permet de vérifier que le nombre d’objets contenu dans un SIP correspond au nombre d’objets déclarés dans le bordereau afin de s’assurer de l’intégralité du SIP.


Le format supporté file SEDA et les schémas  est :

* xml
* sxd

Usage
*****

Pour l'usage interne Vitam
**************************

1) Extraction XML d'informations  en parcourant le fichier manifest:

    public ExtractUriResponse getAllDigitalObjectUriFromManifest(WorkParams params) throws ProcessingException, XMLStreamException {}
    
2) Parcours du fichier manifest avec la technologie StAX pour extraire 1 par 1 des Uri dans la balise Binary Data Object.


.. code-block:: java

    private void getUri(ExtractUriResponse extractUriResponse, XMLEventReader evenReader) throws XMLStreamException, URISyntaxException {  
        while (evenReader.hasNext()) {  
            XMLEvent event = evenReader.nextEvent();
            if (event.isStartElement()) {
                StartElement startElement = event.asStartElement();
                // If we have an Tag Uri element equal Uri into SEDA
                if (startElement.getName().getLocalPart() == (SedaUtils.TAG_URI)){ 
                    event = evenReader.nextEvent();
                    String uri = event.asCharacters().getData(); 
                    // Check element is duplicate
                    checkDuplicatedUri(extractUriResponse, uri);
                    extractUriResponse.getUriListManifest().add(new URI(uri));
                }               
            }
        }
    }


3) Vérification des éléménts dans la liste d'Uri sans doublon.

4) Ajout de l'élément de la liste d'Uri en capsulant dans l'object ExtractUriResponse.

.. code-block:: java

    public class ExtractUriResponse extends ProcessResponse{ 
    private boolean errorDuplicateUri;
    // list contains Uri for Binary Object
    private List<URI> uriListManifest;
    ...


5) Récupération de la liste d'Uri des objets numériques stockés dans un conteneur du workspace (de manière récursive).

Chemin pour récupérer les objets numériques : «GuidContainer/sip/content».


public List<URI> getListUriDigitalObjectFromFolder(String containerName, String folderName) throws ContentAddressableStorageException {
        ...

        List<URI> uriFolderListFromContainer;
        try {
            BlobStore blobStore = context.getBlobStore();

            // It's like a filter
            ListContainerOptions listContainerOptions = new ListContainerOptions();
            // List of all resources in a container recursively
            final PageSet<? extends StorageMetadata> blobStoreList =
                blobStore.list(containerName, listContainerOptions.inDirectory(folderName).recursive());

            uriFolderListFromContainer = new ArrayList<>();
            LOGGER.info(WorkspaceMessage.BEGINNING_GET_URI_LIST_OF_DIGITAL_OBJECT.getMessage());


            for (Iterator<? extends StorageMetadata> iterator = blobStoreList.iterator(); iterator.hasNext();) {
                StorageMetadata storageMetada = iterator.next();

                // select BLOB only, not folder nor relative path
                if ((storageMetada.getType().equals(StorageType.BLOB) && storageMetada.getName() != null &&
                    !storageMetada.getName().isEmpty())) {
                    uriFolderListFromContainer.add(new URI(UriUtils.splitUri(storageMetada.getName())));
                }
            }
 ... 
}

6) Vérification conformité du nombre d'objets numériques.

6.1) Vérification de présence de doublons dans la liste des Uri du bordereau
Si présence de doublons la comparaison avec la liste des Uri provenant du SIP n'est pas déclenchée

  if (extractUriResponse != null && !extractUriResponse.isErrorDuplicateUri()) {
...}

6.2)Comparaison des listes

-Comparaison de la taille des liste.
-Comparaison des URI.
-Identification des Uri non référencés dans le SIP.
-Identification des Uri non déclarés dans le bordereau.

private void checkCountDigitalObjectConformity(List<URI> uriListManifest, List<URI> uriListWorkspace,
        Response response) {
...
}

