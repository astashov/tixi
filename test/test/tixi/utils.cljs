(ns test.tixi.utils
  (:require-macros [cemerick.cljs.test :refer (is deftest use-fixtures)]
                   [tixi.utils :refer (b)])
  (:require [cemerick.cljs.test :as test]
            [tixi.geometry :as g]
            [tixi.mutators :as m]
            [tixi.mutators.current :as mc]
            [tixi.mutators.selection :as ms]
            [tixi.mutators.render :as mr]
            [tixi.utils :refer [p]]
            [tixi.data :as d]))

(defn create-item! [rect]
  (mc/initiate-current-item! (:start rect))
  (mc/update-current-item! (:end rect))
  (mc/finish-current-item!)
  (mr/render-items!)
  (last (keys (d/completed))))

(defn- create-sample-item! []
  (create-item! (g/build-rect (g/build-point 2 3) (g/build-point 4 5))))

(defn create-locked-items! []
  (m/set-tool! :rect)
  (let [id1 (create-item! (g/build-rect 5 5 15 15))]
    (m/set-tool! :line)
    (let [id2 (create-item! (g/build-rect 2 2 3 3))
          id3 (create-item! (g/build-rect 10 10 12 12))]
      (ms/select-item! id2)
      (ms/resize-selection! (g/build-size 2 2) :se)
      (ms/select-item! id3)
      (ms/resize-selection! (g/build-size 3 3) :se)
      [id1 id2 id3])))

(defn item-with-input [input]
  (first (filter (fn [[id item]] (= (:input item) input)) (d/completed))))
