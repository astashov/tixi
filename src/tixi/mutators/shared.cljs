(ns tixi.mutators.shared
  (:require [tixi.data :as d]
            [clojure.zip :as z]
            [tixi.tree :as t]))

(defn- update-state! [f ks & args]
  (swap! d/data assoc-in [:state] (z/replace (d/state-loc)
                                             (t/update (z/node (d/state-loc))
                                                       (apply f (d/state) ks args)))))
