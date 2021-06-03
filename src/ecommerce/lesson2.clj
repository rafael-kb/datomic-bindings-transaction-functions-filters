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

(pprint (db/all-categories (d/db conn)))
(pprint (db/all-products (d/db conn)))

(def checkers {:product/name  "Checkers"
               :product/slug  "/checkers"
               :product/price 15.5M
               :product/id    (model/uuid)})

; update/insert ==> upsert
(db/upsert-products! conn [checkers])
(pprint (db/one-product (d/db conn) (:product/id checkers)))

(db/upsert-products! conn [(assoc checkers :product/slug "/checkers-game")])
(pprint (db/one-product (d/db conn) (:product/id checkers)))

(db/upsert-products! conn [(assoc checkers :product/price 150.5M)])
(pprint (db/one-product (d/db conn) (:product/id checkers)))

(defn update-price []
  (println "updating price")
  (let [product (db/one-product (d/db conn) (:product/id checkers))
        product (assoc product :product/price 999M)]
    (db/upsert-products! conn [product])
    (println "price updated")
    product))

(defn update-slug []
  (println "updating slug")
  (let [product (db/one-product (d/db conn) (:product/id checkers))]
    (Thread/sleep 3000)
    (let [product (assoc product :product/slug "/expensive-checkers-game")]
      (db/upsert-products! conn [product])
      (println "slug updated")
      product)))

(defn run-transactions [tx]
  (let [futures (mapv #(future (%)) tx)]
    (pprint (map deref futures))
    (pprint "final result")
    (pprint (db/one-product (d/db conn) (:product/id checkers)))))

(run-transactions [update-price update-slug])

(defn smart-price-update []
  (println "updating price")
  (let [product {:product/id (:product/id checkers), :product/price 111M}]
    (db/upsert-products! conn [product])
    (println "price updated")
    product))

(defn smart-slug-update []
  (println "updating slug")
  (let [product {:product/id (:product/id checkers), :product/slug "/checkers-with-new-slug"}]
    (Thread/sleep 3000)
    (db/upsert-products! conn [product])
    (println "slug updated")
    product))

(run-transactions [smart-price-update smart-slug-update])