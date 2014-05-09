(ns tixi.data
  (:require [tixi.utils :refer [seq-contains?]]))

(def data
  (atom {:current nil
         :completed {}
         :tool "line"
         :action nil
         :autoincrement 0}))

(defn current []
  (:current @data))

(defn completed []
  (:completed @data))

(defn tool []
  (:tool @data))

(defn action []
  (:action @data))

(defn selected-id []
  (:selected-id @data))

(defn hover-id []
  (:hover-id @data))

(defn moving-from []
  (:moving-from @data))

(defn draw-tool? []
  (seq-contains? ["line" "rect" "rect-line"] (tool)))

(defn select-tool? []
  (seq-contains? ["select"] (tool)))

(defn draw-action? []
  (seq-contains? ["draw"] (action)))

(defn resize-action []
  (if-let [[_ result] (re-matches #"^resize-(.+)" (action))]
    result))
