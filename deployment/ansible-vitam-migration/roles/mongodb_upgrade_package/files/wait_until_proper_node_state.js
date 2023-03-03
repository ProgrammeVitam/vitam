let i = 0;
while (true) {

    const rsStatus = rs.status();
    if (rsStatus.ok){
        // https://www.mongodb.com/docs/manual/reference/replica-states/#replica-set-member-states
        if(rsStatus.myState === 2) {
            print("OK: node is SECONDARY !");
            break;
        } else if(rsStatus.myState === 1) {
            print("OK: node is PRIMARY !");
            break;
        } else if(rsStatus.myState === 7) {
            print("OK: node is ARBITER !");
            break;
        }
    }

    if (i++ === 60) {
        printjson(rsStatus);
        throw "Cannot check mongod replica set status.";
    }

    print("Info: Not ready yet...");
    sleep(1000);

}
