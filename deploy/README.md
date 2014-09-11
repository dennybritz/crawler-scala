### Deployment

We use [Docker](docker.com) and [CoreOS](https://coreos.com/) for our deployment. This allows us to deploy our application on any kind of platform, private or public cloud. AWS and DigitalOcean provide pre-built images for CoreUS. This makes deployment on these platforms easy.

The Dockerfile for the crawler is in the `docker/` directory. It is based on CentOS 7, installs all package dependencies, pull the latet code from github, compiles it, and runs the crawler application. Note that it uses our private SSH keys which are not contained in this repository.