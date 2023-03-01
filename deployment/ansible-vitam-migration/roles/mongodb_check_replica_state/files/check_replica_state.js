const rsStatus = rs.status();
if (rsStatus.ok){
    // https://www.mongodb.com/docs/manual/reference/replica-states/#replica-set-member-states
    if(rsStatus.myState === 2) {
        print("OK: node is SECONDARY !");
    } else if(rsStatus.myState === 1) {
        print("OK: node is PRIMARY !");
    } else if(rsStatus.myState === 7) {
        print("OK: node is ARBITER !");
    } else {
        printjson(rsStatus);
        throw "node is in wrong state (" + rsStatus.myState + ") !";
    }
} else {
    printjson(rsStatus);
    throw "Cannot check mongod replica set status.";
}
