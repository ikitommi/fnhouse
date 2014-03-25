(ns fnhouse.swagger
  (:use plumbing.core)
  (:require
    [fnhouse.handlers :as handlers]
    [ring.swagger.core :as ring-swagger]
    [schema.core :as schema]
    [ring.middleware.resource :as resource]
    [clojure.string :as s]))

(defn- generate-nickname [annotated-handler]
  (str (:api annotated-handler) (get-in annotated-handler [:info :source-map :name])))

(defn collect-routes [swagger {{:keys [method path description request responses] :as info} :info api :api :as annotated-handler}]
  (swap! swagger
    update-in [api]
    update-in [:routes]
    conj {:method method
          :uri path
          :metadata {:summary description
                     :return (get responses 200)
                     :nickname (generate-nickname annotated-handler)
                     :parameters [{:type :body
                                   :model (:body request)}
                                  {:type :query
                                   :model (:query-params request)}]}})
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

(defn swagger-ui [handler]
  (resource/wrap-resource handler "swagger-ui"))
