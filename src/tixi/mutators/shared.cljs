(ns tixi.mutators.shared
  (:require [tixi.data :as d]
            [clojure.zip :as z]
            [tixi.tree :as t]))

(defn autoincrement! []
  (swap! d/data update-in [:autoincrement] inc))

(defn update-state! [f ks & args]
  (let [new-value (apply f (d/state) ks args)]
    (swap! d/data assoc-in [:state] new-value)
    (swap! d/data assoc-in [:stack] (z/replace (d/stack-loc)
                                               (t/update (z/node (d/stack-loc))
                                                         new-value)))))
