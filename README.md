## Akka Service Registry ##

Table of Contents

 * [What is it](#what)
 * [Description](#description)
 * [Why we made it](#why)
 * [Design Considerations](#how)
 * [Usage](#usage)
 * [QA](#qa)
 * [Operations](#ops)
 * [Resources](#resources)


<a name="what">
What is it?
--------
</a>
A microservice actor discovery service for Akka clusters.

<a name="description">
Description
--------
</a>
The Akka Service Registry is a cluster singleton that is used to discover and resolve references to microservice actors across an Akka cluster.  It serves as a dependency injection mechanism that asychronously and dynamically wires together dependencies between service consumers and service implementor actors.  

Subscriber actors interact with the Akka Service Registry asking for dependent service actors.  Publisher actors interact with the Akka Service Registry informing the registry of their availability.  When publisher actors register their availability, subscriber actors are delivered their endpoint references who in turn publish their availability as their dependencies are supplied.  In this way, service actor availability cascades across the cluster.  

Publisher actors withdraw their availability by:


1. declaratively informing the registry of unavailability in response to being told by the registry that one or more of its critical dependents are unavailable
2. declaratively informing the registry of unavailability in response to tripped circuit breakers to critical outside web services 
2. being deathwatch informed to the registry of termination after supervisior recovery max re-tries are exceeded
3. being deathwatch informed to the registry of termination when their hosting cluster node fails

As publisher actors are withdrawn, the registry informs subscriber actors of their unavailability.  This cascades across the cluster as dependent actors are denied their dependencies which in turn causes the dependent actors to become unavailable themselves. 

When withdrawn service actors are re-introduced, subscribers are re-delivered their endpoint references and the  system self-heals into available service states.

The registy cluster singleton is itself resilient to failure as it moves and recovers in response to hosting node failure.  This functionality is provided by the registry being persistent and informing publishers and subscribers of registry restart.  

<a name="why">
Why'd we make it?
-------
</a>
We needed a way to manage dependencies to and between actors that implement or encapsulate access to microservices across an Akka cluster.

<a name="how">
Design considerations
----------
</a>
Microservices in an Akka Cluster are implemented as actors running in specific cluster nodes.  The protocols to these microservice are not web service json payloads over http but are Akka-remoted serialized messages in the traditional Akka way.

Traditional service discovery mechanisms such as Etcd and Consul are suitable for web services - not actor references.

Service discovery mechanisms typically rely on clients polling for dependent service availability.  Actors enable a "call-back" interaction style that is asynchronous and dynamic.  We chose to leverage this capability in the Akka Service Registry.

<a name="usage">
Usage
--------
</a>

Add a reference to the Akka Service Registry in your cluster node project build.sbt:

	tbd

Create the proxy to the Service Registry Singleton in your cluster node main method. Pass the reference into your service initializers.

	object UserServiceNode {

	  def main(args: Array[String]): Unit = {

        // Override the configuration of the port when specified as program argument
        val port = if (args.isEmpty) "0" else args(0)
        val config = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$port").
          withFallback(ConfigFactory.parseString("akka.cluster.roles = [userService]")).
          withFallback(ConfigFactory.load())

        val system = ActorSystem("ClusterSystem", config)

        val registry = system.actorOf(ClusterSingletonProxy.props(
          singletonPath = "/user/singleton/registry",
          role = None),
          name = "registryProxy")

        val userService = system.actorOf(Props[UserServiceEndpoint], UserService.endpointName)
        userService ! InitializeUserServiceEndpoint(registry)

        val cloudAuthService = system.actorOf(Props[CloudAuthServiceEndpoint], CloudAuthService.endpointName)
        cloudAuthService ! InitializeCloudAuthServiceEndpoint(registry)
	  }
	}

In your service actor receive method tell the registry your subscriptions and then field ServiceAvailable messages from it.  After the dependencies have been delivered tell the registry you are available by sending a PublishService message.  

FSM implementors should begin in an offline state and then transition to online after the dependencies have been delivered.

	class CloudAuthServiceEndpoint extends Actor with ActorLogging {

  	  import AuthorizationProtocol._
  	  import CloudAuthServiceEndpointInternalProtocol._
  	  import CloudAuthServiceProtocol._

  	  var registry: Option[ActorRef] = None
	  var userService: Option[ActorRef] = None

  	  def receive = {

        case init: InitializeCloudAuthServiceEndpoint =>
          registry = Option(init.registry)
          registry.foreach(r => r ! SubscribeToService(UserService.endpointName))

        case registryHasRestarted: RegistryHasRestarted =>
          registry = Option(registryHasRestarted.registry)
          userService = None
          registry.foreach(r => r ! SubscribeToService(UserService.endpointName))

        case sa: ServiceAvailable =>
          sa.serviceName match {
            case UserService.endpointName =>
              userService = Option(sa.serviceEndpoint)
              registry.foreach(r => 
                r ! PublishService(serviceName = CloudAuthService.endpointName, serviceEndpoint = self))
            case unknownService =>
              log.error(s"received ServiceAvailable for unknown service: $unknownService")
          }

        case sua: ServiceUnAvailable =>
          sua.serviceName match {
            case UserService.endpointName =>
              registry.foreach(r => r ! UnPublishService(serviceName = CloudAuthService.endpointName))
              userService = None
            case unknownService =>
              log.error(s"received ServiceUnAvailable for unknown service: $unknownService")
      }
	}
	

### Actor protocol: ###

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

<a> Akka Service Registry:  </a>

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

Configure Akka persistence journal and snapshot stores in your application.conf.

Logs to ActorLogging.  Configure appropriately.

<a name="qa">
Quality Assurance
--------
</a>
See unit test: `com.comcast.csv.akka.serviceregistry.TestServiceRegistry`

Automated cluster level tests and in particular service and registry recovery testing is not yet developed. Have tested service and registry recovery manually by: 

1. Stopping cluster node that is hosting the registry, observing it restarting on another node and sending RegistryHasRestarted messages to all previous publishers and subscribers.
2. Stopping a service actor hosting node, observing the registry dispatching ServiceUnavailable messages to all of its subscribers.
3. Re-starting a stopped service actor hosting node, observing the registry dispatching ServiceAvailable messages to its subscribers.

<a name="resources">
Resources
--------
</a>

 1. tbd