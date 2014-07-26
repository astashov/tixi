(ns test.tixi.position
  (:require-macros [cemerick.cljs.test :refer (is deftest use-fixtures)]
                   [tixi.utils :refer (b)])
  (:require [cemerick.cljs.test :as test]
            [tixi.geometry :as g]
            [tixi.position :as p]
            [tixi.mutators :as m]
            [tixi.mutators.text :as mt]
            [test.tixi.utils :refer [create-layer!]]
            [tixi.utils :refer [p]]
            [tixi.data :as d]))

(defn- setup [f]
  (m/reset-data!)
  (f))

(use-fixtures :each setup)

(deftest rect-position->coords
  (is (= (p/position->coords (g/build-rect (g/build-point 20 30) (g/build-point 60 70)))
         (g/build-rect (g/build-point 5 1) (g/build-point 15 3)))))

(deftest point-position->coords
  (is (= (p/position->coords (g/build-point 20 30)) (g/build-point 5 1))))

(deftest size-position->coords
  (is (= (p/position->coords (g/build-size 20 30)) (g/build-size 5 1))))

(deftest rect-coords->position
  (is (= (p/coords->position (g/build-rect (g/build-point 5 2) (g/build-point 16 4)))
         (g/build-rect (g/build-point (.ceil js/Math (* 5 (:width (p/letter-size))))
                               (.ceil js/Math (* 2 (:height (p/letter-size)))))
                       (g/build-point (.ceil js/Math (* 16 (:width (p/letter-size))))
                               (.ceil js/Math (* 4 (:height (p/letter-size)))))))))

(deftest point-coords->position
  (is (= (p/coords->position (g/build-point 16 4))
         (g/build-point (.ceil js/Math (* 16 (:width (p/letter-size))))
                 (.ceil js/Math (* 4 (:height (p/letter-size))))))))

(deftest size-coords->position
  (is (= (p/coords->position (g/build-size 16 4))
         (g/build-size (.ceil js/Math (* 16 (:width (p/letter-size))))
                (.ceil js/Math (* 4 (:height (p/letter-size))))))))

(deftest event->coords
  (let [event (js-obj "clientX" 40
                      "clientY" 60)]
    (is (= (p/event->coords event)
           (g/build-point (.floor js/Math (/ 40 (:width (p/letter-size))))
                   (.floor js/Math (/ 60 (:height (p/letter-size)))))))))

(deftest items-inside-rect
  (let [id1 (create-layer! (g/build-rect (g/build-point 2 2) (g/build-point 3 4)))
        id2 (create-layer! (g/build-rect (g/build-point 2 3) (g/build-point 3 5)))
        id3 (create-layer! (g/build-rect (g/build-point 3 5) (g/build-point 4 6)))]
    (is (= (vec (keys (p/items-inside-rect (g/build-rect (g/build-point 1 1) (g/build-point 5 5))))) [id1 id2]))))

(deftest items-at-point
  (let [id1 (create-layer! (g/build-rect (g/build-point 2 2) (g/build-point 3 4)))
        id2 (create-layer! (g/build-rect (g/build-point 2 2) (g/build-point 3 5)))
        id3 (create-layer! (g/build-rect (g/build-point 3 5) (g/build-point 4 6)))]
    (is (= (vec (keys (p/items-at-point (g/build-point 2 2)))) [id1 id2]))))

(deftest items-at-point-with-text
  (let [id1 (create-layer! (g/build-rect (g/build-point 2 1) (g/build-point 14 7)))]
    (mt/set-text-to-item! id1 "blabla\n\nfoo\nbar")
    (is (= (vec (keys (p/items-at-point (g/build-point 6 3)))) [id1]))))

(deftest items-wrapping-rect
  (let [id1 (create-layer! (g/build-rect (g/build-point 2 2) (g/build-point 3 4)))
        id2 (create-layer! (g/build-rect (g/build-point 2 2) (g/build-point 3 5)))
        id3 (create-layer! (g/build-rect (g/build-point 3 5) (g/build-point 4 6)))]
    (is (= (p/items-wrapping-rect [id1 id2 id3]) (g/build-rect (g/build-point 2 2) (g/build-point 4 6))))))

(deftest item-id-at-point
  (let [id1 (create-layer! (g/build-rect (g/build-point 2 2) (g/build-point 3 4)))
        id2 (create-layer! (g/build-rect (g/build-point 2 2) (g/build-point 3 5)))]
    (is (= (p/item-id-at-point (g/build-point 2 2)) id2))))

(deftest items-with-outlet-at-point
  (m/set-tool! :rect)
  (let [id1 (create-layer! (g/build-rect 2 2 6 6))]
    (is (= (p/items-with-outlet-at-point nil (g/build-point 4 2))
           (list [id1 (d/completed-item id1) (g/build-point 0.5 0)])))
    (is (= (p/items-with-outlet-at-point nil (g/build-point 4 3)) '()))))
