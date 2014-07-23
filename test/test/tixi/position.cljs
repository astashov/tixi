(ns test.tixi.position
  (:require-macros [cemerick.cljs.test :refer (is deftest use-fixtures)]
                   [tixi.utils :refer (b)])
  (:require [cemerick.cljs.test :as test]
            [tixi.geometry :as g :refer [Rect Point Size]]
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
  (is (= (p/position->coords (g/build-rect (Point. 20 30) (Point. 60 70)))
         (g/build-rect (Point. 5 1) (Point. 15 3)))))

(deftest point-position->coords
  (is (= (p/position->coords (Point. 20 30)) (Point. 5 1))))

(deftest size-position->coords
  (is (= (p/position->coords (Size. 20 30)) (Size. 5 1))))

(deftest rect-coords->position
  (is (= (p/coords->position (g/build-rect (Point. 5 2) (Point. 16 4)))
         (g/build-rect (Point. 20 36) (Point. 62 72)))))

(deftest point-coords->position
  (is (= (p/coords->position (Point. 16 4)) (Point. 62 72))))

(deftest size-coords->position
  (is (= (p/coords->position (Size. 16 4)) (Size. 62 72))))

(deftest event->coords
  (let [event (js-obj "clientX" 40
                      "clientY" 60)]
    (is (= (p/event->coords event) (Point. 10 3)))))

(deftest items-inside-rect
  (let [id1 (create-layer! (g/build-rect (Point. 2 2) (Point. 3 4)))
        id2 (create-layer! (g/build-rect (Point. 2 3) (Point. 3 5)))
        id3 (create-layer! (g/build-rect (Point. 3 5) (Point. 4 6)))]
    (is (= (vec (keys (p/items-inside-rect (g/build-rect (Point. 1 1) (Point. 5 5))))) [id1 id2]))))

(deftest items-at-point
  (let [id1 (create-layer! (g/build-rect (Point. 2 2) (Point. 3 4)))
        id2 (create-layer! (g/build-rect (Point. 2 2) (Point. 3 5)))
        id3 (create-layer! (g/build-rect (Point. 3 5) (Point. 4 6)))]
    (is (= (vec (keys (p/items-at-point (Point. 2 2)))) [id1 id2]))))

(deftest items-at-point-with-text
  (let [id1 (create-layer! (g/build-rect (Point. 2 1) (Point. 14 7)))]
    (mt/set-text-to-item! id1 "blabla\n\nfoo\nbar")
    (is (= (vec (keys (p/items-at-point (Point. 6 3)))) [id1]))))

(deftest items-wrapping-rect
  (let [id1 (create-layer! (g/build-rect (Point. 2 2) (Point. 3 4)))
        id2 (create-layer! (g/build-rect (Point. 2 2) (Point. 3 5)))
        id3 (create-layer! (g/build-rect (Point. 3 5) (Point. 4 6)))]
    (is (= (p/items-wrapping-rect [id1 id2 id3]) (g/build-rect (Point. 2 2) (Point. 4 6))))))

(deftest item-id-at-point
  (let [id1 (create-layer! (g/build-rect (Point. 2 2) (Point. 3 4)))
        id2 (create-layer! (g/build-rect (Point. 2 2) (Point. 3 5)))]
    (is (= (p/item-id-at-point (Point. 2 2)) id2))))

(deftest items-with-outlet-at-point
  (m/set-tool! :rect)
  (let [id1 (create-layer! (g/build-rect 2 2 6 6))]
    (is (= (p/items-with-outlet-at-point nil (Point. 4 2))
           (list [id1 (d/completed-item id1) (Point. 0.5 0)])))
    (is (= (p/items-with-outlet-at-point nil (Point. 4 3)) '()))))
