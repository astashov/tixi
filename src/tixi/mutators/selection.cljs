(ns tixi.mutators.selection
  (:require [tixi.data :as d]
            [tixi.items :as i]
            [tixi.geometry :as g]
            [tixi.mutators :as m]
            [tixi.mutators.shared :as ms]
            [tixi.mutators.render :as mr]
            [tixi.mutators.locks :as ml]
            [tixi.utils :refer [p]]
            [tixi.position :as p]))

(defn- absolute-rect [outlet-id connector-id connector-edge outlet]
  (let [outlet-item (d/completed-item outlet-id)
        connector-item (d/completed-item connector-id)
        start-point (if (= connector-edge :start)
                      (g/absolute outlet (:input outlet-item))
                      (:start (:input connector-item)))
        end-point (if (= connector-edge :end)
                      (g/absolute outlet (:input outlet-item))
                      (:end (:input connector-item)))]
    (g/build-rect start-point end-point)))

(defn set-selection-rel-rects! []
  (let [ids (d/selected-ids)
        wrapping-rect (p/items-wrapping-rect ids)]
    (doseq [id ids]
      (let [rect (:input (d/completed-item id))]
        (swap! d/data assoc-in [:selection :rel-rects id] (g/relative rect wrapping-rect))))))

(defn start-selection! [point]
  (swap! d/data assoc-in [:selection :current] (g/build-rect point point)))

(defn update-selection! [point]
  (when (d/current-selection)
    (swap! d/data update-in [:selection :current] g/expand point)))

(defn finish-selection! []
  (when-let [current-rect (d/current-selection)]
    (when (and (not= (g/width current-rect) 0)
               (not= (g/height current-rect) 0))
      (swap! d/data assoc-in [:selection :ids] (map first (p/items-inside-rect (d/current-selection))))
      (set-selection-rel-rects!)
      (swap! d/data assoc-in [:selection :rect] (p/items-wrapping-rect (d/selected-ids)))))
  (swap! d/data assoc-in [:selection :current] nil))

(defn highlight-layer! [id]
  (if id
    (swap! d/data assoc :hover-id id)
    (swap! d/data assoc :hover-id nil)))

(defn- reposition-item! [id rect]
  (let [item (d/completed-item id)
        maybe-locked-item (-> item
                              (ml/try-to-lock! id :start (:start rect))
                              (ml/try-to-lock! id :end (:end rect)))]
    (ms/update-state! assoc-in [:completed id] (i/reposition maybe-locked-item rect))
    (when (not= (g/dimensions rect) (g/dimensions (:input item)))
      (mr/touch-item! id))
    (doseq [[[connector-id connector-edge] outlet] (d/outlet id)]
      (mr/touch-item! connector-id)
      (ms/update-state! assoc-in [:completed connector-id]
                                 (i/reposition (d/completed-item connector-id)
                                               (absolute-rect id connector-id connector-edge outlet))))))

(defn- adjust-layers-to-selection! []
  (let [sel-rect (d/selection-rect)]
    (doseq [id (d/selected-ids)]
      (let [rel-rect (d/selected-rel-rect id)]
        (reposition-item! id (g/absolute sel-rect rel-rect))))))

(defn- update-selection-rect! [new-rect]
  (when (and (= (count (d/selected-ids)) 1)
                (i/connector? (first (d/selected-items))))
    (m/set-connecting-id! (first (d/selected-ids))))
  (swap! d/data assoc-in [:selection :rect] new-rect)
  (adjust-layers-to-selection!))

(defn move-selection! [diff]
  (when-let [rect (d/selection-rect)]
    (update-selection-rect! (g/move rect diff))))

(defn resize-selection! [diff type]
  (when-let [rect (d/selection-rect)]
    (update-selection-rect! (g/resize rect diff type))))

(defn select-layer!
  ([id] (select-layer! id nil))
  ([id point] (select-layer! id point false))
  ([id point add-more?]
  (if id
    (if add-more?
      (swap! d/data update-in [:selection :ids] conj id)
      (when-not (some #{id} (d/selected-ids))
        (swap! d/data assoc-in [:selection :ids] [id])))
    (do
      (swap! d/data assoc-in [:selection :ids] [])
      (when point
        (start-selection! point))))
  (set-selection-rel-rects!)
  (swap! d/data assoc-in [:selection :rect] (p/items-wrapping-rect (d/selected-ids)))))
