(ns test.tixi.items
  (:require-macros [cemerick.cljs.test :as m :refer (is deftest)])
  (:require [cemerick.cljs.test :as test]
            [tixi.utils :refer [p]]
            [tixi.geometry :as g]
            [tixi.items :as i]))

(deftest build-line
  (let [rect (g/build-rect 2 3 4 6)
        item {:type :line :input rect}]
    (is (= (:input item) rect))
    (is (= (:text item) nil))
    (is (= (js->clj (i/render item)) {:points [[[0 0] {"v" "\\"}]
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
        item {:type :rect :input rect}]
    (is (= (:input item) rect))
    (is (= (:text item) nil))
    (is (= (js->clj (i/render item)) {:points [[[0 0] {"v" "+"}]
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
        item {:type :rect-line :input rect}]
    (is (= (:input item) rect))
    (is (= (:text item) nil))
    (is (= (js->clj (i/render item)) {:points [[[0 0] {"v" "-"}]
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
        item {:type :line :input rect}]
    (is (= (i/dimensions item) (g/build-size 2 3)))))

(deftest origin
  (let [rect (g/build-rect 2 3 4 6)
        item {:type :line :input rect}]
    (is (= (i/origin item) (g/build-point 2 3)))))

(deftest update
  (let [rect (g/build-rect 2 3 4 6)
        item {:type :rect-line :input rect}
        updated-item (i/update item (g/build-point 4 7))]
    (is (= (:input updated-item) (g/build-rect 2 3 4 7)))
    (is (= (:text updated-item) nil))
    (is (= (js->clj (i/render updated-item)) {:points [[[0 0] {"v" "|"}]
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
        item {:type :rect-line :input rect}
        repositioned-item (i/reposition item new-rect)]
    (is (= (:input repositioned-item) new-rect))
    (is (= (:text repositioned-item) nil))
    (is (= (js->clj (i/render repositioned-item)) {:points [[[0 0] {"v" "-"}]
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

(deftest reposition-text
  (let [rect (g/build-rect 2 3 4 6)
        new-rect (g/build-rect 3 4 20 20)
        item {:type :text :input rect :text "bla"}
        repositioned-item (i/reposition item new-rect)]
    (is (= (:input repositioned-item) (g/build-rect 3 4 5 7)))
    (is (= (:text repositioned-item) "bla"))))

(deftest set-text
  (let [item {:type :rect-line :input (g/build-rect 2 3 4 6)}
        item-with-text (i/set-text item "bla")]
    (is (= (:text item-with-text) "bla"))))

(deftest build-text
  (let [rect (g/build-rect 2 3 3 5)
        item {:type :text :input rect}]
    (is (= (:input item) rect))
    (is (= (:text item) nil))
    (is (= (js->clj (i/render item)) {:points [], :data " \n ", :index {}}))))

(deftest update-text
  (let [rect (g/build-rect 2 3 2 3)
        item {:type :text :input rect}
        updated-item (i/update item (g/build-point 4 5))]
    (is (= (:input updated-item) (g/build-rect 4 5 4 5)))
    (is (= (:text updated-item) nil))
    (is (= (js->clj (i/render updated-item)) {:points [], :data "", :index {}}))))

(deftest set-text-to-text-item
  (let [rect (g/build-rect 2 3 2 3)
        item {:type :text :input rect}
        item-with-text (i/set-text item "bla\nfoos\nbar")]
    (is (= (:input item-with-text) (g/build-rect 2 3 5 5)))
    (is (= (:text item-with-text) "bla\nfoos\nbar"))
    (is (= (js->clj (i/render item-with-text)) {:points [], :data "   \n   ", :index {}}))))

(deftest build-line-with-edges
  (let [rect (g/build-rect 2 2 7 5)
        item {:type :line :input rect :edges {:start :arrow :end :arrow} :connected #{:start :end}}]
    (is (= (js->clj (i/render item)) {:points [[[0 0] {"v" "+"}]
                                               [[1 1] {"v" "<"}]
                                               [[2 1] {"v" "\\"}]
                                               [[3 2] {"v" "-"}]
                                               [[4 2] {"v" ">"}]
                                               [[5 3] {"v" "+"}]]
                                      :data "+    \n <\\  \n   ->\n     +"
                                      :index {"0_0" {"v" "+"}
                                              "1_1" {"v" "<"}
                                              "2_1" {"v" "\\"}
                                              "3_2" {"v" "-"}
                                              "4_2" {"v" ">"}
                                              "5_3" {"v" "+"}}}))))

(deftest text-dimensions
  (is (= (i/text-dimensions {:type :text :text "abcde\nab\nabcasss"}) (g/build-size 7 3))))

(deftest lines
  (let [rect (g/build-rect 2 2 7 20)
        item {:type :rect :input rect :text "abcde\nab\n\nabcdefg"}]
    (is (= (i/lines item (:text item)) ["abcd" "e" "ab" "" "abcd" "efg"]))))
