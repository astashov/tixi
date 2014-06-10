(ns test.tixi.mutators
  (:require-macros [cemerick.cljs.test :refer (is deftest use-fixtures)]
                   [tixi.utils :refer (b)])
  (:require [cemerick.cljs.test :as test]
            [tixi.geometry :as g :refer [Rect Point Size]]
            [tixi.mutators :as m]
            [test.tixi.utils :refer [create-layer!]]
            [tixi.utils :refer [p]]
            [tixi.data :as d]))

(defn- setup [f]
  (m/reset-data!)
  (f))

(defn- create-sample-layer! []
  (create-layer! (g/build-rect (Point. 2 3) (Point. 4 5))))

(use-fixtures :each setup)

(deftest set-tool!
  (m/set-tool! :select)
  (is (= (d/tool) :select)))

(deftest set-action!
  (m/set-action! :draw)
  (is (= (d/action) :draw)))

(deftest initiate-current-layer!
  (m/initiate-current-layer! (Point. 2 3))
  (is (= (:input (:item (d/current))) (g/build-rect (Point. 2 3) (Point. 2 3)))))

(deftest update-current-layer!
  (m/initiate-current-layer! (Point. 2 3))
  (m/update-current-layer! (Point. 4 5))
  (is (= (:input (:item (d/current))) (g/build-rect (Point. 2 3) (Point. 4 5)))))

(deftest finish-current-layer!
  (m/initiate-current-layer! (Point. 2 3))
  (m/update-current-layer! (Point. 4 5))
  (let [{:keys [id item]} (d/current)]
    (m/finish-current-layer!)
    (is (= (d/current) nil))
    (is (= (d/completed-item id) item))))

(deftest start-selection!
  (m/start-selection! (Point. 2 3))
  (is (= (d/current-selection) (g/build-rect (Point. 2 3) (Point. 2 3)))))

(deftest update-selection!
  (m/start-selection! (Point. 2 3))
  (m/update-selection! (Point. 5 6))
  (is (= (d/current-selection) (g/build-rect (Point. 2 3) (Point. 5 6)))))

(deftest finish-selection!
  (let [id (create-sample-layer!)]
    (m/start-selection! (Point. 1 2))
    (m/update-selection! (Point. 5 6))
    (m/finish-selection!)
    (is (= (d/selected-ids) [id]))
    (is (= (d/selection-rect) (g/build-rect (Point. 2 3) (Point. 4 5))))
    (is (= (d/current-selection) nil))
    (is (= (d/selected-rel-rect id) (g/build-rect (Point. 0 0) (Point. 1 1))))))

(deftest select-layer!
  (let [id (create-sample-layer!)]
    (m/select-layer! id (Point. 3 4))
    (is (= (d/selected-ids) [id]))
    (is (= (d/selection-rect) (g/build-rect (Point. 2 3) (Point. 4 5))))
    (is (= (d/current-selection) nil))
    (is (= (d/selected-rel-rect id) (g/build-rect (Point. 0 0) (Point. 1 1))))))

(deftest highlight-layer!
  (create-layer! (g/build-rect (Point. 2 3) (Point. 4 5)))
  (let [[id _] (first (d/completed))]
    (m/highlight-layer! id)
    (is (= (d/hover-id) id))
    (m/highlight-layer! nil)
    (is (= (d/hover-id) nil))))

(deftest move-selection!-one-item
  (let [id (create-sample-layer!)]
    (m/select-layer! id (Point. 3 4))
    (m/move-selection! (Size. 4 5))
    (is (= (d/selection-rect) (g/build-rect (Point. 6 8) (Point. 8 10))))
    (is (= (:input (d/completed-item id)) (g/build-rect (Point. 6 8) (Point. 8 10))))))

(deftest move-selection!-two-items
  (let [id1 (create-sample-layer!)
        id2 (create-layer! (g/build-rect (Point. 3 4) (Point. 5 6)))]
    (m/select-layer! id1 (Point. 2 3))
    (m/select-layer! id2 (Point. 5 6) true)
    (m/move-selection! (Size. 4 5))
    (is (= (d/selection-rect) (g/build-rect (Point. 6 8) (Point. 9 11))))
    (is (= (:input (d/completed-item id1)) (g/build-rect (Point. 6 8) (Point. 8 10))))
    (is (= (:input (d/completed-item id2)) (g/build-rect (Point. 7 9) (Point. 9 11))))))

(deftest move-selection!-one-item
  (let [id (create-sample-layer!)]
    (m/select-layer! id (Point. 3 4))
    (m/resize-selection! (Size. 4 5) :se)
    (is (= (d/selection-rect) (g/build-rect (Point. 2 3) (Point. 8 10))))))

(deftest delete-selected!
  (let [id1 (create-sample-layer!)
        id2 (create-layer! (g/build-rect (Point. 5 6) (Point. 7 8)))
        id3 (create-layer! (g/build-rect (Point. 9 10) (Point. 11 12)))]
    (m/select-layer! id1 (Point. 2 3))
    (m/select-layer! id2 (Point. 5 6) true)
    (m/delete-selected!)
    (is (= (d/selected-ids) []))
    (is (= (d/selection-rect) nil))
    (is (= (d/current-selection) nil))
    (is (= (vec (keys (d/completed))) [id3]))))

(deftest undo!
  (let [id1 (create-layer! (g/build-rect (Point. 5 6) (Point. 7 8)))]
    (m/snapshot!)
    (let [id2 (create-layer! (g/build-rect (Point. 9 10) (Point. 11 12)))]
      (is (= (keys (d/completed)) '(0 1)))
      (m/undo!)
      (is (= (keys (d/completed)) '(0)))
      (m/undo!)
      (is (= (keys (d/completed)) '(0))))))

(deftest redo!
  (let [id1 (create-layer! (g/build-rect (Point. 5 6) (Point. 7 8)))]
    (m/snapshot!)
    (let [id2 (create-layer! (g/build-rect (Point. 9 10) (Point. 11 12)))]
      (is (= (keys (d/completed)) '(0 1)))
      (m/undo!)
      (is (= (keys (d/completed)) '(0)))
      (m/redo!)
      (is (= (keys (d/completed)) '(0 1)))
      (m/redo!)
      (is (= (keys (d/completed)) '(0 1))))))

(deftest undo-if-unchanged!
  (let [id1 (create-layer! (g/build-rect (Point. 5 6) (Point. 7 8)))]
    (m/snapshot!)
    (let [id2 (create-layer! (g/build-rect (Point. 9 10) (Point. 11 12)))]
      (is (= (keys (d/completed)) '(0 1)))
      (m/undo-if-unchanged!)
      (is (= (keys (d/completed)) '(0 1)))
      (m/snapshot!)
      (m/undo-if-unchanged!)
      (m/undo!)
      (is (= (keys (d/completed)) '(0))))))

(deftest edit-text-in-item!
  (m/edit-text-in-item! 3)
  (is (= (d/edit-text-id) 3)))

(deftest set-text-to-item!
  (let [id (create-layer! (g/build-rect (Point. 5 6) (Point. 7 8)))]
    (m/set-text-to-item! id "bla")
    (is (= (:text (d/completed-item id)) "bla"))))
