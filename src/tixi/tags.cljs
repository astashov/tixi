;; Shamelessly stolen from Om
;;   https://github.com/swannodette/om/blob/f9cd0ce959b17b903a78f5b9c79300291eba7b00/src/om/dom.cljs#L7
;;
;; It fixes this issue:
;;
;; https://github.com/swannodette/om/issues/13
;;
;; TODO: Maybe would be a good idea to implement that right in Quiescent

(ns tixi.tags
  (:require [quiescent :as q]))

(defn- wrap-form-element [ctor display-name]
  (js/React.createClass
    #js
    {:getDisplayName
     (fn [] display-name)
     :getInitialState
     (fn []
       (this-as this
         #js {:value (aget (.-props this) "value")}))
     :onChange
     (fn [e]
       (this-as this
         (let [handler (aget (.-props this) "onChange")]
           (when-not (nil? handler)
             (handler e)
             (.setState this #js {:value (.. e -target -value)})))))
     :componentWillReceiveProps
     (fn [new-props]
       (this-as this
         (.setState this #js {:value (aget new-props "value")})))
     :render
     (fn []
       (this-as this
         (.transferPropsTo this
           ;; NOTE: if switch to macro we remove a closure allocation
           (ctor #js {:value (aget (.-state this) "value")
                      :onChange (aget this "onChange")
                      :children (aget (.-props this) "children")}))))}))

(def -input (wrap-form-element js/React.DOM.input "input"))

(defn input [args]
  (let [js-pr (q/js-props args)]
    (-input js-pr)))
