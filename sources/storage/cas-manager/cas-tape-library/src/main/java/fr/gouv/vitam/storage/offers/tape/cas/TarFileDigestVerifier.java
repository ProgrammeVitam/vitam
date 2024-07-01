/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "https://cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.storage.offers.tape.cas;

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryInputFileObjectStorageLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryTarObjectStorageLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeObjectReferentialEntity;
import fr.gouv.vitam.storage.offers.tape.exception.ObjectReferentialException;
import fr.gouv.vitam.storage.offers.tape.utils.LocalFileUtils;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

public class TarFileDigestVerifier {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(TarFileDigestVerifier.class);

    private final ObjectReferentialRepository objectReferentialRepository;
    private final int bulkSize;
    private MultiValuedMap<String, EntryToCheck> entriesToCheckByContainerName = new ArrayListValuedHashMap<>();

    public TarFileDigestVerifier(ObjectReferentialRepository objectReferentialRepository, int bulkSize) {
        this.objectReferentialRepository = objectReferentialRepository;
        this.bulkSize = bulkSize;
    }

    public void addDigestToCheck(String tarEntryName, String digestValue) throws ObjectReferentialException {
        String containerName = LocalFileUtils.getContainerNameFromTarEntryName(tarEntryName);
        String storageId = LocalFileUtils.getStorageIdFromTarEntryName(tarEntryName);
        int entryIndex = LocalFileUtils.getEntryIndexFromTarEntryName(tarEntryName);
        String objectName = LocalFileUtils.storageIdToObjectName(storageId);

        EntryToCheck entryToCheck = new EntryToCheck(
            tarEntryName,
            containerName,
            objectName,
            storageId,
            entryIndex,
            digestValue
        );
        Collection<EntryToCheck> entriesToCheck = entriesToCheckByContainerName.get(containerName);
        entriesToCheck.add(entryToCheck);

        if (entriesToCheck.size() >= bulkSize) {
            processBulk(containerName, entriesToCheck);
            entriesToCheck.clear();
        }
    }

    public void finalizeChecks() throws ObjectReferentialException {
        for (String containerName : this.entriesToCheckByContainerName.keySet()) {
            Collection<EntryToCheck> entriesToCheck = entriesToCheckByContainerName.get(containerName);

            if (!entriesToCheck.isEmpty()) {
                processBulk(containerName, entriesToCheck);
            }
        }
    }

    private void processBulk(String containerName, Collection<EntryToCheck> entriesToCheck)
        throws ObjectReferentialException {
        Set<String> objectNames = entriesToCheck
            .stream()
            .map(entryToCheck -> entryToCheck.objectName)
            .collect(Collectors.toSet());

        List<TapeObjectReferentialEntity> objectReferentialEntities = objectReferentialRepository.bulkFind(
            containerName,
            objectNames
        );

        Map<String, TapeObjectReferentialEntity> objectReferentialEntityByObjectNameMap = objectReferentialEntities
            .stream()
            .collect(toMap(entity -> entity.getId().getObjectName(), entity -> entity));

        for (EntryToCheck entryToCheck : entriesToCheck) {
            TapeObjectReferentialEntity objectReferentialEntity = objectReferentialEntityByObjectNameMap.get(
                entryToCheck.objectName
            );

            if (
                objectReferentialEntity == null ||
                !entryToCheck.storageId.equals(objectReferentialEntity.getStorageId())
            ) {
                // LOG & ignore
                LOGGER.debug("Ignoring deleted or updated object entry " + entryToCheck.tarEntryName);
            } else if (objectReferentialEntity.getLocation() instanceof TapeLibraryTarObjectStorageLocation) {
                TapeLibraryTarObjectStorageLocation inTarLocation =
                    (TapeLibraryTarObjectStorageLocation) objectReferentialEntity.getLocation();
                if (inTarLocation.getTarEntries().size() <= entryToCheck.entryIndex) {
                    throw new IllegalStateException("Invalid entry index for entry" + entryToCheck.tarEntryName);
                }
                String expectedEntryDigest = inTarLocation
                    .getTarEntries()
                    .get(entryToCheck.entryIndex)
                    .getDigestValue();
                if (entryToCheck.digestValue.equals(expectedEntryDigest)) {
                    // LOG & ignore
                    LOGGER.debug(
                        "Object with entry name {} digest checked successfully {}",
                        entryToCheck.tarEntryName,
                        expectedEntryDigest
                    );
                } else {
                    // VERY BAD !
                    throw new IllegalStateException(
                        String.format(
                            "Object with entry name %s/%s digest mismatch. Found=%s, expected=%s",
                            entryToCheck.containerName,
                            entryToCheck.storageId,
                            entryToCheck.digestValue,
                            expectedEntryDigest
                        )
                    );
                }
            } else if (objectReferentialEntity.getLocation() instanceof TapeLibraryInputFileObjectStorageLocation) {
                // LOG & ignore
                LOGGER.debug("Ignoring non indexed object entry " + entryToCheck.tarEntryName);
            } else {
                throw new IllegalStateException("Invalid location type " + objectReferentialEntity.getLocation());
            }
        }
    }

    private static class EntryToCheck {

        private final String tarEntryName;
        private final String containerName;
        private final String objectName;
        private final String storageId;
        private final int entryIndex;
        private final String digestValue;

        private EntryToCheck(
            String tarEntryName,
            String containerName,
            String objectName,
            String storageId,
            int entryIndex,
            String digestValue
        ) {
            this.tarEntryName = tarEntryName;
            this.containerName = containerName;
            this.objectName = objectName;
            this.storageId = storageId;
            this.entryIndex = entryIndex;
            this.digestValue = digestValue;
        }
    }
}
