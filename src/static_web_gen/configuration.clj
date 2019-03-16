(ns static-web-gen.configuration)

(def port
  (-> (System/getenv)
      (get "PORT" "4444")
      (Integer/parseInt)))
