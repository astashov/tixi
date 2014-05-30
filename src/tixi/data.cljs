(ns tixi.data
  (:require [tixi.utils :refer [seq-contains? p]]))

(def initial-data
  {:current nil
   :completed {}
   :tool :line
   :action nil
   :autoincrement 0
   :selection {:ids []
               :rect nil
               :current nil
               :rel-rects {}}
   :hover-id nil})

(def data
  (atom initial-data))

(defn current
  ([] (current @data))
  ([data] (:current data)))

(defn completed
  ([] (completed @data))
  ([data] (:completed data)))

(defn completed-item
  ([id] (completed-item @data id))
  ([data id] (get-in data [:completed id])))

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
