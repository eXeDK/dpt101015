(ns dpt_changes
  (:require [clojure.java.io :as io]
            [clojure.string :as cstr]))

;; Private Functions
;; ================
(defn- directory-exists
  "Checks if a directory exists, and terminates the program with an error if it does not"
  [dir-handle argument-name]
  (when-not (.isDirectory dir-handle)
    (println (str "ERROR: the provided \"" argument-name "\" is either missing or not a directory"))
    (System/exit -1)))

(defn- file-has-tag?
  "Returns true if a file is not a compiled file and has the dpt1010f15 one the first line otherwise false"
  [file-path]
  (when (.isFile file-path)
    (when (.canRead file-path)
      (when-not (.contains (.getAbsolutePath file-path) "/target/")
        (with-open [file-reader (io/reader file-path)]
          (.contains (.readLine file-reader) "dpt1010f15"))))))

(defn- retrieve-file-list
  "Find all files containing the tag dpt1010f15 in the first line"
  [input-directory]
  (let [dir-handle (io/file input-directory)]
    (directory-exists dir-handle "input-directory")
    ; Is the file readable and contain the tag in the first string?
    (into () (reverse (filter file-has-tag? (file-seq dir-handle))))))

(defn- print-file-marks
  "Prints all lines in the file containing an element in the code-marks global list"
  [marks file]
  ; We would not have found the file if we could not read it, so no need for checks
  (with-open [file-reader (io/reader file)]
    ; Going through two separate seq's provides the Cartesian product and range is infinite
    (doseq [[line line-number] (map list (line-seq file-reader) (range))]
      (when (some #(.contains line %) marks)
        ; Clojure ranges starts from zero and requires both an start and end to change start
        (println (str "  [Line: " (+ line-number 1) "] " (cstr/trim line)))))))

;; Public Functions
;; ================
(defn list-changed-files
  "Prints all files containing the tag dpt1010f15 in the first line"
  [input-directory file-operation]
  ; Prints all files found with the tag by retrieve-file-list
  (doseq [file (retrieve-file-list input-directory)]
    (println (.getAbsolutePath file))
    (when (fn? file-operation)
      (file-operation file))))

(defn extract-changed-files
  "Prints and extracts all files containing the tag dpt1010f15 in the first line"
  [input-directory output-directory]
  (let [output-dir-handle (io/file output-directory)]
    ; Retrieve-file-list checks if the input-directory exists
    (directory-exists output-dir-handle "output-directory")
    ; Prints and copies all files found with the tag by retrieve-file-list to output-directory
    (doseq [file (retrieve-file-list input-directory)]
      (println (.getAbsolutePath file))
      (io/copy ; io/copy input-path (io/file) output-path (io/file)
               file
               (io/file (str (.getAbsolutePath output-dir-handle) (java.io.File/separatorChar) (.getName file)))))))

(defn usage
  "Prints the usage messages if anything unexpected is detected"
  [code-marks]
  (println "usage: clojure dpt_changes operation input-directory [output-directory]"
           "\noperations"
           "\n\t-l prints the name of all files containing the string \"dpt1010f15\" to stdout"
           "\n\t-lt like -l but prints all lines containing either of the following" code-marks
           "\n\t-e copies all files containing the string \"dpt1010f15\" in the first line to output-directory"
           "\n\t-h shows the same helpfull documentation as calling the program without any or wrong arguments"))

;; Main and Constants
;; ==================
(def code-marks ["TODO" "HACK" "FIXME" "XXX"]) ; Colon is not present to get false positives instead of false negatives

(case [(count *command-line-args*) (first *command-line-args*)]
  [2 "-l"] (list-changed-files (second *command-line-args*) nil)
  [2 "-lt"] (list-changed-files (second *command-line-args*) (partial print-file-marks code-marks))
  [3 "-e"] (apply extract-changed-files (drop 1 *command-line-args*))
  ; Either -h or an unknown flag, either way help time
  (usage code-marks))
