(ns fnhouse.swagger
  (:use plumbing.core)
  (:require
    [fnhouse.handlers :as handlers]
    [ring.swagger.core :as ring-swagger]
    [schema.core :as s]
    [ring.middleware.resource :as resource]))

(defn- generate-nickname [annotated-handler]
  (str (:api annotated-handler) (get-in annotated-handler [:info :source-map :name])))

(defn- convert-parameters [request]
  (for [[type f] {:body :body, :query :query-params, :path :uri-args}]
    {:type type :model (f request)}))

(defn collect-route! [swagger {{:keys [method path description request responses] :as info} :info api :api :as annotated-handler}]
  (swap! swagger
    update-in [api]
    update-in [:routes]
    conj {:method method
          :uri path
          :metadata {:summary description
                     :return (get responses 200)
                     :nickname (generate-nickname annotated-handler)
                     :parameters (convert-parameters request)}}))

(defn collect-routes! [swagger handlers]
  (doseq [handler handlers] (collect-route! swagger handler)))

(defn swagger-ui [handler]
  (resource/wrap-resource handler "swagger-ui"))

(defnk $api-docs$GET
  "Apidocs"
  {:responses {200 s/Any}}
  [[:resources swagger]]
  (ring-swagger/api-listing {} swagger))

(defnk $api-docs$:api$GET
  "Apidoc"
  {:responses {200 s/Any}}
  [[:request [:uri-args api :- String] :as request]
   [:resources swagger]]
  (ring-swagger/api-declaration {} swagger api (ring-swagger/extract-basepath request)))
