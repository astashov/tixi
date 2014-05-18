(ns tixi.drawer
  (:require-macros [schema.macros :as scm]
                   [tixi.utils :refer (b)])
  (:require [clojure.string :as string]
            [schema.core :as sc]
            [tixi.schemas :as s]
            [tixi.utils :refer [p]]
            [tixi.data :as d]))

;; Bresenham's line algorithm
;; http://en.wikipedia.org/wiki/Bresenham%27s_line_algorithm
(defn- build-line-rest-coords [current x1 y1 x2 y2 err dx dy sx sy sym slash]
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
      (build-line-rest-coords (assoc current [x1 y1] new-sym) new-x1 new-y1 x2 y2 new-err dx dy sx sy sym slash))
    (assoc current [x1 y1] sym)))

(defn- build-line [data]
  (let [[x1 y1 x2 y2] data
        dx (.abs js/Math (- x2 x1))
        dy (.abs js/Math (- y2 y1))
        sx (if (< x1 x2) 1 -1)
        sy (if (< y1 y2) 1 -1)
        err (- dx dy)
        sym (if (< dx dy) "|" "-")
        slash (if (= sx sy) "\\" "/")]
    (build-line-rest-coords {} x1 y1 x2 y2 err dx dy sx sy sym slash)))

(defn- normalize [data]
  (let [[x1 y1 x2 y2] data
        min-x (.min js/Math x1 x2)
        min-y (.min js/Math y1 y2)
        max-x (.max js/Math x1 x2)
        max-y (.max js/Math y1 y2)
        new-x1 (- x1 min-x)
        new-x2 (- x2 min-x)
        new-y1 (- y1 min-y)
        new-y2 (- y2 min-y)
        width (inc (- max-x min-x))
        height (inc (- max-y min-y))]
    {:origin [min-x min-y]
     :dimensions [width height]
     :coordinates [new-x1 new-y1 new-x2 new-y2]}))

(defn- parse-line [data]
  (let [{:keys [origin dimensions coordinates]} (normalize data)]
    {:origin origin
     :dimensions dimensions
     :points (build-line coordinates)}))

(defn- concat-lines [line-coords]
  (let [all-coords (apply concat (map build-line line-coords))
        repeated-coords (keys (filter (fn ([[_ count]] (> count 1))) (frequencies (map first all-coords))))]
    (reduce
      (fn [new-coords coords] (assoc new-coords coords "+"))
      (into {} all-coords)
      repeated-coords)))

(defn- concat-and-normalize-lines [data f]
  (let [{:keys [origin dimensions coordinates]} (normalize data)]
    {:origin origin
     :dimensions dimensions
     :points (concat-lines (f coordinates))}))

(defn- parse-rect [data]
  (let [[x1 y1 x2 y2] data]
    (if (or (= x1 x2) (= y1 y2))
      (parse-line data)
      (concat-and-normalize-lines data (fn [[nx1 ny1 nx2 ny2]]
                                         [[nx1 ny1 nx2 ny1]
                                          [nx1 ny1 nx1 ny2]
                                          [nx2 ny1 nx2 ny2]
                                          [nx1 ny2 nx2 ny2]])))))

(defn- parse-rect-line [data]
  (let [[x1 y1 x2 y2] data]
    (if (or (= x1 x2) (= y1 y2))
      (parse-line [x1 y1 x2 y2])
      (concat-and-normalize-lines data (fn [[nx1 ny1 nx2 ny2]]
                                         [[nx1 ny1 nx1 ny2]
                                          [nx1 ny2 nx2 ny2]])))))

(scm/defn ^:always-validate parse
  "Parses the data structure, and returns the string to display"
  [data :- s/Item]
  (case (:type data)
    :line (parse-line (:input data))
    :rect (parse-rect (:input data))
    :rect-line (parse-rect-line (:input data))
    nil))

(defn- repeat-string [string times]
  (apply str (repeat times string)))

(defn- sort-data [data]
  (sort-by (comp vec reverse first) data))

(defn- generate-text [dimensions points]
  (let [[width height] dimensions]
    (string/join "\n"
      (map string/join
        (partition
          width
          (str
            (reduce
              (fn [string [[x y] sym]]
                (let [position (- (+ (* width y) x) (count string))]
                  (str string (repeat-string " " position) sym)))
              ""
              points)
            (let [[[x y] sym] (last points)
                  last-position (+ (* width y) x)]
              (repeat-string " " (- (* width height) last-position)))))))))

(scm/defn ^:always-validate render [data :- s/Item]
  (let [{:keys [origin dimensions points]} (parse data)
        sorted-points (sort-data points)
        text (generate-text dimensions sorted-points)]
    {:origin origin
     :dimensions dimensions
     :points sorted-points
     :text text}))
