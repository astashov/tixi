(ns tixi.mutators.undo
  (:require [tixi.data :as d]
            [clojure.zip :as z]
            [tixi.mutators.render :as mr]
            [tixi.tree :as t]
            [tixi.utils :refer [p]]))

(defn snapshot! []
  (swap! d/data assoc-in [:state] (-> (d/state-loc)
                                      (z/insert-child (t/node (d/state)))
                                      z/down)))

(defn- can-undo? []
  (boolean (z/up (d/state-loc))))

(defn- can-redo? []
  (boolean (z/node (z/down (d/state-loc)))))

(defn undo! []
  (when (can-undo?)
    (swap! d/data assoc-in [:state] (z/up (d/state-loc)))
    (doseq [[id _] (d/completed)]
      (mr/touch-item! id))))

(defn redo! []
  (when (can-redo?)
    (swap! d/data assoc-in [:state] (z/down (d/state-loc)))
    (doseq [[id _] (d/completed)]
      (mr/touch-item! id))))

(defn undo-if-unchanged! []
  (when (and (z/up (d/state-loc))
             (= (d/state)
                (:value (z/node (z/up (d/state-loc))))))
    (undo!)))
