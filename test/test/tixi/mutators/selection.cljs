(ns test.tixi.mutators.selection
  (:require-macros [cemerick.cljs.test :refer (is deftest use-fixtures)]
                   [tixi.utils :refer (b)])
  (:require [cemerick.cljs.test :as test]
            [tixi.geometry :as g :refer [Rect Point Size]]
            [tixi.mutators :as m]
            [tixi.mutators.selection :as ms]
            [tixi.mutators.render :as mr]
            [tixi.mutators.undo :as mu]
            [test.tixi.utils :refer [create-layer! create-sample-layer! create-locked-layers! item-with-input]]
            [tixi.utils :refer [p]]
            [tixi.data :as d]))

(defn- setup [f]
  (m/reset-data!)
  (f))

(use-fixtures :each setup)

(deftest start-selection!
  (ms/start-selection! (Point. 2 3))
  (is (= (d/current-selection) (g/build-rect (Point. 2 3) (Point. 2 3)))))

(deftest update-selection!
  (ms/start-selection! (Point. 2 3))
  (ms/update-selection! (Point. 5 6))
  (is (= (d/current-selection) (g/build-rect (Point. 2 3) (Point. 5 6)))))

(deftest finish-selection!
  (let [id (create-sample-layer!)]
    (ms/start-selection! (Point. 1 2))
    (ms/update-selection! (Point. 5 6))
    (ms/finish-selection!)
    (is (= (d/selected-ids) [id]))
    (is (= (d/selection-rect) (g/build-rect (Point. 2 3) (Point. 4 5))))
    (is (= (d/current-selection) nil))
    (is (= (d/selected-rel-rect id) (g/build-rect (Point. 0 0) (Point. 1 1))))))

(deftest select-layer!
  (let [id (create-sample-layer!)]
    (ms/select-layer! id (Point. 3 4))
    (is (= (d/selected-ids) [id]))
    (is (= (d/selection-rect) (g/build-rect (Point. 2 3) (Point. 4 5))))
    (is (= (d/current-selection) nil))
    (is (= (d/selected-rel-rect id) (g/build-rect (Point. 0 0) (Point. 1 1))))))

(deftest highlight-layer!
  (create-layer! (g/build-rect (Point. 2 3) (Point. 4 5)))
  (let [[id _] (first (d/completed))]
    (ms/highlight-layer! id)
    (is (= (d/hover-id) id))
    (ms/highlight-layer! nil)
    (is (= (d/hover-id) nil))))

(deftest move-selection!-one-item
  (let [id (create-sample-layer!)]
    (ms/select-layer! id (Point. 3 4))
    (ms/move-selection! (Size. 4 5))
    (is (= (d/selection-rect) (g/build-rect (Point. 6 8) (Point. 8 10))))
    (is (= (:input (d/completed-item id)) (g/build-rect (Point. 6 8) (Point. 8 10))))))

(deftest move-selection!-two-items
  (let [id1 (create-sample-layer!)
        id2 (create-layer! (g/build-rect (Point. 3 4) (Point. 5 6)))]
    (ms/select-layer! id1 (Point. 2 3))
    (ms/select-layer! id2 (Point. 5 6) true)
    (ms/move-selection! (Size. 4 5))
    (is (= (d/selection-rect) (g/build-rect (Point. 6 8) (Point. 9 11))))
    (is (= (:input (d/completed-item id1)) (g/build-rect (Point. 6 8) (Point. 8 10))))
    (is (= (:input (d/completed-item id2)) (g/build-rect (Point. 7 9) (Point. 9 11))))))

(deftest move-selection!-one-item
  (let [id (create-sample-layer!)]
    (ms/select-layer! id (Point. 3 4))
    (ms/resize-selection! (Size. 4 5) :se)
    (is (= (d/selection-rect) (g/build-rect (Point. 2 3) (Point. 8 10))))))

(deftest add-lock
  (let [[id1 id2 _] (create-locked-layers!)]
    (ms/select-layer! id1)
    (ms/move-selection! (g/Size. 2 2))
    (is (= (:input (d/completed-item id2)) (g/build-rect 2 2 7 7)))))

(deftest remove-lock
  (let [[id1 id2 _] (create-locked-layers!)]
    (ms/select-layer! id2)
    (ms/resize-selection! (g/Size. -1 -1) :se)
    (ms/select-layer! id1)
    (ms/move-selection! (g/Size. 2 2))
    (is (= (:input (d/completed-item id2)) (g/build-rect 2 2 4 4)))))

(deftest copy-paste
  (let [id1 (create-layer! (g/build-rect 0 0 4 4))
        id2 (create-layer! (g/build-rect 5 5 8 8))
        id3 (create-layer! (g/build-rect 10 10 15 15))]
    (ms/select-layer! id2)
    (ms/select-layer! id3 nil true)
    (ms/copy!)
    (ms/paste!)
    (is (= (count (d/completed)) 5))
    (is (not= (item-with-input (g/build-rect 6 6 9 9)) nil))
    (is (not= (item-with-input (g/build-rect 11 11 16 16)) nil))
    (let [id4 (first (item-with-input (g/build-rect 6 6 9 9)))
          id5 (first (item-with-input (g/build-rect 11 11 16 16)))]
      (is (= (d/selected-ids) [id4 id5])))))

(deftest cut-paste
  (let [id1 (create-layer! (g/build-rect 0 0 4 4))]
    (ms/select-layer! id1)
    (ms/cut!)
    (ms/paste!)
    (is (= (count (d/completed)) 1))
    (is (= (item-with-input (g/build-rect 0 0 4 4)) nil))
    (is (not= (item-with-input (g/build-rect 1 1 5 5)) nil))))

(deftest copy-paste-undo
  (let [id1 (create-layer! (g/build-rect 0 0 4 4))]
    (ms/select-layer! id1)
    (ms/copy!)
    (ms/paste!)
    (mu/undo!)
    (is (= (keys (d/completed)) [id1]))))

(deftest copy-paste-locked-items
  (let [[id1 id2 id3] (create-locked-layers!)]
    (ms/select-layer! id1)
    (ms/select-layer! id2 nil true)
    (ms/copy!)
    (ms/paste!)
    (let [new-rect-id (first (item-with-input (g/build-rect 6 6 16 16)))
          new-connector-id (first (item-with-input (g/build-rect 3 3 6 6)))]
      (ms/select-layer! nil)
      (ms/select-layer! new-rect-id)
      (ms/move-selection! (Size. 4 4))
      (is (= (:input (d/completed-item new-connector-id)) (g/build-rect 3 3 10 10))))))
