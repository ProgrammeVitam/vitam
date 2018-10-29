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