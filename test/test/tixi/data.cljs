(ns test.tixi.data
  (:require-macros [cemerick.cljs.test :as m :refer (is deftest use-fixtures)])
  (:require [cemerick.cljs.test :as test]
            [tixi.data :as d]
            [tixi.mutators :as m]
            [tixi.tree :as t]
            [tixi.geometry :as g]
            [tixi.utils :refer [p]]
            [test.tixi.utils :refer [create-layer!]]))

(defn- setup [f]
  (m/reset-data!)
  (f))

(use-fixtures :each setup)

(deftest current
  (is (= (d/current {:current "bla"}) "bla")))

(deftest completed
  (is (= (d/completed {:state (d/zip (t/node {:completed "bla"}))}) "bla")))

(deftest completed-item
  (is (= (d/completed-item {:state (d/zip (t/node {:completed {1 "bla"}}))} 1) "bla")))

(deftest tool
  (is (= (d/tool {:tool "bla"}) "bla")))

(deftest action
  (is (= (d/action {:action "bla"}) "bla")))

(deftest selected-ids
  (is (= (d/selected-ids {:selection {:ids "bla"}}) "bla")))

(deftest selected-rel-rect
  (is (= (d/selected-rel-rect {:selection {:rel-rects {1 "bla"}}} 1) "bla")))

(deftest selection-rect
  (is (= (d/selection-rect {:selection {:rect "bla"}}) "bla")))

(deftest current-selection
  (is (= (d/current-selection {:selection {:current "bla"}}) "bla")))

(deftest hover-id
  (is (= (d/hover-id {:hover-id "bla"}) "bla")))

(deftest draw-tool-true
  (is (= (d/draw-tool? {:tool :rect}) true)))

(deftest draw-tool-false
  (is (= (d/draw-tool? {:tool :select}) false)))

(deftest select-tool-true
  (is (= (d/select-tool? {:tool :select}) true)))

(deftest select-tool-false
  (is (= (d/select-tool? {:tool :rect}) false)))

(deftest draw-action-true
  (is (= (d/draw-action? {:action :draw}) true)))

(deftest draw-action-false
  (is (= (d/draw-action? {:action :bla}) false)))

(deftest resize-action-ne
  (is (= (d/resize-action {:action :resize-ne}) :ne)))

(deftest resize-action-nil
  (is (= (d/resize-action {:action :bla}) nil)))

(deftest edit-text-id
  (is (= (d/edit-text-id {:edit-text-id 2}) 2)))

(deftest result
  (m/set-tool! :line)
  (let [id1 (create-layer! (g/build-rect 1 10 13 1))]
    (m/set-tool! :rect)
    (let [id2 (create-layer! (g/build-rect 9 3 19 9))]
      (m/set-tool! :text)
      (let [id3 (create-layer! (g/build-rect 14 1 14 1))]
        (m/set-text-to-item! id2 "bla\nfoo\nbar")
        (m/set-text-to-item! id3 "oh\ntext" (g/Size. 4 2))
        (is (= (d/result)
               (str "                     \n"
                    "             -oh     \n"
                    "            / text   \n"
                    "         +---------+ \n"
                    "         |         | \n"
                    "        /|   bla   | \n"
                    "      -/ |   foo   | \n"
                    "     /   |   bar   | \n"
                    "    /    |         | \n"
                    "  -/     +---------+ \n"
                    " /                   \n"
                    "                     \n"
                    "                     ")))))))
