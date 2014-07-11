(ns tixi.mutators.delete
  (:require [tixi.data :as d]
            [tixi.mutators.shared :as msh]
            [tixi.mutators.selection :as mse]
            [tixi.mutators.locks :as ml]
            [tixi.mutators.undo :as mu]
            [tixi.items :as i]))

(defn delete-selected! []
  (let [ids (d/selected-ids)]
    (when (not-empty ids)
      (mu/snapshot!)
      (mse/select-layer! nil)
      (doseq [id ids]
        (ml/delete-from-locks! id (d/completed-item id))
        (msh/update-state! update-in [:completed] dissoc id)))))
