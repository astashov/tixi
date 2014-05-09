(ns tixi.drawer
  (:require [clojure.string :as s]
            [tixi.utils :refer [p b]]
            [tixi.data :as d]))

;; Bresenham's line algorithm
;; http://en.wikipedia.org/wiki/Bresenham%27s_line_algorithm
(defn- parse-line-rest-coords [current x1 y1 x2 y2 err dx dy sx sy sym slash]
  (if (not (and (= x1 x2)
                (= y1 y2)))
    (let [e2 (bit-shift-left err 1)
          new-err (cond (and (> e2 (- dy)) (< e2 dx)) (+ (- err dy) dx)
                        (> e2 (- dy)) (- err dy)
                        (< e2 dx) (+ err dx)
                        :else err)
          new-x1 (cond (> e2 (- dy)) (+ x1 sx)
                       :else x1)
          new-y1 (cond (< e2 dx) (+ y1 sy)
                       :else y1)
          new-sym (if (or (and (= sym "|") (not= new-x1 x1))
                          (and (= sym "-") (not= new-y1 y1)))
                    slash
                    sym)]
      (parse-line-rest-coords (assoc current [x1 y1] new-sym) new-x1 new-y1 x2 y2 new-err dx dy sx sy sym slash))
    (assoc current [x1 y1] sym)))


(defn- parse-line [data]
  (let [[x1 y1 x2 y2] data
        dx (.abs js/Math (- x2 x1))
        dy (.abs js/Math (- y2 y1))
        sx (if (< x1 x2) 1 -1)
        sy (if (< y1 y2) 1 -1)
        err (- dx dy)
        sym (if (< dx dy) "|" "-")
        slash (if (= sx sy) "\\" "/")]
    (parse-line-rest-coords {} x1 y1 x2 y2 err dx dy sx sy sym slash)))

(defn- concat-lines [line-coords]
  (let [all-coords (apply concat (map parse-line line-coords))
        repeated-coords (keys (filter (fn ([[_ count]] (> count 1))) (frequencies (map first all-coords))))]
    (reduce
      (fn [new-coords coords] (assoc new-coords coords "+"))
      (into {} all-coords)
      repeated-coords)))

(defn- parse-rect [data]
  (let [[x1 y1 x2 y2] data]
    (if (or (= x1 x2) (= y1 y2))
      (parse-line [x1 y1 x2 y2])
      (concat-lines [[x1 y1 x2 y1]
                     [x1 y1 x1 y2]
                     [x2 y1 x2 y2]
                     [x1 y2 x2 y2]]))))

(defn- parse-rect-line [data]
  (let [[x1 y1 x2 y2] data]
    (if (or (= x1 x2) (= y1 y2))
      (parse-line [x1 y1 x2 y2])
      (concat-lines [[x1 y1 x1 y2]
                     [x1 y2 x2 y2]]))))

(defn parse
  "Parses the data structure, and returns the string to display"
  [data]
  (case (:type data)
    "line" (parse-line (:content data))
    "rect" (parse-rect (:content data))
    "rect-line" (parse-rect-line (:content data))
    nil))

(defn- repeat-string [string times]
  (apply str (repeat times string)))

(defn- prepare-to-render [data width height]
  (sort-by (comp vec reverse first)
           (filter (fn [[[x y] _]] (and (>= x 0) (< x width) (>= y 0) (< y height)))
                   data)))

(defn render [data width height]
  (b #(s/join "\n"
    (map s/join
      (partition
        width
        (let [points (prepare-to-render (parse data) width height)]
          (str
            (reduce 
              (fn [string [[x y] sym]]
                (let [position (- (+ (* width y) x) (count string))]
                  (str string (repeat-string " " position) sym)))
              ""
              points)
            (let [[[x y] sym] (last points)
                  last-position (+ (* width y) x)]
              (repeat-string " " (- (* width height) last-position))))))))))
