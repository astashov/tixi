(ns tixi.mutators
  (:require-macros [tixi.utils :refer (b)])
  (:require [tixi.data :as d]
            [tixi.mutators.render :as mr]
            [tixi.mutators.shared :as ms]
            [tixi.mutators.locks :as ml]
            [tixi.items :as i]
            [tixi.utils :refer [p next-of]]))

(defn reset-data! []
  (reset! d/data d/initial-data))

(defn set-tool! [name]
  (swap! d/data assoc :tool name))

(defn set-action! [name]
  (swap! d/data assoc :action name))

(defn show-result! [value]
  (swap! d/data assoc :show-result value))

(defn set-connecting-id! [id]
  (swap! d/data assoc :connecting-id id))

(defn z-inc! [ids]
  (doseq [id (sort-by #(:z (d/completed-item %)) ids)]
    (let [current-z (:z (d/completed-item id))
          max-z (or (apply max (map (fn [[_ i]] (:z i))
                                    (filter (fn [[id1 _]] (not= id id1))
                                            (d/completed))))
                    0)]
      (ms/update-state! assoc-in [:completed id :z]
                        (min (inc current-z) (inc max-z))))))

(defn z-dec! [ids]
  (doseq [id (sort-by #(:z (d/completed-item %)) ids)]
    (let [current-z (:z (d/completed-item id))]
      (ms/update-state! assoc-in [:completed id :z]
                        (max (dec current-z) 0)))))

(defn z-show! [bool]
  (swap! d/data assoc :show-z-indexes? bool))

(defn toggle-grid! [bool]
  (swap! d/data assoc :show-grid? bool))

(defn cycle-line-edge! [edge]
  (swap! d/data assoc-in [:line-edges edge] (next-of d/line-edge-chars (edge (d/line-edges)))))

(defn cycle-selection-edges! [edge]
  (let [edge-value (d/next-selected-edge-value edge)]
    (doseq [[id item] (reduce (fn [memo id]
                                (let [item (d/completed-item id)]
                                  (if (i/connector? item)
                                    (conj memo [id item])
                                    memo)))
                              []
                              (d/selected-ids))]
      (ms/update-state! assoc-in [:completed id :edges edge] edge-value)
      (mr/touch-item! id))))

(defn set-canvas-size! [dimensions]
  (swap! d/data assoc-in [:canvas-size] dimensions))
