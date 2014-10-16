;; Namepsace to async-tut1-core
;; require-macros will load ClojureScript macros, which is the core.async library rewritten in ClojureScript
;; require is pulling in goog.dom and goog.events from the Google Closure js library:  http://docs.closure-library.googlecode.com/git/index.html
;; import is pulling in specific types from the library:  JsonP and Uri
(ns async-tut1.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [goog.dom :as dom]
            [goog.events :as events]
            [cljs.core.async :refer [put! chan <!]])
  (:import [goog.net Jsonp]
           [goog Uri]))

;; Defines our search url for Wikipedia
(def wiki-search-url 
  "http://en.wikipedia.org/w/api.php?action=opensearch&format=json&search=")

;; Creates a listener on the element with the given event
(defn listen [el type]
  ;; Create a new channel and bind to 'out'
  (let [out (chan)]
    ;; Use the google/events library to listen on the element for the given type (here a 'click' event)
    (events/listen el type
       ;; Event handler called when click is called.  It puts the click event on the async channel
       (fn [e] (put! out e)))
    out))
 
 
(defn jsonp [uri]
  ;; Create a new channel and bind to 'out'
  ;; Also create a new request object
  (let [out (chan)
    	req (Jsonp. (Uri. uri))]
    ;; Send the request, passing a callback that gets the response and puts the value of that onto the async channel
    (.send req nil (fn [res] (put! out res)))
    out))

;; Adds 'q' to the end of the search URL
(defn query-url [q]
  (str  wiki-search-url q))

;; Returns the value the user entered in the input text ("query")
(defn user-query []
  (.-value (dom/getElement "query")))

;; Initialization
(defn init []
  ;; Let block - Binds the click listener on the search button to 'clicks'
  ;;	Binds the results div to 'results-view'
  (let [clicks (listen (dom/getElement "search") "click")
        results-view (dom/getElement "results")]
    ;; Asynchronously executes the body.  Calls to <! will block
    (go (while true
          ;; Takes the value of clicks from the port
          (<! clicks)
          ;; Issues the request over jsonp
          (let [[_ results] (<! (jsonp (query-url (user-query))))]
            ;; Sets the results in the innerHTML of the results-view
            (set! (.-innerHTML results-view) (render-query results)))))))

;; Strings the given results together into an unordered list
(defn render-query [results]
  (str
    "<ul>"
    (apply str
           (for [result results]
             (str "<li>" result "</li>")))
    "</ul>"))

;; Calls the init function
(init)

;; Defaults println to print to the browser console.  The exclamation point is a style guide suggestion
;;	which indicates this function is not pure and not safe in STM transactions (modifying state)
(enable-console-print!)

;; Prints Hello World to the console using Clojure println
(println "Hello world!")

;; Queries for the DOM element with id of "query" and logs to console (uses JS logging)
(.log js/console (dom/getElement "query"))

;; Listen for clicks on search and log the events to the console
(let [clicks (listen (dom/getElement "search") "click")]
  (go (while true
        (.log js/console (<! clicks)))))

;; Log the results of a query of 'cats' to the console
(go (.log js/console (<! (jsonp (query-url "cats")))))


