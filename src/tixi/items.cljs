(ns tixi.items
  (:require-macros [tixi.utils :refer (b)])
  (:require [tixi.geometry :as g]
            [tixi.utils :refer [p]]
            [tixi.drawer :as dr]))

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
  (let [result (-parse item)
        points (.-points result)
        index (.-index result)
        sorted-points (dr/sortData points)
        [width height] (g/values (dimensions item))
        data (dr/generateData width height sorted-points)]
    {:points sorted-points :data data :index index}))

(defn- recache [item rebuild?]
  ((-builder item) {:input (:input item)
                    :text (:text item)
                    :cache (if rebuild? (render item) (:cache item))}))

(defn- parse-line [rect]
  (let [[x1 y1 x2 y2] (g/values (g/shifted-to-0 rect))]
    (dr/buildLine #js [x1 y1 x2 y2])))

(defn- maybe-add-edge-chars [f args]
  (let [result (f)
        points (.-points result)
        index (.-index result)
        {:keys [input start-char end-char]} args
        shifted-input (g/shifted-to-0 input)
        [sx sy] (g/values (:start-point shifted-input))
        [ex ey] (g/values (:end-point shifted-input))]
    (when start-char
      (aset (aget index (str sx "_" sy)) "v" start-char))
    (when end-char
      (aset (aget index (str ex "_" ey)) "v" end-char))
    result))

(defrecord Item [input text cache type]
  IItemBase
  (point-like? [this] false)
  (dimensions [this]
    (g/dimensions (:input this)))
  (origin [this]
    (g/origin (:input this)))
  (update [this point]
    (let [new-input (g/expand (:input this) point)]
      (recache ((-builder this) {:input new-input :text (:text this)}) true)))
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
    (specify (Item. input text cache :line)
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
    (specify (Item. input text cache :rect)
      IItemCustom
      (kind [this] "rect")
      (-builder [this] build-rect)
      (-parse [this]
        (let [[x1 y1 x2 y2] (g/values (g/shifted-to-0 input))]
          (dr/buildRect #js [x1 y1 x2 y2])))
      IItemRelative
      (relative-point [this point]
        (g/relative point (:input this)))

      IItemLock
      (lockable? [this] true)
      (connector? [this] false))))

(defn- build-rect-line [args]
  (let [{:keys [input text cache]} args]
    (specify (Item. input text cache :rect-line)
      IItemBase
      (update [this point]
        (let [new-input (g/expand (:input this) point)
              direction (:direction args)
              new-direction (or direction
                                (when (not= input new-input)
                                  (cond
                                    (not= (:y point) (get-in input [:end-point :y])) "vertical"
                                    (not= (:x point) (get-in input [:end-point :x])) "horizontal")))]
          (recache ((-builder this) {:input new-input :text (:text this) :direction new-direction}) true)))

      IItemLine
      (start-char [this] (:start-char args))
      (end-char [this] (:end-char args))

      IItemCustom
      (kind [this] "rect-line")
      (-builder [this]
        (fn [new-args] (build-rect-line (merge args new-args))))
      (-parse [this]
        (let [result (let [[x1 y1 x2 y2] (g/values (g/shifted-to-0 input))]
                       (dr/buildRectLine #js [x1 y1 x2 y2] (:direction args)))]
          (maybe-add-edge-chars (fn [] result) args)))
      IItemLock
      (lockable? [this] false)
      (connector? [this] true))))

(defn- build-text [args]
  (let [{:keys [input text cache]} args]
    (specify (Item. input text cache :text)
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
      (-parse [this] #js {:index #js {} :points #js []})

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
