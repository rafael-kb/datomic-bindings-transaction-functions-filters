(ns ecommerce.lesson3
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
(def first-product (first products))
(pprint first-product)

; if not found, returns nil
(pprint (db/one-product (d/db conn) (:product/id first-product)))
(pprint (db/one-product (d/db conn) (model/uuid)))

; if not found, throws an exception
(pprint (db/one-product! (d/db conn) (:product/id first-product)))
(pprint (db/one-product! (d/db conn) (model/uuid)))