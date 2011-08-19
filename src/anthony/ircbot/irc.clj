(ns anthony.ircbot.irc
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