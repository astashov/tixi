(ns test.tixi.mutators.locks
  (:require-macros [cemerick.cljs.test :refer (is deftest use-fixtures)]
                   [tixi.utils :refer (b)])
  (:require [cemerick.cljs.test :as test]
            [tixi.geometry :as g]
            [tixi.mutators :as m]
            [tixi.mutators.locks :as ml]
            [test.tixi.utils :refer [create-item!]]
            [tixi.utils :refer [p]]
            [tixi.data :as d]))

(defn- setup [f]
  (m/reset-data!)
  (f))

(use-fixtures :each setup)

(deftest add-chars-when-lock
  (m/set-tool! :rect)
  (let [id1 (create-item! (g/build-rect 5 5 13 13))]
    (m/set-tool! :line)
    (let [id2 (create-item! (g/build-rect 2 2 5 5))]
      (is (= (aget (aget (:index (d/item-cache id2)) "3_3") "v")
             "+")))))

(deftest move-existing-line-edges-when-lock
  (m/set-tool! :rect)
  (let [id1 (create-item! (g/build-rect 5 5 13 13))]
    (m/set-tool! :line)
    (m/cycle-line-edge! :end)
    (let [id2 (create-item! (g/build-rect 2 2 5 5))]
      (is (= (aget (aget (:index (d/item-cache id2)) "2_2") "v")
             ">"))
      (is (= (aget (aget (:index (d/item-cache id2)) "3_3") "v")
             "+")))))
