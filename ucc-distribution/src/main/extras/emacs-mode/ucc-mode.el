;;; ucc-mode.el --- access ucc from within Emacs

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

(require 'ucc-localfilelist-mode)
(require 'ucc-output-mode)
(require 'cl)
(require 'custom)

;; General configuration:

(defgroup ucc nil
  "Run ucc (UNICORE command line client) from within Emacs."
  :group 'tools
  :group 'processes)
  

(defcustom ucc-command "/usr/bin/ucc"
  "Path to the ucc executable."
  :type 'file
  :group 'ucc)

(defcustom ucc-flags ""
  "String of flags passed to every ucc call, i.e.:

ucc <command> <ucc-flags> ...

For example, set it to \"-v\" for verbose output, to
\"-c /path/to/preferences\" to specify the configuration file used,
or leave it an empty string."
  :type 'string
  :group 'ucc)

(defcustom ucc-reuse-buffer t
  "If nil, create a new output buffer for each ucc process.

If non-nil, reuse old buffer. This way, only one process may run at
any time, and old output will be overwritten."
  :type 'boolean
  :group 'ucc)


;; Setting up general key bindings and menus:

(defvar ucc-workflow-submenu
  (let ((map (make-sparse-keymap)))
    (define-key map [workflow-info]
      '("Workflow info" . ucc-workflow-info))
    (define-key map [submit-file]
      '("Submit workflow file" . ucc-workflow-submit-file))
    map)
  "Keymap for workflow submenu in ucc minor mode.")


(defvar ucc-mode-map 
  (let ((map (make-sparse-keymap)))
    ;; keybindings:
    (define-key map (kbd "C-c C-c") 'ucc-connect)
    (define-key map (kbd "C-c C-r") 'ucc-run-file)
    (define-key map (kbd "C-c C-g") 'ucc-run-groovy-file)
    (define-key map (kbd "C-c C-j") 'ucc-list-jobs)
    (define-key map (kbd "C-c C-t") 'ucc-list-sites)
    (define-key map (kbd "C-c C-p") 'ucc-list-applications)
    (define-key map (kbd "C-c C-y") 'ucc-system-info)
    (define-key map (kbd "C-c C-s") 'ucc-get-status)
    (define-key map (kbd "C-c C-o") 'ucc-get-output)
    (define-key map (kbd "C-c C-f") 'ucc-get-file)
    (define-key map (kbd "C-c C-a") 'ucc-abort-job)
    (define-key map (kbd "C-c C-d") 'ucc-destroy-job)
    (define-key map (kbd "C-c C-l") 'ucc-list-remote-file)
    (define-key map (kbd "C-c C-u") 'ucc)
    (define-key map (kbd "C-c C-w") 'ucc-workflow-submit-file)
    (define-key map (kbd "C-c C-n") 'ucc-workflow-info)
    ;; ucc menu in menu bar:
    (define-key map [menu-bar ucc] (cons "ucc" (make-sparse-keymap)))
    (define-key map [menu-bar ucc customize]
      '("Customize ucc-mode..." . (lambda ()
				    (interactive)
				    (customize-group "ucc"))))
    (define-key map [menu-bar ucc customize-seperator] '("---"))
    (define-key map [menu-bar ucc ucc] '("General ucc command..." . ucc))
;;    (define-key map [menu-bar ucc wf-seperator] '("---"))
    (define-key map [menu-bar ucc wf]
      (list 'menu-item "Workflow" ucc-workflow-submenu))
    (define-key map [menu-bar ucc general-seperator] '("---"))
    (define-key map [menu-bar ucc list-storages]
      '("List storages" . ucc-list-storages))
    (define-key map [menu-bar ucc list-remote-dir]
      '("List remote file or dir..." . ucc-list-remote-file))
    (define-key map [menu-bar ucc data-manage-seperator]
      '("---"))
    (define-key map [menu-bar ucc destroy-job]
      '("Destroy job..." . ucc-destroy-job))
    (define-key map [menu-bar ucc abort-job]
      '("Abort job..." . ucc-abort-job))
    (define-key map [menu-bar ucc get-file]
      '("Get file..." . ucc-get-file))
    (define-key map [menu-bar ucc get-output]
      '("Get output..." . ucc-get-output))
    (define-key map [menu-bar ucc get-status]
      '("Get status..." . ucc-get-status))
    (define-key map [menu-bar ucc list-jobs]
      '("List jobs" . ucc-list-jobs))
    (define-key map [menu-bar ucc job-manage-seperator]
      '("---"))
    (define-key map [menu-bar ucc run-groovy-file-at-site]
      '("Run groovy file at site..." . ucc-run-groovy-file-at-site))
    (define-key map [menu-bar ucc run-groovy-file]
      '("Run groovy file" . ucc-run-groovy-file))
    (define-key map [menu-bar ucc run-file-at-site]
      '("Run file at site..." . ucc-run-file-at-site))
    (define-key map [menu-bar ucc run-file]
      '("Run file" . ucc-run-file))
    (define-key map [menu-bar ucc submit-seperator]
      '("---"))
    (define-key map [menu-bar ucc system-info]
      '("System info" . ucc-system-info))
    (define-key map [menu-bar ucc list-sites]
      '("List sites" . ucc-list-sites))
    (define-key map [menu-bar ucc list-applications]
      '("List applications" . ucc-list-applications))
    (define-key map [menu-bar ucc connect]
      '("Connect" . ucc-connect))
    map)
  "Keymap for ucc minor mode.")



;; Setting up minor mode:

(define-minor-mode ucc-mode
  "Toggle ucc-mode.

ucc-mode is a minor mode which provides a menu and shortcuts for accessing
the ucc-* functions. It is not part of GNU Emacs.

The ucc-* functions call the external UNICORE command line client.
Before first usage, make sure that the variable ucc-command points to
your ucc executable.

See www.unicore.eu for further information on UNICORE.

\\{ucc-mode-map}
"
  :init-value nil
  :lighter ""
  :keymap 'ucc-mode-map
  :group 'ucc
  )




;; Internal functions and variables for calling external ucc executable:

(defvar ucc-running-process nil
  "Points to the running ucc process or nil.")


(defun ucc-sentinel (process string)
  "Keep track if ucc process exits and set ucc-running-process accordingly."
  (if (eq (process-status process) 'exit)
	  (save-excursion
	    (setq ucc-running-process nil)
	    (set-buffer (process-buffer process))
	    (goto-char (point-max))
	    (insert "\n--- UCC finished at ")
	    (insert (format-time-string "%Y-%m-%dT%T%z\n" (current-time))))))


(defun* ucc-start-process (buffermode command &rest args)
  "Start ucc asynchronously, put output buffer in mode BUFFERMODE.
If there is already a ucc process running, ask user to terminate it.

COMMAND is the ucc command, ARGS are further arguments passed to ucc."

  (let ((buffer nil))
    (if ucc-reuse-buffer
	(progn
	  (if ucc-running-process
	      (if (not (yes-or-no-p
			"There is already a ucc process running. Terminate it? "))
		  (error "Aborted")
		(signal-process (process-id ucc-running-process) 'SIGINT)))
	  (setq buffer (get-buffer-create "*ucc*")))
      (setq buffer (generate-new-buffer "*ucc*")))

    (set-buffer buffer)
    (apply buffermode nil)
    (ucc-mode t)
    (erase-buffer)
    (let* ((process-connection-type nil) ;; use pipe
	   (process
	    (apply 'start-process-shell-command
		   (append
		    (list "ucc" buffer ucc-command command ucc-flags)
		    args))))
      (setq ucc-running-process process)
      (set-process-sentinel process 'ucc-sentinel))
    (display-buffer buffer)))


;; ucc functions:

(defun ucc-connect ()
  "Connect to UNICORE."
  (interactive)
  (ucc-start-process 'ucc-output-mode "connect"))


(defun ucc-list-jobs ()
  "List your jobs."
  (interactive)
  (ucc-start-process 'ucc-output-mode "list-jobs"))


(defun ucc-list-sites ()
  "List remote sites."
  (interactive)
  (ucc-start-process 'ucc-output-mode "list-sites"))


(defun ucc-list-applications ()
  "Lists applications on target systems."
  (interactive)
  (ucc-start-process 'ucc-output-mode "list-applications"))


(defun ucc-get-status (job)
  "Get job status of JOB.

JOB is an EPR (endpoint reference)."
  (interactive "MJob identifier: ")
  (ucc-start-process 'ucc-output-mode "job-status" (format "\"%s\"" job)))
  

(defun ucc-get-output (job)
  "Get output files of JOB.

JOB is an EPR (endpoint reference)."
  (interactive "MJob identifier: ")
  (ucc-start-process 'ucc-localfilelist-mode "get-output" 
		     (format "\"%s\"" job)))
  

(defun ucc-abort-job (job)
  "Abort JOB.

JOB is an EPR (endpoint reference)."
  (interactive "MJob identifier: ")
  (ucc-start-process 'ucc-output-mode "job-abort" (format "\"%s\"" job)))
  

(defun ucc-destroy-job (job)
  "Destroy JOB.

JOB is an EPR (endpoint reference)."
  (interactive "MJob identifier: ")
  (ucc-start-process 'ucc-output-mode "wsrf" "destroy" (format "\"%s\"" job)))
  

(defun ucc-run-file ()
  "Run the ucc job file in the current buffer."
  (interactive)
  (if (not buffer-file-name)
      (error "Buffer is not associated with any file")
    (ucc-start-process 'ucc-localfilelist-mode "run" 
		       (format "\"%s\"" buffer-file-name))))


(defun ucc-run-file-at-site (sitename)
  "Run the ucc job file in the current buffer at SITENAME."
  (interactive "MSite name: ")
  (if (not buffer-file-name)
      (error "Buffer is not associated with any file")
    (ucc-start-process 'ucc-localfilelist-mode "run"
		       "-s" (format "\"%s\"" sitename)
		       (format "\"%s\"" buffer-file-name))))


(defun ucc-run-groovy-file ()
  "Run the groovy script in the current buffer."
  (interactive)
  (if (not buffer-file-name)
      (error "Buffer is not associated with any file")
    (ucc-start-process 'ucc-localfilelist-mode "run-groovy"
		       "-f" (format "\"%s\"" buffer-file-name))))


(defun ucc-run-groovy-file-at-site (sitename)
  "Run the groovy script in the current buffer at SITENAME."
  (interactive "MSite name: ")
  (if (not buffer-file-name)
      (error "Buffer is not associated with any file")
    (ucc-start-process 'ucc-localfilelist-mode "run-groovy"
		       "-s" (format "\"%s\"" sitename)
		       (format "\"%s\"" buffer-file-name))))


(defun ucc-list-remote-file (file)
  "List FILE on a remote site.

FILE is the file or directory specified in the form
\"u6://SITE/path/to/dir\"
or as an end point reference."
  (interactive "MRemote File or Directory: ")
  (ucc-start-process 'ucc-output-mode "ls" "-l" (format "\"%s\"" file)))


(defun ucc-list-storages ()
  "List available storages."
  (interactive)
  (ucc-start-process 'ucc-output-mode "list-storages" "-l"))


(defun ucc-system-info ()
  "Check the availability of services."
  (interactive)
  (ucc-start-process 'ucc-output-mode "system-info" "-l"))


(defun ucc (parameters)
  "Run an arbitrary ucc command.

Pass PARAMETERS to the ucc call, e.g. \"run -v date.u\"."
  (interactive "MParameters to pass to ucc: ")
  (ucc-start-process 'ucc-output-mode parameters))


(defun ucc-workflow-submit-file ()
  "Submit the worklow file in the current buffer."
  (interactive)
  (if (not buffer-file-name)
      (error "Buffer is not associated with any file")
    (ucc-start-process 'ucc-output-mode "workflow-submit" 
		       (format "\"%s\"" buffer-file-name))))


(defun ucc-workflow-info ()
  "List submitted workflows."
  (interactive)
  (ucc-start-process 'ucc-output-mode "workflow-info" "-l"))

(defun ucc-get-file (src dst)
  "Get a file from a storage.

SRC is the data source, DST is the data target."
  (interactive "MSource: \nMTarget: ")
  (ucc-start-process 'ucc-output-mode "get-file"
		     "-s" (format "\"%s\"" src)
		     "-t" (format "\"%s\"" dst)))


(provide 'ucc-mode) 
	
  
