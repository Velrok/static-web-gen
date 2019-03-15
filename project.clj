(defproject static-web-gen "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :main static-web-gen.main
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [markdown-clj "1.0.7"]
                 [org.clojure/tools.logging "0.4.1"]
                 [compojure "1.6.1"]
                 [mount "0.1.16"]
                 [http-kit "2.3.0"]])
