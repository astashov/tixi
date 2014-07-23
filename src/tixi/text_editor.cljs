(ns tixi.text-editor
  (:require-macros [dommy.macros :refer (node sel)]
                   [tixi.utils :refer (b)])
  (:require [tixi.utils :refer [p]]
            [tixi.position :as p]
            [clojure.string :as string]
            [tixi.geometry :as g]))

(def ^:private codemirror-sizer-width 31)

(defn- find-node [install-node]
  (.querySelector (.-parentNode install-node) ".CodeMirror"))

(defn- remove! [install-node]
  (let [node (find-node install-node)]
    (.removeChild (.-parentNode node) node)))

(defn- padding-top [node]
  (js/parseInt (.-paddingTop (.getComputedStyle js/window node)) 10))

(defn- padding-left [node]
  (js/parseInt (.-paddingLeft (.getComputedStyle js/window node)) 10))

(defn- current-text-width [install-node node]
  (or (if-let [instance (get-instance install-node)]
        (apply max (map count (string/split (.getValue instance) "\n")))
        (apply max (map #(count (.-textContent %)) (sel node :.text--wrapper--content--line))))
      0))

(defn adjust-position! [install-node node editor? text?]
  (let [v-padding (padding-top install-node)
        h-padding (padding-left install-node)
        install-node-height (- (.-offsetHeight install-node) (* v-padding 2))
        install-node-width (- (.-offsetWidth install-node) (* h-padding 2))
        node-height (.-offsetHeight node)
        text-width (p/width->position (current-text-width install-node node))
        margin-top (* (.floor js/Math (/ (- (/ install-node-height 2) (/ node-height 2))
                                        (:height (p/letter-size))))
                     (:height (p/letter-size)))
        margin-left (max (* (.floor js/Math (/ (- (/ install-node-width 2) (/ text-width 2))
                                               (:width (p/letter-size))))
                            (:width (p/letter-size)))
                         0)]
    (if text?
      (do
        (aset (.-style node) "maxWidth" "none")
        (aset (.-style node) "marginTop" "0")
        (aset (.-style node) "marginLeft" "0"))
      (do
        (aset (.-style node) "maxWidth" (str (inc install-node-width) "px"))
        (aset (.-style node) "marginTop" (str margin-top "px"))
        (aset (.-style node) "marginLeft" (str margin-left "px"))))
    (aset (.-style node) "width" (str (if editor? (+ text-width codemirror-sizer-width) text-width) "px"))))

(defn- installed? [install-node]
  (boolean (find-node install-node)))

(defn- get-instance [install-node]
  (when-let [node (find-node install-node)]
    (aget node "CodeMirror")))

(defn- set-text! [instance text]
  (.setValue instance text))

(defn- focus! [instance]
  (.focus instance)
  (.execCommand instance "goDocEnd"))

(defn- commit [instance node on-completed-callback]
  (let [size (g/Size. (- (.-offsetWidth node) codemirror-sizer-width) (.-offsetHeight node))
        value (.getValue instance)]
    (remove! node)
    (on-completed-callback value size)))

(defn- install! [install-node on-completed-callback text?]
  (let [instance (js/CodeMirror install-node #js {:lineWrapping true :mode "text/plain" :smartIndent false :electricChars false})
        node (find-node install-node)]
    (.on instance "change" (fn [] (adjust-position! install-node node true text?)))
    (.on instance "blur" (fn [] (commit instance node on-completed-callback)))
    (.on instance "keyHandled" (fn [_ _ event] (when (= (.-keyCode event) 27) ; Esc
                                                 (.blur (.getInputField instance)))))
    (js/setTimeout #(adjust-position! install-node node true text?) 0)))

(defn install-or-remove! [install? install-node text on-completed-callback text?]
  (if install?
    (do
      (when-not (installed? install-node)
        (install! install-node on-completed-callback text?))
      (let [instance (get-instance install-node)]
        (set-text! instance text)
        (focus! instance)))
    (when (installed? install-node)
      (remove! install-node))))
