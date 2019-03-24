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
    [clj-time.core :as t]
    [clj-time.format :as f]
    [markdown-to-hiccup.core :as md-to-hiccup :refer [md->hiccup]]
    [mount.core :as mount :refer [defstate]]))

(comment

  (f/show-formatters)

  (f/parse (f/formatters))
  
  )

(def blog-post-layout (->> "./content/layout/blog-post.hiccup.edn"
                           slurp
                           edn/read-string))

(def index-layout (->> "./content/layout/index.hiccup.edn"
                       slurp
                       edn/read-string))

(def blog-post-index (atom #{}))



(defmulti content-replacement
  (fn [post-meta x]
    (cond
      (vector? x) (first x)
      :else ::id)))

(defmethod content-replacement :default [post-meta x] #_(prn {::tag (first x)}) x)
(defmethod content-replacement ::id     [post-meta x] x)

(defmethod content-replacement :div#content
  [{:keys [content]} x]
  [:div#content
   [:article content]])

(defmethod content-replacement :title
  [{:keys [title]} [_ existing-title]]
  [:title (str title " - " existing-title)])

(defmethod content-replacement :code
  [_ [_ attr content]]
  (log/info (prn-str [::code content]))
  [:code attr (-> (str content)
                  (string/replace  #">" "&gt;")
                  (string/replace  #"<" "&lt;"))])


(defn produce-blog-post-html!
  [{:keys [original-file target-filename rel-file-name] :as post-meta}]
  (let [final-product   (postwalk (partial content-replacement post-meta)
                                  blog-post-layout)]
    (when target-filename
      (log/info (format "produce blog post for %s -> %s (%s)"
                        original-file
                        target-filename
                        (format "http://localhost:%d/post/%s.html"
                                config/port
                                rel-file-name)))
      (spit target-filename (html final-product)))))

(def posts-prefix "./content/blog/")

(defn generate-index!
  [posts]
  (let [file-hiccup     [:ul
                         (for [{:keys [original-file url-rel title date-str]} (reverse (sort-by :date-str posts))]
                           [:li
                            (when date-str [:span.article-date date-str])
                            [:a {:href url-rel}
                             (str (or title original-file))]])]
        final-product   (postwalk (fn [x]
                                    (cond
                                      (= x [:div#content])
                                      [:div#content
                                       [:article file-hiccup]]

                                      :else x))
                                  index-layout)
        target-filename "./public/index.html"]
    (log/info (format "regenerating index -> %s (%s)"
                      target-filename
                      (format "http://localhost:%d/" config/port)))
    (spit target-filename (html final-product))))

(defn parse-blog-post-md
  [filename]
  (let [file-hiccup     (->> filename slurp md->hiccup md-to-hiccup/component)
        [_ _ title]     (md-to-hiccup/hiccup-in file-hiccup :h1)
        [_ {date :date} _]     (md-to-hiccup/hiccup-in file-hiccup :header :time)
        rel-file-name   (some->> (re-matches #".*/blog/(.*)$" filename) second)
        target-filename (str "./public/post/" rel-file-name ".html")
        rel-url         (format "/post/%s.html" rel-file-name)]
    {:original-file   filename
     :rel-file-name   rel-file-name
     :target-filename target-filename
     :date-str        date
     :title           title
     :url-rel         rel-url
     :content         (postwalk (partial content-replacement {})
                                file-hiccup)
     :layout          blog-post-layout}))

(defn generate-all!
  []
  (reset! blog-post-index #{})
  (log/info "generate-all!")
  (let [posts (->> "./content/blog/"
                   (io/as-file)
                   file-seq
                   (remove #(.isDirectory %))
                   (map #(.getAbsolutePath %))
                   (map parse-blog-post-md))]
    (->> posts
         (map produce-blog-post-html!)
         doall)
    (generate-index! posts)))

(defstate static-file-regenerator
  :start (let [paths [{:path        "./content/blog/"
                       :event-types [:create :modify :delete]
                       :bootstrap   (fn [path] (log/info "Starting to watch " path))
                       :callback    (fn [event filename]
                                      (log/info event filename)
                                      (produce-blog-post-html! filename)
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

(defn -main
  [& args]
  (generate-all!))
