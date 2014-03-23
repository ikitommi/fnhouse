(ns guesthouse.swagger
  (:use plumbing.core)
  (:require
    [fnhouse.handlers :as handlers]
    [ring.swagger.core :as ring-swagger]
    [schema.core :as schema]
    [ring.middleware.resource :as resource]
    [clojure.string :as s]))

(defn- create-uri [s]
  (-> s
    (s/replace #":(.[^:|/]*)" " :$1 ")
    (s/split #" ")
    (->> (map #(if (.startsWith % ":") (keyword (.substring % 1)) %)))))

(defn- generate-nickname [annotated-handler]
  (str (:api annotated-handler) (get-in annotated-handler [:info :source-map :name])))

(defn collect-swagger-info [swagger {{:keys [method path description]} :info api :api :as annotated-handler}]
  (swap! swagger
    update-in [api]
    update-in [:routes]
    conj {:method method
          :uri (create-uri path)
          :metadata {:summary description
                     :nickname (generate-nickname annotated-handler)}})
  annotated-handler)

(defnk $api-docs$GET
  "Apidocs"
  {:responses {200 schema/Any}}
  [[:resources swagger]]
  (ring-swagger/api-listing {} @swagger))

(defnk $api-docs$:api$GET
  "Apidoc"
  {:responses {200 schema/Any}}
  [[:request [:uri-args api :- String] :as request]
   [:resources swagger]]
  (ring-swagger/api-declaration {} @swagger api (ring-swagger/extract-basepath request)))

(defn docs [handler]
  (resource/wrap-resource handler "swagger-ui"))
