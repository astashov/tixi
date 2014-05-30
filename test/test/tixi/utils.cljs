(ns test.tixi.utils
  (:require-macros [cemerick.cljs.test :refer (is deftest use-fixtures)]
                   [tixi.utils :refer (b)])
  (:require [cemerick.cljs.test :as test]
            [tixi.mutators :as m]
            [tixi.utils :refer [p]]
            [tixi.data :as d]))

(defn create-layer! [rect]
  (m/initiate-current-layer! (:start-point rect))
  (m/update-current-layer! (:end-point rect))
  (m/finish-current-layer!)
  (last (keys (d/completed))))
