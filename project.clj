(defproject static-web-gen "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :main static-web-gen.main
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [markdown-to-hiccup "0.6.2"]
                 [hiccup "1.0.5"]
                 [clj-time "0.15.0"]
                 [org.clojure/tools.logging "0.4.1"]
                 [clojure-watch "0.1.14"]
                 [compojure "1.6.1"]
                 [mount "0.1.16"]
                 [http-kit "2.3.0"]]
  :aliases {"gen" ["lein" "run" "-m" "static-web-gen.generator"]})
