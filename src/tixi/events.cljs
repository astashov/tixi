(ns tixi.events
  (:require [dommy.core :as dommy]
            [tixi.view :as v]
            [tixi.channel :refer [channel]]
            [tixi.data :as d]
            [tixi.geometry :as g :refer [Size Rect Point]]
            [tixi.position :as p]
            [tixi.utils :refer [p]]
            [tixi.mutators :as m]))

(def ^:private request-id (atom nil))
(def ^:private start-point (atom (Point. 0 0)))
(def ^:private end-point (atom (Point. 0 0)))
(def ^:private moving-from (atom (Point. 0 0)))
(def ^:private select-second-clicked (atom nil))

(defn reset-data! []
  (reset! request-id nil)
  (reset! moving-from (Point. 0 0)))

(defn- set-moving-from! [point]
  (reset! moving-from point))

(defn render []
  (when @request-id
    (.cancelAnimationFrame js/window @request-id))
  (let [id (.requestAnimationFrame js/window
             (fn [_]
               (reset! request-id nil)
               (v/render @d/data channel)))]
    (reset! request-id id)))

(defn handle-keyboard-events [event]
  (when-not (d/edit-text-id)
    (case (.-keyCode event)
          8  (do ; backspace
               (.preventDefault event)
               (m/delete-selected!))
          76 (m/set-tool! :line) ; l
          81 (*print-fn* (d/result))
          82 (m/set-tool! :rect) ; r
          83 (m/set-tool! :select) ; s
          84 (m/set-tool! :text) ; t
          89 (m/set-tool! :rect-line) ; y
          85 (m/undo!) ; u
          73 (m/redo!) ; i
          nil)
    (render)))

(defn handle-mousemove [event]
  (reset! select-second-clicked false)
  (when-let [point (p/event->coords event)]
    (let [previous-point @moving-from
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
        (m/resize-selection! diff-point (d/resize-action))

        :else
        (when (d/select-tool?)
          (let [client-point (Point. (.-clientX event) (.-clientY event))]
            (m/highlight-layer! (p/item-id-at-point point client-point))))))

    (set-moving-from! point)
    (render)))

(defn handle-input-event [{:keys [type data]}]
  (case type
    :down
    (let [{:keys [point action event id]} data]
      (reset! start-point point)
      (set-moving-from! point)
      (m/set-action! action)
      (m/snapshot!)
      (when (= action :draw)
        (cond
          (d/draw-tool?)
          (m/initiate-current-layer! point)

          (d/select-tool?)
          (let [client-point (Point. (.-clientX event) (.-clientY event))
                id (p/item-id-at-point point client-point)]
            (when (= (d/selected-ids) [id])
              (reset! select-second-clicked true))
            (m/select-layer! id point (.-shiftKey event))))))

    :up
    (let [{:keys [point action]} data]
      (reset! end-point point)
      (m/set-action! nil)
      (when (= action :draw)
        (cond
          (d/draw-tool?)
          (m/finish-current-layer!)

          (d/select-tool?)
          (do
            (m/finish-selection!)
            (when (and @select-second-clicked (= @start-point @end-point))
              (m/edit-text-in-item! (first (d/selected-ids))))
            (reset! select-second-clicked false))))
      (m/undo-if-unchanged!))

    :edit
    (let [{:keys [id text dimensions]} data]
      (m/edit-text-in-item! nil)
      (m/set-text-to-item! id text dimensions)))

  (render))

(defn install-keyboard-events []
  (dommy/listen! js/document :keydown handle-keyboard-events))

(defn install-mouse-events []
  (dommy/listen!  js/document :mousemove handle-mousemove))
