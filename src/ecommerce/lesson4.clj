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

(def products (db/all-products (d/db conn)))
(def prod (first products))
(pprint prod)

(dotimes [n 10] (pprint (db/views! conn (:product/id prod))))
(pprint (db/one-product (d/db conn) (:product/id prod)))