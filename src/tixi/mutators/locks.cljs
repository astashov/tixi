(ns tixi.mutators.locks
  (:require [tixi.data :as d]
            [tixi.geometry :as g]
            [tixi.mutators.shared :as ms]
            [tixi.items :as i]
            [tixi.position :as p]
            [tixi.google-analytics :as ga]
            [tixi.utils :refer [p get-by-val]]))


(declare update-item-chars)

(defn- remove-lock! [connector-id connector-item connector-edge]
  (if-let [outlet-id (d/outlet-id connector-id connector-edge)]
    (do
      (ms/update-state! update-in [:locks :outlets outlet-id] dissoc [connector-id connector-edge])
      (ms/update-state! update-in [:locks :connectors connector-id] dissoc connector-edge)
      (assoc connector-item :connected (disj (or (:connected connector-item) #{}) connector-edge)))
    connector-item))

(defn- add-lock! [connector-id connector-item outlet-id outlet-item outlet connector-edge]
  (if-let [outlet-id (d/outlet-id connector-id connector-edge)]
    connector-item
    (do
      (ga/event! "draw" "add-lock")
      (ms/update-state! assoc-in [:locks :outlets outlet-id [connector-id connector-edge]] outlet)
      (ms/update-state! assoc-in [:locks :connectors connector-id connector-edge] outlet-id)
      (assoc connector-item :connected (conj (or (:connected connector-item) #{}) connector-edge)))))

(defn delete-from-locks! [id item]
  (remove-lock! id item :start)
  (remove-lock! id item :end)
  (doseq [[connector-id connector-edge] (keys (d/outlet id))]
    (remove-lock! connector-id (d/completed-item connector-id) connector-edge))
  (ms/update-state! update-in [:locks :connectors] dissoc id)
  (ms/update-state! update-in [:locks :outlets] dissoc id))

(defn try-to-lock! [connector-item connector-id connector-edge point]
  (if (i/connector? connector-item)
    (if-let [[outlet-id outlet-item outlet] (first (p/items-with-outlet-at-point connector-id point))]
      (add-lock! connector-id connector-item outlet-id outlet-item outlet connector-edge)
      (remove-lock! connector-id connector-item connector-edge))
    connector-item))
