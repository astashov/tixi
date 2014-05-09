(ns tixi.position
  (:require-macros [dommy.macros :refer (node sel1)])
  (:require [dommy.core :as dommy]))

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
  (let [root (sel1 :.canvas)
        x (- (.-clientX event) (.-offsetLeft root))
        y (- (.-clientY event) (.-offsetTop root))]
    (text-coords-from-position x y)))
