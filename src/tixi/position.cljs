(ns tixi.position
  (:require-macros [dommy.macros :refer (node sel1)]
                   [tixi.utils :refer (b)])
  (:require [dommy.core :as dommy]
            [tixi.geometry :as g :refer [Size Rect Point]]
            [tixi.data :as d]
            [tixi.items :as i]
            [tixi.utils :refer [p]]
            [goog.style :as style]))

(defn- calculate-letter-size []
  (let [number-of-x 100]
    (dommy/append! (sel1 :body)
                   [:.calculate-letter-size (apply str (repeat number-of-x "X"))])
    (let [calculator (sel1 :.calculate-letter-size)
          width (.-offsetWidth calculator)
          height (.-offsetHeight calculator)
          result (Size. (.round js/Math (/ width number-of-x)) height)]
      (dommy/remove! calculator)
      result)))

;; TODO: Sometimes it forgets it?
(defn- letter-size []
  ((memoize calculate-letter-size)))

(defn- hit-item? [item point]
  (let [rect (g/build-rect (i/origin item) (i/dimensions item))]
    (g/inside? rect point)))

(defn- item-has-point? [item point]
  (let [index (:index (:cache item))
        moved-point (g/sub point (i/origin item))
        [x y] (g/values moved-point)
        k (str x "_" y)]
    (boolean (aget index k))))

(defn canvas-size []
  (Size. (.floor js/Math (/ (.-innerWidth js/window) (:width (letter-size))))
         (.floor js/Math (/ (.-innerHeight js/window) (:height (letter-size))))))

(defprotocol IConvert
  (position->coords [this])
  (coords->position [this]))

(extend-type Rect
  IConvert
  (position->coords [this]
    (let [{:keys [width height]} (letter-size)
          [x1 y1 x2 y2] (g/values this)]
      (Rect. (Point. (.floor js/Math (/ x1 width))
                     (.floor js/Math (/ y1 height)))
             (Point. (.floor js/Math (/ x2 width))
                     (.floor js/Math (/ y2 height))))))
  (coords->position [this]
    (let [{:keys [width height]} (letter-size)
          [x1 y1 x2 y2] (g/values this)]
      (Rect. (Point. (.floor js/Math (* x1 width))
                     (.floor js/Math (* y1 height)))
             (Point. (.floor js/Math (* x2 width))
                     (.floor js/Math (* y2 height)))))))

(extend-type Point
  IConvert
  (position->coords [this]
    (let [{:keys [width height]} (letter-size)
          [x y] (g/values this)]
      (Point. (.floor js/Math (/ x width))
              (.floor js/Math (/ y height)))))
  (coords->position [this]
    (let [{:keys [width height]} (letter-size)
         [x y] (g/values this)]
      (Point. (.floor js/Math (* x width))
              (.floor js/Math (* y height))))))

(extend-type Size
  IConvert
  (position->coords [this]
    (let [{:keys [width height]} (letter-size)
          [x y] (g/values this)]
      (Size. (.floor js/Math (/ x width))
             (.floor js/Math (/ y height)))))
  (coords->position [this]
    (let [{:keys [width height]} (letter-size)
         [x y] (g/values this)]
      (Size. (.floor js/Math (* x width))
             (.floor js/Math (* y height))))))

(defn event->coords [event]
  (let [root (sel1 :.project)
        offset (if root (style/getPageOffset root) (js-obj "x" 0 "y" 0))
        x (- (.-clientX event) (.-x offset))
        y (- (.-clientY event) (.-y offset))]
    (position->coords (Point. x y))))

(defn items-inside-rect [rect]
  (filter (fn [[_ item]]
            (let [input-rect (g/normalize (:input item))
                  sel-rect (g/normalize rect)]
              (g/inside? sel-rect input-rect)))
          (d/completed)))

(defn- item-at-client-point [client-point]
  (when client-point
    (let [[x y] (g/values client-point)
          element-at-client-point (.elementFromPoint js/document x y)
          [_, id-str] (re-find #"^text-content-(\d+)" (.-id element-at-client-point))]
      (when id-str
        (let [id (.parseInt js/window id-str 10)]
          [id (d/completed-item id)])))))

(defn items-at-point
  ([point] (items-at-point point nil))
  ([point client-point]
    (if-let [result (item-at-client-point client-point)]
      [result]
      (->> (d/completed)
        (filter (fn [[_ item]] (hit-item? item point)))
        (filter (fn [[_ item]] (item-has-point? item point)))))))

(defn item-id-at-point
  ([point] (item-id-at-point point nil))
  ([point client-point] (first (first (items-at-point point client-point)))))

(defn items-wrapping-rect [ids]
  (when (and ids (not-empty ids))
    (g/wrapping-rect (map #(:input (d/completed-item %)) ids))))
