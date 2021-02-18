#!/usr/bin/env python

from __future__ import with_statement
from subprocess import Popen, PIPE, STDOUT

TEMPLATE = "ucc_bash_completion.template"
OUTPUT = "ucc_bash_completion.sh"
UCC_CMD = "ucc"

######################################################################

def find_commands():
    commands = []
    print "Running UCC to get the list of commands ... "
    p = Popen([UCC_CMD], stdout=PIPE, stderr=STDOUT)
    p.wait()
    for line in p.stdout.readlines():
        if not line.startswith(" "):
            continue
        else:
            commands.append(line.split()[0])

    return commands


def find_options(command):
    options = []
    print "Getting options for %s" % command
    p = Popen([UCC_CMD, command, "-h"], stdout=PIPE, stderr=STDOUT)
    p.wait()
    for line in p.stdout.readlines():
        if not line.startswith(" -"):
            continue
        else:
            s = line.split()[0]
            options.append(s.split(",")[1])

    return options


######################################################################

with open(TEMPLATE) as f:
    output = f.read()
    
commands = sorted(find_commands())
global_opts = find_options("system-info")
case_body = ""


for command in commands:
    if command == "rest":
        continue # This is a special case, it's in the template

    opts = find_options(command)
    opts = set(opts) - set(global_opts)
    s = '    %s)\n    opts="$global_opts %s"\n    ;;\n' % (command,
                                                           " ".join(opts))
    case_body += s


output = output % {"commands": " ".join(commands),
                   "global_opts": " ".join(global_opts),
                   "case_body": case_body}


with open(OUTPUT, "w") as f:
    f.write(output)
