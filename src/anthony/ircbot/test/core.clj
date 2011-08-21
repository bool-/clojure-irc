(ns anthony.ircbot.core
  (:use [anthony.ircbot.ircclient]))

(defn on-connected [conn-info source-info nick message] ; source info and args are irrelevant
  (let [{:keys [host writer]} conn-info]
    (println (str "Connected to " host))
    (identify writer "xkjl14m")
    (join-channel writer "#lelel" nil)))

(defn on-message [conn-info user-info target message]
  (let [{:keys [writer]} conn-info]
    (println (str "[" target "] " (:nick user-info) ": " message))
    (when (.startsWith message ";")
      (def command (subs message 1))
      (when (= (:nick user-info) "anthony")
        (if (.startsWith command "eval") (send-message writer target (load-string (subs command 5))))))))

(defn on-notice [conn-info user-info target message]
  (let [{:keys [nick]} user-info]
    (println (str nick " has sent a notice to " target " saying " message))))

(defn on-kick [conn-info user-info channel kicked-nick reason]
  (let [{:keys [writer nick]} conn-info]
    (if (= kicked-nick nick) (join-channel writer channel nil))))

(defn on-join [conn-info user-info channel]
  (let [{:keys [nick]} user-info]
    (println (str nick " has joined " channel))))

(defn on-part [conn-info user-info channel reason]
  (let [{:keys [nick]} user-info]
    (println (str nick " has parted " channel " because " reason))))

(connect
  {:host "irc.strictfp.com" :port 6667 :nick "lisp_bot" :user "bawtzor" :version "lisp_bot-1.0"}
  {:JOIN on-join
   :PRIVMSG on-message
   :PART on-part
   :KICK on-kick
   :NOTICE on-notice
   (keyword "376") on-connected})
