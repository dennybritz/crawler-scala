### Deployment

We use [Docker](docker.com) and [CoreOS](https://coreos.com/) for our deployment. This allows us to deploy our application on any kind of platform, private or public cloud. AWS and DigitalOcean provide pre-built images for CoreUS. This makes deployment on these platforms easy.

The Dockerfile for the crawler is in the `docker/` directory. It is based on CentOS 7, installs all package dependencies, pull the latet code from github, compiles it, and runs the crawler application. Note that it uses our private SSH keys which are not contained in this repository.


### CoreOS Setup

The CoreOS [fleet](https://coreos.com/docs/launching-containers/launching/launching-containers-fleet) configuration consists of the following services:

- `blikk-crawler`: The crawler application. Uses the blikk-crawler docker image.
- `blikk-crawler-discovery`: A discovery service that writes the IP address and port to [etcd](https://coreos.com/docs/distributed-configuration/getting-started-with-etcd). This data is used new nodes to discovery an existing crawling cluster. The discovery service is started together with the crawler.
- `redis`:  A redis instance, started on each machine in the cluster. Uses the dockerfile/redis docker image.