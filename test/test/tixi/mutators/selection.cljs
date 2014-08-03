(ns test.tixi.mutators.selection
  (:require-macros [cemerick.cljs.test :refer (is deftest use-fixtures)]
                   [tixi.utils :refer (b)])
  (:require [cemerick.cljs.test :as test]
            [tixi.geometry :as g]
            [tixi.mutators :as m]
            [tixi.mutators.selection :as ms]
            [tixi.mutators.render :as mr]
            [test.tixi.utils :refer [create-layer! create-sample-layer! create-locked-layers! item-with-input]]
            [tixi.utils :refer [p]]
            [tixi.data :as d]))

(defn- setup [f]
  (m/reset-data!)
  (f))

(use-fixtures :each setup)

(deftest start-selection!
  (ms/start-selection! (g/build-point 2 3))
  (is (= (d/current-selection) (g/build-rect (g/build-point 2 3) (g/build-point 2 3)))))

(deftest update-selection!
  (ms/start-selection! (g/build-point 2 3))
  (ms/update-selection! (g/build-point 5 6))
  (is (= (d/current-selection) (g/build-rect (g/build-point 2 3) (g/build-point 5 6)))))

(deftest finish-selection!
  (let [id (create-sample-layer!)]
    (ms/start-selection! (g/build-point 1 2))
    (ms/update-selection! (g/build-point 5 6))
    (ms/finish-selection!)
    (is (= (d/selected-ids) [id]))
    (is (= (d/selection-rect) (g/build-rect (g/build-point 2 3) (g/build-point 4 5))))
    (is (= (d/current-selection) nil))
    (is (= (d/selected-rel-rect id) (g/build-rect (g/build-point 0 0) (g/build-point 1 1))))))

(deftest select-layer!
  (let [id (create-sample-layer!)]
    (ms/select-layer! id (g/build-point 3 4))
    (is (= (d/selected-ids) #{id}))
    (is (= (d/selection-rect) (g/build-rect (g/build-point 2 3) (g/build-point 4 5))))
    (is (= (d/current-selection) nil))
    (is (= (d/selected-rel-rect id) (g/build-rect (g/build-point 0 0) (g/build-point 1 1))))))

(deftest highlight-layer!
  (create-layer! (g/build-rect (g/build-point 2 3) (g/build-point 4 5)))
  (let [[id _] (first (d/completed))]
    (ms/highlight-layer! id)
    (is (= (d/hover-id) id))
    (ms/highlight-layer! nil)
    (is (= (d/hover-id) nil))))

(deftest move-selection!-one-item
  (let [id (create-sample-layer!)]
    (ms/select-layer! id (g/build-point 3 4))
    (ms/move-selection! (g/build-size 4 5))
    (is (= (d/selection-rect) (g/build-rect (g/build-point 6 8) (g/build-point 8 10))))
    (is (= (:input (d/completed-item id)) (g/build-rect (g/build-point 6 8) (g/build-point 8 10))))))

(deftest move-selection!-two-items
  (let [id1 (create-sample-layer!)
        id2 (create-layer! (g/build-rect (g/build-point 3 4) (g/build-point 5 6)))]
    (ms/select-layer! id1 (g/build-point 2 3))
    (ms/select-layer! id2 (g/build-point 5 6) true)
    (ms/move-selection! (g/build-size 4 5))
    (is (= (d/selection-rect) (g/build-rect (g/build-point 6 8) (g/build-point 9 11))))
    (is (= (:input (d/completed-item id1)) (g/build-rect (g/build-point 6 8) (g/build-point 8 10))))
    (is (= (:input (d/completed-item id2)) (g/build-rect (g/build-point 7 9) (g/build-point 9 11))))))

(deftest move-selection!-one-item
  (let [id (create-sample-layer!)]
    (ms/select-layer! id (g/build-point 3 4))
    (ms/resize-selection! (g/build-size 4 5) :se)
    (is (= (d/selection-rect) (g/build-rect (g/build-point 2 3) (g/build-point 8 10))))))

(deftest add-lock
  (let [[id1 id2 _] (create-locked-layers!)]
    (ms/select-layer! id1)
    (ms/move-selection! (g/build-size 2 2))
    (is (= (:input (d/completed-item id2)) (g/build-rect 2 2 7 7)))))

(deftest remove-lock
  (let [[id1 id2 _] (create-locked-layers!)]
    (ms/select-layer! id2)
    (ms/resize-selection! (g/build-size -1 -1) :se)
    (ms/select-layer! id1)
    (ms/move-selection! (g/build-size 2 2))
    (is (= (:input (d/completed-item id2)) (g/build-rect 2 2 4 4)))))
