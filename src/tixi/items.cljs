(ns tixi.items
  (:require [tixi.drawer :as dr]
            [tixi.geometry :as g]
            [tixi.utils :refer [p]]))

(defprotocol IItemBase
  (point-like? [this])
  (update [this point])
  (dimensions [this])
  (reposition [this input])
  (selection-rect [this])
  (origin [this]))

(defprotocol IItemCustom
  (kind [this])
  (-builder [this])
  (-parse [this]))

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

(defrecord Item [input text cache]
  IItemBase
  (point-like? [this] false)
  (dimensions [this]
    (g/dimensions (:input this)))
  (origin [this]
    (g/origin (:input this)))
  (update [this point]
    (recache ((-builder this) (g/expand (:input this) point) (:text this) nil) true))
  (reposition [this input]
    (let [dimensions-changed? (not= (g/dimensions (:input this)) (g/dimensions input))]
      (recache ((-builder this) input (:text this) (:cache this)) dimensions-changed?))))

(defrecord Text [input text cache]
  IItem
  (kind [this] "text")
  (update [this point]
    (recache ((-builder this) (g/build-rect point point) (:text this)) nil))
  (dimensions [this] (g/Size. 1 1))
  (origin [this] (g/origin (:input this)))
  (-builder [this] build-text)
  (-parse [this] {}))

(defn- build-line [input text cache]
  (specify (Item. input text cache)
    IItemCustom
    (kind [this] "line")
    (-builder [this] build-line)
    (-parse [this]
      (parse-line input))))

(defn- build-rect [input text cache]
  (specify (Item. input text cache)
    IItemCustom
    (kind [this] "rect")
    (-builder [this] build-rect)
    (-parse [this]
      (if (g/line? input)
        (parse-line input)
        (concat-and-normalize-lines input (fn [[nx1 ny1 nx2 ny2]]
                                            [[nx1 ny1 nx2 ny1]
                                             [nx1 ny1 nx1 ny2]
                                             [nx2 ny1 nx2 ny2]
                                             [nx1 ny2 nx2 ny2]]))))))

(defn- build-rect-line [input text cache]
  (specify (Item. input text cache)
    IItemCustom
    (kind [this] "rect-line")
    (-builder [this] build-rect-line)
    (-parse [this]
      (if (g/line? input)
        (parse-line input)
        (concat-and-normalize-lines input (fn [[nx1 ny1 nx2 ny2]]
                                            [[nx1 ny1 nx1 ny2]
                                             [nx1 ny2 nx2 ny2]]))))))

(defn- build-text [input text cache]
  (specify (Item. input text cache)
    IItemBase
    (point-like? [this] true)
    (update [this point]
      (recache ((-builder this) (g/build-rect point point) (:text this) (:cache this)) false))
    (reposition [this input]
      (let [dimensions-changed? (not= (g/dimensions (:input this)) (g/dimensions input))]
        (if dimensions-changed?
          this
          (recache ((-builder this) input (:text this) (:cache this)) false))))

    IItemCustom
    (kind [this] "text")
    (-builder [this] build-text)
    (-parse [this] {})))

(defn build-item [type & args]
  (let [builder (case type
                  :line build-line
                  :rect build-rect
                  :rect-line build-rect-line
                  :text build-text)]
    (recache (apply builder args) true)))

(defn set-text [item text dimensions]
  (let [input (if (point-like? item)
                (g/build-rect (g/origin (:input item)) (g/decr dimensions))
                (:input item))]
    (recache ((-builder item) input text nil) true)))
