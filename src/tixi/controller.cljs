(ns tixi.controller
  (:require-macros [tixi.controller :refer [render]])
  (:require [tixi.data :as d]
            [tixi.channel :refer [channel]]
            [tixi.geometry :as g]
            [tixi.view :as v]
            [tixi.position :as p]
            [tixi.utils :refer [p]]
            [tixi.mutators :as m]))

(def ^:private request-id (atom nil))
(def ^:private select-second-clicked (atom false))

(defn -render [f]
  (m/reset-touched-items!)
  (when f (f))
  (when @request-id
    (.cancelAnimationFrame js/window @request-id))
  (let [id (.requestAnimationFrame js/window
             (fn [_]
               (reset! request-id nil)
               (m/render-items!)
               (v/render @d/data channel)))]
    (reset! request-id id)))

(defn keypress [name]
  (render
    (case name
      :select (m/set-tool! :select)
      :line (m/set-tool! :line)
      :rect-line (m/set-tool! :rect-line)
      :rect (m/set-tool! :rect)
      :text (m/set-tool! :text)
      :undo (m/undo!)
      :redo (m/redo!)
      :result (m/show-result! (not (d/show-result?)))
      :delete (m/delete-selected!))))

(defn mouse-down [client-point raw-client-point modifiers payload]
  (render
    (let [{:keys [action]} payload
          point (p/position->coords client-point)]
      (m/set-action! action)
      (m/snapshot!)
      (when (= action :draw)
        (cond
          (d/draw-tool?)
          (m/initiate-current-layer! point)

          (d/select-tool?)
          (let [id (p/item-id-at-point point raw-client-point)]
            (when (= (d/selected-ids) [id])
              (reset! select-second-clicked true))
            (m/select-layer! id point (:shift modifiers))))))))

(defn mouse-up [start-client-point client-point modifiers payload]
  (render
    (let [{:keys [action]} payload
          point (p/position->coords client-point)
          start-point (p/position->coords start-client-point)]
      (m/set-action! nil)
      (when (= action :draw)
        (cond
          (d/draw-tool?)
          (m/finish-current-layer!)

          (d/select-tool?)
          (do
            (m/finish-selection!)
            (when (and @select-second-clicked (= start-point point))
              (m/edit-text-in-item! (first (d/selected-ids))))
            (reset! select-second-clicked false)))))
      (m/undo-if-unchanged!)))

(defn mouse-drag [start-client-point previous-client-point client-point modifiers payload]
  (render
    (let [{:keys [action]} payload
          point (p/position->coords client-point)
          start-point (p/position->coords start-client-point)
          previous-point (p/position->coords previous-client-point)
          diff-point (g/sub point previous-point)]
      (cond
        (d/draw-action?)
        (cond
          (d/draw-tool?)
          (m/update-current-layer! point)

          (d/select-tool?)
          (do
            (m/update-selection! point)
            (m/move-selection! diff-point)))

        (d/resize-action)
        (m/resize-selection! diff-point (d/resize-action))))))

(defn mouse-move [previous-client-point client-point raw-client-point modifiers payload]
  (render
    (let [point (p/position->coords client-point)]
      (when (d/select-tool?)
        (m/highlight-layer! (p/item-id-at-point point raw-client-point))))))

(defn edit-text [data]
  (render
    (let [{:keys [id text dimensions]} data]
      (m/edit-text-in-item! nil)
      (m/set-text-to-item! id text dimensions))))

(defn click-toolbar [data]
  (render
    (keypress (:name data))))

(defn close [data]
  (render
    (let [{:keys [name]} data]
      (case name
        :result (m/show-result! false)))))
