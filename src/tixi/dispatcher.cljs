(ns tixi.dispatcher
  (:require [dommy.core :as dommy]
            [tixi.data :as d]
            [tixi.controller :as c]
            [tixi.geometry :as g]
            [tixi.position :as p]
            [tixi.utils :refer [p]]))

(def ^:private default-state {:start-action nil
                              :start (g/build-point -1 -1)
                              :previous-point (g/build-point -1 -1)
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

(defn not-in-input? [event]
  (not= (and (.-target event)
             (.. event -target -tagName))
        "INPUT"))

(defn handle-keyboard-events [event]
  (when (and (not (d/edit-text-id)) (not-in-input? event))
    (case (.-keyCode event)
      8  (do ; backspace
           (.preventDefault event)
           (c/keypress :delete))
      67 (when (or (.-metaKey event) (.-ctrlKey event))
           (c/keypress :copy)) ; c
      76 (c/keypress :line) ; l
      81 (c/keypress :result)
      82 (c/keypress :rect) ; r
      83 (c/keypress :select) ; s
      84 (c/keypress :text) ; t
      86 (when (or (.-metaKey event) (.-ctrlKey event))
           (c/keypress :paste)) ; v
      88 (when (or (.-metaKey event) (.-ctrlKey event))
           (c/keypress :cut)) ; x
      89 (c/keypress :rect-line) ; y
      90 (p @d/data) ; z
      85 (c/keypress :undo) ; u
      73 (c/keypress :redo) ; u
      78 (c/keypress :z-inc) ; n
      77 (c/keypress :z-dec) ; m
      37 (c/keypress :move-left) ; left
      38 (c/keypress :move-up) ; up
      39 (c/keypress :move-right) ; right
      40 (c/keypress :move-down) ; down
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
    (c/close data)

    :canvas-size
    (c/canvas-size data)))

(defn install-keyboard-events []
  (dommy/listen! js/document :keydown handle-keyboard-events)

  ;; Defense from the case when mouseup happened outside of .project (or even outside the browser window)
  (dommy/listen! js/document :mouseup (fn [e]
    (when (= (.-button e) 0)
      (js/setTimeout #(set-state! :mouse-down? false) 0)))))

(defn install-onresize-event []
  ;; By some reason if attaching with dommy, it fails in PhantomJS
  (.addEventListener js/window "resize" (fn [_] (p/set-letter-size!))))
