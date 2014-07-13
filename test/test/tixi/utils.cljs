(ns test.tixi.utils
  (:require-macros [cemerick.cljs.test :refer (is deftest use-fixtures)]
                   [tixi.utils :refer (b)])
  (:require [cemerick.cljs.test :as test]
            [tixi.geometry :as g :refer [Point]]
            [tixi.mutators :as m]
            [tixi.mutators.current :as mc]
            [tixi.mutators.selection :as ms]
            [tixi.mutators.render :as mr]
            [tixi.utils :refer [p]]
            [tixi.data :as d]))

(defn create-layer! [rect]
  (mc/initiate-current-layer! (:start rect))
  (mc/update-current-layer! (:end rect))
  (mc/finish-current-layer!)
  (mr/render-items!)
  (last (keys (d/completed))))

(defn- create-sample-layer! []
  (create-layer! (g/build-rect (Point. 2 3) (Point. 4 5))))

(defn create-locked-layers! []
  (m/set-tool! :rect)
  (let [id1 (create-layer! (g/build-rect 5 5 15 15))]
    (m/set-tool! :line)
    (let [id2 (create-layer! (g/build-rect 2 2 3 3))
          id3 (create-layer! (g/build-rect 10 10 12 12))]
      (ms/select-layer! id2)
      (ms/resize-selection! (g/Size. 2 2) :se)
      (ms/select-layer! id3)
      (ms/resize-selection! (g/Size. 3 3) :se)
      [id1 id2 id3])))
