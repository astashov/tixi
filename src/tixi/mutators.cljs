(ns tixi.mutators
  (:require [tixi.data :as d]
            [tixi.drawer :as dr]
            [tixi.position :as p]
            [tixi.geometry :as g :refer [Size]]
            [tixi.utils :refer [p seq-contains?]]))

(defn- find-layer-under-cursor [point]
  (first (p/items-at-point point)))

(defn- cache-item! [id]
  (swap! d/data assoc-in [:completed id :cache] (dr/render (get-in @d/data [:completed id]))))

(defn- reposition-item! [id rect]
  (swap! d/data assoc-in [:completed id :input] rect))

(defn- adjust-layers-to-selection! []
  (let [sel-rect (d/selection-rect)]
    (doseq [id (d/selected-ids)]
      (let [rel-rect (d/selected-rel-rect id)]
        (reposition-item! id (g/absolute sel-rect rel-rect))
        (cache-item! id)))))

(defn- update-selection-rect! [new-rect]
  (swap! d/data assoc-in [:selection :rect] new-rect)
  (adjust-layers-to-selection!))

(defn- build-layer! [type content]
  (let [id (:autoincrement @d/data)
        item {:type type :input content :cache nil}]
    (swap! d/data update-in [:autoincrement] inc)
    {:id id :item (assoc item :cache (dr/render item))}))


(defn reset-data! []
  (reset! d/data d/initial-data))

(defn set-tool! [name]
  (swap! d/data assoc :tool name))

(defn set-action! [name]
  (swap! d/data assoc :action name))

(defn highlight-layer! [point]
  (if-let [[id _] (find-layer-under-cursor point)]
    (swap! d/data assoc :hover-id id)
    (swap! d/data assoc :hover-id nil)))


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

(defn select-layer!
  ([point] (select-layer! point false))
  ([point add-more?]
  (if-let [[id _] (find-layer-under-cursor point)]
    (if add-more?
      (swap! d/data update-in [:selection :ids] conj id)
      (when-not (some #{id} (d/selected-ids))
        (swap! d/data assoc-in [:selection :ids] [id])))
    (do
      (swap! d/data assoc-in [:selection :ids] [])
      (start-selection! point)))
  (set-selection-rel-rects!)
  (swap! d/data assoc-in [:selection :rect] (p/items-wrapping-rect (d/selected-ids)))))


(defn initiate-current-layer! [point]
  (swap! d/data assoc :current (build-layer! (d/tool) (g/build-rect point point))))

(defn update-current-layer! [point]
  (when (d/current)
    (swap! d/data update-in [:current :item :input] g/expand point)
    (swap! d/data assoc-in [:current :item :cache] (dr/render (get-in @d/data [:current :item])))))

(defn finish-current-layer! []
  (when-let [{:keys [id item]} (d/current)]
    (swap! d/data assoc :current nil)
    (swap! d/data update-in [:completed] assoc id item)))


(defn move-selection! [diff]
  (when-let [rect (d/selection-rect)]
    (update-selection-rect! (g/move rect diff))))

(defn resize-selection! [diff type]
  (when-let [rect (d/selection-rect)]
    (update-selection-rect! (g/resize rect diff type))))

(defn delete-selected! []
  (let [ids (d/selected-ids)]
    (select-layer! [-1 -1] false)
    (doseq [id ids]
      (swap! d/data update-in [:completed] dissoc id))))
