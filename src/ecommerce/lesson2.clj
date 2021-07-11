(ns ecommerce.lesson2
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

(db/upsert-products! conn [(assoc prod :product/price 20M)])
(pprint (db/one-product (d/db conn) (:product/id prod)))

(db/upsert-products! conn [(assoc prod :product/price 31M)])
(pprint (db/one-product (d/db conn) (:product/id prod)))

(pprint @(db/update-price! conn (:product/id prod) 30M 20M))
(pprint @(db/update-price! conn (:product/id prod) 20M 40M))
(pprint @(db/update-price! conn (:product/id prod) 35M 40M))