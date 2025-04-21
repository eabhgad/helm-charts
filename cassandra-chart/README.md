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
