#!/usr/bin/env python3

from subprocess import Popen, PIPE, STDOUT

TEMPLATE = "ucc_bash_completion.template"
OUTPUTS = [ "ucc_bash_completion.sh",
            "../../package/distributions/Default/src/etc/bash_completion.d/unicore-ucc" ]
UCC_CMD = "ucc"

######################################################################

def find_commands():
    commands = []
    print("Running UCC to get the list of commands ... ")
    p = Popen([UCC_CMD], stdout=PIPE, stderr=STDOUT, encoding="UTF-8")
    p.wait()
    for line in p.stdout.readlines():
        if not line.startswith(" "):
            continue
        else:
            commands.append(line.split()[0])

    return commands


def find_options(command):
    options = []
    print("Getting options for %s" % command)
    p = Popen([UCC_CMD, command, "-h"], stdout=PIPE, stderr=STDOUT, encoding="UTF-8")
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
global_opts = sorted(find_options("rest"))
global_opts.remove("--accept")
global_opts.remove("--content-type")
case_body = ""

for command in commands:
    if command == "rest":
        continue # This is a special case, it's in the template

    opts = find_options(command)
    opts = sorted(set(opts) - set(global_opts))
    s = '    %s)\n    opts="$global_opts %s"\n    ;;\n' % (command,
                                                           " ".join(opts))
    case_body += s


output = output % {"commands": " ".join(commands),
                   "global_opts": " ".join(global_opts),
                   "case_body": case_body}


for OUTPUT in OUTPUTS: 
    with open(OUTPUT, "w") as f:
        print("Writing to %s" % OUTPUT)
        f.write(output)
