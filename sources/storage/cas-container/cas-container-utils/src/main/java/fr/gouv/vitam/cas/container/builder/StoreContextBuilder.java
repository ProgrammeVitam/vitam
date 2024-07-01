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
package fr.gouv.vitam.cas.container.builder;

import com.mongodb.client.MongoDatabase;
import fr.gouv.vitam.cas.container.swift.OpenstackSwift;
import fr.gouv.vitam.common.security.IllegalPathException;
import fr.gouv.vitam.common.storage.StorageConfiguration;
import fr.gouv.vitam.common.storage.cas.container.api.ContentAddressableStorage;
import fr.gouv.vitam.common.storage.constants.StorageProvider;
import fr.gouv.vitam.common.storage.filesystem.FileSystem;
import fr.gouv.vitam.common.storage.filesystem.v2.HashFileSystem;
import fr.gouv.vitam.common.storage.s3.AmazonS3V1;
import fr.gouv.vitam.common.storage.swift.Swift;
import fr.gouv.vitam.common.storage.swift.SwiftKeystoneFactoryV2;
import fr.gouv.vitam.common.storage.swift.SwiftKeystoneFactoryV3;
import fr.gouv.vitam.storage.offers.tape.TapeLibraryFactory;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

/**
 * Creates {@link ContentAddressableStorage} configured in a configuration file
 * <br/>
 * Ex. to build a {@link ContentAddressableStorage} of a particular store
 * context,
 *
 * <pre>
 *    storeConfiguration = new StorageConfiguration().setProvider(StorageProvider.SWIFT_AUTH_V1.getValue())
 *      .setSwiftKeystoneAuthUrl("http://10.10.10.10:5000/auth/v1.0)
 *      .setTenantName(swift)
 *      .setUserName(user)
 *      .setSwiftPassword(passwd);
 *
 * contentAddressableStorage=StoreContextBuilder.newStoreContext(storeConfiguration);
 * </pre>
 *
 * @see ContentAddressableStorage
 * @see StorageConfiguration
 * @see OpenstackSwift
 * @see Swift
 * @see AmazonS3V1
 * @see HashFileSystem
 * @see FileSystem
 */
public class StoreContextBuilder {

    /**
     * Builds {@link ContentAddressableStorage}
     *
     * @param configuration {@link StorageConfiguration}
     * @return ContentAddressableStorage : by default fileSystem or
     * openstack-swift if it is configured
     */
    public static ContentAddressableStorage newStoreContext(
        StorageConfiguration configuration,
        MongoDatabase mongoDatabase
    )
        throws CertificateException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, IOException, IllegalPathException {
        if (StorageProvider.SWIFT_AUTH_V1.getValue().equalsIgnoreCase(configuration.getProvider())) {
            // TODO: keep keystone V1 authent ? No openstack4j keystone V1 authentication implementation, so we have
            // to keep jcloud or anything else
            return new OpenstackSwift(configuration);
        } else if (StorageProvider.HASHFILESYSTEM.getValue().equalsIgnoreCase(configuration.getProvider())) {
            return new HashFileSystem(configuration);
        } else if (StorageProvider.SWIFT_AUTH_V2.getValue().equalsIgnoreCase(configuration.getProvider())) {
            SwiftKeystoneFactoryV2 swiftKeystoneFactoryV2 = new SwiftKeystoneFactoryV2(configuration);
            return new Swift(swiftKeystoneFactoryV2, configuration);
        } else if (StorageProvider.SWIFT_AUTH_V3.getValue().equalsIgnoreCase(configuration.getProvider())) {
            SwiftKeystoneFactoryV3 swiftKeystoneFactoryV3 = new SwiftKeystoneFactoryV3(configuration);
            return new Swift(swiftKeystoneFactoryV3, configuration);
        } else if (StorageProvider.AMAZON_S3_V1.getValue().equalsIgnoreCase(configuration.getProvider())) {
            return new AmazonS3V1(configuration);
        } else if (StorageProvider.TAPE_LIBRARY.getValue().equalsIgnoreCase(configuration.getProvider())) {
            TapeLibraryFactory tapeLibraryFactory = TapeLibraryFactory.getInstance();
            tapeLibraryFactory.initialize(configuration.getTapeLibraryConfiguration(), mongoDatabase);
            return tapeLibraryFactory.getTapeLibraryContentAddressableStorage();
        } else {
            // by default file system
            return new FileSystem(configuration);
        }
    }
}
