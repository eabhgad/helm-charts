# Cassandra Helm Chart
###Objective: 
To deploy Cassandra database cluster as a Helm chart using StatefulSet.

### Pre-requisite:
-	The Cassandra data is stored on a network storage
-	The NFS shared folder `/mnt/nfs_share/` is used as a network storage which is accessilble from all the hosts in the k8s cluster

### Architecture

![Cassandra Cluster](https://github.com/eabhgad/helm-charts/blob/0.2/images/CassandraCluster_STS_v1.png "Cassandra Cluster")

### Kubernetes / Helm Chart Manifests:

##### StatefulSet
-	*initContainers*
	The initContainers is allows us to make sure that the user (OS non root user) used in Cassandra image has required permissions to write data to the Volume  
-	*containers*
	-	The container image used is [bitnami/cassandra](https://hub.docker.com/r/bitnami/cassandra) version / tag 5.0
		
-	*volumeClaimTemplates*
	These are converted to volume claims by the controller and mounted at the paths mentioned in the manifest file. Do not use these in production.

##### Headless Service
-	Automatically creates DNS records (A or AAAA) for each pod associated with the service.
-	This allows individual pods within the service to be accessed directly by their DNS names, without needing a load balancer proxying traffic.
-	DNS names take the form `<pod-name>.<headless-service-name>.<namespace>.svc.cluster.local`
For example, 
`cassandra-0.cassandra.default.svc.cluster.local`
where,
Pod name -> `cassandra-0` 
Service name -> `cassandra`

### Configuration:

Please refer to `values.yaml` file for configurable parameters.

### Install Chart:
Go to working directory:

`$ cd /home/eabhgad/workspace/git/helm-charts`

Validate if all the manifests before installation:

`helm install cassandra5 ./cassandra-chart --dry-run`
where,
`cassandra5`  = Release name

Install the chart:

`helm install cassandra5 ./cassandra-chart`

Check the status of Chart (along with the associated resources):

`helm status cassandra5 --show-resources`

### Verify Installation:
Verify that all the required objects are created:

`kubectl get pod -l=app=cassandra`

Check the logs of Cassandra Pods:

`kubectl logs cassandra-0`
`kubectl logs cassandra-1`

Check the status of Cassandra cluster using *nodetool*:

`kubectl exec -it cassandra-0 -- nodetool status`

To access the Cassandra DB using CQLSH from the host network,
-	Forward container port 9042 to port 9042 on host:

	`kubectl port-forward pod/cassandra-0 9042:9042`

-	Use CQLSH client to connect to Cassandra DB:

	`cqlsh -u cassandra -p cassandra 192.168.30.3`

### Cleanup:
To delete all the objects created by the chart
`helm delete cassandra5`


##Issues and Fixes:

### Issue:  ConnectTimeoutException: connection timed out after 2000 ms: /10.10.3.72:7000

####Description:
The issue occured when installing the Cassandra chart. The chart was installed successfully on previous daya and it was deleted / cleaned up.

When installing the Cassandra chart following error is shown in the cassandra-0 Pod.

io.netty.channel.ConnectTimeoutException: connection timed out after 2000 ms: /10.10.3.72:7000
```
WARN  [Messaging-EventLoop-3-1] 2025-04-21 14:11:37,991 NoSpamLogger.java:107 - cassandra-0/10.10.231.253:7000->/10.10.3.72:7000-SMALL_MESSAGES-[no-channel] dropping message of type PING_REQ whose timeout expired before reaching the network
INFO  [Messaging-EventLoop-3-3] 2025-04-21 14:11:53,711 NoSpamLogger.java:104 - cassandra-0/10.10.231.253:7000->/10.10.3.72:7000-URGENT_MESSAGES-[no-channel] failed to connect
io.netty.channel.ConnectTimeoutException: connection timed out after 2000 ms: /10.10.3.72:7000
        at io.netty.channel.epoll.AbstractEpollChannel$AbstractEpollUnsafe$2.run(AbstractEpollChannel.java:615)
        at io.netty.util.concurrent.PromiseTask.runTask(PromiseTask.java:98)
        at io.netty.util.concurrent.ScheduledFutureTask.run(ScheduledFutureTask.java:156)
        at io.netty.util.concurrent.AbstractEventExecutor.runTask(AbstractEventExecutor.java:173)
        at io.netty.util.concurrent.AbstractEventExecutor.safeExecute(AbstractEventExecutor.java:166)
        at io.netty.util.concurrent.SingleThreadEventExecutor.runAllTasks(SingleThreadEventExecutor.java:472)
        at io.netty.channel.epoll.EpollEventLoop.run(EpollEventLoop.java:408)
        at io.netty.util.concurrent.SingleThreadEventExecutor$4.run(SingleThreadEventExecutor.java:998)
        at io.netty.util.internal.ThreadExecutorMap$2.run(ThreadExecutorMap.java:74)
        at io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
        at java.base/java.lang.Thread.run(Unknown Source)
INFO  [Messaging-EventLoop-3-3] 2025-04-21 14:12:23,794 NoSpamLogger.java:104 - cassandra-0/10.10.231.253:7000->/10.10.3.72:7000-URGENT_MESSAGES-[no-channel] failed to connect
```
#### References:
https://dba.stackexchange.com/questions/316883/cassandra-nodes-removal-most-appropriate-strategy

#### Root-cause:

https://dba.stackexchange.com/questions/316883/cassandra-nodes-removal-most-appropriate-strategy
> "this issue isn't uncommon in a Kubernetes paradigm. The cluster will "remember" the IPs of old nodes for up to 72 hours after they've been removed or decommissioned...and sometimes just won't "forget" about them for longer than that. You can get rid of it by running:"

That means the Cassandra DB remembers the nodes and their token ranges even after the cluster is deleted. As a result, when a new cluster is deployed, with the same data (volume mount), it will try to establish the internode connectivity between the nodes which will fail. (**Is this assumption correct?**)

#### Fix:
Try deleting the dead node from the cluster and restart the Pod 

Reference:
-	https://docs.strangebee.com/thehive/operations/cassandra-cluster/#removing-a-dead-node-from-a-cassandra-cluster
-	https://stackoverflow.com/questions/8589938/how-to-remove-dead-node-out-of-the-cassandra-cluster

Login to the running Cassandra Pod:
`kubectl exec -it cassandra-0 -- bash`
Check which node is dead / down : 
`$ nodetool status`
Remove the dead node using Host ID / Node ID:
`$ nodetool removenode <node-id>`


