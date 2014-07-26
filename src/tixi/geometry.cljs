(ns tixi.geometry
  (:require-macros [tixi.utils :refer [defpoly defpoly-]])
  (:require [tixi.utils :refer [p]]))

(defn build-point [x y]
  {:x x :y y :type :point})

(defn build-size [width height]
  {:width width :height height :type :size})

(defn build-rect
  ([x1 y1 x2 y2]
  (build-rect (build-point x1 y1) (build-point x2 y2)))

  ([start end-or-size]
  (case (:type end-or-size)
    :point
    {:start start :end end-or-size :type :rectangle}

    :size
    (let [end (build-point (+ (:x start) (:width end-or-size))
                           (+ (:y start) (:height end-or-size)))]
      (build-rect start end)))))

(defpoly add [this other]
  :size
  (build-size (+ (:width this) (:width other))
         (+ (:height this) (:height other)))

  :point
  (build-point (+ (:x this) (:x other))
          (+ (:y this) (:y other))))

(defpoly sub [this other]
  :size
  (build-size (- (:width this) (:width other))
         (- (:height this) (:height other)))
  :point
  (build-point (- (:x this) (:x other))
          (- (:y this) (:y other))))

(defpoly incr [this]
  :size
  (build-size (+ (:width this) 1) (+ (:height this) 1))

  :point
  (build-point (+ (:x this) 1) (+ (:y this) 1)))

(defpoly decr [this]
  :size
  (build-size (- (:width this) 1) (- (:height this) 1))

  :point
  (build-point (- (:x this) 1) (- (:y this) 1)))

(defpoly values [this]
  :rectangle
  (into [] (flatten [(values (:start this)) (values (:end this))]))

  :size
  [(:width this) (:height this)]

  :point
  [(:x this) (:y this)])


(defn aspect-ratio [this]
  (/ (:width this) (:height this)))

(defn portrait? [this]
  (< (aspect-ratio this) 1))

(defn landscape? [this]
  (> (aspect-ratio this) 1))

(defn square? [this]
  (= (aspect-ratio this) 1))

(defn expand [this point]
  (build-rect (:start this) point))

(defn shrink [this point]
  (build-rect point (:end this)))

(defn width [this]
  (.abs js/Math (- (:x (:start this)) (:x (:end this)))))

(defn height [this]
  (.abs js/Math (- (:y (:start this)) (:y (:end this)))))

(defn dimensions [this]
  (build-size (width this) (height this)))


(defn normalize [this]
  (let [min-x (min (:x (:start this)) (:x (:end this)))
        min-y (min (:y (:start this)) (:y (:end this)))
        max-x (max (:x (:start this)) (:x (:end this)))
        max-y (max (:y (:start this)) (:y (:end this)))]
    (build-rect min-x min-y max-x max-y)))

(defn origin [this]
  (:start (normalize this)))

(defn shifted-to-0 [this]
  (let [org (origin this)]
    (build-rect (- (:x (:start this)) (:x org))
                (- (:y (:start this)) (:y org))
                (- (:x (:end this)) (:x org))
                (- (:y (:end this)) (:y org)))))

(defn line? [this]
  (or (= (:x (:start this)) (:x (:end this))
         (:y (:start this)) (:y (:end this)))))

(defn inside? [this point-or-rect]
  (case (:type point-or-rect)
    :point
    (let [r (normalize this)]
      (and (>= (:x point-or-rect) (:x (:start r)))
           (<= (:x point-or-rect) (:x (:end r)))
           (>= (:y point-or-rect) (:y (:start r)))
           (<= (:y point-or-rect) (:y (:end r)))))

    :rectangle
    (let [wrap (normalize this)
          rect (normalize point-or-rect)]
      (and (>= (:x (:start rect)) (:x (:start wrap)))
           (<= (:x (:end rect)) (:x (:end wrap)))
           (>= (:y (:start rect)) (:y (:start wrap)))
           (<= (:y (:end rect)) (:y (:end wrap)))))))

(defn resize [this diff type]
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
    (build-rect new-x1 new-y1 new-x2 new-y2)))

(defn flipped-by-x? [this]
  (> (:x (:start this)) (:x (:end this))))

(defn flipped-by-y? [this]
  (> (:y (:start this)) (:y (:end this))))

(defpoly relative [this wrapper]
  :rectangle
  (let [[x1 y1 x2 y2] (values this)
        [wrx1 wry1 wrx2 wry2] (values (normalize wrapper))
        relx1 (if (= (width wrapper) 0) 0 (/ (if (flipped-by-x? wrapper) (- x2 wrx1) (- x1 wrx1)) (width wrapper)))
        rely1 (if (= (height wrapper) 0) 0 (/ (if (flipped-by-y? wrapper) (- y2 wry1) (- y1 wry1)) (height wrapper)))
        relx2 (if (= (width wrapper) 0) 1 (/ (if (flipped-by-x? wrapper) (- x1 wrx1) (- x2 wrx1)) (width wrapper)))
        rely2 (if (= (height wrapper) 0) 1 (/ (if (flipped-by-y? wrapper) (- y1 wry1) (- y2 wry1)) (height wrapper)))]
    (build-rect relx1 rely1 relx2 rely2))

  :point
  (let [[x y] (values this)
        [wrx1 wry1 wrx2 wry2] (values (normalize wrapper))
        relx (if (= (width wrapper) 0) 0 (/ (if (flipped-by-x? wrapper) (- wrx2 x) (- x wrx1)) (width wrapper)))
        rely (if (= (height wrapper) 0) 0 (/ (if (flipped-by-y? wrapper) (- wry2 y) (- y wry1)) (height wrapper)))]
    (build-point relx rely)))

(defpoly absolute [this wrapper]
  :rectangle
  (let [[rel-x1 rel-y1 rel-x2 rel-y2] (values wrapper)
        [x1 y1 x2 y2] (values (normalize this))
        fx (fn [v] (if (flipped-by-x? this) (- x2 v) (+ x1 v)))
        fy (fn [v] (if (flipped-by-y? this) (- y2 v) (+ y1 v)))
        new-x1 (.round js/Math (fx (* (width this) rel-x1)))
        new-y1 (.round js/Math (fy (* (height this) rel-y1)))
        new-x2 (.round js/Math (fx (* (width this) rel-x2)))
        new-y2 (.round js/Math (fy (* (height this) rel-y2)))]
    (build-rect new-x1 new-y1 new-x2 new-y2))

  :point
  (let [[wrx1 wry1 wrx2 wry2] (values wrapper)
        [x y] (values this)
        abs-x (* (width wrapper) x)
        abs-y (* (height wrapper) y)
        new-x (.round js/Math (if (flipped-by-x? wrapper) (- wrx1 abs-x) (+ wrx1 abs-x)))
        new-y (.round js/Math (if (flipped-by-y? wrapper) (- wry1 abs-y) (+ wry1 abs-y)))]
    (build-point new-x new-y)))

(defpoly move [this diff]
  :rectangle
  (let [[x1 y1 x2 y2] (values this)
        [dx dy] (values diff)]
    (build-rect (+ x1 dx) (+ y1 dy) (+ x2 dx) (+ y2 dy)))

  :point
  (let [[x y] (values this)
        [dx dy] (values diff)]
    (build-point (+ x dx) (+ y dy))))

(defn center [this]
  (move (build-point (/ (width this) 2)
                     (/ (height this) 2))
        (origin this)))

(defn wrapping-rect [rects]
  (when (seqable? rects)
    (let [[x1s y1s x2s y2s] (apply map vector (map #(values (normalize %)) rects))]
      (build-rect (build-point (apply min x1s) (apply min y1s))
             (build-point (apply max x2s) (apply max y2s))))))
