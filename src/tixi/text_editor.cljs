(ns tixi.text-editor
  (:require [tixi.utils :refer [p]]
            [tixi.position :as p]
            [tixi.geometry :as g]))

(defn- find-node [install-node]
  (.querySelector (.-parentNode install-node) ".CodeMirror"))

(defn- remove! [install-node]
  (let [node (find-node install-node)]
    (.removeChild (.-parentNode node) node)))

(defn- padding-top [node]
  (.parseInt js/window (.-paddingTop (.getComputedStyle js/window node)) 10))

(defn- padding-left [node]
  (.parseInt js/window (.-paddingLeft (.getComputedStyle js/window node)) 10))

(defn adjust-height! [install-node node]
  (let [v-padding (padding-top install-node)
        h-padding (padding-left install-node)
        install-node-height (- (.-offsetHeight install-node) (* v-padding 2))
        install-node-width (- (.-offsetWidth install-node) (* h-padding 2))
        node-height (.-offsetHeight node)
        node-width (.-offsetWidth node)
        margin-top (* (.floor js/Math (/ (- (/ install-node-height 2) (/ node-height 2))
                                         (:height (p/letter-size))))
                      (:height (p/letter-size)))

        margin-left (* (.floor js/Math (/ (- (/ install-node-width 2) (/ node-width 2))
                                          (:width (p/letter-size))))
                       (:width (p/letter-size)))]
    (aset (.-style node) "marginTop" (str margin-top "px"))
    (aset (.-style node) "marginLeft" (str margin-left "px"))))

(defn- installed? [install-node]
  (boolean (find-node install-node)))

(defn- get-instance [install-node]
  (.-CodeMirror (find-node install-node)))

(defn- set-text! [instance text]
  (.setValue instance text))

(defn- focus! [instance]
  (.focus instance)
  (.execCommand instance "goDocEnd"))

(defn- commit [instance node on-completed-callback]
  (let [size (g/Size. (.-offsetWidth node) (.-offsetHeight node))
        value (.getValue instance)]
    (remove! node)
    (on-completed-callback value size)))

(defn- install! [install-node on-completed-callback]
  (let [instance (.CodeMirror js/window install-node #js {:lineWrapping true})
        node (find-node install-node)]
    (.on instance "change" (fn [] (adjust-height! install-node node)))
    (.on instance "blur" (fn [] (commit instance node on-completed-callback)))
    (.on instance "keyHandled" (fn [_ _ event] (when (= (.-keyCode event) 27) ; Esc
                                                 (.blur (.getInputField instance)))))
    (adjust-height! install-node node)))

(defn install-or-remove! [install? install-node text on-completed-callback]
  (if install?
    (do
      (when-not (installed? install-node)
        (install! install-node on-completed-callback))
      (let [instance (get-instance install-node)]
        (set-text! instance text)
        (focus! instance)))
    (when (installed? install-node)
      (remove! install-node))))
