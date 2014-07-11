(ns test.tixi.utils
  (:require-macros [cemerick.cljs.test :refer (is deftest use-fixtures)]
                   [tixi.utils :refer (b)])
  (:require [cemerick.cljs.test :as test]
            [tixi.geometry :as g :refer [Point]]
            [tixi.mutators.current :as mc]
            [tixi.mutators.render :as mr]
            [tixi.utils :refer [p]]
            [tixi.data :as d]))

(defn create-layer! [rect]
  (mc/initiate-current-layer! (:start-point rect))
  (mc/update-current-layer! (:end-point rect))
  (mc/finish-current-layer!)
  (mr/render-items!)
  (last (keys (d/completed))))

(defn- create-sample-layer! []
  (create-layer! (g/build-rect (Point. 2 3) (Point. 4 5))))
