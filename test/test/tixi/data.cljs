(ns test.tixi.data
  (:require-macros [cemerick.cljs.test :as m :refer (is deftest)])
  (:require [cemerick.cljs.test :as test]
            [tixi.data :as d]))

(deftest current
  (is (= (d/current {:current "bla"}) "bla")))

(deftest completed
  (is (= (d/completed {:completed "bla"}) "bla")))

(deftest completed-item
  (is (= (d/completed-item {:completed {1 "bla"}} 1) "bla")))

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
