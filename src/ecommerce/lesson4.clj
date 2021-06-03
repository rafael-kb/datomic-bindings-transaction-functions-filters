(ns ecommerce.lesson4
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

(pprint (db/all-products-with-stock (d/db conn)))

(def products (db/all-products (d/db conn)))
(pprint (db/one-product-with-stock (d/db conn) (:product/id (first products))))
(pprint (db/one-product-with-stock (d/db conn) (:product/id (second products))))