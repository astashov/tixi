(ns tixi.items
  (:require-macros [tixi.utils :refer (b defpoly defpoly-)])
  (:require [tixi.geometry :as g]
            [tixi.utils :refer [p]]
            [clojure.string :as string]
            [tixi.drawer :as dr]))

(declare dimensions)

(defn- parse-line [rect]
  (let [[x1 y1 x2 y2] (g/values (g/shifted-to-0 rect))]
    (dr/buildLine #js [x1 y1 x2 y2])))

(defn- arrow-char [item edge]
  (when (= (get-in item [:edges edge]) :arrow)
    (case edge
      :start
      (if (= (direction item) "vertical")
        (if (g/flipped-by-y? (:input item)) "v" "^")
        (if (g/flipped-by-x? (:input item)) ">" "<"))
      :end
      (if (= (direction item) (if (rect-line? item) "horizontal" "vertical"))
        (if (g/flipped-by-y? (:input item)) "^" "v")
        (if (g/flipped-by-x? (:input item)) "<" ">")))))

(defn- replace-char-in-cache! [item cache i chr]
  (when chr
    (let [index (.-index cache)
          points (.-points cache)
          point [(-> points (aget i) (aget 0) (aget 0))
                 (-> points (aget i) (aget 0) (aget 1))]]
      (aset (aget index (string/join "_" point)) "v" chr))))

(defn- maybe-add-edge-chars [item cache]
  (let [index (.-index cache)
        points (.-points cache)
        {:keys [input chars]} item]
    (if (contains? (or (:connected item) #{}) :start)
      (do
        (replace-char-in-cache! item cache 0 "+")
        (replace-char-in-cache! item cache 1 (arrow-char item :start)))
      (replace-char-in-cache! item cache 0 (arrow-char item :start)))
    (if (contains? (or (:connected item) #{}) :end)
      (do
        (replace-char-in-cache! item cache (-> points count dec) "+")
        (replace-char-in-cache! item cache (-> points count dec dec) (arrow-char item :end)))
      (replace-char-in-cache! item cache (-> points count dec) (arrow-char item :end)))
    cache))

(defpoly- parse [item]
  :line
  (maybe-add-edge-chars item (parse-line (:input item)))

  :rect
  (let [[x1 y1 x2 y2] (g/values (g/shifted-to-0 (:input item)))]
    (dr/buildRect #js [x1 y1 x2 y2]))

  :rect-line
  (let [result (let [[x1 y1 x2 y2] (g/values (g/shifted-to-0 (:input item)))]
                 (dr/buildRectLine #js [x1 y1 x2 y2] (:direction item)))]
    (maybe-add-edge-chars item result))

  :text
  #js {:index #js {} :points #js []})

(defn render [item]
  (let [result (parse item)
        points (.-points result)
        index (.-index result)
        sorted-points (dr/sortData points)
        [width height] (g/values (dimensions item))
        data (dr/generateData width height sorted-points)]
    {:points sorted-points :data data :index index}))

(defn dimensions [item]
  (g/dimensions (:input item)))

(defn origin [item]
  (g/origin (:input item)))

(defpoly point-like? [item]
  :text
  true

  false)

(defpoly direction [item]
  :rect-line
  (:direction item)

  :line
  (if (g/portrait? (dimensions item))
    "vertical"
    "horizontal")

  nil)

(defpoly connectors [item]
  #{:line :rect-line}
  [(g/build-point 0 0) (g/build-point 1 1)]

  [])

(defpoly outlets [item]
  :rect
  #{(g/build-point 0 0)    (g/build-point 0.25 0)    (g/build-point 0.5 0)    (g/build-point 0.75 0)    (g/build-point 1 0)
    (g/build-point 0 0.25) (g/build-point 1 0.25)
    (g/build-point 0 0.5)  (g/build-point 1 0.5)
    (g/build-point 0 0.75) (g/build-point 1 0.75)
    (g/build-point 0 1)    (g/build-point 0.25 1)    (g/build-point 0.5 1)    (g/build-point 0.75 1)    (g/build-point 1 1)}

  #{})

(defpoly update [item point]
  :rect-line
  (let [input (:input item)
        new-input (g/expand input point)
        direction (:direction item)
        new-direction (or direction
                          (when (not= input new-input)
                            (cond
                              (not= (:y point) (get-in input [:end :y])) "vertical"
                              (not= (:x point) (get-in input [:end :x])) "horizontal")))]
    (assoc item :input new-input :direction new-direction))

  :text
  (assoc item :input (g/build-rect point point))

  (let [new-input (g/expand (:input item) point)]
    (assoc item :input new-input)))

(defpoly reposition [item input]
  (assoc item :input input))

(defn set-text
  ([item text]
    (set-text item text nil))
  ([item text dimensions]
    (let [input (if (and (point-like? item) dimensions)
                  (g/build-rect (g/origin (:input item)) (g/decr dimensions))
                  (:input item))]
      (assoc item :input input :text text))))

(defpoly connector? [item]
  (boolean (not-empty (connectors item))))

(defn relative-point [item point]
  (g/relative point (:input item)))

(defn line? [item]
  (= (:type item) :line))

(defn rect-line? [item]
  (= (:type item) :rect-line))

(defn text? [item]
  (= (:type item) :text))

(defn pre-edge [edge]
  (keyword (str "pre-" (name edge))))

(defn edge-value [item edge]
  (or (get-in item [:chars (pre-edge edge)])
      (get-in item [:chars edge])))
