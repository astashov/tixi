(ns test.tixi.drawer
  (:require-macros [cemerick.cljs.test :as m :refer (is deftest)])
  (:require [cemerick.cljs.test :as test]
            [tixi.geometry :as g :refer [Rect Point Size]]
            [tixi.drawer :as d]))

(defn- rect [] (g/build-rect (Point. 2 3) (Point. 4 6)))

(deftest parse-line
  (is (= (d/parse {:cache nil :type :line :input (rect)})
         {:origin (Point. 2 3)
          :dimensions (Size. 2 3)
          :points {(Point. 0 0) "\\"
                   (Point. 1 1) "|"
                   (Point. 1 2) "\\"
                   (Point. 2 3) "|"}})))

(deftest parse-rect
  (is (= (d/parse {:cache nil :type :rect :input (rect)})
         {:origin (Point. 2 3)
          :dimensions (Size. 2 3)
          :points {(Point. 1 0) "-"
                   (Point. 2 1) "|"
                   (Point. 0 0) "+"
                   (Point. 2 2) "|"
                   (Point. 0 1) "|"
                   (Point. 0 2) "|"
                   (Point. 2 0) "+"
                   (Point. 0 3) "+"
                   (Point. 1 3) "-"
                   (Point. 2 3) "+"}})))

(deftest parse-rect-line
  (is (= (d/parse {:cache nil :type :rect-line :input (rect)})
         {:origin (Point. 2 3)
          :dimensions (Size. 2 3)
          :points {(Point. 0 0) "|"
                   (Point. 0 1) "|"
                   (Point. 0 2) "|"
                   (Point. 0 3) "+"
                   (Point. 1 3) "-"
                   (Point. 2 3) "-"}})))

(deftest render-line
  (is (= (d/render {:cache nil :type :line :input (rect)})
         {:origin (Point. 2 3)
          :dimensions (Size. 2 3)
          :points (array-map (Point. 0 0) "\\"
                             (Point. 1 1) "|"
                             (Point. 1 2) "\\"
                             (Point. 2 3) "|")
          :text "\\  \n | \n \\ \n  |"})))
