(ns tixi.mutators.current
  (:require [tixi.data :as d]
            [tixi.geometry :as g]
            [tixi.mutators :as m]
            [tixi.mutators.locks :as ml]
            [tixi.mutators.text :as mt]
            [tixi.mutators.shared :as ms]
            [tixi.utils :refer [p]]
            [tixi.items :as i]))

(defn- build-layer! [type content]
  (let [id (:autoincrement @d/data)
        item {:type type :input content :z 0}]
    (swap! d/data update-in [:autoincrement] inc)
    {:id id :item item}))

(defn initiate-current-layer! [point]
  (swap! d/data assoc :current (build-layer! (d/tool) (g/build-rect point point)))
  (when (i/connector? (:item (d/current)))
    (m/set-connecting-id! (:id (d/current)))))

(defn update-current-layer! [point]
  (when (d/current)
    (let [{:keys [id item]} (d/current)
          maybe-locked-item (ml/try-to-lock! item id :end point)]
      (swap! d/data assoc-in [:current :item] (i/update maybe-locked-item point)))))

(defn finish-current-layer! []
  (when-let [{:keys [id item]} (d/current)]
    (swap! d/data assoc :current nil)
    (ms/update-state! assoc-in [:completed id] item)
    (m/set-connecting-id! nil)
    (when (i/text? item)
      (mt/edit-text-in-item! id))))
