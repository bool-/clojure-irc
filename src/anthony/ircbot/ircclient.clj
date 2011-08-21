(ns anthony.ircbot.ircclient
  (import (java.net Socket)
    (java.io OutputStreamWriter BufferedWriter InputStreamReader BufferedReader))
  (:require [clojure.string :as str]))

(defn get-user-info [line]
  (let [[nick user host] (str/split line #"[@!]")]
    {:nick nick :user user :host host}))

(defn get-arg [line arg]
  (get (str/split line #"\s") arg))

(defn get-args [line]
  (let [str-arg (get (str/split line #":") 1)
        args (str/split (get (str/split line #"\s:") 0) #"\s")]
    (if (= (.startsWith (first args) ":") false)
      (conj args str-arg)
      [str-arg])))

(defn get-args-str [line]
  (get (str/split line #"\s" 3) 2))

(defn write-line [writer line]
  (doto writer
    (.write line)
    (.newLine)
    (.flush)))

(defn send-message [writer target message]
  (write-line writer (str "PRIVMSG " target " :" message)))

(defn send-notice [writer target message]
  (write-line writer (str "NOTICE " target " :" message)))

(defn identify [writer password]
  (write-line writer (str "PRIVMSG NickServ :identify " password)))

(defn join-channel [writer channel password]
  (let [line (str "JOIN " channel)]
    (if (nil? password) (write-line writer line) (write-line writer (str line " " password)))))

(defn part-channel [writer channel message]
  (let [line (str "PART " channel)]
    (if (nil? message) (write-line writer line) (write-line writer (str line " :" message)))))

(defn parse-message-fn [event-functions connection-info operation line]
  (let [keyword-operation (keyword operation)
        source (get-arg line 0)
        args-line (get-args-str line)]
    (if (contains? event-functions keyword-operation)
      (if (ifn? (keyword-operation event-functions))
        (apply (keyword-operation event-functions) connection-info (get-user-info source) (seq (get-args args-line)))))))

(defn connect [conn-info event-functions]
  (let [irc-sock (Socket. (:host conn-info) (:port conn-info))
        irc-input (BufferedReader. (InputStreamReader. (.getInputStream irc-sock)))
        irc-output (BufferedWriter. (OutputStreamWriter. (.getOutputStream irc-sock)))
        irc-server (:host conn-info)
        connection-info (assoc conn-info :writer irc-output :sock irc-sock :reader irc-input)
        parse-message (fn [operation line] (parse-message-fn event-functions connection-info operation line))]
    (write-line irc-output (str "NICK " (:nick conn-info)))
    (write-line irc-output (str "USER " (:user conn-info) " * * :" (:version conn-info)))
    (loop [irc-string (.readLine irc-input)]
      (when (not= irc-string nil)
        (println irc-string)
        (if (= (.startsWith irc-string "PING") true)
          (write-line irc-output (.replace irc-string "PING" "PONG")))
        (if (= (.startsWith irc-string ":") true)
          (let [irc-string-new (subs irc-string 1) operation (get-arg irc-string-new 1)]
            (parse-message operation irc-string-new)))
        (recur (.readLine irc-input)))))
  (println "Connection was closed =("))