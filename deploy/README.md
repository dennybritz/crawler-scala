### Deployment

We use [Docker](docker.com) and [CoreOS](https://coreos.com/) for our deployment. This allows us to deploy our application on any kind of platform, private or public cloud. AWS and DigitalOcean provide pre-built images for CoreUS. This makes deployment on these platforms easy.

The Dockerfile for the crawler is in the `docker/` directory. It is based on CentOS 7, installs all package dependencies, pull the latet code from github, compiles it, and runs the crawler application. Note that it uses our private SSH keys which are not contained in this repository.


### CoreOS Setup

The CoreOS [fleet](https://coreos.com/docs/launching-containers/launching/launching-containers-fleet) configuration consists of the following services:

- `blikk-crawler`: The crawler application. Uses the blikk-crawler docker image.
- `blikk-crawler-discovery`: A discovery service that writes the IP address and port to [etcd](https://coreos.com/docs/distributed-configuration/getting-started-with-etcd). This data is used new nodes to discovery an existing crawling cluster. The discovery service is started together with the crawler.
- `redis`:  A local redis instance, started together with the crawler. Uses the dockerfile/redis docker image.

Given taht, a new CoreOS cluster could be setup as follows:

    # Login to docker. Generates a .dockercfg file that is needed to 
    # pull private docker repositories on this machines
    docker login

    # Verify the cluster us running
    fleetctl list-machines
    
    # Submit the unit file templates
    fleetctl submit blikk-crawler@.service blikk-crawler-discovery@.service redis@.service
    fleetctl list-unit-files
    
    # Load the units onto a server
    fleetctl load blikk-crawler@10000.service blikk-crawler-discovery@10000.service redis@10000.service
    
    # Start the units
    fleetctl start redis@10000.service
    fleetctl start blikk-crawler@10000.service
    fleetctl list-units
    fleetctl journal -f blikk-crawler@10000.service

    # When done, stop the units
    fleetctl stop blikk-crawler@10000.service
    fleetctl list-units

    # Destroy the units
    fleetctl destroy blikk-crawler@10000.service blikk-crawler-discovery@10000.service redis@10000.service
    fleetctl unload blikk-crawler@10000.service blikk-crawler-discovery@10000.service redis@10000.service

    # Destroy the templates
    fleetctl destroy blikk-crawler@.service blikk-crawler-discovery@.service redis@.service

