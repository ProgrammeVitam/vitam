package fr.gouv.vitam.metadata.rest;

import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;

public class MetaDataApplicationTest {

    private static int DATABASE_PORT = 45678;
    private static MongodExecutable mongodExecutable;
	private MetaDataApplication application = new MetaDataApplication();
    static MongodProcess mongod;

    @BeforeClass
    public static void setUp() throws IOException {
        final MongodStarter starter = MongodStarter.getDefaultInstance();
        mongodExecutable = starter.prepare(new MongodConfigBuilder()
            .version(Version.Main.PRODUCTION)
            .net(new Net(DATABASE_PORT, Network.localhostIsIPv6()))
            .build());
        mongod = mongodExecutable.start();
    }
    
    @AfterClass
    public static void tearDownAfterClass() {
        mongod.stop();
        mongodExecutable.stop();
    }
	@Test(expected = IllegalArgumentException.class)
	public void givenEmptyArgsWhenConfigureApplicationThenRaiseAnException() throws Exception {
		application.configure(new String[0]);
	}

	@Test(expected = Exception.class)
	public void givenFileNotFoundWhenConfigureApplicationThenRaiseAnException() throws Exception {
		application.configure("src/test/resources/notFound.conf");
	}

	@Test
	public void givenFileExistsWhenConfigureApplicationThenRunServer() throws Exception {
		application.configure("src/test/resources/metadata.conf", "8088");
	}
	
	@Test
	public void givenPortNegativeWhenConfigureApplicationThenUseDefaultPortToRunServer() throws Exception {
		application.configure("src/test/resources/metadata.conf", "-12");
	}
}
