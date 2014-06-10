(ns tixi.data
  (:require [tixi.utils :refer [seq-contains? p]]
            [tixi.tree :as t :refer [Node]]
            [clojure.zip :as z]))

(defn zip [root]
  (z/zipper
    (fn [_] true)
    #(:children %)
    #(assoc %1 :children %2)
    root))

(def initial-data
  {:current nil
   :state (zip (t/node {:completed {}}))
   :tool :line
   :action nil
   :autoincrement 0
   :selection {:ids []
               :rect nil
               :current nil
               :rel-rects {}}
   :edit-text-id nil
   :hover-id nil})

(def data
  (atom initial-data))

(defn current
  ([] (current @data))
  ([data] (:current data)))

(defn state-loc
  ([] (state-loc @data))
  ([data] (:state data)))

(defn state
  ([] (state @data))
  ([data] (:value (z/node (state-loc data)))))

(defn completed
  ([] (completed @data))
  ([data] (:completed (state data))))

(defn completed-item
  ([id] (completed-item @data id))
  ([data id] (get-in (state data) [:completed id])))

(defn tool
  ([] (tool @data))
  ([data] (:tool data)))

(defn action
  ([] (action @data))
  ([data] (:action data)))

(defn selection
  ([] (selection @data))
  ([data] (:selection data)))

(defn selected-ids
  ([] (selected-ids @data))
  ([data] (get-in data [:selection :ids])))

(defn selected-rel-rect
  ([id] (selected-rel-rect @data id))
  ([data id] (get-in data [:selection :rel-rects id])))

(defn selection-rect
  ([] (selection-rect @data))
  ([data] (get-in data [:selection :rect])))

(defn current-selection
  ([] (current-selection @data))
  ([data] (get-in data [:selection :current])))

(defn hover-id
  ([] (hover-id @data))
  ([data] (:hover-id data)))

(defn draw-tool?
  ([] (draw-tool? @data))
  ([data] (seq-contains? [:line :rect :rect-line] (tool data))))

(defn select-tool?
  ([] (select-tool? @data))
  ([data] (seq-contains? [:select] (tool data))))

(defn draw-action?
  ([] (draw-action? @data))
  ([data] (seq-contains? [:draw] (action data))))

(defn resize-action
  ([] (resize-action @data))
  ([data] (when-let [[_ result] (when (action data) (re-matches #"^resize-(.+)" (name (action data))))]
    (keyword result))))

(defn edit-text-id
  ([] (edit-text-id @data))
  ([data] (:edit-text-id data)))
