(ns test.tixi.items
  (:require-macros [cemerick.cljs.test :as m :refer (is deftest)])
  (:require [cemerick.cljs.test :as test]
            [tixi.utils :refer [p]]
            [tixi.geometry :as g]
            [tixi.items :as i :refer [Rect Line RectLine]]))

(deftest build-line
  (let [rect (g/build-rect 2 3 4 6)
        item (i/build-item :line rect)]
    (is (= (:input item) rect))
    (is (= (:text item) nil))
    (is (= (:cache item) {:points {(g/Point. 0 0) "\\"
                                   (g/Point. 1 1) "|"
                                   (g/Point. 1 2) "\\"
                                   (g/Point. 2 3) "|"}
                          :data "\\  \n | \n \\ \n  |"}))))

(deftest build-rect
  (let [rect (g/build-rect 2 3 4 6)
        item (i/build-item :rect rect)]
    (is (= (:input item) rect))
    (is (= (:text item) nil))
    (is (= (:cache item) {:points {(g/Point. 0 0) "+"
                                   (g/Point. 1 0) "-"
                                   (g/Point. 2 0) "+"
                                   (g/Point. 0 1) "|"
                                   (g/Point. 2 1) "|"
                                   (g/Point. 0 2) "|"
                                   (g/Point. 2 2) "|"
                                   (g/Point. 0 3) "+"
                                   (g/Point. 1 3) "-"
                                   (g/Point. 2 3) "+"}
                          :data "+-+\n| |\n| |\n+-+"}))))

(deftest build-rect-line
  (let [rect (g/build-rect 2 3 4 6)
        item (i/build-item :rect-line rect)]
    (is (= (:input item) rect))
    (is (= (:text item) nil))
    (is (= (:cache item) {:points {(g/Point. 0 0) "|"
                                   (g/Point. 0 1) "|"
                                   (g/Point. 0 2) "|"
                                   (g/Point. 0 3) "+"
                                   (g/Point. 1 3) "-"
                                   (g/Point. 2 3) "-"}
                          :data "|  \n|  \n|  \n+--"}))))

(deftest dimensions
  (let [rect (g/build-rect 2 3 4 6)
        item (i/build-item :line rect)]
    (is (= (i/dimensions item) (g/Size. 2 3)))))

(deftest origin
  (let [rect (g/build-rect 2 3 4 6)
        item (i/build-item :line rect)]
    (is (= (i/origin item) (g/Point. 2 3)))))

(deftest update
  (let [rect (g/build-rect 2 3 4 6)
        item (i/build-item :rect-line rect)
        updated-item (i/update item (g/Point. 4 7))]
    (is (= (:input updated-item) (g/build-rect 2 3 4 7)))
    (is (= (:text updated-item) nil))
    (is (= (:cache updated-item) {:points {(g/Point. 0 0) "|",
                                           (g/Point. 0 1) "|"
                                           (g/Point. 0 2) "|"
                                           (g/Point. 0 3) "|"
                                           (g/Point. 0 4) "+"
                                           (g/Point. 1 4) "-"
                                           (g/Point. 2 4) "-"}
                                  :data "|  \n|  \n|  \n|  \n+--"}))))

(deftest reposition
  (let [rect (g/build-rect 2 3 4 6)
        new-rect (g/build-rect 3 4 5 7)
        item (i/build-item :rect-line rect)
        repositioned-item (i/reposition item new-rect)]
    (is (= (:input repositioned-item) new-rect))
    (is (= (:text repositioned-item) nil))
    (is (= (:cache repositioned-item) {:points {(g/Point. 0 0) "|"
                                                (g/Point. 0 1) "|"
                                                (g/Point. 0 2) "|"
                                                (g/Point. 0 3) "+"
                                                (g/Point. 1 3) "-"
                                                (g/Point. 2 3) "-"}
                                       :data "|  \n|  \n|  \n+--"}))))

(deftest set-text
  (let [item (i/build-item :rect-line (g/build-rect 2 3 4 6))
        item-with-text (i/set-text item "bla")]
    (is (= (:text item-with-text) "bla"))))
