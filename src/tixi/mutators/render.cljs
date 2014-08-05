(ns tixi.mutators.render
  (:require [tixi.data :as d]
            [tixi.utils :refer [p]]
            [tixi.items :as i]))

(def ^:private touched-item-ids (atom #{}))

(defn reset-touched-items! []
  (reset! touched-item-ids #{}))

(defn touch-item! [id]
  (swap! touched-item-ids conj id))

(defn render-items!
  ([]
   (render-items! d/data))
  ([data]
    (doseq [[id item] (filter (fn [[id item]]
                                (or (contains? @touched-item-ids id)
                                    (not (d/item-cache id))))
                              (d/completed @data))]
      (swap! data assoc-in [:cache id] (i/render item)))
    (when-let [{:keys [id item]} (d/current)]
      (swap! data assoc-in [:cache id] (i/render item)))))
