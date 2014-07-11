(ns tixi.core
  (:require-macros [tixi.controller :refer (render)]
                   [cljs.core.async.macros :refer [go]])
  (:require [tixi.channel :refer [channel]]
            [tixi.dispatcher :as di]
            [cljs.core.async :as async :refer [<!]]))

(enable-console-print!)

(di/install-keyboard-events)

(render)

(go (while true
  (di/handle-input-event (<! channel))))
