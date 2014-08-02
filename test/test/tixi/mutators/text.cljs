(ns test.tixi.mutators.text
  (:require-macros [cemerick.cljs.test :refer (is deftest use-fixtures)]
                   [tixi.utils :refer (b)])
  (:require [cemerick.cljs.test :as test]
            [tixi.geometry :as g]
            [tixi.mutators :as m]
            [tixi.mutators.text :as mt]
            [test.tixi.utils :refer [create-layer! create-sample-layer!]]
            [tixi.utils :refer [p]]
            [tixi.data :as d]))

(defn- setup [f]
  (m/reset-data!)
  (f))

(use-fixtures :each setup)

(deftest edit-text-in-item!
  (mt/edit-text-in-item! 3)
  (is (= (d/edit-text-id) 3)))

(deftest set-text-to-item!
  (let [id (create-layer! (g/build-rect (g/build-point 5 6) (g/build-point 7 8)))]
    (mt/set-text-to-item! id "bla")
    (is (= (:text (d/completed-item id)) "bla"))))

(deftest set-text-to-item-with-dimensions
  (m/set-tool! :text)
  (let [id (create-layer! (g/build-rect (g/build-point 5 6) (g/build-point 5 6)))]
    (mt/set-text-to-item! id "bla\nfoo\nbar")
    (is (= (:input (d/completed-item id)) (g/build-rect 5 6 7 8)))))
