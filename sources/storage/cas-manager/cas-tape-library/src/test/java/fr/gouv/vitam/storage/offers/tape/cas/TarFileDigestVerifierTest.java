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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryObjectReferentialId;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryTarObjectStorageLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeObjectReferentialEntity;
import fr.gouv.vitam.storage.engine.common.model.TarEntryDescription;
import fr.gouv.vitam.storage.offers.tape.exception.ObjectReferentialException;
import fr.gouv.vitam.storage.offers.tape.utils.LocalFileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class TarFileDigestVerifierTest {

    private static final String FILE_1 = "file1";
    private static final String CONTAINER_1 = "container1";
    private static final String CONTAINER_2 = "container2";

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    ObjectReferentialRepository objectReferentialRepository;

    @Test
    public void testNoEntries() throws Exception {
        TarFileDigestVerifier tarFileDigestVerifier = new TarFileDigestVerifier(objectReferentialRepository, 10);
        tarFileDigestVerifier.finalizeChecks();

        verifyNoMoreInteractions(objectReferentialRepository);
    }

    @Test
    public void testSingleEntryOK() throws Exception {
        // Given
        String storageId1 = LocalFileUtils.createStorageId(FILE_1);
        String entryName1 = LocalFileUtils.createTarEntryName(CONTAINER_1, storageId1, 0);

        doReturn(singletonList(createObjectReferentialEntry(entryName1, "digest1")))
            .when(objectReferentialRepository)
            .bulkFind(any(), any());

        TarFileDigestVerifier tarFileDigestVerifier = new TarFileDigestVerifier(objectReferentialRepository, 10);

        // When
        tarFileDigestVerifier.addDigestToCheck(entryName1, "digest1");
        tarFileDigestVerifier.finalizeChecks();

        // Then
        verify(objectReferentialRepository).bulkFind(CONTAINER_1, ImmutableSet.of(FILE_1));
        verifyNoMoreInteractions(objectReferentialRepository);
    }

    @Test
    public void testSingleEntryBadDigest() throws Exception {
        // Given
        String storageId1 = LocalFileUtils.createStorageId(FILE_1);
        String entryName1 = LocalFileUtils.createTarEntryName(CONTAINER_1, storageId1, 0);

        doReturn(singletonList(createObjectReferentialEntry(entryName1, "digest1")))
            .when(objectReferentialRepository)
            .bulkFind(any(), any());

        TarFileDigestVerifier tarFileDigestVerifier = new TarFileDigestVerifier(objectReferentialRepository, 10);

        // When
        assertThatThrownBy(() -> {
            tarFileDigestVerifier.addDigestToCheck(entryName1, "bad_digest");
            tarFileDigestVerifier.finalizeChecks();
        }).isInstanceOf(Exception.class);
    }

    @Test
    public void testSingleEntryBadEntryIndex() throws Exception {
        // Given
        String storageId1 = LocalFileUtils.createStorageId(FILE_1);
        String entryName1 = LocalFileUtils.createTarEntryName(CONTAINER_1, storageId1, 99);

        doReturn(singletonList(createObjectReferentialEntry(entryName1, "digest1")))
            .when(objectReferentialRepository)
            .bulkFind(any(), any());

        TarFileDigestVerifier tarFileDigestVerifier = new TarFileDigestVerifier(objectReferentialRepository, 10);

        // When
        assertThatThrownBy(() -> {
            tarFileDigestVerifier.addDigestToCheck(entryName1, "digest1");
            tarFileDigestVerifier.finalizeChecks();
        }).isInstanceOf(Exception.class);
    }

    @Test
    public void testEntriesLessThenBulkSize() throws Exception {
        Map<String, String> fileIdsWithContainers = ImmutableMap.of(
            "file1",
            "container1",
            "file2",
            "container1",
            "file3",
            "container1",
            "file4",
            "container1",
            "file5",
            "container1"
        );
        int bulkSize = 10;

        testVerify(fileIdsWithContainers, bulkSize);

        // Then
        verify(objectReferentialRepository, times(1)).bulkFind(eq(CONTAINER_1), any());
        verifyNoMoreInteractions(objectReferentialRepository);
    }

    @Test
    public void testEntriesMatchingBulkSize() throws Exception {
        Map<String, String> fileIdsWithContainers = ImmutableMap.of(
            "file1",
            "container1",
            "file2",
            "container1",
            "file3",
            "container1",
            "file4",
            "container1",
            "file5",
            "container1"
        );
        int bulkSize = 5;

        testVerify(fileIdsWithContainers, bulkSize);

        // Then
        verify(objectReferentialRepository, times(1)).bulkFind(eq(CONTAINER_1), any());
        verifyNoMoreInteractions(objectReferentialRepository);
    }

    @Test
    public void testMultipleContainers() throws Exception {
        Map<String, String> fileIdsWithContainers = ImmutableMap.of(
            "file1",
            CONTAINER_1,
            "file2",
            CONTAINER_2,
            "file3",
            CONTAINER_2,
            "file4",
            CONTAINER_1,
            "file5",
            CONTAINER_1
        );
        int bulkSize = 2;

        testVerify(fileIdsWithContainers, bulkSize);

        // Then
        verify(objectReferentialRepository, times(1)).bulkFind(eq(CONTAINER_2), any());
        verify(objectReferentialRepository, times(2)).bulkFind(eq(CONTAINER_1), any());
        verifyNoMoreInteractions(objectReferentialRepository);
    }

    private void testVerify(Map<String, String> containersByFileIds, int bulkSize) throws ObjectReferentialException {
        // Given
        Set<String> fileIds = containersByFileIds.keySet();
        Map<String, String> storageIds = fileIds
            .stream()
            .collect(toMap(fileId -> fileId, LocalFileUtils::createStorageId));
        Map<String, String> digests = fileIds.stream().collect(toMap(fileId -> fileId, fileId -> "digest" + fileId));

        Map<String, String> entryNames = fileIds
            .stream()
            .collect(
                toMap(
                    fileId -> fileId,
                    fileId ->
                        LocalFileUtils.createTarEntryName(containersByFileIds.get(fileId), storageIds.get(fileId), 0)
                )
            );

        Map<String, Integer> nbInvocationByContainer = new HashMap<>();
        List<String> queriedObjectNames = new ArrayList<>();
        doAnswer(args -> {
            String container = args.getArgument(0);
            Set<String> objectNames = args.getArgument(1);

            queriedObjectNames.addAll(objectNames);

            Integer cpt = nbInvocationByContainer.getOrDefault(container, 0);
            nbInvocationByContainer.put(container, cpt);

            // Check that elements are grouped by container
            assertThat(
                containersByFileIds
                    .entrySet()
                    .stream()
                    .filter(entry -> objectNames.contains(entry.getKey()))
                    .map(Map.Entry::getValue)
                    .collect(Collectors.toSet())
            ).containsExactlyInAnyOrder(container);

            // Return response
            return objectNames
                .stream()
                .map(objectName -> createObjectReferentialEntry(entryNames.get(objectName), digests.get(objectName)))
                .collect(toList());
        })
            .when(objectReferentialRepository)
            .bulkFind(any(), any());

        TarFileDigestVerifier tarFileDigestVerifier = new TarFileDigestVerifier(objectReferentialRepository, bulkSize);

        // When
        for (String fileId : fileIds) {
            tarFileDigestVerifier.addDigestToCheck(entryNames.get(fileId), digests.get(fileId));
        }
        tarFileDigestVerifier.finalizeChecks();

        assertThat(queriedObjectNames).containsExactlyInAnyOrderElementsOf(containersByFileIds.keySet());
    }

    private TapeObjectReferentialEntity createObjectReferentialEntry(String entryName, String digest) {
        return new TapeObjectReferentialEntity(
            new TapeLibraryObjectReferentialId(
                LocalFileUtils.getContainerNameFromTarEntryName(entryName),
                LocalFileUtils.storageIdToObjectName(LocalFileUtils.getStorageIdFromTarEntryName(entryName))
            ),
            1000,
            VitamConfiguration.getDefaultDigestType().getName(),
            digest,
            LocalFileUtils.getStorageIdFromTarEntryName(entryName),
            new TapeLibraryTarObjectStorageLocation(
                singletonList(new TarEntryDescription("tarId", entryName, 0, 1000, digest))
            ),
            null,
            null
        );
    }
}
