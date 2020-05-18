;;; ucc-outout-mode.el --- access ucc from within Emacs

;; Copyright (C) 2008 Rebecca Breu, Research Centre Juelich

;; Maintainer: 2008 Rebecca Breu, Research Centre Juelich
;; Keywords: unicore, ucc
;; Created: 2008-03-31
;; Modified: 2008-03-31
;; X-URL: http://www.unicore.eu

;;; License

;; This program is free software; you can redistribute it and/or
;; modify it under the terms of the GNU General Public License
;; as published by the Free Software Foundation; either version 2
;; of the License, or (at your option) any later version.

;; This program is distributed in the hope that it will be useful,
;; but WITHOUT ANY WARRANTY; without even the implied warranty of
;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
;; GNU General Public License for more details.

;; You should have received a copy of the GNU General Public License
;; along with this program; if not, write to the Free Software
;; Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

(require 'derived)
(require 'custom)


(defface ucc-verbose
  '((t (:foreground "grey50")))
  "Face to use for for ucc's verbose output lines."
  :group 'ucc)


(defface ucc-emphasize
  '((t (:weight bold)))
  "Face to use for emphasized phrases."
  :group 'ucc)


(defface ucc-epr
  '((t (:underline t)))
  "Face to use for EPRs."
  :group 'ucc)


(defface ucc-epr-mouse
  '((t (:background "darkseagreen2")))
  "Face to use for EPRs on mouse-over."
  :group 'ucc)


(defface ucc-c9m
  '((t (:underline t)))
  "Face to use for c9m-files."
  :group 'ucc)


(defface ucc-c9m-mouse
  '((t (:background "darkseagreen2")))
  "Face to use for c9m-files on mouse-over."
  :group 'ucc)


(defface ucc-exception 
  '((t (:foreground "red" :weight bold)))
  "Face to use for exceptions."
  :group 'ucc)


(defvar ucc-epr-menu
  (let ((map (make-sparse-keymap "ucc epr")))
    (define-key map [trace-workflow]
      '("Trace workflow" . ucc-workflow-trace-at-point))
    (define-key map [workflow-info-long]
      '("Long workflow info" . ucc-workflow-info-long-at-point))
    (define-key map [workflow-info]
      '("Workflow info" . ucc-workflow-info-at-point))
    (define-key map [workflow-seperator] '("---"))
    (define-key map [list-remote-file]
      '("List remote dir" . ucc-list-remote-file-at-point))
    (define-key map [data-manage-seperator] '("---"))
    (define-key map [abort-job] '("Abort job" . ucc-abort-job-at-point))
    (define-key map [destroy-job] '("Destroy job" . ucc-destroy-job-at-point))
    (define-key map [get-output] '("Get output" . ucc-get-output-at-point))
    (define-key map [get-status] '("Get status" . ucc-get-status-at-point))
    map)
  "Keymap for a clickable end point reference.")

(defvar ucc-epr-keymap
  (let ((map (make-sparse-keymap)))
    (define-key map (kbd "C-c a") 'ucc-abort-job-at-point)
    (define-key map (kbd "C-c d") 'ucc-destroy-job-at-point)
    (define-key map (kbd "C-c o") 'ucc-get-output-at-point)
    (define-key map (kbd "C-c s") 'ucc-get-status-at-point)
    (define-key map (kbd "C-c n") 'ucc-workflow-info-at-point)
    (define-key map (kbd "C-c t") 'ucc-workflow-trace-at-point)
    (define-key map (kbd "C-c l") 'ucc-list-remote-file-at-point)
    (define-key map [mouse-3] (lambda (event) (interactive "@e")
				(mouse-set-point event)
				(popup-menu ucc-epr-menu)))
    map)
  "Keymap for a clickable end point reference.")

(fset 'ucc-epr-keymap ucc-epr-keymap)


(defvar ucc-c9m-menu
  (let ((map (make-sparse-keymap "ucc c9m file")))
    (define-key map [get-file]
      '("Get file" . ucc-get-file-at-point))
    map)
  "Keymap for a clickable c9m file.")


(defvar ucc-c9m-keymap
  (let ((map (make-sparse-keymap)))
    (define-key map (kbd "C-c f") 'ucc-get-file-at-point)
    (define-key map [mouse-3] (lambda (event) (interactive "@e")
				(mouse-set-point event)
				(popup-menu ucc-c9m-menu)))
    map)
  "Keymap for a clickable c9m-file.")

(fset 'ucc-c9m-keymap ucc-c9m-keymap)


(defvar ucc-output-mode-font-lock-keywords
  '(("^\\[ucc .*?\\].*" 0 'ucc-verbose)
    ("^Applications on target system.*\\|\\<\\(SUCCESSFUL\\|QUEUED\\|NOT SUCCESSFUL\\|ABORTED\\|RUNNING\\)\\>" 0 'ucc-emphasize)
    ("\\<\\(https://\\w*\\)" 1 '(face ucc-epr
				      mouse-face ucc-epr-mouse
				      help-echo "mouse 3: menu"
				      keymap ucc-epr-keymap
				      field "ucc-epr"))
    ("\\<\\(c9m:\\w*\\)" 1 '(face ucc-c9m
				   mouse-face ucc-c9m-mouse
				   help-echo "mouse 3: menu"
				   keymap ucc-c9m-keymap
				   field "ucc-c9m"))
    ("\\<\\(java\\|org\\|de\\)[.].*?\\(Exception\\|Error\\):?\\>" 0 
     'ucc-exception))
  "Syntax highlighting of ucc output.")


(defvar ucc-output-mode-syntax-table
  (let ((table (make-syntax-table)))
    (modify-syntax-entry ?_ "w" table)
    (modify-syntax-entry ?= "w" table)
    (modify-syntax-entry ?. "w" table)
    (modify-syntax-entry ?/ "w" table)
    (modify-syntax-entry ?\\ "w" table)
    (modify-syntax-entry ?- "w" table)
    (modify-syntax-entry ?: "w" table)
    (modify-syntax-entry ?? "w" table)
    table)
  "Syntax table for ucc output mode."
  )


(define-derived-mode ucc-output-mode fundamental-mode
  "ucc output"
  "Major mode for ucc output.

This mode is automatically activated in ucc output buffers.
You can right-click on EPRS to get a context menu or perform the
following keybord shortcuts on them:

\\{ucc-epr-keymap}
"
  (make-local-variable 'font-lock-extra-managed-props)
  (setq font-lock-extra-managed-props '(mouse-face help-echo keymap field))

  (make-local-variable 'font-lock-defaults)
  (setq font-lock-defaults '(ucc-output-mode-font-lock-keywords)))


(defun ucc-get-status-at-point ()
  "Get status of the job whose EPR is under the cursor."
  (interactive)
  (ucc-get-status (field-string)))
			
(defun ucc-get-output-at-point ()
  "Get output of the job whose EPR is under the cursor."
  (interactive)
  (ucc-get-output (field-string)))
							      
(defun ucc-abort-job-at-point ()
  "Abort the job whose EPR is under the cursor."
  (interactive)
  (ucc-abort-job (field-string)))
							      
(defun ucc-destroy-job-at-point ()
  "Abort the job whose EPR is under the cursor."
  (interactive)
  (ucc-destroy-job (field-string)))
							      
(defun ucc-list-remote-file-at-point ()
  "List the remote directory  whose EPR is under the cursor."
  (interactive)
  (ucc-list-remote-file (field-string)))
							      
(defun ucc-workflow-trace-at-point ()
  "Trace the workflow whose EPR is under the cursor."
  (interactive)
  (ucc-workflow-trace (field-string)))
							      
(defun ucc-get-file-at-point (dest)
  "Get the file whose EPR is under the cursor."
  (interactive "FDestination: ")
  (ucc-get-file (field-string) dest))

(defun ucc-workflow-info-at-point ()
  "List workflow whose EPR is under the cursor."
  (interactive)
  (ucc-start-process 'ucc-output-mode "workflow-info" "-l"
		     (format "\"%s\"" (field-string))))

(defun ucc-workflow-info-long-at-point ()
  "List workflow whose EPR is under the cursor."
  (interactive)
  (ucc-start-process 'ucc-output-mode "workflow-info" "-l" "-a"
		     (format "\"%s\"" (field-string))))

(provide 'ucc-output-mode)
