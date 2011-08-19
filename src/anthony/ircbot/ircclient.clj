(ns anthony.ircbot.ircclient
  (import (java.net Socket)
    (java.io OutputStreamWriter BufferedWriter InputStreamReader BufferedReader))
  (:use [anthony.ircbot.irc]))

(declare parse-message)

(defn connect [conn-info event-functions]
  (let [irc-sock (Socket. (:host conn-info) (:port conn-info))
        irc-input (BufferedReader. (InputStreamReader. (.getInputStream irc-sock)))
        irc-output (BufferedWriter. (OutputStreamWriter. (.getOutputStream irc-sock)))
        irc-server (:host conn-info)
        connection-info (assoc conn-info :writer irc-output :sock irc-sock :reader irc-input)]
    (write-line irc-output (str "NICK " (:nick conn-info)))
    (write-line irc-output (str "USER " (:user conn-info) " * * :" (:version conn-info)))
    (loop [irc-string (.readLine irc-input)]
      (when (not= irc-string nil)
        (println irc-string)
        (if (= (.startsWith irc-string "PING") true)
          (write-line irc-output (.replace irc-string "PING" "PONG")))
        (if (= (.startsWith irc-string ":") true)
          (let [irc-string-new (subs irc-string 1) operation (get-arg irc-string-new 1)]
            (parse-message event-functions connection-info operation irc-string-new)))
        (recur (.readLine irc-input)))))
  (println "Connection was closed =("))

(defn parse-message [event-functions connection-info operation line]
  (let [keyword-operation (keyword operation)
        source (get-arg line 0)
        args-line (get-args-str line)]
    (if (contains? event-functions keyword-operation)
      (if (ifn? (keyword-operation event-functions))
        (apply (keyword-operation event-functions) connection-info (get-user-info source) (seq (get-args args-line)))))))