(ns tixi.mutators
  (:require [tixi.data :as d]
            [tixi.drawer :as dr]
            [tixi.position :as p]
            [tixi.tree :as t]
            [tixi.items :as i]
            [clojure.zip :as z]
            [tixi.geometry :as g :refer [Size Point]]
            [tixi.utils :refer [p seq-contains?]]))

(defn- update-state! [f ks & args]
  (swap! d/data assoc-in [:state] (z/replace (d/state-loc) (t/update (z/node (d/state-loc)) (apply f (d/state) ks args)))))

(defn- reposition-item! [id rect]
  (update-state! assoc-in [:completed id] (i/reposition (d/completed-item id) rect)))

(defn- adjust-layers-to-selection! []
  (let [sel-rect (d/selection-rect)]
    (doseq [id (d/selected-ids)]
      (let [rel-rect (d/selected-rel-rect id)]
        (reposition-item! id (g/absolute sel-rect rel-rect))))))

(defn- update-selection-rect! [new-rect]
  (swap! d/data assoc-in [:selection :rect] new-rect)
  (adjust-layers-to-selection!))

(defn- build-layer! [type content]
  (let [id (:autoincrement @d/data)
        item (i/build-item type content)]
    (swap! d/data update-in [:autoincrement] inc)
    {:id id :item item}))

(defn- can-undo? []
  (boolean (z/up (d/state-loc))))

(defn- can-redo? []
  (boolean (z/node (z/down (d/state-loc)))))


(defn reset-data! []
  (reset! d/data d/initial-data))

(defn set-tool! [name]
  (swap! d/data assoc :tool name))

(defn set-action! [name]
  (swap! d/data assoc :action name))

(defn highlight-layer! [id]
  (if id
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


(defn initiate-current-layer! [point]
  (swap! d/data assoc :current (build-layer! (d/tool) (g/build-rect point point))))

(defn update-current-layer! [point]
  (when (d/current)
    (let [{:keys [id item]} (d/current)]
      (swap! d/data assoc-in [:current :item] (i/update item point)))))

(defn finish-current-layer! []
  (when-let [{:keys [id item]} (d/current)]
    (swap! d/data assoc :current nil)
    (update-state! assoc-in [:completed id] item)))


(defn move-selection! [diff]
  (when-let [rect (d/selection-rect)]
    (update-selection-rect! (g/move rect diff))))

(defn resize-selection! [diff type]
  (when-let [rect (d/selection-rect)]
    (update-selection-rect! (g/resize rect diff type))))

(defn delete-selected! []
  (let [ids (d/selected-ids)]
    (when (not-empty ids)
      (snapshot!)
      (select-layer! nil)
      (doseq [id ids]
        (update-state! update-in [:completed] dissoc id)))))


(defn snapshot! []
  (swap! d/data assoc-in [:state] (-> (d/state-loc)
                                      (z/insert-child (t/node (d/state)))
                                      z/down)))

(defn undo-if-unchanged! []
  (when (and (z/up (d/state-loc))
             (= (d/state)
                (:value (z/node (z/up (d/state-loc))))))
    (undo!)))

(defn undo! []
  (when (can-undo?)
    (swap! d/data assoc-in [:state] (z/up (d/state-loc)))))

(defn redo! []
  (when (can-redo?)
    (swap! d/data assoc-in [:state] (z/down (d/state-loc)))))


(defn edit-text-in-item! [id]
  (swap! d/data assoc :edit-text-id id))

(defn set-text-to-item! [id text]
  (update-state! assoc-in [:completed id :text] text))
