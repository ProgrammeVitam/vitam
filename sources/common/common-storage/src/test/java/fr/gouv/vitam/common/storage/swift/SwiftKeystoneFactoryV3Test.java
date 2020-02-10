/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
package fr.gouv.vitam.common.storage.swift;

import fr.gouv.vitam.common.storage.StorageConfiguration;
import org.junit.Ignore;
import org.junit.Test;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.common.Payloads;

import java.io.File;

public class SwiftKeystoneFactoryV3Test {


    @Test
    @Ignore
    public void testSiwftKeystoneV3() throws Exception {
        StorageConfiguration conf = new StorageConfiguration();
        conf.setProvider("openstack-swift-v3");
        conf.setSwiftKeystoneAuthUrl("http://192.168.56.101/identity/v3");
        conf.setSwiftDomain("default");
        conf.setSwiftProjectName("demo");
        conf.setSwiftUser("admin");
        conf.setSwiftPassword("nomoresecret");
        SwiftKeystoneFactoryV3 v3 = new SwiftKeystoneFactoryV3(conf);
        OSClient.OSClientV3 client = v3.get();
        System.err.println(client.objectStorage().containers().list());
        client.objectStorage().containers().create("paris");
        System.err.println(client.objectStorage().containers().list());
        client.objectStorage().objects().put("paris", "paris", Payloads.create(new File("/home/youdsys/Downloads/consistency error.zip")));
        System.err.println(client.objectStorage().objects().list("paris"));
    }
}
