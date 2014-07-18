(ns test.tixi.data
  (:require-macros [cemerick.cljs.test :as m :refer (is deftest use-fixtures)])
  (:require [cemerick.cljs.test :as test]
            [tixi.data :as d]
            [tixi.mutators :as m]
            [tixi.mutators.text :as mt]
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
        (mt/set-text-to-item! id2 "bla\nfoo\nbar")
        (mt/set-text-to-item! id3 "oh\ntext" (g/Size. 4 2))
        (is (= (.-width (d/result)) 19))
        (is (= (.-height (d/result)) 10))
        (is (= (.-content (d/result))
               (str "            -oh    \n"
                    "           / text  \n"
                    "        +---------+\n"
                    "        |         |\n"
                    "       /|   bla   |\n"
                    "     -/ |   foo   |\n"
                    "    /   |   bar   |\n"
                    "   /    |         |\n"
                    " -/     +---------+\n"
                    "/                  ")))))))

(deftest result-text-only
  (m/set-tool! :text)
  (let [id (create-layer! (g/build-rect 0 0 6 2))]
    (mt/set-text-to-item! id (str "foo\n\nbar") (g/Size. 3 3))
    (is (= (.-width (d/result)) 3))
    (is (= (.-height (d/result)) 3))
    (is (= (.-content (d/result))
           (str "foo\n"
                "   \n"
                "bar")))))

(deftest result-z-index
  (m/set-tool! :text)
  (let [id1 (create-layer! (g/build-rect 0 0 6 2))
        id2 (create-layer! (g/build-rect 0 0 6 2))]
    (mt/set-text-to-item! id1 (str "foo\n\nbar") (g/Size. 3 3))
    (mt/set-text-to-item! id2 (str "zuu\n\nwoo") (g/Size. 3 3))
    (is (= (.-content (d/result)) (str "zuu\n" "   \n" "woo")))
    (m/z-inc! [id1])
    (is (= (.-content (d/result)) (str "foo\n" "   \n" "bar")))))
