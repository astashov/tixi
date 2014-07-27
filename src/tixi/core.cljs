(ns tixi.core
  (:require-macros [tixi.controller :refer (render)]
                   [cljs.core.async.macros :refer [go]])
  (:require [tixi.channel :refer [channel]]
            [tixi.data :as d]
            [tixi.dispatcher :as di]
            [cljs.reader :as r]
            [tixi.sync :as s]
            [tixi.compress :as c]
            [tixi.mutators.shared :as ms]
            [tixi.mutators.undo :as mu]
            [tixi.utils :refer [p]]
            [cljs.core.async :as async :refer [<!]]))

(enable-console-print!)

(di/install-keyboard-events)
(di/install-onresize-event)

(s/load (fn [data]
  (when (.val data)
    (mu/snapshot!)
    (render
      (ms/assign-state! (r/read-string (c/decompress (.val data))))))))

(render)

(go (while true
  (di/handle-input-event (<! channel))))
