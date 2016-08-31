Administration  API
======================

Admin API provides entry points and methods to manage the system, except IaaS/PaaS aspects.

----------
Contracts Entry Point
-------------
Access\_contracts, Ingest\_contracts and Preservation\_contracts are the entry point for all contrats.


Authentications and Authorizations Entry Point
--------------------
Users, Roles and Profiles are the entry point for all authentications and authorizations.


Formats Entry Point
-------------
Formats is the entry point to manage formats.


Transformations Entry Point
-------------
Transformations is the entry point under **Formats** to manage the various transformations rules from that particular format.


Transfers Entry Point
-------------
Transfers is the entry point to manage transfers using other API (like FTP or Waarp).


Configuration Entry Point
-------------
Configuration is the entry point to manage global Configuration.


Access Groups Entry Point
-------------
Access Groups is the entry point to manage Access Groups (collection of ingest contracts plus roots).


Consolidated Storages Entry Point
-------------
Consolidated Storages is the entry point to manage Consolidated Storage Offers (group of consistent storage offers as 'Conservation', 'Conservation secured', 'Diffusion', 'Diffusion secured').


Consolidated Transformations Entry Point
-------------
Consolidated Transformations is the entry point to manage Consolidated Transformation Offers (group of consistent transformation rules all together as 'Conservation', 'Diffusion', 'Raw', 'Diffusion Thumbnail', ...).
