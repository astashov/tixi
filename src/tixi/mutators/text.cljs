(ns tixi.mutators.text
  (:require [tixi.data :as d]
            [tixi.mutators.shared :as ms]
            [tixi.items :as i]))

(defn edit-text-in-item! [id]
  (swap! d/data assoc :edit-text-id id))

(defn set-text-to-item!
  ([id text]
    (set-text-to-item! id text nil))
  ([id text dimensions]
    (ms/update-state! assoc-in [:completed id] (i/set-text (d/completed-item id) text dimensions))))
