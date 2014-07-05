(ns test.tixi.items
  (:require-macros [cemerick.cljs.test :as m :refer (is deftest)])
  (:require [cemerick.cljs.test :as test]
            [tixi.utils :refer [p]]
            [tixi.geometry :as g]
            [tixi.items :as i :refer [Rect Line RectLine]]))

(deftest build-line
  (let [rect (g/build-rect 2 3 4 6)
        item (i/build-item {:type :line :input rect})]
    (is (= (:input item) rect))
    (is (= (:text item) nil))
    (is (= (js->clj (:cache item)) {:points [[[0 0] {"v" "\\"}]
                                             [[1 1] {"v" "|"}]
                                             [[1 2] {"v" "\\"}]
                                             [[2 3] {"v" "|"}]]
                                    :data "\\ \n |\n \\\n  |"
                                    :index {"0_0" {"v" "\\"}
                                            "1_1" {"v" "|"}
                                            "1_2" {"v" "\\"}
                                            "2_3" {"v" "|"}}}))))

(deftest build-rect
  (let [rect (g/build-rect 2 3 4 6)
        item (i/build-item {:type :rect :input rect})]
    (is (= (:input item) rect))
    (is (= (:text item) nil))
    (is (= (js->clj (:cache item)) {:points [[[0 0] {"v" "+"}]
                                             [[1 0] {"v" "-"}]
                                             [[2 0] {"v" "+"}]
                                             [[0 1] {"v" "|"}]
                                             [[2 1] {"v" "|"}]
                                             [[0 2] {"v" "|"}]
                                             [[2 2] {"v" "|"}]
                                             [[0 3] {"v" "+"}]
                                             [[1 3] {"v" "-"}]
                                             [[2 3] {"v" "+"}]]
                                    :data "+-+\n| |\n| |\n+-+"
                                    :index {"0_0" {"v" "+"}
                                            "1_0" {"v" "-"}
                                            "0_1" {"v" "|"}
                                            "2_0" {"v" "+"}
                                            "0_2" {"v" "|"}
                                            "2_1" {"v" "|"}
                                            "0_3" {"v" "+"}
                                            "2_2" {"v" "|"}
                                            "1_3" {"v" "-"}
                                            "2_3" {"v" "+"}}}))))

(deftest build-rect-line
  (let [rect (g/build-rect 2 3 4 6)
        item (i/build-item {:type :rect-line :input rect})]
    (is (= (:input item) rect))
    (is (= (:text item) nil))
    (is (= (js->clj (:cache item)) {:points [[[0 0] {"v" "-"}]
                                             [[1 0] {"v" "-"}]
                                             [[2 0] {"v" "+"}]
                                             [[2 1] {"v" "|"}]
                                             [[2 2] {"v" "|"}]
                                             [[2 3] {"v" "|"}]]
                                    :data "--+\n  |\n  |\n  |"
                                    :index {"2_0" {"v" "+"}
                                            "0_0" {"v" "-"}
                                            "1_0" {"v" "-"}
                                            "2_1" {"v" "|"}
                                            "2_2" {"v" "|"}
                                            "2_3" {"v" "|"}}}))))

(deftest dimensions
  (let [rect (g/build-rect 2 3 4 6)
        item (i/build-item {:type :line :input rect})]
    (is (= (i/dimensions item) (g/Size. 2 3)))))

(deftest origin
  (let [rect (g/build-rect 2 3 4 6)
        item (i/build-item {:type :line :input rect})]
    (is (= (i/origin item) (g/Point. 2 3)))))

(deftest update
  (let [rect (g/build-rect 2 3 4 6)
        item (i/build-item {:type :rect-line :input rect})
        updated-item (i/update item (g/Point. 4 7))]
    (is (= (:input updated-item) (g/build-rect 2 3 4 7)))
    (is (= (:text updated-item) nil))
    (is (= (js->clj (:cache updated-item)) {:points [[[0 0] {"v" "|"}]
                                                     [[0 1] {"v" "|"}]
                                                     [[0 2] {"v" "|"}]
                                                     [[0 3] {"v" "|"}]
                                                     [[0 4] {"v" "+"}]
                                                     [[1 4] {"v" "-"}]
                                                     [[2 4] {"v" "-"}]]
                                            :data "| \n| \n| \n| \n+--"
                                            :index {"0_4" {"v" "+"}
                                                    "0_0" {"v" "|"}
                                                    "0_1" {"v" "|"}
                                                    "0_2" {"v" "|"}
                                                    "0_3" {"v" "|"}
                                                    "1_4" {"v" "-"}
                                                    "2_4" {"v" "-"}}}))))

(deftest reposition
  (let [rect (g/build-rect 2 3 4 6)
        new-rect (g/build-rect 3 4 5 7)
        item (i/build-item {:type :rect-line :input rect})
        repositioned-item (i/reposition item new-rect)]
    (is (= (:input repositioned-item) new-rect))
    (is (= (:text repositioned-item) nil))
    (is (= (js->clj (:cache repositioned-item)) {:points [[[0 0] {"v" "-"}]
                                                          [[1 0] {"v" "-"}]
                                                          [[2 0] {"v" "+"}]
                                                          [[2 1] {"v" "|"}]
                                                          [[2 2] {"v" "|"}]
                                                          [[2 3] {"v" "|"}]]
                                                 :data "--+\n  |\n  |\n  |"
                                                 :index {"2_0" {"v" "+"}
                                                         "0_0" {"v" "-"}
                                                         "1_0" {"v" "-"}
                                                         "2_1" {"v" "|"}
                                                         "2_2" {"v" "|"}
                                                         "2_3" {"v" "|"}}}))))

(deftest set-text
  (let [item (i/build-item {:type :rect-line :input (g/build-rect 2 3 4 6)})
        item-with-text (i/set-text item "bla")]
    (is (= (:text item-with-text) "bla"))))


(deftest build-text
  (let [rect (g/build-rect 2 3 3 5)
        item (i/build-item {:type :text :input rect})]
    (is (= (:input item) rect))
    (is (= (:text item) nil))
    (is (= (js->clj (:cache item)) {:points [], :data " \n ", :index {}}))))

(deftest update-text
  (let [rect (g/build-rect 2 3 2 3)
        item (i/build-item {:type :text :input rect})
        updated-item (i/update item (g/Point. 4 5))]
    (is (= (:input updated-item) (g/build-rect 4 5 4 5)))
    (is (= (:text updated-item) nil))
    (is (= (js->clj (:cache updated-item)) {:points [], :data "", :index {}}))))

(deftest reposition-text
  (let [rect (g/build-rect 2 3 2 3)
        item (i/build-item {:type :text :input rect})
        repositioned-item (i/reposition item (g/build-rect 4 5 4 5))
        non-repositioned-item (i/reposition item (g/build-rect 4 5 6 7))] 
    (is (= (:input repositioned-item) (g/build-rect 4 5 4 5)))
    (is (= (:input non-repositioned-item) rect))))

(deftest set-text-with-dimensions
  (let [rect (g/build-rect 2 3 2 3)
        item (i/build-item {:type :text :input rect})
        item-with-text (i/set-text item "bla" (g/Size. 3 4))]
    (is (= (:input item-with-text) (g/build-rect 2 3 4 6)))
    (is (= (:text item-with-text) "bla"))
    (is (= (js->clj (:cache item-with-text)) {:points [], :data "  \n  \n  ", :index {}}))))
