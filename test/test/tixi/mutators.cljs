(ns test.tixi.mutators
  (:require-macros [cemerick.cljs.test :refer (is deftest use-fixtures)]
                   [tixi.utils :refer (b)])
  (:require [cemerick.cljs.test :as test]
            [tixi.geometry :as g :refer [Rect Point Size]]
            [tixi.mutators :as m]
            [test.tixi.utils :refer [create-layer! create-sample-layer!]]
            [tixi.utils :refer [p]]
            [tixi.data :as d]))

(defn- setup [f]
  (m/reset-data!)
  (f))

(use-fixtures :each setup)

(deftest set-tool!
  (m/set-tool! :select)
  (is (= (d/tool) :select)))

(deftest set-action!
  (m/set-action! :draw)
  (is (= (d/action) :draw)))
