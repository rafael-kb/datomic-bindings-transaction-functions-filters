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
(def prod (first products))
(pprint prod)

(pprint @(db/add-variant! conn (:product/id prod) "256 SSD" 3000M))
(pprint @(db/add-variant! conn (:product/id prod) "256 SSD i9 Intel Processor" 6000M))

(pprint (d/q '[:find (pull ?product [*])
               :where [?product :product/name]]
             (d/db conn)))

(pprint (db/all-products (d/db conn)))

(pprint (db/count-products (d/db conn)))

(pprint @(db/remove-product! conn (:product/id prod)))

(pprint (d/q '[:find ?name
               :where [?_ :variant/name ?name]]
             (d/db conn)))