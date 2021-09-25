(ns ecommerce.db
  (:use clojure.pprint)
  (:require [datomic.api :as d]
            [ecommerce.model :as model]
            [schema.core :as s]
            [clojure.walk :as walk]
            [clojure.set :as cset]))

(def db-uri "datomic:dev://localhost:4334/ecommerce")

(defn open-connection! []
  (d/create-database db-uri)
  (d/connect db-uri))

(defn drop-database! []
  (d/delete-database db-uri))

(def schema [
             {:db/ident       :product/name
              :db/valueType   :db.type/string
              :db/cardinality :db.cardinality/one
              :db/doc         "Product name"}
             {:db/ident       :product/slug
              :db/valueType   :db.type/string
              :db/cardinality :db.cardinality/one
              :db/doc         "URL friendly http resource"}
             {:db/ident       :product/price
              :db/valueType   :db.type/bigdec
              :db/cardinality :db.cardinality/one
              :db/doc         "Product price with monetary precision"}
             {:db/ident       :product/keyword
              :db/valueType   :db.type/string
              :db/cardinality :db.cardinality/many}
             {:db/ident       :product/id
              :db/valueType   :db.type/uuid
              :db/cardinality :db.cardinality/one
              :db/unique      :db.unique/identity}
             {:db/ident       :product/category
              :db/valueType   :db.type/ref
              :db/cardinality :db.cardinality/one}
             {:db/ident       :product/stock
              :db/valueType   :db.type/long
              :db/cardinality :db.cardinality/one}
             {:db/ident       :product/digital
              :db/valueType   :db.type/boolean
              :db/cardinality :db.cardinality/one}
             {:db/ident       :product/variant
              :db/valueType   :db.type/ref
              :db/isComponent true
              :db/cardinality :db.cardinality/many}
             {:db/ident       :product/views
              :db/valueType   :db.type/long
              :db/cardinality :db.cardinality/one
              :db/noHistory   true}

             {:db/ident       :variant/id
              :db/valueType   :db.type/uuid
              :db/cardinality :db.cardinality/one
              :db/unique      :db.unique/identity}
             {:db/ident       :variant/name
              :db/valueType   :db.type/string
              :db/cardinality :db.cardinality/one}
             {:db/ident       :variant/price
              :db/valueType   :db.type/bigdec
              :db/cardinality :db.cardinality/one}

             {:db/ident       :category/name
              :db/valueType   :db.type/string
              :db/cardinality :db.cardinality/one}
             {:db/ident       :category/id
              :db/valueType   :db.type/uuid
              :db/cardinality :db.cardinality/one
              :db/unique      :db.unique/identity}

             {:db/ident       :tx-data/ip
              :db/valueType   :db.type/string
              :db/cardinality :db.cardinality/one}

             ])

(defn dissoc-db-id [entity]
  (if (map? entity)
    (dissoc entity :db/id)
    entity))

(defn datomic-to-entity [entities]
  (walk/prewalk dissoc-db-id entities))

(s/defn upsert-products!
  ([conn, products :- [model/Product]]
   (d/transact conn products))
  ([conn, products :- [model/Product], ip]
   (let [db-add-ip [:db/add "datomic.tx" :tx-data/ip ip]]
     (d/transact conn (conj products db-add-ip)))))

(defn create-schema! [conn]
  (d/transact conn schema))

(s/defn one-product :- (s/maybe model/Product) [db, product-id :- java.util.UUID]
  (let [result (d/pull db '[* {:product/category [*]}] [:product/id product-id])
        product (datomic-to-entity result)]
    (if (:product/id product)
      product
      nil)))

(s/defn one-product! :- model/Product [db, product-id :- java.util.UUID]
  (let [product (one-product db product-id)]
    (when (nil? product)
      (throw (ex-info "entity not found" {:type :errors/not-found, :id product-id})))
    product))

(defn db-adds-categories-assignment [products category]
  (reduce (fn [db-adds product] (conj db-adds [:db/add
                                               [:product/id (:product/id product)]
                                               :product/category
                                               [:category/id (:category/id category)]]))
          []
          products))

(defn assign-categories! [conn products category]
  (let [to-transact (db-adds-categories-assignment products category)]
    (d/transact conn to-transact)))

(s/defn add-categories! [conn, categories :- [model/Category]]
  (d/transact conn categories))

(s/defn all-products :- [model/Product] [db]
  (datomic-to-entity
    (d/q '[:find [(pull ?entity [* {:product/category [*]}]) ...]
           :where [?entity :product/name]] db)))

(s/defn all-categories :- [model/Category] [db]
  (datomic-to-entity
    (d/q '[:find [(pull ?category [*]) ...]
           :where [?category :category/id]]
         db)))

(defn create-sample-data [conn]
  (def electronics (model/new-category "Electronics"))
  (def sports (model/new-category "Sports"))
  (add-categories! conn [electronics, sports])

  (def computer (model/new-product (model/uuid) "New computer", "/new-computer", 2500.10M, 10))
  (def phone (model/new-product (model/uuid) "Expensive phone", "/phone", 888888.10M))
  ;(def calculator {:product/name "calculator with 4 functions"})
  (def budget-phone (model/new-product "Budget Phone", "/budget-phone", 0.1M))
  (def chess (model/new-product (model/uuid) "Chess board", "/chess-board", 30M, 5))
  (def game (assoc (model/new-product (model/uuid) "Online Game", "/online-game", 20M) :product/digital true))
  (upsert-products! conn [computer, phone, budget-phone, chess, game] "200.216.222.125")

  (assign-categories! conn [computer, phone, budget-phone, game] electronics)
  (assign-categories! conn [chess] sports))

(def rules
  '[
    [(stock ?product ?stock)
     [?product :product/stock ?stock]]
    [(stock ?product ?stock)
     [?product :product/digital true]
     [(ground 100) ?stock]]
    [(can-sell? ?product)
     (stock ?product ?stock)
     [(> ?stock 0)]]
    [(product-in-category ?product ?category-name)
     [?category :category/name ?category-name]
     [?product :product/category ?category]]
    ])

(s/defn all-salable-products :- [model/Product] [db]
  (datomic-to-entity
    (d/q '[:find [(pull ?product [* {:product/category [*]}]) ...]
           :in $ %
           :where (can-sell? ?product)]
         db rules)))

(s/defn one-salable-product :- (s/maybe model/Product) [db, product-id :- java.util.UUID]
  (let [query '[:find (pull ?product [* {:product/category [*]}]) .
                :in $ % ?id
                :where [?product :product/id ?id]
                (can-sell? ?product)]
        result (d/q query db rules product-id)
        product (datomic-to-entity result)]
    (if (:product/id product)
      product
      nil)))

(s/defn all-products-in-categories :- [model/Product] [db, categories :- [s/Str]]
  (datomic-to-entity
    (d/q '[:find [(pull ?product [* {:product/category [*]}]) ...]
           :in $ % [?category-name ...]
           :where
           (product-in-category ?product ?category-name)]
         db, rules, categories)))

(s/defn all-products-in-categories-and-digital [db, categories :- [s/Str], digital? :- s/Bool]
  (datomic-to-entity
    (d/q '[:find [(pull ?product [* {:product/category [*]}]) ...]
           :in $ % [?category-name ...] ?is-digital?
           :where
           (product-in-category ?product ?category-name)
           [?product :product/digital ?is-digital?]]
         db, rules, categories, digital?)))

(s/defn update-price! [conn, product-id :- java.util.UUID, old, new]
  (d/transact conn [[:db/cas [:product/id product-id] :product/price old new]]))

(s/defn update-product! [conn, old :- model/Product, to-update :- model/Product]
  (let [product-id (:product/id old)
        attributes (cset/intersection (set (keys old)) (set (keys to-update)))
        attributes (disj attributes :product/id)
        txs (map (fn [attr] [:db/cas [:product/id product-id] attr (get old attr) (get to-update attr)]) attributes)]
    (d/transact conn txs))
  )

(s/defn add-variant! [conn, product-id :- java.util.UUID, variant :- s/Str, price :- BigDecimal]
  (d/transact conn [{
                     :db/id         "variant-temp-id"
                     :variant/name  variant
                     :variant/price price
                     :variant/id    (model/uuid)}
                    {:product/id      product-id
                     :product/variant "variant-temp-id"}]))

(defn count-products [db]
  (d/q '[:find [(count ?product)]
         :where [?product :product/name]]
       db))

(s/defn remove-product!
  [conn product-id :- java.util.UUID]
  (d/transact conn [[:db/retractEntity [:product/id product-id]]]))

; danger is not atomic
;(s/defn views [db product-id :- java.util.UUID]
;  (or (d/q '[:find ?views .
;             :in $ id
;             :where [?p :product/id ?id]
;             [?p :product/views ?views]]
;           db product-id) 0))
;
;(s/defn views! [conn product-id :- java.util.UUID]
;  (let [current-views (views (d/db conn) product-id)
;        new-value (inc current-views)]
;    (d/transact conn [{:product/id    product-id
;                       :product/views new-value}]))
;  )

(s/defn views! [conn product-id :- java.util.UUID]
  (d/transact conn [[:inc-views product-id]]))




