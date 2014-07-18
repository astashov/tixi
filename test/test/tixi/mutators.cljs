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

(deftest z-inc!
  (let [id1 (create-layer! (g/build-rect 2 2 6 6))]
    (m/z-inc! [id1])
    (m/z-inc! [id1])
    (let [id2 (create-layer! (g/build-rect 7 7 10 10))]
      (m/z-inc! [id2])
      (m/z-inc! [id1])
      (is (= (:z (d/completed-item id1)) 2))
      (is (= (:z (d/completed-item id2)) 1))

      (m/z-inc! [id1 id2])
      (is (= (:z (d/completed-item id1)) 3))
      (is (= (:z (d/completed-item id2)) 2)))))

(deftest z-dec!
  (let [id1 (create-layer! (g/build-rect 2 2 6 6))]
    (m/z-dec! [id1])
    (m/z-dec! [id1])
    (is (= (:z (d/completed-item id1)) 0))
    (let [id2 (create-layer! (g/build-rect 7 7 10 10))]
      (m/z-inc! [id1 id2])
      (m/z-inc! [id1 id2])
      (m/z-dec! [id1 id2])
      (is (= (:z (d/completed-item id1)) 1))
      (is (= (:z (d/completed-item id2)) 1)))))

(deftest z-show!
  (m/z-show! true)
  (is (= (d/show-z-indexes?) true)))
