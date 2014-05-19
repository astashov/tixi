(ns tixi.data
  (:require [tixi.utils :refer [seq-contains? p]]))

(def data
  (atom {:current nil
         :completed {}
         :tool :line
         :action nil
         :autoincrement 0
         :selection {:ids []
                     :edges nil
                     :current nil
                     :rel-sizes {}}
         :hover-id nil}))

(defn current []
  (:current @data))

(defn completed []
  (:completed @data))

(defn completed-item [id]
  (get-in @data [:completed id]))

(defn tool []
  (:tool @data))

(defn action []
  (:action @data))

(defn selected-ids []
  (get-in @data [:selection :ids]))

(defn selected-rel-size [id]
  (get-in @data [:selection :rel-sizes id]))

(defn selection-edges []
  (get-in @data [:selection :edges]))

(defn current-selection []
  (get-in @data [:selection :current]))

(defn hover-id []
  (:hover-id @data))

(defn draw-tool? []
  (seq-contains? [:line :rect :rect-line] (tool)))

(defn select-tool? []
  (seq-contains? [:select] (tool)))

(defn draw-action? []
  (seq-contains? [:draw] (action)))

(defn resize-action []
  (when-let [[_ result] (when (action) (re-matches #"^resize-(.+)" (name (action))))]
    (keyword result)))
