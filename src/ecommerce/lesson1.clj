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

(defn test-schema []
  (def computer (model/new-product (model/uuid) "New computer", "/new-computer", 2500.10M))
  (pprint (s/validate model/Product computer))
  ;(pprint (s/validate model/Product (assoc computer :product/price 76)))

  (def electronics (model/new-category "Electronics"))
  (pprint (s/validate model/Category electronics))
  (pprint (s/validate model/Product (assoc computer :product/category electronics))))

(test-schema)

(db/create-sample-data conn)

(pprint (db/all-categories (d/db conn)))
(pprint (db/all-products (d/db conn)))
