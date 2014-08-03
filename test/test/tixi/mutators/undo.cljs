(ns test.tixi.mutators.undo
  (:require-macros [cemerick.cljs.test :refer (is deftest use-fixtures)]
                   [tixi.utils :refer (b)])
  (:require [cemerick.cljs.test :as test]
            [tixi.geometry :as g]
            [tixi.mutators :as m]
            [tixi.mutators.shared :as msh]
            [tixi.mutators.selection :as ms]
            [tixi.mutators.undo :as mu]
            [test.tixi.utils :refer [create-layer! create-sample-layer!]]
            [tixi.utils :refer [p]]
            [tixi.data :as d]))

(defn- setup [f]
  (m/reset-data!)
  (f))

(use-fixtures :each setup)

(deftest undo!
  (let [id1 (create-layer! (g/build-rect (g/build-point 5 6) (g/build-point 7 8)))]
    (msh/snapshot!)
    (let [id2 (create-layer! (g/build-rect (g/build-point 9 10) (g/build-point 11 12)))]
      (is (= (keys (d/completed)) '(0 1)))
      (mu/undo!)
      (is (= (keys (d/completed)) '(0)))
      (mu/undo!)
      (is (= (keys (d/completed)) '(0))))))

(deftest redo!
  (let [id1 (create-layer! (g/build-rect (g/build-point 5 6) (g/build-point 7 8)))]
    (msh/snapshot!)
    (let [id2 (create-layer! (g/build-rect (g/build-point 9 10) (g/build-point 11 12)))]
      (is (= (keys (d/completed)) '(0 1)))
      (mu/undo!)
      (is (= (keys (d/completed)) '(0)))
      (mu/redo!)
      (is (= (keys (d/completed)) '(0 1)))
      (mu/redo!)
      (is (= (keys (d/completed)) '(0 1))))))

(deftest undo-if-unchanged!
  (let [id1 (create-layer! (g/build-rect (g/build-point 5 6) (g/build-point 7 8)))]
    (msh/snapshot!)
    (let [id2 (create-layer! (g/build-rect (g/build-point 9 10) (g/build-point 11 12)))]
      (is (= (keys (d/completed)) '(0 1)))
      (mu/undo-if-unchanged!)
      (is (= (keys (d/completed)) '(0 1)))
      (msh/snapshot!)
      (mu/undo-if-unchanged!)
      (mu/undo!)
      (is (= (keys (d/completed)) '(0))))))

(deftest refresh-selection-after-undo
  (let [id1 (create-layer! (g/build-rect (g/build-point 5 6) (g/build-point 7 8)))]
    (msh/snapshot!)
    (ms/select-layer! id1)
    (ms/move-selection! (g/build-point 2 2))
    (is (= (d/selection-rect) (g/build-rect 7 8 9 10)))
    (mu/undo!)
    (is (= (d/selection-rect) (g/build-rect 5 6 7 8)))))

(deftest clear-selection-after-undo-if-item-removed
  (msh/snapshot!)
  (let [id1 (create-layer! (g/build-rect (g/build-point 5 6) (g/build-point 7 8)))]
    (ms/select-layer! id1)
    (is (= (d/selected-ids) #{id1}))
    (mu/undo!)
    (is (= (d/selected-ids) #{}))))
