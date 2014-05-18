(ns tixi.position
  (:require-macros [dommy.macros :refer (node sel1)]
                   [tixi.utils :refer (b)])
  (:require [dommy.core :as dommy]
            [tixi.data :as d]
            [tixi.utils :refer [p]]))

(defn- calculate-letter-size []
  (let [number-of-x 100]
    (dommy/append! (sel1 :body)
             [:.calculate-letter-size (apply str (repeat number-of-x "X"))])
    (let [calculator (sel1 :.calculate-letter-size)
          width (.-offsetWidth calculator)
          height (.-offsetHeight calculator)
          result {:width (/ width number-of-x) :height height}]
      (dommy/remove! calculator)
      result)))

(defn- letter-size []
  ((memoize calculate-letter-size)))

(defn canvas-size []
  [(.floor js/Math (/ (.-innerWidth js/window) (:width (letter-size))))
   (.floor js/Math (/ (.-innerHeight js/window) (:height (letter-size))))])

(defn text-coords-from-position [x y]
  (let [{:keys [width height]} (letter-size)]
    {:x (.round js/Math (/ x width))
     :y (.floor js/Math (/ y height))}))

(defn position-from-text-coords [x y]
  (let [{:keys [width height]} (letter-size)]
    {:x (* x width)
     :y (* y height)}))

(defn text-coords-from-event [event]
  (when-let [root (sel1 :.canvas)]
    (let [x (- (.-clientX event) (.-offsetLeft root))
          y (- (.-clientY event) (.-offsetTop root))]
      (text-coords-from-position x y))))


(defn- hit-item? [item [x y]]
  (let [cache (:cache item)
        [width height] (:dimensions cache)
        [origin-x origin-y] (:origin cache)]
    (and (>= x origin-x)
         (>= y origin-y)
         (<= x (+ origin-x width))
         (<= y (+ origin-y height)))))

(defn- item-has-point? [item [x y]]
  (let [cache (:cache item)
        points (:points cache)
        [origin-x origin-y] (:origin cache)]
    (some (fn [[[px py] _]]
            (= [x y]
               [(inc (+ px origin-x)) (+ py origin-y)]))
          points)))

(defn items-from-text-coords [coords]
  (->> (d/completed)
       (filter (fn [[id item]] (hit-item? item coords)))
       (filter (fn [[id item]] (item-has-point? item coords)))))

(defn- wrapping-coords [coords]
  (let [[x1 y1 x2 y2] coords]
    [(min x1 x2) (min y1 y2) (max x1 x2) (max y1 y2)]))


(defn wrapping-edges [ids]
  (when (and ids (not-empty ids))
    (let [inputs (map #(:input (d/completed-item %)) ids)
          [x1s y1s x2s y2s] (apply map vector (map #(wrapping-coords %) inputs))]
      [(apply min x1s)
       (apply min y1s)
       (apply max x2s)
       (apply max y2s)])))
