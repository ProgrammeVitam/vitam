Access API
==========

Access API provides entry points and methods to reach, request and retrieve information from Units or from Objects.

-------------
Units Entry Point
-------------

Units is the entry point for all archive descriptions. They contain descriptive metadata and archivistic (management) metadata.

One Unit could be either a simple folder (as in a classification schema) or one item description.
      
As of SEDA model, it is equivalent to an ArchiveUnit. As of Isad(G) / EAD, it is equivalent to a Description Unit.
      
At must one archive is attached to one Unit, meaning that if a Unit should have more than one archive attached, you have to specify sub-Units to the main one, each sub-Unit having one archive attached.
      
No delete is allowed, neither full update (only partial update through PATCH command).

-------------
Warning : API interne : Test Add RequestResponseOk
-------------

It's not allowed to add the request Response Ok including hints ,queries in End Points of External or internal Access Module.
The final Result will encapsulate the response and made it  untreatable on behalf of Front end application.
For example : {"$hits":{"total":3,"offset":0,"limit":125,"size":3},"$results":[{"$hits":{"total":3 
The same impact is visualized at the level of LogBook External and LogBook Internal during the functional test .

-------------
Objects Entry Point
-------------

Objects is the entry point for all binary archives but also non binary ones (as of reference to physical ones or remotes one). They contain technical metadata.

As of SEDA model, it is equivalent to one DataObject (binary or physical). As of EAD, it is equivalent to one Digital Archive Object.
      
Each object must be attached to at least one parent Unit.
      
Only access (GET) is allowed.
