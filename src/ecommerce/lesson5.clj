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

(pprint (db/all-salable-products (d/db conn)))

(def products (db/all-products (d/db conn)))

(defn check-if-can-sell [product]
  (println "Checking one product")
  (pprint (:product/stock product))
  (pprint (:product/digital product))
  (pprint (db/one-salable-product (d/db conn) (:product/id product)))
  )

(mapv check-if-can-sell products)