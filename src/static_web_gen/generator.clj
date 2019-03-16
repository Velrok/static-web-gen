(ns static-web-gen.generator
  (:require
    [static-web-gen.configuration :as config]
    [clojure-watch.core :as fs-watch]
    [clojure.tools.logging :as log]
    [clojure.java.io :as io]
    [clojure.string :as string]
    [clojure.edn :as edn]
    [clojure.walk :as walk :refer [postwalk]]
    [hiccup.core :refer [html]]
    [markdown-to-hiccup.core :as md-to-hiccup :refer [md->hiccup]]
    [mount.core :as mount :refer [defstate]]))

(def blog-post-layout (->> "./content/layout/blog-post.hiccup.edn"
                               slurp
                               edn/read-string))

(defmulti produce-static! (fn [classification _] classification))

(defmethod produce-static! ::unknown
  [_ filename]
  [:div
   [:h1 "Unknown content type"]
   [:p "for file " (str filename)]])

(defmethod produce-static! ::blog-post
  [_ filename]
  (let [file-hiccup (->> filename slurp md->hiccup md-to-hiccup/component)
        final-product (postwalk (fn [x]
                                  (if (= x [:div#content])
                                    file-hiccup
                                    x))
                                blog-post-layout)
        rel-file-name (some->> (re-matches #".*/blog/(.*)$" filename) second)
        target-filename (str "./public/post/" rel-file-name ".html")]
    (log/info (prn-str [:final-prod final-product]))
    (when rel-file-name
      (log/info (format "produce blog post for %s -> %s (%s)"
                        filename
                        target-filename
                        (format "http://localhost:%d/post/%s.html"
                                config/port
                                rel-file-name)))
      (spit target-filename (html final-product)))))


(defn- classify-file
  [filename]
  (cond
    (re-find #"/blog/" filename) ::blog-post
    :else ::unknown))

(defstate static-file-regenerator
  :start (let [paths [{:path        "./content/blog/"
                       :event-types [:create :modify :delete]
                       :bootstrap   (fn [path] (log/info "Starting to watch " path))
                       :callback    (fn [event filename]
                                      (log/info event filename)
                                      (produce-static! (classify-file filename) filename))
                       :options     {:recursive true}}]]
           (log/info (format "static-file-generator is watching %s"
                             (string/join ", "
                                          (map :path paths))))
           (fs-watch/start-watch paths))
  :stop (do
          (log/info "stopping static-file-generator")
          (static-file-regenerator)))

(comment
  
  (.getAbsolutePath (io/file "."))
  
  )
