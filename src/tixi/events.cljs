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
    :keydown
    (fn [event]
      (case (.-keyCode event)
        8  (do ; backspace
             (.preventDefault event)
             (m/delete-selected!)
             (render))
        76 (m/set-tool! :line) ; l
        82 (m/set-tool! :rect) ; r
        83 (m/set-tool! :select) ; s
        84 (m/set-tool! :rect-line) ; t
        nil
      ))))

(defn handle-draw-tool-actions [type [x y]]
  (case type
    :down (m/initiate-current-layer! [x y])
    :up (m/finish-current-layer!)))

(defn handle-selection-tool-actions [type [x y] add-more?]
  (case type
    :down (m/select-layer! [x y] add-more?)
    :up nil))

(def ^:private request-id (atom nil))
(def ^:private moving-from (atom [0 0]))

(defn render []
  (when @request-id
    (.cancelAnimationFrame js/window @request-id))
  (let [id (.requestAnimationFrame js/window
             (fn [_]
               (reset! request-id nil)
               (v/render @d/data channel)))]
    (reset! request-id id)))

(defn set-moving-from! [[x y]]
  (reset! moving-from [x y]))

(defn install-mouse-events []
  (dommy/listen!
    js/document
    :mousemove
    (fn [event]
      (when-let [{:keys [x y]} (p/text-coords-from-event event)]
        (let [[px py] @moving-from
              dx (- x px)
              dy (- y py)]
          (cond
            (d/draw-action?)
            (cond
              (d/draw-tool?) (m/update-current-layer! [x y])
              (d/select-tool?) (m/move-selection! [dx dy]))

            (d/resize-action)
            (m/resize-selection! [dx dy] (d/resize-action))

            :else
            (when (d/select-tool?)
              (m/highlight-layer! [x y]))))

        (set-moving-from! [x y])
        (render)))))
