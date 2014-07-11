(ns tixi.controller
  (:require-macros [tixi.controller :refer [render]])
  (:require [tixi.data :as d]
            [tixi.channel :refer [channel]]
            [tixi.geometry :as g]
            [tixi.view :as v]
            [tixi.position :as p]
            [tixi.utils :refer [p]]
            [tixi.mutators.current :as mc]
            [tixi.mutators.delete :as md]
            [tixi.mutators.render :as mr]
            [tixi.mutators.selection :as ms]
            [tixi.mutators.text :as mt]
            [tixi.mutators.undo :as mu]
            [tixi.mutators :as m]))

(def ^:private request-id (atom nil))
(def ^:private select-second-clicked (atom false))

(defn -render [f]
  (mr/reset-touched-items!)
  (when f (f))
  (when @request-id
    (.cancelAnimationFrame js/window @request-id))
  (let [id (.requestAnimationFrame js/window
             (fn [_]
               (reset! request-id nil)
               (mr/render-items!)
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
      :undo (mu/undo!)
      :redo (mu/redo!)
      :result (m/show-result! (not (d/show-result?)))
      :delete (md/delete-selected!))))

(defn mouse-down [client-point raw-client-point modifiers payload]
  (render
    (let [{:keys [action]} payload
          point (p/position->coords client-point)]
      (m/set-action! action)
      (mu/snapshot!)
      (when (= action :draw)
        (cond
          (d/draw-tool?)
          (mc/initiate-current-layer! point)

          (d/select-tool?)
          (let [id (p/item-id-at-point point raw-client-point)]
            (when (= (d/selected-ids) [id])
              (reset! select-second-clicked true))
            (ms/select-layer! id point (:shift modifiers))))))))

(defn mouse-up [start-client-point client-point modifiers payload]
  (render
    (let [{:keys [action]} payload
          point (p/position->coords client-point)
          start-point (p/position->coords start-client-point)]
      (m/set-action! nil)
      (when (= action :draw)
        (cond
          (d/draw-tool?)
          (mc/finish-current-layer!)

          (d/select-tool?)
          (do
            (ms/finish-selection!)
            (when (and @select-second-clicked (= start-point point))
              (mt/edit-text-in-item! (first (d/selected-ids))))
            (reset! select-second-clicked false)))))
      (mu/undo-if-unchanged!)))

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
          (mc/update-current-layer! point)

          (d/select-tool?)
          (do
            (ms/update-selection! point)
            (ms/move-selection! diff-point)))

        (d/resize-action)
        (ms/resize-selection! diff-point (d/resize-action))))))

(defn mouse-move [previous-client-point client-point raw-client-point modifiers payload]
  (render
    (let [point (p/position->coords client-point)]
      (when (d/select-tool?)
        (ms/highlight-layer! (p/item-id-at-point point raw-client-point))))))

(defn edit-text [data]
  (render
    (let [{:keys [id text dimensions]} data]
      (mt/edit-text-in-item! nil)
      (mt/set-text-to-item! id text dimensions))))

(defn click-toolbar [data]
  (render
    (keypress (:name data))))

(defn close [data]
  (render
    (let [{:keys [name]} data]
      (case name
        :result (m/show-result! false)))))
