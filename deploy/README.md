### Deployment

We use [Docker](docker.com) and [CoreOS](https://coreos.com/) for our deployment. This allows us to deploy our application on any kind of platform, private or public cloud. AWS and DigitalOcean provide pre-built images for CoreUS. This makes deployment on these platforms easy.

The Dockerfile for the crawler is in the `docker/` directory. It is based on CentOS 7, installs all package dependencies, pull the latet code from github, compiles it, and runs the crawler application. Note that it uses our private SSH keys which are not contained in this repository.



### CoreOS Setup



For example, a new CoreOS cluster can be setup as follows:

    # Login to docker
    docker login

    # Verify cluster
    fleetctl list-machines
    # Submit unit file templates
    fleetctl submit blikk-crawler@.service blikk-crawler-discovery@.service redisy@.service
    fleetctl list-unit-files
    
    # Load the units onto a server
    fleetctl load blikk-crawler@8081.service blikk-crawler-discovery@8081.service redis@8081.service
    
    # Start the units
    fleetctl start blikk-crawler@8081.service
    fleetctl list-units
    fleetctl journal blikk-crawler@8081.service

    # Stop the unit
    fleetctl stop blikk-crawler@8081.service
    fleetctl list-units

    # Destroy the units
    fleetctl destroy blikk-crawler@8081.service blikk-crawler-discovery@8081.service redis@8081.service

    # Destroy the templates
    fleetctl destroy blikk-crawler@.service blikk-crawler-discovery@.service redis@.service

