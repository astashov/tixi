(ns tixi.mutators
  (:require-macros [tixi.utils :refer (b)])
  (:require [tixi.data :as d]
            [tixi.position :as p]
            [tixi.tree :as t]
            [tixi.items :as i]
            [clojure.zip :as z]
            [tixi.geometry :as g :refer [Size Point]]
            [tixi.utils :refer [p seq-contains? get-by-val]]))

(def ^:private touched-item-ids (atom #{}))

(defn- update-state! [f ks & args]
  (swap! d/data assoc-in [:state] (z/replace (d/state-loc)
                                             (t/update (z/node (d/state-loc))
                                                       (apply f (d/state) ks args)))))

(defn- absolute-rect [lockable-id connector-id types rect]
  (let [lockable-item (d/completed-item lockable-id)
        connector-item (d/completed-item connector-id)
        start-point (if (contains? types :start)
                      (g/absolute (:start-point rect) (:input lockable-item))
                      (:start-point (:input connector-item)))
        end-point (if (contains? types :end)
                      (g/absolute (:end-point rect) (:input lockable-item))
                      (:end-point (:input connector-item)))]
    (g/build-rect start-point end-point)))

(defn- reposition-item! [id rect]
  (let [item (d/completed-item id)
        maybe-locked-item (-> item
                              (try-to-lock! id (:start-point (:input item)) :start)
                              (try-to-lock! id (:end-point (:input item)) :end))]
    (update-state! assoc-in [:completed id] (i/reposition maybe-locked-item rect))
    (when (not= (g/dimensions rect) (g/dimensions (:input item)))
      (touch-item! id))
    (doseq [[connector-id {:keys [types rect]}] (d/lockable id)]
      (touch-item! connector-id)
      (update-state! assoc-in [:completed connector-id]
                              (i/reposition (d/completed-item connector-id)
                                            (absolute-rect id connector-id types rect))))))

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
        item {:type type :input content}]
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
    (let [{:keys [id item]} (d/current)
          maybe-locked-item (try-to-lock! item id point :end)]
      (swap! d/data assoc-in [:current :item] (i/update maybe-locked-item point)))))

(defn finish-current-layer! []
  (when-let [{:keys [id item]} (d/current)]
    (swap! d/data assoc :current nil)
    (update-state! assoc-in [:completed id] item)
    (if (= (:type item) :text)
      (edit-text-in-item! id))))


(defn move-selection! [diff]
  (when-let [rect (d/selection-rect)]
    (update-selection-rect! (g/move rect diff))))

(defn resize-selection! [diff type]
  (when-let [rect (d/selection-rect)]
    (update-selection-rect! (g/resize rect diff type))))

(defn- delete-connector-from-locks! [connector-id]
  (let [lockable-ids (vals (d/connector-types connector-id))]
    (doseq [lockable-id lockable-ids]
      (update-state! update-in [:locks :lockables lockable-id] dissoc connector-id)
      (when (empty? (d/lockable lockable-id))
        (update-state! update-in [:locks :lockables] dissoc lockable-id)))
    (update-state! update-in [:locks :connectors] dissoc connector-id)))

(defn delete-lockable-from-locks! [lockable-id]
  (let [connector-ids (keys (d/lockable lockable-id))]
    (doseq [connector-id connector-ids]
      (let [types (get-by-val (d/connector-types connector-id) lockable-id)]
        (doseq [type types]
          (let [new-item (remove-lock! connector-id (d/completed-item connector-id) type)]
            (update-state! assoc-in [:completed connector-id] new-item)))))
    (update-state! update-in [:locks :lockables] dissoc lockable-id)))

(defn- delete-from-locks! [id item]
  (cond
    (i/lockable? item) (delete-lockable-from-locks! id)
    (i/connector? item) (delete-connector-from-locks! id)))

(defn delete-selected! []
  (let [ids (d/selected-ids)]
    (when (not-empty ids)
      (snapshot!)
      (select-layer! nil)
      (doseq [id ids]
        (delete-from-locks! id (d/completed-item id))
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
    (swap! d/data assoc-in [:state] (z/up (d/state-loc)))
    (doseq [[id _] (d/completed)]
      (touch-item! id))))

(defn redo! []
  (when (can-redo?)
    (swap! d/data assoc-in [:state] (z/down (d/state-loc)))
    (doseq [[id _] (d/completed)]
      (touch-item! id))))


(defn edit-text-in-item! [id]
  (swap! d/data assoc :edit-text-id id))

(defn set-text-to-item! [id text dimensions]
  (update-state! assoc-in [:completed id] (i/set-text (d/completed-item id) text dimensions)))

;; 2 - rect
;; 4 - line
;; {:locks {:lockables  {2 {4 {:types #{:start :end}
;;                             :rect rect}}}}
;;          :connectors {4 {:end 2}}}

;; TODO: Need to cover locking by tests

(defn add-lock! [connector-id connector-item lockable-id lockable-item point type]
  (let [lockable-connector-data (d/lockable-connector lockable-id connector-id)
        input (:input connector-item)
        rect (or (:rect lockable-connector-data) input)
        new-types (into #{} (conj (or (:types lockable-connector-data) #{}) type))
        new-rect ((if (= type :end) g/expand g/shrink) rect (i/relative-point lockable-item point))]
    (update-state! assoc-in [:locks :lockables lockable-id connector-id] {:types new-types :rect new-rect})
    (update-state! assoc-in [:locks :connectors connector-id type] lockable-id)
    (if (= type :end)
      (assoc connector-item :end-char "+")
      (assoc connector-item :start-char "+"))))

(defn remove-lock! [connector-id connector-item type]
  (let [lockable-id (d/lockable-id-by-connector-id-and-type connector-id type)
        lockable-item (d/completed-item lockable-id)
        lockable-connector-data (d/lockable-connector lockable-id connector-id)]
    (if (contains? (:types lockable-connector-data) type)
      (let [input (:input connector-item)
            rect (get lockable-connector-data :rect input)
            new-types (into #{} (disj (or (:types lockable-connector-data) #{}) type))
            new-rect ((if (= type :end) g/expand g/shrink) rect
                                                           ((if (= type :end) :end-point :start-point) input))]
        (if (empty? new-types)
          (update-state! update-in [:locks :lockables lockable-id] dissoc connector-id)
          (update-state! assoc-in [:locks :lockables lockable-id connector-id] {:types new-types :rect new-rect}))
        (update-state! update-in [:locks :connectors connector-id] dissoc type)

        (when (empty? (d/connector-types connector-id))
          (update-state! update-in [:locks :connectors] dissoc connector-id))
        (when (empty? (d/lockable lockable-id))
          (update-state! update-in [:locks :lockables] dissoc lockable-id))
        (if (= type :end)
          (dissoc connector-item :end-char)
          (dissoc connector-item :start-char)))
      connector-item)))

(defn try-to-lock! [connector-item connector-id point type]
  (if (i/connector? connector-item)
    (let [[lockable-id lockable-item] (->> (p/items-at-point point)
                                           (filter (fn [[id _]] (not= id connector-id)))
                                           first)]
      (if (and lockable-item (i/lockable? lockable-item) (i/connector? connector-item))
        (add-lock! connector-id connector-item lockable-id lockable-item point type)
        (remove-lock! connector-id connector-item type)))
    connector-item))

(defn show-result! [value]
  (swap! d/data assoc :show-result value))

(defn render-items! []
  (doseq [[id item] (filter (fn [[id item]]
                              (or (contains? @touched-item-ids id)
                                  (not (d/item-cache id))))
                            (d/completed))]
    (swap! d/data assoc-in [:cache id] (i/render item)))
  (when-let [{:keys [id item]} (d/current)]
    (swap! d/data assoc-in [:cache id] (i/render item))))

(defn reset-touched-items! []
  (reset! touched-item-ids #{}))

(defn touch-item! [id]
  (swap! touched-item-ids conj id))
