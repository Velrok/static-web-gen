(ns static-web-gen.server
  (:require
    [clojure.tools.logging :as log]
    [static-web-gen.configuration :as config]
    [mount.core :refer [defstate start stop]]
    [compojure.route :refer [files not-found]]
    [compojure.core :refer [defroutes GET POST DELETE ANY context]]
    [org.httpkit.server :refer [run-server]]))


(defroutes all-routes
  (GET "/" []
       (slurp "public/index.html"))
  (files "/")
  (not-found "<p>Page not found.</p>"))



(defstate http-server
  :start (let [port config/port]
           (log/info (format "Staring http server at http://localhost:%d" port))
           (run-server all-routes {:port port}))
  :stop (do
          (log/info "Stopping http server.")
          (http-server)))

(comment
  (stop)
  (start))
;
