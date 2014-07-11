(ns tixi.mutators.locks
  (:require [tixi.data :as d]
            [tixi.geometry :as g]
            [tixi.mutators.shared :as ms]
            [tixi.items :as i]
            [tixi.position :as p]
            [tixi.utils :refer [p get-by-val]]))

(defn remove-lock! [connector-id connector-item type]
  (let [lockable-id (d/lockable-id-by-connector-id-and-type connector-id type)
        lockable-item (d/completed-item lockable-id)
        lockable-connector-data (d/lockable-connector lockable-id connector-id)]
    (if (contains? (:types lockable-connector-data) type)
      (let [input (:input connector-item)
            rect (get lockable-connector-data :rect input)
            new-types (into #{} (disj (or (:types lockable-connector-data) #{}) type))
            new-rect ((if (= type :end) g/expand g/shrink) rect
                                                           ((if (= type :end) :end-point :start-point) input))]
        (if (empty? new-types)
          (ms/update-state! update-in [:locks :lockables lockable-id] dissoc connector-id)
          (ms/update-state! assoc-in [:locks :lockables lockable-id connector-id] {:types new-types :rect new-rect}))
        (ms/update-state! update-in [:locks :connectors connector-id] dissoc type)

        (when (empty? (d/connector-types connector-id))
          (ms/update-state! update-in [:locks :connectors] dissoc connector-id))
        (when (empty? (d/lockable lockable-id))
          (ms/update-state! update-in [:locks :lockables] dissoc lockable-id))
        (if (= type :end)
          (dissoc connector-item :end-char)
          (dissoc connector-item :start-char)))
      connector-item)))

(defn- delete-connector-from-locks! [connector-id]
  (let [lockable-ids (vals (d/connector-types connector-id))]
    (doseq [lockable-id lockable-ids]
      (ms/update-state! update-in [:locks :lockables lockable-id] dissoc connector-id)
      (when (empty? (d/lockable lockable-id))
        (ms/update-state! update-in [:locks :lockables] dissoc lockable-id)))
    (ms/update-state! update-in [:locks :connectors] dissoc connector-id)))

(defn- delete-lockable-from-locks! [lockable-id]
  (let [connector-ids (keys (d/lockable lockable-id))]
    (doseq [connector-id connector-ids]
      (let [types (get-by-val (d/connector-types connector-id) lockable-id)]
        (doseq [type types]
          (let [new-item (remove-lock! connector-id (d/completed-item connector-id) type)]
            (ms/update-state! assoc-in [:completed connector-id] new-item)))))
    (ms/update-state! update-in [:locks :lockables] dissoc lockable-id)))

(defn delete-from-locks! [id item]
  (cond
    (i/lockable? item) (delete-lockable-from-locks! id)
    (i/connector? item) (delete-connector-from-locks! id)))

(defn add-lock! [connector-id connector-item lockable-id lockable-item point type]
  (let [lockable-connector-data (d/lockable-connector lockable-id connector-id)
        input (:input connector-item)
        rect (or (:rect lockable-connector-data) input)
        new-types (into #{} (conj (or (:types lockable-connector-data) #{}) type))
        new-rect ((if (= type :end) g/expand g/shrink) rect (i/relative-point lockable-item point))]
    (ms/update-state! assoc-in [:locks :lockables lockable-id connector-id] {:types new-types :rect new-rect})
    (ms/update-state! assoc-in [:locks :connectors connector-id type] lockable-id)
    (if (= type :end)
      (assoc connector-item :end-char "+")
      (assoc connector-item :start-char "+"))))

(defn try-to-lock! [connector-item connector-id point type]
  (if (i/connector? connector-item)
    (let [[lockable-id lockable-item] (->> (p/items-at-point point)
                                           (filter (fn [[id _]] (not= id connector-id)))
                                           first)]
      (if (and lockable-item (i/lockable? lockable-item) (i/connector? connector-item))
        (add-lock! connector-id connector-item lockable-id lockable-item point type)
        (remove-lock! connector-id connector-item type)))
    connector-item))
