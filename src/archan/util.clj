(ns archan.util)

(defn- -shell
  [cmd-list]
  (let [pb (ProcessBuilder. cmd-list)]
    (.redirectOutput pb java.lang.ProcessBuilder$Redirect/INHERIT)
    (.redirectError pb java.lang.ProcessBuilder$Redirect/INHERIT)
    (.waitFor (.start pb))))


(defn shell
  ([cmd]
   (-shell (clojure.string/split cmd #"\s")))
  ([cmd arg0 & args]
   (-shell (into [cmd arg0] args))))
