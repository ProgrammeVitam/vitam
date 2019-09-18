package fr.gouv.vitam.security.internal.filter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.UriInfo;

import fr.gouv.vitam.common.model.administration.ContextStatus;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.security.internal.client.InternalSecurityClientFactory;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Certificate;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.ContextModel;
import fr.gouv.vitam.common.model.administration.PermissionModel;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.security.internal.client.InternalSecurityClient;
import fr.gouv.vitam.security.internal.common.model.IdentityModel;
import fr.gouv.vitam.security.internal.exception.VitamSecurityException;
import sun.misc.BASE64Encoder;
import sun.security.provider.X509Factory;

/**
 * Test class for internal Security
 */
public class InternalSecurityFilterTest {

    public static final String FAKE_CONTEXT_ID = "FAKE_CONTEXT_ID";
    private X509Certificate cert;
    private String pem;
    private static final Integer TENANT_ID = 0;

    private HttpServletRequest httpServletRequest;
    private ContainerRequestContext containerRequestContext;
    private InternalSecurityFilter internalSecurityFilter;
    private InternalSecurityClient internalSecurityClient;
    private AdminManagementClient adminManagementClient;

    private InternalSecurityClientFactory internalSecurityClientFactory;
    private AdminManagementClientFactory adminManagementClientFactory;

    private UriInfo uriInfo;

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());


    private X509Certificate loadCertificate(CertificateFactory cf, File f) throws CertificateException, IOException {
        FileInputStream in = new FileInputStream(f);
        try {
            cert = (X509Certificate) cf.generateCertificate(in);
            cert.checkValidity();
            return cert;
        } finally {
            in.close();
        }
    }


    @Before
    public void setUp() throws Exception {

        generateX509Certificate();

        x509CertificateToPem();

        internalSecurityClientFactory = mock(InternalSecurityClientFactory.class);
        adminManagementClientFactory = mock(AdminManagementClientFactory.class);

        httpServletRequest = mock(HttpServletRequest.class);
        containerRequestContext = mock(ContainerRequestContext.class);
        internalSecurityClient = mock(InternalSecurityClient.class);
        adminManagementClient = mock(AdminManagementClient.class);
        when(internalSecurityClientFactory.getClient()).thenReturn(internalSecurityClient);
        when(adminManagementClientFactory.getClient()).thenReturn(adminManagementClient);

        internalSecurityFilter =
            new InternalSecurityFilter(httpServletRequest, internalSecurityClientFactory, adminManagementClientFactory);

        uriInfo = mock(UriInfo.class);
        when(containerRequestContext.getUriInfo()).thenReturn(uriInfo);

        doThrow(new VitamSecurityException("")).when(containerRequestContext).abortWith(any());
    }

    @After
    public void tearDown() throws Exception {
    }

    private void x509CertificateToPem() throws CertificateEncodingException {
        BASE64Encoder encoder = new BASE64Encoder();

        StringWriter sw = new StringWriter();
        sw.write(X509Factory.BEGIN_CERT);
        sw.write("\n");
        sw.write(encoder.encode(cert.getEncoded()));
        sw.write("\n");
        sw.write(X509Factory.END_CERT);

        pem = sw.toString();
    }

    private void generateX509Certificate()
        throws NoSuchAlgorithmException, IOException, OperatorCreationException, CertificateException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(1024, new SecureRandom());
        KeyPair keyPair = generator.generateKeyPair();

        String dn = "localhost";

        X509v3CertificateBuilder builder = new X509v3CertificateBuilder(
            new X500Name("CN=" + dn),
            BigInteger.valueOf(new SecureRandom().nextLong()),
            new Date(System.currentTimeMillis() - 10000),
            new Date(System.currentTimeMillis() + 24L * 3600 * 1000),
            new X500Name("CN=" + dn),
            SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded()));

        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
        builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature));
        builder.addExtension(Extension.extendedKeyUsage, true, new ExtendedKeyUsage(KeyPurposeId.id_kp_clientAuth));

        AlgorithmIdentifier signatureAlgorithmId =
            new DefaultSignatureAlgorithmIdentifierFinder().find("SHA256withRSA");
        AlgorithmIdentifier digestAlgorithmId = new DefaultDigestAlgorithmIdentifierFinder().find(signatureAlgorithmId);
        AsymmetricKeyParameter privateKey = PrivateKeyFactory.createKey(keyPair.getPrivate().getEncoded());


        X509CertificateHolder
            holder =
            builder.build(new BcRSAContentSignerBuilder(signatureAlgorithmId, digestAlgorithmId).build(privateKey));
        Certificate certificate = holder.toASN1Structure();

        InputStream is = new ByteArrayInputStream(certificate.getEncoded());
        try {
            cert = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(is);
        } finally {
            is.close();
        }
    }

    /**
     * When not certificate in the request attribute
     * @throws Exception
     */
    @Test(expected = VitamSecurityException.class)
    public void filterNotCertificateShouldThrowException() throws Exception {
        internalSecurityFilter.filter(containerRequestContext);
    }

    /**
     * When no certificate in the request attribute but certificate pem in the header
     * @throws Exception
     */
    @Test
    @RunWithCustomExecutor
    public void whenCertificateInTheHeaderThenOK() throws Exception {
        // Needs mock subject for login call
        when(httpServletRequest.getAttribute("javax.servlet.request.X509Certificate"))
            .thenReturn(new X509Certificate[] {cert});
        when(httpServletRequest.getHeader(GlobalDataRest.X_TENANT_ID)).thenReturn(TENANT_ID.toString());
        when(httpServletRequest.getHeader(GlobalDataRest.X_SSL_CLIENT_CERT)).thenReturn(pem);

        when(internalSecurityClient.findIdentity(any())).thenReturn(getIdentityModel(cert));
        when(uriInfo.getPath()).thenReturn("/otherUri");
        when(adminManagementClient.findContextById(anyString()))
            .thenReturn(getTestContext(ContextStatus.ACTIVE, true, "fakeAccessContract", null));
        internalSecurityFilter.filter(containerRequestContext);
    }


    /**
     * When the context status is false
     * @throws Exception
     */
    @Test(expected = VitamSecurityException.class)
    @RunWithCustomExecutor
    public void whenContextInactivatedThenException() throws Exception {
        // Needs mock subject for login call
        when(httpServletRequest.getAttribute("javax.servlet.request.X509Certificate"))
            .thenReturn(new X509Certificate[] {cert});
        when(httpServletRequest.getHeader(GlobalDataRest.X_TENANT_ID)).thenReturn(TENANT_ID.toString());

        when(internalSecurityClient.findIdentity(any())).thenReturn(getIdentityModel(cert));

        when(adminManagementClient.findContextById(anyString()))
            .thenReturn(getTestContext(ContextStatus.INACTIVE, true, null, null));
        internalSecurityFilter.filter(containerRequestContext);
    }


    /**
     * When status uri then verify only certificate
     * @throws Exception
     */
    @Test
    @RunWithCustomExecutor
    public void whenStatusUriThenOK() throws Exception {
        // Needs mock subject for login call
        when(httpServletRequest.getAttribute("javax.servlet.request.X509Certificate"))
            .thenReturn(new X509Certificate[] {cert});
        when(httpServletRequest.getHeader(GlobalDataRest.X_TENANT_ID)).thenReturn(TENANT_ID.toString());

        when(internalSecurityClient.findIdentity(any())).thenReturn(getIdentityModel(cert));
        when(uriInfo.getPath()).thenReturn(VitamConfiguration.STATUS_URL);
        when(adminManagementClient.findContextById(anyString()))
            .thenReturn(getTestContext(ContextStatus.ACTIVE, true, null, null));
        internalSecurityFilter.filter(containerRequestContext);
    }

    /**
     * When tenant uri then verify only certificate
     * @throws Exception
     */
    @Test
    @RunWithCustomExecutor
    public void whenTenantUriThenOK() throws Exception {
        // Needs mock subject for login call
        when(httpServletRequest.getAttribute("javax.servlet.request.X509Certificate"))
            .thenReturn(new X509Certificate[] {cert});
        when(httpServletRequest.getHeader(GlobalDataRest.X_TENANT_ID)).thenReturn(TENANT_ID.toString());

        when(internalSecurityClient.findIdentity(any())).thenReturn(getIdentityModel(cert));
        when(uriInfo.getPath()).thenReturn(VitamConfiguration.TENANTS_URL);
        when(adminManagementClient.findContextById(anyString()))
            .thenReturn(getTestContext(ContextStatus.ACTIVE, true, null, null));
        internalSecurityFilter.filter(containerRequestContext);
    }

    /**
     * when other uri then verify tenant KO
     * @throws Exception
     */
    @Test(expected = VitamSecurityException.class)
    @RunWithCustomExecutor
    public void whenOtherUriCheckTenantKO() throws Exception {
        // Needs mock subject for login call
        when(httpServletRequest.getAttribute("javax.servlet.request.X509Certificate"))
            .thenReturn(new X509Certificate[] {cert});
        when(httpServletRequest.getHeader(GlobalDataRest.X_TENANT_ID)).thenReturn(TENANT_ID.toString());

        when(internalSecurityClient.findIdentity(any())).thenReturn(getIdentityModel(cert));
        when(uriInfo.getPath()).thenReturn("/otherUri");
        when(adminManagementClient.findContextById(anyString()))
            .thenReturn(getTestContext(ContextStatus.ACTIVE, true, null, null));
        internalSecurityFilter.filter(containerRequestContext);
    }

    /**
     * When other uri then verify tenant OK
     * @throws Exception
     */
    @Test
    @RunWithCustomExecutor
    public void whenOtherUriCheckTenantOK() throws Exception {
        // Needs mock subject for login call
        when(httpServletRequest.getAttribute("javax.servlet.request.X509Certificate"))
            .thenReturn(new X509Certificate[] {cert});
        when(httpServletRequest.getHeader(GlobalDataRest.X_TENANT_ID)).thenReturn(TENANT_ID.toString());

        when(internalSecurityClient.findIdentity(any())).thenReturn(getIdentityModel(cert));
        when(uriInfo.getPath()).thenReturn("/otherUri");
        when(adminManagementClient.findContextById(anyString()))
            .thenReturn(getTestContext(ContextStatus.ACTIVE, true, "fakeAccessContract", null));
        internalSecurityFilter.filter(containerRequestContext);
    }

    /**
     * When context enable control true and contract in the header is not exists in the context then KO
     * @throws Exception
     */
    @Test(expected = VitamSecurityException.class)
    @RunWithCustomExecutor
    public void whenEnableControlAndAccessExternalThenCheckNotValidContractKO() throws Exception {
        // Needs mock subject for login call
        when(httpServletRequest.getAttribute("javax.servlet.request.X509Certificate"))
            .thenReturn(new X509Certificate[] {cert});
        when(httpServletRequest.getHeader(GlobalDataRest.X_TENANT_ID)).thenReturn(TENANT_ID.toString());
        when(httpServletRequest.getHeader(GlobalDataRest.X_ACCESS_CONTRAT_ID)).thenReturn("NotValideAccessContract");

        when(internalSecurityClient.findIdentity(any())).thenReturn(getIdentityModel(cert));
        when(uriInfo.getPath()).thenReturn("/access-external/");
        when(adminManagementClient.findContextById(anyString()))
            .thenReturn(getTestContext(ContextStatus.ACTIVE, true, "fakeAccessContract", null));
        internalSecurityFilter.filter(containerRequestContext);
    }

    /**
     * When context enable control is true and the contract in the header exists in the context then OK
     * @throws Exception
     */
    @Test
    @RunWithCustomExecutor
    public void whenEnableControlAndAccessExternalThenCheckContractOK() throws Exception {
        // Needs mock subject for login call
        when(httpServletRequest.getAttribute("javax.servlet.request.X509Certificate"))
            .thenReturn(new X509Certificate[] {cert});
        when(httpServletRequest.getHeader(GlobalDataRest.X_TENANT_ID)).thenReturn(TENANT_ID.toString());
        when(httpServletRequest.getHeader(GlobalDataRest.X_ACCESS_CONTRAT_ID)).thenReturn("fakeAccessContract");

        when(internalSecurityClient.findIdentity(any())).thenReturn(getIdentityModel(cert));
        when(uriInfo.getPath()).thenReturn("/access-external/");
        when(adminManagementClient.findContextById(anyString()))
            .thenReturn(getTestContext(ContextStatus.ACTIVE, true, "fakeAccessContract", null));
        internalSecurityFilter.filter(containerRequestContext);
    }



    /**
     * When context enable control is false and contract in the header not exists in the context then OK
     * @throws Exception
     */
    @Test
    @RunWithCustomExecutor
    public void whenNotEnableControlAndAccessExternalThenCheckNotValidContractOK() throws Exception {
        // Needs mock subject for login call
        when(httpServletRequest.getAttribute("javax.servlet.request.X509Certificate"))
            .thenReturn(new X509Certificate[] {cert});
        when(httpServletRequest.getHeader(GlobalDataRest.X_TENANT_ID)).thenReturn(TENANT_ID.toString());
        when(httpServletRequest.getHeader(GlobalDataRest.X_ACCESS_CONTRAT_ID)).thenReturn("NotValideAccessContract");

        when(internalSecurityClient.findIdentity(any())).thenReturn(getIdentityModel(cert));
        when(uriInfo.getPath()).thenReturn("/access-external/");
        when(adminManagementClient.findContextById(anyString()))
            .thenReturn(getTestContext(ContextStatus.ACTIVE, false, "fakeAccessContract", null));
        internalSecurityFilter.filter(containerRequestContext);
    }



    /**
     * When context enable control is true and not ingest contract in the context then KO
     * @throws Exception
     */
    @Test(expected = VitamSecurityException.class)
    @RunWithCustomExecutor
    public void whenEnableControlAndIngestExternalThenCheckEmptyContractFail() throws Exception {
        // Needs mock subject for login call
        when(httpServletRequest.getAttribute("javax.servlet.request.X509Certificate"))
            .thenReturn(new X509Certificate[] {cert});
        when(httpServletRequest.getHeader(GlobalDataRest.X_TENANT_ID)).thenReturn(TENANT_ID.toString());

        when(internalSecurityClient.findIdentity(any())).thenReturn(getIdentityModel(cert));
        when(uriInfo.getPath()).thenReturn("/ingest-external/");
        when(adminManagementClient.findContextById(anyString()))
            .thenReturn(getTestContext(ContextStatus.ACTIVE, true, "notIngestContract", null));
        internalSecurityFilter.filter(containerRequestContext);
    }

    /**
     * When context enable control is true and ingest contract is not empty in the context then OK
     * @throws Exception
     */
    @Test
    @RunWithCustomExecutor
    public void whenEnableControlAndIngestExternalThenCheckContractOK() throws Exception {
        // Needs mock subject for login call
        when(httpServletRequest.getAttribute("javax.servlet.request.X509Certificate"))
            .thenReturn(new X509Certificate[] {cert});
        when(httpServletRequest.getHeader(GlobalDataRest.X_TENANT_ID)).thenReturn(TENANT_ID.toString());

        when(internalSecurityClient.findIdentity(any())).thenReturn(getIdentityModel(cert));
        when(uriInfo.getPath()).thenReturn("/ingest-external/");
        when(adminManagementClient.findContextById(anyString()))
            .thenReturn(getTestContext(ContextStatus.ACTIVE, true, null, "fakeIngestContract"));
        internalSecurityFilter.filter(containerRequestContext);
    }



    /**
     * When context enable control is false and ingest contracts is empty in the context then OK
     * @throws Exception
     */
    @Test
    @RunWithCustomExecutor
    public void whenNotEnableControlAndIngestExternalThenCheckEmptyContractOK() throws Exception {
        // Needs mock subject for login call
        when(httpServletRequest.getAttribute("javax.servlet.request.X509Certificate"))
            .thenReturn(new X509Certificate[] {cert});
        when(httpServletRequest.getHeader(GlobalDataRest.X_TENANT_ID)).thenReturn(TENANT_ID.toString());

        when(internalSecurityClient.findIdentity(any())).thenReturn(getIdentityModel(cert));
        when(uriInfo.getPath()).thenReturn("/ingest-external/");
        when(adminManagementClient.findContextById(anyString()))
            .thenReturn(getTestContext(ContextStatus.ACTIVE, false, "notIngestContract", null));
        internalSecurityFilter.filter(containerRequestContext);
    }


    /**
     * Get Fake Context Model for test
     * @param status
     * @param enableControl
     * @param accessContract
     * @param ingestContract
     * @return
     */
    private RequestResponse getTestContext(ContextStatus status, boolean enableControl, String accessContract,
        String ingestContract) {
        ContextModel contextModel = new ContextModel();
        contextModel.setId("fakeId");
        contextModel.setIdentifier("fakeIdentifier");
        contextModel.setStatus(status);
        contextModel.setEnablecontrol(enableControl);

        if (null != accessContract || null != ingestContract) {
            if (null != accessContract && null == ingestContract) {
                contextModel.setPermissions(
                    Lists.newArrayList(new PermissionModel(TENANT_ID, Sets.newHashSet(accessContract), null)));
            } else if (null == accessContract && null != ingestContract) {
                contextModel.setPermissions(
                    Lists.newArrayList(new PermissionModel(TENANT_ID, null, Sets.newHashSet(ingestContract))));
            } else {
                contextModel.setPermissions(
                    Lists.newArrayList(new PermissionModel(TENANT_ID, Sets.newHashSet(accessContract),
                        Sets.newHashSet(ingestContract))));
            }
        }
        RequestResponse<ContextModel> requestResponse = new RequestResponseOK<ContextModel>().addResult(contextModel);
        return requestResponse;

    }

    /**
     * Get Fake IdentityModel for test
     * @param cert
     * @return
     */
    private Optional<IdentityModel> getIdentityModel(X509Certificate cert) {
        IdentityModel identityModel = new IdentityModel();
        try {
            identityModel.setCertificate(cert.getEncoded());
            identityModel.setContextId(FAKE_CONTEXT_ID);
        } catch (CertificateEncodingException e) {
            e.printStackTrace();
        }

        return Optional.of(identityModel);
    }
}
