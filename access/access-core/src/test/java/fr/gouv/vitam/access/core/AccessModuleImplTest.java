/*******************************************************************************
 * This file is part of Vitam Project.
 * <p>
 * Copyright Vitam (2012, 2015)
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL license as circulated
 * by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL license and that you
 * accept its terms.
 *******************************************************************************/
package fr.gouv.vitam.access.core;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import fr.gouv.vitam.access.common.exception.AccessExecutionException;
import fr.gouv.vitam.access.config.AccessConfiguration;
import fr.gouv.vitam.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.api.exception.MetadataInvalidSelectException;
import fr.gouv.vitam.client.MetaDataClient;
import fr.gouv.vitam.client.MetaDataClientFactory;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;

public class AccessModuleImplTest {

    private static final String HOST = "http:\\localhost:8082";

    private AccessConfiguration conf;

    private AccessModuleImpl accessModuleImpl;

    private MetaDataClientFactory metaDataClientFactory;

    private MetaDataClient metaDataClient;



    private static final String QUERY =
        "{ \"$queries\": [{ \"$path\": \"aaaaa\" }],\"$filter\": { },\"$projection\": {}}";

    @Before
    public void setUp() {
        metaDataClient = mock(MetaDataClient.class);
        metaDataClientFactory = mock(MetaDataClientFactory.class);
        conf = new AccessConfiguration();
        conf.setUrlMetaData(HOST);

    }



    @Test
    public void given_correct_dsl_When_select_thenOK()
        throws Exception {
        when(metaDataClient.selectUnits(anyObject())).thenReturn(JsonHandler.createObjectNode());
        when(metaDataClientFactory.create(anyObject())).thenReturn(metaDataClient);
        accessModuleImpl = new AccessModuleImpl(metaDataClientFactory, conf);
        accessModuleImpl.selectUnit(QUERY);
    }

    @Test(expected = IllegalArgumentException.class)
    public void given_empty_dsl_When_select_thenTrows_IllegalArgumentException()
        throws Exception {
        accessModuleImpl = new AccessModuleImpl(metaDataClientFactory, conf);
        accessModuleImpl.selectUnit("");
    }



    @Test(expected = IllegalArgumentException.class)
    public void given_test_AccessExecutionException()
        throws IllegalArgumentException, AccessExecutionException, InvalidParseOperationException {
        when(metaDataClientFactory.create(HOST)).thenReturn(metaDataClient);
        Mockito.doThrow(new IllegalArgumentException("")).when(metaDataClient).selectUnits(QUERY);
        accessModuleImpl = new AccessModuleImpl(conf);
        accessModuleImpl.selectUnit(QUERY);
    }



    @Test(expected = InvalidParseOperationException.class)
    public void given_empty_DSLWhen_select_units_ThenThrows_InvalidParseOperationException()
        throws Exception {
        when(metaDataClient.selectUnits(anyObject())).thenReturn(JsonHandler.createObjectNode());
        when(metaDataClientFactory.create(anyObject())).thenReturn(metaDataClient);
        Mockito.doThrow(new InvalidParseOperationException("")).when(metaDataClient).selectUnits(QUERY);
        accessModuleImpl = new AccessModuleImpl(metaDataClientFactory, conf);
        accessModuleImpl.selectUnit(QUERY);


    }

    @Test(expected = MetadataInvalidSelectException.class)
    public void given__DSLWhen_select_units_ThenThrows_MetadataInvalidSelectException()
        throws Exception {
        when(metaDataClient.selectUnits(anyObject())).thenReturn(JsonHandler.createObjectNode());
        when(metaDataClientFactory.create(anyObject())).thenReturn(metaDataClient);
        Mockito.doThrow(new MetadataInvalidSelectException("")).when(metaDataClient).selectUnits(QUERY);
        accessModuleImpl = new AccessModuleImpl(metaDataClientFactory, conf);
        accessModuleImpl.selectUnit(QUERY);
    }



    @Test(expected = MetaDataDocumentSizeException.class)
    public void given_DSLWhen_select_units_ThenThrows_MetaDataDocumentSizeException()
        throws Exception {
        when(metaDataClient.selectUnits(anyObject())).thenReturn(JsonHandler.createObjectNode());
        when(metaDataClientFactory.create(anyObject())).thenReturn(metaDataClient);
        Mockito.doThrow(new MetaDataDocumentSizeException("")).when(metaDataClient).selectUnits(QUERY);
        accessModuleImpl = new AccessModuleImpl(metaDataClientFactory, conf);
        accessModuleImpl.selectUnit(QUERY);
    }



    @Test(expected = MetaDataExecutionException.class)
    public void given_DSL_When_select_units_ThenThrows_MetaDataExecutionException()
        throws Exception {
        when(metaDataClient.selectUnits(anyObject())).thenReturn(JsonHandler.createObjectNode());
        when(metaDataClientFactory.create(anyObject())).thenReturn(metaDataClient);
        Mockito.doThrow(new MetaDataExecutionException("")).when(metaDataClient).selectUnits(QUERY);
        accessModuleImpl = new AccessModuleImpl(metaDataClientFactory, conf);
        accessModuleImpl.selectUnit(QUERY);
    }


    @Test(expected = AccessExecutionException.class)
    public void given_null_params_conf_When_select_units_ThenThrows_AccessExecutionException()
        throws Exception {
        when(metaDataClientFactory.create(anyObject())).thenReturn(metaDataClient);
        accessModuleImpl = new AccessModuleImpl(metaDataClientFactory, null);
        accessModuleImpl.selectUnit(QUERY);
    }

}
