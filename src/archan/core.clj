(ns archan.core
  (:require
    [clojure.java.shell :refer [sh with-sh-dir]]
    [fchan.core :as fchan]
    [clojure.core.async :refer [go]])
  (:gen-class))

(defn thread-url
  [{:keys [board no]}]
  (format "https://boards.4chan.org/%s/thread/%s" board no))

(defn download-thread
  [{:keys [arguments]} url]
  (let [{:keys [exit out err]}]
    (apply sh "thread-archiver" url arguments)))

(defn now
  []
  (.getTime (java.util.Date.)))

(defn of-thread
  [config {:keys [no board] :as thread}]
  (let [id {:no no :board board}]
    (println "Downloading" board no)
    (download-thread config (thread-url thread))
    (println "Finished" board no)))

(defn get-threads
 [board]
 (let [paginated-threads (fchan/get-thread-ids board)]
   (->> paginated-threads
     (map :threads)
     (reduce into [])
     (mapv #(assoc % :board board)))))

(defn of-board
  [{:keys [thread-pool-size] :or {thread-pool-size 4} :as config} board]
  (let [pool (java.util.concurrent.Executors/newFixedThreadPool thread-pool-size)
        tasks (map (fn [thread] #(of-thread config thread)) (get-threads board))]
    (.invokeAll pool tasks)))

(defn of-config
  [{:keys [boards] :as config}]
  (doseq [board boards]
    (of-board config board)))

(defn run
  [{:keys [cadence] :as config}]
  (loop []
    (of-config config)
    (println "Waiting" cadence "...")
    (Thread/sleep cadence)
    (recur)))

(defn -main
  [config-path & args]
  (let [config (read-string (slurp config-path))]
    (run config)))
