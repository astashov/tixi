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
(def ^:private moving-from (atom (Point. 0 0)))

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
  (case (.-keyCode event)
        8  (do ; backspace
             (.preventDefault event)
             (m/delete-selected!)
             (render))
        76 (m/set-tool! :line) ; l
        82 (m/set-tool! :rect) ; r
        83 (m/set-tool! :select) ; s
        84 (m/set-tool! :rect-line) ; t
        nil))

(defn handle-mousemove [event]
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
          (m/highlight-layer! point))))

    (set-moving-from! point)
    (render)))

(defn handle-input-event [{:keys [type point action event]}]
  (case type
    :down
    (do
      (m/set-action! action)
      (set-moving-from! point))
    :up
    (m/set-action! nil))
  (when (= action :draw)
    (cond
      (d/draw-tool?)
      (case type
        :down (m/initiate-current-layer! point)
        :up (m/finish-current-layer!))

      (d/select-tool?)
      (case type
        :down (m/select-layer! point (.-shiftKey event))
        :up (m/finish-selection!))))
  (render))

(defn install-keyboard-events []
  (dommy/listen! js/document :keydown handle-keyboard-events))

(defn install-mouse-events []
  (dommy/listen!  js/document :mousemove handle-mousemove))
