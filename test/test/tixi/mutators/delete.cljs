(ns test.tixi.mutators.delete
  (:require-macros [cemerick.cljs.test :refer (is deftest use-fixtures)]
                   [tixi.utils :refer (b)])
  (:require [cemerick.cljs.test :as test]
            [tixi.geometry :as g]
            [tixi.mutators :as m]
            [tixi.mutators.selection :as ms]
            [tixi.mutators.delete :as md]
            [test.tixi.utils :refer [create-item! create-sample-item! create-locked-items!]]
            [tixi.utils :refer [p]]
            [tixi.data :as d]))

(defn- setup [f]
  (m/reset-data!)
  (f))

(use-fixtures :each setup)

(deftest delete-items!
  (let [id1 (create-sample-item!)
        id2 (create-item! (g/build-rect (g/build-point 5 6) (g/build-point 7 8)))
        id3 (create-item! (g/build-rect (g/build-point 9 10) (g/build-point 11 12)))]
    (ms/select-item! id1 (g/build-point 2 3))
    (ms/select-item! id2 (g/build-point 5 6) true)
    (md/delete-items! (d/selected-ids))
    (is (= (d/current-selection) nil))
    (is (= (vec (keys (d/completed))) [id3]))))

(deftest delete-locked-connector
  (let [[id1 id2 id3] (create-locked-items!)]
    (ms/select-item! id2)
    (md/delete-items! (d/selected-ids))
    (is (empty? (get-in (d/state) [:locks :connectors id2])))
    (is (empty? (get-in (d/state) [:locks :outlets id1 [id2 :end]])))))

(deftest delete-locked-outlet
  (let [[id1 id2 id3] (create-locked-items!)]
    (ms/select-item! id1)
    (md/delete-items! (d/selected-ids))
    (is (empty? (get-in (d/state) [:locks :connectors id2 :end])))
    (is (empty? (get-in (d/state) [:locks :connectors id3 :end])))
    (is (empty? (get-in (d/state) [:locks :outlets id1])))))
