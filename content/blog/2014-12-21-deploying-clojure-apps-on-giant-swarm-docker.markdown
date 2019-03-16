---
layout: post
title: "deploying clojure apps on giant swarm (docker)"
date: 2014-12-21 19:22
comments: true
categories: clojure docker giantswarm
---

<img style="float:left; height:190px" src="/images/giantswarm_logo_standard.png">
<img style="height:190px" src="/images/docker.png"> 

Recently I've been playing around with Clojure and [docker](https://www.docker.com/).
Docker provides your app with an isolated container on a Linux machine, sharing the
same system resources, while isolating them from the other containers.
This has the benefit of providing a separation between apps on a Linux host system,
which is much more light weigh than full blown VMs (Virtual Machines).

In this blog post I will describe the setup I used to deploy my pet project named 
money-balance (a Clojure app) on [giantswarm.io](https://giantswarm.io/), a
docker host in Germany. 
They are currently still in the alpha phase and looking for early adopters, but
so far everything was working flawlessly for me. 
You can find them on [gitter](https://gitter.im/giantswarm/users).

<!-- more -->

## Overview

My app uses the usual combination of [compojure](https://github.com/weavejester/compojure)
and [ring](https://github.com/ring-clojure/ring).
Zaiste has a nice and short article which should set you up with a bare bone
Clojure web app skeleton. Feel free to ignore everything beyond the 
backend section for now. [Article](http://zaiste.net/2014/02/web_applications_in_clojure_all_the_way_with_compojure_and_om/).

My deployment process consists of the following steps.

1. build a stand alone jar `lein ring uberjar`
1. increment application version (mirrored in the docker image version)
1. build a docker image with the necessary tag so that the image is stored in the 
   giantswarm repository instead of docker hub
1. push the image into the giantswarm repository
1. remove the old version of the application in giantswarm
1. create the app anew with the new version in giantswarm
1. start the new version

A disclaimer, this is most likely not the most lean way of doing things.
For instance the whole versioning part of the deploy process could be
omitted, but I had a phase where I wasn't sure if the repository's latest
version of an image is the same as my local one.
According to the giantswarm support there is no way to find out if that true.
So my process states an explicit image version.

The deletion of the app and the recreation means that all services will be stopped
and all the data will be lost.
For my app this is not an issue, because I'm using a dedicated service to host
my database.
The swarm client has an update command, which is probably a better fit, and which
I will give a spin in the next weeks.

The rest of the article dives into the details and lists the source codes.

## the project.clj

The `project.clj` is used by leiningen and declares all dependencies.

```clojure
(defproject money-balance "0.0.1" 
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [ring/ring-json "0.3.1"]
                 [compojure "1.1.6"]
                 [hiccup "1.0.5"]
                 [cheshire "5.3.1"]
                 [ring "1.2.2"]
                 [ring-middleware-format "0.3.0" :exclusions [org.clojure/tools.reader]]
                 [clj-time "0.6.0"]]
  :plugins [[lein-ring "0.8.10"]]
  :min-lein-version "2.0.0"
  :main money-balance.web.server
  :profiles {:production {:env {:production true}}
             :dev {:dependencies [[midje "1.6.3"]]}
             :uberjar {:aot :all}}
  :uberjar-name "money-balance.jar"
  :ring {:handler money-balance.web.server/app})
```

The `plugins` section lists extensions to leiningen itself, for example `lein-ring`
is a plug-in which handles starting our app inside a jetty web server.
Plug-ins sometimes look inside the `defproject` for configuration.
For instance `:ring {:handler money-balance.web.server/app}` tell the ring plug-in
where to find our main handler.

**Note:** The ring-plugin is not be be confused with the ring dependency.
The plug-in is a extention to leiningen, which makes it more convenient to start
a server and build a standalone uberjar, the ring dependency is a abstraction
that allows one to deal with requests and responses as Clojure maps instead of
java objects.

## the Dockerfile

For a introduction to docker head to [https://www.docker.com/](https://www.docker.com/).

```ruby
FROM java:7
MAINTAINER Velrok

RUN mkdir -p /opt/money-balance
ADD ./target/money-balance.jar /opt/money-balance/money-balance.jar
ADD ./VERSION /opt/money-balance/VERSION
ADD ./public /opt/money-balance/public

WORKDIR /opt/money-balance

EXPOSE 3000
ENV APP_ENV production

ENTRYPOINT java -jar money-balance.jar
```

While docker hub provides a [Clojure image](https://registry.hub.docker.com/u/library/clojure/)
I found the plain [java base image](https://registry.hub.docker.com/_/java/) to be a better
fit for the job.
It is simply easier to build a standalone jar on your machine and copy it into a
plain java image.
Having said that, the Clojure image worked without errors for me. If you prefer
to have the lein command at hand on the machine you may as well use that one.

`ADD ./target/money-balance.jar /opt/money-balance/money-balance.jar` copies a local build into the docker image.
`EXPOSE 3000` takes care of exposing the webserver port to the host system, which is
necessary to allow http connections into the container.
`ENTRYPOINT java -jar money-balance.jar` tells docker, that this command should be run
when the image is started. Thus when a new container comes up it will
autostart the standalone jar file.

**Side-note:** 
 - how I tryied to make the Clojure image work

## the build.sh

The build script is concerned with building a docker image, with the application
in it and pushing the image into the swarm registry so it's available for deployment.

```bash
#!/bin/bash
set -e

lein ring uberjar
./increment_version.rb $@
docker build --rm=true -t registry.giantswarm.io/velrok/money-balance:$(cat ./VERSION) . 
docker push registry.giantswarm.io/velrok/money-balance:$(cat ./VERSION)
```

`lein ring uberjar` creates a standalone jar, which starts the app on port 3000.
`./increment_version.rb $@` is a small script, that increments the version string
in my `VERSION` file. 
`docker build --rm=true -t registry.giantswarm.io/velrok/money-balance:$(cat ./VERSION) .`
instructs docker to build a new image, tagged with the giant swarm repository and
my explicit latest version from the `VERSION` file.
`docker push registry.giantswarm.io/velrok/money-balance:$(cat ./VERSION)`
pushes the image to the giant swarm docker repository, so they can start
containers with the latest version.

I found it reassuring to have thouse commands in a simple `build.sh` just to
make sure I never forget to tag my image with the correct repository, because
docker will publish your image to dockerhub by default.

If you don't care about explicit versions just use `latest` and don't maintain
a `VERSION` file.
`docker build --rm=true -t registry.giantswarm.io/velrok/money-balance:latest .`

## deploying to giant swarm

In order to deploy to giantswarm you will need an account and the command
line client. Follow this [setup guide](http://docs.giantswarm.io/installation/cheatsheet/index.html)
to get started.

You describe our application in terms of services and components in a `swarm.json`
file.
Here is mine:

```json
{
  "app_name": "money-balance",
  "services": [
    {
      "service_name": "money-balance-service",
      "components": [
        {
          "component_name": "money-balance",
          "image": "registry.giantswarm.io/velrok/money-balance:$version",
          "ports": [ "3000/tcp" ],
          "domains": { "money-balance.velrok.gigantic.io": "3000" },
          "env" : [
            "APP_ENV=production",
            "MONEY_BALANCE_DATABASE_URI=postgres://xxx"
          ]          
        }
      ]
    }
  ]
}
```

It declares an application named `money-balance` 
(`"app_name": "money-balance"`), which has only one service `money-balance-service`
with only one component.
`"image": "registry.giantswarm.io/velrok/money-balance:$version"` declares that
our container should use our money-balance images, which has been build and
pushed in the `build.sh` script.
`$version` is a swarm client variable, which can be set later, when we actually
call the swarm client to start our app.
It enables us to explicitly name the latest version, without the need to adjust
the `swarm.json` file.
`"domains": { "money-balance.velrok.gigantic.io": "3000" }` declares that we
want our app to be reachable from the internet under the domain name 
`money-balance.velrok.gigantic.io` and that our app excepts http requests on
port 3000.
Since I'm using a different service for my database, the actual connection string
for the production database is set via the environment variable 
`MONEY_BALANCE_DATABASE_URI`.

We can start up the app manually now, calling `swarm create swarm.json` and then 
`swarm start money-balance`.


## the deploy.sh

The deploy script is concerned about stoping the old version of the app
on the host and starting up the latest version instead.

```bash
swarm delete -y money-balance
swarm create --var=version=$(cat ./VERSION) swarm.json
sleep 1
swarm start money-balance
sleep 1
swarm status money-balance
```

`swarm delete -y money-balance` deletes the old version of the app from
giant swarm. This is rather drastic, since this will stop all services
and remove all containers, loosing all the data that has been stored on any
container.
`swarm update` presents a more elegant alternative, which I'm still to explore 
in the next days.
`swarm create --var=version=$(cat ./VERSION) swarm.json` recreates the app with
the latest version specified in the VERSION file.

`swarm start money-balance` starts up the application and
`swarm status money-balance` returns the status of the app.

## conclusion

I found the [plain java image](https://registry.hub.docker.com/_/java/tags/manage/)
to work best as the base image for my Clojure app by compiling it into a single
jar file.
If you prefer to have the lein command available on the docker container
you can just use the [Clojure image](https://registry.hub.docker.com/u/library/clojure/)
as the base image.
Just be aware, that it will have to download all the project dependencies
the first time you start it.

Even so [giantswarm](https://giantswarm.io) is still in alpha it is working well
so far. Definetly worth a shot if you are trying to get more experience with
docker and want a free (for now) hoster.

My current deployment process is lacking any rollback features as well as any
migrations strategy.
