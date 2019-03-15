(ns static-web-gen.server
  (:require
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
  :start (run-server all-routes
                     {:port (-> (System/getenv)
                                (get "PORT" "8080")
                                (Integer/parseInt))})
  :stop (.close http-server))

(comment
  (stop)
  (start))
;
