(ns static-web-gen.generator
  (:require
    [clojure-watch.core :as fs-watch]
    [clojure.tools.logging :as log]
    [mount.core :as mount :refer [defstate]]))


(defstate static-file-generator
  :start (fs-watch/start-watch
           [{:path "/some/valid/path"
             :event-types [:create :modify :delete]
             :bootstrap (fn [path] (println "Starting to watch " path))
             :callback (fn [event filename] (println event filename))
             :options {:recursive true}}]))
