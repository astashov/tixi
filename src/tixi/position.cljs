(ns tixi.position
  (:require-macros [dommy.macros :refer (node sel1)]
                   [tixi.utils :refer (b defpoly)])
  (:require [dommy.core :as dommy]
            [tixi.geometry :as g]
            [tixi.data :as d]
            [tixi.items :as i]
            [clojure.string :as string]
            [goog.style :as style]
            [tixi.utils :refer [p]]))

(defn- calculate-letter-size []
  (let [number-of-x 100]
    (when (not (sel1 :.calculate-letter-size))
      (dommy/append! (sel1 :body) [:.calculate-letter-size])
      (dommy/set-html! (sel1 :.calculate-letter-size) (str (apply str (repeat number-of-x "X"))
                                                           (apply str (repeat (dec number-of-x) "<br />X")))))
    (let [calculator (sel1 :.calculate-letter-size)
          width (.-offsetWidth calculator)
          height (.-offsetHeight calculator)
          result (g/build-size (/ width number-of-x) (/  height number-of-x))]
      (dommy/remove! calculator)
      result)))

(defn set-letter-size! []
  (swap! d/data assoc :letter-size (calculate-letter-size)))

(defn letter-size []
  (when-not (d/letter-size)
    (set-letter-size!))
  (d/letter-size))

(defn- hit-item? [item point]
  (let [rect (g/build-rect (i/origin item) (i/dimensions item))]
    (g/inside? rect point)))

(defn- item-has-point? [id item point]
  (let [index (:index (d/item-cache id))
        moved-point (g/sub point (i/origin item))
        [x y] (g/values moved-point)
        k (str x "_" y)]
    (boolean (aget index k))))

(defn- item-text-has-point? [id item point]
  (let [text (:text item)
        center (g/center (:input item))
        lines (clojure.string/split text "\n")
        max-width (apply max (map count lines))
        center-char-num (/ max-width 2)
        center-line-num (/ (count lines) 2)
        x (.floor js/Math (+ center-char-num (- (:x point) (:x center))))
        y (.floor js/Math (+ center-line-num (- (:y point) (:y center))))]
    (and (>= y 0)
         (< y (count lines))
         (nth (nth lines y) x))))

(defn width->position [width]
  (.ceil js/Math (* width (:width (letter-size)))))

(defn position->width [pos]
  (.floor js/Math (/ pos (:width (letter-size)))))

(defn height->position [height]
  (.ceil js/Math (* height (:height (letter-size)))))

(defn position->height [pos]
  (.floor js/Math (/ pos (:height (letter-size)))))

(defn canvas-size []
  (g/build-size (position->width (.-innerWidth js/window))
         (position->height (.-innerHeight js/window))))

(defpoly position->coords [this]
  :rectangle
  (let [{:keys [width height]} (letter-size)
        [x1 y1 x2 y2] (g/values this)]
    (g/build-rect (position->width x1) (position->height y1)
                  (position->width x2) (position->height y2)))
  :point
  (let [{:keys [width height]} (letter-size)
        [x y] (g/values this)]
    (g/build-point (position->width x) (position->height y)))

  :size
  (let [{:keys [width height]} (letter-size)
        [x y] (g/values this)]
    (g/build-size (position->width x) (position->height y))))

(defpoly coords->position [this]
  :rectangle
  (let [{:keys [width height]} (letter-size)
        [x1 y1 x2 y2] (g/values this)]
    (g/build-rect (width->position x1) (height->position y1)
                  (width->position x2) (height->position y2)))
  :point
  (let [{:keys [width height]} (letter-size)
        [x y] (g/values this)]
    (g/build-point (width->position x) (height->position y)))

  :size
  (let [{:keys [width height]} (letter-size)
        [x y] (g/values this)]
    (g/build-size (width->position x) (height->position y))))

(defn event->coords [event]
  (let [root (sel1 :.project)
        offset (if root (style/getPageOffset root) (js-obj "x" 0 "y" 0))
        x (- (.-clientX event) (.-x offset))
        y (- (.-clientY event) (.-y offset))]
    (position->coords (g/build-point x y))))

(defn items-inside-rect [rect]
  (filter (fn [[_ item]]
            (let [input-rect (g/normalize (:input item))
                  sel-rect (g/normalize rect)]
              (g/inside? sel-rect input-rect)))
          (d/completed)))

(defn items-at-point [point]
  (->> (d/completed)
       (filter (fn [[_ item]] (hit-item? item point)))
       (filter (fn [[id item]]
                 (or (item-has-point? id item point)
                     (item-text-has-point? id item point))))))

(defn items-with-outlet-at-point [connector-id point]
  (->> (items-at-point point)
       (keep (fn [[id item]]
               (when (not= id connector-id)
                 (when-let [used-outlet (first (filter (fn [rel-outlet]
                                                         (= point (g/absolute rel-outlet (:input item))))
                                                       (i/outlets item)))]
                   [id item used-outlet]))))))

(defn item-id-at-point [point]
  (first (last (items-at-point point))))

(defn items-wrapping-rect [ids]
  (when (and ids (not-empty ids))
    (if (and (= (count ids) 1)
             (contains? #{:line :rect-line} (:type (d/completed-item (first ids)))))
      (:input (d/completed-item (first ids)))
      (g/wrapping-rect (map #(:input (d/completed-item %)) ids)))))
