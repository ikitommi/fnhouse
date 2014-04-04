(ns fnhouse.handlers-test
  (:use clojure.test plumbing.core)
  (:require
   [schema.core :as s]
   [schema.test :as schema-test]
   [fnhouse.handlers :as handlers]))

(defn $path$to$my$method$GET [] nil)

(defn overridden-path-and-method {:path "/path/override" :method :post} [] nil)

(deftest path-and-method-test
  (is (= {:path "/path/to/my/method"
          :method :get}
         (#'handlers/path-and-method #'$path$to$my$method$GET)))
  (is (= {:path "/path/override"
          :method :post}
         (#'handlers/path-and-method #'overridden-path-and-method))))

(defnk ^:private $test$:handler$:uri-arg$POST
  "This is my test handler.

   It depends on resources and a request, and produces a result."
  {:auth-level #{:admin}
   :responses {200 {:success? Boolean}}}
  [[:request
    [:body body-arg :- s/Keyword]
    [:uri-args uri-arg :- s/Int]
    [:query-params qp1 :- String {qp2 :- s/Int 3}]]
   [:resources data-store :as resources]]
  (swap! data-store
         conj
         {:resource-keys (keys resources)
          :body-arg body-arg
          :uri-arg uri-arg
          :qp1 qp1
          :qp2 qp2})
  {:body {:success? true}})

(deftest var->handler-info-test
  (testing "duplicate uri-args fail"
    (defnk $:a$:a$GET {:responses {200 s/Any}} [])
    (is (thrown? Exception (handlers/var->handler-info #'$:a$:a$GET)))
    (ns-unmap 'fnhouse.handlers-test '$:a$:a$GET)))

(deftest ns->handler-fns-test
  (letk [[resource] (singleton (handlers/ns->handler-fns 'fnhouse.handlers-test (constantly nil)))]
    (println resource)))

(deftest nss->handlers-fn-test
  (let [annotation-fn (fn-> meta (select-keys [:auth-level :private]))
        handlers-fn (handlers/nss->handlers-fn {"my-test" 'fnhouse.handlers-test} annotation-fn)
        data-store (atom [])
        annotated-handlers (handlers-fn {:data-store data-store :more-junk 117})]
    (letk [[handler
            resource
            [:info resources responses method path description annotations
             [:request uri-args body query-params]]]
           (singleton annotated-handlers)]
      (is (= resource "my-test"))
      (is (= {:uri-arg s/Int :handler String} uri-args))
      (is (= {s/Keyword s/Any :body-arg s/Keyword} body))
      (is (= {s/Keyword s/Any :qp1 String (s/optional-key :qp2) s/Int} query-params))
      (is (= {200 {:success? Boolean}} responses))
      (is (= {s/Keyword s/Any :data-store s/Any} resources))
      (is (= "/my-test/test/:handler/:uri-arg" path))
      (is (= :post method))
      (is (= {:private true :auth-level #{:admin}} annotations))
      (is (= "This is my test handler.

   It depends on resources and a request, and produces a result."
             description))
      (assert (empty? @data-store))
      (handler
       {:body {:body-arg :xx}
        :uri-args {:uri-arg 1}
        :query-params {:qp1 "x"}})
      (is (= {:uri-arg 1 :qp1 "x" :qp2 3 :body-arg :xx :resource-keys [:data-store]}
             (singleton @data-store))))))

(use-fixtures :once schema-test/validate-schemas)
