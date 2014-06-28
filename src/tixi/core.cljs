(ns tixi.core
  (:require-macros [dommy.macros :refer (node sel1)]
                   [cljs.core.async.macros :refer [go]])
  (:require [tixi.view :as v]
            [dommy.core :as dommy]
            [tixi.channel :refer [channel]]
            [tixi.events :as e]
            [tixi.mutators :as m]
            [tixi.data :as d]
            [tixi.utils :refer [p]]
            [cljs.core.async :as async :refer [<! >! chan put! timeout]]))

(enable-console-print!)

(e/install-keyboard-events)

(e/render)

(go (while true
  (e/handle-input-event (<! channel))))
