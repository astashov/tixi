(ns tixi.core
  (:require-macros [tixi.controller :refer (render)]
                   [dommy.macros :refer (node sel1)]
                   [cljs.core.async.macros :refer [go]])
  (:require [tixi.view :as v]
            [clojure.browser.repl :as repl]
            [dommy.core :as dommy]
            [tixi.channel :refer [channel]]
            [tixi.dispatcher :as di]
            [tixi.mutators :as m]
            [tixi.data :as d]
            [tixi.utils :refer [p]]
            [cljs.core.async :as async :refer [<! >! chan put! timeout]]))

(repl/connect "http://localhost:9000/repl")

(enable-console-print!)

(di/install-keyboard-events)

(render)

(go (while true
  (di/handle-input-event (<! channel))))
