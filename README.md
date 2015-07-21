## Akka Service Actor Registry ##

Table of Contents

 * [What is it](#what)
 * [Description](#description)
 * [Why we made it](#why)
 * [How we made it](#how)
 * [Usage](#usage)
 * [QA](#qa)
 * [Operations](#ops)
 * [Resources](#resources)


<a name="what">
### What is it? ###
</a>
A microservice actor discovery service for Akka clusters.

<a name="description">
Description
--------
</a>
The Akka Service Actor Registry is a cluster singleton that is used to discover microservice actors across an Akka cluster.  It serves as a dependency injection mechanism that asychronously and dynamically wires together dependencies between service consumers and service implementor actors.  

Subscriber actors interact with the Akka Service Actor Registry asking for dependent service actors.  Publisher actors interact with the Akka Service Actor Registry informing the registry of their availability.  When publisher actors register their availability, subscriber actors are delivered their endpoint references who in turn publish their availability as dependencies are supplied.  In this way, service actor availability cascades across the cluster.  

Publisher actors withdraw their availability by:

1. declaratively informing the registry of unavailability in response to tripped circuit breakers to outside web services 
2. are deathwatch informed of termination after supervisior recovery max re-tries are exceeded
3. or, are deathwatch informated of termination when their hosting cluster node fails

As publisher actors are withdrawn, the registry informs subscriber actors of their unavailability.  This cascades across the cluster as dependent actors are denied their dependencies which in turn causes the dependent actors to become unavailable themselves. 

When withdrawn service actors are re-introduced, subscribers are re-delivered their endpoint references and the overall system self-heals.

The registy cluster singleton is itself resilient to failure as it moves and recovers in response to hosting node failure.  This functionality is provided by the registry being persistent and informing publishers and subscribers of registry restart.  

<a name="why">
### Why'd we make it? ###
</a>
We needed a way to manage dependencies to and between actors that implement micro-services across an Akka cluster.

<a name="how">
### Design considerations ###
</a>
Microservices in an Akka Cluster are implemented as Actors running in specific cluster nodes.

Traditional service discovery mechanims such as Etcd and Consul are suitable for web services - not actor references.

Service discovery mechanisms typically rely on clients polling for dependent service availability.  Actors enable a "call-back" style interaction that is asynchronous and dynamic.  We chose to leverage this capability in the Akka Service Actor Registry.

<a name="usage">
Usage
--------
</a>
Actor protocol:

<a> Service Implementor:  </a>

Service implementor sends to ServiceRegistry when transitions to online.
   
	case class PublishService(serviceName: String, serviceEndpoint: ActorRef)
 
Service implementor sends to ServiceRegistry when transitions to offline.
  
	case class UnPublishService(serviceName: String)

<a> Service client:  </a>

Service client sends to ServiceRegistry when requiring dependent service.

   	case class SubscribeToService(serviceName: String)

Service client sends to ServiceRegistry when no longer requiring dependent service.

  	case class UnSubscribeToService(serviceName: String)

<a> Akka Service Actor Registry:  </a>

ServiceRegistry sends to service client when subscribed to service is now online.

   	case class ServiceAvailable(serviceName: String, serviceEndpoint: ActorRef)

ServiceRegistry sends to service client when subscribed to service is now offline.

   	case class ServiceUnAvailable(serviceName: String)

ServiceRegistry sends to publishers and subscribers when ServiceRegistry has been restarted requiring all participants to re-subscribe and re-publish.

   	case class RegistryHasRestarted(registry: ActorRef)

**Upon registry restart, all publishers and subsribers must re-publish and re-subscribe to the service registry.  This was implemented this way in order to avoid race conditions where failed registry hosting cluster node also was hosting service actors that failed at the same time.**

<a name="ops">
Operations
--------
</a>

Configure akka persistence journal and snapshot stores in your application.conf.

Logs to ActorLogging.  Configure appropriately.

<a name="qa">
Quality Assurance
--------
</a>
See unit test: `com.comcast.csv.akka.serviceregistry.TestServiceRegistry`

Automated cluster recovery test is not yet developed. Have tested it manually by: 

1. Stopping node that is hosting the registry, observing it restarting on another node and sending RegistryHasRestarted messages to all previous publishers and subscribers.
2. Stopping a service actor hosting node, observing the registry dispatching ServiceUnavailable messages to all of its subscribers.
3. Re-starting a stopped service actor hosting node, observing the registry dispatching ServiceAvailable messages to its subscribers.

<a name="resources">
Resources
--------
</a>

 1. tbd