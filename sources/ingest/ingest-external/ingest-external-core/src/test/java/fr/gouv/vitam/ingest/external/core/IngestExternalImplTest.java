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
package fr.gouv.vitam.ingest.external.core;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.format.identification.FormatIdentifier;
import fr.gouv.vitam.common.format.identification.FormatIdentifierFactory;
import fr.gouv.vitam.common.format.identification.exception.FileFormatNotFoundException;
import fr.gouv.vitam.common.format.identification.exception.FormatIdentifierBadRequestException;
import fr.gouv.vitam.common.format.identification.exception.FormatIdentifierFactoryException;
import fr.gouv.vitam.common.format.identification.exception.FormatIdentifierNotFoundException;
import fr.gouv.vitam.common.format.identification.exception.FormatIdentifierTechnicalException;
import fr.gouv.vitam.common.format.identification.model.FormatIdentifierResponse;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.server.application.junit.AsyncResponseJunitTest;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.ingest.external.common.config.IngestExternalConfiguration;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClient;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClientFactory;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClientMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

public class IngestExternalImplTest {

    private static final String PATH = "/tmp";
    private static final String SCRIPT_SCAN_CLAMAV = "scan-clamav.sh";
    private static final String SCRIPT_SCAN_CLAMAV_OK = "scan-clamav-ok.sh";
    private static final String CONTEXT_ID = "DEFAULT_WORKFLOW";
    private static final String EXECUTION_MODE = "continu";
    private IngestExternalImpl ingestExternalImpl;
    private InputStream stream;
    private static final Integer TENANT_ID = 0;

    private static final FormatIdentifierFactory formatIdentifierFactory = mock(FormatIdentifierFactory.class);
    private static final FormatIdentifier formatIdentifier = mock(FormatIdentifier.class);

    private IngestInternalClientFactory ingestInternalClientFactory;
    private IngestInternalClient ingestInternalClient;

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    private static final long timeoutScanDelay = 60000;

    @Before
    public void setUp() throws Exception {
        reset(formatIdentifier);
        reset(formatIdentifierFactory);
        when(formatIdentifierFactory.getFormatIdentifierFor(anyString())).thenReturn(formatIdentifier);

        ingestInternalClientFactory = mock(IngestInternalClientFactory.class);
        ingestInternalClient = mock(IngestInternalClient.class);
        when(ingestInternalClientFactory.getClient()).thenReturn(ingestInternalClient);

        when(ingestInternalClient.getWorkflowDetails(anyString())).thenReturn(
            new IngestInternalClientMock().getWorkflowDetails("DEFAULT_WORKFLOW")
        );
        when(ingestInternalClient.cancelOperationProcessExecution(anyString())).thenReturn(new RequestResponseOK<>());

        final IngestExternalConfiguration config = new IngestExternalConfiguration();
        config.setPath(PATH);
        config.setAntiVirusScriptName(SCRIPT_SCAN_CLAMAV);
        config.setTimeoutScanDelay(timeoutScanDelay);
        ManifestDigestValidator manifestDigestValidator = new ManifestDigestValidator();
        ingestExternalImpl = new IngestExternalImpl(
            config,
            formatIdentifierFactory,
            ingestInternalClientFactory,
            manifestDigestValidator
        );
    }

    @RunWithCustomExecutor
    @Test
    public void getFormatIdentifierFactoryThenThrowFormatIdentifierNotFoundException() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(formatIdentifierFactory.getFormatIdentifierFor(any())).thenThrow(
            new FormatIdentifierNotFoundException("")
        );
        stream = PropertiesUtils.getResourceAsStream("no-virus.txt");
        final GUID guid = GUIDFactory.newEventGUID(ParameterHelper.getTenantParameter());
        final AsyncResponseJunitTest responseAsync = new AsyncResponseJunitTest();
        PreUploadResume model = ingestExternalImpl.preUploadAndResume(stream, CONTEXT_ID, guid, null, responseAsync);
        StatusCode statusCode = ingestExternalImpl.upload(model, EXECUTION_MODE, guid, null, null);
        Assert.assertTrue(statusCode.equals(StatusCode.KO));
    }

    @RunWithCustomExecutor
    @Test
    public void getFormatIdentifierFactoryError() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(formatIdentifierFactory.getFormatIdentifierFor(any())).thenThrow(new FormatIdentifierFactoryException(""));
        stream = PropertiesUtils.getResourceAsStream("no-virus.txt");
        final GUID guid = GUIDFactory.newEventGUID(ParameterHelper.getTenantParameter());
        final AsyncResponseJunitTest responseAsync = new AsyncResponseJunitTest();
        PreUploadResume model = ingestExternalImpl.preUploadAndResume(stream, CONTEXT_ID, guid, null, responseAsync);
        StatusCode statusCode = ingestExternalImpl.upload(model, EXECUTION_MODE, guid, null, null);
        Assert.assertTrue(statusCode.equals(StatusCode.KO));
    }

    @RunWithCustomExecutor
    @Test
    public void getFormatIdentifierFactoryThenThrowFormatIdentifierTechnicalException() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(formatIdentifierFactory.getFormatIdentifierFor(any())).thenThrow(
            new FormatIdentifierTechnicalException("")
        );
        stream = PropertiesUtils.getResourceAsStream("no-virus.txt");
        final GUID guid = GUIDFactory.newEventGUID(ParameterHelper.getTenantParameter());
        final AsyncResponseJunitTest responseAsync = new AsyncResponseJunitTest();
        PreUploadResume model = ingestExternalImpl.preUploadAndResume(stream, CONTEXT_ID, guid, null, responseAsync);

        StatusCode statusCode = ingestExternalImpl.upload(model, EXECUTION_MODE, guid, null, null);
        Assert.assertTrue(statusCode.equals(StatusCode.KO));
    }

    @RunWithCustomExecutor
    @Test
    public void getFormatIdentifierFactoryThenThrowFileFormatNotFoundException() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(formatIdentifier.analysePath(any())).thenThrow(new FileFormatNotFoundException(""));
        final GUID guid = GUIDFactory.newEventGUID(ParameterHelper.getTenantParameter());

        stream = PropertiesUtils.getResourceAsStream("no-virus.txt");
        final AsyncResponseJunitTest responseAsync = new AsyncResponseJunitTest();
        PreUploadResume model = ingestExternalImpl.preUploadAndResume(stream, CONTEXT_ID, guid, null, responseAsync);

        StatusCode statusCode = ingestExternalImpl.upload(model, EXECUTION_MODE, guid, null, null);
        Assert.assertTrue(statusCode.equals(StatusCode.KO));
    }

    @RunWithCustomExecutor
    @Test
    public void getFormatIdentifierFactoryThenThrowFormatIdentifierBadRequestException() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(formatIdentifier.analysePath(any())).thenThrow(new FormatIdentifierBadRequestException(""));

        when(ingestInternalClient.cancelOperationProcessExecution(anyString())).thenReturn(new RequestResponseOK<>());
        stream = PropertiesUtils.getResourceAsStream("no-virus.txt");
        final GUID guid = GUIDFactory.newEventGUID(ParameterHelper.getTenantParameter());
        final AsyncResponseJunitTest responseAsync = new AsyncResponseJunitTest();
        PreUploadResume model = ingestExternalImpl.preUploadAndResume(stream, CONTEXT_ID, guid, null, responseAsync);

        StatusCode statusCode = ingestExternalImpl.upload(model, EXECUTION_MODE, guid, null, null);
        Assert.assertTrue(statusCode.equals(StatusCode.KO));
    }

    @RunWithCustomExecutor
    @Test
    public void formatNotSupportedInInternalReferential() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(formatIdentifier.analysePath(any())).thenReturn(getNotSupprtedFormatIdentifierResponseList());
        stream = PropertiesUtils.getResourceAsStream("no-virus.txt");
        final GUID guid = GUIDFactory.newEventGUID(ParameterHelper.getTenantParameter());

        final AsyncResponseJunitTest responseAsync = new AsyncResponseJunitTest();
        PreUploadResume model = ingestExternalImpl.preUploadAndResume(stream, CONTEXT_ID, guid, null, responseAsync);

        StatusCode statusCode = ingestExternalImpl.upload(model, EXECUTION_MODE, guid, null, null);
        Assert.assertTrue(statusCode.equals(StatusCode.KO));
    }

    @RunWithCustomExecutor
    @Test
    public void givenFixedVirusFile() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(formatIdentifier.analysePath(any())).thenReturn(getFormatIdentifierTarResponse());
        stream = PropertiesUtils.getResourceAsStream("fixed-virus.txt");
        final GUID guid = GUIDFactory.newEventGUID(ParameterHelper.getTenantParameter());

        final AsyncResponseJunitTest responseAsync = new AsyncResponseJunitTest();
        PreUploadResume model = ingestExternalImpl.preUploadAndResume(stream, CONTEXT_ID, guid, null, responseAsync);

        StatusCode statusCode = ingestExternalImpl.upload(model, EXECUTION_MODE, guid, null, null);
        Assert.assertTrue(statusCode.equals(StatusCode.KO));
    }

    @RunWithCustomExecutor
    @Test
    public void givenUnFixedVirusFileAndSupportedMediaType() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(formatIdentifier.analysePath(any())).thenReturn(getFormatIdentifierTarResponse());
        stream = PropertiesUtils.getResourceAsStream("unfixed-virus.txt");
        final GUID guid = GUIDFactory.newEventGUID(ParameterHelper.getTenantParameter());
        final AsyncResponseJunitTest responseAsync = new AsyncResponseJunitTest();
        PreUploadResume model = ingestExternalImpl.preUploadAndResume(stream, CONTEXT_ID, guid, null, responseAsync);

        StatusCode statusCode = ingestExternalImpl.upload(model, EXECUTION_MODE, guid, null, null);
        Assert.assertTrue(statusCode.equals(StatusCode.KO));
    }

    private List<FormatIdentifierResponse> getFormatIdentifierZipResponse() {
        final List<FormatIdentifierResponse> list = new ArrayList<>();
        list.add(new FormatIdentifierResponse("ZIP Format", "application/zip", "x-fmt/263", "pronom"));
        return list;
    }

    @RunWithCustomExecutor
    @Test
    public void givenNoVirusFile() throws Exception {
        setVirusScanScript(SCRIPT_SCAN_CLAMAV_OK);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(formatIdentifier.analysePath(any())).thenReturn(getFormatIdentifierZipResponse());
        stream = PropertiesUtils.getResourceAsStream("toto_manifest.xml_OK.zip");
        final AsyncResponseJunitTest responseAsync = new AsyncResponseJunitTest();
        final GUID guid = GUIDFactory.newEventGUID(ParameterHelper.getTenantParameter());
        PreUploadResume model = ingestExternalImpl.preUploadAndResume(stream, CONTEXT_ID, guid, null, responseAsync);
        StatusCode statusCode = ingestExternalImpl.upload(model, EXECUTION_MODE, guid, null, null);
        Assert.assertTrue(statusCode.equals(StatusCode.OK));
    }

    @RunWithCustomExecutor
    @Test
    public void givenValidManifestFileNameWithPrefix1() throws Exception {
        setVirusScanScript(SCRIPT_SCAN_CLAMAV_OK);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(formatIdentifier.analysePath(any())).thenReturn(getFormatIdentifierZipResponse());
        stream = PropertiesUtils.getResourceAsStream("toto_manifest.xml_OK.zip");
        final AsyncResponseJunitTest responseAsync = new AsyncResponseJunitTest();
        final GUID guid = GUIDFactory.newEventGUID(ParameterHelper.getTenantParameter());
        PreUploadResume model = ingestExternalImpl.preUploadAndResume(stream, CONTEXT_ID, guid, null, responseAsync);

        StatusCode statusCode = ingestExternalImpl.upload(model, EXECUTION_MODE, guid, null, null);
        Assert.assertTrue(statusCode.equals(StatusCode.OK));
    }

    @RunWithCustomExecutor
    @Test
    public void givenValidManifestFileNameWithPrefix2() throws Exception {
        setVirusScanScript(SCRIPT_SCAN_CLAMAV_OK);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(formatIdentifier.analysePath(any())).thenReturn(getFormatIdentifierZipResponse());
        stream = PropertiesUtils.getResourceAsStream("toto-manifest.xml_OK.zip");
        final AsyncResponseJunitTest responseAsync = new AsyncResponseJunitTest();
        final GUID guid = GUIDFactory.newEventGUID(ParameterHelper.getTenantParameter());
        PreUploadResume model = ingestExternalImpl.preUploadAndResume(stream, CONTEXT_ID, guid, null, responseAsync);

        StatusCode statusCode = ingestExternalImpl.upload(model, EXECUTION_MODE, guid, null, null);
        Assert.assertTrue(statusCode.equals(StatusCode.OK));
    }

    @RunWithCustomExecutor
    @Test
    public void givenValidManifestFileNameWithPrefix3() throws Exception {
        setVirusScanScript(SCRIPT_SCAN_CLAMAV_OK);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(formatIdentifier.analysePath(any())).thenReturn(getFormatIdentifierZipResponse());
        stream = PropertiesUtils.getResourceAsStream("_manifest.xml_OK.zip");
        final AsyncResponseJunitTest responseAsync = new AsyncResponseJunitTest();
        final GUID guid = GUIDFactory.newEventGUID(ParameterHelper.getTenantParameter());
        PreUploadResume model = ingestExternalImpl.preUploadAndResume(stream, CONTEXT_ID, guid, null, responseAsync);

        StatusCode statusCode = ingestExternalImpl.upload(model, EXECUTION_MODE, guid, null, null);
        Assert.assertTrue(statusCode.equals(StatusCode.OK));
    }

    @RunWithCustomExecutor
    @Test
    public void givenInvalidManifestFileNameWithIllegalChars() throws Exception {
        setVirusScanScript(SCRIPT_SCAN_CLAMAV_OK);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(formatIdentifier.analysePath(any())).thenReturn(getFormatIdentifierZipResponse());
        stream = PropertiesUtils.getResourceAsStream("$_manifest.xml_KO.zip");
        final AsyncResponseJunitTest responseAsync = new AsyncResponseJunitTest();
        final GUID guid = GUIDFactory.newEventGUID(ParameterHelper.getTenantParameter());
        PreUploadResume model = ingestExternalImpl.preUploadAndResume(stream, CONTEXT_ID, guid, null, responseAsync);

        StatusCode statusCode = ingestExternalImpl.upload(model, EXECUTION_MODE, guid, null, null);
        Assert.assertTrue(statusCode.equals(StatusCode.KO));
    }

    @RunWithCustomExecutor
    @Test
    public void givenInvalidManifestFileNameWithLengthGreaterThan56Chars() throws Exception {
        setVirusScanScript(SCRIPT_SCAN_CLAMAV_OK);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(formatIdentifier.analysePath(any())).thenReturn(getFormatIdentifierZipResponse());
        stream = PropertiesUtils.getResourceAsStream("$_manifest.xml_KO.zip");
        final AsyncResponseJunitTest responseAsync = new AsyncResponseJunitTest();
        final GUID guid = GUIDFactory.newEventGUID(ParameterHelper.getTenantParameter());
        PreUploadResume model = ingestExternalImpl.preUploadAndResume(stream, CONTEXT_ID, guid, null, responseAsync);

        StatusCode statusCode = ingestExternalImpl.upload(model, EXECUTION_MODE, guid, null, null);
        Assert.assertTrue(statusCode.equals(StatusCode.KO));
    }

    private List<FormatIdentifierResponse> getFormatIdentifierTarResponse() {
        final List<FormatIdentifierResponse> list = new ArrayList<>();
        list.add(new FormatIdentifierResponse("TAR Format", "application/x-tar", "x-fmt/263", "pronom"));
        return list;
    }

    private List<FormatIdentifierResponse> getNotSupprtedFormatIdentifierResponseList() {
        final List<FormatIdentifierResponse> list = new ArrayList<>();
        list.add(new FormatIdentifierResponse("xsd Format", "application/xsd", "x-fmt/263", "pronom"));
        return list;
    }

    private void setVirusScanScript(String script) {
        final IngestExternalConfiguration config = new IngestExternalConfiguration();
        config.setPath(PATH);
        config.setAntiVirusScriptName(script);
        config.setTimeoutScanDelay(timeoutScanDelay);
        ManifestDigestValidator manifestDigestValidator = new ManifestDigestValidator();
        ingestExternalImpl = new IngestExternalImpl(
            config,
            formatIdentifierFactory,
            ingestInternalClientFactory,
            manifestDigestValidator
        );
    }

    @RunWithCustomExecutor
    @Test
    public void givenValidManifestDigestThenOK() throws Exception {
        setVirusScanScript(SCRIPT_SCAN_CLAMAV_OK);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(formatIdentifier.analysePath(any())).thenReturn(getFormatIdentifierZipResponse());
        stream = PropertiesUtils.getResourceAsStream("toto_manifest.xml_OK.zip");
        final AsyncResponseJunitTest responseAsync = new AsyncResponseJunitTest();
        final GUID guid = GUIDFactory.newEventGUID(ParameterHelper.getTenantParameter());
        PreUploadResume model = ingestExternalImpl.preUploadAndResume(stream, CONTEXT_ID, guid, null, responseAsync);

        StatusCode statusCode = ingestExternalImpl.upload(
            model,
            EXECUTION_MODE,
            guid,
            "fcfa0fdd2f27e45c4c80936f99d4144159590e010eb2e9ec7bd1af2165d65e265501d902e9c4c5a858ffd4cd8dba0ca937d0bc327c2518c08c86d355c5a34efa",
            "SHA-512"
        );
        Assert.assertTrue(statusCode.equals(StatusCode.OK));
    }

    @RunWithCustomExecutor
    @Test
    public void givenMissingManifestDigestAlgoThenKO() throws Exception {
        setVirusScanScript(SCRIPT_SCAN_CLAMAV_OK);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(formatIdentifier.analysePath(any())).thenReturn(getFormatIdentifierZipResponse());
        stream = PropertiesUtils.getResourceAsStream("toto_manifest.xml_OK.zip");
        final AsyncResponseJunitTest responseAsync = new AsyncResponseJunitTest();
        final GUID guid = GUIDFactory.newEventGUID(ParameterHelper.getTenantParameter());
        PreUploadResume model = ingestExternalImpl.preUploadAndResume(stream, CONTEXT_ID, guid, null, responseAsync);

        StatusCode statusCode = ingestExternalImpl.upload(
            model,
            EXECUTION_MODE,
            guid,
            "fcfa0fdd2f27e45c4c80936f99d4144159590e010eb2e9ec7bd1af2165d65e265501d902e9c4c5a858ffd4cd8dba0ca937d0bc327c2518c08c86d355c5a34efa",
            null
        );
        Assert.assertTrue(statusCode.equals(StatusCode.KO));
    }

    @RunWithCustomExecutor
    @Test
    public void givenMissingManifestDigestValueThenKO() throws Exception {
        setVirusScanScript(SCRIPT_SCAN_CLAMAV_OK);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(formatIdentifier.analysePath(any())).thenReturn(getFormatIdentifierZipResponse());
        stream = PropertiesUtils.getResourceAsStream("toto_manifest.xml_OK.zip");
        final AsyncResponseJunitTest responseAsync = new AsyncResponseJunitTest();
        final GUID guid = GUIDFactory.newEventGUID(ParameterHelper.getTenantParameter());
        PreUploadResume model = ingestExternalImpl.preUploadAndResume(stream, CONTEXT_ID, guid, null, responseAsync);

        StatusCode statusCode = ingestExternalImpl.upload(model, EXECUTION_MODE, guid, null, "SHA-512");
        Assert.assertTrue(statusCode.equals(StatusCode.KO));
    }

    @RunWithCustomExecutor
    @Test
    public void givenInvalidManifestDigestThenKO() throws Exception {
        setVirusScanScript(SCRIPT_SCAN_CLAMAV_OK);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(formatIdentifier.analysePath(any())).thenReturn(getFormatIdentifierZipResponse());
        stream = PropertiesUtils.getResourceAsStream("toto_manifest.xml_OK.zip");
        final AsyncResponseJunitTest responseAsync = new AsyncResponseJunitTest();
        final GUID guid = GUIDFactory.newEventGUID(ParameterHelper.getTenantParameter());
        PreUploadResume model = ingestExternalImpl.preUploadAndResume(stream, CONTEXT_ID, guid, null, responseAsync);

        StatusCode statusCode = ingestExternalImpl.upload(
            model,
            EXECUTION_MODE,
            guid,
            "baddigestf27e45c4c80936f99d4144159590e010eb2e9ec7bd1af2165d65e265501d902e9c4c5a858ffd4cd8dba0ca937d0bc327c2518c08c86d355c5a34efa",
            "SHA-512"
        );
        Assert.assertTrue(statusCode.equals(StatusCode.KO));
    }

    @RunWithCustomExecutor
    @Test
    public void givenInvalidManifestAlgoThenKO() throws Exception {
        setVirusScanScript(SCRIPT_SCAN_CLAMAV_OK);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(formatIdentifier.analysePath(any())).thenReturn(getFormatIdentifierZipResponse());
        stream = PropertiesUtils.getResourceAsStream("toto_manifest.xml_OK.zip");
        final AsyncResponseJunitTest responseAsync = new AsyncResponseJunitTest();
        final GUID guid = GUIDFactory.newEventGUID(ParameterHelper.getTenantParameter());
        PreUploadResume model = ingestExternalImpl.preUploadAndResume(stream, CONTEXT_ID, guid, null, responseAsync);

        StatusCode statusCode = ingestExternalImpl.upload(
            model,
            EXECUTION_MODE,
            guid,
            "fcfa0fdd2f27e45c4c80936f99d4144159590e010eb2e9ec7bd1af2165d65e265501d902e9c4c5a858ffd4cd8dba0ca937d0bc327c2518c08c86d355c5a34efa",
            "Unknown"
        );
        Assert.assertTrue(statusCode.equals(StatusCode.KO));
    }
}
