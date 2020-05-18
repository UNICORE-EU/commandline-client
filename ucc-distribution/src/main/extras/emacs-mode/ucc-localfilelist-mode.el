;;; ucc-localfilelist-mode.el --- access ucc from within Emacs

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


(defface ucc-localfile
  '((t (:underline t)))
  "Face to use for local files."
  :group 'ucc)


(defface ucc-localfile-mouse
  '((t (:background "darkseagreen2")))
  "Face to use for local files on mouse-over."
  :group 'ucc)


(defvar ucc-localfile-menu
  (let ((map (make-sparse-keymap "ucc local file")))
    (define-key map [open-file-other-window]
      '("Open file other window" . ucc-open-local-file-other-window))
    (define-key map [open-file] '("Open file" . ucc-open-local-file))
    map)
  "Keymap for a clickable local file.")


(defvar ucc-localfile-keymap
  (let ((map (make-sparse-keymap)))
    (define-key map (kbd "C-c f") 'ucc-open-local-file)
    (define-key map (kbd "C-c o") 'ucc-open-local-file-other-window)
    (define-key map [mouse-3] (lambda (event) (interactive "@e")
				(mouse-set-point event)
				(popup-menu ucc-localfile-menu)))
    map)
  "Keymap for a clickable local file.")
    
(fset 'ucc-localfile-keymap ucc-localfile-keymap)


(defvar ucc-localfilelist-mode-font-lock-keywords
  '(("^\\([a-zA-Z]:\\|[./]\\)\\w*" 0 '(face ucc-localfile
					      mouse-face ucc-localfile-mouse
					      help-echo "mouse-3: menu"
					      keymap ucc-localfile-keymap
					      field "ucc-localfile")))
  "Highlighting of local file names."
  )


(define-derived-mode ucc-localfilelist-mode ucc-output-mode
  "ucc output"
  "Major mode for ucc output.

This mode is automatically activated in ucc output buffers.

You can right-click on EPRS to get a context menu or perform the
following keybord shortcuts on them:

\\{ucc-epr-keymap}

You can right-click on local file names to get a context menu or perform the
following keybord shortcuts on them:

\\{ucc-localfile-keymap}

"

  (setq font-lock-defaults (list (append
				  ucc-output-mode-font-lock-keywords
				  ucc-localfilelist-mode-font-lock-keywords))))


(defun ucc-open-local-file ()
"Open the file under the cursor."
(interactive)
  (find-file (field-string)))


(defun ucc-open-local-file-other-window ()
"Open the file under the cursor in other window."
  (interactive)
  (find-file-other-window (field-string)))


(provide 'ucc-localfilelist-mode)
