(ns tixi.view
  (:require-macros [dommy.macros :refer (node sel1)]
                   [tixi.utils :refer (b)]
                   [cljs.core.async.macros :refer [go]])
  (:require [quiescent :as q :include-macros true]
            [quiescent.dom :as dom]
            [clojure.string :as string]
            [dommy.core :as dommy]
            [cljs.core.async :as async :refer [>!]]
            [tixi.data :as d]
            [tixi.geometry :as g]
            [tixi.utils :refer [p next-of]]
            [tixi.position :as p]
            [tixi.items :as i]
            [goog.style :as style]
            [tixi.text-editor :as te]))

(enable-console-print!)

(def css-transition-group
  (-> js/React .-addons .-CSSTransitionGroup))

(aset js/window "css-transition-group" css-transition-group)

(defn- select-text! [node]
  (if (-> js/document .-body .-createTextRange)
    (let [range (-> js/document .-body .createTextRange)]
      (.moveToElementText range node)
      (.select range))
    (let [selection (.getSelection js/window)
          range (.createRange js/document)]
      (.selectNodeContents range node)
      (.removeAllRanges selection)
      (.addRange selection range))))

(defn- event->position [event]
  (let [root (sel1 :.project)
        offset (if root (style/getPageOffset root) (js-obj "x" 0 "y" 0))
        x (- (.-clientX event) (.-x offset))
        y (- (.-clientY event) (.-y offset))]
    (g/build-point x y)))

(defn- send-event-with-coords [action type event channel]
  (when (= (.-button event) 0)
    (let [point (event->position event)
          shift-pressed? (aget (aget event "nativeEvent") "shiftKey")]
      (go (>! channel {:type type :data {:action action
                                         :point point
                                         :modifiers {:shift shift-pressed?}}})))))

(defn- send-mousemove [event channel]
  (let [point (event->position event)
        shift-pressed? (aget (aget event "nativeEvent") "shiftKey")]
    (go (>! channel {:type :move :data {:point point
                                        :modifiers {:shift shift-pressed?}}}))))

(defn- send-tool-click [name channel]
  (go (>! channel {:type :tool :data {:name name}})))

(defn- close-result [channel]
  (go (>! channel {:type :close :data {:name :result}})))

(defn- send-line-edge-click [edge channel]
  (go (>! channel {:type :line-edge :data {:edge edge}})))

(defn- send-selection-edges-click [edge channel]
  (go (>! channel {:type :selection-edges :data {:edge edge}})))

(defn- selection-position [rect]
  (let [normalized-rect (g/normalize rect)
        {left :x top :y} (p/coords->position (g/origin normalized-rect))
        {:keys [width height]} (p/coords->position (g/incr (g/dimensions normalized-rect)))]
    {:left (str left "px") :top (str top "px") :width (str width "px") :height (str height "px")}))

(defn- outlet-position [item point]
  (let [position (p/coords->position (g/absolute point (g/shifted-to-0 (:input item))))]
    {:left (str (:x position) "px") :top (str (:y position) "px")}))

(defn- letter-size []
  (str (:height (p/letter-size)) "px " (:width (p/letter-size)) "px"))

(defn- draw-line-on-canvas [context rect]
  (set! (.-lineWidth context) 1)
  (set! (.-strokeStyle context) "#ececec")
  (doto context
    (.beginPath)
    (.moveTo (-> rect :start :x) (-> rect :start :y))
    (.lineTo (-> rect :end :x) (-> rect :end :y))
    (.stroke)))

(q/defcomponent Text
  [{:keys [id item edit-text-id]} channel]
  (q/on-render
    (dom/div {:ref "text" :className "text" :id (str "text-" id)}
      (dom/div {:className "text--wrapper" :style {:padding (letter-size)}}
        (when (not= id edit-text-id)
          (apply dom/div {:className "text--wrapper--content" :id (str "text-content-" id)}
            (flatten (map (fn [s]
                   [(dom/span {:className "text--wrapper--content--line"}
                      (string/replace s " " '\xA0))
                    (dom/br)])
                 (string/split (:text item) "\n")))))))
    (fn [node]
      (let [install-node (sel1 node :.text--wrapper)]
        (te/install-or-remove!
          (= id edit-text-id) install-node (or (:text item) "")
          (fn [value dimensions]
            (go (>! channel {:type :edit :data {:text value :id id}})))
          (i/text? item))
        (when-let [content (sel1 install-node :.text--wrapper--content)]
          (te/adjust-position! install-node content false (i/text? item)))))))

(q/defcomponent Outlets
  [{:keys [item points]}]
  (apply dom/div {:className "outlets"}
         (map
           (fn [point]
             (dom/div {:style (outlet-position item point) :className "outlets--point"} ""))
           points)))

(q/defcomponent Layer
  "Displays the layer"
  [{:keys [id item is-hover is-selected edit-text-id connecting-id show-z-indexes?]} channel]
  (let [{:keys [data]} (d/item-cache id)
        z-index (:z item)
        {:keys [x y]} (p/coords->position (i/origin item))
        {:keys [width height]} (p/coords->position (g/incr (i/dimensions item)))]
    (dom/pre {:className (str "canvas--content--layer"
                              (str " canvas-content--layer__" (name (:type item)))
                              (if is-selected " is-selected" "")
                              (if is-hover " is-hover" ""))
              :style {:left x :top y :width width :height height :zIndex z-index}
              :id (str "layer-" id)}
             (Text {:id id :item item :edit-text-id edit-text-id} channel)
             (when (and connecting-id (not= connecting-id id))
               (Outlets {:item item :points (i/outlets item)}))
             (when show-z-indexes?
               (dom/div {:className "canvas--content--layer--z-index"
                         :title "z-index"} z-index))
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
                                     :edit-text-id (d/edit-text-id data)
                                     :connecting-id (d/connecting-id data)
                                     :show-z-indexes? (d/show-z-indexes? data)}
                                    channel))
             (if-let [{:keys [id item]} (:current data)]
               (assoc (d/completed data) id item)
               (d/completed data))))))

(q/defcomponent Selection
  "Displays the selection box around the selected item"
  [data channel]
  (let [selected-ids (d/selected-ids data)
        rect (d/selection-rect data)
        items (map #(d/completed-item %) (d/selected-ids data))
        classes (->> items
                     (map #(name (:type %)))
                     sort
                     (string/join "__"))]
    (apply dom/div {:className (str "selection"
                                    (str " selection__" classes)
                                    (when (= (count items) 1) (str " is-" (i/direction (first items))))
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

(q/defcomponent Grid [show-grid?]
  (q/on-render
    (dom/canvas {:className (str "grid" (when show-grid? " is-visible"))})
    (fn [canvas]
      (let [project (sel1 :.project)
            context (.getContext canvas "2d")
            width (.-offsetWidth project)
            height (.-offsetHeight project)
            letter-width (:width (p/letter-size))
            letter-height (:height (p/letter-size))]
        (.setAttribute canvas "width" width)
        (.setAttribute canvas "height" height)
        (.translate context 0.5 0.5)
        (dotimes [i (.floor js/Math (/ width letter-width))]
          (draw-line-on-canvas
            context
            (g/build-rect (dec (* i letter-width)) 0
                          (dec (* i letter-width)) height)))
        (dotimes [i (.floor js/Math (/ height letter-height))]
          (draw-line-on-canvas
            context
            (g/build-rect 0 (* i letter-height)
                          width (* i letter-height))))))))

(q/defcomponent Project
  "Displays the project"
  [data channel]
  (let [selected-ids (d/selected-ids data)
        current-selection (d/current-selection data)]
    (dom/div {:className (str "project"
                           (cond
                             (some #{(d/hover-id data)} selected-ids) " is-able-to-move"
                             (d/hover-id data) " is-hover"
                             :else "")
                           (if (= (d/tool) :text) " is-text" ""))
              :onMouseMove (fn [e] (send-mousemove e channel))}
      (Grid (d/show-grid? data))
      (Canvas data channel)
      (when (and (not-empty selected-ids) (not (d/edit-text-id)))
        (Selection data channel))
      (when (and current-selection (> (g/width current-selection) 0) (> (g/height current-selection) 0))
        (CurrentSelection data channel)))))

(q/defcomponent Sidebar
  [{:keys [tool can-undo? can-redo?]} channel]
  (dom/div {:className "sidebar"}
    (dom/h1 {:className "sidebar--logo"} "Textik")
    (dom/div {:className "sidebar--tools"}
      (dom/button {:className (str "button sidebar--tools--button sidebar--tools--button__select"
                                   (when (= tool :select) " is-selected"))
                   :title "Select [S]"
                   :onClick (fn [e] (send-tool-click :select channel))}
        (dom/div {:className "button--icon sidebar--tools--button--icon"}))
      (dom/button {:className (str "button sidebar--tools--button sidebar--tools--button__line"
                                   (when (= tool :line) " is-selected"))
                   :title "Line [L]"
                   :onClick (fn [e] (send-tool-click :line channel))}
        (dom/div {:className "button--icon sidebar--tools--button--icon"}))
      (dom/button {:className (str "button sidebar--tools--button sidebar--tools--button__rect-line"
                                   (when (= tool :rect-line) " is-selected"))
                   :title "Rectangle-Line [Y]"
                   :onClick  (fn [e] (send-tool-click :rect-line channel))}
        (dom/div {:className "button--icon sidebar--tools--button--icon"}))
    (dom/button {:className (str "button sidebar--tools--button sidebar--tools--button__rect"
                                 (when (= tool :rect) " is-selected"))
                 :title "Rectangle [R]"
                 :onClick  (fn [e] (send-tool-click :rect channel))}
        (dom/div {:className "button--icon sidebar--tools--button--icon"}))
      (dom/button {:className (str "button sidebar--tools--button sidebar--tools--button__text"
                                   (when (= tool :text) " is-selected"))
                   :title "Text [T]"
                   :onClick  (fn [e] (send-tool-click :text channel))}
        (dom/div {:className "button--icon sidebar--tools--button--icon"}))
      (dom/button {:className "button sidebar--tools--button sidebar--tools--button__undo"
                   :title "Undo [U]"
                   :disabled (not can-undo?)
                   :onClick (fn [e] (send-tool-click :undo channel))}
        (dom/div {:className "button--icon sidebar--tools--button--icon"}))
      (dom/button {:className "button sidebar--tools--button sidebar--tools--button__redo"
                   :title "Redo [I]"
                   :disabled (not can-redo?)
                   :onClick (fn [e] (send-tool-click :redo channel))}
        (dom/div {:className "button--icon sidebar--tools--button--icon"}))
      (dom/button {:className "button sidebar--tools--button sidebar--tools--button__result"
                   :title "Show Result [Q]"
                   :onClick (fn [e] (send-tool-click :result channel))}
        (dom/div {:className "button--icon sidebar--tools--button--icon"}))
      (dom/button {:className "button sidebar--tools--button sidebar--tools--button__delete"
                   :title "Delete [Backspace]"
                   :onClick (fn [e] (send-tool-click :delete channel))}
        (dom/div {:className "button--icon sidebar--tools--button--icon"})))))

(q/defcomponent Topbar [data channel]
  (let [tool (d/tool data)
        show-z-indexes? (d/show-z-indexes? data)
        line-edges (d/line-edges data)
        selection-edges-disabled? (empty? (filter #(i/connector? %) (d/selected-items)))]
    (apply dom/div {:className "topbar"}
      (dom/button {:className "button topbar--button topbar--button__icon topbar--button__grid"
                   :title "Toggle grid"
                   :onClick (fn [e] (send-tool-click :grid channel))}
        (dom/div {:className "button--icon topbar--button--icon"}))
      (case tool
        :select
        [(dom/button {:className "button topbar--button topbar--button__icon topbar--button__z-inc"
                      :title "Bring forward"
                      :onClick (fn [e] (send-tool-click :z-inc channel))}
           (dom/div {:className "button--icon topbar--button--icon"}))
         (dom/button {:className "button topbar--button topbar--button__icon topbar--button__z-dec"
                      :title "Bring backward"
                      :onClick (fn [e] (send-tool-click :z-dec channel))}
           (dom/div {:className "button--icon topbar--button--icon"}))
         (dom/button {:className (str "button button__text topbar--button topbar--button__z-show"
                                      (when show-z-indexes? " is-selected"))
                      :title "Show z-indexes"
                      :onClick (fn [e] (send-tool-click :z-show channel))}
           "Show z-indexes")
         (dom/button {:className "button button__text topbar--button topbar--button__edge"
                      :title "Connector's start"
                      :onClick (fn [e] (send-selection-edges-click :start channel))
                      :disabled selection-edges-disabled?}
           (case (d/next-selected-edge-value :start)
             nil "-"
             :arrow "<"))
         (dom/button {:className "button button__text topbar--button topbar--button__edge"
                      :title "Connector's end"
                      :onClick (fn [e] (send-selection-edges-click :end channel))
                      :disabled selection-edges-disabled?}
           (case (d/next-selected-edge-value :end)
             nil "-"
             :arrow ">"))]

        (:line :rect-line)
        [(dom/button {:className "button button__text topbar--button topbar--button__edge"
                      :title "Connector's start"
                      :onClick (fn [e] (send-line-edge-click :start channel))}
           (case (:start line-edges)
             nil "-"
             :arrow "<"))
         (dom/button {:className "button button__text topbar--button topbar--button__edge"
                      :title "Connector's end"
                      :onClick (fn [e] (send-line-edge-click :end channel))}
           (case (:end line-edges)
             nil "-"
             :arrow ">"))]
        []))))

(q/defcomponent Bottombar [data channel]
  (dom/div {:className "bottombar"}
    (dom/div {:className "bottombar--right"}
      (dom/a {:href "http://astashov.github.io"} "My Blog")
      (dom/a {:href "https://github.com/astashov/tixi/issues"} "Bug Tracker")
      (dom/a {:href "https://github.com/astashov/tixi"} "Github"))))

(q/defcomponent Result [data channel] {:key "result"}
  (let [result (d/result data)
        content (.-content result)
        coords-size (g/build-size (.-width result) (.-height result))
        position-size (p/coords->position coords-size)]
    (q/on-render
      (dom/div {:className "result"}
        (dom/div {:className "result--overlay" :onClick (fn [e] (close-result channel))})
        (dom/div {:className "result--popup"}
          (dom/button {:className "result--close-top" :onClick (fn [e] (close-result channel))})
          (dom/div {:className "result--content"}
            (dom/pre {:className "result--content--pre" :style position-size}
              content))
          (dom/div {:className "result--bottom"}
            (dom/div {:className "result--bottom--hint"} "Press Cmd+C (or Ctrl+C) to copy it")
            (dom/button {:className "result--bottom--close"
                         :onClick (fn [e] (close-result channel))}
              "Close"))))
      (fn [node]
        (let [popup (sel1 node :.result--popup)
              pre (sel1 popup :.result--content--pre)]
          (set! (-> popup .-style .-marginLeft) (-> (.-offsetWidth popup) (/ 2) -))
          (set! (-> popup .-style .-marginTop) (-> (.-offsetHeight popup) (/ 2) -))
          (select-text! pre))))))

(q/defcomponent Content [data channel]
  (dom/div {:className "content"}
    (Sidebar {:tool (d/tool data)
              :can-undo? (d/can-undo? data)
              :can-redo? (d/can-redo? data)}
             channel)
    (Topbar data channel)
    (Project data channel)
    (Bottombar data channel)
    (css-transition-group #js {:transitionName "result"}
      (if (d/show-result? data)
        (Result data channel)
        (dom/div {})))))

(defn- dom-content []
  (if-let [content (sel1 :#content)]
    content
    (do
      (dommy/append! (sel1 :body) [:#content ""])
      (sel1 :#content))))

(defn render [data channel]
  "Renders the project"
  (q/render (Content data channel) (dom-content)))
