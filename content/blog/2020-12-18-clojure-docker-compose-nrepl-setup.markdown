<header>
# Clojure docker compose nrepl setup
<time class="article-date" date="2020-12-18">2020-12-18</time>
</header>

[bind to 0.0.0.0](https://fbrs.io/nrepl)
[clojure conjure setup](https://github.com/Olical/conjure/wiki/Quick-start:-Clojure)
[nrepl docs](https://nrepl.org/nrepl/usage/server.html#embedding-nrepl)



compose file
```
version: '3.8'
services:

  api:
    image: clojure:openjdk-8-lein-buster
    restart: "always"
    working_dir: "/app"
    entrypoint: "./start.sh"
    volumes:
      # :delegated is a speed improvements for Mac: https://docs.docker.com/compose/compose-file/#caching-options-for-volume-mounts-docker-for-mac
      - ".:/app:delegated"
      - "jars:/root/.m2"
    ports:
      - "7836:7836"
      - "8191:8191"
      
volumes:
  jars:
```

start.sh
```
#!/usr/bin/env bash
lein deps && lein run
```

project.clj

```
;; interactive development
[nrepl "0.8.3"]
[cider/cider-nrepl "0.25.2"]
```


.nrepl/nrepl.edn
```
{:bind         "::"
 :transport    nrepl.transport/tty
 :middleware   [cider.nrepl/cider-middleware]}
```
