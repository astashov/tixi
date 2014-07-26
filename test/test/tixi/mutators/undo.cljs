(ns test.tixi.mutators.undo
  (:require-macros [cemerick.cljs.test :refer (is deftest use-fixtures)]
                   [tixi.utils :refer (b)])
  (:require [cemerick.cljs.test :as test]
            [tixi.geometry :as g]
            [tixi.mutators :as m]
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
    (mu/snapshot!)
    (let [id2 (create-layer! (g/build-rect (g/build-point 9 10) (g/build-point 11 12)))]
      (is (= (keys (d/completed)) '(0 1)))
      (mu/undo!)
      (is (= (keys (d/completed)) '(0)))
      (mu/undo!)
      (is (= (keys (d/completed)) '(0))))))

(deftest redo!
  (let [id1 (create-layer! (g/build-rect (g/build-point 5 6) (g/build-point 7 8)))]
    (mu/snapshot!)
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
    (mu/snapshot!)
    (let [id2 (create-layer! (g/build-rect (g/build-point 9 10) (g/build-point 11 12)))]
      (is (= (keys (d/completed)) '(0 1)))
      (mu/undo-if-unchanged!)
      (is (= (keys (d/completed)) '(0 1)))
      (mu/snapshot!)
      (mu/undo-if-unchanged!)
      (mu/undo!)
      (is (= (keys (d/completed)) '(0))))))
