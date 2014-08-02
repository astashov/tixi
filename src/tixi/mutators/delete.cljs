(ns tixi.mutators.delete
  (:require [tixi.data :as d]
            [tixi.mutators.shared :as msh]
            [tixi.mutators.locks :as ml]
            [tixi.mutators.undo :as mu]
            [tixi.items :as i]))

(defn delete-items! [ids]
  (when (not-empty ids)
    (mu/snapshot!)
    (doseq [id ids]
      (swap! d/data update-in [:cache] dissoc id)
      (ml/delete-from-locks! id (d/completed-item id))
      (msh/update-state! update-in [:completed] dissoc id))))
