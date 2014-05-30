(ns test.tixi.geometry
  (:require-macros [cemerick.cljs.test :as m :refer (is deftest)])
  (:require [cemerick.cljs.test :as test]
            [tixi.utils :refer [p]]
            [tixi.geometry :as g :refer [Rect Point Size]]))

(defn- rect [] (g/build-rect (Point. 4 5) (Point. 1 3)))

(deftest rect-expand
  (is (= (g/expand (rect) (Point. 6 7))
         (g/build-rect (:start-point (rect)) (Point. 6 7)))))

(deftest rect-width
  (is (= (g/width (rect)) 3)))

(deftest rect-height
  (is (= (g/height (rect)) 2)))

(deftest rect-normalize
  (is (= (g/normalize (rect)) (g/build-rect (Point. 1 3) (Point. 4 5))))
  (is (= (g/normalize (g/build-rect (Point. 1 5) (Point. 4 3))) (g/build-rect (Point. 1 3) (Point. 4 5))))
  (is (= (g/normalize (g/build-rect (Point. 4 3) (Point. 1 5))) (g/build-rect (Point. 1 3) (Point. 4 5))))
  (is (= (g/normalize (g/build-rect (Point. 1 3) (Point. 4 5))) (g/build-rect (Point. 1 3) (Point. 4 5)))))

(deftest rect-origin
  (is (= (g/origin (rect)) (Point. 1 3))))

(deftest rect-shifted-to-0
  (is (= (g/shifted-to-0 (rect)) (g/build-rect (Point. 3 2) (Point. 0 0)))))

(deftest rect-vec
  (is (= (g/values (rect)) [4 5 1 3])))

(deftest rect-inside?
  (is (= (g/inside? (rect) (Point. 2 4)) true))
  (is (= (g/inside? (rect) (Point. 1 1)) false)))

(deftest rect-relative
  (is (= (g/relative (Rect. (Point. 2 2) (Point. 4 4))
                     (Rect. (Point. 1 1) (Point. 6 6)))
         (Rect. (Point. 0.2 0.2) (Point. 0.6 0.6)))))

(deftest rect-absolute
  (is (= (g/absolute (Rect. (Point. 1 1) (Point. 6 6))
                     (Rect. (Point. 0.2 0.2) (Point. 0.6 0.6)))
         (Rect. (Point. 2 2) (Point. 4 4))))
  (is (= (g/absolute (Rect. (Point. 6 6) (Point. 1 1))
                     (Rect. (Point. 0.2 0.2) (Point. 0.6 0.6)))
         (Rect. (Point. 5 5) (Point. 3 3)))))

(deftest wrapping-rect
  (is (= (g/wrapping-rect [(g/build-rect (Point. 1 3) (Point. 4 6))
                           (g/build-rect (Point. 2 5) (Point. 5 7))
                           (g/build-rect (Point. 3 1) (Point. 5 3))])
         (g/build-rect (Point. 1 1) (Point. 5 7)))))

(deftest move-rect
  (is (= (g/move (g/build-rect (Point. 3 4) (Point. 5 6)) (Size. 3 5))
         (g/build-rect (Point. 6 9) (Point. 8 11)))))

(deftest resize-rect-nw
  (is (= (g/resize (g/build-rect (Point. 5 4) (Point. 5 6)) (Size. 3 5) :nw) (g/build-rect (Point. 8 9) (Point. 5 6))))
  (is (= (g/resize (g/build-rect (Point. 5 4) (Point. 5 6)) (Size. 3 5) :n) (g/build-rect (Point. 5 9) (Point. 5 6))))
  (is (= (g/resize (g/build-rect (Point. 5 4) (Point. 5 6)) (Size. 3 5) :ne) (g/build-rect (Point. 5 9) (Point. 8 6))))
  (is (= (g/resize (g/build-rect (Point. 5 4) (Point. 5 6)) (Size. 3 5) :w) (g/build-rect (Point. 8 4) (Point. 5 6))))
  (is (= (g/resize (g/build-rect (Point. 5 4) (Point. 5 6)) (Size. 3 5) :e) (g/build-rect (Point. 5 4) (Point. 8 6))))
  (is (= (g/resize (g/build-rect (Point. 5 4) (Point. 5 6)) (Size. 3 5) :sw) (g/build-rect (Point. 8 4) (Point. 5 11))))
  (is (= (g/resize (g/build-rect (Point. 5 4) (Point. 5 6)) (Size. 3 5) :s) (g/build-rect (Point. 5 4) (Point. 5 11))))
  (is (= (g/resize (g/build-rect (Point. 5 4) (Point. 5 6)) (Size. 3 5) :se) (g/build-rect (Point. 5 4) (Point. 8 11)))))

(deftest point-vec
  (is (= (g/values (Point. 1 2)) [1 2])))

(deftest point-add
  (is (= (g/add (Point. 1 2) (Point. 3 4)) (Point. 4 6))))

(deftest point-sub
  (is (= (g/sub (Point. 3 4) (Point. 1 2)) (Point. 2 2))))
