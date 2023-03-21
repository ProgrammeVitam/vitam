let i = 0;
while (true) {

    dbList = db.adminCommand({listDatabases: 1});

    if (dbList.ok) {
        print("OK: mongos connection success.");
        break;
    } else {
        if (i++ === 60) {
            printjson(dbList);
            throw "Cannot connect through mongos.";
        }
    }

    print("Info: Not ready yet...");
    sleep(1000);

}
