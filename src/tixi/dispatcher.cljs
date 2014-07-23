(ns tixi.dispatcher
  (:require [dommy.core :as dommy]
            [tixi.data :as d]
            [tixi.controller :as c]
            [tixi.geometry :as g :refer [Point]]
            [tixi.position :as p]
            [tixi.utils :refer [p]]))

(def ^:private default-state {:start-action nil
                              :start (Point. -1 -1)
                              :previous-point (Point. -1 -1)
                              :mouse-down? false})

(def ^:private state (atom default-state))

(defn- reset-state! []
  (reset! state default-state))

(defn- set-state! [key value]
  (swap! state assoc key value))

(defn- mouse-up [point modifiers]
  (c/mouse-up (:start @state) point modifiers {:action (:start-action @state)})
  (reset-state!))

(defn- handle-mousemove [point modifiers payload]
  (when point
    (if (not= (:start @state) (:start default-state))
      (if (not (:mouse-down? @state))
        (mouse-up point modifiers)
        (c/mouse-drag (:start @state) (:previous-point @state) point modifiers payload))
      (c/mouse-move (:previous-point @state) point modifiers payload))
    (set-state! :previous-point point)))

(defn handle-keyboard-events [event]
  (when-not (d/edit-text-id)
    (case (.-keyCode event)
      8  (do ; backspace
           (.preventDefault event)
           (c/keypress :delete))
      76 (c/keypress :line) ; l
      81 (c/keypress :result)
      82 (c/keypress :rect) ; r
      83 (c/keypress :select) ; s
      84 (c/keypress :text) ; t
      89 (c/keypress :rect-line) ; y
      90 (p @d/data) ; y
      85 (c/keypress :undo) ; u
      78 (c/keypress :z-inc) ; n
      77 (c/keypress :z-dec) ; m
      nil)))

(defn handle-input-event [{:keys [type data]}]
  (case type
    :down
    (let [{:keys [point modifiers action]} data]
      (set-state! :start point)
      (set-state! :start-action action)
      (set-state! :previous-point point)
      (set-state! :mouse-down? true)
      (c/mouse-down point modifiers {:action action}))

    :up
    (let [{:keys [point modifiers]} data]
      (mouse-up point modifiers))

    :move
    (let [{:keys [point modifiers]} data]
      (handle-mousemove point modifiers {:action (:start-action @state)}))

    :edit
    (c/edit-text data)

    :tool
    (c/click-toolbar data)

    :line-edge
    (c/change-line-edge data)

    :selection-edges
    (c/change-selection-edges data)

    :close
    (c/close data)))

(defn install-keyboard-events []
  (dommy/listen! js/document :keydown handle-keyboard-events)

  ;; Defense from the case when mouseup happened outside of .project (or even outside the browser window)
  (dommy/listen! js/document :mouseup (fn [e]
    (when (= (.-button e) 0)
      (js/setTimeout #(set-state! :mouse-down? false) 0)))))

(defn install-onresize-event []
  ;; By some reason if attaching with dommy, it fails in PhantomJS
  (.addEventListener js/window "resize" (fn [_] (p/set-letter-size!))))
