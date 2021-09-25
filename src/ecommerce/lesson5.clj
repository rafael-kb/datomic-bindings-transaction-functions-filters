(ns ecommerce.lesson5
  (:use clojure.pprint)
  (:require [datomic.api :as d]
            [ecommerce.db :as db]
            [ecommerce.model :as model]
            [schema.core :as s]))

(s/set-fn-validation! true)

(db/drop-database!)
(def conn (db/open-connection!))
(db/create-schema! conn)
(db/create-sample-data conn)

(def products (db/all-products (d/db conn)))
(def prod (first products))
(pprint prod)

; d/function but we would have to scape '
(def inc-views
  #db/fn {
          :lang   :clojure
          :params [db product-id]
          :code
                  (let [views (d/q '[:find ?views .
                                     :in $ id
                                     :where [?p :product/id ?id]
                                     [?p :product/views ?views]]
                                   db product-id)
                        current (or views 0)
                        new-total (inc current)]
                    [{:product/id    product-id
                      :product/views new-total}])})

(pprint @(d/transact conn [{
                            :db/doc   "Inc by one :product/views"
                            :db/ident :inc-views
                            :db/fn    inc-views}]))

(dotimes [n 10] (pprint (db/views! conn (:product/id prod))))
(pprint (db/one-product (d/db conn) (:product/id prod)))