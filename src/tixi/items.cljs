(ns tixi.items
  (:require-macros [tixi.utils :refer (b defpoly defpoly-)])
  (:require [tixi.geometry :as g]
            [tixi.utils :refer [p]]
            [tixi.drawer :as dr]))

(declare dimensions)

(defn- parse-line [rect]
  (let [[x1 y1 x2 y2] (g/values (g/shifted-to-0 rect))]
    (dr/buildLine #js [x1 y1 x2 y2])))

(defn- maybe-add-edge-chars [cache item]
  (let [index (.-index cache)
        {:keys [input chars]} item
        shifted-input (g/shifted-to-0 input)
        [sx sy] (g/values (:start shifted-input))
        [ex ey] (g/values (:end shifted-input))]
    (when (:start chars)
      (aset (aget index (str sx "_" sy)) "v" (:start chars)))
    (when (:end chars)
      (aset (aget index (str ex "_" ey)) "v" (:end chars)))
    cache))

(defpoly- parse [item]
  :line
  (maybe-add-edge-chars (parse-line (:input item)) item)

  :rect
  (let [[x1 y1 x2 y2] (g/values (g/shifted-to-0 (:input item)))]
    (dr/buildRect #js [x1 y1 x2 y2]))

  :rect-line
  (let [result (let [[x1 y1 x2 y2] (g/values (g/shifted-to-0 (:input item)))]
                 (dr/buildRectLine #js [x1 y1 x2 y2] (:direction item)))]
    (maybe-add-edge-chars result item))

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
  [(g/Point. 0 0) (g/Point. 1 1)]

  [])

(defpoly outlets [item]
  :rect
  #{(g/Point. 0 0)    (g/Point. 0.25 0)    (g/Point. 0.5 0)    (g/Point. 0.75 0)    (g/Point. 1 0)
    (g/Point. 0 0.25) (g/Point. 1 0.25)
    (g/Point. 0 0.5)  (g/Point. 1 0.5)
    (g/Point. 0 0.75) (g/Point. 1 0.75)
    (g/Point. 0 1)    (g/Point. 0.25 1)    (g/Point. 0.5 1)    (g/Point. 0.75 1)    (g/Point. 1 1)}

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

(defn text? [item]
  (= (:type item) :text))
