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

import fr.gouv.vitam.common.stream.ExactSizeInputStream;
import fr.gouv.vitam.common.stream.ExtendedFileOutputStream;
import fr.gouv.vitam.storage.offers.tape.utils.LocalFileUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class BasicFileStorage {

    private final Path basePath;
    private final Set<String> containerCache = ConcurrentHashMap.newKeySet();

    public BasicFileStorage(String basePath) {
        this.basePath = Paths.get(basePath);
    }

    public String writeFile(String containerName, String objectName, InputStream inputStream, long size)
        throws IOException {
        ensureContainerExists(containerName);

        String storageId = LocalFileUtils.createStorageId(objectName);
        Path filePath = getFilePath(containerName, storageId);

        try (ExtendedFileOutputStream outputStream = new ExtendedFileOutputStream(filePath, true)) {
            ExactSizeInputStream exactSizeInputStream = new ExactSizeInputStream(inputStream, size);

            IOUtils.copy(exactSizeInputStream, outputStream);

            return storageId;
        } catch (IOException ex) {
            deleteFile(containerName, storageId);
            throw ex;
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    public InputStream readFile(String containerName, String storageId) throws IOException {
        Path filePath = getFilePath(containerName, storageId);
        return Files.newInputStream(filePath, StandardOpenOption.READ);
    }

    public void deleteFile(String containerName, String storageId) {
        Path filePath = getFilePath(containerName, storageId);
        FileUtils.deleteQuietly(filePath.toFile());
    }

    private void ensureContainerExists(String containerName) throws IOException {
        if (!containerCache.contains(containerName)) {
            synchronized (containerCache) {
                if (!containerCache.contains(containerName)) {
                    Files.createDirectories(getContainerPath(basePath, containerName));
                }
            }
        }
    }

    public Stream<String> listStorageIdsByContainerName(String containerName) throws IOException {
        Path containerPath = getContainerPath(basePath, containerName);
        if (!Files.exists(containerPath)) {
            return Stream.empty();
        }
        return Files.list(containerPath).map(path -> path.toFile().getName());
    }

    private Path getFilePath(String containerName, String storageId) {
        return getContainerPath(basePath, containerName).resolve(storageId);
    }

    private Path getContainerPath(Path basePath, String containerName) {
        return basePath.resolve(containerName);
    }
}
