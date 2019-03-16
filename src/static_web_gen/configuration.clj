(ns static-web-gen.configuration)

(def port
  (-> (System/getenv)
      (get "PORT" "8080")
      (Integer/parseInt)))
