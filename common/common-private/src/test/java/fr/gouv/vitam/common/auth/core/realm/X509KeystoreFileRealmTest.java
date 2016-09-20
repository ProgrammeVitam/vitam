/**
 * Copyright Paul Merlin 2011 (Apache Licence v2.0) 
 * 
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
package fr.gouv.vitam.common.auth.core.realm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.junit.Before;
import org.junit.Test;

import fr.gouv.vitam.common.auth.core.authc.X509AuthenticationInfo;
import fr.gouv.vitam.common.auth.core.authc.X509AuthenticationToken;

public class X509KeystoreFileRealmTest{
    
    private X509KeystoreFileRealm realm;
    private X509Certificate cert;
    
    byte[] certBytes = new byte[] { '[', 'B', '@', 1, 4, 0, 'c', 9, 'f', 3, 9 };
    BigInteger serial = new BigInteger("1000000000000000");

    @Before
    public void setUp() throws Exception {
        
        realm = new X509KeystoreFileRealm();
        
        realm.setGrantedKeyStoreName("src/test/resources/tls/server/granted_certs.jks");
        realm.setGrantedKeyStorePassphrase("gazerty");
        realm.setTrustedKeyStoreName("src/test/resources/tls/server/truststore.jks");
        realm.setTrustedKeyStorePassphrase("tazerty");
        
        cert = mock(X509Certificate.class);
        when(cert.getEncoded()).thenReturn(certBytes);
        when(cert.getSerialNumber()).thenReturn(serial);
    }
    
    @Test
    public void testGettersAndSetters(){
        
        realm.getAuthenticationTokenClass();
        
        assertEquals("src/test/resources/tls/server/granted_certs.jks", realm.getGrantedKeyStoreName());
        assertEquals("gazerty", realm.getGrantedKeyStorePassphrase());
        assertEquals("src/test/resources/tls/server/truststore.jks", realm.getTrustedKeyStoreName());
        assertEquals("tazerty", realm.getTrustedKeyStorePassphrase());
    }
    
    @Test
    public void givenRealmWhenSendCertificateTokenThenGetCertificateInfo(){
        
        X509Certificate[] clientCertChain = new X509Certificate[]{cert};
        X509AuthenticationToken token = new X509AuthenticationToken(clientCertChain, "XXX");
        assertTrue(realm.supports(token));
        
        X509AuthenticationInfo info = (X509AuthenticationInfo) realm.doGetAuthenticationInfo(token);
        
    }
    
    @Test(expected = NullPointerException.class)
    public void givenRealmWhenKeyStoreNotFoundThenReturnNull(){
        realm.setGrantedKeyStoreName("XXX.jks");
        realm.setGrantedKeyStorePassphrase("gazerty");
        
        X509Certificate[] clientCertChain = new X509Certificate[]{cert};
        X509AuthenticationToken token = new X509AuthenticationToken(clientCertChain, "XXX");
        realm.doGetAuthenticationInfo(token);
    }

    @Test
    public void givenRealmWhenP12NotGrantedThenReturnNull() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException{
        InputStream inStream = new FileInputStream("src/test/resources/tls/client/client_notgranted.p12");

        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(inStream, "vitam2016".toCharArray());  

        String alias = ks.aliases().nextElement();
        X509Certificate certificate = (X509Certificate) ks.getCertificate(alias);
        
        X509Certificate[] clientCertChain = new X509Certificate[]{certificate};
        X509AuthenticationToken token = new X509AuthenticationToken(clientCertChain, "XXX");
        X509AuthenticationInfo info = (X509AuthenticationInfo) realm.doGetAuthenticationInfo(token);
        assertNull(info);
    }
}
