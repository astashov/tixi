(ns tixi.controller
  (:require-macros [tixi.controller :refer [render]]
                   [tixi.utils :refer [b]])
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
            [tixi.mutators.shared :as msh]
            [tixi.mutators.text :as mt]
            [tixi.mutators.undo :as mu]
            [tixi.mutators.copy-paste :as mcp]
            [tixi.mutators :as m]
            [tixi.google-analytics :as ga]))

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
      :select (do
                (ga/event! "toolbar" "select")
                (m/set-tool! :select))
      :line (do
              (ga/event! "toolbar" "line")
              (m/set-tool! :line))
      :rect-line (do
                   (ga/event! "toolbar" "rect-line")
                   (m/set-tool! :rect-line))
      :rect (do
              (ga/event! "toolbar" "rect")
              (m/set-tool! :rect))
      :text (do
              (ga/event! "toolbar" "text")
              (m/set-tool! :text))
      :undo (do
              (ga/event! "toolbar" "undo")
              (mu/undo!))
      :redo (do
              (ga/event! "toolbar" "redo")
              (mu/redo!))
      :result (do
                (ga/event! "toolbar" "result")
                (m/show-result! (not (d/show-result?))))
      :delete (do
                (ga/event! "toolbar" "delete")
                (md/delete-items! (d/selected-ids))
                (ms/select-layer! nil))
      :z-inc (do
               (ga/event! "topbar" "z-inc")
               (m/z-inc! (d/selected-ids)))
      :z-dec (do
               (ga/event! "topbar" "z-dec")
               (m/z-dec! (d/selected-ids)))
      :z-show (do
                (ga/event! "topbar" "z-show")
                (m/z-show! (not (d/show-z-indexes?))))
      :copy (do
              (ga/event! "topbar" "copy")
              (mcp/copy!))
      :cut (do
             (ga/event! "topbar" "cut")
             (mcp/cut!))
      :paste (do
               (ga/event! "topbar" "paste")
               (mcp/paste!))
      :grid (do
              (ga/event! "topbar" "grid")
              (m/toggle-grid! (not (d/show-grid?))))
      :move-up (do
                 (ga/event! "keypress" "move-up")
                 (msh/snapshot!)
                 (ms/move-selection! (g/build-point 0 -1)))
      :move-down (do
                   (ga/event! "keypress" "move-down")
                   (msh/snapshot!)
                   (ms/move-selection! (g/build-point 0 1)))
      :move-left (do
                   (ga/event! "keypress" "move-left")
                   (msh/snapshot!)
                   (ms/move-selection! (g/build-point -1 0)))
      :move-right (do
                    (ga/event! "keypress" "move-right")
                    (msh/snapshot!)
                    (ms/move-selection! (g/build-point 1 0))))))

(defn mouse-down [client-point modifiers payload]
  (render
    (let [{:keys [action]} payload
          point (p/position->coords client-point)]
      (m/set-action! action)
      (msh/snapshot!)
      (when (= action :draw)
        (cond
          (d/draw-tool?)
          (mc/initiate-current-layer! point)

          (d/select-tool?)
          (let [id (p/item-id-at-point point)]
            (when (= (d/selected-ids) #{id})
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
          (do
            (ga/event! "draw" (str "finish-" (-> (d/current) :item :type name)))
            (mc/finish-current-layer!))

          (d/select-tool?)
          (do
            (ms/finish-selection!)
            (when (and @select-second-clicked (= start-point point))
              (mt/edit-text-in-item! (first (d/selected-ids))))
            (reset! select-second-clicked false)))))
      (m/set-connecting-id! nil)
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

(defn mouse-move [previous-client-point client-point modifiers payload]
  (render
    (let [point (p/position->coords client-point)]
      (when (d/select-tool?)
        (ms/highlight-layer! (p/item-id-at-point point))))))

(defn edit-text [data]
  (render
    (ga/event! "draw" "edit-text")
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

(defn change-line-edge [data]
  (render
    (ga/event! "topbar" "change-line-edge")
    (let [{:keys [edge]} data]
      (m/cycle-line-edge! edge))))

(defn change-selection-edges [data]
  (render
    (ga/event! "topbar" "change-selection-edges")
    (let [{:keys [edge]} data]
      (m/cycle-selection-edges! edge))))
