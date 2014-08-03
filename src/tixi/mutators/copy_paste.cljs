(ns tixi.mutators.copy-paste
  (:require [tixi.data :as d]
            [tixi.items :as i]
            [tixi.geometry :as g]
            [tixi.mutators :as m]
            [tixi.mutators.shared :as msh]
            [tixi.mutators.selection :as ms]
            [tixi.mutators.render :as mr]
            [tixi.mutators.locks :as ml]
            [tixi.mutators.delete :as md]
            [tixi.utils :refer [p]]))

(defn copy! []
  (let [items (d/selected-items)]
    (when (not-empty items)
      (swap! d/data assoc :clipboard items))))

(defn cut! []
  (let [ids (d/selected-ids)]
    (copy!)
    (ms/select-layer! nil)
    (md/delete-items! ids)))

(defn paste! []
  (msh/snapshot!)
  (ms/select-layer! nil)
  (doseq [item (d/clipboard)]
    (let [id (d/autoincrement)]
      (msh/autoincrement!)
      (msh/update-state! assoc-in [:completed id] (update-in item [:input] g/move (g/build-size 1 1)))
      (mr/render-items!)
      (let [new-item (d/completed-item id)]
        (when (i/connector? new-item)
          (ml/try-to-lock! new-item id :start (-> new-item :input :start))
          (ml/try-to-lock! new-item id :end (-> new-item :input :end)))
        (ms/select-layer! id nil true)))))

