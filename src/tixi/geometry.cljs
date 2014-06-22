(ns tixi.geometry
  (:require [tixi.utils :refer [p]]))

(defprotocol IRect
  (expand [this point])
  (shrink [this point])
  (width [this])
  (height [this])
  (dimensions [this])
  (normalize [this])
  (origin [this])
  (shifted-to-0 [this])
  (inside? [this point])
  (line? [this])
  (move [this diff])
  (resize [this diff type])
  (flipped-by-x? [this])
  (flipped-by-y? [this]))

(defprotocol IMath
  (add [this another])
  (sub [this another])
  (incr [this])
  (decr [this]))

(defprotocol IValues
  (values [this]))

(defprotocol IRelative
  (relative [this wrapper])
  (absolute [this wrapper]))

(defrecord Point [x y]
  Object
  (toString [_] (str [x y]))

  IMath
  (add [this point]
    (Point. (+ x (:x point))
            (+ y (:y point))))
  (sub [this point]
    (Point. (- x (:x point))
            (- y (:y point))))
  (incr [this]
    (Point. (+ x 1) (+ y 1)))
  (decr [this]
    (Point. (- x 1) (- y 1)))

  IValues
  (values [this]
    [x y])

  IRelative
  (relative [this wrapper]
    (let [[x y] (values this)
          [wrx1 wry1 wrx2 wry2] (values (normalize wrapper))
          relx (/ (- x wrx1) (width wrapper))
          rely (/ (- y wry1) (height wrapper))]
      (Point. relx rely)))

  (absolute [this wrapper]
    (let [[wrx wry] (values (:start-point wrapper))
          [x y] (values this)
          new-x (.round js/Math (+ wrx (* (width wrapper) x)))
          new-y (.round js/Math (+ wry (* (height wrapper) y)))]
      (Point. new-x new-y))))



(defrecord Size [width height]
  Object
  (toString [_] (str [width height]))

  IValues
  (values [this] [width height])

  IMath
  (add [this size]
    (Size. (+ width (:width size))
           (+ height (:height size))))
  (sub [this size]
    (Size. (- width (:width size))
           (- height (:height size))))
  (incr [this]
    (Size. (+ width 1) (+ height 1)))
  (decr [this]
    (Size. (- width 1) (- height 1))))

(declare build-rect)
(defrecord Rect [start-point end-point]
  Object
  (toString [_] (str (values start-point) (values end-point)))

  IRect
  (expand [this point]
    (build-rect start-point point))

  (shrink [this point]
    (build-rect point end-point))

  (width [this]
    (.abs js/Math (- (:x start-point) (:x end-point))))

  (height [this]
    (.abs js/Math (- (:y start-point) (:y end-point))))

  (dimensions [this]
    (Size. (width this) (height this)))

  (normalize [this]
    (let [min-x (min (:x start-point) (:x end-point))
          min-y (min (:y start-point) (:y end-point))
          max-x (max (:x start-point) (:x end-point))
          max-y (max (:y start-point) (:y end-point))]
      (build-rect (Point. min-x min-y) (Point. max-x max-y))))

  (origin [this]
    (:start-point (normalize this)))

  (shifted-to-0 [this]
    (let [org (origin this)]
      (build-rect (Point. (- (:x start-point) (:x org))
                     (- (:y start-point) (:y org)))
             (Point. (- (:x end-point) (:x org))
                     (- (:y end-point) (:y org))))))

  (line? [this]
    (or (= (:x start-point) (:x end-point)
           (:y start-point) (:y end-point))))

  (inside? [this point-or-rect]
    (cond
      (instance? Point point-or-rect)
      (let [r (normalize this)]
        (and (>= (:x point-or-rect) (:x (:start-point r)))
             (<= (:x point-or-rect) (:x (:end-point r)))
             (>= (:y point-or-rect) (:y (:start-point r)))
             (<= (:y point-or-rect) (:y (:end-point r)))))

      (instance? Rect point-or-rect)
        (let [wrap (normalize this)
              rect (normalize point-or-rect)]
          (and (>= (:x (:start-point rect)) (:x (:start-point wrap)))
               (<= (:x (:end-point rect)) (:x (:end-point wrap)))
               (>= (:y (:start-point rect)) (:y (:start-point wrap)))
               (<= (:y (:end-point rect)) (:y (:end-point wrap)))))))

  (move [this diff]
    (let [[x1 y1 x2 y2] (values this)
          [dx dy] (values diff)]
      (build-rect (Point. (+ x1 dx) (+ y1 dy))
                  (Point. (+ x2 dx) (+ y2 dy)))))

  (resize [this diff type]
    (let [[x1 y1 x2 y2] (values this)
          [dx dy] (values diff)
          [new-x1 new-y1 new-x2 new-y2]
            (case type
              :nw [(+ x1 dx) (+ y1 dy) x2                     y2                    ]
              :n  [x1                     (+ y1 dy) x2                     y2                    ]
              :ne [x1                     (+ y1 dy) (+ x2 dx) y2                    ]
              :w  [(+ x1 dx) y1                     x2                     y2                    ]
              :e  [x1                     y1                     (+ x2 dx) y2                    ]
              :sw [(+ x1 dx) y1                     x2                     (+ y2 dy)]
              :s  [x1                     y1                     x2                     (+ y2 dy)]
              :se [x1                     y1                     (+ x2 dx) (+ y2 dy)])]
      (build-rect (Point. new-x1 new-y1)
                  (Point. new-x2 new-y2))))

  (flipped-by-x? [this]
    (> (:x start-point) (:x end-point)))

  (flipped-by-y? [this]
    (> (:y start-point) (:y end-point)))

  IRelative
  (relative [this wrapper]
    (let [[x1 y1 x2 y2] (values this)
          [wrx1 wry1 wrx2 wry2] (values (normalize wrapper))
          relx1 (/ (- x1 wrx1) (width wrapper))
          rely1 (/ (- y1 wry1) (height wrapper))
          relx2 (/ (- x2 wrx1) (width wrapper))
          rely2 (/ (- y2 wry1) (height wrapper))]
      (Rect. (Point. relx1 rely1) (Point. relx2 rely2))))

  (absolute [this rel-rect]
    (let [[rel-x1 rel-y1 rel-x2 rel-y2] (values rel-rect)
          [x1 y1 x2 y2] (values (normalize this))
          fx (fn [v] (if (flipped-by-x? this) (- x2 v) (+ x1 v)))
          fy (fn [v] (if (flipped-by-y? this) (- y2 v) (+ y1 v)))
          new-x1 (.round js/Math (fx (* (width this) rel-x1)))
          new-y1 (.round js/Math (fy (* (height this) rel-y1)))
          new-x2 (.round js/Math (fx (* (width this) rel-x2)))
          new-y2 (.round js/Math (fy (* (height this) rel-y2)))]
      (build-rect (Point. new-x1 new-y1) (Point. new-x2 new-y2))))

  IValues
  (values [this]
    (vec (flatten [(values (:start-point this)) (values (:end-point this))]))))


(defn build-rect
  ([x1 y1 x2 y2]
  (build-rect (Point. x1 y1) (Point. x2 y2)))

  ([start-point end-point-or-size]
  (cond
    (instance? Point end-point-or-size)
    (Rect. start-point end-point-or-size)

    (instance? Size end-point-or-size)
    (let [end-point (Point. (+ (:x start-point) (:width end-point-or-size))
                            (+ (:y start-point) (:height end-point-or-size)))]
      (Rect. start-point end-point)))))

(defn wrapping-rect [rects]
  (when (seqable? rects)
    (let [[x1s y1s x2s y2s] (apply map vector (map #(values (normalize %)) rects))]
      (Rect. (Point. (apply min x1s) (apply min y1s))
             (Point. (apply max x2s) (apply max y2s))))))

(extend-protocol IPrintWithWriter
  Point
  (-pr-writer [coll writer opts]
    (-write writer (str "P" coll)))

  Size
  (-pr-writer [coll writer opts]
    (-write writer (str "S" coll)))

  Rect
  (-pr-writer [coll writer opts]
    (-write writer (str "R" coll))))
