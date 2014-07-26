(ns test.tixi.mutators.current
  (:require-macros [cemerick.cljs.test :refer (is deftest use-fixtures)]
                   [tixi.utils :refer (b)])
  (:require [cemerick.cljs.test :as test]
            [tixi.geometry :as g]
            [tixi.mutators :as m]
            [tixi.mutators.current :as mc]
            [tixi.mutators.selection :as ms]
            [test.tixi.utils :refer [create-layer!]]
            [tixi.utils :refer [p]]
            [tixi.data :as d]))

(defn- setup [f]
  (m/reset-data!)
  (f))

(use-fixtures :each setup)

(deftest initiate-current-layer!
  (mc/initiate-current-layer! (g/build-point 2 3))
  (is (= (:input (:item (d/current))) (g/build-rect (g/build-point 2 3) (g/build-point 2 3)))))

(deftest update-current-layer!
  (mc/initiate-current-layer! (g/build-point 2 3))
  (mc/update-current-layer! (g/build-point 4 5))
  (is (= (:input (:item (d/current))) (g/build-rect (g/build-point 2 3) (g/build-point 4 5)))))

(deftest finish-current-layer!
  (mc/initiate-current-layer! (g/build-point 2 3))
  (mc/update-current-layer! (g/build-point 4 5))
  (let [{:keys [id item]} (d/current)]
    (mc/finish-current-layer!)
    (is (= (d/current) nil))
    (is (= (d/completed-item id) item))))

(deftest locking
  (m/set-tool! :rect)
  (let [id1 (create-layer! (g/build-rect 3 3 13 13))]
    (m/set-tool! :line)
    (let [id2 (create-layer! (g/build-rect 2 2 3 3))]
      (ms/select-layer! id1)
      (ms/move-selection! (g/build-size 2 2))
      (is (= (:input (d/completed-item id1)) (g/build-rect 5 5 15 15)))
      (is (= (:input (d/completed-item id2)) (g/build-rect 2 2 5 5))))))
