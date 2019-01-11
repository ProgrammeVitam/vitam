Workflow d'import d'un référentiel des formats
##############################################

Introduction
============

Cette section décrit le processus (workflow) permettant d'importer un référentiel des formats

Processus d'import d'un référentiel de formats (vision métier)
==============================================================

Le processus d'import du référentiel des formats contrôle que les informations sont formalisées de la bonne manière dans le fichier soumis à la solution logicielle Vitam et que chaque format contient bien le nombre d'informations minimales attendues. Tous les éléments réalisés au cours de ce processus sont exécutés dans une seule étape.

Import d'un référentiel de formats REFERENTIAL_FORMAT_IMPORT (ReferentialFormatFileImpl)
--------------------------------------------------------------------------------------------

* Vérification du fichier de référentiel des formats


  + **Règle** : le fichier doit être au format xml et respecter le formalisme du référentiel PRONOM publié par the National Archives (UK)

  + **Type** : bloquant

  + **Statuts** :

    - OK : les informations correspondant à chaque format sont décrites comme dans le référentiel PRONOM (STP_REFERENTIAL_FORMAT_IMPORT.OK = Succès du processus d'import du référentiel des formats)

    - KO : la condition ci-dessus n'est pas respectée (STP_REFERENTIAL_FORMAT_IMPORT.KO = Échec du processus d'import du référentiel des formats)
   
    - WARNING : la version du référentiel PRONOM est validée mais un avertissement signale que la version du référentiel est antérieure à celle présente dans le logiciel ou que celle-ci est identique (STP_REFERENTIAL_FORMAT_IMPORT.WARNING = Avertissement lors du processus d'import du référentiel des formats version (n°) du fichier de signature PRONOM)
    
    - FATAL : une erreur technique est survenue lors de l'import du référentiel des formats (STP_REFERENTIAL_FORMAT_IMPORT.FATAL = Erreur technique lors du processus d'import du référentiel des formats)


Processus de sauvegarde du référentiel des formats BACKUP_REFERENTIAL_FORMAT 
------------------------------------------------------------------------------


  + **Règle** : sauvegarde du référentiel des formats 
  
  + **Type** : bloquant

  + **Statuts** :

    - OK : la sauvegarde du référentiel des formats a bien été effectuée (STP_BACKUP_REFERENTIAL_FORMAT.OK = Succès du processus de sauvegarde du référentiel des formats)

    - KO : la sauvegarde du référentiel des formats n'a pas été effectuée (STP_BACKUP_REFERENTIAL_FORMAT.KO = Échec du processus de sauvegarde du référentiel des formats)

    - FATAL : une erreur technique est survenue lors de la sauvegarde du référentiel des formats (STP_BACKUP_REFERENTIAL_FORMAT.FATAL = Erreur technique lors du processus de sauvegarde du référentiel des formats)


Structure du rapport d'import d'un référentiel des formats
==========================================================

Lorsqu'un nouveau référentiel est importé, la solution logicielle Vitam génère un rapport de l'opération. Ce rapport est en plusieurs parties :

  - "Operation" contient :

    * evType : le type d'opération. Dans le cadre de ce rapport, il s'agit toujours de "STP_REFERENTIAL_FORMAT_IMPORT"
    * evDateTime : la date et l'heure de l'opération d'import
    * evId : l'identifiant de l'opération

  - "StatusCode" : le status de l'opération OK, KO, WARNING
  - "PreviousPronomVersion" : le numéro de la version précédement installée  dans le référentiel 
  - "PreviousPronomCreationDate" : la date de la version précédement installée  dans le référentiel 
  - "NewPronomVersion" : le numéro de version désormais installée 
  - "NewPronomCreationDate" : la date de la version désormais installée 
  - "RemovedPUIDs" : la liste des PUIID qui ont été supprimés 
  - "AddedPUID" : la liste des PUIID qui ont été ajoutés 
  - "UpdatedPUIDs" : la liste des PUIID qui ont été mis à jour  
 
  - "Warnings" : le message concernant le warning : le référentiel des formats importé est plus ancien que la version présente dans la solution, le référentiel des formats importé est identique à la version qui été disponible dans la solution. 

exemple :

.. code-block:: json

  "Operation" : {
      "evType" : "STP_REFERENTIAL_FORMAT_IMPORT",
      "evDateTime" : "2018-12-11T14:46:43.856",
      "evId" : "aeeaaaaaaghg7kglaboimalhtw57z7qaaaaq"
    },
    "StatusCode" : "WARNING",
    "PreviousPronomVersion" : "94",
    "PreviousPronomCreationDate" : "2018-09-17T12:54:53.000",
    "NewPronomVersion" : "88",
    "NewPronomCreationDate" : "2016-09-27T15:37:53.000",
    "RemovedPUIDs" : [ "fmt/1216", "fmt/1217", "fmt/1214", "fmt/1215", "fmt/1212", "fmt/1213", "fmt/1210", "fmt/1211", "fmt/1108", "fmt/1109", "fmt/1106", "fmt/1107", "fmt/1104", "fmt/1105", "fmt/1102", "fmt/1103", "fmt/1100", "fmt/1101", "fmt/985", "fmt/986", "fmt/987", "fmt/988", "fmt/981", "fmt/982", "fmt/983", "fmt/984", "fmt/989", "fmt/980", "fmt/975", "fmt/976", "fmt/977", "fmt/978", "fmt/979", "fmt/1209", "fmt/1207", "fmt/1208", "fmt/1205", "fmt/1206", "fmt/1203", "fmt/1204", "fmt/1201", "fmt/1202", "fmt/1200", "fmt/1140", "fmt/1020", "fmt/1141", "fmt/1018", "fmt/1139", "fmt/1019", "fmt/1016", "fmt/1137", "fmt/1017", "fmt/1138", "fmt/1014", "fmt/1135", "fmt/1015", "fmt/1136", "fmt/1012", "fmt/1133", "fmt/1013", "fmt/1134", "fmt/1010", "fmt/1131", "fmt/1011", "fmt/1132", "fmt/1030", "fmt/1151", "fmt/996", "fmt/1031", "fmt/1152", "fmt/997", "fmt/998", "fmt/1150", "fmt/999", "fmt/992", "fmt/993", "fmt/994", "fmt/995", "fmt/1029", "fmt/1027", "fmt/1148", "fmt/1028", "fmt/1149", "fmt/1025", "fmt/1146", "fmt/990", "fmt/1026", "fmt/1147", "fmt/991", "fmt/1023", "fmt/1144", "fmt/1024", "fmt/1145", "fmt/1021", "fmt/1142", "fmt/1022", "fmt/1143", "fmt/1119", "fmt/1117", "fmt/1118", "fmt/1115", "fmt/1116", "fmt/1113", "fmt/1114", "fmt/1111", "fmt/1112", "fmt/1110", "fmt/1130", "fmt/1009", "fmt/1007", "fmt/1128", "fmt/1008", "fmt/1129", "fmt/1005", "fmt/1126", "fmt/1006", "fmt/1127", "fmt/1003", "fmt/1124", "fmt/1004", "fmt/1125", "fmt/1001", "fmt/1122", "fmt/1002", "fmt/1123", "fmt/1120", "fmt/1000", "fmt/1121", "fmt/1063", "fmt/1184", "fmt/1064", "fmt/1185", "fmt/1061", "fmt/206", "fmt/1182", "fmt/1062", "fmt/1183", "fmt/1180", "fmt/1060", "fmt/1181", "fmt/1058", "fmt/1179", "fmt/1059", "fmt/1056", "fmt/1177", "fmt/1057", "fmt/1178", "fmt/1054", "fmt/1175", "fmt/1055", "fmt/1176", "fmt/1074", "fmt/1195", "fmt/1075", "fmt/1196", "fmt/1072", "fmt/1193", "fmt/1073", "fmt/1194", "fmt/1070", "fmt/1191", "fmt/1071", "fmt/1192", "fmt/1190", "fmt/1069", "fmt/1067", "fmt/1188", "fmt/1068", "fmt/1189", "fmt/1065", "fmt/1186", "fmt/1066", "fmt/1187", "fmt/1041", "fmt/1162", "fmt/1042", "fmt/1163", "fmt/1160", "fmt/1040", "fmt/1161", "fmt/1038", "fmt/1159", "fmt/1039", "fmt/1036", "fmt/1157", "fmt/1037", "fmt/1158", "fmt/1034", "fmt/1155", "fmt/1035", "fmt/1156", "fmt/1032", "fmt/1153", "fmt/1033", "fmt/1154", "fmt/1052", "fmt/1173", "fmt/1053", "fmt/1174", "fmt/1050", "fmt/1171", "fmt/1051", "fmt/1172", "fmt/1170", "fmt/1049", "fmt/1047", "fmt/1168", "fmt/1048", "fmt/1169", "fmt/1045", "fmt/1166", "fmt/1046", "fmt/1167", "fmt/1043", "fmt/1164", "fmt/1044", "fmt/1165", "fmt/1098", "fmt/1099", "fmt/1085", "fmt/1086", "fmt/1083", "fmt/1084", "fmt/1081", "fmt/1082", "fmt/1080", "fmt/1078", "fmt/1199", "fmt/1079", "fmt/1076", "fmt/1197", "fmt/1077", "fmt/1198", "fmt/1096", "fmt/1097", "fmt/1094", "fmt/1095", "fmt/1092", "fmt/1093", "fmt/1090", "fmt/1091", "fmt/1089", "fmt/1087", "fmt/1088" ],
    "AddedPUIDs" : [ ],
    "UpdatedPUIDs" : {
      "fmt/563" : [ "+  MimeType : ", "-  MimeType : application/postscript" ],
      "fmt/641" : [ "+  HasPriorityOverFileFormatID : [ fmt/154 ]", "-  HasPriorityOverFileFormatID : [ fmt/154, fmt/353 ]" ],
      "fmt/245" : [ "+  Extension : [ \\n         ]", "-  Extension : [ ]" ],
      "fmt/899" : [ "+  MimeType : application/octet-stream", "-  MimeType : application/vnd.microsoft.portable-executable" ],
      "x-fmt/430" : [ "+  Extension : [ msg ]", "-  Extension : [ msg, oft ]" ],
      "fmt/417" : [ "+  MimeType : ", "-  MimeType : application/postscript" ],
      "fmt/616" : [ "+  MimeType : application/font-woff", "+  Version : ", "-  MimeType : font/woff", "-  Version : 1.0" ],
      "fmt/418" : [ "+  MimeType : ", "-  MimeType : application/postscript" ],
      "fmt/419" : [ "+  MimeType : ", "-  MimeType : application/postscript" ],
      "fmt/570" : [ "+  HasPriorityOverFileFormatID : [ ]", "-  HasPriorityOverFileFormatID : [ fmt/986 ]" ]
    },
    "Warnings" : [ "New imported referential version 94 is older than previous referential version 88", "New imported referential date 2016-09-27T15:37:53.000 is older than previous report date 2018-09-17T12:54:53.000", "244 puids removed." ]
  }
