// https://www.mongodb.com/docs/manual/tutorial/manage-sharded-cluster-balancer/#disable-the-balancer

// Script to stop the mongodb balancer
sh.stopBalancer();
// Check if the balancer is properly stopped
if (sh.getBalancerState()) {
    print("Failed to stop the balancer");
    quit(1);
}
// Wait until the balancer is stopped
// The timeout is managed by ansible, not by the script
while (sh.isBalancerRunning()) {
    sleep(1000);
}
