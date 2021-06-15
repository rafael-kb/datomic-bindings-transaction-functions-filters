(ns ecommerce.lesson1
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

(pprint (db/all-products (d/db conn)))

(pprint (db/all-products-in-categories (d/db conn) ["Electronics" "Food"]))
(pprint (db/all-products-in-categories (d/db conn) ["Electronics" "Sports"]))
(pprint (db/all-products-in-categories (d/db conn) ["Sports"]))
(pprint (db/all-products-in-categories (d/db conn) []))
(pprint (db/all-products-in-categories (d/db conn) ["Food"]))

(pprint (db/all-products-in-categories-and-digital (d/db conn) ["Electronics"] true))
(pprint (db/all-products-in-categories-and-digital (d/db conn) ["Electronics"] false))
