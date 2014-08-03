(ns tixi.mutators.shared
  (:require [tixi.data :as d]
            [clojure.zip :as z]
            [tixi.sync :as s]
            [tixi.tree :as t]))

(defn assign-state! [new-value]
  (swap! d/data assoc-in [:state] new-value)
  (swap! d/data assoc-in [:stack] (z/replace (d/stack-loc)
                                             (t/update (z/node (d/stack-loc))
                                                       new-value)))
  (s/sync!))

(defn update-state! [f ks & args]
  (assign-state! (apply f (d/state) ks args)))

(defn snapshot! []
  (swap! d/data assoc-in [:stack] (-> (d/stack-loc)
                                      (z/insert-child (t/node (d/stack)))
                                      z/down))) 
(defn autoincrement! []
  (update-state! update-in [:autoincrement] inc))
 
