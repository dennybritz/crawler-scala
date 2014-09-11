### Deployment

We use [Docker](docker.com) and [CoreOS](https://coreos.com/) for our deployment. This allows us to deploy our application on any kind of platform, be it private or public cloud. AWS and DigitalOcean both provide pre-built images for CoreUS, which makes deployment on these platforms relatively easy.

The Dockerfile for the crawler is in the `docker/` directory. It is based on CentOS 7, installs all package dependencies, pull the latet code from github, compiles it, and runs the crawler application.  