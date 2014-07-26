(ns tixi.mutators.undo
  (:require [tixi.data :as d]
            [clojure.zip :as z]
            [tixi.mutators.render :as mr]
            [tixi.tree :as t]
            [tixi.utils :refer [p]]))

(defn snapshot! []
  (swap! d/data assoc-in [:stack] (-> (d/stack-loc)
                                      (z/insert-child (t/node (d/stack)))
                                      z/down)))

(defn- can-undo? []
  (boolean (z/up (d/stack-loc))))

(defn- can-redo? []
  (boolean (z/node (z/down (d/stack-loc)))))

(defn undo! []
  (when (can-undo?)
    (swap! d/data assoc-in [:stack] (z/up (d/stack-loc)))
    (swap! d/data assoc-in [:state] (d/stack))
    (doseq [[id _] (d/completed)]
      (mr/touch-item! id))))

(defn redo! []
  (when (can-redo?)
    (swap! d/data assoc-in [:stack] (z/down (d/stack-loc)))
    (swap! d/data assoc-in [:state] (d/stack))
    (doseq [[id _] (d/completed)]
      (mr/touch-item! id))))

(defn undo-if-unchanged! []
  (when (and (z/up (d/stack-loc))
             (= (d/stack)
                (:value (z/node (z/up (d/stack-loc))))))
    (undo!)))
