(ns test.tixi.drawer
  (:require-macros [cemerick.cljs.test :as m :refer (is deftest)])
  (:require [cemerick.cljs.test :as test]
            [tixi.drawer :as drawer]))

(deftest parse-line
  (is (= (drawer/parse {:type "line" :content [0 0 4 2]}) [[0 0] [1 0] [2 1] [3 1] [4 2]])))

(deftest parse-line-str
  (*print-fn* (pr-str (drawer/render {:type "line" :content [3 1 5 3]} 10 5)))
  (is (= (drawer/parse {:type "line" :content [0 0 4 2]}) [[0 0] [1 0] [2 1] [3 1] [4 2]])))
