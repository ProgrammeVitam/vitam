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
package fr.gouv.vitam.storage.offers.tape.impl.catalog;

import com.google.common.collect.Lists;
import fr.gouv.vitam.common.database.server.query.QueryCriteria;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.storage.engine.common.model.TapeCatalog;
import fr.gouv.vitam.storage.engine.common.model.TapeLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeLocationType;
import fr.gouv.vitam.storage.engine.common.model.TapeState;
import fr.gouv.vitam.storage.offers.tape.dto.TapeCartridge;
import fr.gouv.vitam.storage.offers.tape.dto.TapeDrive;
import fr.gouv.vitam.storage.offers.tape.dto.TapeLibrarySpec;
import fr.gouv.vitam.storage.offers.tape.dto.TapeSlot;
import fr.gouv.vitam.storage.offers.tape.dto.TapeSlotType;
import fr.gouv.vitam.storage.offers.tape.exception.TapeCatalogException;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TapeCatalogServiceImplTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private TapeCatalogRepository tapeCatalogRepository;

    @InjectMocks
    private TapeCatalogServiceImpl tapeCatalogService;

    @Test
    public void test_create_tape_ok() throws TapeCatalogException {
        when(tapeCatalogRepository.createTape(any())).thenReturn("FakeTape");
        tapeCatalogService.create(mock(TapeCatalog.class));
        verify(tapeCatalogRepository, times(1)).createTape(any());
    }

    @Test(expected = TapeCatalogException.class)
    public void test_create_tape_ko() throws TapeCatalogException {
        doThrow(new TapeCatalogException("")).when(tapeCatalogRepository).createTape(any());
        tapeCatalogService.create(mock(TapeCatalog.class));
    }

    @Test
    public void test_replace_tape_ok() throws TapeCatalogException {
        when(tapeCatalogRepository.replaceTape(any())).thenReturn(true);
        tapeCatalogService.replace(mock(TapeCatalog.class));
        verify(tapeCatalogRepository, times(1)).replaceTape(any());
    }

    @Test(expected = TapeCatalogException.class)
    public void test_replace_tape_ko() throws TapeCatalogException {
        doThrow(new TapeCatalogException("")).when(tapeCatalogRepository).replaceTape(any());
        tapeCatalogService.replace(mock(TapeCatalog.class));
    }

    @Test
    public void test_update_tape_ok() throws TapeCatalogException {
        when(tapeCatalogRepository.updateTape(anyString(), anyMap())).thenReturn(true);
        tapeCatalogService.update("", new HashMap<>());
        verify(tapeCatalogRepository, times(1)).updateTape(anyString(), anyMap());
    }

    @Test(expected = TapeCatalogException.class)
    public void test_update_tape_ko() throws TapeCatalogException {
        doThrow(new TapeCatalogException("")).when(tapeCatalogRepository).updateTape(anyString(), anyMap());
        tapeCatalogService.update("", new HashMap<>());
    }

    @Test
    public void test_find_by_id_tape_ok() throws TapeCatalogException {
        when(tapeCatalogRepository.findTapeById(anyString())).thenReturn(mock(TapeCatalog.class));
        tapeCatalogService.findById("fakeId");
        verify(tapeCatalogRepository, times(1)).findTapeById(anyString());
    }

    @Test(expected = TapeCatalogException.class)
    public void test_find_by_id_tape_ko() throws TapeCatalogException {
        doThrow(new TapeCatalogException("")).when(tapeCatalogRepository).findTapeById(anyString());
        tapeCatalogService.findById("fakeId");
    }

    @Test
    public void test_find_tape_ok() throws TapeCatalogException {
        when(tapeCatalogRepository.findTapes(anyList())).thenReturn(mock(List.class));
        tapeCatalogService.find(Lists.newArrayList(mock(QueryCriteria.class)));
        verify(tapeCatalogRepository, times(1)).findTapes(anyList());
    }

    @Test(expected = TapeCatalogException.class)
    public void test_find_tape_ko() throws TapeCatalogException {
        doThrow(new TapeCatalogException("")).when(tapeCatalogRepository).findTapes(anyList());
        tapeCatalogService.find(Lists.newArrayList(mock(QueryCriteria.class)));
    }

    @Test
    public void test_init_ok() throws TapeCatalogException {
        Supplier<TapeCatalog> tapeCatalogSupplier = () -> {
            String code = GUIDFactory.newGUID().getId();
            TapeCatalog tapeCatalog = new TapeCatalog();
            tapeCatalog.setId(code);
            tapeCatalog.setCode(code);
            tapeCatalog.setTapeState(TapeState.EMPTY);
            return tapeCatalog;
        };

        String insertedTape = "NewInsertedTape";
        TapeCatalog newInsertedTape = new TapeCatalog();
        newInsertedTape.setCode(insertedTape);

        when(tapeCatalogRepository.findTapeById(anyString())).thenReturn(newInsertedTape);

        List<TapeCatalog> tapeCatalogList = new ArrayList<>();
        List<TapeSlot> tapeSlotList = new ArrayList<>();

        for (int i = 0; i < 20; i++) {
            TapeCatalog tapeCatalog = tapeCatalogSupplier.get();

            if (i < 15) {
                tapeCatalog.setCurrentLocation(new TapeLocation(i, TapeLocationType.SLOT));
                tapeCatalog.setPreviousLocation(tapeCatalog.getCurrentLocation());
            } else {
                tapeCatalog.setCurrentLocation(new TapeLocation(i, TapeLocationType.IMPORTEXPORT));
                tapeCatalog.setPreviousLocation(tapeCatalog.getCurrentLocation());
            }

            tapeCatalogList.add(tapeCatalog);

            // Slot should be returned by robot (mtx) status command
            TapeSlot tapeSlot = new TapeSlot();

            tapeSlot.setIndex(i);

            tapeSlot.setStorageElementType(TapeSlotType.SLOT);

            if (i > 15) {
                tapeSlot.setStorageElementType(TapeSlotType.IMPORTEXPORT);
            }

            if (i != 0 && i != 5) { // SLOT 5 no cartridge
                TapeCartridge cartridge = new TapeCartridge();
                cartridge.setVolumeTag(tapeCatalog.getCode());
                cartridge.setSlotIndex(i);
                tapeSlot.setTape(cartridge);
            }

            tapeSlotList.add(tapeSlot);
            TapeCatalog mocked = new TapeCatalog();
            mocked.setId(tapeCatalog.getId());
            mocked.setCode(tapeCatalog.getCode());

            when(tapeCatalogRepository.findTapeById(eq(tapeCatalog.getId()))).thenReturn(tapeCatalog);
        }

        when(tapeCatalogRepository.findTapes(anyList())).thenReturn(tapeCatalogList);
        when(tapeCatalogRepository.updateTape(anyString(), anyMap())).thenReturn(true);

        TapeLibrarySpec tapeLibrarySpec = mock(TapeLibrarySpec.class);

        TapeDrive tapeDrive1 = mock(TapeDrive.class);
        TapeCartridge tapeCartridge1 = new TapeCartridge();
        tapeCartridge1.setSlotIndex(0);
        tapeCartridge1.setVolumeTag(tapeCatalogList.get(0).getCode());

        when(tapeDrive1.getTape()).thenReturn(tapeCartridge1);
        when(tapeDrive1.getIndex()).thenReturn(0);

        TapeDrive tapeDrive2 = mock(TapeDrive.class);
        TapeCartridge tapeCartridge2 = new TapeCartridge();
        tapeCartridge2.setSlotIndex(30);
        tapeCartridge2.setVolumeTag(insertedTape);
        when(tapeDrive2.getTape()).thenReturn(tapeCartridge2);
        when(tapeDrive2.getIndex()).thenReturn(2);

        when(tapeLibrarySpec.getDrives()).thenReturn(Lists.newArrayList(tapeDrive1, tapeDrive2));

        when(tapeLibrarySpec.getSlots()).thenReturn(tapeSlotList);

        Map<Integer, TapeCatalog> map = tapeCatalogService.init("fakeTapeLibraryIdentifier", tapeLibrarySpec);

        assertThat(map.size()).isEqualTo(2);
        assertThat(map.values()).extracting("code").contains(tapeCatalogList.get(0).getCode(), insertedTape);

        assertThat(tapeCatalogList.get(5).getCurrentLocation().getLocationType()).isEqualTo(TapeLocationType.OUTSIDE);
        assertThat(tapeCatalogList.get(5).getTapeState()).isEqualTo(TapeState.CONFLICT);
    }
}
