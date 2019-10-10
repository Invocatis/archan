(defproject archan "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/core.async "0.4.500"]
                 [fchan "0.1.4"]
                 [honeysql "0.9.8"]
                 [org.clojure/java.jdbc "0.7.10"]
                 [org.clojure/core.cache "0.8.1"]
                 [org.postgresql/postgresql "42.2.8"]]
  :main ^:skip-aot archan.core
  :target-path "target/%s"
  :profiles 
   {:uberjar 
    {:aot :all
     :jvm-opts ["-Dclojure.server.repl={:port 5555 :accept clojure.core.server/repl}"]}})
