(ns tixi.mutators
  (:require [tixi.data :as d]
            [tixi.drawer :refer [parse]]
            [tixi.utils :refer [p seq-contains?]]))

(defn set-tool! [name]
  (swap! d/data assoc :tool name))

(defn set-action! [name]
  (swap! d/data assoc :action name))


(defn start-moving! [x y]
  (swap! d/data assoc :moving-from [x y]))

(defn finish-moving! []
  (swap! d/data assoc :moving-from nil))


(defn- find-layer-under-cursor [x y]
  (first (filter (fn [[_ item]]
                   (let [coords (keys (parse item))]
                     (or (seq-contains? coords [(dec x) y])
                         (seq-contains? coords [x y]))))
                 (d/completed))))

(defn- move-layer-edges! [id client-x client-y f]
  (if (and (d/moving-from) id)
    (let [[initial-x initial-y] (d/moving-from)
          [x1 y1 x2 y2] (get-in @d/data [:completed id :content])
          dx (- client-x initial-x)
          dy (- client-y initial-y)]
      (start-moving! client-x client-y)
      (swap! d/data assoc-in [:completed id :content] (f x1 y1 x2 y2 dx dy)))))


(defn highlight-layer! [x y]
  (if-let [[id _] (find-layer-under-cursor x y)]
    (swap! d/data assoc :hover-id id)
    (swap! d/data assoc :hover-id nil)))

(defn move-layer! [id x y]
  (move-layer-edges! id x y
           (fn [x1 y1 x2 y2 dx dy] [(+ x1 dx) (+ y1 dy) (+ x2 dx) (+ y2 dy)])))

(defn select-layer! [x y]
  (p "Selecting layer")
  (if-let [[id _] (find-layer-under-cursor x y)]
    (swap! d/data assoc :selected-id id)
    (swap! d/data assoc :selected-id nil))
  (start-moving! x y))


(defn- build-layer! [type content]
  (let [id (:autoincrement @d/data)]
    (swap! d/data update-in [:autoincrement] inc)
    [id {:type type :content content}]))

(defn initiate-current-layer! [x y]
  (swap! d/data assoc :current (build-layer! (d/tool) [x y x y])))

(defn update-current-layer! [x y]
  (when (d/current)
    (swap! d/data assoc-in [:current 1 :content 2] x)
    (swap! d/data assoc-in [:current 1 :content 3] y)))

(defn finish-current-layer! []
  (when-let [[id item] (d/current)]
    (swap! d/data assoc :current nil)
    (swap! d/data update-in [:completed] assoc id item)))


(defn resize! [id x y type]
  (let [f (fn [x1 y1 x2 y2 dx dy]
            (case type
              "nw" [(+ x1 dx) (+ y1 dy) x2        y2       ]
              "n"  [x1        (+ y1 dy) x2        y2       ]
              "ne" [x1        (+ y1 dy) (+ x2 dx) y2       ]
              "w"  [(+ x1 dx) y1        x2        y2       ]
              "e"  [x1        y1        (+ x2 dx) y2       ]
              "sw" [(+ x1 dx) y1        x2        (+ y2 dy)]
              "s"  [x1        y1        x2        (+ y2 dy)]
              "se" [x1        y1        (+ x2 dx) (+ y2 dy)]))]
    (move-layer-edges! id x y f)))
