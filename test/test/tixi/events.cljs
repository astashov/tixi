(ns test.tixi.events
  (:require-macros [cemerick.cljs.test :refer (is deftest use-fixtures)]
                   [tixi.utils :refer (b)])
  (:require [cemerick.cljs.test :as test]
            [tixi.geometry :as g :refer [Rect Point Size]]
            [tixi.events :as e]
            [tixi.mutators :as m]
            [tixi.position :as p]
            [test.tixi.utils :refer [create-layer!]]
            [tixi.utils :refer [p]]
            [tixi.data :as d]))

(defn- setup [f]
  (m/reset-data!)
  (e/reset-data!)
  (f))

(use-fixtures :each setup)

(defn- build-keyboard-event
  ([key-code] (build-keyboard-event key-code false))
  ([key-code shift-key]
  (js-obj "keyCode" key-code
          "shiftKey" shift-key
          "preventDefault" (fn [] nil))))

(defn- build-mouse-event [point]
  (js-obj "clientX" (:x point) "clientY" (:y point)))

(deftest handle-keyboard-events-tools
  (e/handle-keyboard-events (build-keyboard-event 76))
  (is (= (d/tool) :line))

  (e/handle-keyboard-events (build-keyboard-event 82))
  (is (= (d/tool) :rect))

  (e/handle-keyboard-events (build-keyboard-event 83))
  (is (= (d/tool) :select))

  (e/handle-keyboard-events (build-keyboard-event 84))
  (is (= (d/tool) :rect-line)))

(deftest handle-keyboard-events-delete
  (create-layer! (g/build-rect (Point. 5 6) (Point. 7 8)))
  (m/select-layer! (Point. 5 6))
  (e/handle-keyboard-events (build-keyboard-event 8))
  (is (= (vec (keys (d/completed))) [])))

(deftest handle-input-event-action-down
  (e/handle-input-event {:type :down, :action :resize-ne})
  (is (= (d/action) :resize-ne)))

(deftest handle-input-event-action-up
  (e/handle-input-event {:type :up, :action :resize-ne})
  (is (= (d/action) nil)))

(deftest handle-input-event-draw-down
  (e/handle-input-event {:type :down, :action :draw, :point (Point. 2 4)})
  (is (= (:input (:item (d/current))) (g/build-rect 2 4 2 4))))

(deftest handle-input-event-draw-up
  (e/handle-input-event {:type :down, :action :draw, :point (Point. 2 4)})
  (e/handle-input-event {:type :up, :action :draw})
  (is (= (:input (:item (d/current))) nil))
  (is (= (:input (first (vals (d/completed)))) (g/build-rect 2 4 2 4))))

(deftest handle-input-event-select-down
  (create-layer! (g/build-rect 2 2 4 4))
  (create-layer! (g/build-rect 5 5 7 7))
  (m/set-tool! :select)
  (e/handle-input-event {:type :down, :action :draw, :point (Point. 2 2), :event (build-keyboard-event 8)})
  (e/handle-input-event {:type :down, :action :draw, :point (Point. 5 5), :event (build-keyboard-event 8)})
  (is (= (d/selected-ids) [1])))

(deftest handle-input-event-select-down
  (create-layer! (g/build-rect 2 2 4 4))
  (create-layer! (g/build-rect 5 5 7 7))
  (m/set-tool! :select)
  (e/handle-input-event {:type :down, :action :draw, :point (Point. 2 2), :event (build-keyboard-event 8)})
  (e/handle-input-event {:type :down, :action :draw, :point (Point. 5 5), :event (build-keyboard-event 8 true)})
  (is (= (d/selected-ids) [0 1])))

(deftest handle-input-event-select-down-more
  (create-layer! (g/build-rect 2 2 4 4))
  (create-layer! (g/build-rect 5 5 7 7))
  (m/set-tool! :select)
  (e/handle-input-event {:type :down, :action :draw, :point (Point. 2 2), :event (build-keyboard-event 8)})
  (e/handle-input-event {:type :down, :action :draw, :point (Point. 5 5), :event (build-keyboard-event 8 true)})
  (is (= (d/selected-ids) [0 1])))

(deftest handle-input-event-select-up
  (create-layer! (g/build-rect 2 2 4 4))
  (m/set-tool! :select)
  (e/handle-input-event {:type :down, :action :draw, :point (Point. 1 1), :event (build-keyboard-event 8)})
  (e/handle-mousemove (build-mouse-event (p/coords->position (Point. 5 5))))
  (e/handle-input-event {:type :up, :action :draw, :point (Point. 5 5)})
  (is (= (d/selected-ids) [0])))

(deftest handle-mousemove-draw
  (m/set-tool! :line)
  (e/handle-input-event {:type :down, :action :draw, :point (Point. 1 1)})
  (e/handle-mousemove (build-mouse-event (p/coords->position (Point. 4 4))))
  (is (= (:input (:item (d/current))) (g/build-rect 1 1 4 4))))

(deftest handle-mousemove-select-update
  (m/set-tool! :select)
  (e/handle-input-event {:type :down, :action :draw, :point (Point. 1 1), :event (build-mouse-event (Point. 0 0))})
  (e/handle-mousemove (build-mouse-event (p/coords->position (Point. 4 4))))
  (is (= (d/current-selection) (g/build-rect 1 1 4 4))))

(deftest handle-mousemove-select-move
  (create-layer! (g/build-rect 2 2 4 4))
  (m/set-tool! :select)
  (e/handle-input-event {:type :down, :action :draw, :point (Point. 2 2), :event (build-mouse-event (Point. 0 0))})
  (e/handle-mousemove (build-mouse-event (p/coords->position (Point. 4 4))))
  (is (= (d/selection-rect) (g/build-rect 4 4 6 6))))

(deftest handle-mousemove-highlight
  (create-layer! (g/build-rect 2 2 4 4))
  (m/set-tool! :select)
  (e/handle-mousemove (build-mouse-event (p/coords->position (Point. 2 2))))
  (is (= (d/hover-id) 0)))

(deftest handle-mousemove-resize
  (create-layer! (g/build-rect 2 2 4 4))
  (m/set-tool! :select)
  (e/handle-input-event {:type :down, :action :draw, :point (Point. 2 2), :event (build-keyboard-event 8)})
  (e/handle-input-event {:type :down, :action :resize-se, :point (Point. 4 4)})
  (e/handle-mousemove (build-mouse-event (p/coords->position (Point. 6 6))))
  (is (= (d/selection-rect) (g/build-rect 2 2 6 6))))
