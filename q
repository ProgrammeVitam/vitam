[33mcommit 36528b23a585f3d4cfd53a007be8626e22919e89[m
Author: Ramzi LAZREG <ramzy.lazreg.ext@culture.gouv.fr>
Date:   Mon Oct 17 10:11:52 2016 +0200

    fix rebase

[33mcommit e001c18850a505d1fb0aa999513cc168d8c032bd[m
Author: Ramzi LAZREG <ramzy.lazreg.ext@culture.gouv.fr>
Date:   Fri Oct 14 18:18:00 2016 +0200

    fix rebase

[33mcommit 02384d44f686d8f195b3df4598f92bb2ca8263b6[m
Author: Ramzi LAZREG <ramzy.lazreg.ext@culture.gouv.fr>
Date:   Fri Oct 14 18:10:11 2016 +0200

    fix rebase

[33mcommit ce21276f4928ddb2b4748cefed353aae4031a472[m
Author: Ramzi LAZREG <ramzy.lazreg.ext@culture.gouv.fr>
Date:   Fri Oct 14 17:22:42 2016 +0200

    fix rebase

[33mcommit c3a7532331e1ffb991e1e4d127dbebe2a7e8bafb[m
Author: Ramzi LAZREG <ramzy.lazreg.ext@culture.gouv.fr>
Date:   Fri Oct 14 11:02:33 2016 +0200

    Detect format in external ingest and uncompress sip in workspace

[33mcommit f26836caf48e734f950b2596878c44d2f6c57c1f[m
Merge: 320c41c 4ae8cb1
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Fri Oct 14 13:32:07 2016 +0000

    Merge branch 'item_1208' into 'master_iteration_10'
    
    rpm for kopf
    
    
    
    See merge request !741

[33mcommit 4ae8cb12ac3e94487b6f830eb8b86bff1e36ab8f[m
Author: Olivier MARSOL <olivier.marsol.ext@culture.gouv.fr>
Date:   Fri Oct 14 14:25:55 2016 +0200

    rpm for kopf

[33mcommit 320c41c708468a1a9744e012ffb1027f74d1167e[m
Merge: 1056baa 1672421
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Fri Oct 14 11:26:59 2016 +0000

    Merge branch 'item_messages_ops' into 'master_iteration_10'
    
    CleanUp next and Add functionalities
    
    - Add logback-test.xml and server-identity.conf to all modules
    - Add "_ops" to Insert (AU and GOT) and Update (AU)
    - Add Messages and VitamMessages into Common-public
    - Fix small issues on Diff when null
    - Add control on UpdateById on Unit to check if it is really an Update
    
    See merge request !739

[33mcommit 167242118be0b0e8396908b706978ed91a1f25ae[m
Author: Frederic Bregier <frederic.bregier@culture.gouv.fr>
Date:   Fri Oct 14 12:24:29 2016 +0200

    Fix bad refactorisation on Storage

[33mcommit c7f07d3a1c34ee6809781cf1daf4ce90e68585a2[m
Author: Frederic Bregier <frederic.bregier@culture.gouv.fr>
Date:   Fri Oct 14 11:21:46 2016 +0200

    CleanUp next and Add functionalities
    
    - Add logback-test.xml and server-identity.conf to all modules
    - Add "_ops" to Insert (AU and GOT) and Update (AU)
    - Add Messages and VitamMessages into Common-public
    - Fix small issues on Diff when null
    - Add control on UpdateById on Unit to check if it is really an Update

[33mcommit 1056baad43fc7f0ac806a95f732bdb43598ec390[m
Merge: adb278d 9eca6a1
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Fri Oct 14 10:37:35 2016 +0000

    Merge branch 'fix_master' into 'master_iteration_10'
    
    Fix master build
    
    
    
    See merge request !740

[33mcommit 9eca6a125dcae435c5e61456596c26feec9d3607[m
Author: NGO BA TUAN <ba-tuan.ngo.ext@culture.gouv.fr>
Date:   Fri Oct 14 12:04:52 2016 +0200

    Fix master build

[33mcommit adb278d88f4c073c07eccd1fdc62108566d1497f[m
Merge: 9174d16 52b7d85
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Fri Oct 14 08:21:11 2016 +0000

    Merge branch 'item_cleanup' into 'master_iteration_10'
    
    Clean up step
    
    Other additions or changes:
    
    - Add some methods to VitamLogger to allow changing default Log Level
    - Fix storage-drivers to include storage-default-driver and not default-driver
    - Refactor Metadata from fr.gouv.vitam to fr.gouv.vitam.metadata
    - Add Common-http-interface that depends on Common-public and is in transitive dependency with Common-Juint and Common-Private
    - Workflow is externalisable in config directory
    - CleanUp all codes
    - Prepare for Next Step client/server Commont http
    
    See merge request !736

[33mcommit 52b7d856b6c77ffa11dfbebcb3a5da007ffedfa7[m
Author: Frederic BREGIER <nomail@nomail.com>
Date:   Fri Oct 14 08:50:36 2016 +0200

    Clean up step
    
    Other additions or changes:
    
    - Add some methods to VitamLogger to allow changing default Log Level
    - Fix storage-drivers to include storage-default-driver and not default-driver
    - Refactor Metadata from fr.gouv.vitam to fr.gouv.vitam.metadata
    - Add Common-http-interface that depends on Common-public and is in transitive dependency with Common-Juint and Common-Private
    - Workflow is externalisable in config directory
    - CleanUp all codes
    - Prepare for Next Step client/server Commont http

[33mcommit 9174d162a1232c0bcfaf7254e153fa1f243a1a90[m
Merge: accb71e cda4b17
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Fri Oct 14 06:33:37 2016 +0000

    Merge branch 'item_744_753' into 'master_iteration_10'
    
    Search on Fund register (story_744_753) : Front end
    
    - Ecran de recherche sur les registres de fonds
    
    See merge request !735

[33mcommit accb71e9b10e1e9136500c4436f960cfce9f14b7[m
Merge: d5cff5a 99bd563
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Fri Oct 14 06:33:11 2016 +0000

    Merge branch 'fix_redirect' into 'master_iteration_10'
    
    Redirect when session expired
    
    
    
    See merge request !730

[33mcommit d5cff5a22f8d344788c7f3efda26beafcc5ff928[m
Merge: 6599546 f2eca0b
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Fri Oct 14 06:02:05 2016 +0000

    Merge branch 'fix_int_port_mapping' into 'master_iteration_10'
    
    INT : add port 8443 mapping on docker
    
    
    
    See merge request !733

[33mcommit cda4b17334eff966720f13ac72d098008f1cec4b[m
Author: Hela Amri <hela.amri.ext@culture.gouv.fr>
Date:   Thu Oct 13 17:13:39 2016 +0200

    story_744_753 : Front end

[33mcommit f2eca0b44e36ded1d4afc3e746c32f73d589410e[m
Author: gafou <gaelle.fournier@smile.fr>
Date:   Thu Oct 13 15:06:14 2016 +0200

    INT : add port 8443 mapping on docker

[33mcommit 99bd563a99d2b1529b5740bf36ca22dada93e6b2[m
Author: NGO BA TUAN <ba-tuan.ngo.ext@culture.gouv.fr>
Date:   Thu Oct 13 11:59:30 2016 +0200

    Redirect when session expired

[33mcommit 6599546f791a29256d98f9bdc01852960c8a15b1[m
Author: jgonthier <joachim.gonthier.ext@culture.gouv.fr>
Date:   Thu Oct 13 11:40:53 2016 +0200

    Update pom from 0.9.0-SNAPSHOT to 0.10.0-SNAPSHOT

[33mcommit 1cabfd407417c1b73dd58d2d724e9dc053e08837[m
Merge: b6f6202 90e795c
Author: Joachim Gonthier <joachim.gonthier.ext@culture.gouv>
Date:   Thu Oct 13 08:46:48 2016 +0000

    Merge branch 'master_iteration_9' into 'master'
    
    Master iteration 9
    
    Merge branch 'master_iteration_9' into 'master'
    
    See merge request !729

[33mcommit 90e795cedaaa6e09ee6926e81eb908d5d1ef19cf[m
Author: Olivier MARSOL <olivier.marsol.ext@culture.gouv.fr>
Date:   Fri Oct 7 12:50:53 2016 +0200

    Global fixes for iteration 9
    
    added siegfried conf files && port variable
    fixed bad filenames
    documentation : manuel utilisateur
    Moved vitam sudoers to sudoers.d and made it work with docker
    Fixed conf-common.py in doc/fr to make it detect again vitam version
    1144 - Fix word wrap
    Purge the inputStream when uploading (and unzipping) a zipFile to the workspace
    story_769 (IHM) + bugs 1148-1149-1150-1016
    story_769:Server side
    Fix Issues Related to Rules Management
    
    FIX STORAGE (ConnectionImpl):
    
    - check response when put object (no end of file case)
    - fix end of file case error
    - Add common FakeInputStream
    - Use FakeInputStream in test with "big" file
    
    Modified conf-common.py
    removed useless file
    
    Item #878 - Format Identification
    - Modification of Workflow
    - Implementation of the New Handler (FormatIdentification)
    - Added documentation for the new Handler
    - Creation of a constant class SedaConstant
    - Usage of the constant class in the Handlers
    - Tests
    - Add some TODOs
    - Correction of many fixme (blockers + majots)
    - Fixed missing package-info
    
    story_769:Correct IHM bug
    story_769:Unit tests
    Fixed start & stop for Vitam
    Modified start order
    Test if DBs port are open
    Included data-model documentation in root documentation
    Fixed some issues with local deployment
    Aggregate result in the logbook message
    Item_75 Notification backend
    Item_75 Notification frontend
    Fix Bad argument in OUT to ExtractSeda
    Correction regression fonctionnelle sur les formats
    Fix RAML template for updated version beta7
    fix CheckObjectUnitConsistencyActionHandler
    fix versions map
    fix OutcomeMessage
    Fixed sudoers for vitamdb-admin
    Moved sudoers.d files in %config (replace) in in the rpm
    Modified sudoers documentation according to the RPMs %config directives
    Added documentation about the NOPASSWD conf
    add integration Test - Binary Object withou Object Group
    item_789 : update doc worker
    
    FIX IT9 - Item 878 - NPE on format identification
    - When a SIP contains an empty FormatIdentification Tag, a npe was thrown
    --> Fixed it + added 2 tests
        - one with a sip with an empty formatIdentification Tag
        - one with no formatIdentification Tag (Bad Request must be thrown)
    
    update status to OK when objects List is empty
    
    FIX STORAGE :
    - Temporarily remove chunk management in default offer
    
    fix bug create double unit logbook life cycle
    Fix missing evDetData column
    Fix labels in multiselect
    Update wording 'Page daccueil' in all page
    item 871: ihm fix bug
    Fix multiselect size bug
    Fix logbook pagination
    Fixed a check for local docker deployment
    Added full packaging for a release.
    timeout on reverse enlarged for ihm-demo

[33mcommit 53d323aa2afe4eca5024c3662351904d593ea148[m
Merge: 6d1ef5f e5f016f
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Fri Oct 7 13:19:19 2016 +0000

    Merge branch 'item_1031' into 'master_iteration_9'
    
    #1031/1037 - Add dynamic information for AU/GOT lifecycle
    
    Add lifecycle dynamic array directive
    Add internalization first step
    Merge on #866 / #867 page
    
    See merge request !682

[33mcommit e5f016fd3301fdcf7c6050f280fb44d2b21e8ff0[m
Author: lubla <lubla@smile.fr>
Date:   Mon Oct 3 17:21:13 2016 +0200

    Add lifecycle dynamic array directive
    Add internalization first step
    Merge 866/867
    Add doc

[33mcommit 6d1ef5f52863da1d21cd07f008a58aec0ea147ea[m
Merge: 6bc2850 82c1c37
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Fri Oct 7 10:14:53 2016 +0000

    Merge branch 'fix_int_local_deploy_ports' into 'master_iteration_9'
    
    FIX INTEG : local deploy
    
    Problem with limit of docker on number of ports for Fedora
    Tested by Jérémie
    
    See merge request !681

[33mcommit 82c1c3716d6ec5a422d092025d43d6bc46f1ca0a[m
Author: gafou <gaelle.fournier@smile.fr>
Date:   Fri Oct 7 10:26:14 2016 +0200

    FIX INTEG : local deploy
    
    Problem with limit of docker on number of ports for Fedora
    Tested by Jérémie

[33mcommit 6bc2850b65040c3c292ae0a13560884b3b5c942e[m
Merge: 0168825 1641452
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Thu Oct 6 16:23:46 2016 +0000

    Merge branch 'fix_kopf' into 'master_iteration_9'
    
    Usage kopf
    
    removed change in kopf_external_settings.json ; now kopf can access both elasticsearch-data & elasticsearch-log ; homepage updated
    
    See merge request !676

[33mcommit 01688251905c3ba16e426bea53bce9853255d053[m
Merge: d5bd40c d7e90e2
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Thu Oct 6 15:30:29 2016 +0000

    Merge branch 'item_1102' into 'master_iteration_9'
    
    Refactor processing with blocking/noblocking actions
    
    
    
    See merge request !658

[33mcommit 16414522383f07cb4cb436090dbfebb9652728dd[m
Author: Olivier MARSOL <olivier.marsol.ext@culture.gouv.fr>
Date:   Thu Oct 6 17:25:55 2016 +0200

    removed change in kopf_external_settings.json ; now kopf can access both elasticsearch-data & elasticsearch-log ; homepage updated

[33mcommit d5bd40ce3ff96b57d55b673c14dcd13dcd65a026[m
Merge: 8ebd46d 7b40e76
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Thu Oct 6 15:21:05 2016 +0000

    Merge branch 'item_933_934_Status_Issues' into 'master_iteration_9'
    
    Status Issues Related to Worker and Offer Default
    
     Status Issues Related to Worker and Offer Default
    
    See merge request !675

[33mcommit 8ebd46dda65b030443cf793db764411cdae8b3e5[m
Merge: ca519a8 5ba21a1
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Thu Oct 6 15:16:04 2016 +0000

    Merge branch 'integration_iteration_9' into 'master_iteration_9'
    
    Integration related :
    
    - Improved local deployment (ES memory, UX for dev, helper scripts,
      default external dns, ...)
    - More consul checks ; integrated consul services resolution for all
      clients ;
    - ES ansible role refactoring ;
    - DIN/DEX doc ;
    - logback Size and Time rolling policy
    
    See merge request !674

[33mcommit 7b40e76c0028bbf2ea4f8745c11c3493e447d7d1[m
Author: Haykel <haykel.benmassaoud.ext@culture.gouv.fr>
Date:   Thu Oct 6 11:17:40 2016 +0200

     Status Issues Related to Worker and Offer Default

[33mcommit 5ba21a183e9a8bf440f7e7a3939045bcdfcd1dfc[m
Author: Kristopher Waltzer <kristopher.waltzer.ext@culture.gouv.fr>
Date:   Thu Oct 6 15:32:28 2016 +0200

    Integration related :
    - Improved local deployment (ES memory, UX for dev, helper scripts,
      default external dns, ...)
    - More consul checks ; integrated consul services resolution for all
      clients ;
    - ES ansible role refactoring ;
    - DIN/DEX doc ;
    - logback Size and Time rolling policy

[33mcommit ca519a8722a6e2f90a9ce3f6af0babc6079b8bbc[m
Merge: f36865c 9312961
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Thu Oct 6 13:09:49 2016 +0000

    Merge branch 'fix_logbook' into 'master_iteration_9'
    
    Fix logbook detail url
    
    
    
    See merge request !673

[33mcommit 9312961b273b4a13c128286a968347f39cf85399[m
Author: NGO BA TUAN <ba-tuan.ngo.ext@culture.gouv.fr>
Date:   Thu Oct 6 14:52:43 2016 +0200

    Fix logbook detail url

[33mcommit f36865cde3376985d059808779176a45718c35fe[m
Merge: 80df9d5 04afc4f
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Thu Oct 6 12:48:18 2016 +0000

    Merge branch 'item_868' into 'master_iteration_9'
    
    story_866 & 867 : display lifeCycle details (Unit + ObjectGroup)
    
    
    
    See merge request !672

[33mcommit d7e90e25f36481bf7906f2cef09815f5b8ad9544[m
Author: Yousri KOUKI <yousri.kouki.ext@culture.gouv.fr>
Date:   Mon Oct 3 16:31:13 2016 +0200

     Refactor processing with blocking/noblocking actions

[33mcommit 04afc4f6b1bedc9b18a2ee9074e8e326e9419b44[m
Author: Hela Amri <hela.amri.ext@culture.gouv.fr>
Date:   Thu Oct 6 13:54:49 2016 +0200

    story_866 & 867 : Server side

[33mcommit 6329f564a1665cd083420dacaaebbe0e61bb5ca4[m
Author: Hela Amri <hela.amri.ext@culture.gouv.fr>
Date:   Thu Oct 6 13:53:56 2016 +0200

    story_866 & 867 : Front side

[33mcommit 80df9d5ce8b693f77e82da8a00ec6320b14c049a[m
Merge: 81c556d 007b42c
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Thu Oct 6 11:19:12 2016 +0000

    Merge branch 'item_789_from_it9' into 'master_iteration_9'
    
    item_789 : parse the manifest succesully when BDO not contains GO
    
    - Create GOT and update map AU and BDO
    - Link the GOT to the BDO
    - Link the GOT to the AU instead of BDO
    - Add node DataObjectVersion when it is empty after the parse
    - Create lifecycle for the GOT
    
    See merge request !655

[33mcommit 81c556d90362e3581e90b19f77f4b400301bf6c1[m
Merge: f363415 c851611
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Thu Oct 6 11:15:11 2016 +0000

    Merge branch 'fix_storage_integration_test' into 'master_iteration_9'
    
    FIX : storage integration test
    
    * Fix : Add jar and driver spec in integration test resources
    * Improvement : Move integration test into its own module
    * Improvement : Clean storage-engine-client module from integration configurations and dependencies
    
    See merge request !671

[33mcommit f36341585d457cf2f6e15fa6b2ca3c370b6807f6[m
Merge: c4eab43 3a4cb1f
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Thu Oct 6 11:13:07 2016 +0000

    Merge branch 'bug_1193' into 'master_iteration_9'
    
    #1193 - AU: Fix update field
    
    Replace input field by textarea for all fields
    
    See merge request !669

[33mcommit c4eab432a77e3371ae37eba795ec7db357065324[m
Merge: 2c940d5 ae1cdcf
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Thu Oct 6 11:08:53 2016 +0000

    Merge branch 'item_488' into 'master_iteration_9'
    
    Rest Pagination
    
    Work In Progress
    
    See merge request !643

[33mcommit 3a4cb1f4b0a24ee0de5607959a8293ac233ff21a[m
Author: lubla <lubla@smile.fr>
Date:   Tue Oct 4 17:09:15 2016 +0200

    BUG #1193 : Replace input field by textarea

[33mcommit 007b42c947f7d70d6b08bab21d93e025feb1dc01[m
Author: Buon SUI <buon.sui.ext@culture.gouv.fr>
Date:   Thu Oct 6 11:10:11 2016 +0200

    item_789 : accept binary data object without object group

[33mcommit c851611bf6bc163ab86dd20847fb15d856c4c07b[m
Author: gafou <gaelle.fournier@smile.fr>
Date:   Wed Oct 5 18:52:39 2016 +0200

    FIX : storage integration test
    
    * Fix : Add jar and driver spec in integration test resources
    * Improvement : Move integration test into its own module
    * Improvement : Clean storage-engine-client module from integration configurations and dependencies

[33mcommit ae1cdcf6546616410cb7cd2d74d11393dfd5fba5[m
Author: NGO BA TUAN <ba-tuan.ngo.ext@culture.gouv.fr>
Date:   Mon Oct 3 13:54:54 2016 +0200

    Pagination Front part

[33mcommit 24f12016ed17de8a70c8a843198ca7648ee19d92[m
Author: Yousri KOUKI <yousri.kouki.ext@culture.gouv.fr>
Date:   Mon Oct 3 10:32:54 2016 +0200

    Pagination Back side

[33mcommit 2c940d5699c25aca62c72fb04d304a78b59246da[m
Merge: d448061 1e6ab45
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Wed Oct 5 15:51:46 2016 +0000

    Merge branch 'item_perf_patch_store_og' into 'master_iteration_9'
    
    ITEM #PERF : fix store objectGroup handler
    
    Remove parse seda from handler
    
    See merge request !652

[33mcommit 1e6ab45b666484afb400a7062e8925a877fc51d9[m
Author: gafou <gaelle.fournier@smile.fr>
Date:   Fri Sep 30 18:46:25 2016 +0200

    ITEM #PERF : fix store objectGroup handler
    - add field "_work" in objectGroup json with the uri
    - add uri in "_work" field by object
    - user objectGroup in objectGroup handler

[33mcommit d4480613493fefb010ddbc875b49a2e72375d784[m
Merge: 9f2ae1e f9fe312
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Wed Oct 5 14:02:09 2016 +0000

    Merge branch 'item_runITs' into 'master_iteration_9'
    
    Fix Intergation tests
    
    
    
    See merge request !666

[33mcommit f9fe312fe05d71f475195a559e513fc4e238838f[m
Author: Yousri KOUKI <yousri.kouki.ext@culture.gouv.fr>
Date:   Wed Oct 5 15:28:15 2016 +0200

    update README

[33mcommit 25b1ff58d057ab1aa24722317ff69740a69bf140[m
Author: Yousri KOUKI <yousri.kouki.ext@culture.gouv.fr>
Date:   Wed Oct 5 15:13:04 2016 +0200

    fix integration tests

[33mcommit 9f2ae1e5ef3e8281f3f5eb19f4f5f3f379204273[m
Merge: 47049fe a06e8b3
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Tue Oct 4 16:42:22 2016 +0000

    Merge branch 'integration_iteration_9' into 'master_iteration_9'
    
    Fixed documentation : added modules documentation into root documentation
    
    
    
    See merge request !664

[33mcommit 47049feb9fa08934294884f71719f7d10c632898[m
Merge: 14183fc 3b4d0d1
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Tue Oct 4 16:36:21 2016 +0000

    Merge branch 'item_runITs' into 'master_iteration_9'
    
    run integration tests
    
    
    
    See merge request !662

[33mcommit a06e8b32cc29dcfbf8772157a47afcd1f6f90118[m
Author: Kristopher Waltzer <kristopher.waltzer@thalesgroup.com>
Date:   Tue Oct 4 16:57:25 2016 +0200

    Fixed documentation : added modules documentation into root
    documentation.
    Fixed some bugs in local deployment VM

[33mcommit 3b4d0d14f4e823be2a9c532575b274fcd24fde8c[m
Author: Yousri KOUKI <yousri.kouki.ext@culture.gouv.fr>
Date:   Tue Oct 4 16:53:58 2016 +0200

    run integration tests

[33mcommit 14183fc28cef0a9e2c720bad581e241ea0c4db31[m
Merge: 269c54d 4c7a8cb
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Tue Oct 4 14:14:40 2016 +0000

    Merge branch 'item_808' into 'master_iteration_9'
    
    Item_808 Add Check Object-Unit Consistency Action Handler
    
    
    
    See merge request !659

[33mcommit 4c7a8cb4748a5c1200a901ed0371cbf1b16b3fde[m
Author: NGO BA TUAN <ba-tuan.ngo.ext@culture.gouv.fr>
Date:   Tue Oct 4 12:44:34 2016 +0200

    Item_808 Add Check Object-Unit Consistency Action Handler

[33mcommit 269c54dde4cd6c8b7bdc29b17a66737671a7617d[m
Merge: 343f10b c7b4ebb
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Tue Oct 4 12:43:10 2016 +0000

    Merge branch 'item_perf_metadata' into 'master_iteration_9'
    
    fix addUnit
    
    
    
    See merge request !660

[33mcommit c7b4ebb39cb33376d7b9e1cb229f81d95c385005[m
Author: Yousri KOUKI <yousri.kouki.ext@culture.gouv.fr>
Date:   Tue Oct 4 12:11:04 2016 +0200

    fix addUnit

[33mcommit 343f10baea27ac2f068348429a0e4e78df3b4f39[m
Merge: 290d84b e6d919d
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Mon Oct 3 14:58:48 2016 +0000

    Merge branch 'item_1038' into 'master_iteration_9'
    
    TASK #1038 : Diff on unit update
    
    - generate diff
    - add diff information in metadata unit update response
    - add diff on logbook lifecycle (AccessModule)
    - fix duplicate dependecies (shiro-core, shiro-web) on parent pom.xml
    - fix antbuild target on ingest-external when build with option maven.skip.test
    
    See merge request !656

[33mcommit e6d919de2aee2815a70eb5f8ae41b5037d5acff6[m
Author: Cedric Legrand <cedric.legrand.ext@culture.gouv.fr>
Date:   Wed Sep 28 14:28:08 2016 +0200

    TASK #1038 : Diff on unit update
    
    - generate diff
    - add diff information in metadata unit update response
    - add diff on logbook lifecycle (AccessModule)
    - fix duplicate dependecies (shiro-core, shiro-web) on parent pom.xml
    - fix antbuild target on ingest-external when build with option maven.skip.test

[33mcommit 290d84b501cd715af3f4c9b83739845c4fd3a17a[m
Merge: 320e798 dbce125
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Mon Oct 3 13:03:16 2016 +0000

    Merge branch 'item_878_format_identifier' into 'master_iteration_9'
    
    ITEM #878 - Common Part : Format Identification
    
    - Configuration of format identifier services
    - Factory for format identifiers services instanciation
    - Add Siegfried Implementation + Mock
    - Documentation
    
    IMPORTANT : this is only the common part to be used by workers (FormatChecking + Test SIP Entry)
    
    See merge request !646

[33mcommit dbce125b58de1e87343f05d40a6f8bf4dcc8a342[m
Author: gafou <gaelle.fournier@smile.fr>
Date:   Tue Sep 27 17:29:56 2016 +0200

    ITEM #878 - Common Part : Format Identification
    - Configuration of format identifier services
    - Factory for format identifiers services instanciation
    - Add Siegfried Implementation + Mock
    - Documentation
    - Base 64 With Padding Added in BaseXX

[33mcommit 320e798ca0ff70ebc9a2463bd4d17c32b81f4935[m
Merge: 8f8869e 84ae387
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Mon Oct 3 10:40:45 2016 +0000

    Merge branch 'item_1102' into 'master_iteration_9'
    
    fix findFileFromWorkspace and update maxResults for listContainerOptions
    
    
    
    See merge request !654

[33mcommit 84ae387710eb645259d979b958746f2ab869491d[m
Author: Yousri KOUKI <yousri.kouki.ext@culture.gouv.fr>
Date:   Mon Oct 3 12:10:25 2016 +0200

    fix findFileFromWorkspace and update maxResults for listContainerOptions

[33mcommit 8f8869e62520ff709937e0561990f8e6f73e4f9f[m
Author: Kristopher Waltzer <kristopher.waltzer.ext@culture.gouv.fr>
Date:   Mon Oct 3 12:06:14 2016 +0200

    Added new folder rights for /vitam

[33mcommit d04be16c02b7a397bce16aa6b0181e30c0146e6f[m
Merge: ce119eb b8a581d
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Sun Oct 2 09:38:55 2016 +0000

    Merge branch 'item_933_status_implementation' into 'master_iteration_9'
    
    Item_933_934 : Include Status Services in all Modules
    
     Include Status Services in all Modules
    
    See merge request !650

[33mcommit b8a581dd0d708fdc59311f296a9f0c81b254fdcc[m
Author: Haykel <haykel.benmassaoud.ext@culture.gouv.fr>
Date:   Fri Sep 30 10:19:52 2016 +0200

    Item_933_934 : Include Status Services in all Modules

[33mcommit ce119eb26f7c9f915dd41120ebd5b552d9a6ff2d[m
Merge: 76ad38c 31b0fa5
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Fri Sep 30 15:49:54 2016 +0000

    Merge branch 'bug_local_deploy' into 'master_iteration_9'
    
    Fixes for local deployment environment
    
    - Fixed user detection if sudoers
    - Fixed build script : added siegfried build
    - Fixed doc
    
    See merge request !649

[33mcommit 31b0fa5e280a1ce0ff871350f0c27c206f15bec8[m
Author: Kristopher Waltzer <kristopher.waltzer.ext@culture.gouv.fr>
Date:   Fri Sep 30 17:07:10 2016 +0200

    Fixes for local deployment environment :
    - Fixed user detection if sudoers
    - Fixed build script : added siegfried build
    - Fixed doc

[33mcommit 76ad38ca148f315bbd1aafca07c7b111c6f68807[m
Merge: 982a4b9 c2467b0
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Fri Sep 30 14:37:37 2016 +0000

    Merge branch 'item_1102' into 'master_iteration_9'
    
    item_1102 Refactor processing
    
    
    
    See merge request !632

[33mcommit c2467b0f33bbb415b8470bf5274f95e132de0abd[m
Author: NGO BA TUAN <ba-tuan.ngo.ext@culture.gouv.fr>
Date:   Fri Sep 30 09:56:13 2016 +0200

    Item_1102 Refactor processing with input and output

[33mcommit 982a4b9f7a8f1d83a7a7a659c5a1811a63579150[m
Merge: 3d44622 d58ffc9
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Fri Sep 30 12:48:33 2016 +0000

    Merge branch 'item_fix_license' into 'master_iteration_9'
    
    Fix Licenses
    
    Fix Comment on external source work - X509 from Paul Merlin -
    Fix Comment with correct License
    
    See merge request !647

[33mcommit d58ffc9d39b4bd5fa86ad72b9810a6b4d3a678b8[m
Author: Frederic Bregier <frederic.bregier@culture.gouv.fr>
Date:   Fri Sep 30 12:05:38 2016 +0200

    Fix Licenses
    
    Fix Comment on external source work - X509 from Paul Merlin -
    Fix Comment with correct License

[33mcommit 3d446221319a28f377dbbfcd51aa3216e14d541f[m
Merge: 44b6175 9082326
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Fri Sep 30 11:58:00 2016 +0000

    Merge branch 'item_75_notification' into 'master_iteration_9'
    
    Item_75 : Notification d'entrée
    
    1. Extension modèle SEDA  vitam
    
    See merge request !621

[33mcommit 9082326ecd5615b10074e0c967dde5715632453e[m
Author: Hoan Vu <hoan.vu.ext@culture.gouv.fr>
Date:   Fri Sep 23 09:51:08 2016 +0200

    Extension modèle SEDA

[33mcommit 44b6175a28915d43a45bb36cae75fe605b705eb7[m
Merge: a245ce8 1c09d2d
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Thu Sep 29 16:24:38 2016 +0000

    Merge branch 'item_1076' into 'master_iteration_9'
    
    Added siegfried packaging
    
    
    
    See merge request !645

[33mcommit a245ce8a219c6bbebcb8aeb9c4068ac52321921c[m
Merge: 95e52d0 746e8e6
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Thu Sep 29 16:23:42 2016 +0000

    Merge branch 'integration_iteration_9' into 'master_iteration_9'
    
    Integration-related fixes & improvement
    
    - removed docker-based ansible playbooks & inventories ;
    - fixed root user usage in development container ; added dev-rpm-base
      Dockerfile & build files in vitam ;
    - Changed systemd services names (now in vitam-*) ;
    - Fixed documentation (& library-server) build ;
    - Bumped consul version to 0.7.0 ;
    - Added default shell for vitam-user-vitam & vitam-user-vitamdb ; fixed
      wrong group for vitam-user-vitamdb ; added vitam-admin & vitam-admindb groups ;
    - Added sudoers configuration for vitam-admin & vitam-admindb groups ;
    - Added first half of PKI.
    
    See merge request !644

[33mcommit 1c09d2d70ab586714ee1cb39c032ad82c78a79f5[m
Author: Kristopher Waltzer <kristopher.waltzer.ext@culture.gouv.fr>
Date:   Wed Sep 28 14:26:46 2016 +0200

    Added siegfried packaging

[33mcommit 746e8e61fe392e89c14d5a02d44a2c5b53ff6475[m
Author: Kristopher Waltzer <kristopher.waltzer.ext@culture.gouv.fr>
Date:   Thu Sep 29 16:56:09 2016 +0200

    Integration fixes & dev :
    - removed docker-based ansible playbooks & inventories ;
    - fixed root user usage in development container ; added dev-rpm-base
      Dockerfile & build files in vitam ;
    - Changed systemd services names (now in vitam-*) ;
    - Fixed documentation (& library-server) build ;
    - Bumped consul version to 0.7.0 ;
    - Added default shell for vitam-user-vitam & vitam-user-vitamdb ; fixed
      wrong group for vitam-user-vitamdb ; added vitam-admin & vitam-admindb groups ;
    - Added sudoers configuration for vitam-admin & vitam-admindb groups ;
    - Added first half of PKI.

[33mcommit 95e52d03e9a4e934d2e14d65fc70421c183f3f2a[m
Merge: 11a8e42 5be39da
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Thu Sep 29 14:55:31 2016 +0000

    Merge branch 'item_999' into 'master_iteration_9'
    
    item_999 search by unit Id and fix bug date
    
    
    
    See merge request !634

[33mcommit 11a8e42bbbbce53db647254b804573541c7af42a[m
Merge: a7492cb 1616263
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Thu Sep 29 14:53:15 2016 +0000

    Merge branch 'bug_900_v2' into 'master_iteration_9'
    
    BUG #900 : Bug Fixes (Storage and general)
    
    General bug fixes:
    - Remove duplicate dependencies (shiro-core and shiro-web) on parent pom.xml
    - ingest-external : modify pom.xml ant plugin target, do not run if maven.test.skip
    
    Storage bug fixes:
    - Fixing Object Request/Result
    - Fix FakeDriver
    - Rename createContainer method to initCreateObject and review javadoc
    - Review usedSpace calculation (FileSystem/workspace) for getCapacity storage method
    - Add javadoc detail (cannot remove non-empty container) on StorageClient.deleteContainer method
    - Add lower camel case ObjectMapper on JsonHandler for storage strategy files (without upper camel case unlike SEDA files)
    - Add specifics methods for lower camel case json on JsonHandler and use them on storage strategy file load
    - Update documentation + tests
    
    See merge request !636

[33mcommit 5be39dad2bc54124f88e9fbd40223dc6557b7f67[m
Author: Ramzi LAZREG <ramzy.lazreg.ext@culture.gouv.fr>
Date:   Tue Sep 27 15:00:42 2016 +0200

    item_999 search by unit Id and fix bug date

[33mcommit 16162630b18c194792d4e6af2074ebdbb65d384a[m
Author: lubla <lubla@smile.fr>
Date:   Mon Sep 19 14:19:13 2016 +0200

    BUG #900 :
    
    General bug fixes:
    - Remove duplicate dependencies (shiro-core and shiro-web) on parent pom.xml
    - ingest-external : modify pom.xml ant plugin target, do not run if maven.test.skip
    
    Storage bug fixes:
    - Fixing Object Request/Result
    - Fix FakeDriver
    - Rename createContainer method to initCreateObject and review javadoc
    - Review usedSpace calculation (FileSystem/workspace) for getCapacity storage method
    - Add javadoc detail (cannot remove non-empty container) on StorageClient.deleteContainer method
    - Add lower camel case ObjectMapper on JsonHandler for storage strategy files (without upper camel case unlike SEDA files)
    - Add specifics methods for lower camel case json on JsonHandler and use them on storage strategy file load
    - Update documentation + tests

[33mcommit a7492cb10655ed9c1bed14c258d38af8b08208c2[m
Merge: 4bd9e9a 1a36cb3
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Thu Sep 29 07:39:55 2016 +0000

    Merge branch 'item_perf_workspace_7000' into 'master_iteration_9'
    
    Clean Exception management in workspace
    
    
    
    See merge request !641

[33mcommit 4bd9e9a11134d350cf5622d82cbf5da5b74de966[m
Merge: 8343603 757c05b
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Thu Sep 29 07:37:54 2016 +0000

    Merge branch 'bug_905' into 'master_iteration_9'
    
    Fix bug #905
    
    - Check existence of object before add/replace it in offer
    - Add mock driver to sources
    - Update unit tests
    
    See merge request !627

[33mcommit 1a36cb3df16c4f6ce2a1f0d8905fd7653926a2c6[m
Author: Etienne CARRIERE <etienne.carriere@culture.gouv.fr>
Date:   Thu Sep 29 08:32:20 2016 +0200

    Improve Exception management
    Clean javadoc error
    Delete double declaration of shiro dependencies

[33mcommit 8343603a436627089dac2b016b8fbf531b2613cb[m
Merge: 8625183 c12e3fd
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Wed Sep 28 17:10:01 2016 +0000

    Merge branch 'item_933_status_implementation' into 'master_iteration_9'
    
    Item_933_934 : Implement Generic Status Service Treatment
    
    
    
    See merge request !637

[33mcommit 757c05b7015eaa1ea9c7681b3e382957879589cf[m
Author: lubla <lubla@smile.fr>
Date:   Thu Sep 22 17:44:18 2016 +0200

    Check existance of object before add/replace it in offer
    Add mock driver to sources

[33mcommit c12e3fd619bb83ca9ea0b5d6737f5d97938eaccf[m
Author: Haykel <haykel.benmassaoud.ext@culture.gouv.fr>
Date:   Tue Sep 27 17:52:30 2016 +0200

    Item_933_934 : Implement Generic Status Service Treatment : Module Access

[33mcommit 8625183f83b7824ac62195d8f39422235547f012[m
Merge: d1e0450 6cd9f30
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Wed Sep 28 12:50:38 2016 +0000

    Merge branch 'bug_864' into 'master_iteration_9'
    
    BUG #864 : Tree Structure in Offer
    
    *  Tree structure in offer (example : idTenant/object/guid)
    *  update driver
    * update client storage engine
    
    
    See merge request !635

[33mcommit 6cd9f30c67b05d88f2c39ac04f638edc2919c5d6[m
Author: gafou <gaelle.fournier@smile.fr>
Date:   Fri Sep 23 15:49:43 2016 +0200

    BUG #864 : Tree Structure in Offer

[33mcommit d1e045010272c9b7e4faf37e2b3a325392a805f8[m
Merge: 0cf0339 faa4069
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Tue Sep 27 16:27:08 2016 +0000

    Merge branch 'item_869' into 'master_iteration_9'
    
    Item #869 - Logbook Operation Simplified
    
    - Fields removed when we deal with an update
    - Changed the actual enum to add the new field
    - Tests added to check if the fields are present or not
    
    BugFix :
    - Rest tests in logbook were not working on local (mvn build), it has been fixed
    
    See merge request !630

[33mcommit 0cf0339db2c58cc58aaf0d828181f98dc1525204[m
Merge: d234953 79f42aa
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Tue Sep 27 14:24:50 2016 +0000

    Merge branch 'bug_1131_smile' into 'master_iteration_9'
    
    BUG #1131 : download object firefox
    
    Fix a bug on dowload object
    
    See merge request !633

[33mcommit faa40690b618c56d37361a0592aee82c4e395698[m
Author: germain ledroit <germain.ledroit.ext@culture.gouv.fr>
Date:   Thu Sep 22 19:05:20 2016 +0200

    Item #869 - Logbook Operation Simplified
    - Fields removed when we deal with an update
    - Changed the actual enum to add the new field
    - Tests added to check if the fields are present or not
    
    BugFix :
    - Rest tests in logbook were not working on local (mvn build), it has been fixed

[33mcommit d234953402326c9ce2429052491287b182cb8a74[m
Merge: 5726e38 219af92
Author: Olivier Marsol <olivier.marsol.ext@culture.gouv.fr>
Date:   Tue Sep 27 09:47:41 2016 +0000

    Merge branch 'item_fix_rest_doc' into 'master_iteration_9'
    
    Fix RAML to 1.0
    
    Additional tasks to do:
    - change raml2html to develop version from github: https://github.com/raml2html/raml2html/tree/develop
    - add the following directory into a web server:
      - console (including symlink to externe and interne-1.0) including an index.html (which can be improved)
      - externe
      - interne-1.0
    - DO NOT compile interne but only externe and interne-1.0
    
    See merge request !620

[33mcommit 79f42aac0d2d007f47d44fb75b57f132e913ed0d[m
Author: gafou <gaelle.fournier@smile.fr>
Date:   Tue Sep 27 11:46:50 2016 +0200

    BUG #1131 : download object firefox

[33mcommit 219af927d6815264098f3336733598ebe027399b[m
Author: Olivier MARSOL <olivier.marsol.ext@culture.gouv.fr>
Date:   Tue Sep 27 11:44:40 2016 +0200

    fix by Frédéric BREGIER for correct raml2html ; adapted Makefile according to comments

[33mcommit 5726e38bc9c80c3473205322df092d40bdefc206[m
Merge: feed597 d45d04e
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Mon Sep 26 16:13:57 2016 +0000

    Merge branch 'item_868' into 'master_iteration_9'
    
    story_868 & 870 : Simplify LifeCycle model
    
    
    
    See merge request !625

[33mcommit d45d04eb5b1e94153f4e26099eb45e147f921dfe[m
Author: Hela Amri <hela.amri.ext@culture.gouv.fr>
Date:   Mon Sep 26 17:00:35 2016 +0200

    story_868 & 870 : Simplify Logbook LifeCycle Model

[33mcommit feed597940236fea63a9920a9dc1b4ca9b6fdc71[m
Merge: 332dc72 d43523a
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Mon Sep 26 09:39:44 2016 +0000

    Merge branch 'item_871' into 'master_iteration_9'
    
    item_871
    
    
    
    See merge request !624

[33mcommit 332dc72332594eb782327f5b62b1b728a02f2e1b[m
Merge: 28bb619 f3cc5e5
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Mon Sep 26 09:14:14 2016 +0000

    Merge branch 'integration_iteration_9' into 'master_iteration_9'
    
    added tools for local deployment
    
    Tested on my local machine ; it works : I can look at logbook operations; inject a SIP.
    
    See merge request !609

[33mcommit f3cc5e5347958a63f22bba18d1c5d125953f1e6a[m
Author: Olivier MARSOL <olivier.marsol.ext@culture.gouv.fr>
Date:   Mon Sep 26 11:12:57 2016 +0200

    correct gc log dir for elasticsearch ; should be OK for local deployment

[33mcommit d43523aa69d8d32ae6af78c472d779540fda95cd[m
Author: CHEN Zhang <zchen@linagora.com>
Date:   Fri Sep 23 17:30:40 2016 +0200

    item_871

[33mcommit 28bb619b9a8aac5376e1b7dd825d33e9ed08f062[m
Merge: 877f032 71589f5
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Fri Sep 23 16:48:03 2016 +0000

    Merge branch 'cherry-pick-05c916be' into 'master_iteration_9'
    
    as of release_iteration_8
    
    added inventory for recette iteration 8
    
    
    
    See merge request !610
    
    See merge request !611

[33mcommit 877f0327e87f443715e1a05c2297c9738d8dda77[m
Merge: 7216594 bc06a94
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Fri Sep 23 13:29:57 2016 +0000

    Merge branch 'item_fix_archi' into 'master_iteration_9'
    
    Various fixes
    
    - correct Logger level (WARN & INFO to DEBUG) and remove String.format when not necessary (Logger use {} notation instead of %s)
    - remove deprecated from GUID factory (but not existing calls to "generic" GUID constructor)
    - Fix and create VitamConfiguration (move global configuration element in one place)
    - Fix and create SysErrLogger for exceptional logging without VitamLogger (to prevent SONAR issues but allow to log to System.err)
    - Fix constructor of GUID preventing integer overflow
    - small fixes seen in Sonar
    
    See merge request !618

[33mcommit bc06a945850521a8c022818545c013d954a120d2[m
Author: Frederic Bregier <frederic.bregier@culture.gouv.fr>
Date:   Wed Sep 21 18:39:54 2016 +0200

    Various fixes
    
    - correct Logger level (WARN & INFO to DEBUG)
    - remove deprecated from GUID factory (but not existing calls to "generic" GUID constructor)
    - Fix and create VitamConfiguration (move global configuration element in one place)
    - Fix and create SysErrLogger for exceptional logging without VitamLogger (to prevent SONAR issues but allow to log to System.err)
    - Fix constructor of GUID preventing integer overflow
    - small fixes seen in Sonar

[33mcommit 721659414131d87ce5c58098f9c947d629b920ea[m
Merge: bc5d6ea 2784fc9
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Fri Sep 23 11:06:58 2016 +0000

    Merge branch 'item_673' into 'master_iteration_9'
    
    stop ingest workflow (when a virus is found and can be corrected)
    
    https://dev.programmevitam.fr/plugins/tracker/?aid=673
    
    See merge request !612

[33mcommit bc5d6ea02fefaf8e686058ed57a5e63b90f97688[m
Merge: 099cdb0 76771bc
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Fri Sep 23 11:06:39 2016 +0000

    Merge branch 'item_714' into 'master_iteration_9'
    
    separate UT and IT, execute UT in parallel
    
    
    
    See merge request !617

[33mcommit 099cdb0557ccce990b396f0515d757468219e090[m
Merge: e7202e2 fa37beb
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Fri Sep 23 10:40:23 2016 +0000

    Merge branch 'item_access_log' into 'master_iteration_9'
    
    Correction for accesslog
    
    
    
    See merge request !619

[33mcommit fa37bebc34db6934282e9d0edf5f6cd20d76acdb[m
Author: Etienne CARRIERE <etienne.carriere@culture.gouv.fr>
Date:   Fri Sep 23 11:21:39 2016 +0200

    Correction for accesslog

[33mcommit 76771bcf9cc18dff00921f6949a9acf93b33f5cc[m
Author: Yousri KOUKI <yousri.kouki.ext@culture.gouv.fr>
Date:   Thu Sep 22 16:31:15 2016 +0200

    separate UT and IT, execute UT in parallel

[33mcommit 2784fc992c44d96079df4b5c9d7e8ee6a2a064ef[m
Author: Yousri KOUKI <yousri.kouki.ext@culture.gouv.fr>
Date:   Thu Sep 22 17:37:05 2016 +0200

    stop ingest workflow (when a virus is found and can be corrected)

[33mcommit e7202e296b45c7ffdfcb835f6a7510a8b68c9e04[m
Merge: 6316aea beb4bd8
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Thu Sep 22 14:24:48 2016 +0000

    Merge branch 'bugfix_1126_worker_no_ssl' into 'master_iteration_9'
    
    BugFix #1126 - No SSL Configuration
    
    During Iteration #8, worker was using a ssl client. The bug #1126 was created in order to fix this in iteration #9
    - Now 2 abstract clients : AbstractSSLClient and AbstractClient
    - Use non SSL Client in worker
    
    See merge request !616

[33mcommit beb4bd809623c4aa487dbffa8483fc63e391a935[m
Author: germain ledroit <germain.ledroit.ext@culture.gouv.fr>
Date:   Mon Sep 19 11:47:06 2016 +0200

    BugFix #1126 - No SSL Configuration
    During Iteration #8, worker was using a ssl client. The bug #1126 was created in order to fix this in iteration #9
    - Now 2 abstract clients : AbstractSSLClient and AbstractClient
    - Use non SSL Client in worker

[33mcommit 6316aead1ad446d2032c5286ae4546f0ebc20629[m
Author: Kristopher Waltzer <kristopher.waltzer.ext@culture.gouv>
Date:   Thu Sep 22 09:47:49 2016 +0000

    Delete index.html

[33mcommit 71589f500c3c20e948f3c1e97f5696f2e624f16e[m
Author: Olivier Marsol <olivier.marsol.ext@culture.gouv.fr>
Date:   Wed Sep 21 12:43:53 2016 +0000

    Merge branch 'release_integration_8' into 'release_iteration_8'
    
    added inventory for recette iteration 8
    
    
    
    See merge request !610

[33mcommit 7fcd530d8e4120da3ecb66675ff4adca8e56b35d[m
Merge: 842d7c0 e9fd9d3
Author: Kristopher Waltzer <kristopher.waltzer.ext@culture.gouv>
Date:   Wed Sep 21 10:07:23 2016 +0000

    Merge branch 'item_1000' into 'master_iteration_9'
    
    Git structure change
    
    Moved all vitam sources into a 'sources' folder
    
    See merge request !608

[33mcommit e9fd9d38b1a201c5a1c6b7278ca12cdebd00a775[m
Author: Kristopher Waltzer <kristopher.waltzer.ext@culture.gouv.fr>
Date:   Wed Sep 21 10:36:15 2016 +0200

    Moved all vitam sources into a 'sources' folder

[33mcommit 842d7c04e576b79983ef39ead3ef97d29e4f8393[m
Author: jgonthier <joachim.gonthier.ext@culture.gouv.fr>
Date:   Tue Sep 20 17:32:44 2016 +0200

    [manual update] prepare for next development iteration

[33mcommit b6f62024dce24539e9a2bf21aa1b4387f7a63b79[m
Merge: db53359 26b1bc5
Author: Joachim Gonthier <joachim.gonthier.ext@culture.gouv>
Date:   Tue Sep 20 15:18:51 2016 +0000

    Merge branch 'master_iteration_8' into 'master'
    
    Merge branch 'master_iteration_8' into 'master'
    
    Merge branch 'master_iteration_8' into 'master'
    
    See merge request !606

[33mcommit 26b1bc5052398f39a35afa917195f73c9f2a3ace[m
Merge: 589f031 c458647
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Tue Sep 20 13:26:10 2016 +0000

    Merge branch 'integration_iteration_8' into 'master_iteration_8'
    
    fixes after demonstration
    
    * Documentation fixes
    * Deployment scripts fixes
    * Local deployment fixes
    
    See merge request !605

[33mcommit c458647b6e4ef8731198254d01fc0ea7122022b2[m
Author: Kristopher Waltzer <kristopher.waltzer.ext@culture.gouv.fr>
Date:   Tue Sep 20 14:10:46 2016 +0200

    * Documentation fixes
    * Deployment scripts fixes
    * Local deployment fixes

[33mcommit 589f0315e34319fe40434ab5ca53e4d11804c8a3[m
Merge: a6a071a cc6020c
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Mon Sep 19 18:49:17 2016 +0000

    Merge branch 'item_509' into 'master_iteration_8'
    
    story_490 (corrections after functional tests)
    
    
    
    See merge request !604

[33mcommit a6a071ac3f26e3000611a502d0a0ecfc4151d6bb[m
Merge: 45ec438 1c76753
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Mon Sep 19 18:44:53 2016 +0000

    Merge branch 'item_786_v2_logbook' into 'master_iteration_8'
    
    US786 : logBook config xml jetty
    
    
    
    See merge request !578

[33mcommit 45ec438a8b533397ba82e92058db7e1f51a32779[m
Merge: e4f60a5 93163da
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Mon Sep 19 18:42:18 2016 +0000

    Merge branch 'item_786_storage_do' into 'master_iteration_8'
    
    US 786 : Storage offers xml jetty config
    
    
    
    See merge request !595

[33mcommit cc6020cfbe85fd1c1202030113d92c0831f702cb[m
Author: Hela Amri <hela.amri.ext@culture.gouv.fr>
Date:   Mon Sep 19 20:20:25 2016 +0200

    story_490 (corrections after functional tests

[33mcommit e4f60a5fd98945c6e941eeb34fc7f718b4a6f72e[m
Merge: 0600e84 4c2e395
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Mon Sep 19 17:13:17 2016 +0000

    Merge branch 'item_509' into 'master_iteration_8'
    
    story_509 (correction in IHM after functionnal tests)
    
    
    
    See merge request !603

[33mcommit 4c2e3952e12c0674dd675d2485a4a138273f23f1[m
Author: Hela Amri <hela.amri.ext@culture.gouv.fr>
Date:   Mon Sep 19 19:09:30 2016 +0200

    story_509:functionnal tests

[33mcommit 0600e840df9ecc7f0c489e481fc2abd9858a78fe[m
Merge: 428910b 2ce2f38
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Mon Sep 19 17:03:01 2016 +0000

    Merge branch 'item_490_Regle_De_Gestion' into 'master_iteration_8'
    
    IHM Update : Fix Details Issue
    
    
    
    See merge request !602

[33mcommit 2ce2f38edb4b75b5f389fc8675c6727a330ef63e[m
Author: Haykel <haykel.benmassaoud.ext@culture.gouv.fr>
Date:   Mon Sep 19 18:23:02 2016 +0200

    IHM Update : Fix Details Issue

[33mcommit 93163daa93c72aae643c2b4b564ba98d537c6082[m
Author: Ramzi LAZREG <ramzy.lazreg.ext@culture.gouv.fr>
Date:   Mon Sep 19 14:01:21 2016 +0200

    US 786 : Storage offers xml jetty config

[33mcommit 428910be4c0c6f97d844e1b2ad05ab14616a9da8[m
Merge: 15eb38a 8461441
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Mon Sep 19 15:01:16 2016 +0000

    Merge branch 'item_490_Regle_De_Gestion' into 'master_iteration_8'
    
    IHM Update : Conflicts Fixes
    
    
    
    See merge request !601

[33mcommit 15eb38a705753805e854998f0da79a83d889be68[m
Merge: 8c87676 4edc40d
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Mon Sep 19 14:39:11 2016 +0000

    Merge branch 'item_786_storage' into 'master_iteration_8'
    
    US 786 : storage engine xml jetty config
    
    
    
    See merge request !598

[33mcommit 84614411f44ad02d313b407912e48f392976903d[m
Author: Haykel <haykel.benmassaoud.ext@culture.gouv.fr>
Date:   Mon Sep 19 16:34:55 2016 +0200

    IHM Update : Conflicts Fixes

[33mcommit 8c87676f08d573cc18bb8eec19c0d444a9237609[m
Merge: 570e856 1d53a8c
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Mon Sep 19 14:07:55 2016 +0000

    Merge branch 'item_490_Regle_De_Gestion' into 'master_iteration_8'
    
    IHM Update : Fix DSL Details Search
    
    Fix DSL Details Search Multiple Choice
    
    See merge request !599

[33mcommit 4edc40d9284d50cab3007c3f2c99998dba1e16e4[m
Author: Ramzi LAZREG <ramzy.lazreg.ext@culture.gouv.fr>
Date:   Mon Sep 19 15:12:38 2016 +0200

    US 786 : storage engine xml jetty config

[33mcommit 1c7675332e6510e081d0d4a122941f10cff284dd[m
Author: Ramzi LAZREG <ramzy.lazreg.ext@culture.gouv.fr>
Date:   Mon Sep 19 15:49:59 2016 +0200

    US786 : logBook xml jetty config

[33mcommit 570e856395a9550ec0e700466a9eff6797e44e85[m
Merge: 886c2f6 673b2a4
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Mon Sep 19 13:40:04 2016 +0000

    Merge branch 'integration_iteration_8' into 'master_iteration_8'
    
    Integration-related tasks : deployment (rpm & local)
    
    - added worker module
    - upgraded deployment scripts (notably with vault)
    - added access logs
    - added gc logs & RollingFileAppender configuration for logback ;
    - added vitam-external rpm repository build ;
    - added new working draft of DAT / DEX ;
    
    See merge request !597

[33mcommit 1d53a8c2ab11de3f20ff49d8931a28e99a5e48be[m
Author: Haykel <haykel.benmassaoud.ext@culture.gouv.fr>
Date:   Mon Sep 19 15:37:57 2016 +0200

    IHM Update : Fix DSL Details Search

[33mcommit 673b2a45e5c748e858aa58460c3aa658ca2ffbf2[m
Author: Kristopher Waltzer <kristopher.waltzer.ext@culture.gouv.fr>
Date:   Mon Sep 19 14:33:07 2016 +0200

    Integration-related :
    - added worker module
    - upgraded deployment scripts (notably with vault)
    - added access logs
    - added gc logs & RollingFileAppender configuration for logback ;
    - added vitam-external rpm repository build ;
    - added new working draft of DAT / DEX ;

[33mcommit 886c2f6fbb9e84e82c97950d5ded4ea2f81b7a68[m
Merge: 63ad5aa 09c0d31
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Mon Sep 19 12:44:06 2016 +0000

    Merge branch 'bug_919_unzip' into 'master_iteration_8'
    
    bug 919
    
    
    
    See merge request !576

[33mcommit 63ad5aa10121f3700160858614e4cda497bac393[m
Merge: 67f017b 790f196
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Mon Sep 19 12:39:56 2016 +0000

    Merge branch 'item_fix_schema_seda' into 'master_iteration_8'
    
    Update SEDA schema with a relaxed one replacing Abstract with xsd:any
    
    SEDA XSD schema contains natively some Abstract components.
    
    In order to be a full valid schema, it is mandatory to replaced them with the necessary extensions.
    
    Those new XSD replace the Abstract with xsd:any in order to be able to check any SEDA compliant xml file using valid extensions.
    
    See merge request !594

[33mcommit 790f196538eb4ae13bab70e646c13a55fe4754d5[m
Author: Frederic Bregier <frederic.bregier@culture.gouv.fr>
Date:   Mon Sep 19 13:37:21 2016 +0200

    Update SEDA schema with a relaxed one replacing Abstract with xsd:any

[33mcommit 09c0d314523582cdee025bcb2e98460e99d316b6[m
Author: Ramzi LAZREG <ramzy.lazreg.ext@culture.gouv.fr>
Date:   Mon Sep 19 10:45:27 2016 +0200

    bug 919 : unzip in worksapce

[33mcommit 67f017b005061c36ec50b0a5d0c5807962ee6d25[m
Merge: d5aca9b 7bc9b1e
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Mon Sep 19 11:06:08 2016 +0000

    Merge branch 'item_490_Regle_De_Gestion' into 'master_iteration_8'
    
    IHM Update : Fix View Format of Search Result
    
     Fix View Format of Search Result
    
    See merge request !593

[33mcommit d5aca9b6255c3abff3678ec4a1d068fe33b5cdb0[m
Merge: 5dfbd29 20c3d05
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Mon Sep 19 11:05:12 2016 +0000

    Merge branch 'item_910' into 'master_iteration_8'
    
    Item #910 - Processing-Worker Separation
    
    1/ Worker Client
    - Modularization
    - Worker Client REST/Mock, Tests and Documentation + Fix storage Header (remove hardcoded value in order to use GlobalDataRest)
    
    2/ Worker Server
    - Application + Configuration
    - Resource
    - Documentations
    - Tests
    - VitamError + RequestResponse in the common-private
    
    3/ Transfer of temporary maps to workspace
    - Save temporary maps of extract SEDA in the workspace instead of tmp local fodler :
    - Replace tmp maps by json files
    - Some bugfix
    
    4/ Processing Rest
    - Fixed processing uri -> processing/v1
    - Rest services for worker_family operation (register and unregister)
    - WorkerBean + WorkerConfiguration for the worker registering
    - Fixed Worker-server for docker (conf)
    - Add a resource in ProcessManagementApplication
    - Documentation updated
    
    5/ Processing Client
    - Modification of Exceptions
    - Modification of ProcessingManagementClient to add register / unregister methods
    - Add some test coverage for the worker
    
    6/ Externalize handlers + worker class from processing to worker modules
    - Move handlers from processing to worker
    - Move Utils from processing to worker
    - Register worker to processing
    - Worker integration test
    
    7/ Processing update
    - Remove processing-worker module
    - Update ProcessingIntegrationTest
    - Move ProcessingIntegrationTest to processing-integration-test
    
    See merge request !585

[33mcommit 20c3d05eba80bd1291d9362d819d6e6c037fef10[m
Author: germain ledroit <germain.ledroit.ext@culture.gouv.fr>
Date:   Mon Sep 5 17:15:10 2016 +0200

    Item #910 - Processing-Worker Separation
    1/ Worker Client
    - Modularization
    - Worker Client REST/Mock, Tests and Documentation + Fix storage Header (remove hardcoded value in order to use GlobalDataRest)
    
    2/ Worker Server
    - Application + Configuration
    - Resource
    - Documentations
    - Tests
    - VitamError + RequestResponse in the common-private
    
    3/ Transfer of temporary maps to workspace
    - Save temporary maps of extract SEDA in the workspace instead of tmp local fodler :
    - Replace tmp maps by json files
    - Some bugfix
    
    4/ Processing Rest
    - Fixed processing uri -> processing/v1
    - Rest services for worker_family operation (register and unregister)
    - WorkerBean + WorkerConfiguration for the worker registering
    - Fixed Worker-server for docker (conf)
    - Add a resource in ProcessManagementApplication
    - Documentation updated
    
    5/ Processing Client
    - Modification of Exceptions
    - Modification of ProcessingManagementClient to add register / unregister methods
    - Add some test coverage for the worker
    
    6/ Externalize handlers + worker class from processing to worker modules
    - Move handlers from processing to worker
    - Move Utils from processing to worker
    - Register worker to processing
    - Worker integration test
    
    7/ Processing update
    - Remove processing-worker module
    - Update ProcessingIntegrationTest
    - Move ProcessingIntegrationTest to processing-integration-test
    
    8/ Jetty configuration
    
    TODO :
    - SSLClient to be replaced by NoSSLClient in WorkerClientConfiguration

[33mcommit 5dfbd29f0c13090e5f6661b7557dc7a2c510875e[m
Merge: e69f3b2 4dd3c58
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Mon Sep 19 09:44:06 2016 +0000

    Merge branch 'item_509' into 'master_iteration_8'
    
    Fix bug : Tree creation
    
    La correction consiste à traiter les archives units non insérées déjà dans l'arbre et qui sont référencées par une autre archive.
    
    See merge request !591

[33mcommit 4dd3c582f2237a35cc2dfbceef42688c2c304402[m
Author: Hela Amri <hela.amri.ext@culture.gouv.fr>
Date:   Mon Sep 19 11:10:05 2016 +0200

    Fix Archive details Form (Front part)

[33mcommit 7bc9b1e7b1ae80ca199eb7d32ca2d37b7d14b9cf[m
Author: Haykel <haykel.benmassaoud.ext@culture.gouv.fr>
Date:   Mon Sep 19 11:04:12 2016 +0200

    IHM Update : Fix View Format of Search Result

[33mcommit 73846f5cbcf328e2e214105b3e9a0254ec370731[m
Author: Hela Amri <hela.amri.ext@culture.gouv.fr>
Date:   Mon Sep 19 10:48:44 2016 +0200

    Bug fix: unit tree creation

[33mcommit e69f3b2ff76d22712d42cd1c35d6549918f89024[m
Merge: c26d680 973d0fd
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Fri Sep 16 16:14:40 2016 +0000

    Merge branch 'item_490_Regle_De_Gestion' into 'master_iteration_8'
    
    Rules features Implementation:IHM And DSL Integration Tests fixes
    
    -HM And DSL Integration Tests fixes
    
    See merge request !588

[33mcommit c26d6800518c537524ef4bd909a80472e0592b13[m
Merge: e3dbf06 cbd2d1f
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Fri Sep 16 15:50:44 2016 +0000

    Merge branch 'item_874_client_auth' into 'master_iteration_8'
    
    add package-info
    
    
    
    See merge request !570

[33mcommit e3dbf062430ce4749c59932a90715061615d7669[m
Merge: 6307279 87719cb
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Fri Sep 16 14:46:50 2016 +0000

    Merge branch 'item_509' into 'master_iteration_8'
    
    Functional tests : correct Front problems
    
    
    
    See merge request !586

[33mcommit 973d0fde1b334829b36e12a264dd24fab3a904f6[m
Author: Haykel <haykel.benmassaoud.ext@culture.gouv.fr>
Date:   Fri Sep 16 15:46:17 2016 +0200

    Rules features Implementation:IHM And DSL Integration Tests fixes

[33mcommit 6307279780c2a60d4467a4b5ad179ab52aa0e8a4[m
Merge: 8d945dc 287b5b6
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Fri Sep 16 14:26:57 2016 +0000

    Merge branch 'item-jsonnode2object' into 'master_iteration_8'
    
    Add a method to JSON common to unserialize JsonNode
    
    
    
    See merge request !574

[33mcommit 8d945dcf1e011742208aad9f0123f0028068dc90[m
Merge: 54e7ebc 5ebee9d
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Fri Sep 16 14:25:12 2016 +0000

    Merge branch 'documentation_fix' into 'master_iteration_8'
    
    Documentation fix
    
    - documentation module ingest : ingest-internal & ingest-external
    - documentation module functional-administration
    
    See merge request !459

[33mcommit 54e7ebc356fefaf65197a69d059039b8788a750a[m
Merge: 588d764 d951655
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Fri Sep 16 14:03:09 2016 +0000

    Merge branch 'bug_859' into 'master_iteration_8'
    
    fix Logbook Operation Parameters (ingest-ext)
    
    https://dev.programmevitam.fr/plugins/tracker/?aid=859
    
    See merge request !584

[33mcommit 87719cbf58bd71dab6011426b7a1ecc92ecf9d95[m
Author: Hela Amri <hela.amri.ext@culture.gouv.fr>
Date:   Fri Sep 16 15:56:02 2016 +0200

    Functional Tests : correct Front problems

[33mcommit 588d764213c0d9c732bb5beb88247f44bc3da9ec[m
Merge: cb40e8e 871116a
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Fri Sep 16 13:20:27 2016 +0000

    Merge branch 'item_886_v2' into 'master_iteration_8'
    
    Item #886 - Update archive detail template
    
    Add directive for duplicate template chunk
    Update angular main documentation + Add archive unit module documentation
    Update archive unit module and controller
    
    See merge request !573

[33mcommit d95165557767a95ff145c5d143a9066b4cb35faa[m
Author: Yousri KOUKI <yousri.kouki.ext@culture.gouv.fr>
Date:   Fri Sep 16 14:48:37 2016 +0200

    fix Logbook Operation Parameters (ingest-ext)

[33mcommit cb40e8e682a29f6b429880c24aceea137f9d0113[m
Merge: 7332a78 b63bae0
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Fri Sep 16 09:08:49 2016 +0000

    Merge branch 'item_590' into 'master_iteration_8'
    
    ITEM #590 : Error code
    
    - Service enum
    - Domain enum
    - VitamCode enum (Vitam error code)
    - VitamCodeHelper
    - Apply to Storage module (REST part, StorageDistribution, StorageClientRest, DriverMapper)
    
    RAF / TODO :
    
    - See if we add VitamCode field to VitamException
    - Review VitamError object fields and VitamCode
    - Apply VitamCode to all modules
    
    See merge request !572

[33mcommit 287b5b647b209bf3c181d9dd072ba1ebcddf778c[m
Author: Etienne CARRIERE <etienne.carriere@culture.gouv.fr>
Date:   Thu Sep 15 19:11:35 2016 +0200

    Add a method to JSON common to unserialize JsonNode

[33mcommit 871116afcca6d3c29bdad582ced27cead770e6b4[m
Author: Ludovic Blanchet <ludocic.blanchet.ext@culture.gouv.fr>
Date:   Mon Sep 12 14:52:40 2016 +0200

    Update archive detail template
    Add directive for duplicate template chunk
    Update angular main documentation + Add archive unit module documentation
    Update archive unit module and controller

[33mcommit 5ebee9de556a53919e034fcc8be9b199aaa0950e[m
Author: Hoan Vu <hoan.vu.ext@culture.gouv.fr>
Date:   Thu Aug 11 17:11:49 2016 +0200

    documentation fix : module functional-administration and module ingest (ingest-internal & ingest-external)

[33mcommit b63bae0075197e65560c91b8ef625fd02cf492c4[m
Author: Cedric Legrand <cedric.legrand.ext@culture.gouv.fr>
Date:   Tue Sep 13 18:47:25 2016 +0200

    ITEM #590 : Error code
    
    - Service enum
    - Domain enum
    - VitamCode enum (Vitam error code)
    - VitamCodeHelper
    - Apply to Storage module (REST part, StorageDistribution, StorageClientRest, DriverMapper)
    
    RAF / TODO :
    
    - See if we add VitamCode field to VitamException
    - Review VitamError object fields and VitamCode
    - Apply VitamCode to all modules

[33mcommit 7332a78bf509a58fa050f887b441abfd67c2bfce[m
Merge: ba08c2c e69bc6b
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Fri Sep 16 06:17:04 2016 +0000

    Merge branch 'item_509' into 'master_iteration_8'
    
    item 509 search unit by date and title or description
    
    item 509 search unit
    
    control dates
    
    Correct dates control + Unit Test
    
    See merge request !575

[33mcommit e69bc6bfee83393d5da3ec0e07c6decd05eb5737[m
Author: Ramzi LAZREG <ramzy.lazreg.ext@culture.gouv.fr>
Date:   Tue Sep 13 15:39:43 2016 +0200

    item 509 search unit by date and title or description
    
    item 509 search unit
    
    control dates
    
    Correct dates control + Unit Test

[33mcommit ba08c2cc756fe00f4ce060751237dde2ac70f975[m
Merge: daac67a e20de0d
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Thu Sep 15 16:55:55 2016 +0000

    Merge branch 'item_490_Regle_De_Gestion' into 'master_iteration_8'
    
    Rules features Implementation:IHM & DSL
    
    - IHM Rules Management (Accepted Validated by a PO)
    - Related DSL Changes
    
    
    See merge request !569

[33mcommit daac67aef5a936d5a05d63ce0d454ac71e5169a6[m
Merge: acff2ab c749417
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Thu Sep 15 16:49:07 2016 +0000

    Merge branch 'item_885' into 'master_iteration_8'
    
    Item 885
    
    
    
    See merge request !563

[33mcommit acff2ab93aa8771efe7a080587b4c2393088845d[m
Merge: 08c8659 6a522e3
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Thu Sep 15 15:52:23 2016 +0000

    Merge branch 'item_bug_895' into 'master_iteration_8'
    
    fix bug 895
    
    
    
    See merge request !555

[33mcommit e20de0d3c6052515d87d24d106e2558a6595ca76[m
Author: Haykel <haykel.benmassaoud.ext@culture.gouv.fr>
Date:   Wed Sep 14 18:24:02 2016 +0200

    Rules features Implementation:IHM & DSL

[33mcommit 6a522e3d08e615f29051669b28eeb59e92a6285d[m
Author: CHEN Zhang <zchen@linagora.com>
Date:   Mon Aug 29 18:11:56 2016 +0200

    fix bug 895

[33mcommit c7494172f92f42d9c324233773146caa933107c3[m
Author: NGO BA TUAN <ba-tuan.ngo.ext@culture.gouv.fr>
Date:   Wed Sep 14 12:44:59 2016 +0200

    Update ihm-demo webapp with angular-shiro

[33mcommit 1793bfcd0ca2780f269b159cb3c4ba2ef05ca291[m
Author: NGO BA TUAN <ba-tuan.ngo.ext@culture.gouv.fr>
Date:   Tue Sep 13 16:05:13 2016 +0200

    Update ihm-demo-web-application to use shiro filter for Authentication

[33mcommit cbd2d1f4b7b48dd5bbaa998e3a7262b241ea91b1[m
Author: Yousri KOUKI <yousri.kouki.ext@culture.gouv.fr>
Date:   Thu Sep 15 14:02:06 2016 +0200

    add package-info

[33mcommit a62d120a73a7ef67bc2a8963377b5f679b62b1e5[m
Author: Yousri KOUKI <yousri.kouki.ext@culture.gouv.fr>
Date:   Tue Sep 13 16:19:31 2016 +0200

    shiro X509 Authentication
    
    shiro filter IngestExternal Server
    
    secure client configuration (common-private)
    
    secure client configuration (ingest-external-client)

[33mcommit 08c8659051d3e2ee078deaeeb461fc5906f69650[m
Merge: 51aefac b240ef2
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Thu Sep 15 11:36:03 2016 +0000

    Merge branch 'item_748' into 'master_iteration_8'
    
    story_748 (IHM) + Bug Fix Processing (remove redondant Unit LC update)
    
    
    
    See merge request !568

[33mcommit b240ef28b3d0115e59df63402e8828e7bf610e18[m
Author: Hela Amri <hela.amri.ext@culture.gouv.fr>
Date:   Thu Sep 15 12:36:21 2016 +0200

    story_748 : Front part

[33mcommit 6793b826585d3693d4de99ea94d4f86616dbb445[m
Author: Hela Amri <hela.amri.ext@culture.gouv.fr>
Date:   Thu Sep 15 12:35:39 2016 +0200

    Bug Fix Processing : remove redondant Unit LC update

[33mcommit 51aefac72162ba2a726b1755328d4bbaa190ac2b[m
Merge: af9f6d0 73e9081
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Thu Sep 15 10:22:52 2016 +0000

    Merge branch 'item_874_client_auth' into 'master_iteration_8'
    
    item_874 : config ssl-jetty TSL
    
    item_874 : config ssl-jetty TSL
    
    See merge request !538

[33mcommit 73e9081a7324716ad313b3bc3ddf317ce9c97b27[m
Author: Yousri KOUKI <yousri.kouki.ext@culture.gouv.fr>
Date:   Tue Sep 13 16:19:31 2016 +0200

    shiro X509 Authentication
    
    shiro filter IngestExternal Server
    
    secure client configuration (common-private)
    
    secure client configuration (ingest-external-client)

[33mcommit af9f6d0b24f810a29f4c6fbaa312c4dc11583d42[m
Merge: 2991a29 9c2d5da
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Wed Sep 14 17:45:17 2016 +0000

    Merge branch 'bug_856' into 'master_iteration_8'
    
    bug_856 add default value for format
    
    
    
    See merge request !564

[33mcommit 9c2d5da80920f18a7c3b45b397f1dca0c219d016[m
Author: NGO BA TUAN <ba-tuan.ngo.ext@culture.gouv.fr>
Date:   Wed Sep 14 16:53:04 2016 +0200

    bug_856 Add default value for format

[33mcommit 2991a29fba15ad79d71e28d0254484d3f025cda2[m
Merge: 43fd369 b220f54
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Wed Sep 14 11:36:22 2016 +0000

    Merge branch 'item_946' into 'master_iteration_8'
    
    ITEM #946 : Bugfixes + Non static Parameters + Public API
    
    - Public API for NOPQuery
    
    ITEM #946 :
    -- BUG FIXES
    - CheckStorageAvailability final result of workflow bugfix
    - CheckConformity action to NOBLOCK
    - Bugfix FATAL on missing manifest
    - Fix filename in upload SIP step if not present : empty
    - Fix on CheckConformity result : WARNING => KO and multiple message => one message
    - bug 757 / checkConformity - Add KO message for other KO/FATAL
    - Add managment of down server in processing client to ensure we have a final status FATAL in workflow + Fixed ProcessIntegrationTest (wasnt working !!)
    
    -- #913 Non static parameters
    - Refactoring : VitamParameter and ParameterHelper in common-private module
    - Refactoring : LogbookParameters with VitamParameter and ParameterHelper
    - Add : WorkerParameters, its abstract and default implementation, parameter name WorkerParameterName and the factory WorkerParametersFactory
    - Apply : WorkerParamaters on processing module (set default mandatory and fix unit tests)
    - Documentation : add common-private documentation for VitamParameters and ParameterHelper
    - Documentation : add documentation on processing module for WorkerParameters
    
    See merge request !558

[33mcommit 43fd3698a10c9796c5a730afef02e4868aaabbb3[m
Merge: 222b883 717eb23
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Wed Sep 14 11:35:23 2016 +0000

    Merge branch 'item_945' into 'master_iteration_8'
    
    library rpm including javadoc, docs & landing for docs
    
    
    
    See merge request !562

[33mcommit 717eb2339ca93a0029ebac99f087359b9b7f3279[m
Author: Olivier MARSOL <olivier.marsol.ext@culture.gouv.fr>
Date:   Wed Sep 14 10:03:36 2016 +0200

    library rpm including javadoc, docs & landing for docs

[33mcommit b220f54d112a938996b73fa214eebf1c9db325ef[m
Author: gafou <gaelle.fournier@smile.fr>
Date:   Thu Sep 1 11:26:59 2016 +0200

    Quick FIX :
    - Public API for NOPQuery
    
    ITEM #946 :
    -- BUG FIXES
    - CheckStorageAvailability final result of workflow bugfix
    - CheckConformity action to NOBLOCK
    - Bugfix FATAL on missing manifest
    - Fix filename in upload SIP step if not present : empty
    - Fix on CheckConformity result : WARNING => KO and multiple message => one message
    - bug 757 / checkConformity - Add KO message for other KO/FATAL
    - Add managment of down server in processing client to ensure we have a final status FATAL in workflow + Fixed ProcessIntegrationTest (wasnt working !!)
    
    -- #913 Non static parameters
    - Refactoring : VitamParameter and ParameterHelper in common-private module
    - Refactoring : LogbookParameters with VitamParameter and ParameterHelper
    - Add : WorkerParameters, its abstract and default implementation, parameter name WorkerParameterName and the factory WorkerParametersFactory
    - Apply : WorkerParamaters on processing module (set default mandatory and fix unit tests)
    - Documentation : add common-private documentation for VitamParameters and ParameterHelper
    - Documentation : add documentation on processing module for WorkerParameters

[33mcommit 222b883b477e7f03c2b45466af67b6fda07fb9da[m
Merge: ddcc427 cd59b12
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Wed Sep 14 07:33:41 2016 +0000

    Merge branch 'item_490_Regle_De_Gestion' into 'master_iteration_8'
    
    item_490: Rules Management Implementation
    
    * fix feedback received from Fréd .
    * rebase done .
    
    See merge request !559

[33mcommit cd59b120ab0603f17f89fe8154c57f321363d155[m
Author: Haykel <haykel.benmassaoud.ext@culture.gouv.fr>
Date:   Tue Sep 13 16:41:31 2016 +0200

    Rules features Implementation

[33mcommit ddcc427e097878d27319d8d1244fd41814d5ea8a[m
Merge: badf552 4de895b
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Tue Sep 13 14:24:30 2016 +0000

    Merge branch 'item_786_v2_workspace' into 'master_iteration_8'
    
    item 786:xml jetty configuration worksapce module
    
    
    
    See merge request !560

[33mcommit badf552ebc0c0d178981f68ddddcdbda7a05f3f0[m
Merge: c7f6477 8e095dc
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Tue Sep 13 13:31:39 2016 +0000

    Merge branch 'item_748' into 'master_iteration_8'
    
    story_748 (fil d'ariane) : Back end + Init Front part
    
    
    
    See merge request !553

[33mcommit 4de895bf1b64c5703212c335b9c4707455f8d5c3[m
Author: Ramzi LAZREG <ramzy.lazreg.ext@culture.gouv.fr>
Date:   Mon Sep 12 14:48:05 2016 +0200

    item 786:xml jetty configuration worksapce module

[33mcommit 8e095dcd211fdf5a1f0c0f33c3f020fb00759b7f[m
Author: Hela Amri <hela.amri.ext@culture.gouv.fr>
Date:   Tue Sep 13 14:05:08 2016 +0200

    story_748: Back side

[33mcommit 015225ba2340f434987fa083308ee35757616488[m
Author: Hela Amri <hela.amri.ext@culture.gouv.fr>
Date:   Tue Sep 13 14:04:16 2016 +0200

    story_748 : Front part

[33mcommit c7f6477247356e633a1895b9f8bd7f79dab6846d[m
Merge: e192185 a4bbf2a
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Tue Sep 13 07:10:10 2016 +0000

    Merge branch 'item_783_metadata' into 'master_iteration_8'
    
    select Unit Elasticsearch
    
    
    
    See merge request !539

[33mcommit e192185bd873d2fb6841fc3a87bfb33a5b19365d[m
Merge: bd1ee88 b28948b
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Tue Sep 13 07:09:40 2016 +0000

    Merge branch 'item_710' into 'master_iteration_8'
    
    Item #710 : soap ui integration
    
    - init Soap UI tests
    - added tests cases for #655/946
    
    Fixed some bugs :
    - Ingest external response json serialization problem
    - NPE in Select DSL for 'empty' query
    - Fixed some logbook outcome messages and evType
    - Changed step outcomeMessage aggregation
    
    See merge request !547

[33mcommit bd1ee883b322e4bef8b8a9946ec70240a5847a0f[m
Merge: 9641ce7 9351114
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Tue Sep 13 07:09:31 2016 +0000

    Merge branch 'item_885' into 'master_iteration_8'
    
    item_885 Fix filter algo
    
    
    
    See merge request !554

[33mcommit 9641ce7e8c3b1a568ad2089164a113ff6b0f8dba[m
Merge: a8c93f9 c65e94b
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Mon Sep 12 16:52:36 2016 +0000

    Merge branch 'bug_901' into 'master_iteration_8'
    
    fix list Container Options (maxResults)
    
    https://dev.programmevitam.fr/plugins/tracker/?aid=901
    
    See merge request !510

[33mcommit 93511143b33e6553fc331e363372bec5c35675a4[m
Author: NGO BA TUAN <ba-tuan.ngo.ext@culture.gouv.fr>
Date:   Mon Sep 12 15:34:09 2016 +0200

    item_885 Fix filter algo

[33mcommit a4bbf2a65bd1ae4f02646b95c974550f9f9ca432[m
Author: Yousri KOUKI <yousri.kouki.ext@culture.gouv.fr>
Date:   Mon Sep 12 15:47:40 2016 +0200

    select unit unsing Elasticsearch

[33mcommit 8af0b367f645ea1f5a4ec4da20759266c2903cb6[m
Author: Yousri KOUKI <yousri.kouki.ext@culture.gouv.fr>
Date:   Mon Sep 12 15:46:40 2016 +0200

    select unit Elasticsearch

[33mcommit b28948bef3b64af5d38c000109033b4fe885172d[m
Author: Gael Nieutin <gael.nieutin.ext@culture.gouv.fr>
Date:   Fri Sep 2 17:00:31 2016 +0200

    Item #710 : soap ui integration
    - init Soap UI tests
    - added tests cases for #655/946
    
    Fixed some bugs :
    - Ingest external response json serialization problem
    - NPE in Select DSL for 'empty' query -> NOPQuery
    - Fixed some logbook outcome messages and evType
    - Changed step outcomeMessage aggregation

[33mcommit a8c93f979ceda0ae6aed3f5771f4239c6f45a950[m
Merge: 1b60638 e238960
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Fri Sep 9 15:26:47 2016 +0000

    Merge branch 'item_885' into 'master_iteration_8'
    
    WAF filter
    
    
    
    See merge request !546

[33mcommit e2389600eb4dc79618e5b68c2a83cdde7e2593a7[m
Author: NGO BA TUAN <ba-tuan.ngo.ext@culture.gouv.fr>
Date:   Fri Sep 9 16:30:02 2016 +0200

    item_885 Add web application firewall with ESAPI validator

[33mcommit 1b60638033db3236d8f9c13d7b9a652051f94ae2[m
Merge: 4eff938 770a302
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Fri Sep 9 08:20:12 2016 +0000

    Merge branch 'bug_894' into 'master_iteration_8'
    
    Fix minor bug in ihm-demo
    
    
    
    See merge request !548

[33mcommit 4eff9381095ae6998f3d7a3b68943444f1648c78[m
Merge: d54f4f1 8f329f8
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Fri Sep 9 07:43:39 2016 +0000

    Merge branch 'item_786_v2_funct_adm' into 'master_iteration_8'
    
    Item 786 v2 funct adm
    
    apply jetty config xml to internal module functional administration with :
    increase time out
    
    See merge request !541

[33mcommit 770a3024572f59e24eec32a091f6a2dda0d832a5[m
Author: NGO BA TUAN <ba-tuan.ngo.ext@culture.gouv.fr>
Date:   Fri Sep 9 09:34:41 2016 +0200

    Fix minor bug in ihm-demo

[33mcommit 8f329f8e40c79c010cedaa12d8afca391a9df057[m
Author: Ramzi LAZREG <ramzy.lazreg.ext@culture.gouv.fr>
Date:   Thu Sep 8 10:52:17 2016 +0200

    US786 : apply config xml jetty and port configuration in test and production

[33mcommit d54f4f12aca1759405dbf78079171428f116fa7e[m
Merge: ef8b93b ad77c71
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Thu Sep 8 07:10:52 2016 +0000

    Merge branch 'integration_iteration_8' into 'master_iteration_8'
    
    Integration iteration 8
    
    Added shiro (related to new group & users in VITAM LDAP)
    
    See merge request !544

[33mcommit ad77c71ca8f13828dff68f672591e470c9bb84e7[m
Author: Olivier MARSOL <olivier.marsol.ext@culture.gouv.fr>
Date:   Wed Sep 7 18:20:50 2016 +0200

    added shiro with correct rights as in LDAP

[33mcommit ef8b93bcfa4a05edf63a0c7ab0a3b6c9dbed843e[m
Merge: e6f2889 bbea4dd
Author: Olivier Marsol <olivier.marsol.ext@culture.gouv.fr>
Date:   Wed Sep 7 10:03:33 2016 +0000

    Merge branch 'integration_iteration_8' into 'master_iteration_8'
    
    added shiro config file on IHM-demo
    
    
    
    See merge request !542

[33mcommit bbea4dd1fa0bcc1b1868a8a7a680b3240fced277[m
Author: Olivier MARSOL <olivier.marsol.ext@culture.gouv.fr>
Date:   Wed Sep 7 10:10:00 2016 +0200

    added shiro config file on IHM-demo

[33mcommit e6f28890ba3a9be92382c19c9ef83d9556809424[m
Merge: 8c2ac71 b22f131
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Tue Sep 6 16:01:04 2016 +0000

    Merge branch 'item_786_v1_port_release3' into 'master_iteration_8'
    
    Item 786 v1 port release3
    
    US786 : apply config xml jetty and port configuration in test and production code apply for :
    - access
    - ingest-ext
    -  ingest-int
    - metadata
    - ihm-demo
    
    remove test on system.exit
    increase time out
    
    See merge request !531

[33mcommit b22f131f68a7f01e23895762264401b96d149a39[m
Author: Ramzi LAZREG <ramzy.lazreg.ext@culture.gouv.fr>
Date:   Tue Sep 6 17:07:40 2016 +0200

    US786 : apply config xml jetty and port configuration in test and production code apply for access, ingest-ext, ingest-int, metadata and ihm-demo

[33mcommit 8c2ac719f9a05779735dd70b9f6b487ca81914f8[m
Merge: 91e1008 ae543c6
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Tue Sep 6 14:59:29 2016 +0000

    Merge branch 'bug_894' into 'master_iteration_8'
    
    Item 585
    
    _ Add authentication API in ihm-demo
    _ Add login and logout button in ihm-demo webapp (valid by POs)
    _ Add angular-cookies (third party) in ihm-demo webapp
    _  ihm-demo core module for authentication
    
    See merge request !535

[33mcommit ae543c696549257403fbedcb263796ac61bb1fd9[m
Author: NGO BA TUAN <ba-tuan.ngo.ext@culture.gouv.fr>
Date:   Mon Sep 5 17:49:23 2016 +0200

    Item_585 Add authentication API in ihm-demo

[33mcommit 3186c405db895c0f338859659a58c7169c7ec5d6[m
Author: NGO BA TUAN <ba-tuan.ngo.ext@culture.gouv.fr>
Date:   Mon Sep 5 17:48:57 2016 +0200

    Item_585 Add login and logout button in ihm-demo webapp

[33mcommit 4fd867acb60c699a0c2437771803fbc11ce9f57b[m
Author: NGO BA TUAN <ba-tuan.ngo.ext@culture.gouv.fr>
Date:   Mon Sep 5 17:47:56 2016 +0200

    Item_585 Add angular-cookies in ihm-demo webapp

[33mcommit 9f89114c8e7b1461b89936d546cf09c9265b40d1[m
Author: NGO BA TUAN <ba-tuan.ngo.ext@culture.gouv.fr>
Date:   Mon Sep 5 17:46:43 2016 +0200

    Item_585 ihm-demo core module for authentication

[33mcommit 91e1008ab076d5fa866b9785949e1c8268b7c240[m
Merge: 589d109 a474f17
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Tue Sep 6 11:23:33 2016 +0000

    Merge branch 'integration_iteration_8' into 'master_iteration_8'
    
    fixed a bug in topbeat ; now works.
    
    And now, 2 roles :
    - one for VITAM
    - one for extra (useful tools only for our platform)
    
    Added :
    - correct elasticsearch cluster (working with iteration 8 on int3)
    - fixed storage-engine to storage (along with the other merge request that fixes pom)
    
    See merge request !532

[33mcommit a474f17f8fdeb8e32cb7994362e995e06ebac81d[m
Author: Olivier MARSOL <olivier.marsol.ext@culture.gouv.fr>
Date:   Tue Sep 6 11:21:03 2016 +0200

    added correct storage call to rpm

[33mcommit 589d1090246266201eee420f350aabb6e458e32b[m
Merge: 7aca221 7565cf4
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Tue Sep 6 10:32:44 2016 +0000

    Merge branch 'update_pom' into 'master_iteration_8'
    
    storage-engine component becomes storage
    
    Renamed storage-engine to storage
    
    See merge request !537

[33mcommit 7565cf47d8f8d8ddf5e93fc4d631f3436e7fc52d[m
Author: Olivier MARSOL <olivier.marsol.ext@culture.gouv.fr>
Date:   Tue Sep 6 11:05:36 2016 +0200

    storage-engine component becomes storage

[33mcommit 7aca22185814e7d6e69ee3f174596fc3471f3f18[m
Merge: b13f2a7 04bcebf
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Mon Sep 5 15:11:03 2016 +0000

    Merge branch 'item_783_metadata' into 'master_iteration_8'
    
    Item 783 metadata
    
    insert Unit
    
    See merge request !528

[33mcommit b13f2a7409ceced8faf1608e18cadaa384f31a8e[m
Merge: 8146ebf b23a2e9
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Mon Sep 5 13:32:29 2016 +0000

    Merge branch 'fix_docker_access' into 'master_iteration_8'
    
    added missing call to storage engine from access component in docker
    
    
    
    See merge request !529

[33mcommit 8146ebfc00d072da549337c4f49ddce61d895358[m
Merge: 061e9fe 9e5bbf2
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Mon Sep 5 13:31:49 2016 +0000

    Merge branch 'story_87' into 'master_iteration_8'
    
    Story 87 : objects existence indicator on search page
    
    
    
    See merge request !527

[33mcommit 04bcebf18a197520633934b640c7d0d9cbe7dee6[m
Author: Yousri KOUKI <yousri.kouki.ext@culture.gouv.fr>
Date:   Mon Sep 5 15:15:10 2016 +0200

    index Unit

[33mcommit b23a2e9cff79cedec648a8a4f3b54aea88a8fe8a[m
Author: Olivier MARSOL <olivier.marsol.ext@culture.gouv.fr>
Date:   Mon Sep 5 14:42:33 2016 +0200

    added missing call to storage engine from access component in docker

[33mcommit 9e5bbf2584bb65accdef3c8839d26050fdcc9016[m
Author: Hela Amri <hela.amri.ext@culture.gouv.fr>
Date:   Mon Sep 5 14:09:59 2016 +0200

    story_87:add _og token to enable its selection

[33mcommit 98e76fb1ed7603c2b366cc502c3408b41b5b31a6[m
Author: Hela Amri <hela.amri.ext@culture.gouv.fr>
Date:   Mon Sep 5 14:08:47 2016 +0200

    story_87 : Front part

[33mcommit 369746966a0ac58593e791f94edfcc7524354557[m
Author: Hela Amri <hela.amri.ext@culture.gouv.fr>
Date:   Mon Sep 5 14:07:06 2016 +0200

    story_87 : new dependency (jquery-ui) to add datepicker component

[33mcommit 061e9fe353f204fd531c735de6f99e81d7600e5f[m
Merge: cc544bc 4d40803
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Mon Sep 5 11:48:33 2016 +0000

    Merge branch 'item_922' into 'master_iteration_8'
    
    Item #922 - IHM Download
    
    - Fixed Qualifier : DSL Part
    - Modification of the Backend : calling access to get objects groups and objects files
    - Modification of the Frontend : IHM Part + angular methods
    - Tests + javadoc
    
    See merge request !526

[33mcommit cc544bc44fc20c7295a6809960eca16a3f0edf13[m
Merge: 23097b6 6a12a07
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Mon Sep 5 08:53:19 2016 +0000

    Merge branch 'bug_894' into 'master_iteration_8'
    
    Fix bug 893 + story_872 Fix PUID display iin ihm-demo
    
    - Fix bug 893
    * https://dev.programmevitam.fr/plugins/tracker/?aid=893
    - Fix ihm-demo :
    * Display all formats when web page loaded
    * Display priority list with puid which is clickable
    
    See merge request !507

[33mcommit 4d408031485e053e74fe00de0ce79f3615328f19[m
Author: germain ledroit <germain.ledroit.ext@culture.gouv.fr>
Date:   Mon Aug 29 15:24:33 2016 +0200

    Item #922 - IHM Download
    - Fixed Qualifier : DSL Part
    - Modification of the Backend : calling access to get objects groups and objects files
    - Modification of the Frontend : IHM Part + angular methods
    - Tests + javadoc

[33mcommit 6a12a07a843d338ef8250beb9f0211b192b6612b[m
Author: NGO BA TUAN <ba-tuan.ngo.ext@culture.gouv.fr>
Date:   Thu Sep 1 10:38:54 2016 +0200

    Item_872 Refactor format display

[33mcommit f556e63787b2de72b200b35cc8f41ffa4568c737[m
Author: NGO BA TUAN <ba-tuan.ngo.ext@culture.gouv.fr>
Date:   Mon Aug 29 16:33:59 2016 +0200

    Fix bug 893

[33mcommit 23097b6b7c009cf1d75254bd33321e6b57f41541[m
Merge: 6582cef 534a01c
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Fri Sep 2 16:25:20 2016 +0000

    Merge branch 'item_768' into 'master_iteration_8'
    
    item 768: upload sip screen
    
    
    
    See merge request !525

[33mcommit 534a01c1196b2f0e36f50dc4ec95b299fd843b4b[m
Author: Ramzi LAZREG <ramzy.lazreg.ext@culture.gouv.fr>
Date:   Fri Sep 2 14:03:10 2016 +0200

    item 768: upload sip screen

[33mcommit 6582cefa8c40afcdde2f5ce1a15ae91a224ff8d6[m
Merge: 6c29107 2e2b6be
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Fri Sep 2 14:17:53 2016 +0000

    Merge branch 'item_891' into 'master_iteration_8'
    
    item_891: IHM ref format
    
    ça était vu par les archivistes
    
    See merge request !522

[33mcommit 2e2b6becf4f709f7dbc641e77f7a73551ddb6473[m
Author: CHEN Zhang <zchen@linagora.com>
Date:   Thu Sep 1 18:24:06 2016 +0200

    item_891: IHM ref format

[33mcommit 6c29107d9ec346d37ba6998963a0dd66741ca5de[m
Merge: 8c12101 9ea9c26
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Thu Sep 1 14:29:59 2016 +0000

    Merge branch 'integration_iteration_8' into 'master_iteration_8'
    
    added transform shell so jenkins slave sphinx can build raml 2 html
    
    Job on Jenkins  is vitam-master_iteration_n-raml2html (https://dev.programmevitam.fr/jenkins/job/vitam-master_iteration_n-raml2html/)
    
    See merge request !519

[33mcommit 9ea9c26d52f266be19dd0770680d767c0f9f3cb8[m
Author: Olivier MARSOL <olivier.marsol.ext@culture.gouv.fr>
Date:   Thu Sep 1 16:18:26 2016 +0200

    added transform shell so jenkins slave sphinx can build raml 2 html

[33mcommit 8c12101fdad538cd55ab26f2e4d03f5217db149e[m
Merge: 60395b1 a3a5f20
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Thu Sep 1 13:26:55 2016 +0000

    Merge branch 'poc_consul' into 'master_iteration_8'
    
    Added native (light) LB & HA capability to vitam with consul (dns-based)
    
    Added POC consul
    
    See merge request !514

[33mcommit 60395b149d28cfdd5bbb9aa613ffd70d6e8f7bf5[m
Merge: 6733507 b93bef7
Author: Olivier Marsol <olivier.marsol.ext@culture.gouv.fr>
Date:   Thu Sep 1 09:27:53 2016 +0000

    Merge branch 'fix_docker_local_it8' into 'master_iteration_8'
    
    Fixed version in hosts.local
    
    Fixed version in hosts.local
    
    See merge request !515
    
    Merge Request accepté par l'intégration après accord de Frédéric BREGIER

[33mcommit b93bef7470232dc36e29f2ff9b63b72ed16afc51[m
Author: germain ledroit <germain.ledroit.ext@culture.gouv.fr>
Date:   Wed Aug 31 17:53:37 2016 +0200

    Fixed version in hosts.local

[33mcommit a3a5f203686d722b74e3280dc0e0fbfec248ab27[m
Author: Kristopher Waltzer <kristopher.waltzer.ext@culture.gouv.fr>
Date:   Wed Aug 31 17:28:01 2016 +0200

    Added native (light) LB & HA capability to vitam with consul (dns-based)

[33mcommit 67335076105e7fdd32e6944d049bf75705509a3b[m
Merge: e0893d0 54f9326
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Wed Aug 31 12:56:37 2016 +0000

    Merge branch 'fix_ingestExternalConf' into 'master_iteration_8'
    
    rename conf file (ingest external client)
    
    
    
    See merge request !435

[33mcommit e0893d068ec87fc043eae7c4811fbbd15653c044[m
Merge: 39b396d bfae0f6
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Wed Aug 31 12:52:22 2016 +0000

    Merge branch 'integration_iteration_8' into 'master_iteration_8'
    
    modified ansible so that integration can deploy iteration 8
    
    Modified ansible & added empt role for future use : topbeat integration
    
    See merge request !513

[33mcommit 39b396da71a98b4bb9bf7c4fa83a47a807de07bc[m
Merge: 868f446 e3b4837
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Wed Aug 31 12:50:12 2016 +0000

    Merge branch 'fix_dependency' into 'master_iteration_8'
    
    Remove metadata-builder dependency
    
    
    
    See merge request !512

[33mcommit bfae0f6b022b34725b60490f506d770f4f250bdb[m
Author: Olivier MARSOL <olivier.marsol.ext@culture.gouv.fr>
Date:   Wed Aug 31 14:46:48 2016 +0200

    modified ansible so that integration can deploy iteration 8

[33mcommit 54f9326521d994a01a1762c9dd3bcc1025f8d539[m
Author: Yousri KOUKI <yousri.kouki.ext@culture.gouv.fr>
Date:   Tue Aug 9 13:09:32 2016 +0200

    rename conf file (ingest external client)

[33mcommit c65e94b932b1af29e63dffdd5deb634bc715ea49[m
Author: Yousri KOUKI <yousri.kouki.ext@culture.gouv.fr>
Date:   Tue Aug 30 13:50:29 2016 +0200

    fix list Container Options (maxResults)

[33mcommit e3b48377ec432859acd05ee67130c0cdcab45d0b[m
Author: NGO BA TUAN <ba-tuan.ngo.ext@culture.gouv.fr>
Date:   Wed Aug 31 11:33:47 2016 +0200

    Remove metadata-builder dependency

[33mcommit 868f44616a4b36abcd208fa1c3fb6d1f96be58fd[m
Author: jgonthier <joachim.gonthier.ext@culture.gouv.fr>
Date:   Wed Aug 31 12:21:45 2016 +0200

    [manual update] prepare for next development iteration

[33mcommit db5335945a6894636093587c42edb4284e1f47e7[m
Merge: a051862 c4d90cb
Author: Joachim Gonthier <joachim.gonthier.ext@culture.gouv>
Date:   Wed Aug 31 08:07:13 2016 +0000

    Merge branch 'master_iteration_7' into 'master'
    
    Merge branch 'master_iteration_7' into 'master'
    
    Merge branch 'master_iteration_7' into 'master'
    
    See merge request !511

[33mcommit c4d90cb43b3e10c05468e01cfed580bcedc185af[m
Merge: 6715568 d1eed2c
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Mon Aug 29 13:34:50 2016 +0000

    Merge branch 'item_655_fix_workflow' into 'master_iteration_7'
    
    Item #655 - Workflow Fix
    
    - Step Blocking instead of non-blocking
    
    See merge request !505

[33mcommit d1eed2c5ae99774a9d035060be285badf9e4d535[m
Author: germain ledroit <germain.ledroit.ext@culture.gouv.fr>
Date:   Mon Aug 29 15:31:04 2016 +0200

    Item #655 - Workflow Fix
    - Step Blocking instead of non-blocking

[33mcommit 67155688ed5008dc23c4c28b7052ef0f9db455f3[m
Merge: 1dd17d5 2419c6a
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Mon Aug 29 11:17:12 2016 +0000

    Merge branch 'fix_docker' into 'master_iteration_7'
    
    ansible docker with external jetty configuration
    
    For dev that cannot install rpm ; compatible with 0.7.0
    
    See merge request !504

[33mcommit 2419c6a34daee5839c463dad6a0a9bb121b3f92c[m
Author: Olivier MARSOL <olivier.marsol.ext@culture.gouv.fr>
Date:   Mon Aug 29 12:42:29 2016 +0200

    ansible docker with external jetty configuration

[33mcommit 1dd17d5672906cfc390671019b95545e28d6a2d3[m
Merge: e6b9202 e9bb0d1
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Mon Aug 29 10:39:13 2016 +0000

    Merge branch 'item_889_fixes' into 'master_iteration_7'
    
    ITEM #889 (review fixes):
    
    - remove useless import
    - file format
    - using JsonHandler in toString() method in VitamError class
    - add unit test for this toString() method
    - remove useless String in AccessModuleImpl class
    
    See merge request !503

[33mcommit e9bb0d1b3e3354494f2fc8f86c49c49ec3a7a49f[m
Author: Cedric Legrand <cedric.legrand.ext@culture.gouv.fr>
Date:   Mon Aug 29 12:29:57 2016 +0200

    ITEM #889 (review fixes):
    
    - remove useless import
    - file format
    - using JsonHandler in toString() method in VitamError class
    - add unit test for this toString() method
    - remove useless String in AccessModuleImpl class

[33mcommit e6b92025c062b57c9502d1569c18db08ee3a03f0[m
Merge: e338dd3 ed35f91
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Mon Aug 29 09:41:21 2016 +0000

    Merge branch 'item_fix_apiTest' into 'master_iteration_7'
    
    fix test coverage
    
    Jenkins : https://dev.programmevitam.fr/jenkins/job/api-metadata/7/
    Sonar https://dev.programmevitam.fr/sonar/components/index?id=fr.gouv.vitam%3Ametadata%3Aitem_fix_apiTest
    
    See merge request !500

[33mcommit e338dd322392fc0049bd2ba70d221982ce1c9c50[m
Merge: 09ecca0 97ca141
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Mon Aug 29 09:16:18 2016 +0000

    Merge branch 'item_655' into 'master_iteration_7'
    
    Item #655 - Global Workflow/Ingests corrections
    
    - Task #906 : Review REST processing responses
    - Task #907 - Status management in the Process
    - Task #908 : Update logbook lifecycle messages
    - Task #909 : Workflow to match with functionnal requirements
    
    (Best effort mode)
    
    See merge request !498

[33mcommit 09ecca0d4370c87083cb9b041938de68ca110bf6[m
Merge: 9b06bd1 4306a6b
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Mon Aug 29 09:11:34 2016 +0000

    Merge branch 'story_741' into 'master_iteration_7'
    
    Story 741 : documentation + IHM correction
    
    
    
    See merge request !502

[33mcommit 9b06bd18d1093bf5f713d686561cab6bf911e7e8[m
Merge: 09da862 a2746b1
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Mon Aug 29 09:08:46 2016 +0000

    Merge branch 'bug_894' into 'master_iteration_7'
    
    Fix bug 894
    
    https://dev.programmevitam.fr/plugins/tracker/?aid=894
    
    See merge request !501

[33mcommit 4306a6bb90c3a12d2c91311af550e8759e7e4427[m
Author: Hela Amri <hela.amri.ext@culture.gouv.fr>
Date:   Mon Aug 29 10:59:10 2016 +0200

    story_741: IHM correction after functional tests

[33mcommit a2746b16332a1a29934267529645ec93a608b02f[m
Author: NGO BA TUAN <ba-tuan.ngo.ext@culture.gouv.fr>
Date:   Mon Aug 29 10:57:09 2016 +0200

    Fix bug 894

[33mcommit ed35f915fb27ecb4e4663559fc37e977f99cdd5c[m
Author: Yousri KOUKI <yousri.kouki.ext@culture.gouv.fr>
Date:   Fri Aug 26 17:06:51 2016 +0200

    fix test coverage

[33mcommit 97ca141db778d553f405650bf7c32d601bc3bd01[m
Author: Cedric Legrand <cedric.legrand.ext@culture.gouv.fr>
Date:   Mon Aug 22 18:59:10 2016 +0200

    Item #655 - Global Workflow/Ingests corrections
    
    - Task #906 : Review REST processing responses
    - Task #907 - Status management in the Process
    - Task #908 : Update logbook lifecycle messages
    - Task #909 : Workflow to match with functionnal requirements

[33mcommit 09da862824f32c674ad9ab59461d908f802ccdd9[m
Merge: a70aad3 19c46eb
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Mon Aug 29 08:27:21 2016 +0000

    Merge branch 'DEX_it7' into 'master_iteration_7'
    
    DEX improved version ; now full rpm
    
    
    
    See merge request !499

[33mcommit fbc7093798052c1571f4feca672a5e8cf24f6b1d[m
Author: hela.amri <hela.amri.ext@culture.gouv.fr>
Date:   Sun Aug 28 21:17:34 2016 +0200

    module ihm-demo : documentation

[33mcommit 19c46eb66951dbdc2f5f8489860c4af6f89ab392[m
Author: Olivier MARSOL <olivier.marsol.ext@culture.gouv.fr>
Date:   Fri Aug 26 19:33:21 2016 +0200

    enhanced version ; now full rpm

[33mcommit a70aad3539a3ab8f4d895506fb3f3a7d9982a085[m
Merge: 715e714 e135b22
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Fri Aug 26 17:24:15 2016 +0000

    Merge branch 'item_889' into 'master_iteration_7'
    
    Item #889: task Access GET objectGroup info + download + fix #904
    
    Client part (Get Object Information + Download Object)
    - AccessClient Interface for getting object informations with its ID as a json
    - AccessClient Interface for getting an object with its ID, as an inputStream
    - Implementation of the interface Mock + Rest
    - Tests
    - Correction of some FIXME
    - Documentation
    
    Download core
    - Core implementation of object download
    - put headers management in common module
    
    Download rest
    
    US #749 - Item #889 : Bug + review fixes + bug #904
    - Fix review comment for access client
    - Fixed Common database SelectToMongoDb to handle Slice
    - Fixed Metadata to explicitly use 'objectgroups' hint filter in DSL
    - Fixed Storage handler to use object GUID instead of digest (#904)
    - Fixed unit tests (access + processing + metadata + common private database)
    - Fixed VitamError json serialization (toString implemented)
    
    See merge request !497

[33mcommit e135b226f593e82638f68eaaca12e190ff559e49[m
Author: Gael Nieutin <gael.nieutin.ext@culture.gouv.fr>
Date:   Fri Aug 19 17:13:56 2016 +0200

    Item #889: task Access GET objectGroup info + download + fix #904
    
    Client part (Get Object Information + Download Object)
    - AccessClient Interface for getting object informations with its ID as a json
    - AccessClient Interface for getting an object with its ID, as an inputStream
    - Implementation of the interface Mock + Rest
    - Tests
    - Correction of some FIXME
    - Documentation
    
    Download core
    - Core implementation of object download
    - put headers management in common module
    
    Download rest
    
    US #749 - Item #889 : Bug + review fixes + bug #904
    - Fix review comment for access client
    - Fixed Common database SelectToMongoDb to handle Slice
    - Fixed Metadata to explicitly use 'objectgroups' hint filter in DSL
    - Fixed Storage handler to use object GUID instead of digest (#904)
    - Fixed unit tests (access + processing + metadata + common private database)
    - Fixed VitamError json serialization (toString implemented)

[33mcommit 715e714b9e4dcd9b7b9c209d7a469312042e833d[m
Merge: aba6626 581e6d0
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Fri Aug 26 15:56:49 2016 +0000

    Merge branch 'DIN_it7' into 'master_iteration_7'
    
    fixed typo & added enhancements
    
    DIN compatible with iteration 7
    
    See merge request !496

[33mcommit 581e6d005ec84760dc5f24715c0840485631eb0b[m
Author: Olivier MARSOL <olivier.marsol.ext@culture.gouv.fr>
Date:   Fri Aug 26 17:50:23 2016 +0200

    fixed typo & added enhancements

[33mcommit aba662644180b152fc52fde75eb6ecfebec1ea24[m
Merge: 6df32a1 cf1cab4
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Fri Aug 26 14:46:46 2016 +0000

    Merge branch 'integration_iteration_7' into 'master_iteration_7'
    
    Added external jetty config & storage-client conf for access & improvements
    
    
    
    See merge request !492

[33mcommit cf1cab4ceb4c3d41d6999f39ca3b296fa13c82ab[m
Author: Olivier MARSOL <olivier.marsol.ext@culture.gouv.fr>
Date:   Fri Aug 26 16:39:00 2016 +0200

    added external jetty config & storage-client conf for access & improvements

[33mcommit 6df32a16c6fe8b315a42df5477279aa083f9d4de[m
Merge: cf2424f 7cae189
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Fri Aug 26 14:34:48 2016 +0000

    Merge branch 'item_bug_898' into 'master_iteration_7'
    
    Fix bug 898 : javadoc & documentation
    
    
    
    See merge request !490

[33mcommit cf2424f163fa8d4a569babdb604d5ad6bd74888e[m
Author: Yousri Kouki <yousri.kouki.ext@culture.gouv.fr>
Date:   Fri Aug 26 14:32:55 2016 +0000

    Update DslQueryHelper.java

[33mcommit 7cae1896e0e739beb341a0d63cfb7a376d160ef1[m
Author: Ramzi LAZREG <ramzy.lazreg.ext@culture.gouv.fr>
Date:   Fri Aug 26 14:12:22 2016 +0200

    bug 898Optimize Graph class-Using WeakHashMap

[33mcommit 2e3ecb0d149ffb49ce9ba7b19bd581b51b53f93e[m
Merge: 3bf0703 6b8fb9d
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Fri Aug 26 12:56:05 2016 +0000

    Merge branch 'item_786_v1_release' into 'master_iteration_7'
    
    US786 - use xml configuration in jetty.
    
    apply to access, ihm-demo, ingest-int, ingest-ext, metadata
    
    See merge request !486

[33mcommit 3bf07032b5d7a64f44e642d103dc8d3eca894dc2[m
Merge: 9b1de58 136d188
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Fri Aug 26 10:39:25 2016 +0000

    Merge branch 'raml' into 'master_iteration_7'
    
    RAML Worker + Distributor
    
    
    
    See merge request !487

[33mcommit 9b1de58bde4fdd75162e3b7d2519a4f89ee46d87[m
Merge: efd30df 4e2e534
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Fri Aug 26 10:37:27 2016 +0000

    Merge branch '833-raml-V2' into 'master_iteration_7'
    
    Remove Web directory in REST/interne
    
    Closes #833
    
    See merge request !488

[33mcommit 4e2e534b81c879fea533a622bd250142dab29d5d[m
Author: Frederic Bregier <frederic.bregier@culture.gouv.fr>
Date:   Fri Aug 26 12:35:45 2016 +0200

    Remove Web directory in REST/interne

[33mcommit 136d188088e0a0f218684e445d7c713191e1d555[m
Author: Yousri KOUKI <yousri.kouki.ext@culture.gouv.fr>
Date:   Fri Aug 26 12:18:06 2016 +0200

    RAML Worker + Distributor

[33mcommit 6b8fb9df2c554819238abd680363d4d33af221da[m
Author: Buon SUI <bsui@cdh-it.com>
Date:   Thu Aug 11 09:09:09 2016 +0200

    US786 - use xml configuration in jetty.
      apply to access, ihm-demo, ingest-int, ingest-ext, metadata

[33mcommit efd30df038bfef4c5a98aaea20924be156e1a60b[m
Author: Thomas Morsellino <thomas.morsellino.ext@culture.gouv.fr>
Date:   Thu Aug 25 15:53:43 2016 +0200

    US #833 add RAML in vitam root

[33mcommit ecb524e28959d4523c43a1ab590099c293b87c01[m
Merge: 536e80d 797b917
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Thu Aug 25 17:13:02 2016 +0000

    Merge branch 'bugfix_item_656' into 'master_iteration_7'
    
    Bugfix test seda to ignore
    
    Re-add @ignore in test
    
    See merge request !483

[33mcommit 797b9178e0ba83bc9185559e0b2ec31999e33a45[m
Author: gafou <gaelle.fournier@smile.fr>
Date:   Thu Aug 25 12:39:19 2016 +0200

    Bugfix test seda to ignore

[33mcommit 536e80d61d15b9f90a356bd07c72a801b23b578b[m
Merge: 202336d b6a1bca
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Thu Aug 25 08:32:06 2016 +0000

    Merge branch 'item_656' into 'master_iteration_7'
    
    ITEM #656 : control SIP format ZIP
    
    Add control of SIP while unziping in wokspace
    Bugfix on IHM-DEMO
    TODO add filename on upload
    
    
    See merge request !480

[33mcommit b6a1bca75cd11963c7a3e50e361347046cc940d1[m
Author: gafou <gaelle.fournier@smile.fr>
Date:   Tue Aug 23 13:43:08 2016 +0200

    ITEM #656 : control SIP format ZIP
    
    Add control of SIP while unziping in wokspace
    Bugfix on IHM-DEMO
    TODO add filename on upload

[33mcommit 202336d9684a92f5a2ef2b8f3b7264133c855450[m
Merge: 6e87055 4a56a5c
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Thu Aug 25 07:47:42 2016 +0000

    Merge branch 'curator' into 'master_iteration_7'
    
    Curator
    
    Added curator add-on ; for now, removes older than 30 days indexes.
    
    See merge request !481

[33mcommit 4a56a5ca8a54881169881b1c05aba4ac71ed8e5d[m
Author: Olivier MARSOL <olivier.marsol.ext@culture.gouv.fr>
Date:   Thu Aug 25 09:44:14 2016 +0200

    curator add-on

[33mcommit 6e8705576502d025febcd7ce026bc222eae29c23[m
Merge: 7a04a1d e1392a8
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Wed Aug 24 17:54:26 2016 +0000

    Merge branch 'item_783_database' into 'master_iteration_7'
    
    QueryToElasticsearch, RequestToElasticsearch and ElasticsearchAccess
    
    
    
    See merge request !473

[33mcommit 7a04a1d4f5288d5c91f9cc3cf8a8903cae445908[m
Merge: 30b4738 f9dbc21
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Wed Aug 24 13:00:30 2016 +0000

    Merge branch 'integration_iteration_7' into 'master_iteration_7'
    
    Mise à jour des documentations intégration + fix déploiement docker
    
    - Mise à jour des documentations intégration : DAT, DEX, DIN (notamment modification de structure cible)
    - Automatisation de la récupération de la version du pom et de l'année
      courante pour l'appliquer à la configuration sphinx ;
    - Fix : ansiblerie pour le bon fonctionnement du déploiement docker
    
    See merge request !467

[33mcommit f9dbc21f25e73921adfaf66278ac5bfdd0f4f8c9[m
Author: Olivier MARSOL <olivier.marsol.ext@culture.gouv.fr>
Date:   Wed Aug 24 14:46:57 2016 +0200

    updated pom for proper rpm build according to new tree in dirs ; ansible rpm OK with log and consul tested on int3 ; doc review after comments

[33mcommit e1392a8d67b3ad98eb5aba78ce6d36c909573429[m
Author: Yousri KOUKI <yousri.kouki.ext@culture.gouv.fr>
Date:   Wed Aug 24 14:37:25 2016 +0200

    QueryToElasticsearch, RequestToElasticsearch and ElasticsearchAccess

[33mcommit 30b47387185b89d9f6ea1fb9649707f7d2b89906[m
Merge: 743594b 1e7f19e
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Tue Aug 23 16:07:18 2016 +0000

    Merge branch 'story_741' into 'master_iteration_7'
    
    Story 741 : documentation + maj de l'affichage du résultat de recherche logbook
    
    
    
    See merge request !474

[33mcommit 1e7f19ee85d1f025ab5698f1ce8dc65e3a09c005[m
Author: Hela Amri <hela.amri.ext@culture.gouv.fr>
Date:   Tue Aug 23 10:16:50 2016 +0200

    story_741: architecture fonctionnelle + technique de l'application Front

[33mcommit 29c7a5cea8378579f8efe199180702fb64de8810[m
Author: Hela Amri <hela.amri.ext@culture.gouv.fr>
Date:   Tue Aug 23 10:13:33 2016 +0200

    story_741 : add evIdProc to logbook search result

[33mcommit 743594bc8a653e81bab9cb75df013cbef6550c5c[m
Merge: 2a1e20c 762cd0c
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Thu Aug 18 16:43:05 2016 +0000

    Merge branch 'analyse-iteration-7' into 'master_iteration_7'
    
    Global Review Iteration 6
    
    
    
    See merge request !468

[33mcommit 762cd0c0cb321ee5f832c6325fa023b57a71ec12[m
Author: Frederic Bregier <frederic.bregier@culture.gouv.fr>
Date:   Tue Aug 16 19:33:31 2016 +0200

    Global Review Iteration 6

[33mcommit 2a1e20c074a01ff344a5748eca32f7c7db681fd8[m
Merge: cd4f6f6 24c2cca
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Thu Aug 18 15:26:48 2016 +0000

    Merge branch 'item_887' into 'master_iteration_7'
    
    US #749: Item #887 : Storage GET objects
    
    Client part
    - Definition of the interface + mock : StorageClient + StorageClientMock
    - Implementation of the interface for Rest : StorageClientRest
    - Documentation + Tests
    
    ENGINE
    - Storage distribution getContainerObject implementation
    - Storage resource getObject implementation
    - Add toString for RequestResponseError and VitamError for octet-stream serialisation
    
    Offer Implementation
    - Core : Modifications of the interface for getObject + Implementation
    - Rest : Modifications of the interface for GET Object + Implementation
    
    DRIVER
    - Driver implementation for default offer
    
    + Integration test
    
    See merge request !463

[33mcommit 24c2cca17a6ea687039f7f8cd11ff930af8e8e4d[m
Author: germain ledroit <germain.ledroit.ext@culture.gouv.fr>
Date:   Wed Aug 10 18:21:22 2016 +0200

    US #749: Item #887 : - Storage GET objects
    Client part
    - Definition of the interface + mock : StorageClient + StorageClientMock
    - Implementation of the interface for Rest : StorageClientRest
    - Documentation + Tests
    
    ENGINE
    - Storage distribution getContainerObject implementation
    - Storage resource getObject implementation
    - Add toString for RequestResponseError and VitamError for octet-stream serialisation
    
    Offer Implementation
    - Core : Modifications of the interface for getObject + Implementation
    - Rest : Modifications of the interface for GET Object + Implementation
    
    DRIVER
    - Driver implementation for default offer
    
    + Integration test

[33mcommit cd4f6f67c408b4daa09a03ddf85c7445d90145d8[m
Merge: 00be321 11775d7
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Wed Aug 17 16:37:56 2016 +0200

    Merge branch 'item_clean_pom' into 'master_iteration_7'
    
    Nettoyage pom.xml
    
    
    
    See merge request !465

[33mcommit 11775d76ea7415f44cde7ab7ee9a70683d666fb3[m
Author: Etienne CARRIERE <etienne.carriere@gmail.com>
Date:   Wed Aug 17 07:53:39 2016 +0200

    Clean pom.xml :
    
    + Delete duplicate dependencies that generates warning
    + Delete non standard maven plugin (ex: shade)
    + Remove dependency versions in the sub pom
    + Remove plugin version in the sub pom

[33mcommit 00be3218fb113330b6c667b714c099c245028559[m
Merge: 600ee8b cf3bef2
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Tue Aug 16 16:46:19 2016 +0200

    Merge branch 'item_888' into 'master_iteration_7'
    
    US #749: Item #888 : Metadata component update to handle GET/search on ObjectGroups
    
    Core
    - Implemented selectObjectGroupById + refactored with selectUnit
    
    Client
    - Implementation of Metadaclient method to get an object with an id and a query
    
    REST
    - Implement getObjectGroup resources (POST and GET)
    
    See merge request !464

[33mcommit 600ee8bbcd25cf2b5ff23cef96775f816057c3a8[m
Merge: e54437c 7e92420
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Tue Aug 16 16:26:21 2016 +0200

    Merge branch 'story_741' into 'master_iteration_7'
    
    story_741: functional tests corrections (Front part)
    
    
    
    See merge request !457

[33mcommit e54437c34e05b9d9d79951258aaa480d202a96a3[m
Merge: d5fc65b bb6ab30
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Tue Aug 16 15:32:57 2016 +0200

    Merge branch 'item_fix_816' into 'master_iteration_7'
    
    [BUGFIX] Fixed CheckStorageAvailabilityHandler to parse correctly json response from storage engine
    
    * Fixed CheckStorageAvailabilityHandler that was buggy at runtime
    
    See merge request !456

[33mcommit cf3bef24d9164d2ada99165a664f0ebc0dc48a2c[m
Author: Gael Nieutin <gael.nieutin.ext@culture.gouv.fr>
Date:   Fri Aug 12 17:02:23 2016 +0200

    US #749: Item #888 : Metadata component update to handle GET/search on ObjectGroups
    Core
    - Implemented selectObjectGroupById + refactored with selectUnit
    
    Client
    - Implementation of Metadaclient method to get an object with an id and a query
    
    REST
    - Implement getObjectGroup resources (POST and GET)

[33mcommit bb6ab303f948a32cc5c9a597ac3a6589af991723[m
Author: Gael Nieutin <gael.nieutin.ext@culture.gouv.fr>
Date:   Fri Aug 12 15:14:58 2016 +0200

    [BUGFIX] Fixed CheckStorageAvailabilityHandler to parse correctly json response from storage engine

[33mcommit 7e924209cdc86f09b5ffe1e8dbb506065c2229f0[m
Author: Hela Amri <hela.amri.ext@culture.gouv.fr>
Date:   Fri Aug 12 17:12:03 2016 +0200

    story_741: correct archive unit screen (User interface problems)

[33mcommit d5fc65b023d515cc737425b74c7a7cda9b7e26f1[m
Merge: 5a75d25 145906e
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Fri Aug 12 14:51:23 2016 +0200

    Merge branch 'integration_iteration_7' into 'master_iteration_7'
    
    For iteration 7 deployment :
    
    - rpm deployment
    - fix on docker deployment as in rpm deployment (esapi)
    - beginning of log management subsystem
    
    Première mouture du déploiement de l'itération 7 (rpm & docker + début log management) pour tests avec les développeurs
    
    See merge request !453

[33mcommit 145906e94aa2bfe6c5ee0f708e4acfd112e50b35[m
Author: Olivier MARSOL <olivier.marsol.ext@culture.gouv.fr>
Date:   Fri Aug 12 14:37:36 2016 +0200

    For iteration 7 deployment :
    - rpm deployment
    - fix on docker deployment as in rpm deployment (esapi)
    - beginning of log management subsystem

[33mcommit 5a75d25e7d6ecbeff5fd49436005d89ca66f20ea[m
Merge: dc6c445 f02463f
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Fri Aug 12 10:59:28 2016 +0200

    Merge branch 'item_816' into 'master_iteration_7'
    
    ITEM #816 : Storage capacity
    
    Capacity feature implementation on :
    - Storage Engine
    - Default offert
    - Workspace modification : get object information (size)
    - Handler creation + modification to call Storage methods
    - Handler registration in the Json + the process (WorkerImpl)
    - SedaUtils : add new methods for size calculation
    - Documentation
    - Fixed concurrent issues in Workspace and Storage client
    - Integration Test for Storage Part
    - Quick Fix for StorageDistribution
    - Fixed ESAPI OWASP problem causing maven tests failure
    
    See merge request !448

[33mcommit f02463f6d7c2aecb4c17a59bf3ac5b3e3e92ae7c[m
Author: Cedric Legrand <cedric.legrand.ext@culture.gouv.fr>
Date:   Thu Aug 4 14:35:46 2016 +0200

    ITEM #816 : Storage capacity
    Capacity feature implementation on :
    - Storage Engine
    - Default offert
    - Workspace modification : get object information (size)
    - Handler creation + modification to call Storage methods
    - Handler registration in the Json + the process (WorkerImpl)
    - SedaUtils : add new methods for size calculation
    - Documentation
    - Fixed concurrent issues in Workspace and Storage client
    - Integration Test for Storage Part
    - Quick Fix for StorageDistribution
    - Fixed ESAPI OWASP problem causing maven tests failure

[33mcommit dc6c445860fcea78b7cfd298468b1cad8bdae72e[m
Merge: f377b21 4915e69
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Thu Aug 11 12:19:11 2016 +0200

    Merge branch 'fix_storage' into 'master_iteration_7'
    
    store binary data object using GUID
    
    
    
    See merge request !446

[33mcommit 4915e69f2a4b8e303eed90c3192279e0e25d49de[m
Author: Yousri KOUKI <yousri.kouki.ext@culture.gouv.fr>
Date:   Thu Aug 11 09:48:13 2016 +0200

    store binary data object using GUID

[33mcommit f377b2126f125d1a90ca22891d59e6589acfa68b[m
Merge: 0b7c7c0 c6a4cb6
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Wed Aug 10 17:57:52 2016 +0200

    Merge branch 'item_785_mongo_access_collection' into 'master_iteration_7'
    
    item_785 : refactor DSL metadata Collection, Mongodbaccess, Module Access
    
    item_785 : refactor DSL metadata Collection, Mongodbaccess, Module Access
    
    See merge request !434

[33mcommit c6a4cb652f70cd0c87af209637ac9ff2b6e0b9c9[m
Author: Hoan Vu <hoan.vu.ext@culture.gouv.fr>
Date:   Tue Aug 9 17:03:18 2016 +0200

    item_785 : Refactor DSL metadata for collection, mongoDbAccess and module Access

[33mcommit 0b7c7c06393cc59ca8b5216d1223fb9b180b05b8[m
Merge: 2144ae2 c0e4460
Author: Kristopher Waltzer <kristopher.waltzer.ext@culture.gouv>
Date:   Wed Aug 10 16:54:34 2016 +0200

    Merge branch 'item_clean_logging' into 'master_iteration_7'
    
    Configuration du logger pour limiter la verbosité de la sortie standard des builds
    
    
    
    See merge request !444

[33mcommit c0e4460678604c8d9bc3d4b57b67acc5718f9639[m
Author: Etienne CARRIERE <etienne.carriere@culture.gouv.fr>
Date:   Wed Aug 10 15:59:02 2016 +0200

    Passage du niveau des fichiers logback-test.xml au niveau WARN sur les
    fichiers existants + ajout du fichier pour
    functional-administration-rest
    access-rest
    ihm-demo-web-application

[33mcommit 2144ae21f126a9cce58453078704f6aa4dafde12[m
Merge: 7ff6df9 70b077c
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Wed Aug 10 12:19:02 2016 +0200

    Merge branch 'item_818_1_integrate_esapi' into 'master_iteration_7'
    
    Item 818 1 integrate esapi
    
    item_818 : integration of esapi into common and workspace-rest
    
    See merge request !424

[33mcommit 70b077c56bae41030ed5dada8835e7969047d938[m
Author: Buon SUI <bsui@cdh-it.com>
Date:   Thu Aug 4 15:55:19 2016 +0200

    Item 818 : common ESAPI sanitize check + integration in access, functionnal-administration,ihm-demo and workspace
    
    fix functionnal admin test

[33mcommit 4d5818ef65bf06f558bb698111b6ae0e6ceb0773[m
Author: Frederic Bregier <frederic.bregier@culture.gouv.fr>
Date:   Thu Aug 4 15:55:07 2016 +0200

    integrate owasp and esapi into workspace-rest

[33mcommit 7ff6df9278abadf95706d7da9d8c9deaea6365fa[m
Merge: e5cd35b 4360d45
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Tue Aug 9 18:57:06 2016 +0200

    Merge branch 'story_741' into 'master_iteration_7'
    
    Story 741 : meilleure lisibilité du logbook
    
    
    
    See merge request !433

[33mcommit e5cd35b0ce8199728584a62bc84cb5f81454d54e[m
Merge: 14523dc 92480ab
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Tue Aug 9 18:27:34 2016 +0200

    Merge branch 'item_817_SIPComplexe' into 'master_iteration_7'
    
    item_817:Creation d'arbre _ Archive Unit Ref Management
    
    
    
    See merge request !436

[33mcommit 4360d4510ce27fb734f7352e0a888b9fae1fc590[m
Author: Hela Amri <hela.amri.ext@culture.gouv.fr>
Date:   Tue Aug 9 18:21:29 2016 +0200

    story_741 : add before last event to get obIdIn value

[33mcommit 8b2d78c3b785fa81deb74aa9e019f1f8d454cc8d[m
Author: Hela Amri <hela.amri.ext@culture.gouv.fr>
Date:   Tue Aug 9 18:20:16 2016 +0200

    story_741: update controllers + html templates + css sheets

[33mcommit a0e06139b9f1ad0e4a2b0514e890f6beced63fcf[m
Author: Hela Amri <hela.amri.ext@culture.gouv.fr>
Date:   Tue Aug 9 18:17:05 2016 +0200

    story_741 : new user interface components + new logo + update css files

[33mcommit 92480ab588855e8b4f7a794f315b0627654ff066[m
Author: Haykel <haykel.benmassaoud.ext@culture.gouv.fr>
Date:   Tue Aug 9 10:22:33 2016 +0200

    item_817:Creation d'arbre _ Archive Unit Ref Management

[33mcommit 14523dcc7d15c7b52f66aae4878b82a368921ceb[m
Merge: 650638f e5f5960
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Tue Aug 9 17:04:33 2016 +0200

    Merge branch 'systemd_ingest_external' into 'master_iteration_7'
    
    changed systemD service name & parameters for ingest-internal
    
    
    
    See merge request !437

[33mcommit e5f5960a25276f922dbf12b4024a61603a2ebf57[m
Author: Olivier MARSOL <olivier.marsol.ext@culture.gouv.fr>
Date:   Tue Aug 9 16:16:29 2016 +0200

    changed systemD service name for ingest internal & parameters for all services (unix.user and unix.group)

[33mcommit 650638f6f0b7b641ccd4a5942293980cde7b7cdf[m
Merge: 50c6509 6bd42cf
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Mon Aug 8 15:28:35 2016 +0200

    Merge branch 'fix_master07' into 'master_iteration_7'
    
    fix master_iteration_5
    
    fix objectIdentifierIncome
    https://dev.programmevitam.fr/plugins/tracker/?aid=759
    
    fix ObjectGroupReference GUID
    https://dev.programmevitam.fr/plugins/tracker/?aid=805
    
    Fix BinaryObject id in ObjectGroup
    https://dev.programmevitam.fr/plugins/tracker/?aid=805
    
    See merge request !432

[33mcommit 6bd42cfacf13446ecc2a24d4245bba4d54039199[m
Author: Yousri KOUKI <yousri.kouki.ext@culture.gouv.fr>
Date:   Mon Aug 8 14:38:50 2016 +0200

    fix master_iteration_5

[33mcommit 50c650939d18e318a3518c7a83e25bf24f8ee570[m
Merge: 875cfba e7594e0
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Mon Aug 8 12:05:20 2016 +0200

    Merge branch 'item_785_builder_md' into 'master_iteration_7'
    
    item_785 : refector DSL metadata builder parser
    
    1. Delete metadata-builder and metadata-parser
    2. Refactor
    
    See merge request !426

[33mcommit e7594e0e28c61876c257492a0310e549a6eb6664[m
Author: Hoan Vu <hoan.vu.ext@culture.gouv.fr>
Date:   Wed Aug 3 16:32:34 2016 +0200

    Refactor DSL :  builder parser metadata

[33mcommit 875cfba368858533506795d0a3ab0cab5ad28828[m
Author: Olivier MARSOL <olivier.marsol.ext@culture.gouv.fr>
Date:   Fri Aug 5 12:01:49 2016 +0200

    access deployment was missing

[33mcommit 8e2e43193acbf97d7d1fb5abd20372e50555d1bb[m
Merge: a700389 81354c5
Author: Olivier Marsol <olivier.marsol.ext@culture.gouv.fr>
Date:   Thu Aug 4 18:35:46 2016 +0200

    Merge branch 'rpm-pom' into 'master_iteration_7'
    
    added conf for RPM building on logbook & storage-engine & ingest-external + adde…
    
    …d some new params on all components (ystem user & group)
    
    See merge request !428

[33mcommit 81354c5f00896cad9642a3d57289fe703c2dee18[m
Author: Olivier MARSOL <olivier.marsol.ext@culture.gouv.fr>
Date:   Thu Aug 4 15:35:43 2016 +0200

    added conf for RPM building on logbook & storage-engine & ingest-external + added some new params on all components (ystem user & group)

[33mcommit a700389609a71814222c965f1af5b754577b0f4a[m
Merge: 9e8f12c c801bd0
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Thu Aug 4 15:34:16 2016 +0200

    Merge branch 'item-technical-03' into 'master_iteration_7'
    
    Refactor DSL for logbook
    
    Refactor DSL for logbook
    
    See merge request !423

[33mcommit 9e8f12ce2f6730d0dcb3879111d9fd9acb6bd581[m
Author: jgonthier <joachim.gonthier.ext@culture.gouv.fr>
Date:   Thu Aug 4 15:20:06 2016 +0200

    set versions to 0.7.0-SNAPSHOT

[33mcommit c801bd06a467d5b764fd4f572e5aa5aee64a8755[m
Author: NGO BA TUAN <ba-tuan.ngo.ext@culture.gouv.fr>
Date:   Thu Aug 4 14:15:28 2016 +0200

    Refactor DSL logbook and ihm-demo

[33mcommit a051862774fb03aae085f51e6f4c704f5d385e66[m
Merge: bec9129 0610243
Author: Joachim Gonthier <joachim.gonthier.ext@culture.gouv>
Date:   Thu Aug 4 13:19:58 2016 +0200

    Merge branch 'master_iteration_6' into 'master'
    
    Master iteration 6
    
    Merge branch 'master_iteration_6' into 'master'
    
    See merge request !422

[33mcommit 0610243c081fa1863bf97cd9a95a89410ab6c476[m
Merge: a814a1f 16d434d
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Thu Aug 4 13:02:41 2016 +0200

    Merge branch 'item_javadoc' into 'master_iteration_6'
    
    Item javadoc
    
    
    
    See merge request !418

[33mcommit a814a1f31d71d1ea65b8755d81aafa3cf315464d[m
Merge: a4229d1 c39745c
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Thu Aug 4 12:37:26 2016 +0200

    Merge branch 'item_520_fix' into 'master_iteration_6'
    
    item_520 : update lifecycle during archive unit modification
    
    
    
    See merge request !419

[33mcommit c39745cddd43980d9199d2ad008a585b61d72a45[m
Author: Hela Amri <hela.amri.ext@culture.gouv.fr>
Date:   Thu Aug 4 11:59:52 2016 +0200

    item_520_fix : remove FIXME comment + set objectIdentifier in logbook operation + replace FATAL by KO in extractSedaActionHandler

[33mcommit 16d434d38b726a7657f9b2b6432f1d4ed2811c69[m
Author: CHEN Zhang <zchen@linagora.com>
Date:   Wed Aug 3 17:26:10 2016 +0200

    javadoc common-database-private

[33mcommit a4229d1c5315023e164c298562a6b421027ca4ac[m
Merge: f7f016e 4df5258
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Wed Aug 3 17:25:25 2016 +0200

    Merge branch 'item_641_bis' into 'master_iteration_6'
    
    item_641 : add logbook for import & delete pronom format
    
    item_641 : add logbook for import & delete pronom format
    
    See merge request !411

[33mcommit f7f016efde2e9c9bf9dd34ef264202f7d14f0264[m
Merge: b4bf03f e63678d
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Wed Aug 3 17:22:47 2016 +0200

    Merge branch 'storage_exploit_doc' into 'master_iteration_6'
    
    Storage exploitation documentation
    
    Documentations d'exploitation sur :
    - le moteur de stockage
    - l'offre de stockage
    - le processing (màj)
    
    See merge request !416

[33mcommit e63678da8e90d199fe239b157d493af50c601f27[m
Author: gafou <gaelle.fournier@smile.fr>
Date:   Wed Aug 3 16:56:26 2016 +0200

    Storage exploitation documentation

[33mcommit 4df525870172d70472bf4901d99995b97b93d268[m
Author: Hoan Vu <hoan.vu.ext@culture.gouv.fr>
Date:   Mon Aug 1 12:57:11 2016 +0200

    item_641 : add logbook for import & delete pronom format

[33mcommit b4bf03fb9332f6a9d93bcd398a2362c784bf7e40[m
Merge: efbc55c fa2762a
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Wed Aug 3 16:36:53 2016 +0200

    Merge branch 'linagora_javadoc' into 'master_iteration_6'
    
    Fix javadoc common-public and functional admin
    
    
    
    See merge request !414

[33mcommit efbc55ce4c02ef02eaeda96dd1f014844a6d75a3[m
Merge: 0d9a874 d5e1071
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Wed Aug 3 16:23:59 2016 +0200

    Merge branch 'master_iteration_6_storage_fix' into 'master_iteration_6'
    
    Master Iteration 6 : Storage fixes
    
    Fixes :
    - dépendances cycliques maven
    - NPE sur SedaUtils
    - Log exception sur SedaUtils
    
    See merge request !415

[33mcommit fa2762a8f8877c7a43eb9660a56e224f214a2469[m
Author: NGO BA TUAN <ba-tuan.ngo.ext@culture.gouv.fr>
Date:   Wed Aug 3 16:05:57 2016 +0200

    Fix javadoc common-public and functional admin

[33mcommit d5e107113f4ee237302bf1a357854c64072b5e21[m
Author: Cedric Legrand <cedric.legrand.ext@culture.gouv.fr>
Date:   Wed Aug 3 15:09:56 2016 +0200

    fix : dependencies, some qualities

[33mcommit 0d9a8746fc3783a90aef7370ae5eb01896296820[m
Author: jgonthier <joachim.gonthier.ext@culture.gouv.fr>
Date:   Wed Aug 3 10:26:26 2016 +0200

    ingest-internal-api (pom.xml) : remove the white line 1

[33mcommit bec9129fc1109479525361062a9cd6687c0a7e79[m
Merge: 029c74b b92d2fd
Author: Joachim Gonthier <joachim.gonthier.ext@culture.gouv>
Date:   Tue Aug 2 15:55:06 2016 +0200

    Merge branch 'master_iteration_6' into 'master'
    
    Master iteration 6
    
    Merge branch 'master_iteration_5' into 'master'
    
    See merge request !412

[33mcommit e60a4aff80de937090bf4171122d17d6cb829a76[m
Author: CHEN Zhang <zchen@linagora.com>
Date:   Tue Aug 2 15:26:14 2016 +0200

    javadoc ihm-demo

[33mcommit b92d2fdebe1e90c9e1b3aebcf4483ecdeb6dc26c[m
Merge: 2c646a6 6f0a069
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Tue Aug 2 15:21:40 2016 +0200

    Merge branch 'user_manuals' into 'master_iteration_6'
    
    #491 ajout de la documentation sur le référentiel des formats.
    
    
    
    See merge request !375

[33mcommit 2c646a6eac8d964150e43a26d9967899310c09ea[m
Merge: 9ece7a4 e4bfc02
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Tue Aug 2 15:20:56 2016 +0200

    Merge branch 'item_510_80_Documentation' into 'master_iteration_6'
    
    Graph Documentation et ihm demo Doc updated
    
    
    
    See merge request !403

[33mcommit 9ece7a4718bef3362cf221aad180b911c5bcbbde[m
Merge: e9e3cd5 63c2eb6
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Tue Aug 2 12:10:48 2016 +0200

    Merge branch 'integration_iteration_6' into 'master_iteration_6'
    
    Deployment for iteration 6
    
    Deployment for iteration 6 ; only docker deployment is running now.
    
    See merge request !407

[33mcommit e9e3cd579c1e64de842236617251ab54fc937676[m
Merge: e677dd6 2199a19
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Tue Aug 2 09:08:37 2016 +0200

    Merge branch 'item_storage_javadoc' into 'master_iteration_6'
    
    Fixed javadoc
    
    Fixed javadoc
    
    See merge request !410

[33mcommit e4bfc021a45f79b11e7bded00036b8532841b70d[m
Author: Haykel <haykel.benmassaoud.ext@culture.gouv.fr>
Date:   Mon Aug 1 14:46:06 2016 +0200

    Graph Documentation

[33mcommit 2199a19ed40a4ecdff1d892eaded4780d51ee042[m
Author: germain ledroit <germain.ledroit.ext@culture.gouv.fr>
Date:   Mon Aug 1 18:15:11 2016 +0200

    Fixed javadoc

[33mcommit e677dd66f36b78e77b7aa07c7da192410f00d0b2[m
Merge: d053390 375ac60
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Mon Aug 1 17:57:12 2016 +0200

    Merge branch 'fix_item_510' into 'master_iteration_6'
    
    item_510 integration
    
    
    
    See merge request !409

[33mcommit d05339042f733eb5f92ea08be16a6123b10586d2[m
Merge: 3687037 f6247ae
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Mon Aug 1 17:50:22 2016 +0200

    Merge branch 'item_ihm_modif' into 'master_iteration_6'
    
    modifier ihm
    
    
    
    See merge request !408

[33mcommit f6247aeccc680b865dd385e7293681e78cdd2a70[m
Author: CHEN Zhang <zchen@linagora.com>
Date:   Mon Aug 1 17:46:25 2016 +0200

    modifier ihm

[33mcommit 3687037ffb9df7838e726fe7e9f9fc6485fb9b31[m
Merge: e4a3742 34f2e1f
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Mon Aug 1 17:45:39 2016 +0200

    Merge branch 'item_doc_storage' into 'master_iteration_6'
    
    Integration bugfixes + documentation
    
    Integration bugfixes + documentation
    
    See merge request !406

[33mcommit 63c2eb6646b6446ebd55d71e1ca046c408b60fc6[m
Author: Olivier MARSOL <olivier.marsol.ext@culture.gouv.fr>
Date:   Mon Jul 25 19:04:47 2016 +0200

    Deployment for iteration 6

[33mcommit e4a37428a784cb8d79fb323415d32af165b09b46[m
Merge: 3796043 de07893
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Mon Aug 1 17:40:03 2016 +0200

    Merge branch 'DEX' into 'master_iteration_6'
    
    DIN & DEX doc init
    
    DIN & DEX doc init
    
    See merge request !405

[33mcommit de0789364678ab95058c24d6900c09f8f074f047[m
Author: Olivier MARSOL <olivier.marsol.ext@culture.gouv.fr>
Date:   Fri Jul 22 17:01:04 2016 +0200

    DIN & DEX doc init

[33mcommit 34f2e1fc6a58d6d73a3c21e9e991715ea46cdbdd[m
Author: germain ledroit <germain.ledroit.ext@culture.gouv.fr>
Date:   Mon Aug 1 17:09:20 2016 +0200

    Integration bugfixes + documentation

[33mcommit 375ac6086b91917d7e743474eb8e0619beef1154[m
Author: Ramzi LAZREG <ramzy.lazreg.ext@culture.gouv.fr>
Date:   Mon Aug 1 16:51:29 2016 +0200

    item_510 integration

[33mcommit 379604349c74a5271f6232ad3350ad55c4d27999[m
Merge: 3b08013 64bc9fa
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Mon Aug 1 16:13:02 2016 +0200

    Merge branch 'item_345_refacto' into 'master_iteration_6'
    
    Fix logbook entry modal
    
    
    
    See merge request !404

[33mcommit 64bc9faa4cb3d4ba68229c15540bcb9f62335119[m
Author: NGO BA TUAN <ba-tuan.ngo.ext@culture.gouv.fr>
Date:   Mon Aug 1 16:07:14 2016 +0200

    Fix logbook entry modal

[33mcommit 3b08013589f0146d05b359ac161a7cab126e57ed[m
Merge: f1b5593 ae7c4ed
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Mon Aug 1 15:07:40 2016 +0200

    Merge branch 'item_739' into 'master_iteration_6'
    
    Item #739 / Item #618 : Worker and Storage Engine client / GOT storage lifecycle
    
    Item #739 : Storage Engine Client
    
    Common private :
    * Added common client classes to common-private from api-design project
    * Added common client configuration to common-private from api-design project
    * Added common server status class StatusMessage to common-private from api-design project
    
    Item #739 : Worker
    - added StoreObjectGroup handler
    - update SedaUtils with retrieval of OG and GOT
    
    Item #618 : GOT storage lifecycle
    - update of OG lifecycle with StoreObjectGroup action
    - update of OG lifecycle with StoreObjectGroup's binary data objet storage
    
    See merge request !399

[33mcommit ae7c4ed43146330c5afefedc253ff9c46444e369[m
Author: gafou <gaelle.fournier@smile.fr>
Date:   Thu Jul 21 17:41:46 2016 +0200

    Item #739 / Item #618 : Worker and Storage Engine client / GOT storage lifecycle
    
    Item #739 : Storage Engine Client
    
    Common private :
    * Added common client classes to common-private from api-design project
    * Added common client configuration to common-private from api-design project
    * Added common server status class StatusMessage to common-private from api-design project
    
    Item #739 : Worker
    - added StoreObjectGroup handler
    - update SedaUtils with retrieval of OG and GOT
    
    Item #618 : GOT storage lifecycle
    - update of OG lifecycle with StoreObjectGroup action
    - update of OG lifecycle with StoreObjectGroup's binary data objet storage

[33mcommit f1b5593a0a9bc04ef9109a231b4c39f1bb4f24af[m
Merge: cb92120 dcb697a
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Mon Aug 1 13:57:32 2016 +0200

    Merge branch 'item_510_release_fv' into 'master_iteration_6'
    
    item_510 index with level stack
    
    
    
    See merge request !395

[33mcommit dcb697a0b70f396a98067de04498a813ddd393df[m
Author: Ramzi LAZREG <ramzy.lazreg.ext@culture.gouv.fr>
Date:   Fri Jul 29 13:30:11 2016 +0200

    item_510 index with level stack
    
    add _up field
    
    fix comment

[33mcommit cb921201d8c579f7525a4082d1e3148141a9e760[m
Merge: b1dca1e 8a972a4
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Mon Aug 1 13:12:26 2016 +0200

    Merge branch 'item_345_refacto' into 'master_iteration_6'
    
    Ihm-demo and functional-admin documentation + Fix format client API name
    
    
    
    See merge request !401

[33mcommit 8a972a4f353cf03c8aa63dec2dfbc7ddbf6e7756[m
Author: NGO BA TUAN <ba-tuan.ngo.ext@culture.gouv.fr>
Date:   Mon Aug 1 12:13:57 2016 +0200

    Ihm-demo and functional-admin documentation + Fix format client API name

[33mcommit b1dca1ef7ec8bb946bbe92f7930ef6389b8087f1[m
Merge: 256f35d f49d999
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Mon Aug 1 10:46:16 2016 +0200

    Merge branch 'item_513' into 'master_iteration_6'
    
    story_80 [functional corrections]
    
    Corrections :
    - Enable modification on complex fields (except _mt fields)
    
    See merge request !400

[33mcommit 256f35d8f16cca1fd809053a36db98cf83b4457f[m
Merge: b83b348 55e394b
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Mon Aug 1 10:45:27 2016 +0200

    Merge branch 'item_345_refacto' into 'master_iteration_6'
    
    Fix la PIC
    
    _ Fix logbook order
    _ Fix pronom import
    
    See merge request !398

[33mcommit 55e394bbcdea029e419ed163c740ddf43d5b79b3[m
Author: NGO BA TUAN <ba-tuan.ngo.ext@culture.gouv.fr>
Date:   Mon Aug 1 10:02:40 2016 +0200

    Fix la PIC

[33mcommit 6f0a0696668daa994e7798ca72752b61ecc73189[m
Author: Thomas Morsellino <thomas.morsellino.ext@culture.gouv.fr>
Date:   Thu Jul 28 16:48:34 2016 +0200

    ajout de la documentation sur le référentiel des formats (US 491)

[33mcommit defa431ea73942742c27345b57e639fe82bf3e0c[m
Author: NGO BA TUAN <ba-tuan.ngo.ext@culture.gouv.fr>
Date:   Fri Jul 29 18:29:07 2016 +0200

    Fix logbook operation order

[33mcommit f49d999ac14420b78a67fdccdfb036764927446b[m
Author: hela.amri <hela.amri.ext@culture.gouv.fr>
Date:   Sun Jul 31 00:40:31 2016 +0200

    story_80 [functional tests]: enable modification on complex fields
    (except _mgt fields)

[33mcommit b83b348b26fddcd9090a23c47e704a3ba1167215[m
Merge: 16bb488 d02ab76
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Fri Jul 29 19:00:17 2016 +0200

    Merge branch 'item_617' into 'master_iteration_6'
    
    ITEM #617 : Offre de stockage
    
    - Service offre de stockage (intégration du workspace, le chunk est fait coté service, il pourrait être fait côté ressource via Jetty, de plus il est très simplifié (pas de gestion d'erreur ou presque))
    - Implémentation du driver de l'offre
    - Implémentation des ressources de l'offre
    - Configuration docker
    - Test d'intégration driver -> offre
    
    See merge request !396

[33mcommit d02ab76ff08928d5c572b9204ff1181f9a7a85a7[m
Author: Cedric Legrand <cedric.legrand.ext@culture.gouv.fr>
Date:   Mon Jul 25 15:58:12 2016 +0200

    ITEM #617 : Offre de stockage
    
    - Service offre de stockage (intégration du workspace, le chunk est fait coté service, il pourrait être fait côté ressource via Jetty, de plus il est très simplifié (pas de gestion d'erreur ou presque))
    - Implémentation du driver de l'offre
    - Implémentation des ressources de l'offre
    - Configuration docker
    - Test d'intégration driver -> offre

[33mcommit 16bb48811b687320191c7fe3fd91d837d14ba976[m
Merge: 2773ed7 34d9fe0
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Fri Jul 29 18:05:30 2016 +0200

    Merge branch 'item_548' into 'master_iteration_6'
    
    Item #548 - Logbook Mock for Storage
    
    - Interface StorageLogbook
    - Implementation of the Mock
    - Factory (for the moment only the mock can be returned)
    - StorageLogbookParameters to determine the mandatory fields
    - Tests
    - Integration of the storage logbook in the StorageDistributionImpl
    
    See merge request !391

[33mcommit 34d9fe0d80b81cf710a834ae8fc1f206001d3140[m
Author: germain ledroit <germain.ledroit.ext@culture.gouv.fr>
Date:   Thu Jul 28 17:44:00 2016 +0200

    Item #548 - Logbook Mock for Storage
    
    - Interface StorageLogbook
    - Implementation of the Mock
    - Factory (for the moment only the mock can be returned)
    - StorageLogbookParameters to determine the mandatory fields
    - Tests
    - Integration of the log in the StorageDistributionImpl

[33mcommit 2773ed75fb2d6795211468baf5416bf45d039864[m
Merge: 33a7cfb c836c51
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Fri Jul 29 17:25:39 2016 +0200

    Merge branch 'item_636' into 'master_iteration_6'
    
    item 636: ihm-demo -- import format
    
    
    
    See merge request !388

[33mcommit c836c519aeff1f8c83205c9e5759c52df36b3747[m
Author: CHEN Zhang <zchen@linagora.com>
Date:   Thu Jul 28 15:31:58 2016 +0200

    item 636: ihm-demo -- import format

[33mcommit 33a7cfbdd805112a881ba732de8368a6cf0efdbd[m
Merge: cbfb5d1 47716c8
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Fri Jul 29 17:22:06 2016 +0200

    Merge branch 'item_547_integration' into 'master_iteration_6'
    
    Item 547 integration
    
    Pom docker build + docker files (setenv.sh / entry.sh)
    
    See merge request !390

[33mcommit cbfb5d1dda29601a75655856dc5bb88d1562f643[m
Merge: ef430c4 cec70f4
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Fri Jul 29 17:08:26 2016 +0200

    Merge branch 'item_345_refacto' into 'master_iteration_6'
    
    Item_83 Fix DSL tests
    
    
    
    See merge request !389

[33mcommit cec70f41446755e1b7a735c7364280091670e867[m
Author: NGO BA TUAN <ba-tuan.ngo.ext@culture.gouv.fr>
Date:   Fri Jul 29 16:52:40 2016 +0200

    Item_83 Fix DSL tests

[33mcommit ef430c4723114bbd1f4193a31f4107595b390f89[m
Merge: 1deb882 59418c6
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Fri Jul 29 16:44:38 2016 +0200

    Merge branch 'item_345_refacto' into 'master_iteration_6'
    
    Item-83 Search formats and display format detail
    
    
    
    See merge request !386

[33mcommit 47716c87580b0cdbc36138c7afb1124b202fad4c[m
Author: gafou <gaelle.fournier@smile.fr>
Date:   Fri Jul 29 16:37:03 2016 +0200

    Item #547 docker integration configuration

[33mcommit 59418c6ec225bb06c48b5c479e31ede516987730[m
Author: NGO BA TUAN <ba-tuan.ngo.ext@culture.gouv.fr>
Date:   Fri Jul 29 09:27:48 2016 +0200

    Item-83 Search formats and display format detail

[33mcommit 1deb882626954419ab49698419f67c6ff1c999d6[m
Merge: e84432d b7573b3
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Fri Jul 29 15:20:36 2016 +0200

    Merge branch 'fix_ingest' into 'master_iteration_6'
    
    Fix uploader
    
    
    
    See merge request !385

[33mcommit e84432d6068f2f13ef9c31080687653bf5f74aa3[m
Merge: 96c0b35 a45fd73
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Fri Jul 29 15:18:53 2016 +0200

    Merge branch 'item_513' into 'master_iteration_6'
    
    Item 513 : Update Archive unit
    
    
    
    See merge request !384

[33mcommit a45fd737e99ad6deb5c694fd12c9d982dd6fbea1[m
Author: Hela Amri <hela.amri.ext@culture.gouv.fr>
Date:   Fri Jul 29 15:10:48 2016 +0200

    item_513: back part

[33mcommit 565754853737e30ba2add74e230452223e7d819c[m
Author: Hela Amri <hela.amri.ext@culture.gouv.fr>
Date:   Fri Jul 29 15:10:31 2016 +0200

    item_513: Front part

[33mcommit 96c0b35c3f729f027b052fe47a6b490749fa2a4b[m
Merge: 8d0ae2f 8a418ef
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Fri Jul 29 15:03:44 2016 +0200

    Merge branch 'item_560' into 'master_iteration_6'
    
    item_562 rest & client referential file format
    
    item_562 rest & client referential file format
    
    See merge request !374

[33mcommit b7573b3bbbe74b7209a26d4e9498e7d2a402915e[m
Author: NGO BA TUAN <ba-tuan.ngo.ext@culture.gouv.fr>
Date:   Fri Jul 29 14:37:55 2016 +0200

    Fix uploader

[33mcommit 8a418efc5c86fcc10a9d35e5245290f9c7a6dcb7[m
Author: Hoan Vu <hoan.vu.ext@culture.gouv.fr>
Date:   Wed Jul 27 10:34:50 2016 +0200

    item_562 : rest and client

[33mcommit 8d0ae2f1c7e12c8cb2e43963542e1ad1b28d3efc[m
Merge: d64333d 08750f5
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Fri Jul 29 13:39:57 2016 +0200

    Merge branch 'item_547' into 'master_iteration_6'
    
    Item #547 : Storage engine
    
    - Server part + REST resources
    - DriverManager implementation (SPI)
    - Distribution Service implementation : implemented File system strategy and offer referential provider
    - Implemented storeData for objects/units/objectgroup/logbook
    - implemented storeData with retry and multiple execution result concatenation
    
    See merge request !378

[33mcommit 08750f53a32e9f7e46c7ab4cb28876e911e35178[m
Author: germain ledroit <germain.ledroit.ext@culture.gouv.fr>
Date:   Fri Jul 22 18:49:36 2016 +0200

    Item #547 : Storage engine
    - Server part + REST resources
    - DriverManager implementation (SPI)
    - Distribution Service implementation : implemented File system strategy and offer referential provider
    - Implemented storeData for objects/units/objectgroup/logbook
    - implemented storeData with retry and multiple execution result concatenation

[33mcommit d64333df4c463fc4bc73c100873c8021f1c39d52[m
Merge: 50751d4 d254bf1
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Fri Jul 29 12:20:28 2016 +0200

    Merge branch 'fix_ingest' into 'master_iteration_6'
    
    fix ingest internal
    
    
    
    See merge request !382

[33mcommit d254bf168fa7715fd3585f5b8cf9d7b2768e063c[m
Author: Yousri KOUKI <yousri.kouki.ext@culture.gouv.fr>
Date:   Fri Jul 29 11:14:55 2016 +0200

    fix ingest internal

[33mcommit 50751d4c3352f9449170d7cd30a9a25d1c26c2ec[m
Merge: aba4534 41c624c
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Thu Jul 28 16:03:46 2016 +0200

    Merge branch 'item_345_refacto' into 'master_iteration_6'
    
    Item 345 Upload sip REST API
    
    
    
    See merge request !372

[33mcommit 41c624cd7e97a8e0c398acd66ade04470ecdd3c9[m
Author: NGO BA TUAN <ba-tuan.ngo.ext@culture.gouv.fr>
Date:   Thu Jul 28 10:25:20 2016 +0200

    Item_345 Add upload sip REST API

[33mcommit 2b41225edc37025222788b79594d7132b684f3b1[m
Author: NGO BA TUAN <ba-tuan.ngo.ext@culture.gouv.fr>
Date:   Thu Jul 28 10:24:49 2016 +0200

    Item_345 Fix Ingest external/internal

[33mcommit aba4534c5a1423f148c4873b182cea11dc00417a[m
Merge: 3ba1788 1f001a0
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Wed Jul 27 18:22:08 2016 +0200

    Merge branch 'item_345_refacto' into 'master_iteration_6'
    
    Item 345 refacto
    
    Item 345 refacto (ingest-internal-rest, ingest-internal-client)
    Integration Ingest Internal/External (ingest-external-core)
    
    See merge request !364

[33mcommit 3ba178885354b5a7d541ac16745a0f256c40af5d[m
Merge: 185f0a9 87b2254
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Wed Jul 27 16:54:08 2016 +0200

    Merge branch 'item_80_ihm_rest' into 'master_iteration_6'
    
    ihm rest implementation
    
    include update features
    
    See merge request !369

[33mcommit 185f0a9d623f3977c3eda1d2e656e5bb96e29df2[m
Merge: b7d40a4 ec614dc
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Wed Jul 27 16:09:28 2016 +0200

    Merge branch 'item_560' into 'master_iteration_6'
    
    Item 562: Add tests and javadoc in functional admin module
    
    
    
    See merge request !368

[33mcommit 1f001a04b2ca7413f1f75e72a25030f7ec6ee27d[m
Author: Yousri KOUKI <yousri.kouki.ext@culture.gouv.fr>
Date:   Wed Jul 27 09:23:36 2016 +0200

    Ingest Internal/External

[33mcommit 87b22549f7fe4a9f2618b9c3719a491c2d02a9ef[m
Author: Haykel <haykel.benmassaoud.ext@culture.gouv.fr>
Date:   Wed Jul 27 15:58:48 2016 +0200

    ihm rest implementation

[33mcommit f461ca28e986455ba95775154d7348540738edec[m
Author: Yousri KOUKI <yousri.kouki.ext@culture.gouv.fr>
Date:   Fri Jul 22 18:12:53 2016 +0200

    Refacto Ingest

[33mcommit ec614dc8affde2e2b53491fc1e933c30fc733da4[m
Author: NGO BA TUAN <ba-tuan.ngo.ext@culture.gouv.fr>
Date:   Wed Jul 27 15:20:06 2016 +0200

    Add tests and javadoc

[33mcommit b7d40a43a9db12b6cb269c28cec137fd564b75dd[m
Merge: 99b9904 f85eb58
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Wed Jul 27 11:19:17 2016 +0200

    Merge branch 'item_560' into 'master_iteration_6'
    
    Item 562 Core : file format check & import
    
    - Fix common DSL
    - File format Core : file format check & import
    
    See merge request !361

[33mcommit 99b9904a45086344a902f9a13ba087da5fa24b7c[m
Merge: d013b68 35f97c7
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Wed Jul 27 11:16:18 2016 +0200

    Merge branch 'item_80_ihm_rest' into 'master_iteration_6'
    
    Ihm rest Implementation + Test Coverage
    
    Add update features to ihm Rest
    
    See merge request !363

[33mcommit f85eb5893540a486afcbb10b59b60e85fa08bee2[m
Author: NGO BA TUAN <ba-tuan.ngo.ext@culture.gouv.fr>
Date:   Tue Jul 26 12:49:57 2016 +0200

    Item_562 Fix common DSL

[33mcommit 06cbb57ce9c0bf287ed6f682c387c5ad457fa2e3[m
Author: NGO BA TUAN <ba-tuan.ngo.ext@culture.gouv.fr>
Date:   Tue Jul 26 12:49:29 2016 +0200

    Item_562 Core : file format check & import

[33mcommit 35f97c719b815cb4441269c1cde132639b12b1de[m
Author: Haykel <haykel.benmassaoud.ext@culture.gouv.fr>
Date:   Wed Jul 27 10:22:10 2016 +0200

    ihm rest implementation

[33mcommit d013b68f1cab21e7bd9359d1c259e4cf4964f89c[m
Merge: d7d23df dd9ea16
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Tue Jul 26 14:07:16 2016 +0200

    Merge branch 'item_345_complete' into 'master_iteration_6'
    
    Item 345 complete
    
    
    
    See merge request !350

[33mcommit dd9ea16724cc2db203e139202400944c0202605f[m
Author: NGO BA TUAN <ba-tuan.ngo.ext@culture.gouv.fr>
Date:   Thu Jul 7 10:44:44 2016 +0200

    Item_345 Controle sanitaire

[33mcommit d7d23df2a57bd10f038a83925cf3200671cf19ae[m
Merge: d2f1f51 bc096b7
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Mon Jul 25 16:17:23 2016 +0200

    Merge branch 'item_761' into 'master_iteration_6'
    
    Item 761 : recursive parsing + create archive unit tree
    
    
    
    See merge request !351

[33mcommit d2f1f511a6948402bbc5a67a22d046f8fbecde7a[m
Merge: 36e4bdd ee689ed
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Mon Jul 25 16:12:26 2016 +0200

    Merge branch 'item-technical-03' into 'master_iteration_6'
    
    Item technical 03
    
    
    ## Refactor request:
    - common/common-database-vitam/common-database-public/src/main/java/fr/gouv/vitam/common/database/builder/request/configuration/BuilderToken.java
    - common/common-database-vitam/common-database-private/src/main/java/fr/gouv/vitam/common/database/parser/query/ParserTokens.java
    - common/common-database-vitam/common-database-public/src/main/java/fr/gouv/vitam/common/database/builder/request/multiple/RequestMultiple.java
    - common/common-database-vitam/common-database-public/src/main/java/fr/gouv/vitam/common/database/builder/request/multiple/VitamFieldsHelper.java
    - common/common-database-vitam/common-database-public/src/main/java/fr/gouv/vitam/common/database/builder/request/single/RequestSingle.java
    - common/common-database-vitam/common-database-public/src/main/java/fr/gouv/vitam/common/database/builder/request/AbstractRequest.java
    
    ## Refactor request parser:
    - common/common-database-vitam/common-database-private/src/main/java/fr/gouv/vitam/common/database/parser/request/AbstractParser.java
    - common/common-database-vitam/common-database-private/src/main/java/fr/gouv/vitam/common/database/parser/request/GlobalDatasParser.java
    - common/common-database-vitam/common-database-private/src/main/java/fr/gouv/vitam/common/database/parser/request/single/RequestParserSingle.java
    - common/common-database-vitam/common-database-private/src/main/java/fr/gouv/vitam/common/database/parser/request/multiple/RequestParserHelper.java
    - common/common-database-vitam/common-database-private/src/main/java/fr/gouv/vitam/common/database/parser/request/multiple/RequestParserMultiple.java
    
    ## Refactor collections:
    - common/common-database-vitam/common-database-private/src/main/java/fr/gouv/vitam/common/database/collections/VitamCollection.java
    - common/common-database-vitam/common-database-private/src/main/java/fr/gouv/vitam/common/database/collections/VitamCollectionHelper.java
    
    See merge request !342

[33mcommit bc096b7de86d9231e1bcf03da59a6f7839a123b1[m
Author: Hela Amri <hela.amri.ext@culture.gouv.fr>
Date:   Mon Jul 25 15:05:45 2016 +0200

    Item 761 : recursive parsing + create archive unit tree

[33mcommit 36e4bdd15115b09c871040868292da250056bf3d[m
Merge: 156009a eca78f7
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Mon Jul 25 12:32:23 2016 +0200

    Merge branch 'item_520_release' into 'master_iteration_6'
    
    item_520 update archive unit with log in logbook lifecycle and operation
    
    item_520 update archive unit with log in logbook lifecycle and operation
    
    
    See merge request !356

[33mcommit ee689edd4d642f1278d3a887af0e9d13f9328c58[m
Author: NGO BA TUAN <ba-tuan.ngo.ext@culture.gouv.fr>
Date:   Thu Jul 21 18:04:43 2016 +0200

    Refactor DSL: Query, BuilderToken, Parser...

[33mcommit 156009ae9e05dd1b04d37cec188010b6a4832837[m
Merge: 32a9fee 3a6b31b
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Mon Jul 25 11:04:07 2016 +0200

    Merge branch 'fix_738_missingdir_3' into 'master_iteration_6'
    
    fixed jenkins problem related to build docker
    
    
    
    See merge request !354

[33mcommit 32a9feeedc56ad606b72cdc1178f7c75a046f535[m
Merge: e320685 fa4c6f3
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Mon Jul 25 11:03:12 2016 +0200

    Merge branch 'fix_sedaUtils' into 'master_iteration_6'
    
    fix extract SEDA
    
    
    
    See merge request !353

[33mcommit e32068593745e5c521d8e803f829c82bb16430d7[m
Merge: db7d968 093c17c
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Mon Jul 25 11:00:00 2016 +0200

    Merge branch 'item_560' into 'master_iteration_6'
    
    item_560_561 : Référentiel format fichier
    
    
    
    See merge request !343

[33mcommit 3a6b31b4353419ca191482e3371f4b36c7cde93f[m
Author: Olivier MARSOL <olivier.marsol.ext@culture.gouv.fr>
Date:   Mon Jul 25 10:59:32 2016 +0200

    fixed jenkins problem related to build docker

[33mcommit eca78f7dd37a32325bd902f9f48cff4309611452[m
Author: Buon SUI <bsui@cdh-it.com>
Date:   Wed Jul 20 16:06:54 2016 +0200

    item_520 update archive unit with log in logbook lifecycle and operation
    
    item_520 update archive unit with log in logbook lifecycle and operation
    
    update comment in access
    
    item_520 call logbook operation and lifecycles when update archive unit
    
    update call befor and after logbook, variabilize tenanId
    
    update call before and after logbook
    
    add commit ligecycle and refactor rollback when exception thrown
    
    contrstructor comment and add fixme in logbook
    
    update comment when rollback
    
    fix comment of MR348
    
    fix test metadata config null

[33mcommit 093c17c88345e0daa073c20485e42ab6db30fe5f[m
Author: Hoan Vu <hoan.vu.ext@culture.gouv.fr>
Date:   Tue Jul 19 18:50:22 2016 +0200

    item_560_561 : Référentiel format fichier

[33mcommit fa4c6f3a84b2d02266bd1a97812241a25fde5856[m
Author: Yousri KOUKI <yousri.kouki.ext@culture.gouv.fr>
Date:   Wed Jul 20 17:43:34 2016 +0200

    fix extract SEDA

[33mcommit db7d9683f6f8a8f01c755eff5af53283248d026a[m
Merge: 8f40548 6ee4b89
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Fri Jul 22 13:44:53 2016 +0200

    Merge branch 'fix_storage_738_javadoc' into 'master_iteration_6'
    
    Fixed javadoc for storage - Item #738
    
    Fixed javadoc for storage part.
    
    See merge request !344

[33mcommit 6ee4b896e8c3d6a2ae86795a10290943eb1e2755[m
Author: germain ledroit <germain.ledroit.ext@culture.gouv.fr>
Date:   Fri Jul 22 09:14:20 2016 +0200

    Fixed javadoc for storage - Item #738

[33mcommit 8f40548cc000a5449d9cfee5d6f59dc7881196f2[m
Merge: c7d548b 6e36e34
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Thu Jul 21 18:47:35 2016 +0200

    Merge branch 'item_738' into 'master_iteration_6'
    
    Item #738 : Global conception + first work on interfaces/mocks
    
    Maven modularization:
    - Modularization of module Storage
    - Documentation
    
    General Technical Design:
    - schema
    - workflow
    
    Conception of Storage DriverManager
    Conception of Storage Driver API
    Workspace offer resource mock
    
    Conception + engine Mock + engine Rest :
    - Creation of StorageException classes
    - Rest Storage Resource + Application + Configuration + Tests (returning not implementing for the moment)
    - Interface StorageDistribution declaring methods to be implemented
    - Documentation : rst, for the Rest explanation
    
    See merge request !334

[33mcommit 6e36e3421fde9ff98ed78e30a9d31021e3fa4b06[m
Author: germain ledroit <germain.ledroit.ext@culture.gouv.fr>
Date:   Mon Jul 18 18:33:41 2016 +0200

    Item #738 : Global conception + first work on interfaces/mocks
    
    Maven modularization:
    - Modularization of module Storage
    - Documentation
    
    General Technical Design:
    - schema
    - workflow
    
    Conception of Storage DriverManager
    Conception of Storage Driver API
    Workspace offer resource mock
    
    Conception + engine Mock + engine Rest :
    - Creation of StorageException classes
    - Rest Storage Resource + Application + Configuration + Tests (returning not implementing for the moment)
    - Interface StorageDistribution declaring methods to be implemented
    - Documentation : rst, for the Rest explanation

[33mcommit c7d548bb912854f04f742290ca1f9b28ad8b47a1[m
Merge: 68bd00a 3fc0384
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Thu Jul 21 17:54:15 2016 +0200

    Merge branch 'continuous_deploy' into 'master_iteration_6'
    
    had forgotten automatic deploy forcing version
    
    Sorry, in previous merge request was missing correct hosts.int.deploy file (for continuous deployment under int environment). No forced to download 0.6.0-SNAPSHOT tagged dockers.
    
    See merge request !341

[33mcommit 3fc03847b92ca266af25f809ffbd7958b9a1f853[m
Author: Olivier MARSOL <olivier.marsol.ext@culture.gouv.fr>
Date:   Thu Jul 21 17:47:12 2016 +0200

    had forgotten automatic deploy forcing version

[33mcommit 68bd00a72d4058206464e436321c3dbe382e5a89[m
Merge: 68dda71 48fa065
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Thu Jul 21 17:24:50 2016 +0200

    Merge branch 'continuous_deploy' into 'master_iteration_6'
    
    Continuous deploy
    
    1st attempt for continuous deployment on int environment ; shell launched as sudo vitam-deploy in jenkins + modified inventory (using vitam-deploy instead of ansible) ; fixed a bug related to pulled docker versions. Moved parameter from group_vars to a parameter in each environment (so can be different).
    
    See merge request !337

[33mcommit 68dda7171a6016643c121f207a23597a34296c17[m
Merge: f3a3189 4af9787
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Thu Jul 21 17:21:38 2016 +0200

    Merge branch 'br-integ-update-version' into 'master_iteration_6'
    
    update pom versions from 0.5.0-SNAPSHOT to from 0.6.0-SNAPSHOT
    
    Update needed for development teams
    
    See merge request !335

[33mcommit 48fa0650892766ef2ba196076f9c4cb87039a128[m
Author: Olivier MARSOL <olivier.marsol.ext@culture.gouv.fr>
Date:   Thu Jul 21 16:53:13 2016 +0200

    to prevent a stupid bug related to the release version

[33mcommit 4af9787fdcb404f6e970d8201a5930f8530ea114[m
Author: jgonthier <joachim.gonthier.ext@culture.gouv.fr>
Date:   Thu Jul 21 16:08:48 2016 +0200

    update pom versions from 0.5.0-SNAPSHOT to from 0.6.0-SNAPSHOT

[33mcommit f3a31898c70a51009268158560055ddc37ae6b23[m
Merge: 97ac32c 0001378
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Thu Jul 21 14:38:29 2016 +0200

    Merge branch 'item_70_IHMDemo' into 'master_iteration_6'
    
    Item_70_CreateDSLUpdate + Test Coverage
    
    CreateDSLUpdate + Test Coverage
    
    See merge request !328

[33mcommit e9002fe89b75bcb089ffe8dc261c69381c109679[m
Author: Olivier MARSOL <olivier.marsol.ext@culture.gouv.fr>
Date:   Thu Jul 21 11:53:55 2016 +0200

    1st attempt for continuous deployment ; shell + modified inventory

[33mcommit 00013786aa3d30c1cc8ea831d910add2ecc6dcdf[m
Author: Haykel <haykel.benmassaoud.ext@culture.gouv.fr>
Date:   Wed Jul 20 14:21:53 2016 +0200

    Item_70_CreateDSLUpdate + Test Coverage

[33mcommit 97ac32c82d65aaa8a20bc9fed0961ef5ff3d27ea[m
Merge: ecc2053 47973ef
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Wed Jul 20 13:09:03 2016 +0200

    Merge branch 'item_514_access_release' into 'master_iteration_6'
    
    item_514 access : update archive unit access side
    
    item_514 access : update archive unit access side
    
    See merge request !326

[33mcommit 47973ef183883e3f0821124658c41cd82fb0bdc0[m
Author: Buon SUI <bsui@cdh-it.com>
Date:   Mon Jul 18 09:26:33 2016 +0200

    item_514 access : update archive unit access side
    
    fix access item 514 test
    
    item_514 access : update archive unit access side

[33mcommit ecc20532375a003fd4f9d7d3329f6eae3805efcd[m
Merge: 31f7147 b2e6024
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Tue Jul 19 14:59:17 2016 +0200

    Merge branch 'item_514_Metadata' into 'master_iteration_6'
    
    Item 514 metadata
    
    Merge Requests including Etienne feedback
    
    See merge request !318

[33mcommit b2e6024f8b1e57691dfe0340046b417d9f76b974[m
Author: Haykel <haykel.benmassaoud.ext@culture.gouv.fr>
Date:   Tue Jul 19 14:54:23 2016 +0200

    Item_514_Metadata: add Update features to Metadata Implementation + Test Coverage

[33mcommit 31f714704c43612fdc4c8d4025b54b605196def9[m
Merge: b9476d5 e7993d4
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Tue Jul 19 09:32:43 2016 +0200

    Merge branch 'item_514_Metadata' into 'master_iteration_6'
    
    Item 514 metadata
    
    add update features
    
    See merge request !315

[33mcommit b9476d5bd1ff9459158454361cbed0d2be79bcdd[m
Merge: 029c74b 54111b7
Author: Olivier Marsol <olivier.marsol.ext@culture.gouv.fr>
Date:   Mon Jul 18 18:01:43 2016 +0200

    Merge branch 'rpm-packaging' into 'master_iteration_6'
    
    RPM packaging and deployment
    
    1er jet du packaging et déploiement RPM
    
    See merge request !311

[33mcommit e7993d4a468af19564b65b79019202aa0a1df6cf[m
Author: Haykel <haykel.benmassaoud.ext@culture.gouv.fr>
Date:   Fri Jul 15 11:29:18 2016 +0200

    Item_514_Metadata: add Update features to Metadata  Implementation + Test Coverage

[33mcommit 54111b7def69c511a11987712a842d506e36b791[m
Author: Etienne CARRIERE <etienne.carriere@culture.gouv.fr>
Date:   Mon Jul 18 15:21:51 2016 +0200

    RPM packaging and deployment

[33mcommit 029c74baf179e0b2b431b220094169d8c3bd56d2[m
Merge: 0996d64 8133580
Author: Kristopher Waltzer <kristopher.waltzer.ext@culture.gouv>
Date:   Tue Jul 12 18:50:29 2016 +0200

    Merge branch 'master_iteration_5' into 'master'
    
    Merge : fin d'itération 5
    
    Fin d'itération 5 (merge dans master en prévision de la release 0.5.0)
    
    See merge request !307

[33mcommit 81335806de196fa640c1527f48c4e7c3190fae56[m
Merge: 7aa5508 ce9d973
Author: Kristopher Waltzer <kristopher.waltzer.ext@culture.gouv>
Date:   Tue Jul 12 15:36:49 2016 +0200

    Merge branch 'bug_fix_master_iteration_5' into 'master_iteration_5'
    
    Fix compareDigestMessage in processing and fix public value in SanityChecker
    
    Fix compareDigestMessage in processing
    Fix public value in SanityChecker
    
    See merge request !305

[33mcommit ce9d973dd9a9dd5bc3843142fbe249e704841fa8[m
Author: Yousri KOUKI <yousri.kouki.ext@culture.gouv.fr>
Date:   Tue Jul 12 15:28:33 2016 +0200

    Fix compareDigestMessage in processing and fix public value in SanityChecker

[33mcommit 7aa55088b9c9faeaf15472f9c3290457bdf6081d[m
Merge: 8ffcd28 341433d
Author: Kristopher Waltzer <kristopher.waltzer.ext@culture.gouv>
Date:   Tue Jul 12 15:18:03 2016 +0200

    Merge branch 'bug_rec-deploy-iteration-5' into 'master_iteration_5'
    
    Fix du bug de déploiement sur l'environnement de recette
    
    L'host de connexion de mongo-express vers mongodb était faux sur l'environnement de recette
    
    See merge request !302

[33mcommit 341433d7e43a6ae39f2b538610a06c28b76884ad[m
Author: Kristopher <kristopher.waltzer.ext@culture.gouv.fr>
Date:   Tue Jul 12 09:21:19 2016 +0200

    Fixed deployment bug (mongo-express had the wrong mongo server for rec)

[33mcommit 8ffcd283c477e0e65f73a29120a84788c1ddea9d[m
Merge: d2a690f 0c865da
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Tue Jul 12 07:54:55 2016 +0200

    Merge branch 'item_647_backup' into 'master_iteration_5'
    
    story_486 : integration processing
    
    
    
    See merge request !300

[33mcommit 0c865da2c3367f9309458f57a3538b328aba4ef1[m
Author: Ramzi LAZREG <ramzy.lazreg.ext@culture.gouv.fr>
Date:   Mon Jul 11 21:53:55 2016 +0200

    integration processing

[33mcommit d2a690fabb06457a51e4eca5fae6b35aed018980[m
Merge: 0b6f2e5 cabd731
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Mon Jul 11 22:59:47 2016 +0200

    Merge branch 'item_647_increase_ingest_test_2' into 'master_iteration_5'
    
    increase ingest rest coverage test
    
    increase ingest rest test
    
    See merge request !299

[33mcommit cabd73136f209f6e7f36c317d11d46024ade6398[m
Author: Buon SUI <bsui@cdh-it.com>
Date:   Mon Jul 11 21:43:35 2016 +0200

    increase ingest rest coverage test

[33mcommit 0b6f2e5dd01a3398297432be9ee69de3b6e0a7ec[m
Merge: 34a7dfa 9acf5bd
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Mon Jul 11 19:12:14 2016 +0200

    Merge branch 'iteration5_Doc' into 'master_iteration_5'
    
    Add Doc
    
    
    
    See merge request !290

[33mcommit 34a7dfaa0cc2a166e66cbed3e5c7fa651fba32a4[m
Merge: 81a6d2c 04d6fb0
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Mon Jul 11 19:11:53 2016 +0200

    Merge branch 'deploy_master_iteration_5' into 'master_iteration_5'
    
    fix vitam deployment
    
    fix vitam deployment
    
    https://dev.programmevitam.fr/jenkins/job/deploy_master_iteration_5/29/
    
    See merge request !294

[33mcommit 04d6fb02cd63f339e6fe8d42de7841a0a8bcb1bb[m
Author: Yousri KOUKI <yousri.kouki.ext@culture.gouv.fr>
Date:   Mon Jul 11 18:46:34 2016 +0200

    fix vitam deployment

[33mcommit 9acf5bdc113e63e1db4baa3adf57e4858cbeebb5[m
Author: kim LE <kim.le.ext@culture.gouv.fr>
Date:   Mon Jul 11 16:31:32 2016 +0200

    Add Doc

[33mcommit 81a6d2c69f4c2645cdb972b58c17e5c83b670ab9[m
Merge: b09b7d2 18a79f9
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Sun Jul 10 22:54:19 2016 +0200

    Merge branch 'item_647' into 'master_iteration_5'
    
    item_646_647; add logbook life cycle methods
    
    item_647: logBook life cycle core
    item_646: logBook life cycle rest and client:
     - Add create, update ,delete and commit for unit and object group
    
    See merge request !287

[33mcommit 18a79f9d3a9e2d79729ae526d6bfcd045f23785e[m
Author: ubuntu ramzi 64 bit <ramzy.lazreg.ext>
Date:   Sat Jul 9 15:38:54 2016 -0700

    item_646_647; add logbook life cycle methods
    
    java doc

[33mcommit b09b7d2069d1e4614a870acc95ce1aec606b4b61[m
Merge: cd0c97b 554d01f
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Sat Jul 9 09:43:21 2016 +0200

    Merge branch 'item_611' into 'master_iteration_5'
    
    story_78 : correct integration tests errors
    
    
    
    See merge request !284

[33mcommit 554d01f651c9c26f33bc34e77014d869af4dc0ad[m
Author: hamri <hamri@vitam-02.coraud.com>
Date:   Fri Jul 8 20:49:20 2016 +0200

    correct critical sonar error

[33mcommit 0c6186fa8a1681e30816c5c14c1af060a95b1637[m
Author: hamri <hamri@vitam-02.coraud.com>
Date:   Fri Jul 8 19:53:06 2016 +0200

    story_78 : correct integration tests errors

[33mcommit cd0c97b63d1dc5c7ffdc6f60b005fd9d69ca4946[m
Merge: 020b3f1 c37f2f6
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Fri Jul 8 18:21:19 2016 +0200

    Merge branch 'item_581' into 'master_iteration_5'
    
    Fix detail message in CheckObjectNumberHandler
    
    
    
    See merge request !281

[33mcommit c37f2f6a30589840220c912ad960d7a0bbf2aa7f[m
Author: NGO BA TUAN <ba-tuan.ngo.ext@culture.gouv.fr>
Date:   Fri Jul 8 15:23:01 2016 +0200

    Fix detail message in CheckObjectNumberHandler

[33mcommit 020b3f18c4bb49e559172214596b406e9d1e4bbc[m
Merge: 3f401ce 11bc411
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Fri Jul 8 17:23:18 2016 +0200

    Merge branch 'item_567' into 'master_iteration_5'
    
    item_567 : XML & Json Sanity Check
    
    Check Sanity  of Json & XML
    
    See merge request !276

[33mcommit 11bc41186dfd17b7eb847ed61bdb70fa9118fdb4[m
Author: Hoan VU <hoan.vu.ext@culture.gouv.fr>
Date:   Mon Jul 4 16:22:49 2016 +0200

    item_567 : XML & Json Sanity Check, fix java-doc workspace

[33mcommit 3f401ceea672e1742d957bf979a94878f9d6dbb3[m
Merge: d2f3109 555ea81
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Fri Jul 8 14:05:18 2016 +0200

    Merge branch 'item_documentation' into 'master_iteration_5'
    
    Documentation US33 US68 US70 US90
    
    Documentation for US33 US68 US70 US90
    
    See merge request !275

[33mcommit 555ea8190bd4ab8b617ec8aee6a09dd539a473cc[m
Author: NGO BA TUAN <ba-tuan.ngo.ext@culture.gouv.fr>
Date:   Fri Jul 8 10:27:30 2016 +0200

    Documentation US33 US68 US70 US90

[33mcommit d2f310998b70e04a0f6de99c60446ed568b059c7[m
Merge: e38f1f8 997d61d
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Thu Jul 7 13:51:30 2016 +0200

    Merge branch 'item_611' into 'master_iteration_5'
    
    Item 611 Release
    
    - Correct response processing (in front side)
    - Correct skipped tests (ihm-demo module)
    - Correct ihm-demo config file
    
    See merge request !272

[33mcommit 997d61d673f937e5382a799d89aef1e9f041df57[m
Author: Hela Amri <hamri@vitam-02.coraud.com>
Date:   Thu Jul 7 13:02:38 2016 +0200

    item_611 release

[33mcommit e38f1f85e489496a634396e9d00a9980a293a3c2[m
Merge: f7998d9 8008526
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Wed Jul 6 19:02:31 2016 +0200

    Merge branch 'item_611_Access' into 'master_iteration_5'
    
    item 611 : add select unit by id on access module
    
    Module Access add select by id
    
    See merge request !263

[33mcommit f7998d936a84ef7d83f9338c65a1a740ed99d6b3[m
Merge: 04afce0 6fa07c7
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Wed Jul 6 18:56:48 2016 +0200

    Merge branch 'item_581' into 'master_iteration_5'
    
    Add error message in processing response
    
    Add error message in processing response
    
    See merge request !260

[33mcommit 6fa07c74248a29a69af7a74e4fda7d0f1e0de857[m
Author: NGO BA TUAN <ba-tuan.ngo.ext@culture.gouv.fr>
Date:   Wed Jul 6 15:45:01 2016 +0200

    Fix processing javadoc

[33mcommit be3cd73993b77617018877eaeb351a925adabd93[m
Author: NGO BA TUAN <ba-tuan.ngo.ext@culture.gouv.fr>
Date:   Tue Jul 5 17:06:42 2016 +0200

    Fix forkmode error

[33mcommit 84a3042cfd4f886220c824c5ff3f34c509da9074[m
Author: NGO BA TUAN <ba-tuan.ngo.ext@culture.gouv.fr>
Date:   Mon Jul 4 13:30:38 2016 +0200

    Fix minor error in ihm-demo

[33mcommit c70130e9d362e886bd6703eb4c2aa97f1e9b0de8[m
Author: NGO BA TUAN <ba-tuan.ngo.ext@culture.gouv.fr>
Date:   Mon Jul 4 12:49:09 2016 +0200

    Add error message in processing response

[33mcommit 8008526260ecb10c2fb4080e10b9714d13c1eb4a[m
Author: Haykel <haykel.benmassaoud.ext@culture.gouv.fr>
Date:   Wed Jul 6 18:09:50 2016 +0200

    item 611 Access : add select unit by id

[33mcommit cc54a8049eb988531e8b44d6ddac3cda38562e97[m
Author: Haykel <haykel.benmassaoud.ext@culture.gouv.fr>
Date:   Tue Jul 5 09:19:12 2016 +0200

    item 611 : add select unit by id on access module

[33mcommit 04afce07239723f7b47d466375adc1ef636f1dfb[m
Merge: bae5290 fa4aae3
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Mon Jul 4 18:07:37 2016 +0200

    Merge branch 'item_610' into 'master_iteration_5'
    
    item 610 : add select unit by id
    
     - metadata module and fixed access test
    
    See merge request !251

[33mcommit bae52908dcb9c1b4b1b348a466a5a3f9a41bb58d[m
Merge: 42aa70d a624d8a
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Mon Jul 4 17:17:57 2016 +0200

    Merge branch 'item_611' into 'master_iteration_5'
    
    Item 611 release
    
    - Update IHM part to call REST services
    - Add selectUnitById in ServerApplication
    
    See merge request !259

[33mcommit a624d8af144a1f32fb5cf84a91fd67faa5253f77[m
Author: hamri <hamri@vitam-02.coraud.com>
Date:   Mon Jul 4 16:42:34 2016 +0200

    item_611 : use global variable + UnitRequestDTOTest

[33mcommit fa4aae358eff7dd9d585876cf4c1408a2394ec7d[m
Author: Ramzi LAZREG <ramzy.lazreg.ext@culture.gouv.fr>
Date:   Mon Jul 4 15:17:43 2016 +0200

    item 610 add select unit by id

[33mcommit f9bc2de4942209ba294ef121312ee42f3cb72bae[m
Author: Hela Amri <hamri@vitam-02.coraud.com>
Date:   Mon Jul 4 15:23:22 2016 +0200

    item_611 release

[33mcommit 42aa70d8e70d8c621a072bb49ed13028f7851397[m
Merge: 2c04588 82f5c65
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Mon Jul 4 15:17:10 2016 +0200

    Merge branch 'item_improve_db' into 'master_iteration_5'
    
    Reset Parallel mode until Fix
    
    
    
    See merge request !261

[33mcommit 82f5c655087953aa1e3e4e2130760a9defcfd31c[m
Author: Frederic Bregier <frederic.bregier@culture.gouv.fr>
Date:   Mon Jul 4 14:41:20 2016 +0200

    Reset Parallel mode until Fix

[33mcommit 96cc3c76a226aba019c43494f1b6552ad32d236f[m
Author: hamri <hamri@vitam-02.coraud.com>
Date:   Thu Jun 30 17:45:37 2016 +0200

    item_611 : IHM part

[33mcommit 2c04588bcfeab4e46597d4bd5cd558acf54e3ef5[m
Merge: 9e248dd 970c933
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Sun Jul 3 20:31:54 2016 +0200

    Merge branch 'item_improve_db' into 'master_iteration_5'
    
    Fix deprecated forkMode and set reuseForks=false
    
    
    
    See merge request !256

[33mcommit 970c933f74b290e3dedec825a6feac96be133ddd[m
Author: Frederic Bregier <frederic.bregier@culture.gouv.fr>
Date:   Sun Jul 3 20:29:33 2016 +0200

    Fix deprecated forkMode and set reuseForks=false

[33mcommit 9e248ddbc2c4286cb856b97450e6510f62151b99[m
Merge: 4d38af6 80296f5
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Sun Jul 3 19:40:01 2016 +0200

    Merge branch 'item_improve_db' into 'master_iteration_5'
    
    Fix Junit, Metadata, Logbook, add JunitHelper
    
    Metadata:
    
    - Fix some bad codes
    - First step for futur publication
    
    Logbook:
    
    - Fix the Junit using src/test/resources with another name than default (Eclipse bug)
    
    Junit:
    
    - Add default server-identity.conf
    - Add default logback-test.xml
    
    Note: Not on all modules since not ready for all
    
    - All Junits that depends on Net services should use JunitHelper
    - All Junits that depends on default configuration files should write them on runtime but not using in source the default name
    
    - In case a module cannot **yet** be tested in parallel, add **temporarily** the following to the module pom.xml, until your tests are ok with default parallel parent configuration (class level).
    
    ```xml
      <build>
        <pluginManagement>
          <plugins>
    	<plugin>
    	  <!-- Run the Junit unit tests in an isolated classloader and not Parallel. -->
    	  <artifactId>maven-surefire-plugin</artifactId>
    	  <version>2.19.1</version>
    	  <configuration>
    	    <argLine>-Xmx2048m -Dvitam.tmp.folder=/tmp ${coverageAgent}</argLine>
    	    <forkMode>always</forkMode>
    	    <parallel>classes</parallel>
    	    <threadCount>1</threadCount>
    	    <perCoreThreadCount>false</perCoreThreadCount>
    	    <forkCount>1</forkCount>
    	    <reuseForks>false</reuseForks>
    	  </configuration>
    	</plugin>
          </plugins>
        </pluginManagement>
      </build>
    ```
    
    Allow to run in parallel Junit tests:
    
    - Add in your pom the common-junit in scope **test**
    
    ```xml
     <dependency>
       <groupId>fr.gouv.vitam</groupId>
       <artifactId>common-junit</artifactId>
       <version>${project.version}</version>
       <scope>test</scope>
     </dependency>
    ```
    - From @BeforeClass method
    
      - From JunitHelper, gets an available port
      - Use it in your server port configuration
      - Eventually use the PropertyUtils.writeYaml to write your default configuration file using this port
    
    - From @AfterClass method
    
      - Release your resources
      - Delete your eventual yaml configuration file
      - Release the assigned port using JunitHelper
    
      - Declare static variables
    
    ```java
          private static JunitHelper junitHelper;
          private static int databasePort;
          private static int serverPort;
    ```
    
      - In the @BeforeClass
    
    ```java
          // dans le @BeforeClass
          // Créer un objet JunitHelper
          junitHelper = new JunitHelper();
    
          // Pour MongoDB (exemple)
          databasePort = junitHelper.findAvailablePort();
          final MongodStarter starter = MongodStarter.getDefaultInstance();
          // On utilise le port
          mongodExecutable = starter.prepare(new MongodConfigBuilder()
              .version(Version.Main.PRODUCTION)
              .net(new Net(databasePort, Network.localhostIsIPv6()))
              .build());
          mongod = mongodExecutable.start();
    
          // Pour le serveur web (ici Logbook)
          // On initialise le mongoDbAccess pour le service
          mongoDbAccess =
              MongoDbAccessFactory.create(
                  new DbConfigurationImpl(DATABASE_HOST, databasePort,
                      "vitam-test"));
          // On alloue un port pour le serveur Web
          serverPort = junitHelper.findAvailablePort();
    
          // On lit le fichier de configuration par défaut présent dans le src/test/resources
          File logbook = PropertiesUtils.findFile(LOGBOOK_CONF);
          // On extraie la configuration
          LogbookConfiguration realLogbook = PropertiesUtils.readYaml(logbook, LogbookConfiguration.class);
          // On change le port
          realLogbook.setDbPort(databasePort);
          // On sauvegarde le fichier (dans un nouveau fichier différent) (static File)
          newLogbookConf = File.createTempFile("test", LOGBOOK_CONF, logbook.getParentFile());
          PropertiesUtils.writeYaml(newLogbookConf, realLogbook);
    
          // On utilise le port pour RestAssured
          RestAssured.port = serverPort;
          RestAssured.basePath = REST_URI;
    
          // On démarre le serveur
          try {
             vitamServer = LogbookApplication.startApplication(new String[] {
                // On utilise le fichier de configuration ainsi créé
                 newLogbookConf.getAbsolutePath(),
                 Integer.toString(serverPort)});
             ((BasicVitamServer) vitamServer).start();
          } catch (FileNotFoundException | VitamApplicationServerException e) {
             LOGGER.error(e);
             throw new IllegalStateException(
                 "Cannot start the Logbook Application Server", e);
         }
    ```
    
      - Dans le @AfterClass
    
    ```java
         // Dans le @AfterClass
         // On arrête le serveur
         try {
             ((BasicVitamServer) vitamServer).stop();
         } catch (final VitamApplicationServerException e) {
             LOGGER.error(e);
         }
         mongoDbAccess.close();
         junitHelper.releasePort(serverPort);
         // On arrête MongoDb
         mongod.stop();
         mongodExecutable.stop();
         junitHelper.releasePort(databasePort);
         // On efface le fichier temporaire
         newLogbookConf.delete();
    ```
    
    
    See merge request !255

[33mcommit 80296f5eb6fc57787dd8dfedce3b0d00407af99f[m
Author: Yousri KOUKI <yousri.kouki.ext@culture.gouv.fr>
Date:   Fri Jun 24 18:34:41 2016 +0200

    Fix Junit, Metadata, Logbook, add JunitHelper
    
    Metadata:
    
    - Fix some bad codes
    - First step for futur publication
    
    Logbook:
    
    - Fix the Junit using src/test/resources with another name than default (Eclipse bug)
    
    Junit:
    
    - Add default server-identity.conf
    - Add default logback-test.xml
    
    Note: Not on all modules since not ready for all
    
    - All Junits that depends on Net services should use JunitHelper
    - All Junits that depends on default configuration files should write them on runtime but not using in source the default name
    
    Allow to run in parallel Junit tests:
    
    - From @BeforeClass method
    
      - From JunitHelper, gets an available port
      - Use it in your server port configuration
      - Eventually use the PropertyUtils.writeYaml to write your default configuration file using this port
    
    - From @AfterClass method
    
      - Release your resources
      - Delete your eventual yaml configuration file
      - Release the assigned port using JunitHelper
    
          private static JunitHelper junitHelper;
          private static int databasePort;
          private static int serverPort;
    
          // dans le @BeforeClass
          // Créer un objet JunitHelper
          junitHelper = new JunitHelper();
    
          // Pour MongoDB (exemple)
          databasePort = junitHelper.findAvailablePort();
          final MongodStarter starter = MongodStarter.getDefaultInstance();
          // On utilise le port
          mongodExecutable = starter.prepare(new MongodConfigBuilder()
              .version(Version.Main.PRODUCTION)
              .net(new Net(databasePort, Network.localhostIsIPv6()))
              .build());
          mongod = mongodExecutable.start();
    
          // Pour le serveur web (ici Logbook)
          // On initialise le mongoDbAccess pour le service
          mongoDbAccess =
              MongoDbAccessFactory.create(
                  new DbConfigurationImpl(DATABASE_HOST, databasePort,
                      "vitam-test"));
          // On alloue un port pour le serveur Web
          serverPort = junitHelper.findAvailablePort();
    
          // On lit le fichier de configuration par défaut présent dans le src/test/resources
          File logbook = PropertiesUtils.findFile(LOGBOOK_CONF);
          // On extraie la configuration
          LogbookConfiguration realLogbook = PropertiesUtils.readYaml(logbook, LogbookConfiguration.class);
          // On change le port
          realLogbook.setDbPort(databasePort);
          // On sauvegarde le fichier (dans un nouveau fichier différent) (static File)
          newLogbookConf = File.createTempFile("test", LOGBOOK_CONF, logbook.getParentFile());
          PropertiesUtils.writeYaml(newLogbookConf, realLogbook);
    
          // On utilise le port pour RestAssured
          RestAssured.port = serverPort;
          RestAssured.basePath = REST_URI;
    
          // On démarre le serveur
          try {
             vitamServer = LogbookApplication.startApplication(new String[] {
                // On utilise le fichier de configuration ainsi créé
                 newLogbookConf.getAbsolutePath(),
                 Integer.toString(serverPort)});
             ((BasicVitamServer) vitamServer).start();
          } catch (FileNotFoundException | VitamApplicationServerException e) {
             LOGGER.error(e);
             throw new IllegalStateException(
                 "Cannot start the Logbook Application Server", e);
         }
    
         // Dans le @AfterClass
         // On arrête le serveur
         try {
             ((BasicVitamServer) vitamServer).stop();
         } catch (final VitamApplicationServerException e) {
             LOGGER.error(e);
         }
         mongoDbAccess.close();
         junitHelper.releasePort(serverPort);
         // On arrête MongoDb
         mongod.stop();
         mongodExecutable.stop();
         junitHelper.releasePort(databasePort);
         // On efface le fichier temporaire
         newLogbookConf.delete();

[33mcommit 4d38af6245923ba55d4f30e091b52278f4321f2c[m
Merge: 2625401 5c61dd2
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Fri Jul 1 18:46:06 2016 +0200

    Merge branch 'item_581' into 'master_iteration_5'
    
    Integration story global for Ingest
    
    
    
    See merge request !252

[33mcommit 5c61dd2bd25525802e6245226ef50bec797206f9[m
Author: NGO BA TUAN <ba-tuan.ngo.ext@culture.gouv.fr>
Date:   Fri Jul 1 16:55:05 2016 +0200

    Fix message identifier in processing

[33mcommit bd964ed97e05be15413a6e863faa3f5e9b82c5c9[m
Author: NGO BA TUAN <ba-tuan.ngo.ext@culture.gouv.fr>
Date:   Fri Jul 1 16:54:45 2016 +0200

    Fix ingest integration

[33mcommit e189b82bc002e10fbedd9c37570c67f579834caf[m
Author: NGO BA TUAN <ba-tuan.ngo.ext@culture.gouv.fr>
Date:   Fri Jul 1 16:54:17 2016 +0200

    Fix upload Sip url in ihm-demo

[33mcommit 23bc117e88971d8c4c9251e583e7a5f5402fab7a[m
Author: NGO BA TUAN <ba-tuan.ngo.ext@culture.gouv.fr>
Date:   Fri Jul 1 16:52:31 2016 +0200

    Third party bower components

[33mcommit 2625401437e2ef973ea242b668d7789f0cac599c[m
Merge: c18f848 09634c3
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Fri Jul 1 16:29:50 2016 +0200

    Merge branch 'story_526' into 'master_iteration_5'
    
    DAT Vitam (1ère version)
    
    Première version du DAT
    
    See merge request !249

[33mcommit c18f848fb3900818a599e82b3dcf047b868ecacb[m
Merge: 230fdd1 528b961
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Fri Jul 1 15:53:54 2016 +0200

    Merge branch 'user_manuals' into 'master_iteration_5'
    
    add user manual for stories 33/90, 76 and 78.
    
    
    
    See merge request !248

[33mcommit 528b961ef021a756866c51468a7ad9d914450515[m
Author: Thomas Morsellino <thomas.morsellino.ext@culture.gouv.fr>
Date:   Fri Jul 1 14:45:17 2016 +0200

    add user manual for stories 33/90, 76 and 78.

[33mcommit 09634c39257614aa843ec527c58a965c344020fb[m
Author: Kristopher Waltzer <kristopher.waltzer.ext@culture.gouv.fr>
Date:   Fri Jul 1 15:37:29 2016 +0200

    DAT Vitam (1ère version)

[33mcommit 230fdd1ea9255ffb3d4e1daf32552fb8e56a62fe[m
Merge: 75b8b26 6527fc0
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Fri Jul 1 11:40:32 2016 +0200

    Merge branch 'item_581' into 'master_iteration_5'
    
    Item 581 Metadata Insert ObjectGroup + Processing
    
    Metadata Insert ObjectGroup
    - core
    - rest
    - client
    
    Processing update Handler
    
    See merge request !241

[33mcommit 6527fc0758d4386d54ef54a82215bc87da1c555c[m
Author: NGO BA TUAN <ba-tuan.ngo.ext@culture.gouv.fr>
Date:   Thu Jun 30 17:15:14 2016 +0200

    Item 581 : Metadata & processing handler to index ObjectGroup

[33mcommit c518a72f1f522a547a27e79f5ef0c4352564a592[m
Author: NGO BA TUAN <ba-tuan.ngo.ext@culture.gouv.fr>
Date:   Thu Jun 30 17:04:23 2016 +0200

    item_581 Metadata rest API index ObjectGroup

[33mcommit 75b8b26379c410450d590054d4480e9d62957f07[m
Merge: 6a065f7 2faadf5
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Fri Jul 1 11:06:41 2016 +0200

    Merge branch 'item_429' into 'master_iteration_5'
    
    item 429: comparer empreintes
    
    
    
    See merge request !239

[33mcommit 6a065f71b67d83e0a0f4e42d86aa31df1226a278[m
Merge: 95a8079 1133bf2
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Fri Jul 1 11:04:28 2016 +0200

    Merge branch 'Iteration5_DocIngest_Access' into 'master_iteration_5'
    
    Add Doc Access et Ingest
    
    Add Doc Access et Ingest
    
    See merge request !246

[33mcommit 2faadf58c19c4dd3724fcbe9c1730af6aec14530[m
Author: CHEN Zhang <zchen@linagora.com>
Date:   Wed Jun 29 17:51:15 2016 +0200

    item 429: comparer empreintes

[33mcommit 1133bf291fa87c33340dbea298c30ab8d206d77b[m
Author: kim LE <kim.le.ext@culture.gouv.fr>
Date:   Thu Jun 30 11:11:57 2016 +0200

    Add Doc Access et Ingest

[33mcommit 95a807988b01748812fa7714baca03afce0b5a98[m
Merge: 9ced7e7 fc480ff
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Thu Jun 30 18:35:25 2016 +0200

    Merge branch 'item_refacto_ingest_release2' into 'master_iteration_5'
    
    Item refacto ingest release2
    
    integrate comments from last merge request
    
    See merge request !243

[33mcommit fc480ff6541f919c981d841063860c4dc656c92b[m
Author: Buon SUI <bsui@cdh-it.com>
Date:   Thu Jun 30 17:49:05 2016 +0200

    fix ingest-rest test

[33mcommit b24545bab200eb38114e164e74612600ebf63e78[m
Author: Buon SUI <bsui@cdh-it.com>
Date:   Thu Jun 30 17:34:57 2016 +0200

    fix build jenkins

[33mcommit 557177ec136e154e20b64cf6261b14c178316ecd[m
Author: Buon SUI <bsui@cdh-it.com>
Date:   Thu Jun 30 16:50:09 2016 +0200

    fix pom issue for centralisation properties

[33mcommit 16b33fa68017dac76974ad5b865df08c5f9e394d[m
Author: Buon SUI <bsui@cdh-it.com>
Date:   Thu Jun 30 16:15:08 2016 +0200

    resolving merge request comment

[33mcommit cb8bffd324c9362fcf0767e65c4b9ee3e18c022b[m
Author: Buon SUI <bsui@cdh-it.com>
Date:   Thu Jun 30 15:49:30 2016 +0200

    integrate merge request comments for refactoring ingest

[33mcommit 3c7cbe95e6fa6a47eb7fa05a0ab7b4f1a65014f2[m
Author: Buon SUI <bsui@cdh-it.com>
Date:   Thu Jun 30 15:39:50 2016 +0200

    external js libs

[33mcommit 9ced7e74ae578b179bd698f44c33d4c11ae80a4e[m
Merge: a677bff daf6d65
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Wed Jun 29 16:38:21 2016 +0200

    Merge branch 'item_579' into 'master_iteration_5'
    
    Item 579
    
    
    
    See merge request !221

[33mcommit daf6d65ac72c7109eedf1c40c72b287c44198e45[m
Author: CHEN Zhang <zchen@linagora.com>
Date:   Wed Jun 29 15:47:01 2016 +0200

    item_579

[33mcommit a677bff3714f02c45faad2833da2b8362817ad02[m
Merge: ccffad2 e0a8561
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Wed Jun 29 15:12:05 2016 +0200

    Merge branch 'item_504' into 'master_iteration_5'
    
    Item 504 : IHM part
    
    
    
    See merge request !234

[33mcommit ccffad244c1deb97d69770489368e11797cf23f8[m
Merge: dc94893 786b384
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Wed Jun 29 14:07:03 2016 +0200

    Merge branch 'item_580' into 'master_iteration_5'
    
    Add reference between ArchiveUnit and ObjectGroup
    
    Add reference between ArchiveUnit and ObjectGroup
    
    See merge request !231

[33mcommit 0f219bb85dda59806d933ecd5fc4a0d3b9da71c6[m
Author: CHEN Zhang <zchen@linagora.com>
Date:   Tue Jun 28 18:22:41 2016 +0200

    item 579

[33mcommit 32a40cd456f553326476365bd40c51ec7a068013[m
Author: CHEN Zhang <zchen@linagora.com>
Date:   Tue Jun 28 17:24:54 2016 +0200

    item 579: get version list & compare version list & integrer workspace associe -- checkVersionHandler

[33mcommit e0a8561354d5af516143b83148c122d404bcf7f0[m
Author: Hela Amri <hamri@vitam-02.coraud.com>
Date:   Wed Jun 29 10:16:57 2016 +0200

    item 504 release

[33mcommit 41436106ffa96d5e8137475fd5dd4e21c567d5e8[m
Author: hamri <hamri@vitam-02.coraud.com>
Date:   Tue Jun 28 19:13:20 2016 +0200

    item_504: css + images

[33mcommit 453f66bf496c0ee8b520599031f6e219dcf42596[m
Author: hamri <hamri@vitam-02.coraud.com>
Date:   Tue Jun 28 19:12:10 2016 +0200

    item_504: update js libraries (angular material) + add new component
    (v-accordion)

[33mcommit dc94893b1a32ca2e9b182b0b0fe26a525b77d601[m
Merge: 523e6bd 8adce39
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Tue Jun 28 19:39:03 2016 +0200

    Merge branch 'item_516_2' into 'master_iteration_5'
    
    item_516 release
    
    item_516
     -add acces module
     -add select units by query on metadata module
    
    See merge request !219

[33mcommit 8adce39fc76f8821ca8c88812af336472a5791fb[m
Author: Ramzi LAZREG <ramzy.lazreg.ext@culture.gouv.fr>
Date:   Mon Jun 27 10:14:04 2016 +0200

    item_516 release

[33mcommit 786b3849d388b95680a6e9c0c2a5417041a5d7cc[m
Author: NGO BA TUAN <ba-tuan.ngo.ext@culture.gouv.fr>
Date:   Tue Jun 28 18:30:20 2016 +0200

    Add reference between ArchiveUnit and ObjectGroup

[33mcommit 523e6bd086534b20621b691179551ba10acc18ad[m
Merge: 2084f35 097b606
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Tue Jun 28 16:23:01 2016 +0200

    Merge branch 'item_580' into 'master_iteration_5'
    
    Fix workspace unzipObject + Fix processing integration test
    
    Fix workspace unzipObject
    Fix processing integration test
    
    See merge request !228

[33mcommit 097b606e85a7bfbda4f6ea63f39e4bd59e618b2e[m
Author: NGO BA TUAN <ba-tuan.ngo.ext@culture.gouv.fr>
Date:   Tue Jun 28 12:54:12 2016 +0200

    Fix workspace unzipObject

[33mcommit 2084f3540028f3e3bb8fff387aa5c5ba4af1617a[m
Merge: 7c1351d 949c43b
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Tue Jun 28 12:48:48 2016 +0200

    Merge branch 'item_580' into 'master_iteration_5'
    
    item_580 Create objectGroup from manifest file
    
    _ Create objectGroup from manifest file
    _ Save objectGroup in workspace
    _ Read objectGroup from workspace and call metadataclient (todo)
    
    See merge request !214

[33mcommit 949c43bdc3d217309045814fe4bd07bda5dee098[m
Author: NGO BA TUAN <ba-tuan.ngo.ext@culture.gouv.fr>
Date:   Fri Jun 24 17:03:01 2016 +0200

    item_580 Create objectGroup from manifest file

[33mcommit 7c1351d4bff84846c1e18ff5fefcb2a9b19a7916[m
Merge: 46dc571 b22ad01
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Mon Jun 27 18:21:22 2016 +0200

    Merge branch 'item_431' into 'master_iteration_5'
    
    Item #431 #432 #428 Compute Message Digest (API, Core, Rest, Rest Client)
    
    https://dev.programmevitam.fr/sonar/components/index?id=41799
    
    See merge request !213

[33mcommit b22ad01e6cc24508bcead28a2aaf4958f89f8868[m
Author: Yousri KOUKI <yousri.kouki.ext@culture.gouv.fr>
Date:   Mon Jun 27 17:05:17 2016 +0200

    Item #431 #432 #428 Compute Message Digest (API, Core, Rest, Rest Client)

[33mcommit 46dc5714edd21a0a45ade3af46f4bc632b06a747[m
Merge: aaefe70 0f1e154
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Fri Jun 24 19:02:59 2016 +0200

    Merge branch 'item_JunitGetAvailablePort' into 'master_iteration_5'
    
    Add JunitFindAvailablePort in Common-Junit
    
    - Add JunitFindAvailablePort in Common-Junit
    - Fix LogbookOperationParameters creation method (argument to be String)
    
    See merge request !217

[33mcommit 0f1e1548a172cdab247ea4ff8409ea95f67f57bf[m
Author: Frederic Bregier <frederic.bregier@culture.gouv.fr>
Date:   Fri Jun 24 17:52:53 2016 +0200

    Add JunitFindAvailablePort in Common-Junit

[33mcommit aaefe706fb2d9bb7694940dd75af3cbd08d87aa2[m
Merge: 0996d64 34f9264
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Thu Jun 23 17:32:09 2016 +0200

    Merge branch 'item_504' into 'master_iteration_5'
    
    Item_504 : new module archive unit (IHM part) + story_76 (IHM Part)
    
    
    
    See merge request !207

[33mcommit 34f92645ea3105f0a04b32ef04eabecbee8739ac[m
Author: hamri <hamri@vitam-02.coraud.com>
Date:   Thu Jun 23 15:56:14 2016 +0200

    item_504 : integrate archive-unit module (IHM part)

[33mcommit 6c3f1708801c5a53de13c56b8761e3f06d1e8f78[m
Author: hamri <hamri@vitam-02.coraud.com>
Date:   Thu Jun 23 14:38:03 2016 +0200

    item_504: integrate archive-unit module (IHM part)

[33mcommit 77bcb524798ffaa30a65c082bb130a50e567b4fd[m
Author: hamri <hamri@vitam-02.coraud.com>
Date:   Thu Jun 23 12:43:51 2016 +0200

    story_76: update js libraries dependencies

[33mcommit 617032a9980ba03f8912f0d00ba19a7cd17af4f8[m
Author: hamri <hamri@vitam-02.coraud.com>
Date:   Thu Jun 23 12:32:37 2016 +0200

    story_76: IHM part

[33mcommit 0996d64037f6e894b843de965a69689d771fb7b8[m
Merge: b0a8ed9 5988b9d
Author: Kristopher Waltzer <kristopher.waltzer.ext@culture.gouv>
Date:   Tue Jun 21 11:40:27 2016 +0200

    Merge branch 'release_iteration_4' into 'master'
    
    Release iteration 4
    
    Merge branch 'release_iteration_4' into 'master'
    
    See merge request !203

[33mcommit 5988b9dc65e80e59c764b72dbdb167c4b4e17b9d[m
Author: app-jenkins <app-jenkins@vitam-prod-jenkins-1.internet.agri>
Date:   Tue Jun 21 10:41:47 2016 +0200

    [maven-release-plugin] prepare for next development iteration

[33mcommit 8d20eebb87d5546d88592624b2395de55c1e1193[m
Author: app-jenkins <app-jenkins@vitam-prod-jenkins-1.internet.agri>
Date:   Tue Jun 21 10:41:38 2016 +0200

    [maven-release-plugin] prepare release 0.4.0

[33mcommit b0a8ed9fbbe85b2b46d35cf2a02de7543daa69f8[m
Merge: a3ca56d c41c706
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Tue Jun 21 10:10:43 2016 +0200

    Merge branch 'master_iteration_4' into 'master'
    
    Merge master_iteration_4 en fin d'itération
    
    Merge en fin d'itération 4 en vue de la release
    
    See merge request !202

[33mcommit c41c706fe1d7a89417e95d71512ec7f3131f07f9[m
Merge: 15d6c2d 1cbef6d
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Mon Jun 20 16:56:07 2016 +0200

    Merge branch 'item_ihm-demo' into 'master_iteration_4'
    
    Story 33 final
    
    _ Add Ihm demo module
    _ Add search operation Rest API for logbook
    _ Update message identifier for logbook
    
    See merge request !197

[33mcommit 1cbef6de1e5eac7a7f48c3c40dd922564e3b78d9[m
Author: NGO BA TUAN <ba-tuan.ngo.ext@culture.gouv.fr>
Date:   Fri Jun 17 19:14:55 2016 +0200

    Ihm-demo frontend code

[33mcommit 45de11ae492b093612a45979f06ab658158ed8ca[m
Author: NGO BA TUAN <ba-tuan.ngo.ext@culture.gouv.fr>
Date:   Fri Jun 17 19:14:36 2016 +0200

    Ihm-demo Java code

[33mcommit c5b7d3a6fae5999efe9b55bbf4903e084490c0d5[m
Author: NGO BA TUAN <ba-tuan.ngo.ext@culture.gouv.fr>
Date:   Fri Jun 17 19:12:34 2016 +0200

    Javascript vendors lib

[33mcommit 4484880e8869afcb8f62bb00b4474a8e912ce2e5[m
Author: NGO BA TUAN <ba-tuan.ngo.ext@culture.gouv.fr>
Date:   Fri Jun 17 18:55:33 2016 +0200

    Update operation identifier income for logbook

[33mcommit d9aa14501da111dbaa0746d0956e1549bf592248[m
Author: NGO BA TUAN <ba-tuan.ngo.ext@culture.gouv.fr>
Date:   Mon Jun 20 13:11:48 2016 +0200

    Add logbook Rest API for searching operations

[33mcommit 15d6c2d9891fba9d66b810c920fe41da4ce901e8[m
Merge: f97e3c8 46e949a
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Mon Jun 20 14:04:00 2016 +0200

    Merge branch 'support_deployment_iteration_4' into 'master_iteration_4'
    
    Updated deployment configuration for iteration 4
    
    Notably :
    - added new components ;
    - fixed configuration options
    - now using the group contents to set internal link configuration
      between components ;
    - added mongo-express
    
    See merge request !195

[33mcommit 46e949a8c910e3330ab1e97835f9912570fb96b0[m
Author: Kristopher Waltzer <kristopher.waltzer.ext@culture.gouv.fr>
Date:   Mon Jun 20 12:07:57 2016 +0200

    Updated deployment configuration for iteration 4 ; notably :
    - added new components ;
    - fixed configuration options
    - now using the group contents to set internal link configuration
      between components ;
    - added mongo-express

[33mcommit f97e3c8b0247958206a62dc9e787f4d16d3d67ca[m
Merge: 5ee2ad5 80444d6
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Mon Jun 20 10:47:21 2016 +0200

    Merge branch 'review_master_iteration_4' into 'master_iteration_4'
    
    Review Iteration 4 and some Fixes
    
    
    
    See merge request !192

[33mcommit 80444d6967012e899fcbf01f88d4f5deb9e8533e[m
Author: Frederic Bregier <frederic.bregier@culture.gouv.fr>
Date:   Sun Jun 19 17:50:44 2016 +0200

    Fix after review of Checkmarx

[33mcommit 01d99f0ea42c7af657911fe39a411801dd28b73f[m
Author: Frederic Bregier <frederic.bregier@culture.gouv.fr>
Date:   Sun Jun 19 16:02:00 2016 +0200

    Review Iteration 4 and some Fixes

[33mcommit 5ee2ad5b3279ef8330e9afa6445deb4a6006a6e5[m
Merge: 38137b6 f5cf684
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Thu Jun 16 15:48:15 2016 +0200

    Merge branch 'item_568' into 'master_iteration_4'
    
    P1 Item_568 Add Config/Data/Log/Tmp support
    
    Within Common-Public
    - Add SystemPropertyUtil support
    - Add PropertyUtils support
    - Add documentation
    
    See merge request !177

[33mcommit f5cf68466a23b644f0186167820f733814f82808[m
Author: Frederic Bregier <frederic.bregier@culture.gouv.fr>
Date:   Thu Jun 16 13:09:48 2016 +0200

    P1 Item_568 Add Config/Data/Log/Tmp support
    
    Within Common-Public
    - Add SystemPropertyUtil support
    - Add PropertyUtils support
    - Add documentation

[33mcommit 38137b68a227774e33333a806c8e5500b2269f43[m
Merge: ca6fa19 5710de8
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Wed Jun 15 17:45:47 2016 +0200

    Merge branch 'support_460' into 'master_iteration_4'
    
    Support 460 : Changement de structure de la documentation
    
    C'est le changement de structure de documentation fait par Yohann en itération 3 et dont le merge a été repoussé jusqu'aujourd'hui.
    Normalement, j'ai réintégré toutes les modifications documentaires qui avaient été faites depuis.
    
    See merge request !173

[33mcommit 5710de886027fd7e0abe905baaffe4ebaad705a6[m
Author: Yoann Fouquet <yfouquet@localhost.localdomain>
Date:   Wed Jun 1 18:29:07 2016 +0200

    Support 460

[33mcommit ca6fa19bac9ad02a87fcacce30c3b9c730c87a12[m
Merge: a3ca56d 7edeb79
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Mon Jun 13 18:29:44 2016 +0200

    Merge branch 'digest_type' into 'master_iteration_4'
    
    Add fromValue method to Digest
    
    Proposition to add a FromValue so that it is possible to use the enum based on a String (example : from a configuration file)
    
    See merge request !170

[33mcommit 7edeb795d9b6b3ded6e86222547e075a7938ce1e[m
Author: Etienne CARRIERE <etienne.carriere@culture.gouv.fr>
Date:   Mon Jun 13 18:15:00 2016 +0200

    Add fromValue method to Digest

[33mcommit a3ca56dd03f46bd31e0d4f6a0692e6329f84d547[m
Merge: e3d6574 125951a
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Mon Jun 13 17:23:18 2016 +0200

    Merge branch 'release_iteration_3' into 'master'
    
    Release iteration 3
    
    
    
    See merge request !169

[33mcommit 125951ab7d1db372ab9906dafe414dbc74b00a98[m
Author: app-jenkins <app-jenkins@vitam-prod-jenkins-1.internet.agri>
Date:   Mon Jun 13 12:48:47 2016 +0200

    [maven-release-plugin] prepare for next development iteration

[33mcommit 6ad923eaa142c09f7f84979ad35d0f8ab7dffc7c[m
Author: app-jenkins <app-jenkins@vitam-prod-jenkins-1.internet.agri>
Date:   Mon Jun 13 12:48:43 2016 +0200

    [maven-release-plugin] prepare release 0.3.0

[33mcommit e3d6574b7d406af02d332cfc679aece47b85ff6c[m
Merge: 2fbf2a3 c17bae0
Author: Kristopher Waltzer <kristopher.waltzer.ext@culture.gouv>
Date:   Mon Jun 13 10:55:56 2016 +0200

    Merge branch 'master_iteration_3' into 'master'
    
    Merge de l'itération 3 dans master (fin d'itération)
    
    
    
    See merge request !168

[33mcommit c17bae002525cefed55b9b9125cf9abc9378f4d6[m
Merge: 0400b36 d465e97
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Sun Jun 12 01:10:53 2016 +0200

    Merge branch 'story_74_global_refactor' into 'master_iteration_3'
    
    Big Refactoring
    
    - Fix globally POM on all projects except Ingest
    - Create Common-database to contain common part of Metadata and Logbook (and others?)
    - Create Logbook-common-client for DSL part (could be moved to metadata-builder as a new common package later on)
    - Apply Vitam style everywhere
    - Fix VitamLogger as much as possible
    
    See merge request !167

[33mcommit d465e97e04339d4c1e70058d404036713231d876[m
Author: Frederic Bregier <frederic.bregier@culture.gouv.fr>
Date:   Sun Jun 12 00:51:20 2016 +0200

    Big Refactoring
    
    - Fix globally POM on all projects except Ingest
    - Create Common-database to contain common part of Metadata and Logbook (and others?)
    - Create Logbook-common-client for DSL part (could be moved to metadata-builder as a new common package later on)
    - Apply Vitam style everywhere
    - Fix VitamLogger as much as possible

[33mcommit 0400b369c23cb0f80bc7d6f60d216bcb95033f45[m
Author: Frederic Bregier <frederic.bregier@culture.gouv.fr>
Date:   Fri Jun 10 23:42:41 2016 +0200

    Story 74 final refactor
    
    Refactorization of Select/Query
    Remove of Deprecated
    Add one deprecated for LogbookOperationParameters creation
    Create a new LogbookOperationParameters creation method

[33mcommit 7b50c451cd4db2f7c90fa67e31c13bac92a0c379[m
Merge: 9f38d63 5cec6f5
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Fri Jun 10 17:39:09 2016 +0200

    Merge branch 'support_482' into 'master_iteration_3'
    
    Mise à jour des scripts de déploiement pour l'itération 3
    
    Mise à jour des scripts de déploiement pour l'itération 3 ; inventaire de déploiement pour les postes de développement.
    
    See merge request !166

[33mcommit 5cec6f56cff80e64344716df6ffc9a8947bf78e4[m
Author: Kristopher Waltzer <kristopher.waltzer.ext@culture.gouv.fr>
Date:   Fri Jun 10 17:31:00 2016 +0200

    Ajout du rôle docker-raz ; variabilisation permettant le déploiement local ; activation des rôles ingest & ihm-demo

[33mcommit 9f38d63034002b83b9c8f84d5f163b9c8f51cb57[m
Merge: 46653c7 8f18cb7
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Thu Jun 9 20:16:56 2016 +0200

    Merge branch 'story_84_68_qua' into 'master_iteration_3'
    
    fix uri for docker bis
    
    patch bis : remove maven docker build in ingest-core
    
    See merge request !165

[33mcommit 8f18cb7db8641e91613212cd7418c1b128e83d80[m
Author: Buon SUI <bsui@cdh-it.com>
Date:   Thu Jun 9 20:13:05 2016 +0200

    fix uri for docker bis

[33mcommit 46653c783f0215daf513159eff9a8eae459b8d48[m
Merge: 98a6ce9 f43288e
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Thu Jun 9 20:05:42 2016 +0200

    Merge branch 'story_84_68_qua' into 'master_iteration_3'
    
    fix uri for docker
    
    patch for url in ingestweb with docker
    
    See merge request !164

[33mcommit f43288ec99a7ccdbe68172d3ba3e892c03acc17c[m
Author: Buon SUI <bsui@cdh-it.com>
Date:   Thu Jun 9 19:53:47 2016 +0200

    fix uri for docker

[33mcommit 98a6ce950cb2d04d8d125b5164132d4e0a8a63ca[m
Merge: 2522486 d618f90
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Thu Jun 9 18:29:36 2016 +0200

    Merge branch 'story_84_68_ter_release' into 'master_iteration_3'
    
    Story 84 68 ter release
    
    change ipadr in angular javascript, hardcodded for docker test
    
    See merge request !161

[33mcommit d618f90b8bded5ab7deb7f6baeb5ee672b40ae3c[m
Author: Buon SUI <bsui@cdh-it.com>
Date:   Thu Jun 9 17:40:55 2016 +0200

    change ipadr for docker demo correction

[33mcommit cf77e5ee86a69bcedf9295411375439c4dd776c2[m
Author: Buon SUI <bsui@cdh-it.com>
Date:   Thu Jun 9 16:28:00 2016 +0200

    ingest module

[33mcommit 2522486c00fc4e8df7c388fd031c3433751be2d8[m
Merge: 61a10c8 f9bc9a1
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Thu Jun 9 15:22:07 2016 +0200

    Merge branch 'story_68-69-70-release' into 'master_iteration_3'
    
    US_68 Integration logbook processing
    
    _ Fix logbook client in processing
    _ Add documentation
    _ Fix TODO in processing
    _ Remove check uri from workspace
    
    See merge request !158

[33mcommit f9bc9a10d37eb6062f8a15ad7a17f9e738325074[m
Author: NGO BA TUAN <ba-tuan.ngo.ext@culture.gouv.fr>
Date:   Wed Jun 8 13:50:32 2016 +0200

    US_68 Integration logbook processing

[33mcommit 61a10c80e50781dbfea1533db481ad780d040574[m
Author: Frederic Bregier <frederic.bregier@culture.gouv.fr>
Date:   Thu Jun 9 11:33:46 2016 +0200

    Fix pom dependency for Logbook-Operations-Client

[33mcommit 2c13254009c8fa26ede44fd860835c33b1ef54a6[m
Merge: 5a329b6 6de8218
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Tue Jun 7 23:53:00 2016 +0200

    Merge branch 'story_74_fix_init' into 'master_iteration_3'
    
    Fix Junit and NullPointerException bad usage (Common and Processing)
    
    - Fix start but not stopped Application in test in Logbook
    - Change NullPointerException to IllegalArgumentException in Logger and Processing
    
    
    See merge request !153

[33mcommit 6de8218692a1653c3c322c9e7d744835a57a9f20[m
Author: Frederic Bregier <frederic.bregier@culture.gouv.fr>
Date:   Tue Jun 7 23:50:11 2016 +0200

    Fix start but not stopped Application in test, and Change NullPointerException to IllegalArgumentException

[33mcommit 5a329b66d047b1edee8be433b03f4578e54254fb[m
Merge: 1a59bb7 aea5f8e
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Tue Jun 7 18:28:45 2016 +0200

    Merge branch 'story_74_fix_init' into 'master_iteration_3'
    
    Fix Story 74 and PropertyUtils
    
    - Fix Story 74 Junit (blocked) and Initialization
    - Fix PropertyUtils to make easier to load configuration for all modules through -Dvitam.config.folder
    
    See merge request !152

[33mcommit aea5f8e29e099a1dfbcc865e48fa53a581b35719[m
Author: Frederic Bregier <frederic.bregier@culture.gouv.fr>
Date:   Tue Jun 7 18:20:51 2016 +0200

    Fix Story 74 and PropertyUtils

[33mcommit 1a59bb7570cd58d69385dcefa4d7bd7a9d9f5a75[m
Merge: 5d62a44 f040997
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Mon Jun 6 23:47:50 2016 +0200

    Merge branch 'support_482' into 'master_iteration_3'
    
    Deployment update for iteration 3
    
    Update of the deployment & configuration files for the iteration 3
    
    See merge request !150

[33mcommit f040997e104aaa01629cdaf321be11f365bf4a2d[m
Author: Kristopher Waltzer <kristopher.waltzer.ext@culture.gouv.fr>
Date:   Mon Jun 6 23:44:42 2016 +0200

    Deployment update for iteration 3

[33mcommit 5d62a4426b347a11d7d58f70dfd020a9511b0562[m
Merge: f59fbb3 23c0495
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Mon Jun 6 23:25:47 2016 +0200

    Merge branch 'story_74_fix_init' into 'master_iteration_3'
    
    Fix ServerIdentity and Logbook init in Docker
    
    Add -Dvitam.config.folder
    
    See merge request !149

[33mcommit 23c0495946d522f9ac0e3711713bb7992d556b86[m
Author: Frederic Bregier <frederic.bregier@culture.gouv.fr>
Date:   Mon Jun 6 22:30:45 2016 +0200

    Fix ServerIdentity and Logbook init in Docker
    
    Add -Dvitam.config.folder

[33mcommit f59fbb30f62d23601bb6dbbcb22f5c8ac9101f98[m
Merge: c468c01 7ada518
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Mon Jun 6 19:28:15 2016 +0200

    Merge branch 'bug_badbaseimageversion' into 'master_iteration_3'
    
    Fixed : bad image version in parent pom & fixed base image
    
    Image version was wrong, and not used in all docker builds
    
    See merge request !148

[33mcommit 7ada51831052870e69d8425b928c6203fc253a7f[m
Author: Kristopher Waltzer <kristopher.waltzer.ext@culture.gouv.fr>
Date:   Mon Jun 6 19:16:12 2016 +0200

    Fixed : bad image version in parent pom & fixed base image

[33mcommit c468c01e13da157e8e458d26ff294d6765d6fabd[m
Merge: c3a10ba 5fe7705
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Mon Jun 6 19:22:34 2016 +0200

    Merge branch 'documentation_poba' into 'master_iteration_3'
    
    Documentation PO BA
    
    Update user manuel for SIP upload (story 84).
    
    See merge request !137

[33mcommit 5fe7705fd8d2307a5f0ee2d69b73189a5d753325[m
Author: Gwendoline Stab <gwendoline.stab@culture.gouv.fr>
Date:   Fri Jun 3 15:54:14 2016 +0200

    add documentation for SIP upload

[33mcommit c3a10baab6debe77059d514644a734e6113823ac[m
Merge: a2b1a26 7834ab8
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Mon Jun 6 18:59:27 2016 +0200

    Merge branch 'story_68_69_70' into 'master_iteration_3'
    
    Story 68 69 70
    
    https://dev.programmevitam.fr/sonar/overview?id=fr.gouv.vitam%3Aparent%3Aorigin%2Fstory_68_69_70
    
    See merge request !135

[33mcommit 7834ab8bc1c5089434941d44374613e41546d0c6[m
Author: NGO BA TUAN <ba-tuan.ngo.ext@culture.gouv.fr>
Date:   Mon Jun 6 18:47:50 2016 +0200

    Integration story 68, 69 and 70

[33mcommit a2b1a266671eb0e7954b3a23741357ee530d1738[m
Merge: 2cfb203 7db3bfa
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Mon Jun 6 18:31:56 2016 +0200

    Merge branch 'story_74_release' into 'master_iteration_3'
    
    STORY #74 : Fixed logbook
    
    Added automatic configuration (application server + server identity + logbook client)
    Fixed JUnits
    
    See merge request !145

[33mcommit 7db3bfa57fd736ebbe96ab27bb45e68cb0f79cd1[m
Author: Gael Nieutin <gael.nieutin.ext@culture.gouv.fr>
Date:   Mon Jun 6 11:04:21 2016 +0200

    Fixed logbook
    
    Added automatic configuration (application server + server identity + logbook client)
    Fixed JUnits

[33mcommit 33779477349d8c2d16caab555fa9c399314dc4aa[m
Author: Ramzi LAZREG <ramzy.lazreg.ext@culture.gouv.fr>
Date:   Mon Jun 6 10:19:36 2016 +0200

    STORY_69

[33mcommit 8640415f5f4356ba35d765a3ab1f37f85013fa5f[m
Author: NGO BA TUAN <ba-tuan.ngo.ext@culture.gouv.fr>
Date:   Mon Jun 6 10:03:21 2016 +0200

    STORY_69 and STORY_70

[33mcommit 2cfb2034c8e0c1b9330486cee7c0abfa7a654de0[m
Merge: 2fbf2a3 88cdd1d
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Mon Jun 6 01:48:29 2016 +0200

    Merge branch 'story_74_release' into 'master_iteration_3'
    
    P0 User Story 74 Logbook
    
    Include:
    
    - Documentation
    - Javadoc
    - Junits including full test (from client to database)
    
    Contains some deprecated to be removed in the next iteration.
    
    Tasks:
    - TASK 450 - DSL documentation choice
    - TASK 438 - Rest interface: includes client for Operations and Status
    - TASK 439 - Mongo implementation: includes almost all needed code also for Lifecycles and Bulk insert/update
    - TASK 442 - Client Create
    - TASK 452 - Client Update
    - TASK 440 - DSL MongoDB: based on Metadata
    - TASK 451 - DSL Parsing/Builder: based on Metadata
    - Story 376 - Quality fix on Common
    - Story 374 - Mock Logbook Create and Update for Operation only
    
    Still needed:
    - Client and Rest interface for Read
    - Client and Rest interface for Select
    - Client and Rest interface for Lifecycle (all)
    - Refactoring of Metadata code shared with Logbook
    
    See merge request !127

[33mcommit 88cdd1dafdb3d53c8e7f1c5904d8267a1a58f986[m
Author: Frederic Bregier <frederic.bregier@culture.gouv.fr>
Date:   Wed Jun 1 00:04:57 2016 +0200

    P0 User Story 74 Logbook
    
    Include:
    
    - Documentation
    - Javadoc
    - Junits including full test (from client to database)
    
    Contains some deprecated to be removed in the next iteration.
    
    Tasks:
    - TASK 450 - DSL documentation choice
    - TASK 438 - Rest interface: includes client for Operations and Status
    - TASK 439 - Mongo implementation: includes almost all needed code also for Lifecycles and Bulk insert/update
    - TASK 442 - Client Create
    - TASK 452 - Client Update
    - TASK 440 - DSL MongoDB: based on Metadata
    - TASK 451 - DSL Parsing/Builder: based on Metadata
    - Story 376 - Quality fix on Common
    - Story 374 - Mock Logbook Create and Update for Operation only
    
    Still needed:
    - Client and Rest interface for Read
    - Client and Rest interface for Select
    - Client and Rest interface for Lifecycle (all)
    - Refactoring of Metadata code shared with Logbook

[33mcommit 2fbf2a3792a094830b12803c69090904627eeffe[m
Merge: 78f27ec 7a78e4e
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Tue May 31 23:47:00 2016 +0200

    Merge branch 'story_374_Fix' into 'master'
    
    P0 Story 374 Additional fixes
    
    Fix Mock to log on one line
    Fix Variable ordering and documentation
    Add StringBuilder option for VitamLoggerHelper
    Fix ServerIdentity Name server
    
    See merge request !97

[33mcommit 7a78e4ef660be8322c908f665f23e7cfd2c687e4[m
Author: Frederic Bregier <frederic.bregier@culture.gouv.fr>
Date:   Tue May 31 23:34:36 2016 +0200

    Story 374 Additional fixes
    
    Fix Mock to log on one line
    Fix Variable ordering and documentation
    Add StringBuilder option for VitamLoggerHelper
    Fix ServerIdentity Name server

[33mcommit 78f27ec4bca9b3fd61383bd2f706995bb925b086[m
Merge: c915478 7f25f60
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Tue May 31 18:49:13 2016 +0200

    Merge branch 'story_374_release' into 'master'
    
    P0 US #374 Logbook Operation Mock
    
    Modules and packages for logbook project #444
    
    Archi-fonctionelle documentation about datamodel of opertation logbook #446
    
    Client Create / Update for logbook operation #443
    
    Mock logbook operation #445
    
    See merge request !96

[33mcommit 7f25f60ed382ce0802f082c74c9f3d1d12f2c46c[m
Author: germain ledroit <germainledroit@gmail.com>
Date:   Thu May 26 17:53:44 2016 +0200

    P0 US #374 Logbook Operation Mock
    
    Modules and packages for logbook project #444
    
    Archi-fonctionelle documentation about datamodel of opertation logbook #446
    
    Client Create / Update for logbook operation #443
    
    Mock logbook operation #445

[33mcommit c9154784354132bf843c1504931d3b7a8c090876[m
Merge: 12be3a3 35308cf
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Mon May 30 23:35:59 2016 +0200

    Merge branch 'task_375_000_test_resources' into 'master'
    
    Fix Ressources files access
    
    Common utility for Resources Files
    - Add in Common utility method to load correctly Files from Resources
    - Check also with 'space' in directory names
    
    Also fix JDKLogger
    
    See merge request !91

[33mcommit 35308cfd5bd7ab6e27367e316c9ead933af6ec4d[m
Author: Frederic Bregier <frederic.bregier@culture.gouv.fr>
Date:   Mon May 30 18:07:06 2016 +0200

    Fix Ressources files access
    
    Add in Common utility class to load correctly Files from Resources
    Check also with 'space' in directory names
    
    Also fix JDKLogger

[33mcommit 12be3a3b2e16d401bec997a7aa927ce4221bcefa[m
Merge: 1cc1489 c7d9ac2
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Mon May 30 13:20:27 2016 +0200

    Merge branch 'story_375_release' into 'master'
    
    P0 Story 375 Common GUID Logger Digest ParameterCheck ServerIdentity
    
    # P0 Story #375 Improvement of Commons
    
    Multiple tasks were done
    
    ## Quality fix #434: step 1 (next in Story #376)
    
    - Split Common in Public and Private Module
    - CleanUp all Common
    - Fix callers such as Metadata and Processing
    
    ## ServerIdentity #379
    
    ServerIdentity allows to fix global information for one Module/Engine within one instance:
    
    - Name: by default hostname, but should be GUID of host from Vitam deploymenent configuration
    - Role: by default to UnknownRole, but should be ROLE from various types within Vitam (AccessExternal, AccessInternal, ...)
    - PlatformId: by default to Mac address as int, but should be an ID from Vitam from 0 to 2^30-1
    
    ## GUID #380
    
    - Refactorized GUID
    - Simplify it
    - make a public (reader) and a private (writer) part.
    
    ## Logger #437: steap 1 (next in Story #376)
    
    - First Step: Put in Public and add a special Private Helper
    
    ## Digest #381
    
    - Fix with new implementation
    
    ## ParameterCheck #382
    
    - Add from Metadata
    - Improve it
    
    # Issues Remaining:
    
    - Issue on Jenkins not fixed (not able to load src/test/resources files) while possible on command line
    
      - Add Assume to ignore unreachable files
    
      - **Level does reach > 80% locally but not on Jenkins due to this bug**
    
    - Documentation: Done but do not compile (issue on main Pom.xml)
    
    - Conflict between various logger: One should remove the extra loggers from Vitam main pom.xml
    
    To Keep:
    ```xml
        <dependency>
               <groupId>org.slf4j</groupId>
               <artifactId>slf4j-api</artifactId>
               <version>${slf4j.version}</version>
               <!--<scope>provided</scope> -->
        </dependency>
        <dependency>
               <groupId>ch.qos.logback</groupId>
               <artifactId>logback-classic</artifactId>
               <version>${logback.version}</version>
        </dependency>
        <dependency>
               <groupId>ch.qos.logback</groupId>
               <artifactId>logback-core</artifactId>
               <version>${logback.version}</version>
               <!--<scope>provided</scope> -->
        </dependency>
    ```
    To Remove:
    ```xml
    	<dependency>
    		<groupId>log4j</groupId>
    		<artifactId>log4j</artifactId>
    		<version>${log4j.version}</version>
    		<optional>true</optional>
    	</dependency>
    	<dependency>
    		<groupId>org.slf4j</groupId>
    		<artifactId>jcl-over-slf4j</artifactId>
    		<version>${slf4j.version}</version>
    		<!--<scope>provided</scope> -->
    	</dependency>
    	<dependency>
    		<groupId>org.slf4j</groupId>
    		<artifactId>log4j-over-slf4j</artifactId>
    		<version>${slf4j.version}</version>
    		<!--<scope>provided</scope> -->
    	</dependency>
    	<dependency>
    		<groupId>org.apache.logging.log4j</groupId>
    		<artifactId>log4j-api</artifactId>
    		<version>${apache.logging.version}</version>
    	</dependency>
    	<dependency>
    		<groupId>org.apache.logging.log4j</groupId>
    		<artifactId>log4j-core</artifactId>
    		<version>${apache.logging.version}</version>
    	</dependency>
    	<dependency>
    		<groupId>org.apache.logging.log4j</groupId>
    		<artifactId>log4j-1.2-api</artifactId>
    		<version>${apache.logging.version}</version>
    	</dependency>
    	<dependency>
    		<groupId>org.slf4j</groupId>
    		<artifactId>slf4j-log4j12</artifactId>
    		<version>${slf4j.version}</version>
    	</dependency>
    	<dependency>
    		<groupId>org.slf4j</groupId>
    		<artifactId>slf4j-simple</artifactId>
    		<version>${slf4j.version}</version>
    	</dependency>
    	<dependency>
    		<groupId>org.slf4j</groupId>
    		<artifactId>jul-to-slf4j</artifactId>
    		<version>${slf4j.version}</version>
    		<!--<scope>provided</scope> -->
    	</dependency>
    ```
    
    See merge request !88

[33mcommit c7d9ac264725fbd33bae1e84f884431bb1dc6bad[m
Author: Frederic Bregier <frederic.bregier@culture.gouv.fr>
Date:   Thu May 26 00:49:18 2016 +0200

    P0 Story 375 Common GUID Logger
    
    Quality fix #434: step 1 (next in Story #376)
    - Split Common in Public and Private Module
    - CleanUp all Common
    - Fix callers such as Metadata and Processing
    
    ServerIdentity #379
    
    ServerIdentity allows to fix global information for one Module/Engine within one instance:
    - Name: by default hostname, but should be GUID of host from Vitam deploymenent configuration
    - Role: by default to UnknownRole, but should be ROLE from various types within Vitam (AccessExternal, AccessInternal, ...)
    - PlatformId: by default to Mac address as int, but should be an ID from Vitam from 0 to 2^30-1
    
    GUID #380
    
    - Refactorized GUID
    - Simplify it
    - make a public (reader) and a private (writer) part.
    
    Logger #437: steap 1 (next in Story #376)
    - First Step: Put in Public and add a special Private Helper
    
    Digest #381
    - Fix with new implementation
    
    ParameterCheck #382
    - Add from Metadata
    - Improve it
    
    Issues:
    - Issue on Jenkins not fixed (not able to load src/test/resources files) while possible on command line
      Add Assume to ignore unreachable files
    
    - Documentation: Done but do not compile (issue on main Pom.xml)
    
    - Conflict between various logger: One should remove the extra loggers from Vitam main pom.xml
    
    To Keep:
                            <dependency>
                                    <groupId>org.slf4j</groupId>
                                    <artifactId>slf4j-api</artifactId>
                                    <version>${slf4j.version}</version>
                                    <!--<scope>provided</scope> -->
                            </dependency>
                            <dependency>
                                    <groupId>ch.qos.logback</groupId>
                                    <artifactId>logback-classic</artifactId>
                                    <version>${logback.version}</version>
                            </dependency>
                            <dependency>
                                    <groupId>ch.qos.logback</groupId>
                                    <artifactId>logback-core</artifactId>
                                    <version>${logback.version}</version>
                                    <!--<scope>provided</scope> -->
                            </dependency>
    
    To Remove:
    			<dependency>
    				<groupId>log4j</groupId>
    				<artifactId>log4j</artifactId>
    				<version>${log4j.version}</version>
    				<optional>true</optional>
    			</dependency>
    			<dependency>
    				<groupId>org.slf4j</groupId>
    				<artifactId>jcl-over-slf4j</artifactId>
    				<version>${slf4j.version}</version>
    				<!--<scope>provided</scope> -->
    			</dependency>
    			<dependency>
    				<groupId>org.slf4j</groupId>
    				<artifactId>log4j-over-slf4j</artifactId>
    				<version>${slf4j.version}</version>
    				<!--<scope>provided</scope> -->
    			</dependency>
    			<dependency>
    				<groupId>org.apache.logging.log4j</groupId>
    				<artifactId>log4j-api</artifactId>
    				<version>${apache.logging.version}</version>
    			</dependency>
    			<dependency>
    				<groupId>org.apache.logging.log4j</groupId>
    				<artifactId>log4j-core</artifactId>
    				<version>${apache.logging.version}</version>
    			</dependency>
    			<dependency>
    				<groupId>org.apache.logging.log4j</groupId>
    				<artifactId>log4j-1.2-api</artifactId>
    				<version>${apache.logging.version}</version>
    			</dependency>
    			<dependency>
    				<groupId>org.slf4j</groupId>
    				<artifactId>slf4j-log4j12</artifactId>
    				<version>${slf4j.version}</version>
    			</dependency>
    			<dependency>
    				<groupId>org.slf4j</groupId>
    				<artifactId>slf4j-simple</artifactId>
    				<version>${slf4j.version}</version>
    			</dependency>
    			<dependency>
    				<groupId>org.slf4j</groupId>
    				<artifactId>jul-to-slf4j</artifactId>
    				<version>${slf4j.version}</version>
    				<!--<scope>provided</scope> -->
    			</dependency>

[33mcommit 1cc148949dc6fa08f9f814cc8a6ce5275caa1a4b[m
Merge: d804eea a0ececd
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Thu May 19 16:52:51 2016 +0000

    Merge branch 'release_iteration_2' into 'master'
    
    Release iteration 2
    
    
    
    See merge request !73

[33mcommit a0ececd880b2d2bc8f4df585baad6c1375d4feab[m
Author: app-jenkins <app-jenkins@vitam-prod-jenkins-1.internet.agri>
Date:   Thu May 19 17:06:34 2016 +0200

    [maven-release-plugin] prepare for next development iteration

[33mcommit 101e20d05df2cb56fc8d81c738f6e0c4e4f769b8[m
Author: app-jenkins <app-jenkins@vitam-prod-jenkins-1.internet.agri>
Date:   Thu May 19 17:06:25 2016 +0200

    [maven-release-plugin] prepare release 0.2.0

[33mcommit 759b5a500707ff7d362575e520cb2ed5df185e9c[m
Merge: 28d3ef6 0e65bca
Author: Kristopher Waltzer <kristopher.waltzer.ext@culture.gouv.fr>
Date:   Thu May 19 14:51:46 2016 +0000

    Merge branch 'review_2' into 'release_iteration_2'
    
    Review for iteration 2
    
    
    
    See merge request !72

[33mcommit d804eea0f30873e26fedbc61d4e17ad103839955[m
Merge: 3d287b6 0e65bca
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Thu May 19 12:25:12 2016 +0000

    Merge branch 'review_2' into 'master'
    
    Review for iteration 2
    
    
    
    See merge request !71

[33mcommit 0e65bcae8b1ef1e1ca74e057315b927d9a3fe87f[m
Author: Etienne CARRIERE <etienne.carriere@gmail.com>
Date:   Sun May 15 08:47:02 2016 +0200

    Review for iteration 2

[33mcommit 28d3ef67cf5d4c88ac422b46d918ff8fbc1e2bbb[m
Merge: 81ae0a2 9331667
Author: Etienne Carriere <etienne.carriere@culture.gouv.fr>
Date:   Thu May 19 10:32:19 2016 +0000

    Merge branch 'bug_ansible_deployment' into 'release_iteration_2'
    
    Fixed : ansible inventory & pull
    
    Fixed ansible docker usage (that didn't update the local container if remote had changed)
    
    See merge request !70

[33mcommit 9331667ce2ed04938839a74ff93a227ce4c59a88[m
Author: Olivier MARSOL <olivier.marsol.ext@culture.gouv.fr>
Date:   Wed May 18 11:02:20 2016 +0200

    uncommeted line in hosts.int + added pull: always in all docker directives

[33mcommit 81ae0a2d1891eb10fd47dc392205e42fba3cd7ef[m
Author: app-jenkins <app-jenkins@vitam-prod-jenkins-1.internet.agri>
Date:   Thu May 19 11:59:18 2016 +0200

    [maven-release-plugin] prepare for next development iteration

[33mcommit 65d5ed570361895425f7d88e7924c3e7e5a764b2[m
Author: app-jenkins <app-jenkins@vitam-prod-jenkins-1.internet.agri>
Date:   Thu May 19 11:59:12 2016 +0200

    [maven-release-plugin] prepare release 0.2.0-RC1

[33mcommit 65add841be5464d9cac462f40803b9a826c7f037[m
Author: Kristopher Waltzer <kristopher.waltzer.ext@culture.gouv.fr>
Date:   Thu May 19 11:37:33 2016 +0200

    Fixed : set git implementation to use as jgit

[33mcommit 41cc9ca54354d15865b1355cfbc247b7122636f6[m
Author: jgonthier <joachim.gonthier.ext@culture.gouv.fr>
Date:   Wed May 18 17:15:41 2016 +0200

    Property project.scm.id added

[33mcommit 3d287b660880ef8923f1febac12776b7c89d712d[m
Merge: 16c677f 474c3a1
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Fri May 13 17:58:27 2016 +0000

    Merge branch 'story_201-release-v1' into 'master'
    
    STORY_201 Processing Module
    
    
    
    See merge request !69

[33mcommit 474c3a1afaf571b7f6b924c626a87ba1133c7494[m
Author: Ramzi LAZREG <ramzi.lazreg.ext@culture.gouv.fr>
Date:   Fri May 13 19:47:20 2016 +0200

    STORY_201 Processing Module

[33mcommit 16c677f790960f291059706b11d72cef815b771e[m
Merge: af1095e 2ba318c
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Fri May 13 16:27:43 2016 +0000

    Merge branch 'story_257' into 'master'
    
    STORY 257 : First version of ansible deployement scripts for VITAM
    
    Première version des scripts de déploiement, pour l'instant limités à l'environnement d'intégration.
    
    See merge request !65

[33mcommit 2ba318c1df20af321b591ef9c3edca19483125b8[m
Author: Kristopher Waltzer <kristopher.waltzer.ext@culture.gouv.fr>
Date:   Fri May 13 18:15:05 2016 +0200

    STORY 257 : First version of ansible deployement scripts for VITAM

[33mcommit af1095ec3e3b23b656c4dde55a285dcea684fc23[m
Author: NGO BA TUAN <ba-tuan.ngo.ext@culture.gouv.fr>
Date:   Fri May 13 15:39:24 2016 +0200

    STORY_106 METADATA module

[33mcommit bb13ab4ac43ab3e5e29d6269138951cdda47e54f[m
Author: Yousri KOUKI <yousri.kouki.ext@culture.gouv.fr>
Date:   Fri May 13 13:24:31 2016 +0200

    STORY_119 Worksapce Module (Common, Api, Core, Rest and Client)

[33mcommit 82b18578d8e56a0ae60b0e04881c36285741e41c[m
Merge: 57b9053 8fb344e
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Wed May 11 17:04:14 2016 +0000

    Merge branch 'documentation-template' into 'master'
    
    Added documentation skeleton (for common parts of the documentation only)
    
    Ajout du squelette documentaire (notamment la partie build sphinx) tel que défini dans la réunion sur la documentation ; ajout également d'une petite documentation (README.rst) permettant de documenter la manière de builder la documentation.
    
    See merge request !49

[33mcommit 8fb344efc731f319750e04a49e015263e02c09c7[m
Author: Kristopher Waltzer <kristopher.waltzer.ext@culture.gouv.fr>
Date:   Wed May 11 14:38:36 2016 +0200

    Added documentation skeleton (for common parts of the documentation only) ; added README.rst for build information.

[33mcommit 57b9053706a9b92d4ffc3162bf78f1346d7bb28c[m
Merge: a224931 d9387d0
Author: Frederic  Bregier <frederic.bregier@culture.gouv.fr>
Date:   Tue May 3 14:01:35 2016 +0000

    Merge branch 'bug_sonar-test-coverage' into 'master'
    
    Fixed jacoco configuration : enabled agent to run
    
    Jacoco agent wasn't launched ; fixed that in the pom. For information, sonar java plugin works out of the box with sonar.
    
    For result : cf. https://dev.programmevitam.fr/sonar/overview?id=1284
    
    
    See merge request !22

[33mcommit d9387d0a619b1e97d39bf3d12f8bece98eba22ac[m
Author: Kristopher Waltzer <kristopher.waltzer@thalesgroup.com>
Date:   Tue May 3 14:53:47 2016 +0200

    Fixed OOME in tests due to jacoco ; set memory limit for surefire plugin

[33mcommit 3338e21a7261ff8be56a665fd1c005ee0b2654f2[m
Author: Kristopher Waltzer <kristopher.waltzer@thalesgroup.com>
Date:   Tue May 3 14:17:44 2016 +0200

    Fixed jacoco configuration : enabled agent to run

[33mcommit a22493156b6066e40609a0dd30755756c16f84ba[m
Author: Yousri KOUKI <yousri.kouki.ext@culture.gouv.fr>
Date:   Tue May 3 13:38:19 2016 +0200

    Initial commit
