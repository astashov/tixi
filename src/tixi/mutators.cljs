(ns tixi.mutators
  (:require [tixi.data :as d]
            [tixi.drawer :as dr]
            [tixi.position :as p]
            [tixi.utils :refer [p seq-contains?]]))

(defn set-tool! [name]
  (swap! d/data assoc :tool name))

(defn set-action! [name]
  (swap! d/data assoc :action name))


(defn- find-layer-under-cursor [[x y]]
  (first (p/items-from-text-coords [x y])))

(defn- cache-item! [id]
  (swap! d/data assoc-in [:completed id :cache] (dr/render (get-in @d/data [:completed id]))))

(defn- reposition-item! [id coords]
  (swap! d/data assoc-in [:completed id :input] coords))

(defn- adjust-layers-to-selection! []
  (let [[selx1 sely1 selx2 sely2] (d/selection-edges)
        selwidth (- selx2 selx1)
        selheight (- sely2 sely1)]
    (doseq [id (d/selected-ids)]
      (let [[relx1 rely1 relx2 rely2] (d/selected-rel-size id)
            newx1 (.round js/Math (+ selx1 (* selwidth relx1)))
            newy1 (.round js/Math (+ sely1 (* selheight rely1)))
            newx2 (.round js/Math (+ selx1 (* selwidth relx2)))
            newy2 (.round js/Math (+ sely1 (* selheight rely2)))]
        (reposition-item! id [newx1 newy1 newx2 newy2])
        (cache-item! id)))))

(defn- move-selection-edges! [[dx dy] f]
  (when-let [[x1 y1 x2 y2] (d/selection-edges)]
    (swap! d/data assoc-in [:selection :edges] (f x1 y1 x2 y2 dx dy))
    (adjust-layers-to-selection!)))

(defn highlight-layer! [[x y]]
  (if-let [[id _] (find-layer-under-cursor [x y])]
    (swap! d/data assoc :hover-id id)
    (swap! d/data assoc :hover-id nil)))

(defn move-selection! [[dx dy]]
  (move-selection-edges!
    [dx dy]
    (fn [x1 y1 x2 y2 dx dy] [(+ x1 dx) (+ y1 dy) (+ x2 dx) (+ y2 dy)])))

(defn set-selection-rel-sizes! []
  (let [ids (d/selected-ids)
        [selx1 sely1 selx2 sely2] (p/wrapping-edges ids)
        selwidth (- selx2 selx1)
        selheight (- sely2 sely1)]
    (doseq [id ids]
      (let [[x1 y1 x2 y2] (:input (d/completed-item id))
            relx1 (/ (- x1 selx1) selwidth)
            rely1 (/ (- y1 sely1) selheight)
            relx2 (/ (- x2 selx1) selwidth)
            rely2 (/ (- y2 sely1) selheight)]
        (swap! d/data assoc-in [:selection :rel-sizes id] [relx1 rely1 relx2 rely2])))))

(defn start-selection! [[x y]]
  (swap! d/data assoc-in [:selection :current] [x y x y]))

(defn update-selection! [[x y]]
  (when (d/current-selection)
    (swap! d/data assoc-in [:selection :current 2] x)
    (swap! d/data assoc-in [:selection :current 3] y)))

(defn finish-selection! []
  (let [[x1 y1 x2 y2] (d/current-selection)]
    (when (and (not= x1 x2) (not= y1 y2))
      (swap! d/data assoc-in [:selection :ids] (map first (p/items-inside-coords (d/current-selection))))
      (set-selection-rel-sizes!)
      (swap! d/data assoc-in [:selection :edges] (p/wrapping-edges (d/selected-ids)))))
  (swap! d/data assoc-in [:selection :current] nil))

(defn select-layer! [[x y] add-more?]
  (if-let [[id _] (find-layer-under-cursor [x y])]
    (if add-more?
      (swap! d/data update-in [:selection :ids] conj id)
      (when-not (some #{id} (d/selected-ids))
        (swap! d/data assoc-in [:selection :ids] [id])))
    (do
      (swap! d/data assoc-in [:selection :ids] [])
      (start-selection! [x y])))
  (set-selection-rel-sizes!)
  (swap! d/data assoc-in [:selection :edges] (p/wrapping-edges (d/selected-ids))))

(defn- build-layer! [type content]
  (let [id (:autoincrement @d/data)
        item {:type type :input content :cache nil}]
    (swap! d/data update-in [:autoincrement] inc)
    {:id id :item (assoc item :cache (dr/render item))}))

(defn initiate-current-layer! [[x y]]
  (swap! d/data assoc :current (build-layer! (d/tool) [x y x y])))

(defn update-current-layer! [[x y]]
  (when (d/current)
    (swap! d/data assoc-in [:current :item :input 2] x)
    (swap! d/data assoc-in [:current :item :input 3] y)
    (swap! d/data assoc-in [:current :item :cache] (dr/render (get-in @d/data [:current :item])))))

(defn finish-current-layer! []
  (when-let [{:keys [id item]} (d/current)]
    (swap! d/data assoc :current nil)
    (swap! d/data update-in [:completed] assoc id item)))

(defn- recalculate-edges-fn [type]
  (fn [x1 y1 x2 y2 dx dy]
    (case type
      :nw [(+ x1 dx) (+ y1 dy) x2        y2       ]
      :n  [x1        (+ y1 dy) x2        y2       ]
      :ne [x1        (+ y1 dy) (+ x2 dx) y2       ]
      :w  [(+ x1 dx) y1        x2        y2       ]
      :e  [x1        y1        (+ x2 dx) y2       ]
      :sw [(+ x1 dx) y1        x2        (+ y2 dy)]
      :s  [x1        y1        x2        (+ y2 dy)]
      :se [x1        y1        (+ x2 dx) (+ y2 dy)])))


(defn resize-selection! [[dx dy] type]
  (move-selection-edges! [dx dy] (recalculate-edges-fn type)))

(defn delete-selected! []
  (let [ids (d/selected-ids)]
    (select-layer! [-1 -1] false)
    (doseq [id ids]
      (swap! d/data update-in [:completed] dissoc id))))
