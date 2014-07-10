(ns tixi.items
  (:require-macros [tixi.utils :refer (b defpoly defpoly-)])
  (:require [tixi.geometry :as g]
            [tixi.data :as d]
            [tixi.utils :refer [p]]
            [tixi.drawer :as dr]))

(declare dimensions)

(defn- parse-line [rect]
  (let [[x1 y1 x2 y2] (g/values (g/shifted-to-0 rect))]
    (dr/buildLine #js [x1 y1 x2 y2])))

(defn- maybe-add-edge-chars [f item]
  (let [result (f)
        points (.-points result)
        index (.-index result)
        {:keys [input start-char end-char]} item
        shifted-input (g/shifted-to-0 input)
        [sx sy] (g/values (:start-point shifted-input))
        [ex ey] (g/values (:end-point shifted-input))]
    (when start-char
      (aset (aget index (str sx "_" sy)) "v" start-char))
    (when end-char
      (aset (aget index (str ex "_" ey)) "v" end-char))
    result))

(defpoly- parse [item]
  :line
  (maybe-add-edge-chars #(parse-line (:input item)) item)

  :rect
  (let [[x1 y1 x2 y2] (g/values (g/shifted-to-0 (:input item)))]
    (dr/buildRect #js [x1 y1 x2 y2]))

  :rect-line
  (let [result (let [[x1 y1 x2 y2] (g/values (g/shifted-to-0 (:input item)))]
                 (dr/buildRectLine #js [x1 y1 x2 y2] (:direction item)))]
    (maybe-add-edge-chars (fn [] result) item))

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

(defpoly update [item point]
  :rect-line
  (let [input (:input item)
        new-input (g/expand input point)
        direction (:direction item)
        new-direction (or direction
                          (when (not= input new-input)
                            (cond
                              (not= (:y point) (get-in input [:end-point :y])) "vertical"
                              (not= (:x point) (get-in input [:end-point :x])) "horizontal")))]
    (assoc item :input new-input :direction new-direction))

  :text
  (assoc item :input (g/build-rect point point))

  (let [new-input (g/expand (:input item) point)]
    (assoc item :input new-input)))

(defpoly reposition [item input]
  (assoc item :input input))

(defn set-text [item text dimensions]
  (let [input (if (point-like? item)
                (g/build-rect (g/origin (:input item)) (g/decr dimensions))
                (:input item))]
    (assoc item :input input :text text)))

(defpoly lockable? [item]
  :rect
  true

  false)

(defpoly connector? [item]
  #{:line :rect-line}
  true

  false)

(defn relative-point [item point]
  (g/relative point (:input item)))
