(ns static-web-gen.main
  (:require
    [clojure.tools.logging :as log]
    [static-web-gen.server]
    [static-web-gen.generator]
    [mount.core :as mount]))

(defn -main
  [& _]
  (log/info "I'm alive")
  (let [rt (Runtime/getRuntime)]
    (log/info "Registering shutdown hook")
    (.addShutdownHook rt
                      (Thread. (fn []
                                 (log/info "Received SIGTERM!")
                                 (log/info "Shutting down")
                                 (mount/stop)))))
  (log/info "Starting system")
  (mount/start)
  (log/info "System running ..."))


(comment
  (do
    (mount/stop)
    (mount/start)))
;
