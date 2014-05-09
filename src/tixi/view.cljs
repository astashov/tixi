(ns tixi.view
  (:require-macros [dommy.macros :refer (node sel1)]
                   [cljs.core.async.macros :refer [go]])
  (:require [quiescent :as q :include-macros true]
            [quiescent.dom :as d]
            [dommy.core :as dommy]
            [cljs.core.async :as async :refer [>!]]
            [tixi.utils :refer [p]]
            [tixi.position :as p]
            [tixi.drawer :as drawer]))

(enable-console-print!)

(defn- send-event-with-coords [action type event channel]
  (let [text-coords (p/text-coords-from-event (.-nativeEvent event))]
    (go (>! channel {:action action :type type :value text-coords}))))

(defn- selection-position [item]
  (let [[x1 y1 x2 y2] (:content item)
        [small-x large-x] (sort [x1 x2])
        [small-y large-y] (sort [y1 y2])
        {left :x top :y} (p/position-from-text-coords small-x small-y)
        {width :x height :y} (p/position-from-text-coords (inc (.abs js/Math (- large-x small-x)))
                                                          (inc (.abs js/Math (- large-y small-y))))]
    {:left left :top top :width width :height height}))

(q/defcomponent Layer
  "Displays the layer"
  [{:keys [item is-hover is-selected]}]
  (d/pre {:className (str "canvas--content--layer"
                          (if is-selected " is-selected" "")
                          (if is-hover " is-hover" ""))}
    (apply drawer/render item (p/canvas-size))))

(q/defcomponent Canvas
  "Displays the canvas"
  [data channel]
  (d/div {:className "canvas"
          :onMouseDown (fn [e] (send-event-with-coords "draw" "down" e channel))
          :onMouseUp (fn [e] (send-event-with-coords "draw" "up" e channel))}
    (apply d/div {:className "canvas--content"} 
           (map
             (fn [[id item]] (Layer {:item item
                                     :is-hover (= id (:hover-id data))
                                     :is-selected (= id (:selected-id data))}))
             (if-let [[id item] (:current data)]
               (assoc (:completed data) id item)
               (:completed data))))))

(q/defcomponent Selection
  "Displays the selection box around the selected item"
  [data channel]
  (let [selected-item (get-in data [:completed (:selected-id data)])
        [x1 y1 x2 y2] (:content selected-item)]
    (apply d/div {:className (str
                               "selection"
                               (when (> x1 x2) " is-flipped-x")
                               (when (> y1 y2) " is-flipped-y"))
                  :style (into {} (map (fn [[key value]] [key (str value "px")])
                                       (selection-position (get-in data [:completed (:selected-id data)]))))}
      (map (fn [css-class]
             (d/div {:className (str "selection--dot selection--dot__" css-class)
                     :onMouseDown (fn [e] (send-event-with-coords (str "resize-" css-class) "down" e channel))
                     :onMouseUp (fn [e] (send-event-with-coords (str "resize-" css-class) "up" e channel))}))
           ["nw" "n" "ne" "w" "e" "sw" "s" "se"]))))

(q/defcomponent Project
  "Displays the project"
  [data channel]
  (d/div {:className (str "project"
                          (cond
                            (and (:selected-id data)
                                 (= (:hover-id data) (:selected-id data))) " is-able-to-move"
                            (:hover-id data) " is-hover"
                            :else ""))}
    (Canvas data channel)
    (if (:selected-id data)
      (Selection data channel))))

(defn render [data channel]
  "Renders the project"
  (q/render (Project data channel) (sel1 :#content)))
