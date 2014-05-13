(ns tixi.events
  (:require [dommy.core :as dommy]
            [tixi.view :as v]
            [tixi.channel :refer [channel]]
            [tixi.data :as d]
            [tixi.position :as p]
            [tixi.utils :refer [p]]
            [tixi.mutators :as m]))

(defn install-keyboard-events []
  (dommy/listen!
    js/document
    :keypress
    (fn [event]
      (case (.-keyCode event)
        108 (m/set-tool! :line) ; l
        114 (m/set-tool! :rect) ; r
        115 (m/set-tool! :select) ; s
        116 (m/set-tool! :rect-line) ; t
      ))))

(defn handle-draw-tool-actions [type [x y]]
  (case type
    :down (m/initiate-current-layer! [x y])
    :up (m/finish-current-layer!)))

(defn handle-selection-tool-actions [type [x y]]
  (case type
    :down (m/select-layer! [x y])
    :up (m/finish-moving!)))

(def ^:private request-id (atom nil))

(defn render []
  (when @request-id
    (.cancelAnimationFrame js/window @request-id))
  (let [id (.requestAnimationFrame js/window
             (fn [_]
               (reset! request-id nil)
               (v/render @d/data channel)))]
    (reset! request-id id)))

(defn install-mouse-events []
  (dommy/listen!
    js/document
    :mousemove
    (fn [event]
      (when-let [{:keys [x y]} (p/text-coords-from-event event)]
        (cond
          (d/draw-action?)
          (cond
            (d/draw-tool?) (m/update-current-layer! [x y])
            (d/select-tool?) (m/move-layer! (d/selected-id) [x y]))

          (d/resize-action)
          (m/resize! (d/selected-id) [x y] (d/resize-action))

          :else
          (when (d/select-tool?)
            (m/highlight-layer! [x y])))
        (render)))))
