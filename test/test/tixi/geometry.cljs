(ns test.tixi.geometry
  (:require-macros [cemerick.cljs.test :as m :refer (is deftest)])
  (:require [cemerick.cljs.test :as test]
            [tixi.utils :refer [p]]
            [tixi.geometry :as g]))

(defn- rect [] (g/build-rect (g/build-point 4 5) (g/build-point 1 3)))

(deftest rect-expand
  (is (= (g/expand (rect) (g/build-point 6 7))
         (g/build-rect (:start (rect)) (g/build-point 6 7)))))

(deftest rect-width
  (is (= (g/width (rect)) 3)))

(deftest rect-height
  (is (= (g/height (rect)) 2)))

(deftest rect-normalize
  (is (= (g/normalize (rect)) (g/build-rect (g/build-point 1 3) (g/build-point 4 5))))
  (is (= (g/normalize (g/build-rect (g/build-point 1 5) (g/build-point 4 3))) (g/build-rect (g/build-point 1 3) (g/build-point 4 5))))
  (is (= (g/normalize (g/build-rect (g/build-point 4 3) (g/build-point 1 5))) (g/build-rect (g/build-point 1 3) (g/build-point 4 5))))
  (is (= (g/normalize (g/build-rect (g/build-point 1 3) (g/build-point 4 5))) (g/build-rect (g/build-point 1 3) (g/build-point 4 5)))))

(deftest rect-origin
  (is (= (g/origin (rect)) (g/build-point 1 3))))

(deftest rect-shifted-to-0
  (is (= (g/shifted-to-0 (rect)) (g/build-rect (g/build-point 3 2) (g/build-point 0 0)))))

(deftest rect-vec
  (is (= (g/values (rect)) [4 5 1 3])))

(deftest rect-inside?
  (is (= (g/inside? (rect) (g/build-point 2 4)) true))
  (is (= (g/inside? (rect) (g/build-point 1 1)) false)))

(deftest rect-relative
  (is (= (g/relative (g/build-rect (g/build-point 2 2) (g/build-point 4 4))
                     (g/build-rect (g/build-point 1 1) (g/build-point 6 6)))
         (g/build-rect (g/build-point 0.2 0.2) (g/build-point 0.6 0.6))))
  (is (= (g/relative (g/build-rect (g/build-point 2 2) (g/build-point 4 2))
                     (g/build-rect (g/build-point 1 2) (g/build-point 6 2)))
         (g/build-rect (g/build-point 0.2 0) (g/build-point 0.6 1))))
  (is (= (g/relative (g/build-rect (g/build-point 4 4) (g/build-point 2 2))
                     (g/build-rect (g/build-point 6 6) (g/build-point 1 1)))
         (g/build-rect (g/build-point 0.2 0.2) (g/build-point 0.6 0.6)))))

(deftest rect-absolute
  (is (= (g/absolute (g/build-rect (g/build-point 1 1) (g/build-point 6 6))
                     (g/build-rect (g/build-point 0.2 0.2) (g/build-point 0.6 0.6)))
         (g/build-rect (g/build-point 2 2) (g/build-point 4 4))))
  (is (= (g/absolute (g/build-rect (g/build-point 6 6) (g/build-point 1 1))
                     (g/build-rect (g/build-point 0.2 0.2) (g/build-point 0.6 0.6)))
         (g/build-rect (g/build-point 5 5) (g/build-point 3 3)))))

(deftest wrapping-rect
  (is (= (g/wrapping-rect [(g/build-rect (g/build-point 1 3) (g/build-point 4 6))
                           (g/build-rect (g/build-point 2 5) (g/build-point 5 7))
                           (g/build-rect (g/build-point 3 1) (g/build-point 5 3))])
         (g/build-rect (g/build-point 1 1) (g/build-point 5 7)))))

(deftest move-rect
  (is (= (g/move (g/build-rect (g/build-point 3 4) (g/build-point 5 6)) (g/build-size 3 5))
         (g/build-rect (g/build-point 6 9) (g/build-point 8 11)))))

(deftest resize-rect-nw
  (is (= (g/resize (g/build-rect (g/build-point 5 4) (g/build-point 5 6)) (g/build-size 3 5) :nw) (g/build-rect (g/build-point 8 9) (g/build-point 5 6))))
  (is (= (g/resize (g/build-rect (g/build-point 5 4) (g/build-point 5 6)) (g/build-size 3 5) :n) (g/build-rect (g/build-point 5 9) (g/build-point 5 6))))
  (is (= (g/resize (g/build-rect (g/build-point 5 4) (g/build-point 5 6)) (g/build-size 3 5) :ne) (g/build-rect (g/build-point 5 9) (g/build-point 8 6))))
  (is (= (g/resize (g/build-rect (g/build-point 5 4) (g/build-point 5 6)) (g/build-size 3 5) :w) (g/build-rect (g/build-point 8 4) (g/build-point 5 6))))
  (is (= (g/resize (g/build-rect (g/build-point 5 4) (g/build-point 5 6)) (g/build-size 3 5) :e) (g/build-rect (g/build-point 5 4) (g/build-point 8 6))))
  (is (= (g/resize (g/build-rect (g/build-point 5 4) (g/build-point 5 6)) (g/build-size 3 5) :sw) (g/build-rect (g/build-point 8 4) (g/build-point 5 11))))
  (is (= (g/resize (g/build-rect (g/build-point 5 4) (g/build-point 5 6)) (g/build-size 3 5) :s) (g/build-rect (g/build-point 5 4) (g/build-point 5 11))))
  (is (= (g/resize (g/build-rect (g/build-point 5 4) (g/build-point 5 6)) (g/build-size 3 5) :se) (g/build-rect (g/build-point 5 4) (g/build-point 8 11)))))

(deftest point-vec
  (is (= (g/values (g/build-point 1 2)) [1 2])))

(deftest point-add
  (is (= (g/add (g/build-point 1 2) (g/build-point 3 4)) (g/build-point 4 6))))

(deftest point-sub
  (is (= (g/sub (g/build-point 3 4) (g/build-point 1 2)) (g/build-point 2 2))))

(deftest point-incr
  (is (= (g/incr (g/build-point 3 4)) (g/build-point 4 5))))

(deftest size-incr
  (is (= (g/incr (g/build-size 3 4)) (g/build-size 4 5))))

(deftest point-decr
  (is (= (g/decr (g/build-point 3 4)) (g/build-point 2 3))))

(deftest point-relative
  (is (= (g/relative (g/build-point 2 2)
                     (g/build-rect (g/build-point 1 1) (g/build-point 6 6)))
         (g/build-point 0.2 0.2)))
  (is (= (g/relative (g/build-point 2 2)
                     (g/build-rect (g/build-point 1 2) (g/build-point 6 2)))
         (g/build-point 0.2 0)))
  (is (= (g/relative (g/build-point 5 5)
                     (g/build-rect (g/build-point 6 6) (g/build-point 1 1)))
         (g/build-point 0.2 0.2))))

(deftest point-absolute
  (is (= (g/absolute (g/build-point 0.2 0.2)
                     (g/build-rect (g/build-point 1 1) (g/build-point 6 6)))
         (g/build-point 2 2)))
  (is (= (g/absolute (g/build-point 0.2 0.2)
                     (g/build-rect (g/build-point 6 6) (g/build-point 1 1)))
         (g/build-point 5 5))))

(deftest point-move
  (is (= (g/move (g/build-point 3 4) (g/build-size 3 5))
         (g/build-point 6 9))))

(deftest size-decr
  (is (= (g/decr (g/build-size 3 4)) (g/build-size 2 3))))

(deftest size-aspect-ratio
  (is (= (g/aspect-ratio (g/build-size 1 2)) 0.5)))

(deftest size-portrait?
  (is (= (g/portrait? (g/build-size 1 2)) true))
  (is (= (g/portrait? (g/build-size 2 1)) false)))

(deftest size-landscape?
  (is (= (g/landscape? (g/build-size 2 1)) true))
  (is (= (g/landscape? (g/build-size 1 2)) false)))

(deftest size-square?
  (is (= (g/square? (g/build-size 1 1)) true))
  (is (= (g/square? (g/build-size 2 1)) false)))
