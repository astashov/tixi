(ns tixi.core
  (:require-macros [dommy.macros :refer (node sel1)]
                   [cljs.core.async.macros :refer [go]])
  (:require [tixi.view :as v]
            [dommy.core :as dommy]
            [tixi.drawer :as drawer]
            [tixi.channel :refer [channel]]
            [tixi.events :as e]
            [tixi.mutators :as m]
            [tixi.data :as d]
            [tixi.utils :refer [p]]
            [cljs.core.async :as async :refer [<! >! chan put! timeout]]))

(enable-console-print!)

(e/install-keyboard-events)
(e/install-mouse-events)

(v/render @d/data channel)

(go (while true
      (let [{:keys [type value action]} (<! channel)
            {:keys [x y]} value]
        (case type
          "down" (do
                   (m/set-action! action)
                   (m/start-moving! x y))
          "up" (do
                 (m/set-action! nil)
                 (m/finish-moving!)))

        (when (= action "draw")
          (cond
            (d/draw-tool?)
            (e/handle-draw-tool-actions type x y)

            (d/select-tool?)
            (e/handle-selection-tool-actions type x y)))

        (v/render @d/data channel))))
