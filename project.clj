(defproject tixi "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://example.com/FIXME"

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [com.facebook/react "0.10.0"]
                 [org.clojure/clojurescript "0.0-2202"]
                 [org.clojars.astashov/quiescent "0.1.4-1-SNAPSHOT"]
                 [prismatic/dommy "0.1.1"]
                 [org.clojure/core.async "0.1.301.0-deb34a-alpha"]
                 [prismatic/schema "0.2.2"]
                 [cider/cider-nrepl "0.6.1-SNAPSHOT"]]

  :jvm-opts ["-Xmx1G"]

  :plugins [[lein-cljsbuild "1.0.3"]
            [com.cemerick/austin "0.1.4"]
            [cider/cider-nrepl "0.6.1-SNAPSHOT"]]

  :profiles {
    :dev {
       :plugins [[com.cemerick/clojurescript.test "0.3.0"]]}}

  :source-paths ["src"]

  :cljsbuild {
    :builds [{:id "tixi"
              :source-paths ["src"]
              :compiler {
                :output-to "tixi.js"
                :output-dir "out"
                :preamble ["react/react_with_addons.min.js" "tixi/js/codemirror.js"]
                :libs ["tixi/js/drawer.js" "tixi/js/uuid.js" "tixi/js/compress.js"]
                :optimizations :none
                :source-map true}}
              {:id "dev"
               :source-paths ["src" "brepl"]
               :notify-command ["./notifier"]
               :compiler {
                 :output-to "tixi_dev.js"
                 :optimizations :whitespace
                 :pretty-print true
                 :preamble ["react/react_with_addons.min.js" "tixi/js/codemirror.min.js"]
                 :libs ["tixi/js/drawer.js" "tixi/js/uuid.js" "tixi/js/compress.js"]
                 :externs ["react/externs/react.js" "tixi/js/externs.js"]
                 :source-map "tixi_dev.js.map"}}
              {:id "release"
               :source-paths ["src"]
               :compiler {
                 :output-to "tixi_prod.js"
                 :optimizations :advanced
                 :pretty-print false
                 :preamble ["react/react_with_addons.min.js" "tixi/js/codemirror.min.js"]
                 :libs ["tixi/js/drawer.js" "tixi/js/uuid.js" "tixi/js/compress.js"]
                 :externs ["react/externs/react.js" "tixi/js/externs.js"]
                 :source-map "tixi_prod.js.map"}}
              {:id "test"
               :source-paths ["src" "test"]
               :notify-command ["phantomjs" :cljs.test/runner "resources/tixi/js/function-bind-shim.js" "resources/tixi/js/raf.js" "tixi_test.js"]
               :compiler {
                 :preamble ["react/react_with_addons.min.js" "tixi/js/codemirror.js"]
                 :libs ["tixi/js/drawer.js" "tixi/js/uuid.js" "tixi/js/compress.js"]
                 :output-to "tixi_test.js"
                 :optimizations :whitespace}}]
    :test-commands {"unit"
      ["phantomjs" :runner "resources/tixi/js/function-bind-shim.js" "resources/tixi/js/raf.js" "tixi_test.js"]}})
