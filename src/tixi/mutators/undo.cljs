(ns tixi.mutators.undo
  (:require [tixi.data :as d]
            [clojure.zip :as z]
            [tixi.mutators.render :as mr]
            [tixi.mutators.selection :as ms]
            [tixi.tree :as t]
            [tixi.utils :refer [p]]))

(defn undo! []
  (when (d/can-undo?)
    (swap! d/data assoc-in [:stack] (z/up (d/stack-loc)))
    (swap! d/data assoc-in [:state] (d/stack))
    (doseq [[id _] (d/completed)]
      (mr/touch-item! id))
    (ms/refresh-selection!)))

(defn redo! []
  (when (d/can-redo?)
    (swap! d/data assoc-in [:stack] (z/down (d/stack-loc)))
    (swap! d/data assoc-in [:state] (d/stack))
    (doseq [[id _] (d/completed)]
      (mr/touch-item! id))
    (ms/refresh-selection!)))

(defn undo-if-unchanged! []
  (when (and (z/up (d/stack-loc))
             (= (d/stack)
                (:value (z/node (z/up (d/stack-loc))))))
    (undo!)))
