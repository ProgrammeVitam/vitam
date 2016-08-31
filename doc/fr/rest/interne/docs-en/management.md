Management API
===============

Management API provides entry points and methods to manage various transactions, except ingest ones.

----------
Freezes Entry Point
-------------
Freezes is the entry point for the freeze process. It contains the very same access as in Units, but in Creation/Delete mode.

No operation can change the content but they only apply to the addition or the removal or any Units or Objects.


Destructions Entry Point
-------------
Destructions is the entry point for the destruction process. It contains the very same access as in Units, but in Creation/Update/Delete mode.

No operation can change the content but they only apply to the addition or the removal or any Units or Objects, except on updating archivistic (management) metadata.


Transformations Entry Point
-------------
Transformations is the entry point for all asynchronous transformations tasks.


Transfers Entry Point
-------------
Transfers is the entry point to seek information from transfers using other API (like FTP or Waarp).


Formats Entry Point
-------------
Formats is the entry point to reach in read or update only the various formats.


Transformations Entry Point
-------------
Transformations is the entry point under **Formats** to reach in read only the various transformations rules from that particular format.


Logbooks Entry Point
-------------
Logbooks is the entry point for the Logbooks access.
