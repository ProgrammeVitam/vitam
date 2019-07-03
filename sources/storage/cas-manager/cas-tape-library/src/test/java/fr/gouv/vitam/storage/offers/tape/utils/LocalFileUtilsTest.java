package fr.gouv.vitam.storage.offers.tape.utils;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class LocalFileUtilsTest {

    @Test
    public void test_tar_and_zip_naming() {


        String tarId = "20190701165216262-a8e7c997-3c44-4119-b169-5d2000015697.tar";
        String backupId = "20190701165216263-mongod-shard12.zip";
        String date = LocalFileUtils.getCreationDateFromArchiveId(tarId);
        Assertions.assertThat(tarId).contains(date);
        date = LocalFileUtils.getCreationDateFromArchiveId(backupId);
        Assertions.assertThat(backupId).contains(date);


    }
}