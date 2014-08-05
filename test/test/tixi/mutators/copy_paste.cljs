(ns test.tixi.mutators.copy-paste
  (:require-macros [cemerick.cljs.test :refer (is deftest use-fixtures)]
                   [tixi.utils :refer (b)])
  (:require [cemerick.cljs.test :as test]
            [tixi.geometry :as g]
            [tixi.mutators :as m]
            [tixi.mutators.copy-paste :as mcp]
            [tixi.mutators.selection :as ms]
            [tixi.mutators.undo :as mu]
            [test.tixi.utils :refer [create-item! create-sample-item! create-locked-items! item-with-input]]
            [tixi.utils :refer [p]]
            [tixi.data :as d]))

(defn- setup [f]
  (m/reset-data!)
  (f))

(use-fixtures :each setup)

(deftest copy-paste
  (let [id1 (create-item! (g/build-rect 0 0 4 4))
        id2 (create-item! (g/build-rect 5 5 8 8))
        id3 (create-item! (g/build-rect 10 10 15 15))]
    (ms/select-item! id2)
    (ms/select-item! id3 nil true)
    (mcp/copy!)
    (mcp/paste!)
    (is (= (count (d/completed)) 5))
    (is (not= (item-with-input (g/build-rect 6 6 9 9)) nil))
    (is (not= (item-with-input (g/build-rect 11 11 16 16)) nil))
    (let [id4 (first (item-with-input (g/build-rect 6 6 9 9)))
          id5 (first (item-with-input (g/build-rect 11 11 16 16)))]
      (is (= (d/selected-ids) #{id4 id5})))))

(deftest cut-paste
  (let [id1 (create-item! (g/build-rect 0 0 4 4))]
    (ms/select-item! id1)
    (mcp/cut!)
    (mcp/paste!)
    (is (= (count (d/completed)) 1))
    (is (= (item-with-input (g/build-rect 0 0 4 4)) nil))
    (is (not= (item-with-input (g/build-rect 1 1 5 5)) nil))))

(deftest copy-paste-undo
  (let [id1 (create-item! (g/build-rect 0 0 4 4))]
    (ms/select-item! id1)
    (mcp/copy!)
    (mcp/paste!)
    (mu/undo!)
    (is (= (keys (d/completed)) [id1]))))

(deftest copy-paste-locked-items
  (let [[id1 id2 id3] (create-locked-items!)]
    (ms/select-item! id1)
    (ms/select-item! id2 nil true)
    (mcp/copy!)
    (mcp/paste!)
    (let [new-rect-id (first (item-with-input (g/build-rect 6 6 16 16)))
          new-connector-id (first (item-with-input (g/build-rect 3 3 6 6)))]
      (ms/select-item! nil)
      (ms/select-item! new-rect-id)
      (ms/move-selection! (g/build-size 4 4))
      (is (= (:input (d/completed-item new-connector-id)) (g/build-rect 3 3 10 10))))))

