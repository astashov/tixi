(ns tixi.items
  (:require-macros [tixi.utils :refer (b)])
  (:require [tixi.drawer :as dr]
            [tixi.geometry :as g]
            [tixi.utils :refer [p]]))

(defprotocol IItemBase
  (point-like? [this])
  (update [this point])
  (dimensions [this])
  (reposition [this input])
  (selection-rect [this])
  (origin [this])
  (set-text [this text dimensions]))

(defprotocol IItemLock
  (lockable? [this])
  (connector? [this]))

(defprotocol IItemCustom
  (kind [this])
  (-builder [this])
  (-parse [this]))

(defprotocol IItemLine
  (start-char [this])
  (end-char [this]))

(defprotocol IItemRelative
  (relative-point [this point]))

(defn- render [item]
  (let [points (dr/sort-data (-parse item))
        data (dr/generate-data (dimensions item) points)]
    {:points points :data data}))

(defn- recache [item rebuild?]
  ((-builder item) {:input (:input item)
                    :text (:text item)
                    :cache (if rebuild? (render item) (:cache item))}))

(defn- parse-line [rect]
  (dr/build-line (g/values (g/shifted-to-0 rect))))

(defn- concat-and-normalize-lines [rect f]
  (dr/concat-lines (f (g/values (g/shifted-to-0 rect)))))

(defn- maybe-add-edge-chars [f args]
  (let [result (f)
        {:keys [input start-char end-char]} args
        shifted-input (g/shifted-to-0 input)]
    (cond
      (and start-char end-char) (assoc result (:start-point shifted-input) start-char
                                              (:end-point shifted-input) end-char)
      start-char (assoc result (:start-point shifted-input) start-char)
      end-char (assoc result (:end-point shifted-input) end-char)
      :else result)))

(defrecord Item [input text cache]
  IItemBase
  (point-like? [this] false)
  (dimensions [this]
    (g/dimensions (:input this)))
  (origin [this]
    (g/origin (:input this)))
  (update [this point]
    (recache ((-builder this) {:input (g/expand (:input this) point) :text (:text this)}) true))
  (reposition [this input]
    (let [dimensions-changed? (not= (g/dimensions (:input this)) (g/dimensions input))]
      (recache ((-builder this) {:input input :text (:text this) :cache (:cache this)}) dimensions-changed?)))
  (set-text [this text dimensions]
    (let [input (if (point-like? this)
                  (g/build-rect (g/origin (:input this)) (g/decr dimensions))
                  (:input this))]
      (recache ((-builder this) {:input input :text text}) true))))

(defn- build-line [args]
  (let [{:keys [input text cache]} args]
    (specify (Item. input text cache)
      IItemLine
      (start-char [this] (:start-char args))
      (end-char [this] (:end-char args))

      IItemCustom
      (kind [this] "line")
      (-builder [this]
        (fn [new-args] (build-line (merge args new-args))))
      (-parse [this]
        (maybe-add-edge-chars #(parse-line input) args))

      IItemLock
      (lockable? [this] false)
      (connector? [this] true))))

(defn- build-rect [args]
  (let [{:keys [input text cache]} args]
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
                                               [nx1 ny2 nx2 ny2]]))))
      IItemRelative
      (relative-point [this point]
        (g/relative point (:input this)))

      IItemLock
      (lockable? [this] true)
      (connector? [this] false))))

(defn- build-rect-line [args]
  (let [{:keys [input text cache]} args]
    (specify (Item. input text cache)
      IItemLine
      (start-char [this] (:start-char args))
      (end-char [this] (:end-char args))

      IItemCustom
      (kind [this] "rect-line")
      (-builder [this]
        (fn [new-args] (build-rect-line (merge args new-args))))
      (-parse [this]
        (maybe-add-edge-chars #(if (g/line? input)
                                 (parse-line input)
                                 (concat-and-normalize-lines input (fn [[nx1 ny1 nx2 ny2]]
                                                                     [[nx1 ny1 nx1 ny2]
                                                                      [nx1 ny2 nx2 ny2]])))
                              args))
      IItemLock
      (lockable? [this] false)
      (connector? [this] true))))

(defn- build-text [args]
  (let [{:keys [input text cache]} args]
    (specify (Item. input text cache)
      IItemBase
      (point-like? [this] true)
      (update [this point]
        (recache ((-builder this) (assoc args :input (g/build-rect point point))) false))
      (reposition [this input]
        (let [dimensions-changed? (not= (g/dimensions (:input this)) (g/dimensions input))]
          (if dimensions-changed?
            this
            (recache ((-builder this) (assoc args :input input)) false))))

      IItemCustom
      (kind [this] "text")
      (-builder [this] build-text)
      (-parse [this] {})

      IItemLock
      (lockable? [this] false)
      (connector? [this] false))))

(defn build-item [type args]
  (let [builder (case type
                  :line build-line
                  :rect build-rect
                  :rect-line build-rect-line
                  :text build-text)]
    (recache (builder args) true)))
