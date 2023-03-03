dbList = db.adminCommand({listDatabases: 1});

if (!dbList.ok) {
    printjson(dbList);
    throw "Cannot connect through mongos.";
}

print("OK: mongos connection success.");
