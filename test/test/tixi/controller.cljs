(ns test.tixi.controller
  (:require-macros [cemerick.cljs.test :as m :refer (is deftest use-fixtures)])
  (:require [cemerick.cljs.test :as test]
            [tixi.data :as d]
            [tixi.controller :as c]
            [tixi.mutators :as m]
            [tixi.geometry :as g]
            [tixi.mutators.selection :as ms]
            [tixi.utils :refer [p]]
            [test.tixi.utils :refer [create-layer!]]))

(defn- setup [f]
  (m/reset-data!)
  (f))

(use-fixtures :each setup)

(deftest keypress-z-inc
  (let [id (create-layer! (g/build-rect 2 2 6 6))]
    (ms/select-layer! id)
    (c/keypress :z-inc)
    (is (= (:z (d/completed-item id)) 1))))

(deftest keypress-z-dec
  (let [id (create-layer! (g/build-rect 2 2 6 6))]
    (ms/select-layer! id)
    (c/keypress :z-inc)
    (c/keypress :z-dec)
    (is (= (:z (d/completed-item id)) 0))))

(deftest keypress-z-show
  (c/keypress :z-show)
  (is (= (d/show-z-indexes?) true))
  (c/keypress :z-show)
  (is (= (d/show-z-indexes?) false)))
