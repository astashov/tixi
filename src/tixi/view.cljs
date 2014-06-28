(ns tixi.view
  (:require-macros [dommy.macros :refer (node sel1)]
                   [schema.macros :as scm]
                   [tixi.utils :refer (b)]
                   [cljs.core.async.macros :refer [go]])
  (:require [quiescent :as q :include-macros true]
            [quiescent.dom :as dom]
            [clojure.string :as string]
            [dommy.core :as dommy]
            [cljs.core.async :as async :refer [>!]]
            [tixi.schemas :as s]
            [tixi.data :as d]
            [tixi.geometry :as g :refer [Size]]
            [tixi.utils :refer [p]]
            [tixi.position :as p]
            [tixi.items :as i]
            [tixi.text-editor :as te]))

(enable-console-print!)

(defn- send-event-with-coords [action type event channel]
  (let [nativeEvent (.-nativeEvent event)
        point (p/event->coords nativeEvent)]
    (go (>! channel {:type type :data {:action action :point point :event nativeEvent}}))))

(defn- send-mousemove [event channel]
  (let [nativeEvent (.-nativeEvent event)
        point (p/event->coords nativeEvent)]
    (go (>! channel {:type :move :data {:point point :event nativeEvent}}))))

(defn- send-tool-click [name channel]
  (go (>! channel {:type :tool :data {:name name}})))

(defn- selection-position [rect]
  (let [normalized-rect (g/normalize rect)
        {left :x top :y} (p/coords->position (g/origin normalized-rect))
        {:keys [width height]} (p/coords->position (g/incr (g/dimensions normalized-rect)))]
    {:left (str left "px") :top (str top "px") :width (str width "px") :height (str height "px")}))

(defn- letter-size []
  (str (:height (p/letter-size)) "px " (:width (p/letter-size)) "px"))

(q/defcomponent Text
  [{:keys [id item edit-text-id]} channel]
  (q/on-update
    (dom/div {:ref "text" :className "text" :id (str "text-" id)}
      (dom/div {:className "text--wrapper" :style {:padding (letter-size)}}
        (when (not= id edit-text-id)
          (dom/div {:className "text--wrapper--content" :id (str "text-content-" id)} (:text item)))))
    (fn [node]
      (let [install-node (sel1 node :.text--wrapper)]
        (te/install-or-remove!
          (= id edit-text-id) install-node (or (:text item) "")
          (fn [value dimensions]
            (go (>! channel {:type :edit :data {:text value :id id :dimensions (p/position->coords dimensions)}}))))
        (when-let [content (sel1 install-node :.text--wrapper--content)]
          (te/adjust-height! install-node content))))))

(q/defcomponent Layer
  "Displays the layer"
  [{:keys [id item is-hover is-selected edit-text-id]} channel]
  (let [{:keys [data]} (:cache item)
        {:keys [x y]} (p/coords->position (i/origin item))
        {:keys [width height]} (p/coords->position (g/incr (i/dimensions item)))]
    (dom/pre {:className (str "canvas--content--layer"
                            (str " canvas-content--layer__" (i/kind item))
                            (if is-selected " is-selected" "")
                            (if is-hover " is-hover" ""))
            :style {:left x :top y :width width :height height}
            :id (str "layer-" id)}
      (Text {:id id :item item :edit-text-id edit-text-id} channel)
      data)))

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
                                     :is-selected (some #{id} (d/selected-ids data))
                                     :edit-text-id (d/edit-text-id data)}
                                    channel))
             (if-let [{:keys [id item]} (:current data)]
               (assoc (d/completed data) id item)
               (d/completed data))))))

(q/defcomponent Selection
  "Displays the selection box around the selected item"
  [data channel]
  (let [selected-ids (d/selected-ids data)
        rect (p/items-wrapping-rect selected-ids)
        classes (->> (d/selected-ids data)
                     (map #(d/completed-item %))
                     (map #(i/kind %))
                     sort
                     (string/join "__"))]
    (apply dom/div {:className (str "selection"
                                    (str " selection__" classes)
                                    (when (g/flipped-by-x? rect) " is-flipped-x")
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
    (dom/div {:className "current-selection" :style (selection-position rect)})))

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
                             :else ""))
              :onMouseMove (fn [e] (send-mousemove e channel))}
      (Canvas data channel)
      (when (and (not-empty selected-ids) (not (d/edit-text-id)))
        (Selection data channel))
      (when (and current-selection (> (g/width current-selection) 0) (> (g/height current-selection) 0))
        (CurrentSelection data channel))
      (Tool data))))

(q/defcomponent Sidebar
  [data channel]
  (dom/div {:className "sidebar"}
    (dom/h1 {:className "sidebar--logo"} "Tixi")
    (dom/div {:className "sidebar--tools"}
      (dom/button {:className (str "sidebar--tools--button sidebar--tools--button__select"
                                   (when (= (d/tool data) :select) " is-selected"))
                   :title "Select [S]"
                   :onClick (fn [e] (send-tool-click :select channel))}
        (dom/div {:className "sidebar--tools--button--icon"}))
      (dom/button {:className (str "sidebar--tools--button sidebar--tools--button__line"
                                   (when (= (d/tool data) :line) " is-selected"))
                   :title "Line [L]"
                   :onClick (fn [e] (send-tool-click :line channel))}
        (dom/div {:className "sidebar--tools--button--icon"}))
      (dom/button {:className (str "sidebar--tools--button sidebar--tools--button__rect-line"
                                   (when (= (d/tool data) :rect-line) " is-selected"))
                   :title "Rectangle-Line [Y]"
                   :onClick  (fn [e] (send-tool-click :rect-line channel))}
        (dom/div {:className "sidebar--tools--button--icon"}))
    (dom/button {:className (str "sidebar--tools--button sidebar--tools--button__rect"
                                 (when (= (d/tool data) :rect) " is-selected"))
                 :title "Rectangle [R]"
                 :onClick  (fn [e] (send-tool-click :rect channel))}
        (dom/div {:className "sidebar--tools--button--icon"}))
      (dom/button {:className (str "sidebar--tools--button sidebar--tools--button__text"
                                   (when (= (d/tool data) :text) " is-selected"))
                   :title "Text [T]"
                   :onClick  (fn [e] (send-tool-click :text channel))}
        (dom/div {:className "sidebar--tools--button--icon"}))
      (dom/button {:className "sidebar--tools--button sidebar--tools--button__undo"
                   :title "Undo [U]"
                   :onClick (fn [e] (send-tool-click :undo channel))}
        (dom/div {:className "sidebar--tools--button--icon"}))
      (dom/button {:className "sidebar--tools--button sidebar--tools--button__redo"
                   :title "Redo [I]"
                   :onClick (fn [e] (send-tool-click :redo channel))}
        (dom/div {:className "sidebar--tools--button--icon"}))
      (dom/button {:className "sidebar--tools--button sidebar--tools--button__result"
                   :title "Show Result [Q]"
                   :onClick (fn [e] (send-tool-click :result channel))}
        (dom/div {:className "sidebar--tools--button--icon"}))
      (dom/button {:className "sidebar--tools--button sidebar--tools--button__delete"
                   :title "Delete [Backspace]"
                   :onClick (fn [e] (send-tool-click :delete channel))}
        (dom/div {:className "sidebar--tools--button--icon"})))))

(q/defcomponent Content
  [data channel]
  (dom/div {:className "content"}
    (Sidebar data channel)
    (Project data channel)))

(defn- dom-content []
  (if-let [content (sel1 :#content)]
    content
    (do
      (dommy/append! (sel1 :body) [:#content ""])
      (sel1 :#content))))

(scm/defn render [data :- s/Data channel]
  "Renders the project"
  (q/render (Content data channel) (dom-content)))
