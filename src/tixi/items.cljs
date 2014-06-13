(ns tixi.items
  (:require [tixi.drawer :as dr]
            [tixi.geometry :as g]
            [tixi.utils :refer [p]]))

(defprotocol IItem
  (-builder [this])
  (-parse [this]))

(declare dimensions)
(declare build-line)
(declare build-rect)
(declare build-rect-line)

(defn- render [item]
  (let [points (dr/sort-data (-parse item))
        data (dr/generate-data (dimensions item) points)]
    {:points points :data data}))

(defn- recache [item rebuild?]
  ((-builder item) (:input item) (:text item) (if rebuild? (render item) (:cache item))))

(defn- parse-line [rect]
  (dr/build-line (g/values (g/shifted-to-0 rect))))

(defn- concat-and-normalize-lines [rect f]
  (dr/concat-lines (f (g/values (g/shifted-to-0 rect)))))


(defrecord Rect [input text cache]
  IItem
  (-builder [this] build-rect)
  (-parse [this]
    (if (g/line? input)
      (parse-line input)
      (concat-and-normalize-lines input (fn [[nx1 ny1 nx2 ny2]]
                                          [[nx1 ny1 nx2 ny1]
                                           [nx1 ny1 nx1 ny2]
                                           [nx2 ny1 nx2 ny2]
                                           [nx1 ny2 nx2 ny2]])))))

(defrecord Line [input text cache]
  IItem
  (-builder [this] build-line)
  (-parse [this]
    (parse-line input)))

(defrecord RectLine [input text cache]
  IItem
  (-builder [this] build-rect-line)
  (-parse [this]
    (if (g/line? input)
      (parse-line input)
      (concat-and-normalize-lines input (fn [[nx1 ny1 nx2 ny2]]
                                          [[nx1 ny1 nx1 ny2]
                                           [nx1 ny2 nx2 ny2]])))))

(defn- build-line [input text cache] (Line. input text cache))
(defn- build-rect [input text cache] (Rect. input text cache))
(defn- build-rect-line [input text cache] (RectLine. input text cache))


(defn build-item [type input]
  (let [builder (case type :line build-line :rect build-rect :rect-line build-rect-line)]
    (recache (builder input nil nil) true)))

(defn dimensions [this]
  (g/dimensions (:input this)))

(defn origin [this]
  (g/origin (:input this)))

(defn update [item point]
  (recache ((-builder item) (g/expand (:input item) point) (:text item) nil) true))

(defn reposition [item input]
  (let [dimensions-changed? (not= (g/dimensions (:input item)) (g/dimensions input))]
    (recache ((-builder item) input (:text item) (:cache item)) dimensions-changed?)))

(defn set-text [item text]
  ((-builder item) (:input item) text (:cache item)))
