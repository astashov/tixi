(ns test.tixi.tree
  (:require-macros [cemerick.cljs.test :as m :refer (is deftest)])
  (:require [cemerick.cljs.test :as test]
            [tixi.utils :refer [p]]
            [clojure.zip :as zip]
            [tixi.tree :as t :refer [Node]]))

(deftest build-node
  (is (= (t/node 1) (Node. 1 [])))
  (is (= (t/node 1 2) (Node. 1 2))))

(deftest node-update
  (is (= (t/update (t/node 1 2) 3) (Node. 3 2))))
