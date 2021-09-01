## Reproducer for https://github.com/akka/akka-projection/issues/586

This is a strip down version of the shopping cart sample from the platform guide.

It has one projeciton and one ticker actor running with sharded daemon.

After starting a second node, we should see some of the sharded daemons stopping on the first node and starting on the second node. 
Instead we see:

* ticker behavior stopping with PoisonPill and message "Maybe the handOffStopMessage [akka.actor.typed.internal.PoisonPill$] is not handled? Waiting additional [55 seconds] before stopping the remaining entities."
* no similar message is shown for the projection daemon.
* ticker restart on new node
* projection start on new node - at that point we have the same projection running in two nodes (the one in first node never gets the PoisonPill)