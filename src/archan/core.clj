(ns archan.core
  (:require
    [archan.util :refer [shell]]
    [clojure.java.shell :refer [sh with-sh-dir]]
    [fchan.core :as fchan]
    [abrade.client :as abrade]
    [clojure.core.async :refer [go]]
    [clojure.java.io :as io])
  (:gen-class))

(defn thread-url
  [{:keys [board no]}]
  (format "https://boards.4chan.org/%s/thread/%s" board no))

(defmacro suppress-output
  [& body]
  `(let [out# (new java.io.StringWriter)
         err# (new java.io.StringWriter)]
     (binding [*out* out#
               *err* err#]
       ~@body)))

(.setLevel (java.util.logging.Logger/getLogger
             "com.gargoylesoftware.htmlunit")
           java.util.logging.Level/OFF);
(.setLevel (java.util.logging.Logger/getLogger
             "org.apache.commons.httpclient")
           java.util.logging.Level/OFF);

(defn delete-directory
  "Recursively delete a directory."
  [^java.io.File file]
  ;; when `file` is a directory, list its entries and call this
  ;; function with each entry. can't `recur` here as it's not a tail
  ;; position, sadly. could cause a stack overflow for many entries?
  (when (.isDirectory file)
    (doseq [file-in-dir (.listFiles file)]
      (delete-directory file-in-dir)))
  ;; delete the file or directory. if it it's a file, it's easily
  ;; deletable. if it's a directory, we already have deleted all its
  ;; contents with the code above (remember?)
  (io/delete-file file))

(defn copy-uri-to-file
  [uri file]
  (with-open [in (clojure.java.io/input-stream uri)
              out (clojure.java.io/output-stream file)]
    (clojure.java.io/copy in out)))

(defn download-images
  [{:keys [path]} {:keys [no board] :as thread}]
  (let [thread (fchan/get-thread board no)]
    (for [{:keys [tim ext filename] :as post} (:posts thread)]
      (when tim
        (copy-uri-to-file
          (fchan/get-image-url board tim ext)
          (format "%s/%s/%s/thread/%s%s" path board no tim ext))))))

(defn download-thread
  [{:keys [path] :as config} {:keys [no board] :as thread}]
  (let [folder (format "%s/%s/%s/" path board no)
        path (str folder "thread")
        browser (abrade/browser :chrome)
        wp (abrade/open browser (thread-url thread))
        folder (java.io.File. folder)]
    (when (.exists folder)
      (delete-directory folder))
    (.save wp (java.io.File. path))
    (download-images config thread)))

(defn download-thread
  [{:keys [arguments]} thread]
  ; (println 'thread-archiver (thread-url thread) arguments)
  (apply shell "thread-archiver" (thread-url thread) arguments))

(defn now
  []
  (.getTime (java.util.Date.)))

(defn of-thread
  [config {:keys [no board] :as thread}]
  (let [id {:no no :board board}]
    ; (println "Downloading" board no)
    (download-thread config thread)))
    ; (println "Finished" board no)))

(defn get-threads
 [board]
 (let [paginated-threads (fchan/get-thread-ids board)]
   (->> paginated-threads
     (map :threads)
     (reduce into [])
     (mapv #(assoc % :board board)))))

(defn of-board-par
  [{:keys [thread-pool-size] :or {thread-pool-size 4} :as config} board]
  (let [pool (java.util.concurrent.Executors/newFixedThreadPool thread-pool-size)
        threads (get-threads board)
        tasks (map-indexed (fn [i thread]
                             #(do (println (format "Thread %s/%s" i (count threads)))
                                  (of-thread config thread)))
                           threads)]
    (.invokeAll pool tasks)))

(defn of-board-seq
  [config board]
  (let [i (atom 0)
        threads (get-threads board)]
    (doseq [thread threads]
      (println (format "Thread %s/%s" (str @i) (str (count threads))))
      (of-thread config thread)
      (swap! i inc))))

(defn of-config
  [{:keys [boards thread-pool-size] :as config}]
  (doseq [board boards]
    (if (> thread-pool-size 1)
      (of-board-par config board)
      (of-board-seq config board))))

(defn wait-message
  [{:keys [hours minutes seconds]}]
  (format "Waiting %s ..."
    (apply str
      (interpose ", "
        (remove nil?
          (vector
            (when (and hours   (not= 0 hours))
              (str hours " hour"
                (when (> hours 1) "s")))
            (when (and minutes (not= 0 minutes))
              (str minutes " minute"
                (when (> minutes 1) "s")))
            (when (and seconds (not= 0 seconds))
              (str seconds " second"
                (when (> seconds 1) "s")))))))))

(defn run
  [{:keys [cadence] :as config}]
  (loop []
    (of-config config)
    (let [{:keys [hours minutes seconds]} cadence]
      (println (wait-message cadence))
      (Thread/sleep (+ (* 1000 60 60 hours) (* 1000 60 minutes) (* 1000 seconds))))
    (recur)))

(defn -main
  [& [config-path & args]]
  (let [config-path (or config-path "config.edn")
        config (read-string (slurp config-path))]
    (run config)))
