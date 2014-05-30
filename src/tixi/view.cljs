(ns tixi.view
  (:require-macros [dommy.macros :refer (node sel1)]
                   [schema.macros :as scm]
                   [cljs.core.async.macros :refer [go]])
  (:require [quiescent :as q :include-macros true]
            [quiescent.dom :as dom]
            [dommy.core :as dommy]
            [cljs.core.async :as async :refer [>!]]
            [tixi.schemas :as s]
            [tixi.data :as d]
            [tixi.geometry :as g :refer [Size]]
            [tixi.utils :refer [p]]
            [tixi.position :as p]
            [tixi.drawer :as drawer]))

(enable-console-print!)

(defn- send-event-with-coords [action type event channel]
  (let [nativeEvent (.-nativeEvent event)
        point (p/event->coords nativeEvent)]
    (go (>! channel {:action action :type type :point point :event nativeEvent}))))

(defn- selection-position [rect]
  (let [normalized-rect (g/normalize rect) 
        {left :x top :y} (p/coords->position (g/origin normalized-rect))
        {rect-width :width rect-height :height} (g/dimensions normalized-rect) 
        {:keys [width height]} (p/coords->position (Size. (inc rect-width) (inc rect-height)))]
    {:left (str left "px") :top (str top "px") :width (str width "px") :height (str height "px")}))

(q/defcomponent Layer
  "Displays the layer"
  [{:keys [id item is-hover is-selected]}]
  (let [{:keys [origin dimensions text]} (:cache item)
        {:keys [x y]} (p/coords->position origin)
        {:keys [width height]} (p/coords->position dimensions)]
    (dom/pre {:className (str "canvas--content--layer"
                            (if is-selected " is-selected" "")
                            (if is-hover " is-hover" ""))
            :style {:left x :top y :width width :height height}
            :id (str "layer-" id)}
      text)))

(q/defcomponent Canvas
  "Displays the canvas"
  [data channel]
  (dom/div {:className "canvas"
            :onMouseDown (fn [e] (send-event-with-coords :draw :down e channel))
            :onMouseUp (fn [e] (send-event-with-coords :draw :up e channel))}
    (apply dom/div {:className "canvas--content"}
           (map
             (fn [[id item]] (Layer {:id id
                                     :item item
                                     :is-hover (= id (:hover-id data))
                                     :is-selected (some #{id} (d/selected-ids data))}))
             (if-let [{:keys [id item]} (:current data)]
               (assoc (:completed data) id item)
               (:completed data))))))

(q/defcomponent Selection
  "Displays the selection box around the selected item"
  [data channel]
  (let [selected-ids (d/selected-ids data)
        rect (p/items-wrapping-rect selected-ids)]
    (apply dom/div {:className (str "selection" (when (g/flipped-by-x? rect) " is-flipped-x")
                               (when (g/flipped-by-y? rect) " is-flipped-y"))
                    :style (selection-position rect)}
      (map (fn [css-class]
             (dom/div {:className (str "selection--dot selection--dot__" css-class)
                     :onMouseDown (fn [e] (send-event-with-coords (keyword (str "resize-" css-class)) :down e channel))
                     :onMouseUp (fn [e] (send-event-with-coords (keyword (str "resize-" css-class)) :up e channel))}))
           ["nw" "n" "ne" "w" "e" "sw" "s" "se"]))))

(q/defcomponent CurrentSelection
  "Displays the selection box around the selected item"
  [data channel]
  (let [rect (g/normalize (d/current-selection data))]
    (when (p (and (> (g/width rect) 0) (> (g/height rect) 0)))
      (dom/div {:className "current-selection" :style (selection-position rect)}))))

(q/defcomponent Tool
  "Displays the currently selected tool"
  [data]
  (dom/div {:className "tool"} (str (:tool data))))

(q/defcomponent Project
  "Displays the project"
  [data channel]
  (let [selected-ids (d/selected-ids data)
        current-selection (d/current-selection data)]
    (dom/div {:className (str "project"
                           (cond
                             (some #{(d/hover-id data)} selected-ids) " is-able-to-move"
                             (d/hover-id data) " is-hover"
                             :else ""))}
      (Canvas data channel)
      (when (not-empty selected-ids)
        (Selection data channel))
      (when current-selection
        (CurrentSelection data channel))
      (when ())
      (Tool data))))

(defn dom-content []
  (if-let [content (sel1 :#content)]
    content
    (do
      (dommy/append! (sel1 :body) [:#content ""])
      (sel1 :#content))))

(scm/defn ^:always-validate render [data :- s/Data channel]
  "Renders the project"
  (q/render (Project data channel) (dom-content)))
