(ns test.tixi.mutators.current
  (:require-macros [cemerick.cljs.test :refer (is deftest use-fixtures)]
                   [tixi.utils :refer (b)])
  (:require [cemerick.cljs.test :as test]
            [tixi.geometry :as g :refer [Rect Point Size]]
            [tixi.mutators :as m]
            [tixi.mutators.current :as mc]
            [test.tixi.utils :refer [create-layer!]]
            [tixi.utils :refer [p]]
            [tixi.data :as d]))

(defn- setup [f]
  (m/reset-data!)
  (f))

(use-fixtures :each setup)

(deftest initiate-current-layer!
  (mc/initiate-current-layer! (Point. 2 3))
  (is (= (:input (:item (d/current))) (g/build-rect (Point. 2 3) (Point. 2 3)))))

(deftest update-current-layer!
  (mc/initiate-current-layer! (Point. 2 3))
  (mc/update-current-layer! (Point. 4 5))
  (is (= (:input (:item (d/current))) (g/build-rect (Point. 2 3) (Point. 4 5)))))

(deftest finish-current-layer!
  (mc/initiate-current-layer! (Point. 2 3))
  (mc/update-current-layer! (Point. 4 5))
  (let [{:keys [id item]} (d/current)]
    (mc/finish-current-layer!)
    (is (= (d/current) nil))
    (is (= (d/completed-item id) item))))
