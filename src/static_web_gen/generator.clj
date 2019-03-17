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

(def index-layout (->> "./content/layout/index.hiccup.edn"
                       slurp
                       edn/read-string))

(def post-discovered (atom #{}))


(defmulti produce-static! (fn [classification _] classification))

(defmethod produce-static! ::unknown
  [_ filename]
  [:div
   [:h1 "Unknown content type"]
   [:p "for file " (str filename)]])

(defn post-meta
  [filename]
  (let [file-hiccup     (->> filename slurp md->hiccup md-to-hiccup/component)
        [_ _ title]     (md-to-hiccup/hiccup-in file-hiccup :h1)
        rel-file-name   (some->> (re-matches #".*/blog/(.*)$" filename) second)
        target-filename (str "./public/post/" rel-file-name ".html")
        rel-url         (format "/post/%s.html" rel-file-name)]
    {:original-file   filename
     :rel-file-name   rel-file-name
     :target-filename target-filename
     :title           title
     :url-rel         rel-url}))

(comment
  
  (def x (->> "/Users/velrok/private/static-web-gen/./content/blog/2013-04-29-hello-world.markdown" slurp md->hiccup md-to-hiccup/component))

  (clojure.pprint/pprint (md-to-hiccup/hiccup-in x :h1))
  )

(defmethod produce-static! ::blog-post
  [_ filename]
  (let [file-hiccup     (->> filename slurp md->hiccup md-to-hiccup/component)
        {:keys [title target-filename rel-file-name] :as p-meta} (post-meta filename)
        final-product   (postwalk (fn [x]
                                    (cond
                                      (= x [:div#content])
                                      [:div#content
                                       [:article file-hiccup]]

                                      (and (vector? x)
                                           (if-let [[tag _] x]
                                             (= :title tag)))
                                      [:title (str title " - " (second x))]

                                      :else x))
                                  blog-post-layout)]
    (when target-filename
      (log/info (format "produce blog post for %s -> %s (%s)"
                        filename
                        target-filename
                        (format "http://localhost:%d/post/%s.html"
                                config/port
                                rel-file-name)))
      (spit target-filename (html final-product))
      (swap! post-discovered conj p-meta))))


(defn- classify-file
  [filename]
  (cond
    (re-find #"/blog/" filename) ::blog-post
    :else ::unknown))

(def posts-prefix "./content/blog/")

(defn generate-index!
  []
  (let [file-hiccup     [:ul
                         (for [{:keys [original-file url-rel title]} @post-discovered]
                           [:li
                            [:a {:href url-rel} (str (or title original-file))]])]
        final-product   (postwalk (fn [x]
                                    (cond
                                      (= x [:div#content])
                                      [:article file-hiccup]

                                      :else x))
                                  index-layout)
        target-filename "./public/index.html"]
    (log/info (format "regenerating index -> %s (%s)"
                      target-filename
                      (format "http://localhost:%d/" config/port)))
    (spit target-filename (html final-product))))

(defn generate-all!
  []
  (log/info "generate-all!")
  (->> "./content/blog/"
       (io/as-file)
       file-seq
       (remove #(.isDirectory %))
       (map (fn [f]
              (let [filename (.getAbsolutePath f)]
                (produce-static! (classify-file filename)
                                 filename))))
       doall)
  (generate-index!))

(defstate static-file-regenerator
  :start (let [paths [{:path        "./content/blog/"
                       :event-types [:create :modify :delete]
                       :bootstrap   (fn [path] (log/info "Starting to watch " path))
                       :callback    (fn [event filename]
                                      (log/info event filename)
                                      (produce-static! (classify-file filename) filename)
                                      (generate-index!))
                       :options     {}}]]
           (generate-all!)
           (log/info (format "static-file-generator is watching %s"
                             (string/join ", "
                                          (map :path paths))))
           (fs-watch/start-watch paths))
  :stop (do
          (log/info "stopping static-file-generator")
          (static-file-regenerator)))
