(ns tixi.core
  (:require-macros [tixi.controller :refer (render)]
                   [cljs.core.async.macros :refer [go]])
  (:require [tixi.channel :refer [channel]]
            [tixi.dispatcher :as di]
            [tixi.utils :refer [p]]
            [cljs.core.async :as async :refer [<!]]))

(enable-console-print!)

(di/install-keyboard-events)
(di/install-onresize-event)

(render)

(go (while true
  (di/handle-input-event (<! channel))))
